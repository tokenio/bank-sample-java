package io.token.banksample.impl;

import static io.token.proto.common.token.TokenProtos.TransferTokenStatus.FAILURE_DESTINATION_ACCOUNT_NOT_FOUND;
import static io.token.proto.common.token.TokenProtos.TransferTokenStatus.FAILURE_SOURCE_ACCOUNT_NOT_FOUND;
import static io.token.proto.common.transaction.TransactionProtos.TransactionStatus.FAILURE_CANCELED;
import static io.token.proto.common.transaction.TransactionProtos.TransactionStatus.FAILURE_INVALID_CURRENCY;
import static io.token.proto.common.transaction.TransactionProtos.TransactionStatus.SUCCESS;
import static io.token.proto.common.transaction.TransactionProtos.TransactionType.CREDIT;
import static io.token.proto.common.transaction.TransactionProtos.TransactionType.DEBIT;
import static io.token.sdk.util.ProtoFactory.newMoney;

import io.token.banksample.config.Account;
import io.token.banksample.model.AccountTransaction;
import io.token.banksample.model.AccountTransfer;
import io.token.banksample.model.Accounting;
import io.token.banksample.model.Pricing;
import io.token.proto.common.account.AccountProtos.BankAccount;
import io.token.proto.common.money.MoneyProtos.Money;
import io.token.sdk.api.InstantTransaction;
import io.token.sdk.api.InstantTransfer;
import io.token.sdk.api.PrepareTransferException;
import io.token.sdk.api.TransferException;
import io.token.sdk.api.service.InstantTransferService;

import java.util.UUID;

/**
 * Sample implementation of the {@link InstantTransferService}. Returns fake
 * data.
 *
 * TODO:
 *      - Remove the Optional quote passed by the client from the prepare* calls.
 *        We want to instead skip the prepare* calls altogether if we have the
 *        quote already.
 */
public class InstantTransferServiceImpl implements InstantTransferService {
    private final Accounting accounts;
    private final Pricing pricing;

    public InstantTransferServiceImpl(Accounting accounts, Pricing pricing) {
        this.accounts = accounts;
        this.pricing = pricing;
    }

    /**
     * Begins the debit sequence by placing money in the holding account.
     *
     * {@inheritDoc}
     */
    @Override
    public InstantTransaction beginDebitTransaction(InstantTransfer transfer) {
        Account account = accounts
                .lookupAccount(transfer.getAccount())
                .orElseThrow(() -> new PrepareTransferException(
                        FAILURE_SOURCE_ACCOUNT_NOT_FOUND,
                        "Account not found: " + transfer.getAccount()));

        AccountTransaction transaction = AccountTransaction.builder(DEBIT)
                .id(UUID.randomUUID().toString())
                .referenceId(transfer.getTokenTransferId())
                .from(transfer.getAccount())
                .to(transfer.getCounterpartyAccount().getAccount())
                .amount(
                        transfer.getTransactionAmount().doubleValue(),
                        transfer.getTransactionAmountCurrency())
                .description(transfer.getDescription())
                .build();
        accounts.createPayment(transaction);

        if (account.matches(accounts.getRejectAccount(transfer.getTransactionAmountCurrency()))) {
            transaction.setStatus(FAILURE_CANCELED);
        } else if (!transfer.getPricing().hasSourceQuote()) {
            // If FX is not needed, just move the money to the holding account.
            accounts.post(AccountTransfer.builder()
                    .transferId(transfer.getTokenTransferId())
                    .from(transfer.getAccount())
                    .to(accounts.getHoldAccount(transfer.getTransactionAmountCurrency()))
                    .withAmount(
                            transfer.getTransactionAmount().doubleValue(),
                            transfer.getTransactionAmountCurrency())
                    .build());
        } else {
            // Create two transfers to account for FX.
            // 1) DB customer, credit FX in the customer account currency.
            // 2) DB FX, credit hold account in the settlement account currency.
            // Note that we are not accounting for  the spread with this
            // transaction pair, it goes 'nowhere'.
            pricing.redeemQuote(transfer.getPricing().getSourceQuote());
            accounts.post(
                    AccountTransfer.builder()
                            .transferId(transfer.getTokenTransferId())
                            .from(transfer.getAccount())
                            .to(accounts.getFxAccount(transfer.getTransactionAmountCurrency()))
                            .withAmount(
                                    transfer.getTransactionAmount().doubleValue(),
                                    transfer.getTransactionAmountCurrency())
                            .build(),
                    AccountTransfer.builder()
                            .transferId(transfer.getTokenTransferId())
                            .from(accounts.getFxAccount(transfer.getSettlementAmountCurrency()))
                            .to(accounts.getHoldAccount(transfer.getSettlementAmountCurrency()))
                            .withAmount(
                                    transfer.getSettlementAmount().doubleValue(),
                                    transfer.getSettlementAmountCurrency())
                            .build());
        }

        // Return first transaction id back so that we can find the hold transaction
        // later during commit / rollback.
        return InstantTransaction.builder(transaction.getId())
                .amount(newMoney(
                        transfer.getTransactionAmount(),
                        transfer.getTransactionAmountCurrency()))
                .settlementAccount(accounts.getSettlementAccount(
                        transfer.getSettlementAmountCurrency()))
                .build();
    }

    /**
     * Moves money from the hold account to the settlement account, completing
     * the debit operation.
     *
     * {@inheritDoc}
     */
    @Override
    public void commitDebitTransaction(
            String transferId,
            String transactionId,
            BankAccount account,
            Money amount) {
        AccountTransaction transaction = accounts.lookupPayment(account, transactionId);
        transaction.setStatus(SUCCESS);
        accounts.post(AccountTransfer.builder()
                .transferId(transferId)
                .from(transaction.getTo())
                .to(accounts.getSettlementAccount(transaction.getCurrency()))
                .withAmount(transaction.getAmount(), transaction.getCurrency())
                .build());
    }

    /**
     * Reverses the previous hold acquired by the {@link #beginDebitTransaction}.
     *
     * {@inheritDoc}
     */
    @Override
    public void rollbackDebitTransaction(
            String transferId,
            String transactionId,
            BankAccount account,
            Money amount) {
        AccountTransaction transaction = accounts.lookupPayment(account, transactionId);
        transaction.setStatus(FAILURE_CANCELED);
        accounts.post(AccountTransfer.builder()
                .transferId(transferId)
                .from(transaction.getTo())
                .to(transaction.getFrom())
                .withAmount(transaction.getAmount(), transaction.getCurrency())
                .build());
    }

    /**
     * Credit leg of the instant updatePayment initiation. We don't want to do
     * anything here. Don't want to credit the customer account just yet,
     * the transaction has not cleared yet.
     *
     * <p>So all we do here is verify that account exists and that account
     * currency matches updatePayment currency, beneficiary side FX is not
     * supported at this point.
     *
     *
     * {@inheritDoc}
     */
    @Override
    public InstantTransaction beginCreditTransaction(InstantTransfer transfer) {
        Account account = accounts
                .lookupAccount(transfer.getAccount())
                .orElseThrow(() -> new PrepareTransferException(
                    FAILURE_DESTINATION_ACCOUNT_NOT_FOUND,
                    "Account not found: " + transfer.getAccount()));

        if (!account
                .getBalance()
                .getCurrency()
                .equals(transfer.getTransactionAmountCurrency())) {
            throw new TransferException(
                    FAILURE_INVALID_CURRENCY,
                    "Credit side FX is not supported");
        }

        if (account.matches(accounts.getRejectAccount(transfer.getTransactionAmountCurrency()))) {
            throw new TransferException(
                    FAILURE_CANCELED,
                    "Reject account - cancelled");
        }

        AccountTransaction transaction = AccountTransaction.builder(CREDIT)
                .id(UUID.randomUUID().toString())
                .referenceId(transfer.getTokenTransferId())
                .from(transfer.getAccount())
                .to(transfer.getCounterpartyAccount().getAccount())
                .amount(
                        transfer.getTransactionAmount().doubleValue(),
                        transfer.getTransactionAmountCurrency())
                .description(transfer.getDescription())
                .build();
        accounts.createPayment(transaction);

        return InstantTransaction.builder(transaction.getId())
                .amount(newMoney(
                        transfer.getTransactionAmount(),
                        transfer.getTransactionAmountCurrency()))
                .settlementAccount(accounts.getSettlementAccount(
                        transfer.getSettlementAmountCurrency()))
                .build();
    }

    /**
     * Credit customer account. We don't support beneficiary side FX at this
     * point, so simply debit settlement account and credit end user destination
     * account.
     *
     * TODO: Apply fees
     *
     * {@inheritDoc}
     */
    @Override
    public void commitCreditTransaction(
            String transferId,
            String transactionId,
            BankAccount account,
            Money amount) {
        AccountTransaction transaction = accounts.lookupPayment(account, transactionId);
        transaction.setStatus(SUCCESS);
        accounts.post(AccountTransfer.builder()
                .transferId(transferId)
                .from(accounts.getSettlementAccount(amount.getCurrency()))
                .to(account)
                .withAmount(amount)
                .build());
    }

    /**
     * There is nothing to do here because {@link #beginCreditTransaction} is
     * no-op.
     *
     * {@inheritDoc}
     */
    @Override
    public void rollbackCreditTransaction(
            String transferId,
            String transactionId,
            BankAccount account,
            Money amount) {
        accounts.deletePayment(account, transactionId);
    }
}

package io.token.banksample.services;

import static io.token.proto.common.token.TokenProtos.TransferTokenStatus.FAILURE_DESTINATION_ACCOUNT_NOT_FOUND;
import static io.token.proto.common.transaction.TransactionProtos.TransactionStatus.FAILURE_INVALID_CURRENCY;
import static io.token.proto.common.transaction.TransactionProtos.TransactionType.CREDIT;
import static io.token.proto.common.transaction.TransactionProtos.TransactionType.DEBIT;
import static io.token.sdk.util.ProtoFactory.newMoney;

import io.token.banksample.config.AccountConfig;
import io.token.banksample.model.AccountTransaction;
import io.token.banksample.model.Accounting;
import io.token.banksample.model.Pricing;
import io.token.proto.common.account.AccountProtos.BankAccount;
import io.token.proto.common.money.MoneyProtos.Money;
import io.token.sdk.api.InstantTransaction;
import io.token.sdk.api.InstantTransfer;
import io.token.sdk.api.PrepareTransferException;
import io.token.sdk.api.TransferException;
import io.token.sdk.api.service.InstantTransferService;

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
        pricing.redeemQuote(transfer.getPricing().getSourceQuote());

        // TODO: Make this idempotent.
        // TODO: We should still try to do FX if needed and there is no quote.
        // Request a quote on the fly.
        AccountTransaction transaction = AccountTransaction.builder(DEBIT)
                .referenceId(transfer.getTokenTransferId())
                .from(transfer.getAccount())
                .to(transfer.getCounterpartyAccount().getAccount())
                .amount(
                        transfer.getTransactionAmount().doubleValue(),
                        transfer.getTransactionAmountCurrency())
                .transferAmount(
                        transfer.getPricing().getSourceQuote(),
                        transfer.getSettlementAmount().doubleValue(),
                        transfer.getSettlementAmountCurrency())
                .description(transfer.getDescription())
                .build();
        accounts.createDebitTransaction(transaction);

        return InstantTransaction.builder(transaction.getId())
                .amount(newMoney(
                        transfer.getSettlementAmount(),
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
        accounts.commitDebitTransaction(account, transferId, transactionId);
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
        accounts.rollbackDebitTransaction(account, transferId, transactionId);
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
        AccountConfig account = accounts
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

        AccountTransaction transaction = AccountTransaction.builder(CREDIT)
                .referenceId(transfer.getTokenTransferId())
                .from(transfer.getAccount())
                .to(transfer.getCounterpartyAccount().getAccount())
                .amount(
                        transfer.getTransactionAmount().doubleValue(),
                        transfer.getTransactionAmountCurrency())
                .transferAmount(
                        transfer.getPricing().getDestinationQuote(),
                        transfer.getSettlementAmount().doubleValue(),
                        transfer.getSettlementAmountCurrency())
                .description(transfer.getDescription())
                .build();
        accounts.createCreditTransaction(transaction);

        return InstantTransaction.builder(transaction.getId())
                .amount(newMoney(
                        transfer.getSettlementAmount(),
                        transfer.getSettlementAmountCurrency()))
                .build();
    }

    /**
     * Credit customer account. We don't support beneficiary side FX at this
     * point, so simply debit settlement account and credit end user destination
     * account.
     *
     * {@inheritDoc}
     */
    @Override
    public void commitCreditTransaction(
            String transferId,
            String transactionId,
            BankAccount account,
            Money amount) {
        accounts.commitCreditTransaction(account, transferId, transactionId);
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
        accounts.rollbackCreditTransaction(account, transferId, transactionId);
    }
}

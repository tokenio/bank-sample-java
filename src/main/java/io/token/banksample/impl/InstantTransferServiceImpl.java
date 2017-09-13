package io.token.banksample.impl;

import static com.google.common.base.Preconditions.checkState;
import static io.token.proto.common.token.TokenProtos.TransferTokenStatus.FAILURE_DESTINATION_ACCOUNT_NOT_FOUND;
import static io.token.proto.common.token.TokenProtos.TransferTokenStatus.FAILURE_INVALID_CURRENCY;
import static io.token.sdk.util.ProtoFactory.newMoney;
import static java.lang.Double.parseDouble;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.token.banksample.model.AccountTransactionPair;
import io.token.banksample.model.AccountTransfer;
import io.token.banksample.model.Accounting;
import io.token.banksample.model.Pricing;
import io.token.proto.common.account.AccountProtos.BankAccount;
import io.token.proto.common.money.MoneyProtos.Money;
import io.token.sdk.api.Balance;
import io.token.sdk.api.InstantTransaction;
import io.token.sdk.api.InstantTransfer;
import io.token.sdk.api.PrepareTransferException;
import io.token.sdk.api.service.InstantTransferService;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private static final Logger logger = LoggerFactory.getLogger(InstantTransferServiceImpl.class);

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
        // Book FX deal.
        // TODO: redeem result needs to be recorded in the accounts change...
        // TODO: Use the right amount, need to take FX into account?
        pricing.redeemQuote(transfer.getPricing().getSourceQuote());
        // Put a hold on the account.
        AccountTransactionPair txPair = accounts.transfer(AccountTransfer.transfer()
                .withId(transfer.getTokenTransferId())
                .from(transfer.getAccount())
                .to(accounts.getHoldAccount(transfer.getTransactionAmountCurrency()))
                .withAmount(
                        transfer.getTransactionAmount().doubleValue(),
                        transfer.getTransactionAmountCurrency())
                .build());

        return InstantTransaction.builder(txPair.getDebit().getTransactionId())
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
        Optional<AccountTransfer> holdOptional = accounts.lookupTransfer(transactionId);
        if (!holdOptional.isPresent()) {
            throw new StatusRuntimeException(Status
                    .NOT_FOUND
                    .withDescription("Hold is not found for transaction id: " + transactionId));
        }

        AccountTransfer hold = holdOptional.get();
        checkState(parseDouble(amount.getValue()) == hold.getAmount());
        checkState(amount.getCurrency().equals(hold.getCurrency()));
        checkState(account.equals(hold.getFrom()));

        accounts.transfer(AccountTransfer.transfer()
                .withId(transferId)
                .from(hold.getTo())
                .to(accounts.getSettlementAccount(hold.getCurrency()))
                .withAmount(hold.getAmount(), hold.getCurrency())
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
        Optional<AccountTransfer> holdOptional = accounts.lookupTransfer(transactionId);
        if (!holdOptional.isPresent()) {
            throw new StatusRuntimeException(Status
                    .NOT_FOUND
                    .withDescription("Hold is not found for transaction id: " + transactionId));
        }

        AccountTransfer hold = holdOptional.get();
        checkState(parseDouble(amount.getValue()) == hold.getAmount());
        checkState(amount.getCurrency().equals(hold.getCurrency()));
        checkState(account.equals(hold.getFrom()));

        accounts.transfer(AccountTransfer.transfer()
                .withId(transferId)
                .from(hold.getTo())
                .to(hold.getFrom())
                .withAmount(hold.getAmount(), hold.getCurrency())
                .build());
    }

    /**
     * Credit leg of the instant transfer initiation. We don't want to do
     * anything here. Don't want to credit the customer account just yet,
     * the transaction has not cleared yet.
     *
     * <p>So all we do here is verify that account exists and that account
     * currency matches transfer currency, beneficiary side FX is not
     * supported at this point.
     *
     *
     * {@inheritDoc}
     */
    @Override
    public InstantTransaction beginCreditTransaction(InstantTransfer transfer) {
        Optional<Balance> balance = accounts.lookupBalance(transfer.getAccount());
        if (!balance.isPresent()) {
            throw new PrepareTransferException(
                    FAILURE_DESTINATION_ACCOUNT_NOT_FOUND,
                    "Account not found: " + transfer.getAccount());
        }

        if (!balance.get().getCurrency().equals(transfer.getTransactionAmountCurrency())) {
            throw new PrepareTransferException(
                    FAILURE_INVALID_CURRENCY,
                    "Credit side FX is not supported");
        }

        return InstantTransaction.builder(transfer.getTokenTransferId())
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
        accounts.transfer(AccountTransfer.transfer()
                .withId(transferId)
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
    }
}

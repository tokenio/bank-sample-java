package io.token.banksample.impl;

import static io.token.proto.common.account.AccountProtos.BankAccount.AccountCase.SWIFT;
import static io.token.proto.common.transaction.TransactionProtos.TransactionStatus.SUCCESS;
import static io.token.proto.common.transaction.TransactionProtos.TransactionType.DEBIT;
import static io.token.sdk.util.ProtoFactory.newMoney;
import static java.lang.String.join;

import io.token.proto.common.account.AccountProtos.BankAccount;
import io.token.proto.common.account.AccountProtos.BankAccount.AccountCase;
import io.token.proto.common.account.AccountProtos.BankAccount.Swift;
import io.token.proto.common.money.MoneyProtos.Money;
import io.token.proto.common.pricing.PricingProtos.FeeResponsibility;
import io.token.proto.common.pricing.PricingProtos.TransferQuote;
import io.token.proto.common.transaction.TransactionProtos.Transaction;
import io.token.proto.common.transferinstructions.TransferInstructionsProtos.PurposeOfPayment;
import io.token.proto.common.transferinstructions.TransferInstructionsProtos.TransferEndpoint;
import io.token.sdk.api.Transfer;
import io.token.sdk.api.TransferException;
import io.token.sdk.api.service.TransferService;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sample implementation of the {@link TransferService}. Returns fake data.
 */
public class TransferServiceImpl implements TransferService {
    private static final Logger logger = LoggerFactory.getLogger(TransferServiceImpl.class);

    @Override
    public Transaction transfer(Transfer transfer) throws TransferException {
        String transactionId = UUID.randomUUID().toString();
        logger.info("Example transaction id: {}", transactionId);

        String transferId = transfer.getTokenTransferId();
        logger.info("Token transfer id: {}", transferId);

        String tokenRefId = transfer.getTokenRefId();
        logger.info("token ref id: {}", tokenRefId);

        String transferRefId = transfer.getTransferRefId();
        logger.info("transfer ref id: {}", transferRefId);

        // INFO: The original transfer amount requested by the customer.
        Money requestedAmount = newMoney(
                transfer.getRequestedAmount(),
                transfer.getRequestedAmountCurrency());

        // A fee adjusted amount in local currency to be debited from the customer account.
        Money transactionAmount = newMoney(
                transfer.getTransactionAmount(),
                transfer.getTransactionAmountCurrency());

        // INFO: The pricing information used to perform amount calculations.
        TransferQuote pricingQuote = transfer.getQuote();
        FeeResponsibility responsibility = transfer.getFeeResponsibility();

        Swift remittierAccount = getSwiftAccount(transfer.getAccount());
        logger.info("Remitter Swift account: {}; BIC: {}",
                remittierAccount.getAccount(), remittierAccount.getBic());

        TransferEndpoint destination = findSwiftEndpoint(transfer.getDestinations());
        Swift beneficiaryAccount = getSwiftAccount(destination);
        logger.info(
                "Beneficiary Swift account: {}; BIC: {}; Legal Names: {}",
                beneficiaryAccount.getAccount(),
                beneficiaryAccount.getBic(),
                join(", ", destination.getCustomerData().getLegalNamesList()));

        PurposeOfPayment paymentPurpose = transfer.getPaymentPurpose();
        logger.info("Purpose of payment code: {}", paymentPurpose.name());

        return Transaction.newBuilder()
                .setId(transactionId)
                .setTokenTransferId(transferId)
                .setStatus(SUCCESS)
                .setType(DEBIT)
                .setAmount(transactionAmount)
                .setDescription(transfer.getDescription())
                .build();
    }

    private TransferEndpoint findSwiftEndpoint(List<TransferEndpoint> endpoints) {
        return endpoints.stream()
                .filter(endpoint -> endpoint.getAccount().getAccountCase() == SWIFT)
                .findAny()
                .orElseThrow(() -> new IllegalArgumentException("No supported destinations found"));
    }

    private Swift getSwiftAccount(TransferEndpoint transferEndpoint) {
        return getSwiftAccount(transferEndpoint.getAccount());
    }

    private Swift getSwiftAccount(BankAccount bankAccount) {
        AccountCase accountCase = bankAccount.getAccountCase();
        if (accountCase == SWIFT) {
            return bankAccount.getSwift();
        }
        throw new IllegalArgumentException("Unsupported Account Type: " + accountCase.name());
    }
}

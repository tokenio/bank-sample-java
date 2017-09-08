package com.example.demo.impl;

import static io.token.proto.common.account.AccountProtos.BankAccount.AccountCase.SWIFT;
import static io.token.proto.common.token.TokenProtos.TransferTokenStatus.FAILURE_DESTINATION_ACCOUNT_NOT_FOUND;
import static io.token.proto.common.token.TokenProtos.TransferTokenStatus.FAILURE_INSUFFICIENT_FUNDS;
import static io.token.proto.common.token.TokenProtos.TransferTokenStatus.FAILURE_INVALID_CURRENCY;
import static io.token.proto.common.transaction.TransactionProtos.TransactionStatus.PROCESSING;
import static io.token.proto.common.transaction.TransactionProtos.TransactionStatus.SUCCESS;
import static io.token.proto.common.transaction.TransactionProtos.TransactionType.CREDIT;
import static io.token.proto.common.transaction.TransactionProtos.TransactionType.DEBIT;
import static io.token.sdk.util.ProtoFactory.newMoney;
import static java.lang.String.join;
import static java.math.BigDecimal.ZERO;
import static java.time.Duration.ofDays;

import io.token.proto.common.account.AccountProtos.BankAccount;
import io.token.proto.common.account.AccountProtos.BankAccount.AccountCase;
import io.token.proto.common.account.AccountProtos.BankAccount.Swift;
import io.token.proto.common.address.AddressProtos.Address;
import io.token.proto.common.money.MoneyProtos.Money;
import io.token.proto.common.pricing.PricingProtos.FeeResponsibility;
import io.token.proto.common.pricing.PricingProtos.Pricing;
import io.token.proto.common.pricing.PricingProtos.TransferQuote;
import io.token.proto.common.pricing.PricingProtos.TransferQuote.Fee;
import io.token.proto.common.pricing.PricingProtos.TransferQuote.FxRate;
import io.token.proto.common.transaction.TransactionProtos.Transaction;
import io.token.proto.common.transaction.TransactionProtos.TransactionStatus;
import io.token.proto.common.transaction.TransactionProtos.TransactionType;
import io.token.proto.common.transferinstructions.TransferInstructionsProtos.CustomerData;
import io.token.proto.common.transferinstructions.TransferInstructionsProtos.PurposeOfPayment;
import io.token.proto.common.transferinstructions.TransferInstructionsProtos.TransferEndpoint;
import io.token.sdk.api.Balance;
import io.token.sdk.api.BankService;
import io.token.sdk.api.InstantTransaction;
import io.token.sdk.api.InstantTransfer;
import io.token.sdk.api.PrepareTransferException;
import io.token.sdk.api.Transfer;
import io.token.sdk.api.TransferException;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sample implementation of the {@link BankService}. Returns fake data.
 */
public class BankServiceImpl implements BankService {
    private static final Logger logger = LoggerFactory.getLogger(BankServiceImpl.class);
    private static final String BIC = "bic";
    private static final String ACCOUNT = "account";
    private static final BankAccount SETTLEMENT_ACCOUNT = newBankAccount(ACCOUNT, BIC);

    // account balances map: account number string -> centi-EUROS
    // A real bank probably has a db; this simple example just has in-memory lookup
    private static ConcurrentHashMap<String, Long> balance = new ConcurrentHashMap<>();
    {
        balance.put("TokenTest0001", 100l * 100);
        balance.put("TokenTest0002", 100l * 1000);
        balance.put("TokenTest0003", 100l * 10000);
    }

    // TODO: maybe static Concurrent is silly. Can I assume just one BankService?

    // map: tokenRefId -> TransferQuote
    // A real bank probably has a db; this simple example just has an
    // in-memory lookup
    private static ConcurrentHashMap<String, TransferQuote> storedCreditQuotes =
            new ConcurrentHashMap<>();

    // map: tokenRefId -> TransferQuote
    // A real bank probably has a db; this simple example just has an
    // in-memory lookup
    private static ConcurrentHashMap<String, TransferQuote> storedDebitQuotes =
            new ConcurrentHashMap<>();

    // A real bank has real FX rates; this simple example just has a static lookup table
    private static final HashMap<String, Double> FxFrom = new HashMap<String, Double>() {{
        put("EUR", 1.0);
        put("USD", 0.8);
        put("JPY", 0.0076);
    }};
    private static final HashMap<String, Double> FxTo = new HashMap<String, Double>() {{
        put("EUR", 1.0);
        put("USD", 1.18);
        put("JPY", 130.0);
    }};

    @Override
    public TransferQuote prepareCredit(
            String tokenRefId,
            BigDecimal amount,
            String currency,
            TransferEndpoint source,
            TransferEndpoint destination,
            PurposeOfPayment paymentPurpose,
            Optional<TransferQuote> creditQuote) { // TODO, what does this mean?
        if (storedCreditQuotes.containsKey(tokenRefId)) {
            return storedCreditQuotes.get(tokenRefId);
        }

        // TODO: existing sample assumed account types == Swift. OK assumption?

        Swift remitterAccount = getSwiftAccount(source);

        Swift beneficiaryAccount = getSwiftAccount(destination);
        if (!beneficiaryAccount.getBic().equals(BIC)) {
            throw new PrepareTransferException(
                    FAILURE_DESTINATION_ACCOUNT_NOT_FOUND,
                    "TODO: BIC == my bic, right?");
        }
        if (!balance.containsKey(beneficiaryAccount.getAccount())) {
            throw new PrepareTransferException(
                    FAILURE_DESTINATION_ACCOUNT_NOT_FOUND,
                    String.format(
                            "No such account %s",
                            beneficiaryAccount.getAccount()));
        }

        FxRate fxRate = null;
        // simple sample's accounts are all in EUR currency.
        // We support FX for a few other currencies; we reject most
        if (!currency.equals("EUR")) {
            if (FxFrom.containsKey(currency)) {
                fxRate = FxRate.newBuilder()
                        .setBaseCurrency(currency)
                        .setQuoteCurrency("EUR")
                        .setRate(FxFrom.get(currency).toString())
                        .build();
            } else {
                throw new PrepareTransferException(
                        FAILURE_INVALID_CURRENCY,
                        String.format("Invalid currency %s; only support EUR USD JPY", currency));
            }
        }

        String uniqueTransferQuoteId = UUID.randomUUID().toString();
        logger.info("Transfer Quote Id: {}", uniqueTransferQuoteId);

        logger.info("Purpose of payment code: {}", paymentPurpose.name());

        TransferQuote.Builder retvalBuilder = TransferQuote.newBuilder()
                .setId(uniqueTransferQuoteId)
                .setAccountCurrency(currency)
                .setFeesTotal("0.0")
                .setExpiresAtMs(Instant
                        .now()
                        .plus(Duration.ofDays(1))
                        .toEpochMilli());

        if (fxRate != null) {
            retvalBuilder.addRates(fxRate);
        }

        TransferQuote retval = retvalBuilder.build();

        storedCreditQuotes.put(tokenRefId, retval);

        return retval;
    }

    @Override
    public TransferQuote prepareDebit(
            String tokenRefId,
            BigDecimal amount,
            String currency,
            BankAccount source,
            TransferEndpoint destination,
            TransferQuote counterpartyQuote, // TODO realistic way to use this?
            PurposeOfPayment paymentPurpose,
            Optional<TransferQuote> debitQuote) { // TODO what does this mean?
        if (storedDebitQuotes.containsKey(tokenRefId)) {
            return storedDebitQuotes.get(tokenRefId);
        }

        Swift remitterAccount = getSwiftAccount(source);
        if (!remitterAccount.getBic().equals(BIC)) {
            throw new PrepareTransferException(
                    FAILURE_DESTINATION_ACCOUNT_NOT_FOUND,
                    "TODO: BIC == my bic, right?");
        }
        if (!balance.containsKey(remitterAccount.getAccount())) {
            throw new PrepareTransferException(
                    FAILURE_DESTINATION_ACCOUNT_NOT_FOUND,
                    String.format(
                            "No such account %s",
                            remitterAccount.getAccount()));
        }

        FxRate fxRate = null;
        BigDecimal necessaryBalance = amount.multiply(new BigDecimal(100));
        // simple sample's accounts are all in EUR currency.
        // We support FX for a few other currencies; we reject most
        if (!currency.equals("EUR")) {
            if (FxTo.containsKey(currency)) {
                fxRate = FxRate.newBuilder()
                        .setBaseCurrency("EUR")
                        .setQuoteCurrency(currency)
                        .setRate(FxTo.get(currency).toString())
                        .build();
                necessaryBalance = necessaryBalance.divide(new BigDecimal(FxTo.get(currency)));
            } else {
                throw new PrepareTransferException(
                        FAILURE_INVALID_CURRENCY,
                        String.format("Invalid currency %s; only support EUR USD JPY", currency));
            }
        }

        if (necessaryBalance.longValue() >= balance.get(remitterAccount.getAccount())) {
            throw new PrepareTransferException(
                    FAILURE_INSUFFICIENT_FUNDS,
                    String.format(
                            "Needs %s EUR but has just %s EUR",
                            necessaryBalance,
                            balance.get(remitterAccount.getAccount())));
        }

        Swift beneficiaryAccount = getSwiftAccount(destination);

        String uniqueTransferQuoteId = UUID.randomUUID().toString();

        TransferQuote.Builder retvalBuilder = TransferQuote.newBuilder()
                .setId(uniqueTransferQuoteId)
                .setAccountCurrency(currency)
                .setFeesTotal("0.25")
                .addFees(Fee.newBuilder()
                        .setAmount("0.17")
                        .setDescription("Transaction Fee")
                        .build())
                .addFees(Fee.newBuilder()
                        .setAmount("0.08")
                        .setDescription("Initiation Fee")
                        .build())
                .setExpiresAtMs(Instant
                        .now()
                        .plus(ofDays(1))
                        .toEpochMilli());

        if (fxRate != null) {
            retvalBuilder.addRates(fxRate);
        }

        TransferQuote retval = retvalBuilder.build();

        storedDebitQuotes.put(tokenRefId, retval);
        return retval;
    }

    /*
     *
     *  BELOW THIS LINE: haven't changed yet
     *
     */

    @Override
    public InstantTransaction beginDebitTransaction(InstantTransfer transfer)
            throws TransferException {
        String exampleTransactionId = UUID.randomUUID().toString();
        logger.info("Example transaction id: {}", exampleTransactionId);

        String transferId = transfer.getTokenTransferId();
        logger.info("Token transfer id: {}", transferId);

        String tokenRefId = transfer.getTokenRefId();
        logger.info("token ref id: {}", tokenRefId);

        String transferRefId = transfer.getTransferRefId();
        logger.info("transfer ref id: {}", transferRefId);

        PurposeOfPayment purposeOfPayment = transfer.getPurposeOfPayment();
        logger.info("Purpose of payment code: {}", purposeOfPayment.name());

        // A fee adjusted amount in local currency to be debited from the customer account.
        Money transactionAmount = newMoney(
                transfer.getTransactionAmount(),
                transfer.getTransactionAmountCurrency());

        // INFO: The original transfer amount requested by the customer.
        Money requestedAmount = newMoney(
                transfer.getRequestedAmount(),
                transfer.getRequestedAmountCurrency());

        // INFO: The settlement amount in either requested or counterparty currency
        // to be used for settlement with the beneficiary bank.
        Money settlementAmount = newMoney(
                transfer.getSettlementAmount(),
                transfer.getSettlementAmountCurrency());

        // INFO: The pricing information used to perform amount calculations.
        Pricing pricing = transfer.getPricing();
        pricing.getSourceQuote().getId();

        Transaction transaction = Transaction.newBuilder()
                .setId(exampleTransactionId)
                .setTokenTransferId(transferId)
                .setStatus(PROCESSING)
                .setType(DEBIT)
                .setAmount(transactionAmount)
                .setDescription(transfer.getDescription())
                .build();

        // INFO: A flag indicating that the transaction has been delayed due to sanctions check
        boolean delayed = false;

        return InstantTransaction.create(transaction, SETTLEMENT_ACCOUNT, delayed);
    }

    @Override
    public InstantTransaction beginCreditTransaction(InstantTransfer transfer)
            throws TransferException {
        String exampleTransactionId = UUID.randomUUID().toString();
        logger.info("Example transaction id: {}", exampleTransactionId);

        String transferId = transfer.getTokenTransferId();
        logger.info("Token transfer id: {}", transferId);

        String tokenRefId = transfer.getTokenRefId();
        logger.info("token ref id: {}", tokenRefId);

        String transferRefId = transfer.getTransferRefId();
        logger.info("transfer ref id: {}", transferRefId);

        PurposeOfPayment purposeOfPayment = transfer.getPurposeOfPayment();
        logger.info("Purpose of payment code: {}", purposeOfPayment.name());

        // A fee adjusted amount in local currency to be credited to the customer account.
        Money transactionAmount = newMoney(
                transfer.getTransactionAmount(),
                transfer.getTransactionAmountCurrency());

        // INFO: The original transfer amount requested by the customer.
        Money requestedAmount = newMoney(
                transfer.getRequestedAmount(),
                transfer.getRequestedAmountCurrency());

        // INFO: The settlement amount in either requested or local currency
        // received from the remitter bank.
        Money settlementAmount = newMoney(
                transfer.getSettlementAmount(),
                transfer.getSettlementAmountCurrency());

        // INFO: The pricing information used to perform amount calculations.
        Pricing pricing = transfer.getPricing();

        Transaction transaction = Transaction.newBuilder()
                .setId(UUID.randomUUID().toString())
                .setTokenTransferId(transferId)
                .setStatus(PROCESSING)
                .setType(CREDIT)
                .setAmount(transactionAmount)
                .setDescription(transfer.getDescription())
                .build();

        // INFO: A flag indicating that the transaction has been delayed due to sanctions check
        boolean delayed = false;

        return InstantTransaction.create(transaction, SETTLEMENT_ACCOUNT, delayed);
    }

    @Override
    public void commitTransaction(String transferId, String transactionId) {
        logger.info("Token transfer id: {}", transferId);
        logger.info("Example transaction id: {}", transactionId);
    }

    @Override
    public void rollbackTransaction(String transferId, String transactionId) {
        logger.info("Token transfer id: {}", transferId);
        logger.info("Example transaction id: {}", transactionId);
    }

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

    @Override
    public Balance getBalance(BankAccount account) {
        Swift customerAccount = getSwiftAccount(account);
        logger.info("Customer Swift account: {}; BIC: {}",
                customerAccount.getAccount(), customerAccount.getBic());

        return Balance.create("GBP", ZERO, ZERO);
    }

    @Override
    public Optional<Transaction> getTransaction(BankAccount account, String transactionId) {
        Swift customerAccount = getSwiftAccount(account);
        logger.info("Customer Swift account: {}; BIC: {}",
                customerAccount.getAccount(), customerAccount.getBic());
        logger.info("Example transaction id: {}", transactionId);
        logger.warn("Need to verify that the transaction belongs to the passed in account");

        Transaction.newBuilder()
                .setId("transaction_id")
                .setAmount(Money.newBuilder()
                        .setValue("10.0")
                        .setCurrency("EUR"))
                .setDescription("Description")
                .setType(TransactionType.DEBIT) // For debit leg, CREDIT for credit leg.
                .setStatus(TransactionStatus.SUCCESS) // For completed transaction, TransactionStatus.PROCESSING if the transaction has not been committed yet.
                .build();

        return Optional.empty();
    }

    @Override
    public List<Transaction> getTransactions(BankAccount account, int offset, int limit) {
        Swift customerAccount = getSwiftAccount(account);
        logger.info("Customer Swift account: {}; BIC: {}",
                customerAccount.getAccount(), customerAccount.getBic());

        return Collections.emptyList();
    }

    @Override
    public CustomerData getCustomerData(BankAccount bankAccount) {
        Swift customerAccount = getSwiftAccount(bankAccount);
        logger.info("Customer Swift account: {}; BIC: {}",
                customerAccount.getAccount(), customerAccount.getBic());

        return CustomerData.newBuilder()
                .addLegalNames("Customer Name")
                .setAddress(Address.newBuilder()
                        .setHouseNumber("845")
                        .setStreet("Market")
                        .setCity("San Francisco")
                        .setPostCode("94103")
                        .setState("CA")
                        .setCountry("US"))
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

    private static BankAccount newBankAccount(String account, String bic) {
        return BankAccount.newBuilder()
                .setSwift(Swift.newBuilder()
                        .setAccount(account)
                        .setBic(bic)
                        .build())
                .build();
    }
}

package io.token.banksample.model.impl;

import static io.token.proto.common.token.TokenProtos.TransferTokenStatus.FAILURE_INVALID_CURRENCY;
import static io.token.proto.common.token.TokenProtos.TransferTokenStatus.FAILURE_INVALID_QUOTE;
import static java.lang.String.format;

import io.token.banksample.model.Pricing;
import io.token.proto.common.pricing.PricingProtos.TransferQuote;
import io.token.proto.common.pricing.PricingProtos.TransferQuote.FxRate;
import io.token.sdk.api.PrepareTransferException;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Configuration based {@link Pricing} engine implementation.
 */
public final class PricingImpl implements Pricing {
    private final List<FxRate> rates;
    private final Map<String, TransferQuote> quotes;

    public PricingImpl(List<FxRate> rates) {
        this.rates = rates;
        this.quotes = new HashMap<>();
    }

    @Override
    public synchronized TransferQuote lookupQuote(String id) {
        return Optional
                .ofNullable(quotes.get(id))
                .orElseThrow(() -> new PrepareTransferException(
                        FAILURE_INVALID_QUOTE,
                        format("Price quote not found: %s", id)));
    }

    /**
     * Only remitter side FX is supported. Therefore, on the beneficiary
     * side we only apply fees.
     *
     * {@inheritDoc}
     */
    @Override
    public synchronized TransferQuote creditQuote(String baseCurrency, String quoteCurrency) {
        TransferQuote quote = TransferQuote.newBuilder()
                .setId(UUID.randomUUID().toString())
                .setAccountCurrency(quoteCurrency)
                .setFeesTotal("0")
                .setExpiresAtMs(Instant.now()
                        .plus(Duration.ofDays(1))
                        .toEpochMilli())
                .build();

        quotes.put(quote.getId(), quote);
        return quote;
    }

    /**
     * We perform FX on the remitter side.
     *
     * {@inheritDoc}
     */
    @Override
    public synchronized TransferQuote debitQuote(String baseCurrency, String quoteCurrency) {
        FxRate fxRate = rates.stream()
                .filter(r -> r.getBaseCurrency().equals(baseCurrency))
                .filter(r -> r.getQuoteCurrency().equals(quoteCurrency))
                .findFirst()
                .orElseThrow(() -> new PrepareTransferException(
                        FAILURE_INVALID_CURRENCY,
                        format("FX rate not found %s -> %s", baseCurrency, quoteCurrency)));

        TransferQuote quote = TransferQuote.newBuilder()
                .setId(UUID.randomUUID().toString())
                .setAccountCurrency(baseCurrency)
                .setFeesTotal("0")
                .addRates(fxRate)
                .setExpiresAtMs(Instant.now()
                        .plus(Duration.ofDays(1))
                        .toEpochMilli())
                .build();

        quotes.put(quote.getId(), quote);
        return quote;
    }

    @Override
    public synchronized void redeemQuote(TransferQuote quote) {
        TransferQuote lookedUp = quotes.get(quote.getId());
        if (!quote.equals(lookedUp)) {
            throw new PrepareTransferException(
                    FAILURE_INVALID_QUOTE,
                    format("Quote not found: %s", quote));
        }
    }
}

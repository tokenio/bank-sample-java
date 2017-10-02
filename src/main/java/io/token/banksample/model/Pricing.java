package io.token.banksample.model;

import io.token.proto.common.pricing.PricingProtos.TransferQuote;

import java.math.BigDecimal;

/**
 * Pricing engine abstraction.
 */
public interface Pricing {
    /**
     * Looks up a previously generated quote.
     *
     * @param id quote id
     * @return looked up quote
     * @throws io.token.sdk.api.PrepareTransferException if quote has not been
     *      found
     */
    TransferQuote lookupQuote(String id);

    /**
     * Generates a new pricing quote, credit side.
     *
     * @param baseCurrency base currency
     * @param quoteCurrency quote currency
     * @return newly created quote
     */
    TransferQuote creditQuote(String baseCurrency, String quoteCurrency);

    /**
     * Generates a new pricing quote, debit side.
     *
     * @param baseCurrency base currency
     * @param quoteCurrency quote currency
     * @return newly created quote
     */
    TransferQuote debitQuote(String baseCurrency, String quoteCurrency);

    /**
     * Redeems a previously issued pricing quote, booking the FX deal.
     *
     * @param quote previously generated quote
     */
    void redeemQuote(TransferQuote quote);

    /**
     * Look up an FX rate
     * @param baseCurrency 'EUR' in 'EUR/USD'
     * @param quoteCurrency 'USD' in 'EUR/USD'
     * @return rate
     */
    BigDecimal lookupFxRate(String baseCurrency, String quoteCurrency);
}

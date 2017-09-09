package io.token.banksample.model;

import io.token.proto.common.pricing.PricingProtos.TransferQuote;

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
     * Generates a new pricing quote.
     *
     * @param baseCurrency base currency
     * @param quoteCurrency quote currency
     * @return newly created quote
     */
    TransferQuote quote(String baseCurrency, String quoteCurrency);
}

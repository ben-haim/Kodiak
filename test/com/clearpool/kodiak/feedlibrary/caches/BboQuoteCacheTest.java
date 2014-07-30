package com.clearpool.kodiak.feedlibrary.caches;

import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.clearpool.kodiak.feedlibrary.core.MdFeed;
import com.clearpool.messageobjects.marketdata.Exchange;
import com.clearpool.messageobjects.marketdata.Quote;

public class BboQuoteCacheTest
{
	private BboQuoteCache bboQuoteCache;

	@Before
	public void setUp()
	{
		this.bboQuoteCache = new BboQuoteCache(null, MdFeed.CQS, "[A-Z]", 1);
	}

	@Test
	public void testSymbolSequenceNumber1MarketCenter()
	{
		Map<Exchange, Quote> bboQuotes = null;
		Quote bboQuote = null;

		// Process first quote
		this.bboQuoteCache.updateBidAndOffer("TEST", Exchange.USEQ_BATS_EXCHANGE, 10.0, 100, 11.0, 100, System.currentTimeMillis(), 0);
		bboQuotes = this.bboQuoteCache.getData("TEST");
		Assert.assertNotNull(bboQuotes);
		bboQuote = bboQuotes.get(Exchange.USEQ_BATS_EXCHANGE);
		Assert.assertNotNull(bboQuote);
		Assert.assertEquals(1, bboQuote.getSymbolSequenceNumber());

		// Process second quote
		this.bboQuoteCache.updateBidAndOffer("TEST", Exchange.USEQ_BATS_EXCHANGE, 10.0, 200, 11.0, 200, System.currentTimeMillis(), 0);
		bboQuotes = this.bboQuoteCache.getData("TEST");
		Assert.assertNotNull(bboQuotes);
		bboQuote = bboQuotes.get(Exchange.USEQ_BATS_EXCHANGE);
		Assert.assertNotNull(bboQuote);
		Assert.assertEquals(2, bboQuote.getSymbolSequenceNumber());
	}

	@Test
	public void testSymbolSequenceNumber2MarketCenters()
	{
		Map<Exchange, Quote> bboQuotes = null;
		Quote bboQuote = null;

		// Process first quote
		this.bboQuoteCache.updateBidAndOffer("TEST", Exchange.USEQ_BATS_EXCHANGE, 10.0, 100, 11.0, 100, System.currentTimeMillis(), 0);
		bboQuotes = this.bboQuoteCache.getData("TEST");
		Assert.assertNotNull(bboQuotes);
		bboQuote = bboQuotes.get(Exchange.USEQ_BATS_EXCHANGE);
		Assert.assertNotNull(bboQuote);
		Assert.assertEquals(1, bboQuote.getSymbolSequenceNumber());

		// Process second quote
		this.bboQuoteCache.updateBidAndOffer("TEST", Exchange.USEQ_EDGA_EXCHANGE, 10.0, 200, 11.0, 200, System.currentTimeMillis(), 0);
		bboQuotes = this.bboQuoteCache.getData("TEST");
		Assert.assertNotNull(bboQuotes);
		Assert.assertEquals(2, bboQuotes.size());
		bboQuote = bboQuotes.get(Exchange.USEQ_EDGA_EXCHANGE);
		Assert.assertNotNull(bboQuote);
		Assert.assertEquals(2, bboQuote.getSymbolSequenceNumber());
	}
}

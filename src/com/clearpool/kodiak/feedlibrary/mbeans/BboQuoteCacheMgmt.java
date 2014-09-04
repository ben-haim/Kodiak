package com.clearpool.kodiak.feedlibrary.mbeans;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import com.clearpool.kodiak.feedlibrary.caches.BboQuoteCache;
import com.clearpool.kodiak.feedlibrary.mbeans.format.MdEntityHtmlFormatter;
import com.clearpool.messageobjects.marketdata.Exchange;
import com.clearpool.messageobjects.marketdata.Quote;

public class BboQuoteCacheMgmt implements BboQuoteCacheMgmtMBean
{
	private final BboQuoteCache bboQuoteCache;

	public BboQuoteCacheMgmt(BboQuoteCache bboQuoteCache)
	{
		this.bboQuoteCache = bboQuoteCache;
	}

	@Override
	public String getMdService()
	{
		return String.valueOf(this.bboQuoteCache.getMdServiceType());
	}

	@Override
	public String[] getAllSymbols()
	{
		String[] symbols = this.bboQuoteCache.getAllSymbols();
		Arrays.sort(symbols);
		return symbols;
	}

	@Override
	public String getData(String symbol)
	{
		Map<Exchange, Quote> quotes = this.bboQuoteCache.getData(symbol);
		if (quotes == null) return String.valueOf(quotes);
		StringBuilder builder = new StringBuilder();
		for (Quote quote : quotes.values())
		{
			builder.append(quote.toString());
			builder.append("<br>");
		}
		return builder.toString();
	}

	@Override
	public String getFormattedData(String symbol)
	{
		Map<Exchange, Quote> quotes = this.bboQuoteCache.getData(symbol);
		if (quotes == null) return String.valueOf(quotes);
		return MdEntityHtmlFormatter.formatBbos(quotes.values());
	}

	@Override
	public String publishAllData()
	{
		Collection<String> publishedSymbols = this.bboQuoteCache.publishAllData();
		return String.valueOf(publishedSymbols);
	}
}

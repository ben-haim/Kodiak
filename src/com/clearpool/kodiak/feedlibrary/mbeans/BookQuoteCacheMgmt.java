package com.clearpool.kodiak.feedlibrary.mbeans;

import java.util.Arrays;
import java.util.List;

import com.clearpool.common.datastractures.Pair;
import com.clearpool.kodiak.feedlibrary.caches.BookQuoteCache;
import com.clearpool.kodiak.feedlibrary.mbeans.format.MdEntityHtmlFormatter;
import com.clearpool.messageobjects.marketdata.BookQuote;

public class BookQuoteCacheMgmt implements BookQuoteCacheMgmtMBean
{
	private final BookQuoteCache bookQuoteCache;

	public BookQuoteCacheMgmt(BookQuoteCache bookQuoteCache)
	{
		this.bookQuoteCache = bookQuoteCache;
	}

	@Override
	public String getMdService()
	{
		return String.valueOf(this.bookQuoteCache.getMdServiceType());
	}

	@Override
	public String[] getAllSymbols()
	{
		String[] symbols = this.bookQuoteCache.getAllSymbols();
		Arrays.sort(symbols);
		return symbols;
	}

	@Override
	public String getData(String symbol)
	{
		Pair<List<BookQuote>, List<BookQuote>> pair = this.bookQuoteCache.getData(symbol);
		List<BookQuote> bids = pair.getA();
		List<BookQuote> asks = pair.getB();

		if (bids.size() == 0 && asks.size() == 0) return "null";

		StringBuilder builder = new StringBuilder();
		if (bids.size() > 0)
		{
			builder.append("Bids:").append("<br>");
			for (BookQuote bookQuote : bids)
			{
				builder.append(bookQuote.toString());
				builder.append("<br>");
			}
		}

		if (asks.size() > 0)
		{
			builder.append("Asks:").append("<br>");
			for (BookQuote bookQuote : asks)
			{
				builder.append(bookQuote.toString());
				builder.append("<br>");
			}
		}

		return builder.toString();
	}

	@Override
	public String getFormattedData(String symbol)
	{
		Pair<List<BookQuote>, List<BookQuote>> pair = this.bookQuoteCache.getData(symbol);
		List<BookQuote> bids = pair.getA();
		List<BookQuote> asks = pair.getB();

		if (bids.size() == 0 && asks.size() == 0) return "null";

		return MdEntityHtmlFormatter.formatBookQuote(bids, asks);
	}
}

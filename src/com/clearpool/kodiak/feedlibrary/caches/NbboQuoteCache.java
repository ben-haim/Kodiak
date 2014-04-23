package com.clearpool.kodiak.feedlibrary.caches;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.clearpool.common.symbology.ISymbolConverter;
import com.clearpool.kodiak.feedlibrary.callbacks.IMdQuoteListener;
import com.clearpool.kodiak.feedlibrary.core.MdFeed;
import com.clearpool.kodiak.feedlibrary.utils.symbolconverters.SymbolConverterFactory;
import com.clearpool.messageobjects.marketdata.Exchange;
import com.clearpool.messageobjects.marketdata.MdEntity;
import com.clearpool.messageobjects.marketdata.MdServiceType;
import com.clearpool.messageobjects.marketdata.Quote;

public class NbboQuoteCache implements IMdServiceCache
{
	private final IMdQuoteListener quoteListener;
	private final MdFeed feedType;
	private final String range;
	private final MdServiceType mdServiceType;
	private final Map<String, Quote> quotes;

	public NbboQuoteCache(IMdQuoteListener quoteListener, MdFeed feedType, String range)
	{
		this.quoteListener = quoteListener;
		this.feedType = feedType;
		this.range = range;
		this.mdServiceType = MdServiceType.NBBO;
		this.quotes = new HashMap<>();
	}

	@Override
	public MdServiceType getMdServiceType()
	{
		return this.mdServiceType;
	}

	public void updateBidAndOffer(String symbol, double bidPrice, int bidSize, Exchange bidExchange, double askPrice, int askSize, Exchange askExchange, long timestamp,
			char condition)
	{
		Quote quote = this.quotes.get(symbol);
		if (quote == null)
		{
			quote = this.createQuote(symbol);
			this.quotes.put(symbol, quote);
		}

		quote.setBidPrice(bidPrice);
		quote.setBidSize(bidSize);
		quote.setBidExchange(bidExchange);
		quote.setAskPrice(askPrice);
		quote.setAskSize(askSize);
		quote.setAskExchange(askExchange);
		quote.setTimestamp(timestamp);
		quote.setCondition(condition);
		quote.setConditionCode(0);
		sendQuote(quote);
	}

	public void updateBid(String symbol, double bidPrice, int bidSize, Exchange bidExchange, long timestamp, char condition)
	{
		Quote quote = this.quotes.get(symbol);
		if (quote == null)
		{
			quote = this.createQuote(symbol);
			this.quotes.put(symbol, quote);
		}

		quote.setBidPrice(bidPrice);
		quote.setBidSize(bidSize);
		quote.setBidExchange(bidExchange);
		quote.setTimestamp(timestamp);
		quote.setCondition(condition);
		quote.setConditionCode(0);
		sendQuote(quote);
	}

	public void updateOffer(String symbol, double askPrice, int askSize, Exchange askExchange, long timestamp, char condition)
	{
		Quote quote = this.quotes.get(symbol);
		if (quote == null)
		{
			quote = this.createQuote(symbol);
			this.quotes.put(symbol, quote);
		}

		quote.setAskPrice(askPrice);
		quote.setAskSize(askSize);
		quote.setAskExchange(askExchange);
		quote.setTimestamp(timestamp);
		quote.setCondition(condition);
		quote.setConditionCode(0);
		sendQuote(quote);
	}

	private void sendQuote(Quote quote)
	{
		quote.setMdTimestamp(System.currentTimeMillis());
		quote.setConditionCode(MdEntity.setCondition(quote.getConditionCode(), MdEntity.CONDITION_FRESH));
		quote.setSymbolSequenceNumber(quote.getSymbolSequenceNumber() + 1);

		if (this.quoteListener != null)
		{
			quote = quote.clone();
			this.quoteListener.quoteReceived(quote);
		}
	}

	private Quote createQuote(String symbol)
	{
		Quote quote = new Quote();
		quote.setServiceType(MdServiceType.NBBO);
		ISymbolConverter symbolConverter = SymbolConverterFactory.getConverterInstance(this.feedType);
		if (symbolConverter != null) quote.setSymbol(symbolConverter.convert(symbol));
		else quote.setSymbol(symbol);
		return quote;
	}

	@Override
	public String[] getAllSymbols()
	{
		return this.quotes.keySet().toArray(new String[0]);
	}

	@Override
	public Quote getData(String symbol)
	{
		return this.quotes.get(symbol);
	}

	@Override
	public String getRange()
	{
		return this.range;
	}

	@Override
	public Collection<String> publishAllData()
	{
		for (Quote quote : this.quotes.values())
		{
			if (this.quoteListener != null)
			{
				quote = quote.clone();
				this.quoteListener.quoteReceived(quote);
			}
		}
		return this.quotes.keySet();
	}
}

package com.clearpool.kodiak.feedlibrary.mbeans;

import java.util.Arrays;
import java.util.Collection;

import com.clearpool.kodiak.feedlibrary.caches.NbboQuoteCache;
import com.clearpool.kodiak.feedlibrary.mbeans.format.AnnotatedMBean;
import com.clearpool.kodiak.feedlibrary.mbeans.format.MdEntityHtmlFormatter;
import com.clearpool.messageobjects.marketdata.Quote;

public class NbboQuoteCacheMgmt extends AnnotatedMBean<NbboQuoteCacheMgmtMBean> implements NbboQuoteCacheMgmtMBean
{
	private final NbboQuoteCache nbboQuoteCache;

	public NbboQuoteCacheMgmt(NbboQuoteCache bboQuoteCache)
	{
		super(NbboQuoteCacheMgmtMBean.class);
		this.nbboQuoteCache = bboQuoteCache;
	}

	@Override
	public String getMdService()
	{
		return String.valueOf(this.nbboQuoteCache.getMdServiceType());
	}

	@Override
	public String[] getAllSymbols()
	{
		String[] symbols = this.nbboQuoteCache.getAllSymbols();
		Arrays.sort(symbols);
		return symbols;
	}

	@Override
	public String getData(String symbol)
	{
		return String.valueOf(this.nbboQuoteCache.getData(symbol));
	}

	@Override
	public String getFormattedData(String symbol)
	{
		Quote quote = this.nbboQuoteCache.getData(symbol);
		if (quote == null) return String.valueOf(quote);
		return MdEntityHtmlFormatter.formatNbbo(quote);
	}

	@Override
	public String publishAllData()
	{
		Collection<String> publishedSymbols = this.nbboQuoteCache.publishAllData();
		return String.valueOf(publishedSymbols);
	}
}

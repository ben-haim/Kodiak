package com.clearpool.kodiak.feedlibrary.mbeans;

import java.util.Arrays;
import java.util.Collection;

import com.clearpool.commonserver.mbean.MBeanMethodDescription;
import com.clearpool.commonserver.mbean.ParameterDescription;
import com.clearpool.commonserver.mbean.marketdata.MdEntityHtmlFormatter;
import com.clearpool.kodiak.feedlibrary.caches.ImbalanceCache;
import com.clearpool.messageobjects.marketdata.Imbalance;

public class ImbalanceCacheMgmt implements ImbalanceCacheMgmtMBean
{
	private ImbalanceCache imbalanceCache;

	public ImbalanceCacheMgmt(ImbalanceCache imbalanceCache)
	{
		this.imbalanceCache = imbalanceCache;
	}

	@Override
	public String getMdService()
	{
		return String.valueOf(this.imbalanceCache.getMdServiceType());
	}

	@Override
	@MBeanMethodDescription("Get all symbols managed by permanent cache")
	public String[] getAllSymbols()
	{
		String[] symbols = this.imbalanceCache.getAllSymbols();
		Arrays.sort(symbols);
		return symbols;
	}

	@Override
	@MBeanMethodDescription("Retrieves market data given symbol")
	public String getData(@ParameterDescription("Symbol") String symbol)
	{
		return String.valueOf(this.imbalanceCache.getData(symbol));
	}

	@Override
	@MBeanMethodDescription("Retrieves formatted market data given symbol")
	public String getFormattedData(@ParameterDescription("Symbol") String symbol)
	{
		Imbalance imbalance = this.imbalanceCache.getData(symbol);
		if (imbalance == null) return String.valueOf(imbalance);
		return MdEntityHtmlFormatter.formatImbalance(imbalance);
	}

	@Override
	public String publishAllData()
	{
		Collection<String> publishedSymbols = this.imbalanceCache.publishAllData();
		return String.valueOf(publishedSymbols);
	}
}
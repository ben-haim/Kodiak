package com.clearpool.kodiak.feedlibrary.mbeans;

import java.util.Arrays;
import java.util.Collection;

import com.clearpool.kodiak.feedlibrary.caches.SaleCache;
import com.clearpool.kodiak.feedlibrary.mbeans.format.MdEntityHtmlFormatter;
import com.clearpool.messageobjects.marketdata.Sale;

public class SaleCacheMgmt implements SaleCacheMgmtMBean
{
	private final SaleCache saleCache;

	public SaleCacheMgmt(SaleCache saleCache)
	{
		this.saleCache = saleCache;
	}

	@Override
	public String getMdService()
	{
		return String.valueOf(this.saleCache.getMdServiceType());
	}

	@Override
	public String[] getAllSymbols()
	{
		String[] symbols = this.saleCache.getAllSymbols();
		Arrays.sort(symbols);
		return symbols;
	}

	@Override
	public String getData(String symbol)
	{
		return String.valueOf(this.saleCache.getData(symbol));
	}

	@Override
	public String getFormattedData(String symbol)
	{
		Sale sale = this.saleCache.getData(symbol);
		if (sale == null) return String.valueOf(sale);
		return MdEntityHtmlFormatter.formatSale(sale);
	}

	@Override
	public String publishAllData()
	{
		Collection<String> publishedSymbols = this.saleCache.publishAllData();
		return String.valueOf(publishedSymbols);
	}
}

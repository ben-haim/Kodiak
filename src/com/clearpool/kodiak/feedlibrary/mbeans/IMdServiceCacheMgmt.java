package com.clearpool.kodiak.feedlibrary.mbeans;

import com.clearpool.kodiak.feedlibrary.mbeans.format.MBeanMethodDescription;
import com.clearpool.kodiak.feedlibrary.mbeans.format.ParameterDescription;

public interface IMdServiceCacheMgmt
{
	public String getMdService();

	@MBeanMethodDescription("Get all symbols managed by permanent cache")
	public String[] getAllSymbols();

	@MBeanMethodDescription("Retrieves market data given symbol")
	public String getData(@ParameterDescription("Symbol") String symbol);

	@MBeanMethodDescription("Retrieves formatted market data given symbol")
	public String getFormattedData(@ParameterDescription("Symbol") String symbol);
}

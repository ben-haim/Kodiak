package com.clearpool.kodiak.feedlibrary.mbeans;

import com.clearpool.commonserver.mbean.MBeanMethodDescription;
import com.clearpool.commonserver.mbean.ParameterDescription;

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

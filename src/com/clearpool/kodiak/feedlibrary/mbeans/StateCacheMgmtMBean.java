package com.clearpool.kodiak.feedlibrary.mbeans;

import com.clearpool.commonserver.mbean.MBeanMethodDescription;
import com.clearpool.commonserver.mbean.ParameterDescription;

public interface StateCacheMgmtMBean extends IMdServiceCacheMgmt
{
	@MBeanMethodDescription("Sets market session given symbol and market session")
	public String setMarketSession(@ParameterDescription("symbol") String symbol, @ParameterDescription("primaryListing") char primaryListing,
			@ParameterDescription("isPrimaryListing") boolean isPrimaryListing, @ParameterDescription("marketSession") String marketSession);

	@MBeanMethodDescription("Sets trading state given symbol and trading state")
	public String setTradingState(@ParameterDescription("symbol") String symbol, @ParameterDescription("primaryListing") char primaryListing,
			@ParameterDescription("isPrimaryListing") boolean isPrimaryListing, @ParameterDescription("tradingState") String tradingState);

	@MBeanMethodDescription("Sets condition code given symbol, and conditionCode")
	public String setConditionCode(@ParameterDescription("symbol") String symbol, @ParameterDescription("primaryListing") char primaryListing,
			@ParameterDescription("isPrimaryListing") boolean isPrimaryListing, @ParameterDescription("conditionCode") String conditionCode);

	@MBeanMethodDescription("Sets market session all symbols in cache")
	public String setAllMarketSessions(@ParameterDescription("marketSession") String marketSession);

	@MBeanMethodDescription("Republish all data for all symbols in cache")
	public String publishAllData();
}

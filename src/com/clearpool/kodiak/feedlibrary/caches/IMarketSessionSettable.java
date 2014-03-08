package com.clearpool.kodiak.feedlibrary.caches;

import com.clearpool.messageobjects.marketdata.MarketSession;

public interface IMarketSessionSettable
{
	public MarketSession getMarketSession(String symbol, long timestamp);
}

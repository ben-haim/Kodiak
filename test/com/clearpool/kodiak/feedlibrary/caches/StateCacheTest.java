package com.clearpool.kodiak.feedlibrary.caches;

import org.junit.Assert;
import org.junit.Test;

import com.clearpool.messageobjects.marketdata.MarketState;
import com.clearpool.messageobjects.marketdata.MdServiceType;

@SuppressWarnings("static-method")
public class StateCacheTest
{

	@Test
	public void testStateCaching()
	{
		StateCache cache = new StateCache(null, null, null, null);
		MarketState state1 = new MarketState();
		state1.setServiceType(MdServiceType.STATE);
		state1.setSymbol("AAPL");
		state1.setLowerBand(434.0);
		cache.update(state1);

		Assert.assertEquals("AAPL state should be cached", 434.0, cache.getData("AAPL").getLowerBand(), 0);

		MarketState state2 = new MarketState();
		state2.setServiceType(MdServiceType.STATE);
		state2.setSymbol("AAPL");
		state2.setLowerBand(435.0);
		cache.update(state2);

		Assert.assertEquals("Second AAPL sale should override first one", 435.0, cache.getData("AAPL").getLowerBand(), 0);
	}
}

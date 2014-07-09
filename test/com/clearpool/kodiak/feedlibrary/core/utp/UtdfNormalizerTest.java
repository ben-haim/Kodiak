package com.clearpool.kodiak.feedlibrary.core.utp;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import com.clearpool.kodiak.feedlibrary.caches.SaleCache;
import com.clearpool.kodiak.feedlibrary.callbacks.IMdLibraryCallback;
import com.clearpool.kodiak.feedlibrary.core.MdFeed;
import com.clearpool.kodiak.feedlibrary.core.MdFeedProps;
import com.clearpool.messageobjects.marketdata.MdServiceType;


public class UtdfNormalizerTest
{
	
	private UtdfNormalizer normalizer;

	@Before
	public void setUp()
	{
		HashMap<String, Double> closePrices = new HashMap<String, Double>();
		closePrices.put("Syed", new Double(100.12));
		closePrices.put("Saad", new Double(99.12));
		MdFeedProps.putInstanceProperty(closePrices, MdFeed.UTDF.toString(), "CLOSEPRICES");
		Map<MdServiceType, IMdLibraryCallback> callbacks = new HashMap<MdServiceType, IMdLibraryCallback>();
		this.normalizer = new UtdfNormalizer(callbacks, "S-T",0);
	}
	
	@SuppressWarnings({ "unchecked", "unchecked" })
	@Test
	public void testClosePrices() {
		SaleCache cache = this.normalizer.getSalesCache();
		assertEquals(cache.getData("Syed").getLatestClosePrice(), 100.12, 1e-15);
		assertEquals(cache.getData("Saad").getLatestClosePrice(), 99.12, 1e-15);
	}
}

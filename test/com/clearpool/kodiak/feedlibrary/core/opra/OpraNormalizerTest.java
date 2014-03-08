package com.clearpool.kodiak.feedlibrary.core.opra;

import org.junit.Assert;
import org.junit.Test;

import com.clearpool.kodiak.feedlibrary.core.opra.OpraNormalizer;

@SuppressWarnings("static-method")
public class OpraNormalizerTest
{
	@Test
	public void testGetPrice()
	{
		Assert.assertEquals(1d, OpraNormalizer.getPrice(10l, 'A'), 0);
		Assert.assertEquals(.1d, OpraNormalizer.getPrice(10l, 'B'), 0);
		Assert.assertEquals(.01d, OpraNormalizer.getPrice(10l, 'C'), 0);
		Assert.assertEquals(.001d, OpraNormalizer.getPrice(10l, 'D'), 0);
		Assert.assertEquals(.0001d, OpraNormalizer.getPrice(10l, 'E'), 0);
		Assert.assertEquals(.00001d, OpraNormalizer.getPrice(10l, 'F'), 0);
		Assert.assertEquals(.000001d, OpraNormalizer.getPrice(10l, 'G'), 0);
		Assert.assertEquals(.0000001d, OpraNormalizer.getPrice(10l, 'H'), 0);
		Assert.assertEquals(10d, OpraNormalizer.getPrice(10l, 'I'), 0);
	}
}
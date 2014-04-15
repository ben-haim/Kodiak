package com.clearpool.kodiak.feedlibrary.core.cta;

import static org.junit.Assert.assertEquals;

import java.math.BigDecimal;
import java.util.Random;

import org.junit.Test;

public class CtaUtilsTest
{
	@SuppressWarnings("static-method")
	@Test
	public void testGetPrice()
	{
		Random r = new Random();
		for (int i = 0; i < 10000000; ++i)
		{
			long numerator = r.nextInt(2000000000);
			assertEquals(new BigDecimal(numerator).divide(new BigDecimal(100l)).doubleValue(), CtaUtils.getPrice(numerator, 'B'), 0.000000000000000001);
		}
	}
}

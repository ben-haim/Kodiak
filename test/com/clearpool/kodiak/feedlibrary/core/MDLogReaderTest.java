package com.clearpool.kodiak.feedlibrary.core;

import org.junit.Assert;
import org.junit.Test;

@SuppressWarnings("static-method")
public class MDLogReaderTest
{
	@Test
	public void testLength()
	{
		int length = 390;
		int firstByte = (byte) length;
		int secondByte = (byte) (length >> 8);
		Assert.assertEquals(-122, firstByte);
		Assert.assertEquals(1, secondByte);

		// Before applying 0xFF
		Assert.assertTrue(length != (secondByte << 8) + firstByte);

		// After applying 0xFF
		int postFirstByte = firstByte & 0xFF;
		int postSecondbyte = secondByte & 0xFF;
		int postLength = (postSecondbyte << 8) + postFirstByte;
		Assert.assertTrue(length == postLength);
	}
}
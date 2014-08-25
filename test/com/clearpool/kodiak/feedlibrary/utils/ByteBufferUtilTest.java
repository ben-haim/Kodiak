package com.clearpool.kodiak.feedlibrary.utils;

import static org.junit.Assert.assertEquals;

import java.nio.ByteBuffer;
import java.util.Date;

import org.junit.Test;

import com.clearpool.common.util.DateUtil;

public class ByteBufferUtilTest
{

	@SuppressWarnings("static-method")
	@Test
	public void testGetUnsignedLong()
	{
		ByteBuffer buffer = ByteBuffer.allocate(8);
		long nanosSinceMidnight = (new Date().getTime() - DateUtil.TODAY_MIDNIGHT_EST.getTime()) * DateUtil.NANOS_PER_MILLISECOND;
		buffer.putLong(nanosSinceMidnight);
		buffer.flip();
		ByteBufferUtil.advancePosition(buffer, 2);
		assertEquals(nanosSinceMidnight, ByteBufferUtil.getUnsignedLong(buffer, 6));
	}
}
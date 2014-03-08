package com.clearpool.kodiak.feedlibrary.core;

import java.nio.ByteBuffer;

import org.junit.Assert;

import org.junit.Test;

@SuppressWarnings("static-method")
public class MdPacketTest
{
	@Test
	public void testUnsignedToSigned()
	{
		int unsigned = 64000;
		byte[] bytes = new byte[2];
		ByteBuffer buffer = ByteBuffer.wrap(bytes);
		buffer.putShort((short) unsigned);
		buffer.rewind();

		short valueRead = buffer.getShort();

		// Value before 0xFFFF
		Assert.assertTrue(unsigned != valueRead);

		// Value after 0xFFFF
		int valueReadAsUnsigned = (valueRead & 0xFFFF);
		Assert.assertTrue(64000 == valueReadAsUnsigned);
	}
}
package com.clearpool.kodiak.feedlibrary.utils;

import java.nio.ByteBuffer;

public class ByteBufferUtil
{
	public static void advancePosition(ByteBuffer buffer, int count)
	{
		buffer.position(buffer.position() + count);
	}

	public static short getUnsignedByte(ByteBuffer buffer)
	{
		return (short) (buffer.get() & 0xFF);
	}

	public static int getUnsignedShort(ByteBuffer buffer)
	{
		return buffer.getShort() & 0xFFFF;
	}

	public static long getUnsignedInt(ByteBuffer buffer)
	{
		return buffer.getInt() & 0xFFFFFFFF;
	}

	public static String getString(ByteBuffer buffer, int length)
	{
		byte[] bytes = new byte[length];
		for (int i = 0; i < length; i++)
		{
			bytes[i] = buffer.get();
		}
		return new String(bytes);
	}

	public static long readAsciiLong(ByteBuffer buffer, int length)
	{
		long val = 0;
		for (int i = 0; i < length; i++)
		{
			int charVal = buffer.get() - '0';
			val = val * 10 + charVal;
		}
		return val;
	}
}

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

	public static void putChar(ByteBuffer buffer, char character)
	{
		buffer.put((byte) character);
	}

	/* Strings are left justified with 0x20 */
	public static void putString(ByteBuffer buffer, String value, int length)
	{
		int numSpaces = length - value.length();
		if (numSpaces >= 0)
		{
			byte[] bytes = value.getBytes();
			buffer.put(bytes, 0, bytes.length);
			for (int n = 0; n < numSpaces; n++)
			{
				putChar(buffer, ' ');
			}
		}
	}

	/* Numbers are right justified with 0x30 */
	public static void putLong(ByteBuffer buffer, long value, int length)
	{
		byte[] bytes = new byte[length];
		for (int n = 0; n < length; n++)
		{
			bytes[length - n - 1] = (byte) ('0' + (value % 10));
			value = value / 10;
		}
		buffer.put(bytes);
	}

}

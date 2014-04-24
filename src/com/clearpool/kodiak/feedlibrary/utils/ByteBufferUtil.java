package com.clearpool.kodiak.feedlibrary.utils;

import java.nio.ByteBuffer;

public class ByteBufferUtil
{
	private static final byte SPACE = 32;

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

	public static String getString(ByteBuffer buffer, byte[] bytes)
	{
		buffer.get(bytes);
		return new String(bytes).trim().intern();
	}

	public static String getUnboundedString(ByteBuffer buffer, int length)
	{
		byte[] bytes = new byte[length];
		buffer.get(bytes);
		return new String(bytes).trim().intern();
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
			byte[] newBytes = value.getBytes();
			buffer.put(newBytes, 0, newBytes.length);
			for (int n = 0; n < numSpaces; n++)
			{
				buffer.put(SPACE);
			}
		}
	}

	/* Numbers are right justified with 0x30 */
	public static void putLong(ByteBuffer buffer, long value, int length)
	{
		byte[] newBytes = new byte[length];
		for (int n = 0; n < length; n++)
		{
			newBytes[length - n - 1] = (byte) ('0' + (value % 10));
			value = value / 10;
		}
		buffer.put(newBytes);
	}

}

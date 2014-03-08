package com.clearpool.kodiak.feedlibrary.utils;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MdLogReader implements Iterator<byte[]>
{
	private static final Logger LOGGER = Logger.getLogger(MdLogReader.class.getName());

	private final BufferedInputStream stream;

	public MdLogReader(String file) throws FileNotFoundException
	{
		this.stream = new BufferedInputStream(new FileInputStream(file));
	}

	@Override
	public boolean hasNext()
	{
		try
		{
			return this.stream.available() > 0;
		}
		catch (IOException e)
		{
			LOGGER.log(Level.SEVERE, e.getMessage(), e);
		}
		return false;
	}

	@Override
	public byte[] next()
	{
		try
		{
			int firstByte = this.stream.read() & 0xFF;
			int secondByte = this.stream.read() & 0xFF;
			int length = (secondByte << 8) + firstByte;
			byte[] bytes = new byte[length];
			this.stream.read(bytes);
			return bytes;
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, e.getMessage(), e);
		}
		return null;
	}

	@Override
	public void remove()
	{

	}

	public static void main(String[] args) throws FileNotFoundException
	{
		MdLogReader reader = new MdLogReader("Z:\\NASDAQ#1");

		// Breakdown fields
		boolean CAPTURE_BREAKDOWN = true;
		long minute = 0;
		int minuteCount = 0;
		int[] minuteBreakdown = new int[60];

		// Gap fields
		int count = 0;
		long nextExpected = 1;
		while (reader.hasNext())
		{
			byte[] next = reader.next();
			ByteBuffer buffer = ByteBuffer.wrap(next);
			long timestamp = buffer.getLong();
			Date date = new Date(timestamp);

			if (CAPTURE_BREAKDOWN)
			{
				long currentMinute = (timestamp / 60000) * 60000;

				if (currentMinute != minute)
				{
					System.out.println(new Date(minute) + "," + minuteCount + "," + Arrays.toString(minuteBreakdown));
					minute = currentMinute;
					minuteCount = 0;
					Arrays.fill(minuteBreakdown, 0);
				}

				if (currentMinute == minute)
				{
					minuteCount++;
					long second = (timestamp % 60000) / 1000;
					int index = (int) second;
					minuteBreakdown[index]++;
				}
			}

			// Check for GAPS
			count++;
			ByteBufferUtil.advancePosition(buffer, 10);
			long seqNo = buffer.getLong();
			int messageCount = ByteBufferUtil.getUnsignedShort(buffer);
			if (seqNo != nextExpected)
			{
				System.out.println(date + " " + count + " GAP.  expected=" + nextExpected + " got=" + seqNo);
			}
			nextExpected = seqNo + messageCount;
		}
		System.out.println("done");
	}
}

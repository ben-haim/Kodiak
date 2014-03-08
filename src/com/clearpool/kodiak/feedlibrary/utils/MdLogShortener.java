package com.clearpool.kodiak.feedlibrary.utils;

import java.io.FileNotFoundException;
import java.nio.ByteBuffer;
import java.util.Date;

import com.clearpool.kodiak.feedlibrary.utils.MdLogger.MDLoggerProcessor;


public class MdLogShortener
{
	private static final String SOURCE_FILE = "C:\\cta\\CQS#1";
	private static final long DELETE_BEFORE = MdDateUtil.createTime(new Date(), 3, 30, 0).getTime();
	
	public static void main(String[] args) throws FileNotFoundException
	{
		MdLogReader reader = new MdLogReader(SOURCE_FILE);
		MdLogger.MDLoggerProcessor processor = new MDLoggerProcessor(SOURCE_FILE+"_shortened");
		while(reader.hasNext())
		{
			byte[] bytes = reader.next();
			ByteBuffer buffer = ByteBuffer.wrap(bytes);
			long timestamp = buffer.getLong();
			if(timestamp >= DELETE_BEFORE)
			{
				byte[] newBytes = new byte[buffer.remaining()];
				buffer.get(newBytes);
				processor.onSelection(null, ByteBuffer.wrap(newBytes), timestamp);
				processor.writePacketQueueToFile();
			}
		}
	}
}
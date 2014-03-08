package com.clearpool.kodiak.feedlibrary.core;

import java.nio.ByteBuffer;

import com.clearpool.kodiak.feedlibrary.core.MdFeedPacket;


public class TestMdFeedPacket extends MdFeedPacket
{
	private final boolean isSequenceNumberReset;
	
	public TestMdFeedPacket(long sequenceNumber, int messageCount, boolean isPacketIncrement, boolean isSequenceNumberReset)
	{
		super(isPacketIncrement);
		this.sequenceNumber = sequenceNumber;
		this.messageCount = messageCount;
		this.isSequenceNumberReset = isSequenceNumberReset;
		this.buffer = ByteBuffer.wrap(new byte[0]);
	}
	
	@Override
	public void parseHeader()
	{
		
	}

	@Override
	public boolean isSequenceNumberReset()
	{
		return this.isSequenceNumberReset;
	}

	@Override
	public boolean isEndOfTransmission()
	{
		return false;
	}
}
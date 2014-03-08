package com.clearpool.kodiak.feedlibrary.core.nasdaq;

import com.clearpool.kodiak.feedlibrary.core.MdFeedPacket;
import com.clearpool.kodiak.feedlibrary.utils.ByteBufferUtil;

public class NasdaqPacket extends MdFeedPacket
{
	private boolean isEndOfTransmission;
	
	public NasdaqPacket()
	{
		super(false);
	}

	@Override
	public void parseHeader()
	{
		ByteBufferUtil.advancePosition(this.buffer, 10); //skip session
		this.sequenceNumber = this.buffer.getLong();
		this.messageCount = ByteBufferUtil.getUnsignedShort(this.buffer);
		this.isEndOfTransmission = false;
	}

	@Override
	public boolean isSequenceNumberReset()
	{
		return false;
	}

	@Override
	public boolean isEndOfTransmission()
	{
		return this.isEndOfTransmission;
	}
	
	public void setEndOfTransmission(boolean isEndOfTransmission)
	{
		this.isEndOfTransmission = isEndOfTransmission;
	}
}
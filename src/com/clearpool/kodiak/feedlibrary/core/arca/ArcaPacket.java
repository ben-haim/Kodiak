package com.clearpool.kodiak.feedlibrary.core.arca;

import com.clearpool.kodiak.feedlibrary.core.MdFeedPacket;
import com.clearpool.kodiak.feedlibrary.utils.ByteBufferUtil;

public class ArcaPacket extends MdFeedPacket
{
	private short deliveryFlag;

	public ArcaPacket(long selectionTimeNanos)
	{
		super(false, selectionTimeNanos);
	}

	@Override
	public void parseHeader()
	{
		ByteBufferUtil.advancePosition(this.buffer, 2); // skip PktSize(2)
		this.deliveryFlag = ByteBufferUtil.getUnsignedByte(this.buffer); // to handle sequence number reset
		this.messageCount = ByteBufferUtil.getUnsignedByte(this.buffer);
		this.sequenceNumber = ByteBufferUtil.getUnsignedInt(this.buffer);
		ByteBufferUtil.advancePosition(this.buffer, 4); // skip rest of PacketHeader
	}

	@Override
	public boolean isSequenceNumberReset()
	{
		return this.deliveryFlag == 12;
	}

	@Override
	public boolean isEndOfTransmission()
	{
		return false;
	}
}

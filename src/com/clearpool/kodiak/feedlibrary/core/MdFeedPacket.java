package com.clearpool.kodiak.feedlibrary.core;

import java.nio.ByteBuffer;

public abstract class MdFeedPacket implements Comparable<MdFeedPacket>
{
	protected final boolean isPacketIncrement;

	protected long sequenceNumber;
	protected int messageCount;
	protected ByteBuffer buffer;
	protected long selectionTimeNanos;

	public abstract void parseHeader();

	public abstract boolean isSequenceNumberReset();

	public abstract boolean isEndOfTransmission();

	protected MdFeedPacket(boolean isPacketIncrement, long selectionTimeNanos)
	{
		this.isPacketIncrement = isPacketIncrement;
		this.selectionTimeNanos = selectionTimeNanos;
	}

	public long getSequenceNumber()
	{
		return this.sequenceNumber;
	}

	public int getMessageCount()
	{
		return this.messageCount;
	}

	public void setBuffer(ByteBuffer packetBuffer)
	{
		this.buffer = packetBuffer;
	}

	public ByteBuffer getBuffer()
	{
		return this.buffer;
	}

	public long getSelectionTimeNanos()
	{
		return this.selectionTimeNanos;
	}

	public boolean isPacketIncrement()
	{
		return this.isPacketIncrement;
	}

	@Override
	public String toString()
	{
		return "MdPacketHeader [sequenceNumber=" + this.sequenceNumber + ", messageCount=" + this.messageCount + "]";
	}

	@Override
	public int compareTo(MdFeedPacket o)
	{
		long diff = this.sequenceNumber - o.sequenceNumber;
		return (diff < 0) ? -1 : ((diff == 0) ? 0 : 1);
	}
}
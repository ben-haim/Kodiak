package com.clearpool.kodiak.feedlibrary.core.utp;

import java.nio.ByteBuffer;

import com.clearpool.common.util.DateUtil;
import com.clearpool.kodiak.feedlibrary.core.MdFeedPacket;
import com.clearpool.kodiak.feedlibrary.utils.ByteBufferUtil;
import com.clearpool.kodiak.feedlibrary.utils.MdDateUtil;

public class UtpPacket extends MdFeedPacket
{
	private static final long TODAY = MdDateUtil.TODAY_EST.getTime();

	private char messageCategory;
	private char messageType;
	private char participantId;
	private long timestamp;

	public UtpPacket(long selectionTimeNanos)
	{
		super(true, selectionTimeNanos);
	}

	@Override
	public void parseHeader()
	{
		this.messageCategory = (char) this.buffer.get();
		this.messageType = (char) this.buffer.get();
		ByteBufferUtil.advancePosition(this.buffer, 3); // session identifier, retrans requester
		this.sequenceNumber = ByteBufferUtil.readAsciiLong(this.buffer, 8);
		this.participantId = (char) this.buffer.get();
		this.timestamp = readTimestamp(this.buffer);
		ByteBufferUtil.advancePosition(this.buffer, 1); // reserved
		this.messageCount = 1;
	}

	private static long readTimestamp(ByteBuffer buffer)
	{
		int hours = (int) ByteBufferUtil.readAsciiLong(buffer, 2);
		int mins = (int) ByteBufferUtil.readAsciiLong(buffer, 2);
		int seconds = (int) ByteBufferUtil.readAsciiLong(buffer, 2);
		long millis = ByteBufferUtil.readAsciiLong(buffer, 3);
		return TODAY + (hours * DateUtil.MILLIS_PER_HOUR) + (mins * DateUtil.MILLIS_PER_MINUTE) + (seconds * DateUtil.MILLIS_PER_SECOND) + millis;
	}

	public char getMessageCategory()
	{
		return this.messageCategory;
	}

	public char getMessageType()
	{
		return this.messageType;
	}

	public char getParticipantId()
	{
		return this.participantId;
	}

	public long getTimestamp()
	{
		return this.timestamp;
	}

	@Override
	public boolean isSequenceNumberReset()
	{
		return this.messageCategory == 'C' && this.messageType == 'L';
	}

	@Override
	public boolean isEndOfTransmission()
	{
		return this.messageCategory == 'C' && this.messageType == 'Z';
	}

}
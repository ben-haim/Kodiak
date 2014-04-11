package com.clearpool.kodiak.feedlibrary.core.opra;

import com.clearpool.common.util.DateUtil;
import com.clearpool.kodiak.feedlibrary.core.MdFeedPacket;
import com.clearpool.kodiak.feedlibrary.utils.ByteBufferUtil;

public class OpraPacket extends MdFeedPacket
{
	private long timestamp;
	private long nanos;
	private boolean isSequenceNumberReset;
	private boolean isEndOfTransmission;

	public OpraPacket(long selectionTimeNanos)
	{
		super(true, selectionTimeNanos);
	}

	@Override
	public void parseHeader()
	{
		ByteBufferUtil.advancePosition(this.buffer, 6); // skip version + blockSize, data feed indicator, retransmission indicator, reserved field
		this.sequenceNumber = ByteBufferUtil.getUnsignedInt(this.buffer);
		this.messageCount = ByteBufferUtil.getUnsignedByte(this.buffer);
		readTimestamp();
		ByteBufferUtil.advancePosition(this.buffer, 2); // skip checksum
		if (this.messageCount == 1)
		{
			int position = this.buffer.position();
			ByteBufferUtil.advancePosition(this.buffer, 1); // skip participantId
			char category = (char) this.buffer.get();
			char type = (char) this.buffer.get();
			ByteBufferUtil.advancePosition(this.buffer, 1); // skip indicator
			this.isSequenceNumberReset = (category == 'H' && type == 'K');
			this.isEndOfTransmission = (category == 'H' && type == 'J');
			this.buffer.position(position);
		}
		else
		{
			this.isSequenceNumberReset = false;
			this.isEndOfTransmission = false;
		}
	}

	private void readTimestamp()
	{
		long secondsPart = ByteBufferUtil.getUnsignedInt(this.buffer);
		long nanosPart = ByteBufferUtil.getUnsignedInt(this.buffer);
		this.timestamp = (secondsPart * DateUtil.MILLIS_PER_SECOND) + (nanosPart / DateUtil.NANOS_PER_MILLISECOND);
		this.nanos = nanosPart % DateUtil.NANOS_PER_MILLISECOND;
	}

	public long getTimestamp()
	{
		return this.timestamp;
	}

	public long getNanos()
	{
		return this.nanos;
	}

	@Override
	public boolean isSequenceNumberReset()
	{
		return this.isSequenceNumberReset;
	}

	@Override
	public boolean isEndOfTransmission()
	{
		return this.isEndOfTransmission;
	}
}
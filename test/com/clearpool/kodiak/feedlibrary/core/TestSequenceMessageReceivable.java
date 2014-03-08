package com.clearpool.kodiak.feedlibrary.core;

import com.clearpool.kodiak.feedlibrary.core.ISequenceMessageReceivable;
import com.clearpool.kodiak.feedlibrary.core.MdFeedPacket;

public class TestSequenceMessageReceivable implements ISequenceMessageReceivable
{
	private MdFeedPacket lastPacket;
	private boolean lastShouldIgnore;
	
	@Override
	public void sequenceMessageReceived(MdFeedPacket packet, boolean shouldIgnore)
	{
		this.lastPacket = packet;
		this.lastShouldIgnore = shouldIgnore;
	}

	public MdFeedPacket getLastPacket()
	{
		return this.lastPacket;
	}

	public boolean isLastShouldIgnore()
	{
		return this.lastShouldIgnore;
	}
}
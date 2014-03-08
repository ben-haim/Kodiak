package com.clearpool.kodiak.feedlibrary.core;

public interface ISequenceMessageReceivable
{
	public void sequenceMessageReceived(MdFeedPacket packet, boolean shouldIgnore);
}

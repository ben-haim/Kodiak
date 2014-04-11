package com.clearpool.kodiak.feedlibrary.core;

import com.clearpool.common.util.DateUtil;

public class MDSequencerStats
{
	private long totalDropCount;
	private int totalPacketProcessedCount;
	private long totalMessageProcessedCount;
	private long totalProcessTime;

	private long intervalDropCount;
	private int intervalPacketProcessedCount;
	private long intervalMessageProcessedCount;
	private long intervalProcessTime;

	public void updateProcessTime(long time)
	{
		this.totalProcessTime += time;
		this.intervalProcessTime += time;
	}

	public void incrementMessageProcessedCount()
	{
		this.totalMessageProcessedCount++;
		this.intervalMessageProcessedCount++;
	}

	public void incrementPacketProcessedCount()
	{
		this.totalPacketProcessedCount++;
		this.intervalPacketProcessedCount++;
	}

	public void updateDropCount(long count)
	{
		this.totalDropCount += count;
		this.intervalDropCount += count;
	}

	public long getDropCount()
	{
		return this.totalDropCount;
	}

	public String getStats()
	{
		StringBuilder builder = new StringBuilder();
		builder.append("PacketsProcessed=").append(this.intervalPacketProcessedCount).append("(").append(this.totalPacketProcessedCount).append(')');
		builder.append("| MessagesProcessed=").append(this.intervalMessageProcessedCount).append("(").append(this.totalMessageProcessedCount).append(')');
		builder.append("| Drops=").append(this.intervalDropCount).append("(").append(this.totalDropCount).append(')');
		long totalProcessTimeSeconds = this.totalProcessTime / DateUtil.NANOS_PER_SECOND;
		long intervalProcessTimeSeconds = this.intervalProcessTime / DateUtil.NANOS_PER_SECOND;
		builder.append(" | TotalProcessTime(s)=").append(intervalProcessTimeSeconds).append("(").append(totalProcessTimeSeconds).append(')');
		long packetsPerSecond = (totalProcessTimeSeconds == 0) ? 0 : this.totalPacketProcessedCount / totalProcessTimeSeconds;
		long intervalPacketsPerSecond = (intervalProcessTimeSeconds == 0) ? 0 : this.intervalPacketProcessedCount / intervalProcessTimeSeconds;
		builder.append(" | packets/s=").append(intervalPacketsPerSecond).append("(").append(packetsPerSecond).append(')');
		long messagesPerMillisecond = (this.totalProcessTime == 0) ? 0 : this.totalMessageProcessedCount * DateUtil.NANOS_PER_MILLISECOND / this.totalProcessTime;
		long intervalMessagesPerMillisecond = (this.intervalProcessTime == 0) ? 0 : this.intervalMessageProcessedCount * DateUtil.NANOS_PER_MILLISECOND / this.intervalProcessTime;
		builder.append(" | messages/ms=").append(intervalMessagesPerMillisecond).append("(").append(messagesPerMillisecond).append(')');
		resetInterval();
		return builder.toString();
	}

	private void resetInterval()
	{
		this.intervalDropCount = 0;
		this.intervalPacketProcessedCount = 0;
		this.intervalMessageProcessedCount = 0;
		this.intervalProcessTime = 0;
	}
}
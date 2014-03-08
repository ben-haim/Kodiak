package com.clearpool.kodiak.feedlibrary.core;

import java.nio.ByteBuffer;
import java.util.PriorityQueue;
import java.util.logging.Logger;

public class MdSequencer
{
	private static final Logger LOGGER = Logger.getLogger(MdSequencer.class.getName());
	
	private static final int OUT_OF_ORDER_PACKET_THRESHOLD = 25;
	private static final int LAGGING_WAIT_PACKET_THRESHOLD = 1000;
	private static final long QUEUE_GIVEUP_TIME = 3000;
	
	private final ISequenceMessageReceivable callback;
	private final String name;
	private final boolean recover;
	private final PriorityQueue<MdFeedPacket> queue;
	private final MdFeedStat statsA;
	private final MdFeedStat statsB;
	private final MDSequencerStats stats;
	
	private Object keyA;
	private Object keyB;
	private long nextSequenceNumber;
	private long timeOfFirstQueue;
	
	public MdSequencer(ISequenceMessageReceivable callback, String name, boolean recover)
	{
		this.callback = callback;
		this.name = name;
		this.recover = recover;
		this.queue = new PriorityQueue<MdFeedPacket>();
		this.statsA = new MdFeedStat();
		this.statsB = new MdFeedStat();
		this.stats = new MDSequencerStats();
		
		this.keyA = null;
		this.keyB = null;
		this.nextSequenceNumber = 1;
		this.timeOfFirstQueue = 0;
	}

	public void setSelectionKeyA(Object key)
	{
		this.keyA = key;
	}

	public void setSelectionKeyB(Object key)
	{
		this.keyB = key;
	}

	public void sequencePacket(Object key, MdFeedPacket packet)
	{
		long start = System.nanoTime();
		updateKeyStats(key, packet);
		long headSequenceNumber = packet.getSequenceNumber();
		long tailSequenceNumber = (packet.isPacketIncrement())? packet.getSequenceNumber() : packet.getSequenceNumber() + packet.getMessageCount() - 1;
		if(headSequenceNumber <= this.nextSequenceNumber && this.nextSequenceNumber <= tailSequenceNumber)
		{
			processPacket(packet, headSequenceNumber, tailSequenceNumber);
			emptyQueue();
		}
		else if(tailSequenceNumber < this.nextSequenceNumber)
		{
			return;
		}
		else
		{
			addPacketToQueue(packet);
			if(shouldRequestData() || packet.isSequenceNumberReset())
			{
				if(!this.recover)
				{
					declareDropAndContinue();
				}
			}
		}
		this.stats.updateProcessTime(System.nanoTime() - start);
	}

	private void updateKeyStats(Object key, MdFeedPacket packet)
	{
		if(key == this.keyA)
		{
			this.statsA.updateStats(packet);
		}
		else if(key == this.keyB)
		{
			this.statsB.updateStats(packet);
		}
	}

	private void processPacket(MdFeedPacket packet, long headSequenceNumber, long tailSequenceNumber)
	{
		if(packet.isPacketIncrement())
		{
			for(long i=0; i<packet.getMessageCount(); i++)
			{
				this.callback.sequenceMessageReceived(packet, false);
				this.stats.incrementMessageProcessedCount();
			}
			if(packet.isSequenceNumberReset())
			{
				LOGGER.info(this.name + " - Sequence number reset received - currNextExpected="+this.nextSequenceNumber + " new="+1);
				this.nextSequenceNumber = 1;
			}
			this.nextSequenceNumber++;
		}
		else
		{
			for(long i=headSequenceNumber; i<=tailSequenceNumber; i++)
			{
				boolean shouldIgnore = i < this.nextSequenceNumber;
				this.callback.sequenceMessageReceived(packet, shouldIgnore);
				if(!shouldIgnore)
				{
					if(packet.isSequenceNumberReset())
					{
						LOGGER.info(this.name + " - Sequence number reset received - currNextExpected="+this.nextSequenceNumber + " new="+1);
						this.nextSequenceNumber = 1;
					}
					else
					{
						this.nextSequenceNumber++;
					}
					this.stats.incrementMessageProcessedCount();
				}
			}
		}
		this.stats.incrementPacketProcessedCount();
	}

	private void emptyQueue()
	{
		if(this.queue.size() == 0) return;
		while(this.queue.size() > 0)
		{
			MdFeedPacket head = this.queue.peek();
			long headSequenceNumber = head.getSequenceNumber();
			long tailSequenceNumber = (head.isPacketIncrement())? head.getSequenceNumber() : head.getSequenceNumber() + head.getMessageCount() - 1;
			if(tailSequenceNumber < this.nextSequenceNumber)
			{
				this.queue.remove();
			}
			else if(headSequenceNumber <= this.nextSequenceNumber && this.nextSequenceNumber <= tailSequenceNumber)
			{
				head = this.queue.remove();
				processPacket(head, headSequenceNumber, tailSequenceNumber);
			}
			else
			{
				break;
			}
		}
		
		if(this.queue.size() == 0)
		{
			this.timeOfFirstQueue = 0;
		}
	}

	private void addPacketToQueue(MdFeedPacket packet)
	{
		byte[] bytes = new byte[packet.getBuffer().remaining()];
		packet.getBuffer().get(bytes);
		packet.setBuffer(ByteBuffer.wrap(bytes));
		this.queue.add(packet);
		if(this.queue.size() == 1)
		{
			this.timeOfFirstQueue = System.currentTimeMillis();
		}
	}

	private boolean shouldRequestData()
	{
		long aSequence = this.statsA.getFeedSequenceNumber();
		long bSequence = this.statsB.getFeedSequenceNumber();
		long laggingSequence = 0;
		long leadingSequence = 0;				
		if(bSequence < aSequence)
		{
			laggingSequence = bSequence;
			leadingSequence = aSequence;
		}
		else
		{
			laggingSequence = aSequence;
			leadingSequence = bSequence;
		}
		
		long averageMessagesPerPacket = Math.max(this.statsA.getMessagesPerPacket(), this.statsB.getMessagesPerPacket());
		long outOfOrderMessageThreshold = OUT_OF_ORDER_PACKET_THRESHOLD*averageMessagesPerPacket;
		long laggingWaitMessageThreshold = LAGGING_WAIT_PACKET_THRESHOLD*averageMessagesPerPacket;
		return (laggingSequence - this.nextSequenceNumber > outOfOrderMessageThreshold) ||
				(leadingSequence - this.nextSequenceNumber > outOfOrderMessageThreshold && leadingSequence - laggingSequence > laggingWaitMessageThreshold)||
				(System.currentTimeMillis() - this.timeOfFirstQueue > QUEUE_GIVEUP_TIME);
	}

	private void declareDropAndContinue()
	{
		long firstSequenceNumber = this.queue.peek().getSequenceNumber();
		long count = firstSequenceNumber - this.nextSequenceNumber;
		if(this.nextSequenceNumber != 1) this.stats.updateDropCount(count);
		LOGGER.warning(this.name + " - Declaring drop [" + this.nextSequenceNumber + "," + (firstSequenceNumber - 1) + "] - " + count + " messages");
		this.nextSequenceNumber = firstSequenceNumber;
		emptyQueue();
	}
	
	long getDropCount()
	{
		return this.stats.getDropCount();
	}
	
	int getQueueSize()
	{
		return this.queue.size();
	}
	
	long getNextSequenceNumber()
	{
		return this.nextSequenceNumber;
	}
	
	class MdFeedStat
	{
		private long feedSequenceNumber;
		private int packetCount;
		private long messageCount;
		private long messagesPerPacket;
		
		public void updateStats(MdFeedPacket packet)
		{
			this.feedSequenceNumber = packet.getSequenceNumber();
			this.packetCount++;
			this.messageCount+=packet.getMessageCount();
			this.messagesPerPacket = this.messageCount / this.packetCount;
		}
		
		public long getFeedSequenceNumber()
		{
			return this.feedSequenceNumber;
		}
		
		public long getMessagesPerPacket()
		{
			return this.messagesPerPacket;
		}
		
		@Override
		public String toString()
		{
			StringBuilder builder = new StringBuilder();
			builder.append("packtCount=").append(this.packetCount);
			builder.append(",messageCount=").append(this.messageCount);
			builder.append(",messagesPerPacket=").append(this.messagesPerPacket);
			return builder.toString();
		}
	}

	public String getStatistics()
	{
		return this.stats.getStats();
	}
}
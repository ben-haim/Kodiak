package com.clearpool.kodiak.feedlibrary.core;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.HdrHistogram.Histogram;

import com.clearpool.common.util.DateUtil;

public class MdProcessor implements ISelectable, ISequenceMessageReceivable
{
	private static final Logger LOGGER = Logger.getLogger(MdProcessor.class.getName());

	private final MdFeed feed;
	private final String line;
	private final String processorName;
	private final String interfaceIpA;
	private final String interfaceIpB;
	private final String range;
	private final String groupA;
	private final String groupB;
	private final MdSequencer sequencer;
	private final IMdNormalizer normalizer;
	private final Histogram procStats;

	private DatagramChannel channelA;
	private DatagramChannel channelB;

	public MdProcessor(MdFeed feed, String line, String interfaceIpA, String interfaceIpB, IMdNormalizer normalizer)
	{
		this.feed = feed;
		this.line = line;
		this.processorName = feed + "#" + line;
		this.interfaceIpA = interfaceIpA;
		this.interfaceIpB = interfaceIpB;
		this.range = MdFeedProps.getProperty(feed.toString(), this.line, MdFeedProps.RANGE);
		this.groupA = MdFeedProps.getProperty(feed.toString(), this.line, "A");
		this.groupB = MdFeedProps.getProperty(feed.toString(), this.line, "B");
		this.sequencer = new MdSequencer(this, this.processorName, false);
		this.normalizer = normalizer;
		this.procStats = new Histogram(DateUtil.NANOS_PER_MINUTE, 3);
	}

	// Called by MDLibrary during start sequence
	public void registerWithSocketSelector(MdSocketSelector mdSelector)
	{
		Object[] registrationA = registerSocketChannel(this.groupA, this.interfaceIpA, mdSelector);
		Object[] registrationB = registerSocketChannel(this.groupB, this.interfaceIpB, mdSelector);
		this.sequencer.setSelectionKeyA(registrationA[0]);
		this.sequencer.setSelectionKeyB(registrationB[0]);
		this.channelA = (DatagramChannel) registrationA[1];
		this.channelB = (DatagramChannel) registrationB[1];
	}

	// Helper
	private Object[] registerSocketChannel(String group, String interfaceIp, MdSocketSelector mdSelector)
	{
		String[] groupSplit = group.split(":");
		String ip = groupSplit[0];
		int port = Integer.valueOf(groupSplit[1]).intValue();
		return mdSelector.registerMulticastChannel(ip, port, interfaceIp, this);
	}

	// Called by MDLibrary during start sequence
	public void registerWithFileSelector(String recordDirectory, MdFileSelector fileSelector) throws FileNotFoundException
	{
		String file = recordDirectory + File.separator + this.feed + "#" + this.line;
		Object a = new Object();
		Object b = new Object();
		fileSelector.registerFile(file, a, this);
		fileSelector.registerFile(file, b, this);
		this.sequencer.setSelectionKeyA(a);
		this.sequencer.setSelectionKeyB(b);
	}

	// Called by SelectorThread
	@Override
	public void onSelection(Object key, ByteBuffer buffer)
	{
		MdFeedPacket packet = MdFeedPacketFactory.createPacket(this.feed, System.nanoTime());
		if (this.feed.containsMultiplePacketsInBlock())
		{
			while (buffer.remaining() > 1)
			{
				buffer.get(); // read SOH or US
				int startPosition = buffer.position();
				while (buffer.hasRemaining())
				{
					byte nextByte = buffer.get();
					if (nextByte == 31 || nextByte == 3) break;
				}
				int separatorPosition = buffer.position();
				byte[] bytes = new byte[(separatorPosition - 1) - startPosition];
				buffer.position(startPosition);
				buffer.get(bytes);
				packet.setBuffer(ByteBuffer.wrap(bytes));
				packet.parseHeader();
				this.sequencer.sequencePacket(key, packet);
			}
			buffer.get(); // ETX
		}
		else
		{
			packet.setBuffer(buffer);
			packet.parseHeader();
			this.sequencer.sequencePacket(key, packet);
		}
	}

	// Called by sequencer
	@Override
	public void sequenceMessageReceived(MdFeedPacket packet, boolean shouldIgnore)
	{
		this.normalizer.processMessage(this.processorName, packet, shouldIgnore);
		if (packet.isEndOfTransmission())
		{
			handleEndOfTransmission();
		}
		this.procStats.recordValue(System.nanoTime() - packet.getSelectionTimeNanos());
	}

	private void handleEndOfTransmission()
	{
		LOGGER.info(this.processorName + " - Received end of transmission message");
		try
		{
			if (this.channelA != null) this.channelA.close();
			if (this.channelB != null) this.channelB.close();
		}
		catch (IOException e)
		{
			LOGGER.log(Level.SEVERE, e.getMessage(), e);
		}
	}

	public IMdNormalizer getNormalizer()
	{
		return this.normalizer;
	}

	public String getLine()
	{
		return this.line;
	}

	public String getRange()
	{
		return this.range;
	}

	public String getStatistics()
	{
		return this.sequencer.getStatistics();
	}

	public Histogram getHistogram()
	{
		return this.procStats;
	}
}
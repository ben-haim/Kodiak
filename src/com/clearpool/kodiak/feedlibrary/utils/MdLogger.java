package com.clearpool.kodiak.feedlibrary.utils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.clearpool.common.util.DateUtil;
import com.clearpool.kodiak.feedlibrary.core.ISelectable;
import com.clearpool.kodiak.feedlibrary.core.MdFeed;
import com.clearpool.kodiak.feedlibrary.core.MdFeedProps;
import com.clearpool.kodiak.feedlibrary.core.MdSocketSelector;

public class MdLogger
{
	static final Logger LOGGER = Logger.getLogger(MdLogger.class.getName());
	private static final int TIMER_INTERVAL = 1000;

	private final MdSocketSelector selector;
	private final List<MDLoggerProcessor> processors;
	private final Timer timer;

	public MdLogger(String feed, String[] lines, String networkIp, String logDirectory, boolean isPrimary) throws IOException
	{
		this.selector = new MdSocketSelector(feed + " Logger Selector Thread", 8388608);
		this.processors = new LinkedList<MDLoggerProcessor>();
		this.timer = new Timer();
		registerProcessors(feed, lines, networkIp, logDirectory, isPrimary);
	}

	public void start()
	{
		this.timer.schedule(new TimerTask() {

			@SuppressWarnings("synthetic-access")
			@Override
			public void run()
			{
				for (MDLoggerProcessor processor : MdLogger.this.processors)
				{
					processor.writePacketQueueToFile();
				}
			}
		}, TIMER_INTERVAL, TIMER_INTERVAL);
		this.selector.start();
	}

	private void registerProcessors(String inputfeed, String[] lines, String networkIp, String logDirectory, boolean isPrimary)
	{
		String[] feeds = null;
		if (inputfeed.equals("CTA")) feeds = new String[] { MdFeed.CQS.toString(), MdFeed.CTS.toString() };
		else if (inputfeed.equals("UTP")) feeds = new String[] { MdFeed.UQDF.toString(), MdFeed.UTDF.toString() };
		else feeds = new String[] { inputfeed };

		for (String feed : feeds)
		{
			for (int i = 0; i < lines.length; i++)
			{
				String line = lines[i];
				String group = MdFeedProps.getProperty(feed, line, isPrimary ? "A" : "B");
				String[] groupSplit = group.split(":");
				String ip = groupSplit[0];
				int port = Integer.parseInt(groupSplit[1]);
				String fileName = logDirectory + File.separator + feed + "#" + line;
				MDLoggerProcessor processor = new MDLoggerProcessor(fileName);
				this.processors.add(processor);
				this.selector.registerMulticastChannel(ip, port, networkIp, processor);
			}
		}
	}

	static class MDLoggerProcessor implements ISelectable
	{
		private final BlockingQueue<byte[]> packetQueue;
		private final BufferedOutputStream outStream;

		public MDLoggerProcessor(String fileName)
		{
			this.packetQueue = new LinkedBlockingQueue<byte[]>();
			this.outStream = createOutstream(fileName);
		}

		private static BufferedOutputStream createOutstream(String fileName)
		{
			try
			{
				File file = new File(fileName);
				if (file.exists())
				{
					long lastMod = file.lastModified();
					if (lastMod < DateUtil.TODAY_MIDNIGHT_EST.getTime())
					{
						file.delete();
					}
				}
				else
				{
					file.createNewFile();
				}
				BufferedOutputStream stream = new BufferedOutputStream(new FileOutputStream(file, true));
				return stream;
			}
			catch (Exception e)
			{
				LOGGER.log(Level.SEVERE, e.getMessage(), e);
			}
			return null;
		}

		// Called by Selector Thread
		@Override
		public void onSelection(Object key, ByteBuffer buffer)
		{
			long timestamp = System.currentTimeMillis();
			onSelection(key, buffer, timestamp);
		}

		@SuppressWarnings("unused")
		public void onSelection(Object key, ByteBuffer buffer, long timestamp)
		{
			byte[] bytes = new byte[8 + buffer.remaining()];
			ByteBuffer newBuffer = ByteBuffer.wrap(bytes);
			newBuffer.putLong(timestamp);
			newBuffer.put(buffer);
			try
			{
				this.packetQueue.put(bytes);
			}
			catch (InterruptedException e)
			{
				LOGGER.log(Level.SEVERE, e.getMessage(), e);
			}
		}

		// Called by timer thread
		public void writePacketQueueToFile()
		{
			if (this.packetQueue.size() > 0)
			{
				LinkedList<byte[]> byteList = new LinkedList<>();
				try
				{
					this.packetQueue.drainTo(byteList);
					for (byte[] bytes : byteList)
					{
						int length = bytes.length;
						int firstByte = (byte) length;
						int secondByte = (byte) (length >> 8);
						this.outStream.write(firstByte);
						this.outStream.write(secondByte);
						this.outStream.write(bytes);
					}
					this.outStream.flush();
				}
				catch (Exception e)
				{
					LOGGER.log(Level.SEVERE, e.getMessage(), e);
				}
			}
		}
	}

	public static void main(String[] args) throws IOException
	{
		String feed = args[0];
		String[] lines = args[1].split(",");
		String networkIp = args[2];
		String logDirectory = args[3];
		boolean isPrimary = (args.length >= 5) ? Boolean.parseBoolean(args[4]) : true;
		MdLogger logger = new MdLogger(feed, lines, networkIp, logDirectory, isPrimary);
		logger.start();
	}
}
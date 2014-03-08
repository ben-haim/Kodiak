package com.clearpool.kodiak.feedlibrary.core;

import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.clearpool.kodiak.feedlibrary.callbacks.IMdLibraryCallback;
import com.clearpool.messageobjects.marketdata.MdServiceType;

public class MdLibrary
{
	private static final Logger LOGGER = Logger.getLogger(MdLibrary.class.getName());
	private static final Timer TIMER = new Timer("MDLibrary Timer", true);

	private final MdLibraryContext context;
	private final MdFeed feed;
	private final String[] lines;
	private final String interfaceA;
	private final String interfaceB;
	private final String readFromDir;
	private final Map<MdServiceType, IMdLibraryCallback> callbacks;

	private MdProcessor[] mdProcessors;

	public MdLibrary(MdLibraryContext context, MdFeed feed, String[] lines, String interfaceA, String interfaceB, int statTime, String readFromDir)
	{
		this.context = context;
		this.feed = feed;
		this.lines = lines;
		this.interfaceA = interfaceA;
		this.interfaceB = interfaceB;
		this.readFromDir = readFromDir;
		this.callbacks = new HashMap<MdServiceType, IMdLibraryCallback>();
		if (statTime > 0) TIMER.schedule(new MdLibraryStatisticsTask(this), statTime, statTime);
	}

	public void registerService(MdServiceType serviceType, IMdLibraryCallback callback)
	{
		this.callbacks.put(serviceType, callback);
	}

	public void initProcessors()
	{
		try
		{
			this.mdProcessors = new MdProcessor[this.lines.length];
			for (int i = 0; i < this.lines.length; i++)
			{
				String line = this.lines[i];
				int lineInt = Integer.parseInt(line);
				String range = MdFeedProps.getProperty(this.feed.toString(), line, MdFeedProps.RANGE);
				IMdNormalizer normalizer = createNormalizer(this.feed, range);
				MdProcessor processor = new MdProcessor(this.feed, line, this.interfaceA, this.interfaceB, normalizer);
				this.mdProcessors[i] = processor;
				if (this.context.readFromSocket())
				{
					processor.registerWithSocketSelector(this.context.getSocketSelectorForLine(lineInt));
				}
				else
				{
					processor.registerWithFileSelector(this.readFromDir, this.context.getFileSelectorForLine(lineInt));
				}
			}

		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, e.getMessage(), e);
		}
	}

	// Helper
	private IMdNormalizer createNormalizer(MdFeed normalizerFeed, String range)
	{
		Object object = instantiateClass(normalizerFeed.toString(), MdFeedProps.NORMALIZER, this.callbacks, range);
		if (object == null) return null;
		return (IMdNormalizer) object;
	}

	@SuppressWarnings("unchecked")
	private static Object instantiateClass(String feed, String prop, Object constructorParameter1, String constructorParameter2)
	{
		String className = MdFeedProps.getProperty(feed, prop);
		if (className == null || className.isEmpty()) return null;
		try
		{
			@SuppressWarnings("rawtypes")
			Class classz = Class.forName(className);
			return classz.getConstructor(Map.class, String.class).newInstance(constructorParameter1, constructorParameter2);
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, e.getMessage(), e);
		}
		return null;
	}

	public MdFeed getMdFeed()
	{
		return this.feed;
	}

	public String[] getLines()
	{
		return this.lines;
	}

	public String getInterfaceA()
	{
		return this.interfaceA;
	}

	public String getInterfaceB()
	{
		return this.interfaceB;
	}

	public MdProcessor[] getMdProcessors()
	{
		return this.mdProcessors;
	}

	public int getSelectorThreadCount()
	{
		return this.context.getSelectorThreadCount();
	}
}
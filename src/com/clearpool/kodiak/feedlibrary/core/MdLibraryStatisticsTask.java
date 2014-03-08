package com.clearpool.kodiak.feedlibrary.core;

import java.util.TimerTask;
import java.util.logging.Logger;

public class MdLibraryStatisticsTask extends TimerTask
{
	private static final Logger LOGGER = Logger.getLogger(MdLibraryStatisticsTask.class.getSimpleName());

	private final MdLibrary library;

	public MdLibraryStatisticsTask(MdLibrary library)
	{
		this.library = library;
	}

	@Override
	public void run()
	{
		for (MdProcessor processor : this.library.getMdProcessors())
		{
			if(processor != null)
			{
				StringBuilder builder = new StringBuilder();
				builder.append(this.library.getMdFeed());
				builder.append(" Line#").append(processor.getLine());
				builder.append(" Range=").append(processor.getRange());
				builder.append(" Stats=").append(processor.getStatistics());
				LOGGER.info(builder.toString());
			}
		}
	}
}
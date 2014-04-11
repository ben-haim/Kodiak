package com.clearpool.kodiak.feedlibrary.core;

import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.HdrHistogram.HistogramData;

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
			if (processor != null)
			{
				try
				{
					StringBuilder builder = new StringBuilder();
					builder.append(this.library.getMdFeed());
					builder.append(" Line#").append(processor.getLine());
					builder.append(" Range=").append(processor.getRange());
					builder.append(" Stats=[").append(processor.getStatistics());
					HistogramData procData = processor.getHistogramData();
					builder.append("] Histogram=[Count=").append(procData.getTotalCount());
					builder.append(", Min=").append(procData.getMinValue());
					builder.append(", Max=").append(procData.getMaxValue());
					builder.append(", Mean=").append(procData.getMean());
					builder.append(", Median=").append(procData.getValueAtPercentile(50));
					builder.append(", 95%ile=").append(procData.getValueAtPercentile(95));
					builder.append(", 99%ile=").append(procData.getValueAtPercentile(99));
					builder.append("]");
					LOGGER.info(builder.toString());
				}
				catch (Exception e)
				{
					LOGGER.log(Level.SEVERE, e.getMessage(), e);
				}
			}
		}
	}
}
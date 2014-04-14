package com.clearpool.kodiak.feedlibrary.core;

import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.HdrHistogram.Histogram;
import org.HdrHistogram.HistogramData;

import com.clearpool.common.util.DateUtil;

public class MdLibraryStatisticsTask extends TimerTask
{
	private static final Logger LOGGER = Logger.getLogger(MdLibraryStatisticsTask.class.getSimpleName());

	private final MdLibrary library;
	private final Histogram procStats;

	public MdLibraryStatisticsTask(MdLibrary library)
	{
		this.library = library;
		this.procStats = new Histogram(DateUtil.NANOS_PER_MINUTE, 3);
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
					processor.getHistogram().copyInto(this.procStats);
					HistogramData procData = this.procStats.getHistogramData();
					builder.append("] Histogram=[Count=").append(procData.getTotalCount());
					builder.append(", Min=").append(procData.getMinValue());
					builder.append(", Max=").append(procData.getMaxValue());
					builder.append(", Mean=").append(procData.getMean());
					builder.append(", Median=");
					try
					{
						builder.append(procData.getValueAtPercentile(50));
					}
					catch (ArrayIndexOutOfBoundsException e)
					{
						builder.append("n/a");
					}
					builder.append(", 95%ile=");
					try
					{
						builder.append(procData.getValueAtPercentile(95));
					}
					catch (ArrayIndexOutOfBoundsException e)
					{
						builder.append("n/a");
					}
					builder.append(", 99%ile=");
					try
					{
						builder.append(procData.getValueAtPercentile(99));
					}
					catch (ArrayIndexOutOfBoundsException e)
					{
						builder.append("n/a");
					}
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
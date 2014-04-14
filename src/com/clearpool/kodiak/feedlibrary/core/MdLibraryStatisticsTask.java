package com.clearpool.kodiak.feedlibrary.core;

import java.util.Date;
import java.util.HashMap;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.HdrHistogram.Histogram;
import org.HdrHistogram.HistogramData;

import com.clearpool.common.util.DateUtil;
import com.clearpool.commonserver.ConfigManager;
import com.clearpool.commonserver.db.DBAdapterProto;
import com.clearpool.commonserver.db.DBConnectionInfo;
import com.clearpool.commonserver.db.DbType;
import com.clearpool.commonserver.db.IDBAdapter;
import com.clearpool.commonserver.utils.ProcessConfigUtil;
import com.clearpool.messageobjects.metrics.MetricType;
import com.clearpool.messageobjects.metrics.MetricsState;

public class MdLibraryStatisticsTask extends TimerTask
{
	private static final Logger LOGGER = Logger.getLogger(MdLibraryStatisticsTask.class.getSimpleName());
	private static final String PROC_NAME = ProcessConfigUtil.getProcName();
	private static final String PROC_PARTITION = ProcessConfigUtil.getPartitionId();
	private static final String DB_ADAPTER_THREADS = "DB_ADAPTER_THREADS";
	private static final String METRIC_NAME = "MDProcessorLatency";
	private static final int DEFAULT_NUM_DB_ADAPTER_THREADS = 5;

	private final MdLibrary library;
	private final Histogram procStats;
	private final HashMap<String, MetricsState> metricsStateMap;
	private final IDBAdapter dbAdapter;

	public MdLibraryStatisticsTask(MdLibrary library) throws Exception
	{
		this.library = library;
		this.procStats = new Histogram(DateUtil.NANOS_PER_MINUTE, 3);
		this.metricsStateMap = new HashMap<String, MetricsState>();
		this.dbAdapter = DBAdapterProto.getInstance(DBConnectionInfo.getDBConnectionInfo(ProcessConfigUtil.getEnvironmentType(), DbType.ORDER), ConfigManager.getInstance()
				.getIntConfigValueDefaultIfNull(DB_ADAPTER_THREADS, DEFAULT_NUM_DB_ADAPTER_THREADS));
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

					MetricsState state = getMetricsState(processor.getProcessorName());
					state.setVersion(state.getVersion() + 1);
					state.setLastModUser(MdLibraryStatisticsTask.PROC_NAME);
					state.setLastModTime(new Date());
					processHistogram(processor, builder, state);
					builder.append("]");
					LOGGER.info(builder.toString());
					this.dbAdapter.insert(state);
				}
				catch (Exception e)
				{
					LOGGER.log(Level.SEVERE, e.getMessage(), e);
				}
			}
		}
	}

	private void processHistogram(MdProcessor processor, StringBuilder builder, MetricsState state)
	{
		processor.getHistogram().copyInto(this.procStats);
		this.procStats.reestablishTotalCount();
		HistogramData procData = this.procStats.getHistogramData();
		long count = procData.getTotalCount();
		builder.append("] Histogram=[Count=").append(count);
		state.setHistogramEntryCount(procData.getTotalCount());
		long minValue = procData.getMinValue();
		builder.append(", Min=").append(minValue);
		state.setMin(minValue);
		long maxValue = procData.getMaxValue();
		builder.append(", Max=").append(maxValue);
		state.setMax(maxValue);
		double meanValue = procData.getMean();
		builder.append(", Mean=").append(meanValue);
		state.setMean(meanValue);
		if (count > 0)
		{
			state.setStdDev(procData.getStdDeviation());
			long medianValue = procData.getValueAtPercentile(50);
			builder.append(", Median=").append(medianValue);
			state.setPercentile50th(medianValue);
			state.setPercentile75th(procData.getValueAtPercentile(75));
			long percentile95 = procData.getValueAtPercentile(95);
			builder.append(", 95%ile=").append(percentile95);
			state.setPercentile95th(percentile95);
			state.setPercentile98th(procData.getValueAtPercentile(98));
			long percentile99 = procData.getValueAtPercentile(99);
			builder.append(", 99%ile=").append(percentile99);
			state.setPercentile99th(percentile99);
			state.setPercentile999th(procData.getValueAtPercentile(99.9));
		}
	}

	private MetricsState getMetricsState(String mdProcName)
	{
		String metricStateKey = DateUtil.TODAY_TRADE_DATE + ":" + MdLibraryStatisticsTask.PROC_PARTITION + ":" + MdLibraryStatisticsTask.PROC_NAME + ":" + mdProcName + ":"
				+ MetricType.HISTOGRAM + ":" + METRIC_NAME;
		MetricsState state = this.metricsStateMap.get(metricStateKey);
		if (state == null)
		{
			state = new MetricsState();
			state.setVersion(0);
			state.setProcName(MdLibraryStatisticsTask.PROC_NAME);
			state.setProcPartition(MdLibraryStatisticsTask.PROC_PARTITION);
			state.setMetricType(MetricType.HISTOGRAM);
			state.setMetricName(METRIC_NAME);
			state.setTradeDate(DateUtil.TODAY_TRADE_DATE);
			this.metricsStateMap.put(metricStateKey, state);
		}
		return state;
	}
}
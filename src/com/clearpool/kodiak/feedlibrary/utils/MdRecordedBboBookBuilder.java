package com.clearpool.kodiak.feedlibrary.utils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.clearpool.common.util.DateUtil;
import com.clearpool.messageobjects.marketdata.MdEntity;
import com.clearpool.messageobjects.marketdata.Quote;

public class MdRecordedBboBookBuilder
{
	private static final String FILE = "Z:\\zsl";
	private static final Date PRINT_START_TIME = DateUtil.createTime(new Date(), 10, 01, 55);
	private static final Date PRINT_END_TIME = DateUtil.createTime(new Date(), 10, 01, 56);

	public static void main(String[] args) throws Exception
	{
		BufferedReader reader = new BufferedReader(new FileReader(FILE));
		String line = null;
		Map<String, Quote> bidQuotes = new HashMap<String, Quote>();
		Map<String, Quote> askQuotes = new HashMap<String, Quote>();
		while ((line = reader.readLine()) != null)
		{
			String[] fields = line.split(String.valueOf("\\" + MdEntity.DELIMITER));
			Date time = new Date(Long.valueOf(fields[4]).longValue());
			double bidPrice = Double.valueOf(fields[5]).doubleValue();
			int bidSize = Integer.valueOf(fields[6]).intValue();
			String bidExchange = fields[7];
			double askPrice = Double.valueOf(fields[8]).doubleValue();
			int askSize = Integer.valueOf(fields[9]).intValue();
			String askExchange = fields[10];

			Quote quote = new Quote();
			quote.setTimestamp(time.getTime());
			quote.setBidPrice(bidPrice);
			quote.setBidSize(bidSize);
			quote.setAskPrice(askPrice);
			quote.setAskSize(askSize);

			if (bidSize > 0)
			{
				bidQuotes.put(bidExchange, quote);
			}
			else
			{
				bidQuotes.remove(bidExchange);
			}
			if (askSize > 0)
			{
				askQuotes.put(askExchange, quote);
			}
			else
			{
				askQuotes.remove(askExchange);
			}

			if (PRINT_START_TIME.getTime() <= time.getTime() && time.getTime() <= PRINT_END_TIME.getTime())
			{
				System.out.println(DateUtil.MILLISECOND_FORMATTER.format(time));
				for (Entry<String, Quote> entry : bidQuotes.entrySet())
				{
					System.out.println("BID," + entry.getKey() + "," + entry.getValue().getBidPrice() + "," + entry.getValue().getBidSize() + ","
							+ new Date(entry.getValue().getTimestamp()));
				}
				for (Entry<String, Quote> entry : askQuotes.entrySet())
				{
					System.out.println("ASK," + entry.getKey() + "," + entry.getValue().getAskPrice() + "," + entry.getValue().getAskSize() + ","
							+ new Date(entry.getValue().getTimestamp()));
				}
				System.out.println();
			}
			else if (time.getTime() > PRINT_END_TIME.getTime())
			{
				break;
			}
		}
	}
}

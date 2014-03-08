package com.clearpool.kodiak.feedlibrary.core.utp;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.clearpool.common.util.DateUtil;
import com.clearpool.kodiak.feedlibrary.caches.IMdServiceCache;
import com.clearpool.kodiak.feedlibrary.caches.SaleCache;
import com.clearpool.kodiak.feedlibrary.callbacks.IMdLibraryCallback;
import com.clearpool.kodiak.feedlibrary.callbacks.IMdSaleListener;
import com.clearpool.kodiak.feedlibrary.core.IMdNormalizer;
import com.clearpool.kodiak.feedlibrary.core.MdFeed;
import com.clearpool.kodiak.feedlibrary.core.MdFeedPacket;
import com.clearpool.kodiak.feedlibrary.core.MdFeedProps;
import com.clearpool.kodiak.feedlibrary.utils.ByteBufferUtil;
import com.clearpool.messageobjects.marketdata.Exchange;
import com.clearpool.messageobjects.marketdata.MdEntity;
import com.clearpool.messageobjects.marketdata.MdServiceType;
import com.clearpool.messageobjects.marketdata.Sale;

public class UtdfNormalizer implements IMdNormalizer
{
	private static final Logger LOGGER = Logger.getLogger(UtdfNormalizer.class.getName());

	private static final char CATEGORY_TRADE = 'T';
	private static final char CATEGORY_ADMINISTRATIVE = 'A';
	private static final char CATEGORY_CONTROL = 'C';
	private static final char TYPE_SHORT_TRADE = 'A';
	private static final char TYPE_LONG_TRADE = 'W';
	private static final char TYPE_CORRECTION = 'Y';
	private static final char TYPE_CANCEL_ERROR = 'Z';
	private static final char TYPE_ADMIN_MESSAGE = 'A';
	private static final char TYPE_CLOSING_TRADE_SUMMARY_REPORT = 'Z';
	private static final char TYPE_END_OF_CONSOLIDATED_LAST_SALE_ELIGIBILITY = 'S';
	private static final char NASDAQ_PARTICIPANT = 'Q';

	private final String range;
	private final SaleCache sales;

	private boolean receivedEndOfLastSaleEligibleControlMessage;
	private BufferedWriter closePriceWriter;

	public UtdfNormalizer(Map<MdServiceType, IMdLibraryCallback> callbacks, String range)
	{
		this.range = range;
		this.sales = new SaleCache((IMdSaleListener) callbacks.get(MdServiceType.SALE), MdFeed.UTDF, range, false);

		this.receivedEndOfLastSaleEligibleControlMessage = false;
		this.closePriceWriter = null;

		loadClosePrices();
	}

	private void loadClosePrices()
	{
		String filePrefix = (String) MdFeedProps.getInstanceProperty(MdFeed.UTDF.toString(), "CLOSE_PRICE_FILE_PREFIX");
		if (filePrefix != null)
		{
			File file = new File(filePrefix + "_" + this.range);
			if (file.exists())
			{
				Map<String, Double> prices = new HashMap<String, Double>();
				try
				{
					BufferedReader reader = new BufferedReader(new FileReader(file));
					String line = null;

					while ((line = reader.readLine()) != null)
					{
						String[] split = line.split(",");
						String symbol = split[0];
						Double price = Double.valueOf(split[1]);
						prices.put(symbol, price);
					}
				}
				catch (Exception e)
				{
					LOGGER.log(Level.SEVERE, e.getMessage(), e);
				}

				if (prices.size() > 0)
				{
					String[] rangeSplit = this.range.split("-");
					String firstRange = rangeSplit[0].replace("[", "");
					String secondRange = rangeSplit[1].replace("]", "") + "ZZZZZZZZ";

					for (Map.Entry<String, Double> entry : prices.entrySet())
					{
						String symbol = entry.getKey();
						double price = entry.getValue().doubleValue();
						if (firstRange.compareTo(symbol) <= 0 && symbol.compareTo(secondRange) <= 0)
						{
							this.sales.setLatestClosePrice(symbol, Exchange.USEQ_SIP, price, DateUtil.TODAY_MIDNIGHT_EST.getTime(), "SDS");
						}
					}
				}
			}
			else
			{
				LOGGER.severe("Unable to load close prices because can't find file=" + file);
			}
		}
	}

	@Override
	public IMdServiceCache[] getMdServiceCaches()
	{
		return new IMdServiceCache[] { this.sales };
	}

	@Override
	public void processMessage(String processorName, MdFeedPacket packet, boolean shouldIgnore)
	{
		if (shouldIgnore) return;
		UtpPacket utpPacket = (UtpPacket) packet;
		ByteBuffer buffer = utpPacket.getBuffer();

		if (utpPacket.getMessageCategory() == CATEGORY_TRADE)
		{
			if (utpPacket.getMessageType() == TYPE_SHORT_TRADE || utpPacket.getMessageType() == TYPE_LONG_TRADE)
			{
				boolean isLong = utpPacket.getMessageType() == TYPE_LONG_TRADE;
				String symbol = ByteBufferUtil.getString(buffer, isLong ? 11 : 5).trim();
				if (isLong) ByteBufferUtil.advancePosition(buffer, 1); // trade through exempt flag
				String saleCondition = ByteBufferUtil.getString(buffer, isLong ? 4 : 1);
				if (isLong) ByteBufferUtil.advancePosition(buffer, 2); // seller's sale days
				char priceDenominator = (char) buffer.get();
				double price = UtpUtils.getPrice(ByteBufferUtil.readAsciiLong(buffer, isLong ? 10 : 6), priceDenominator);
				if (isLong) ByteBufferUtil.advancePosition(buffer, 3); // Currency
				int size = (int) ByteBufferUtil.readAsciiLong(buffer, isLong ? 9 : 6);
				ByteBufferUtil.advancePosition(buffer, 1); // consolidated price change indicator

				Exchange exchange = UtpUtils.getExchange(utpPacket.getParticipantId());
				Sale previousSale = this.sales.getData(symbol);
				int conditionCode = getSaleConditions(saleCondition, previousSale, utpPacket.getParticipantId() == NASDAQ_PARTICIPANT);
				this.sales.updateWithSaleCondition(symbol, price, size, exchange, utpPacket.getTimestamp(), conditionCode, saleCondition);
			}
			else if (utpPacket.getMessageType() == TYPE_CORRECTION)
			{
				long originalSequenceNumber = ByteBufferUtil.readAsciiLong(buffer, 8);
				String symbol = ByteBufferUtil.getString(buffer, 11).trim();
				ByteBufferUtil.advancePosition(buffer, 1); // trade through exmept flag
				String originalSaleCondition = ByteBufferUtil.getString(buffer, 4);
				ByteBufferUtil.advancePosition(buffer, 2); // seller's sale days
				char originalPriceDenominator = (char) buffer.get();
				double originalPrice = UtpUtils.getPrice(ByteBufferUtil.readAsciiLong(buffer, 10), originalPriceDenominator);
				ByteBufferUtil.advancePosition(buffer, 3); // Currency
				int originalSize = (int) ByteBufferUtil.readAsciiLong(buffer, 9);
				ByteBufferUtil.advancePosition(buffer, 1); // trade through exmept flag
				String correctedSaleCondition = ByteBufferUtil.getString(buffer, 4);
				ByteBufferUtil.advancePosition(buffer, 2); // seller's sale days
				char correctedPriceDenominator = (char) buffer.get();
				double correctedPrice = UtpUtils.getPrice(ByteBufferUtil.readAsciiLong(buffer, 10), correctedPriceDenominator);
				ByteBufferUtil.advancePosition(buffer, 3); // Currency
				int correctedSize = (int) ByteBufferUtil.readAsciiLong(buffer, 9);
				char highPriceDenominator = (char) buffer.get();
				double highPrice = UtpUtils.getPrice(ByteBufferUtil.readAsciiLong(buffer, 10), highPriceDenominator);
				char lowPriceDenominator = (char) buffer.get();
				double lowPrice = UtpUtils.getPrice(ByteBufferUtil.readAsciiLong(buffer, 10), lowPriceDenominator);
				char lastPriceDenominator = (char) buffer.get();
				double lastPrice = UtpUtils.getPrice(ByteBufferUtil.readAsciiLong(buffer, 10), lastPriceDenominator);
				Exchange lastExchange = UtpUtils.getExchange((char) buffer.get());
				ByteBufferUtil.advancePosition(buffer, 3); // Currency
				long consolidatedVolume = ByteBufferUtil.readAsciiLong(buffer, 11);
				ByteBufferUtil.advancePosition(buffer, 1); // consolidatedPriceChangeIndicator

				int originalConditionCode = getSaleConditions(originalSaleCondition, null, utpPacket.getParticipantId() == NASDAQ_PARTICIPANT);
				int correctedConditionCode = getSaleConditions(correctedSaleCondition, null, utpPacket.getParticipantId() == NASDAQ_PARTICIPANT);
				this.sales.correctWithStats(symbol, originalPrice, originalSize, originalConditionCode, correctedPrice, correctedSize, correctedConditionCode,
						utpPacket.getTimestamp(), lastExchange, lastPrice, highPrice, lowPrice, -1, consolidatedVolume);
				LOGGER.info(processorName + " - Received Correction Message - Symbol=" + symbol + " orig=" + originalPrice + "@" + originalSize + " (origiSeqNo="
						+ originalSequenceNumber + ") corrected=" + correctedPrice + "@" + correctedSize);
			}
			else if (utpPacket.getMessageType() == TYPE_CANCEL_ERROR)
			{
				long originalSequenceNumber = ByteBufferUtil.readAsciiLong(buffer, 8);
				String symbol = ByteBufferUtil.getString(buffer, 11).trim();
				ByteBufferUtil.advancePosition(buffer, 2); // function, trade through exmept flag
				String originalSaleCondition = ByteBufferUtil.getString(buffer, 4);
				ByteBufferUtil.advancePosition(buffer, 2); // seller's sale days
				char originalPriceDenominator = (char) buffer.get();
				double originalPrice = UtpUtils.getPrice(ByteBufferUtil.readAsciiLong(buffer, 10), originalPriceDenominator);
				ByteBufferUtil.advancePosition(buffer, 3); // Currency
				int originalSize = (int) ByteBufferUtil.readAsciiLong(buffer, 9);
				char highPriceDenominator = (char) buffer.get();
				double highPrice = UtpUtils.getPrice(ByteBufferUtil.readAsciiLong(buffer, 10), highPriceDenominator);
				char lowPriceDenominator = (char) buffer.get();
				double lowPrice = UtpUtils.getPrice(ByteBufferUtil.readAsciiLong(buffer, 10), lowPriceDenominator);
				char lastPriceDenominator = (char) buffer.get();
				double lastPrice = UtpUtils.getPrice(ByteBufferUtil.readAsciiLong(buffer, 10), lastPriceDenominator);
				Exchange lastExchange = UtpUtils.getExchange((char) buffer.get());
				ByteBufferUtil.advancePosition(buffer, 3); // Currency
				long consolidatedVolume = ByteBufferUtil.readAsciiLong(buffer, 11);
				ByteBufferUtil.advancePosition(buffer, 1); // consolidatedPriceChangeIndicator

				int originalConditionCode = getSaleConditions(originalSaleCondition, null, utpPacket.getParticipantId() == NASDAQ_PARTICIPANT);
				this.sales.cancelWithStats(symbol, originalPrice, originalSize, originalConditionCode, utpPacket.getTimestamp(), lastExchange, lastPrice, highPrice, lowPrice, -1,
						consolidatedVolume);
				LOGGER.info(processorName + " - Received Cancel Message - Symbol=" + symbol + " orig=" + originalPrice + "@" + originalSize + " (origiSeqNo="
						+ originalSequenceNumber + ")");
			}
		}
		else if (utpPacket.getMessageCategory() == CATEGORY_ADMINISTRATIVE)
		{
			if (utpPacket.getMessageType() == TYPE_ADMIN_MESSAGE)
			{
				String message = ByteBufferUtil.getString(buffer, buffer.remaining()).trim();
				LOGGER.info(processorName + " - Received Admin Message - " + message);
				if (message.startsWith("IPO PRICE AT"))
				{
					String[] spaceSplit = message.split(" ");
					String symbol = spaceSplit[4];
					String priceString = spaceSplit[5];
					int index = priceString.indexOf(".");
					double price = Integer.valueOf(priceString.substring(0, index)).intValue() / 100d;
					this.sales.setLatestClosePrice(symbol, Exchange.USEQ_SIP, price, utpPacket.getTimestamp(), "SDS");
				}
			}
			else if (utpPacket.getMessageType() == TYPE_CLOSING_TRADE_SUMMARY_REPORT)
			{
				String symbol = ByteBufferUtil.getString(buffer, 11).trim();
				char highPriceDenominator = (char) buffer.get();
				double highPrice = UtpUtils.getPrice(ByteBufferUtil.readAsciiLong(buffer, 10), highPriceDenominator);
				char lowPriceDenominator = (char) buffer.get();
				double lowPrice = UtpUtils.getPrice(ByteBufferUtil.readAsciiLong(buffer, 10), lowPriceDenominator);
				char closingPriceDenominator = (char) buffer.get();
				double closingPrice = UtpUtils.getPrice(ByteBufferUtil.readAsciiLong(buffer, 10), closingPriceDenominator);
				Exchange closingPriceExchange = UtpUtils.getExchange((char) buffer.get());
				ByteBufferUtil.advancePosition(buffer, 3); // Currency
				long consolidatedVolume = ByteBufferUtil.readAsciiLong(buffer, 11);
				ByteBufferUtil.advancePosition(buffer, 1); // tradeActionIndicator
				int numberOfMarketCenters = (int) ByteBufferUtil.readAsciiLong(buffer, 2);
				ByteBufferUtil.advancePosition(buffer, numberOfMarketCenters * 24);

				this.sales.updateEndofDay(symbol, closingPrice, lowPrice, highPrice, consolidatedVolume, utpPacket.getTimestamp(), closingPriceExchange);
				writeClosePriceToFile(symbol, closingPrice);
			}
		}
		else if (utpPacket.getMessageCategory() == CATEGORY_CONTROL)
		{
			if (utpPacket.getMessageType() == TYPE_END_OF_CONSOLIDATED_LAST_SALE_ELIGIBILITY)
			{
				this.receivedEndOfLastSaleEligibleControlMessage = true;
			}
			LOGGER.info(processorName + " - Received Control Message Type=" + utpPacket.getMessageType());
		}
	}

	private int getSaleConditions(String saleConditions, Sale previousSale, boolean isPrimary)
	{
		int conditionCode = 0;
		boolean marketCenterClose = false;
		boolean marketCenterCloseUpdate = false;
		boolean marketCenterOpen = false;
		for (int i = 0; i < saleConditions.length(); i++)
		{
			char saleCondition = saleConditions.charAt(i);
			marketCenterClose |= saleCondition == 'M';
			marketCenterCloseUpdate |= saleCondition == '9';
			marketCenterOpen |= (saleCondition == 'Q');
			int charCondition = getCharSaleCondition(saleCondition, previousSale);
			if (charCondition > 0)
			{
				if (conditionCode > 0) conditionCode &= charCondition;
				else conditionCode = charCondition;
			}
		}
		conditionCode = ((marketCenterClose && (previousSale == null || previousSale.getLatestClosePrice() == 0)) || marketCenterCloseUpdate) ? MdEntity.setCondition(
				conditionCode, Sale.CONDITION_CODE_LATEST_CLOSE) : conditionCode;
		conditionCode = (marketCenterOpen && isPrimary) ? MdEntity.setCondition(conditionCode, Sale.CONDITION_CODE_OPEN) : conditionCode;
		return conditionCode;
	}

	private int getCharSaleCondition(char charSaleCondition, Sale previousSale)
	{
		boolean noteOne = (previousSale == null || previousSale.getPrice() == 0);
		boolean noteTwo = !this.receivedEndOfLastSaleEligibleControlMessage;

		switch (charSaleCondition)
		{
			case '@':
				return MdEntity.setCondition(0, Sale.CONDITION_CODE_VWAP, Sale.CONDITION_CODE_VOLUME, Sale.CONDITION_CODE_HIGH, Sale.CONDITION_CODE_LOW, Sale.CONDITION_CODE_LAST);
			case 'A':
				return MdEntity.setCondition(0, Sale.CONDITION_CODE_VWAP, Sale.CONDITION_CODE_VOLUME, Sale.CONDITION_CODE_HIGH, Sale.CONDITION_CODE_LOW, Sale.CONDITION_CODE_LAST);
			case 'B':
				return MdEntity.setCondition(0, Sale.CONDITION_CODE_VWAP, Sale.CONDITION_CODE_VOLUME, Sale.CONDITION_CODE_HIGH, Sale.CONDITION_CODE_LOW, Sale.CONDITION_CODE_LAST);
			case 'C':
				return MdEntity.setCondition(0, Sale.CONDITION_CODE_VWAP, Sale.CONDITION_CODE_VOLUME);
			case 'D':
				return MdEntity.setCondition(0, Sale.CONDITION_CODE_VWAP, Sale.CONDITION_CODE_VOLUME, Sale.CONDITION_CODE_HIGH, Sale.CONDITION_CODE_LOW, Sale.CONDITION_CODE_LAST);
			case 'F':
				return MdEntity.setCondition(0, Sale.CONDITION_CODE_VWAP, Sale.CONDITION_CODE_VOLUME, Sale.CONDITION_CODE_HIGH, Sale.CONDITION_CODE_LOW, Sale.CONDITION_CODE_LAST);
			case 'G':
				if (noteOne)
					return MdEntity.setCondition(0, Sale.CONDITION_CODE_VWAP, Sale.CONDITION_CODE_VOLUME, Sale.CONDITION_CODE_HIGH, Sale.CONDITION_CODE_LOW,
							Sale.CONDITION_CODE_LAST);
				return MdEntity.setCondition(0, Sale.CONDITION_CODE_VWAP, Sale.CONDITION_CODE_VOLUME, Sale.CONDITION_CODE_HIGH, Sale.CONDITION_CODE_LOW);
			case 'H':
				return MdEntity.setCondition(0, Sale.CONDITION_CODE_VOLUME);
			case 'I':
				return MdEntity.setCondition(0, Sale.CONDITION_CODE_VWAP, Sale.CONDITION_CODE_VOLUME);
			case 'K':
				return MdEntity.setCondition(0, Sale.CONDITION_CODE_VWAP, Sale.CONDITION_CODE_VOLUME, Sale.CONDITION_CODE_HIGH, Sale.CONDITION_CODE_LOW, Sale.CONDITION_CODE_LAST);
			case 'L':
				if (noteTwo)
					return MdEntity.setCondition(0, Sale.CONDITION_CODE_VWAP, Sale.CONDITION_CODE_VOLUME, Sale.CONDITION_CODE_HIGH, Sale.CONDITION_CODE_LOW,
							Sale.CONDITION_CODE_LAST);
				return MdEntity.setCondition(0, Sale.CONDITION_CODE_VWAP, Sale.CONDITION_CODE_VOLUME, Sale.CONDITION_CODE_HIGH, Sale.CONDITION_CODE_LOW);
			case 'N':
				return MdEntity.setCondition(0, Sale.CONDITION_CODE_VOLUME);
			case 'O':
				return MdEntity.setCondition(0, Sale.CONDITION_CODE_VWAP, Sale.CONDITION_CODE_VOLUME, Sale.CONDITION_CODE_HIGH, Sale.CONDITION_CODE_LOW, Sale.CONDITION_CODE_LAST);
			case 'P':
				if (noteOne)
					return MdEntity.setCondition(0, Sale.CONDITION_CODE_VWAP, Sale.CONDITION_CODE_VOLUME, Sale.CONDITION_CODE_HIGH, Sale.CONDITION_CODE_LOW,
							Sale.CONDITION_CODE_LAST);
				return MdEntity.setCondition(0, Sale.CONDITION_CODE_VWAP, Sale.CONDITION_CODE_VOLUME, Sale.CONDITION_CODE_HIGH, Sale.CONDITION_CODE_LOW);
			case 'R':
				return MdEntity.setCondition(0, Sale.CONDITION_CODE_VWAP, Sale.CONDITION_CODE_VOLUME);
			case 'S':
				return MdEntity.setCondition(0, Sale.CONDITION_CODE_VWAP, Sale.CONDITION_CODE_VOLUME, Sale.CONDITION_CODE_HIGH, Sale.CONDITION_CODE_LOW, Sale.CONDITION_CODE_LAST);
			case 'T':
				return MdEntity.setCondition(0, Sale.CONDITION_CODE_VWAP, Sale.CONDITION_CODE_VOLUME, Sale.CONDITION_CODE_LAST);
			case 'U':
				return MdEntity.setCondition(0, Sale.CONDITION_CODE_VWAP, Sale.CONDITION_CODE_VOLUME, Sale.CONDITION_CODE_LAST);
			case 'V':
				return MdEntity.setCondition(0, Sale.CONDITION_CODE_VWAP, Sale.CONDITION_CODE_VOLUME, Sale.CONDITION_CODE_HIGH, Sale.CONDITION_CODE_LOW, Sale.CONDITION_CODE_LAST);
			case 'W':
				return MdEntity.setCondition(0, Sale.CONDITION_CODE_VOLUME);
			case 'X':
				return MdEntity.setCondition(0, Sale.CONDITION_CODE_VWAP, Sale.CONDITION_CODE_VOLUME, Sale.CONDITION_CODE_HIGH, Sale.CONDITION_CODE_LOW, Sale.CONDITION_CODE_LAST);
			case 'Y':
				return MdEntity.setCondition(0, Sale.CONDITION_CODE_VWAP, Sale.CONDITION_CODE_VOLUME, Sale.CONDITION_CODE_HIGH, Sale.CONDITION_CODE_LOW, Sale.CONDITION_CODE_LAST);
			case 'Z':
				if (noteOne)
					return MdEntity.setCondition(0, Sale.CONDITION_CODE_VWAP, Sale.CONDITION_CODE_VOLUME, Sale.CONDITION_CODE_HIGH, Sale.CONDITION_CODE_LOW,
							Sale.CONDITION_CODE_LAST);
				return MdEntity.setCondition(0, Sale.CONDITION_CODE_VWAP, Sale.CONDITION_CODE_VOLUME, Sale.CONDITION_CODE_HIGH, Sale.CONDITION_CODE_LOW);
			case '1':
				return MdEntity.setCondition(0, Sale.CONDITION_CODE_VWAP, Sale.CONDITION_CODE_VOLUME, Sale.CONDITION_CODE_HIGH, Sale.CONDITION_CODE_LOW, Sale.CONDITION_CODE_LAST);
			case '2':
				if (noteTwo)
					return MdEntity.setCondition(0, Sale.CONDITION_CODE_VWAP, Sale.CONDITION_CODE_VOLUME, Sale.CONDITION_CODE_HIGH, Sale.CONDITION_CODE_LOW,
							Sale.CONDITION_CODE_LAST);
				return MdEntity.setCondition(0, Sale.CONDITION_CODE_VWAP, Sale.CONDITION_CODE_VOLUME, Sale.CONDITION_CODE_HIGH, Sale.CONDITION_CODE_LOW);
			case '3':
				if (noteOne)
					return MdEntity.setCondition(0, Sale.CONDITION_CODE_VWAP, Sale.CONDITION_CODE_VOLUME, Sale.CONDITION_CODE_HIGH, Sale.CONDITION_CODE_LOW,
							Sale.CONDITION_CODE_LAST);
				return MdEntity.setCondition(0, Sale.CONDITION_CODE_VWAP, Sale.CONDITION_CODE_VOLUME, Sale.CONDITION_CODE_HIGH, Sale.CONDITION_CODE_LOW);
			case '4':
				if (noteOne)
					return MdEntity.setCondition(0, Sale.CONDITION_CODE_VWAP, Sale.CONDITION_CODE_VOLUME, Sale.CONDITION_CODE_HIGH, Sale.CONDITION_CODE_LOW,
							Sale.CONDITION_CODE_LAST);
				return MdEntity.setCondition(0, Sale.CONDITION_CODE_VWAP, Sale.CONDITION_CODE_VOLUME, Sale.CONDITION_CODE_HIGH, Sale.CONDITION_CODE_LOW);
			case '5':
				return MdEntity.setCondition(0, Sale.CONDITION_CODE_VWAP, Sale.CONDITION_CODE_VOLUME, Sale.CONDITION_CODE_HIGH, Sale.CONDITION_CODE_LOW, Sale.CONDITION_CODE_LAST);
			case '6':
				return MdEntity.setCondition(0, Sale.CONDITION_CODE_VWAP, Sale.CONDITION_CODE_VOLUME, Sale.CONDITION_CODE_HIGH, Sale.CONDITION_CODE_LOW, Sale.CONDITION_CODE_LAST);
			default:
				return 0;
		}
	}

	private void writeClosePriceToFile(String symbol, double price)
	{
		try
		{
			if (this.closePriceWriter == null)
			{
				String filePrefix = (String) MdFeedProps.getInstanceProperty(MdFeed.UTDF.toString(), "CLOSE_PRICE_FILE_PREFIX");
				if (filePrefix != null)
				{
					File file = new File(filePrefix + "_" + this.range);
					if (file.exists())
					{
						file.delete();
					}
					file.createNewFile();
					this.closePriceWriter = new BufferedWriter(new FileWriter(file));
				}
				else
				{
					LOGGER.severe("Unable to find CLOSE_PRICE_FILE_PREFIX - Unable to write closePrice-" + symbol + "," + price);
				}
			}

			this.closePriceWriter.write(symbol);
			this.closePriceWriter.write(",");
			this.closePriceWriter.write(Double.valueOf(price).toString());
			this.closePriceWriter.newLine();
			this.closePriceWriter.flush();
		}
		catch (Exception e)
		{
			LOGGER.severe("Exception:" + e.getMessage() + " - Unable to write closePrice-" + symbol + "," + price);
		}
	}
}

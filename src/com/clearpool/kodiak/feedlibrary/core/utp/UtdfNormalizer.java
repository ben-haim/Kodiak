package com.clearpool.kodiak.feedlibrary.core.utp;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
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
	private final byte[] tmpBuffer4;
	private final byte[] tmpBuffer5;
	private final byte[] tmpBuffer11;

	private boolean receivedEndOfLastSaleEligibleControlMessage;
	private BufferedWriter closePriceWriter;

	public UtdfNormalizer(Map<MdServiceType, IMdLibraryCallback> callbacks, String range)
	{
		this.range = range;
		this.sales = new SaleCache((IMdSaleListener) callbacks.get(MdServiceType.SALE), MdFeed.UTDF, range, false);
		this.tmpBuffer4 = new byte[4];
		this.tmpBuffer5 = new byte[5];
		this.tmpBuffer11 = new byte[11];

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
						prices.put(split[0], Double.valueOf(split[1]));
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

					for (Entry<String, Double> entry : prices.entrySet())
					{
						String symbol = entry.getKey();
						if (firstRange.compareTo(symbol) <= 0 && symbol.compareTo(secondRange) <= 0)
						{
							this.sales.setLatestClosePrice(symbol, Exchange.USEQ_SIP, entry.getValue().doubleValue(), DateUtil.TODAY_MIDNIGHT_EST.getTime(), "SDS");
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
		char msgCategory = utpPacket.getMessageCategory();
		char msgType = utpPacket.getMessageType();
		char participantId = utpPacket.getParticipantId();
		long timestamp = utpPacket.getTimestamp();
		ByteBuffer buffer = utpPacket.getBuffer();

		if (msgCategory == CATEGORY_TRADE)
		{
			if (msgType == TYPE_SHORT_TRADE || msgType == TYPE_LONG_TRADE)
			{
				boolean isLong = msgType == TYPE_LONG_TRADE;
				String symbol = ByteBufferUtil.getString(buffer, isLong ? this.tmpBuffer11 : this.tmpBuffer5);
				if (isLong) ByteBufferUtil.advancePosition(buffer, 1); // trade through exempt flag
				String saleCondition = isLong ? ByteBufferUtil.getString(buffer, this.tmpBuffer4) : String.valueOf((char) buffer.get());
				if (isLong) ByteBufferUtil.advancePosition(buffer, 2); // seller's sale days
				char priceDenominator = (char) buffer.get();
				double price = UtpUtils.getPrice(ByteBufferUtil.readAsciiLong(buffer, isLong ? 10 : 6), priceDenominator);
				if (isLong) ByteBufferUtil.advancePosition(buffer, 3); // Currency
				int size = (int) ByteBufferUtil.readAsciiLong(buffer, isLong ? 9 : 6);
				ByteBufferUtil.advancePosition(buffer, 1); // consolidated price change indicator

				this.sales.updateWithSaleCondition(symbol, price, size, UtpUtils.getExchange(participantId), timestamp,
						getSaleConditions(saleCondition, this.sales.getData(symbol), participantId == NASDAQ_PARTICIPANT), saleCondition);
			}
			else if (msgType == TYPE_CORRECTION)
			{
				long originalSequenceNumber = ByteBufferUtil.readAsciiLong(buffer, 8);
				String symbol = ByteBufferUtil.getString(buffer, this.tmpBuffer11);
				ByteBufferUtil.advancePosition(buffer, 1); // trade through exmept flag
				String originalSaleCondition = ByteBufferUtil.getString(buffer, this.tmpBuffer4);
				ByteBufferUtil.advancePosition(buffer, 2); // seller's sale days
				char originalPriceDenominator = (char) buffer.get();
				double originalPrice = UtpUtils.getPrice(ByteBufferUtil.readAsciiLong(buffer, 10), originalPriceDenominator);
				ByteBufferUtil.advancePosition(buffer, 3); // Currency
				int originalSize = (int) ByteBufferUtil.readAsciiLong(buffer, 9);
				ByteBufferUtil.advancePosition(buffer, 1); // trade through exmept flag
				String correctedSaleCondition = ByteBufferUtil.getString(buffer, this.tmpBuffer4);
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

				this.sales.correctWithStats(symbol, originalPrice, originalSize, getSaleConditions(originalSaleCondition, null, participantId == NASDAQ_PARTICIPANT),
						correctedPrice, correctedSize, getSaleConditions(correctedSaleCondition, null, participantId == NASDAQ_PARTICIPANT), timestamp, lastExchange, lastPrice,
						highPrice, lowPrice, -1, consolidatedVolume);
				LOGGER.info(processorName + " - Received Correction Message - Symbol=" + symbol + " orig=" + originalPrice + "@" + originalSize + " (origiSeqNo="
						+ originalSequenceNumber + ") corrected=" + correctedPrice + "@" + correctedSize);
			}
			else if (msgType == TYPE_CANCEL_ERROR)
			{
				long originalSequenceNumber = ByteBufferUtil.readAsciiLong(buffer, 8);
				String symbol = ByteBufferUtil.getString(buffer, this.tmpBuffer11);
				ByteBufferUtil.advancePosition(buffer, 2); // function, trade through exmept flag
				String originalSaleCondition = ByteBufferUtil.getString(buffer, this.tmpBuffer4);
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

				this.sales.cancelWithStats(symbol, originalPrice, originalSize, getSaleConditions(originalSaleCondition, null, participantId == NASDAQ_PARTICIPANT), timestamp,
						lastExchange, lastPrice, highPrice, lowPrice, -1, consolidatedVolume);
				LOGGER.info(processorName + " - Received Cancel Message - Symbol=" + symbol + " orig=" + originalPrice + "@" + originalSize + " (origiSeqNo="
						+ originalSequenceNumber + ")");
			}
		}
		else if (msgCategory == CATEGORY_ADMINISTRATIVE)
		{
			if (msgType == TYPE_ADMIN_MESSAGE)
			{
				String message = ByteBufferUtil.getUnboundedString(buffer, buffer.remaining());
				LOGGER.info(processorName + " - Received Admin Message - " + message);
				if (message.startsWith("IPO PRICE AT"))
				{
					String[] spaceSplit = message.split(" ");
					String priceString = spaceSplit[5];
					this.sales.setLatestClosePrice(spaceSplit[4], Exchange.USEQ_SIP, Integer.valueOf(priceString.substring(0, priceString.indexOf("."))).intValue() / 100d,
							timestamp, "SDS");
				}
			}
			else if (msgType == TYPE_CLOSING_TRADE_SUMMARY_REPORT)
			{
				String symbol = ByteBufferUtil.getString(buffer, this.tmpBuffer11);
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

				this.sales.updateEndofDay(symbol, closingPrice, lowPrice, highPrice, consolidatedVolume, timestamp, closingPriceExchange);
				writeClosePriceToFile(symbol, closingPrice);
			}
		}
		else if (msgCategory == CATEGORY_CONTROL)
		{
			if (msgType == TYPE_END_OF_CONSOLIDATED_LAST_SALE_ELIGIBILITY) this.receivedEndOfLastSaleEligibleControlMessage = true;
			LOGGER.info(processorName + " - Received Control Message Type=" + msgType);
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
			case 'A':
			case 'B':
			case 'D':
			case 'F':
			case 'K':
			case 'O':
			case 'S':
			case 'V':
			case 'X':
			case 'Y':
			case '1':
			case '5':
			case '6':
				return MdEntity.setCondition(0, Sale.CONDITION_CODE_VWAP, Sale.CONDITION_CODE_VOLUME, Sale.CONDITION_CODE_HIGH, Sale.CONDITION_CODE_LOW, Sale.CONDITION_CODE_LAST);
			case 'C':
			case 'I':
			case 'R':
				return MdEntity.setCondition(0, Sale.CONDITION_CODE_VWAP, Sale.CONDITION_CODE_VOLUME);
			case 'G':
			case 'P':
			case 'Z':
			case '3':
			case '4':
				if (noteOne)
					return MdEntity.setCondition(0, Sale.CONDITION_CODE_VWAP, Sale.CONDITION_CODE_VOLUME, Sale.CONDITION_CODE_HIGH, Sale.CONDITION_CODE_LOW,
							Sale.CONDITION_CODE_LAST);
				return MdEntity.setCondition(0, Sale.CONDITION_CODE_VWAP, Sale.CONDITION_CODE_VOLUME, Sale.CONDITION_CODE_HIGH, Sale.CONDITION_CODE_LOW);
			case 'H':
			case 'N':
			case 'W':
				return MdEntity.setCondition(0, Sale.CONDITION_CODE_VOLUME);
			case 'L':
			case '2':
				if (noteTwo)
					return MdEntity.setCondition(0, Sale.CONDITION_CODE_VWAP, Sale.CONDITION_CODE_VOLUME, Sale.CONDITION_CODE_HIGH, Sale.CONDITION_CODE_LOW,
							Sale.CONDITION_CODE_LAST);
				return MdEntity.setCondition(0, Sale.CONDITION_CODE_VWAP, Sale.CONDITION_CODE_VOLUME, Sale.CONDITION_CODE_HIGH, Sale.CONDITION_CODE_LOW);
			case 'T':
			case 'U':
				return MdEntity.setCondition(0, Sale.CONDITION_CODE_VWAP, Sale.CONDITION_CODE_VOLUME, Sale.CONDITION_CODE_LAST);
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

			if (this.closePriceWriter != null)
			{
				this.closePriceWriter.write(symbol);
				this.closePriceWriter.write(",");
				this.closePriceWriter.write(Double.valueOf(price).toString());
				this.closePriceWriter.newLine();
				this.closePriceWriter.flush();
			}
		}
		catch (Exception e)
		{
			LOGGER.severe("Exception:" + e.getMessage() + " - Unable to write closePrice-" + symbol + "," + price);
		}
	}
}

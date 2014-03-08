package com.clearpool.kodiak.feedlibrary.core.cta;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.logging.Logger;

import com.clearpool.kodiak.feedlibrary.caches.IMdServiceCache;
import com.clearpool.kodiak.feedlibrary.caches.SaleCache;
import com.clearpool.kodiak.feedlibrary.callbacks.IMdLibraryCallback;
import com.clearpool.kodiak.feedlibrary.callbacks.IMdSaleListener;
import com.clearpool.kodiak.feedlibrary.core.IMdNormalizer;
import com.clearpool.kodiak.feedlibrary.core.MdFeed;
import com.clearpool.kodiak.feedlibrary.core.MdFeedPacket;
import com.clearpool.kodiak.feedlibrary.utils.ByteBufferUtil;
import com.clearpool.messageobjects.marketdata.Exchange;
import com.clearpool.messageobjects.marketdata.MdEntity;
import com.clearpool.messageobjects.marketdata.MdServiceType;
import com.clearpool.messageobjects.marketdata.Sale;

public class CtsNormalizer implements IMdNormalizer
{
	private static final Logger LOGGER = Logger.getLogger(CtsNormalizer.class.getName());

	private static final char CATEGORY_ADMIN = 'A';
	private static final char CATEGORY_BOND = 'B';
	private static final char CATEGORY_EQUITY = 'E';
	private static final char CATEGORY_LOCAL = 'L';
	private static final char TYPE_START_OF_DAY_SUMMARY = 'O';
	private static final char TYPE_CONSOLIDATED_END_OF_DAY_SUMMARY = 'S';
	private static final char TYPE_SHORT_TRADE = 'I';
	private static final char TYPE_LONG_TRADE = 'B';
	private static final char TYPE_CORRECTION = 'P';
	private static final char TYPE_CANCEL = 'Q';
	private static final char TYPE_ADMIN_UNFORMATTED_TEXT = 'H';

	private final SaleCache sales;

	public CtsNormalizer(Map<MdServiceType, IMdLibraryCallback> callbacks, String range)
	{
		this.sales = new SaleCache((IMdSaleListener) callbacks.get(MdServiceType.SALE), MdFeed.CTS, range, false);
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
		CtaPacket ctaPacket = (CtaPacket) packet;
		ByteBuffer buffer = ctaPacket.getBuffer();
		if (ctaPacket.getMessageCategory() == CATEGORY_BOND || ctaPacket.getMessageCategory() == CATEGORY_LOCAL) return;

		if (ctaPacket.getMessageCategory() == CATEGORY_EQUITY)
		{
			if (ctaPacket.getMessageType() == TYPE_SHORT_TRADE || ctaPacket.getMessageType() == TYPE_LONG_TRADE)
			{
				boolean isLong = ctaPacket.getMessageType() == TYPE_LONG_TRADE;
				String symbol = ByteBufferUtil.getString(buffer, isLong ? 11 : 3).trim();
				if (isLong) ByteBufferUtil.advancePosition(buffer, 3); // suffix, test message indicator, trade reporting facility
				char primaryListing = isLong ? (char) buffer.get() : ((ctaPacket.getMessageNetwork() == 'A') ? 'N' : 'A');
				primaryListing = (primaryListing == ' ') ? ((ctaPacket.getMessageNetwork() == 'A') ? 'N' : 'A') : primaryListing;
				if (isLong) ByteBufferUtil.advancePosition(buffer, 10); // reserved, financial status, currency, held trade indicator, instrument type, seller's sale days
				String saleConditions = ByteBufferUtil.getString(buffer, isLong ? 4 : 1);

				char priceDenominatorIndicator;
				long tradePrice;
				int tradeVolume;
				if (isLong)
				{
					ByteBufferUtil.advancePosition(buffer, 3); // trade through exempt indicator, ssr indicator, reserved
					priceDenominatorIndicator = (char) buffer.get();
					tradePrice = ByteBufferUtil.readAsciiLong(buffer, 12);
					tradeVolume = (int) ByteBufferUtil.readAsciiLong(buffer, 9);
					ByteBufferUtil.advancePosition(buffer, 4); // consolidated high/low/last price indicator, participant open/high/low/last price indicator, reserved, stop stock indicator
				}
				else
				{
					tradeVolume = (int) ByteBufferUtil.readAsciiLong(buffer, 4);
					priceDenominatorIndicator = (char) buffer.get();
					tradePrice = ByteBufferUtil.readAsciiLong(buffer, 8);
					ByteBufferUtil.advancePosition(buffer, 3); // consolidated high/low/last price indicator, participant open/high/low/last price indicator, reserved
				}
				double price = CtaUtils.getPrice(tradePrice, priceDenominatorIndicator);
				Exchange exchange = CtaUtils.getExchange(ctaPacket.getParticipantId(), null);
				Sale previousSale = this.sales.getData(symbol);
				int conditionCode = getSaleConditions(saleConditions, previousSale, ctaPacket.getParticipantId(), ctaPacket.getParticipantId() == primaryListing);
				this.sales.updateWithSaleCondition(symbol, price, tradeVolume, exchange, ctaPacket.getTimestamp(), conditionCode, saleConditions);
			}
			else if (ctaPacket.getMessageType() == TYPE_CORRECTION)
			{
				ByteBufferUtil.advancePosition(buffer, 5); // reserved
				char primaryListing = (char) buffer.get();
				ByteBufferUtil.advancePosition(buffer, 1); // trade reporting facility
				String symbol = ByteBufferUtil.getString(buffer, 11).trim();
				ByteBufferUtil.advancePosition(buffer, 6); // temporary suffix, financial status, currency, instrument type,
				long sequenceNumber = ByteBufferUtil.readAsciiLong(buffer, 9);
				ByteBufferUtil.advancePosition(buffer, 4); // reserved, seller's sale days
				String originalSaleCondition = ByteBufferUtil.getString(buffer, 4);
				char originalPriceDenominatorIndicator = (char) buffer.get();
				double originalPrice = CtaUtils.getPrice(ByteBufferUtil.readAsciiLong(buffer, 12), originalPriceDenominatorIndicator);
				int originalTradeVolume = (int) ByteBufferUtil.readAsciiLong(buffer, 9);
				ByteBufferUtil.advancePosition(buffer, 14); // trade through exempt indicator, ssr indicator, reserved, seller's sale days
				String correctedSaleCondition = ByteBufferUtil.getString(buffer, 4);
				char correctedPriceDenominatorIndicator = (char) buffer.get();
				double correctedPrice = CtaUtils.getPrice(ByteBufferUtil.readAsciiLong(buffer, 12), correctedPriceDenominatorIndicator);
				int correctedTradeVolume = (int) ByteBufferUtil.readAsciiLong(buffer, 9);
				ByteBufferUtil.advancePosition(buffer, 11); // stop stock indicator, trade through exempt indicator, ssr indicator, reserved
				Exchange lastExchange = CtaUtils.getExchange((char) buffer.get(), null);
				char lastPriceDenominatorIndicator = (char) buffer.get();
				double lastPrice = CtaUtils.getPrice(ByteBufferUtil.readAsciiLong(buffer, 12), lastPriceDenominatorIndicator);
				ByteBufferUtil.advancePosition(buffer, 6); // previous close price date
				char highPriceDenominatorIndicator = (char) buffer.get();
				double highPrice = CtaUtils.getPrice(ByteBufferUtil.readAsciiLong(buffer, 12), highPriceDenominatorIndicator);
				char lowPriceDenominatorIndicator = (char) buffer.get();
				double lowPrice = CtaUtils.getPrice(ByteBufferUtil.readAsciiLong(buffer, 12), lowPriceDenominatorIndicator);
				long totalVolume = ByteBufferUtil.readAsciiLong(buffer, 11);
				ByteBufferUtil.advancePosition(buffer, 42); // reserved, last price denominator, last price, previous close price date, total volume, tick
				char openPriceDenominatorIndicator = (char) buffer.get();
				double openPrice = CtaUtils.getPrice(ByteBufferUtil.readAsciiLong(buffer, 12), openPriceDenominatorIndicator);
				ByteBufferUtil.advancePosition(buffer, 38); // high price denominator, high price, low price denominator, low price, reserved

				int originalConditionCode = getSaleConditions(originalSaleCondition, null, ctaPacket.getParticipantId(), primaryListing == ctaPacket.getParticipantId());
				int correctedConditionCode = getSaleConditions(correctedSaleCondition, null, ctaPacket.getParticipantId(), primaryListing == ctaPacket.getParticipantId());
				this.sales.correctWithStats(symbol, originalPrice, originalTradeVolume, originalConditionCode, correctedPrice, correctedTradeVolume, correctedConditionCode,
						ctaPacket.getTimestamp(), lastExchange, lastPrice, highPrice, lowPrice, openPrice, totalVolume);
				LOGGER.info(processorName + " - Received Correction Message - Symbol=" + symbol + " orig=" + originalPrice + "@" + originalTradeVolume + " (origiSeqNo="
						+ sequenceNumber + ") corrected=" + correctedPrice + "@" + correctedTradeVolume);
			}
			else if (ctaPacket.getMessageType() == TYPE_CANCEL)
			{
				ByteBufferUtil.advancePosition(buffer, 5); // reserved
				char primaryListing = (char) buffer.get();
				ByteBufferUtil.advancePosition(buffer, 1); // trade reporting facility
				String symbol = ByteBufferUtil.getString(buffer, 11).trim();
				ByteBufferUtil.advancePosition(buffer, 7); // temporary suffix, financial status, currency, instrument type, cancel/error action
				long sequenceNumber = ByteBufferUtil.readAsciiLong(buffer, 9);
				ByteBufferUtil.advancePosition(buffer, 3); // seller's sale days
				String originalSaleCondition = ByteBufferUtil.getString(buffer, 4);
				char originalPriceDenominatorIndicator = (char) buffer.get();
				double originalPrice = CtaUtils.getPrice(ByteBufferUtil.readAsciiLong(buffer, 12), originalPriceDenominatorIndicator);
				int originalTradeVolume = (int) ByteBufferUtil.readAsciiLong(buffer, 9);
				ByteBufferUtil.advancePosition(buffer, 11); // stop stock indicator, trade through exempt indicator, ssr indicator, reserved
				Exchange lastExchange = CtaUtils.getExchange((char) buffer.get(), null);
				char lastPriceDenominatorIndicator = (char) buffer.get();
				double lastPrice = CtaUtils.getPrice(ByteBufferUtil.readAsciiLong(buffer, 12), lastPriceDenominatorIndicator);
				ByteBufferUtil.advancePosition(buffer, 6); // previous close price date
				char highPriceDenominatorIndicator = (char) buffer.get();
				double highPrice = CtaUtils.getPrice(ByteBufferUtil.readAsciiLong(buffer, 12), highPriceDenominatorIndicator);
				char lowPriceDenominatorIndicator = (char) buffer.get();
				double lowPrice = CtaUtils.getPrice(ByteBufferUtil.readAsciiLong(buffer, 12), lowPriceDenominatorIndicator);
				long totalVolume = ByteBufferUtil.readAsciiLong(buffer, 11);
				ByteBufferUtil.advancePosition(buffer, 42); // reserved, last price denominator, last price, previous close price date, total volume, tick
				char openPriceDenominatorIndicator = (char) buffer.get();
				double openPrice = CtaUtils.getPrice(ByteBufferUtil.readAsciiLong(buffer, 12), openPriceDenominatorIndicator);
				ByteBufferUtil.advancePosition(buffer, 38); // high price denominator, high price, low price denominator, low price, reserved

				int originalConditionCode = getSaleConditions(originalSaleCondition, null, ctaPacket.getParticipantId(), primaryListing == ctaPacket.getParticipantId());
				this.sales.cancelWithStats(symbol, originalPrice, originalTradeVolume, originalConditionCode, ctaPacket.getTimestamp(), lastExchange, lastPrice, highPrice,
						lowPrice, openPrice, totalVolume);
				LOGGER.info(processorName + " - Received Cancel Message - Symbol=" + symbol + " orig=" + originalPrice + "@" + originalTradeVolume + " (origiSeqNo="
						+ sequenceNumber + ")");
			}
			else if (ctaPacket.getMessageType() == TYPE_CONSOLIDATED_END_OF_DAY_SUMMARY)
			{
				String symbol = ByteBufferUtil.getString(buffer, 11).trim();
				ByteBufferUtil.advancePosition(buffer, 17); // temporary suffix, financial status, currency, instrument type, ssr, reserved
				Exchange exchange = CtaUtils.getExchange((char) buffer.get(), null);
				char closePriceDenominatorIndicator = (char) buffer.get();
				double closePrice = CtaUtils.getPrice(ByteBufferUtil.readAsciiLong(buffer, 12), closePriceDenominatorIndicator);
				ByteBufferUtil.advancePosition(buffer, 6); // previous close price date
				char highDenominatorIndicator = (char) buffer.get();
				double highPrice = CtaUtils.getPrice(ByteBufferUtil.readAsciiLong(buffer, 12), highDenominatorIndicator);
				char lowDenominatorIndicator = (char) buffer.get();
				double lowPrice = CtaUtils.getPrice(ByteBufferUtil.readAsciiLong(buffer, 12), lowDenominatorIndicator);
				long totalVolume = ByteBufferUtil.readAsciiLong(buffer, 11);
				ByteBufferUtil.advancePosition(buffer, 13); // reserved, numberParticipants

				this.sales.updateEndofDay(symbol, closePrice, lowPrice, highPrice, totalVolume, ctaPacket.getTimestamp(), exchange);
			}
			else if (ctaPacket.getMessageType() == TYPE_START_OF_DAY_SUMMARY)
			{
				String symbol = ByteBufferUtil.getString(buffer, 11).trim();
				ByteBufferUtil.advancePosition(buffer, 17); // temporary suffix, financial status, currency, instrument type, ssr, reserved
				Exchange exchange = CtaUtils.getExchange((char) buffer.get(), null);
				char previousClosePriceDenominatorIndicator = (char) buffer.get();
				double previousClosePrice = CtaUtils.getPrice(ByteBufferUtil.readAsciiLong(buffer, 12), previousClosePriceDenominatorIndicator);
				ByteBufferUtil.advancePosition(buffer, 16);
				int numberOfIterations = (int) ByteBufferUtil.readAsciiLong(buffer, 2);
				ByteBufferUtil.advancePosition(buffer, numberOfIterations * 30);

				this.sales.setLatestClosePrice(symbol, exchange, previousClosePrice, ctaPacket.getTimestamp(), "SDS");
			}
		}
		else if (ctaPacket.getMessageCategory() == CATEGORY_ADMIN)
		{
			if (ctaPacket.getMessageType() == TYPE_ADMIN_UNFORMATTED_TEXT)
			{
				String message = ByteBufferUtil.getString(buffer, buffer.remaining()).trim();
				if (message.startsWith("ALERT ALERT ALERT"))
				{
					LOGGER.info(processorName + " - Exception - Received Admin Message - " + message);
				}
				else
				{
					LOGGER.info(processorName + " - Received Admin Message - " + message);
					// String ipoSymbolString = "TICKER/SYMBOL//";
					// String ipoPriceString = "INITIAL/PUBLIC/OFFERING/PRICE$";
					// if (message.contains("TRADING/BEGINS") && message.contains(ipoPriceString))
					// {
					// int indexStartSymbol = message.indexOf(ipoSymbolString);
					// int indexEndSymbol = message.indexOf("//POST8SEC/");
					// String symbol = message.substring(indexStartSymbol + ipoSymbolString.length(), indexEndSymbol);
					// int indexStartPrice = message.indexOf(ipoPriceString);
					// int indexEndPrice = message.indexOf("SETTLEMENT");
					// double price = Double.valueOf(message.substring(indexStartPrice + ipoPriceString.length(), indexEndPrice)).doubleValue();
					//
					// this.sales.setLatestClosePrice(symbol, Exchange.USEQ_SIP, price, ctaPacket.getTimestamp(), "SDS");
					// }
				}
			}
		}
	}

	private static int getSaleConditions(String saleConditions, Sale previousSale, char participantId, boolean isPrimary)
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
			marketCenterOpen |= (saleCondition == 'O');
			int charCondition = getCharSaleCondition(saleCondition, previousSale, participantId, isPrimary);
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

	private static int getCharSaleCondition(char charSaleCondition, Sale previousSale, char participantId, boolean isPrimary)
	{
		boolean noteOne = (previousSale == null || previousSale.getPrice() == 0);
		boolean noteTwo = (previousSale == null || previousSale.getPrice() == 0);
		boolean noteThree = (previousSale == null || previousSale.getPrice() == 0 || previousSale.getExchange() == CtaUtils.getExchange(participantId, null) || isPrimary);

		switch (charSaleCondition)
		{
			case ' ':
				return 0;
			case '@':
				return MdEntity.setCondition(0, Sale.CONDITION_CODE_VWAP, Sale.CONDITION_CODE_VOLUME, Sale.CONDITION_CODE_HIGH, Sale.CONDITION_CODE_LOW, Sale.CONDITION_CODE_LAST);
			case 'B':
				return MdEntity.setCondition(0, Sale.CONDITION_CODE_VOLUME);
			case 'C':
				return MdEntity.setCondition(0, Sale.CONDITION_CODE_VWAP, Sale.CONDITION_CODE_VOLUME);
			case 'E':
				return MdEntity.setCondition(0, Sale.CONDITION_CODE_VWAP, Sale.CONDITION_CODE_VOLUME, Sale.CONDITION_CODE_HIGH, Sale.CONDITION_CODE_LOW, Sale.CONDITION_CODE_LAST);
			case 'F':
				return MdEntity.setCondition(0, Sale.CONDITION_CODE_VWAP, Sale.CONDITION_CODE_VOLUME, Sale.CONDITION_CODE_HIGH, Sale.CONDITION_CODE_LOW, Sale.CONDITION_CODE_LAST);
			case 'H':
				return MdEntity.setCondition(0, Sale.CONDITION_CODE_VWAP, Sale.CONDITION_CODE_VOLUME);
			case 'I':
				return MdEntity.setCondition(0, Sale.CONDITION_CODE_VWAP, Sale.CONDITION_CODE_VOLUME);
			case 'K':
				return MdEntity.setCondition(0, Sale.CONDITION_CODE_VWAP, Sale.CONDITION_CODE_VOLUME, Sale.CONDITION_CODE_HIGH, Sale.CONDITION_CODE_LOW, Sale.CONDITION_CODE_LAST);
			case 'L':
				if (noteThree)
					return MdEntity.setCondition(0, Sale.CONDITION_CODE_VWAP, Sale.CONDITION_CODE_VOLUME, Sale.CONDITION_CODE_HIGH, Sale.CONDITION_CODE_LOW,
							Sale.CONDITION_CODE_LAST);
				return MdEntity.setCondition(0, Sale.CONDITION_CODE_VWAP, Sale.CONDITION_CODE_VOLUME, Sale.CONDITION_CODE_HIGH, Sale.CONDITION_CODE_LOW);
			case 'N':
				return MdEntity.setCondition(0, Sale.CONDITION_CODE_VOLUME);
			case 'O':
				if (noteOne)
					return MdEntity.setCondition(0, Sale.CONDITION_CODE_VWAP, Sale.CONDITION_CODE_VOLUME, Sale.CONDITION_CODE_HIGH, Sale.CONDITION_CODE_LOW,
							Sale.CONDITION_CODE_LAST);
				return MdEntity.setCondition(0, Sale.CONDITION_CODE_VWAP, Sale.CONDITION_CODE_VOLUME, Sale.CONDITION_CODE_HIGH, Sale.CONDITION_CODE_LOW);
			case 'P':
				if (noteTwo)
					return MdEntity.setCondition(0, Sale.CONDITION_CODE_VWAP, Sale.CONDITION_CODE_VOLUME, Sale.CONDITION_CODE_HIGH, Sale.CONDITION_CODE_LOW,
							Sale.CONDITION_CODE_LAST);
				return MdEntity.setCondition(0, Sale.CONDITION_CODE_VWAP, Sale.CONDITION_CODE_VOLUME, Sale.CONDITION_CODE_HIGH, Sale.CONDITION_CODE_LOW);
			case 'R':
				return MdEntity.setCondition(0, Sale.CONDITION_CODE_VWAP, Sale.CONDITION_CODE_VOLUME);
			case 'T':
				return MdEntity.setCondition(0, Sale.CONDITION_CODE_VWAP, Sale.CONDITION_CODE_VOLUME, Sale.CONDITION_CODE_LAST);
			case 'U':
				return MdEntity.setCondition(0, Sale.CONDITION_CODE_VWAP, Sale.CONDITION_CODE_VOLUME, Sale.CONDITION_CODE_LAST);
			case 'V':
				return MdEntity.setCondition(0, Sale.CONDITION_CODE_VWAP, Sale.CONDITION_CODE_VOLUME, Sale.CONDITION_CODE_HIGH, Sale.CONDITION_CODE_LOW, Sale.CONDITION_CODE_LAST);
			case 'X':
				return MdEntity.setCondition(0, Sale.CONDITION_CODE_VWAP, Sale.CONDITION_CODE_VOLUME, Sale.CONDITION_CODE_HIGH, Sale.CONDITION_CODE_LOW, Sale.CONDITION_CODE_LAST);
			case 'Z':
				if (noteTwo)
					return MdEntity.setCondition(0, Sale.CONDITION_CODE_VWAP, Sale.CONDITION_CODE_VOLUME, Sale.CONDITION_CODE_HIGH, Sale.CONDITION_CODE_LOW,
							Sale.CONDITION_CODE_LAST);
				return MdEntity.setCondition(0, Sale.CONDITION_CODE_VWAP, Sale.CONDITION_CODE_VOLUME, Sale.CONDITION_CODE_HIGH, Sale.CONDITION_CODE_LOW);
			case '4':
				if (noteTwo) MdEntity.setCondition(0, Sale.CONDITION_CODE_VOLUME, Sale.CONDITION_CODE_HIGH, Sale.CONDITION_CODE_LOW, Sale.CONDITION_CODE_LAST);
				return MdEntity.setCondition(0, Sale.CONDITION_CODE_VOLUME, Sale.CONDITION_CODE_HIGH, Sale.CONDITION_CODE_LOW);
			case '5':
				return MdEntity.setCondition(0, Sale.CONDITION_CODE_VWAP, Sale.CONDITION_CODE_VOLUME, Sale.CONDITION_CODE_HIGH, Sale.CONDITION_CODE_LOW, Sale.CONDITION_CODE_LAST);
			case '6':
				return MdEntity.setCondition(0, Sale.CONDITION_CODE_VWAP, Sale.CONDITION_CODE_VOLUME, Sale.CONDITION_CODE_HIGH, Sale.CONDITION_CODE_LOW, Sale.CONDITION_CODE_LAST);
			default:
				return 0;
		}
	}
}

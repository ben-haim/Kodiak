package com.clearpool.kodiak.feedlibrary.core.opra;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.logging.Logger;

import com.clearpool.commonserver.adapter.IMulticastAdapter;
import com.clearpool.kodiak.feedlibrary.caches.BboQuoteCache;
import com.clearpool.kodiak.feedlibrary.caches.IMdServiceCache;
import com.clearpool.kodiak.feedlibrary.caches.NbboQuoteCache;
import com.clearpool.kodiak.feedlibrary.caches.SaleCache;
import com.clearpool.kodiak.feedlibrary.callbacks.IMdLibraryCallback;
import com.clearpool.kodiak.feedlibrary.callbacks.IMdQuoteListener;
import com.clearpool.kodiak.feedlibrary.callbacks.IMdSaleListener;
import com.clearpool.kodiak.feedlibrary.core.IMdNormalizer;
import com.clearpool.kodiak.feedlibrary.core.MdFeed;
import com.clearpool.kodiak.feedlibrary.core.MdFeedPacket;
import com.clearpool.kodiak.feedlibrary.utils.ByteBufferUtil;
import com.clearpool.messageobjects.marketdata.Exchange;
import com.clearpool.messageobjects.marketdata.MdEntity;
import com.clearpool.messageobjects.marketdata.MdServiceType;
import com.clearpool.messageobjects.marketdata.Quote;
import com.clearpool.messageobjects.marketdata.Sale;

public class OpraNormalizer implements IMdNormalizer
{
	private static final Logger LOGGER = Logger.getLogger(OpraNormalizer.class.getName());

	private static final int MESSAGE_HEADER_SIZE = 4;
	private static final int MESSAGE_DATA_LENGTH_SIZE = 2;
	private static final char CATEGORY_SHORT_EQUITY_AND_INDEX_QUOTE = 'q';
	private static final char CATEGORY_LONG_EQUITY_AND_INDEX_QUOTE = 'k';
	private static final char CATEGORY_EQUITY_INDEX_LAST_SALE = 'a';
	private static final char CATEGORY_OPEN_INTEREST = 'd';
	private static final char CATEGORY_EQUITY_AND_INDEX_EOD_SUMMARY = 'f';
	private static final char CATEGORY_ADMINISTRATIVE = 'C';
	private static final char CATEGORY_CONTROL = 'H';
	private static final double[] POWERS = new double[] { 1E1, 1E2, 1E3, 1E4, 1E5, 1E6, 1E7, 1E8 };
	private static final Exchange[] EXCHANGES = new Exchange[26];

	private final NbboQuoteCache nbbos;
	private final BboQuoteCache bbos;
	private final SaleCache sales;
	private final byte[] tmpBuffer4;
	private final byte[] tmpBuffer5;

	public OpraNormalizer(Map<MdServiceType, IMdLibraryCallback> callbacks, String range, IMulticastAdapter multicastAdapter)
	{
		this.nbbos = new NbboQuoteCache((IMdQuoteListener) callbacks.get(MdServiceType.NBBO), MdFeed.OPRA, range, multicastAdapter);
		this.bbos = new BboQuoteCache((IMdQuoteListener) callbacks.get(MdServiceType.BBO), MdFeed.OPRA, range, multicastAdapter);
		this.sales = new SaleCache((IMdSaleListener) callbacks.get(MdServiceType.SALE), MdFeed.OPRA, range, null, true);
		this.tmpBuffer4 = new byte[4];
		this.tmpBuffer5 = new byte[5];
	}

	@Override
	public IMdServiceCache[] getMdServiceCaches()
	{
		return new IMdServiceCache[] { this.nbbos, this.bbos, this.sales };
	}

	@Override
	public void processMessage(String processorName, MdFeedPacket packet, boolean shouldIgnore)
	{
		OpraPacket opraPacket = (OpraPacket) packet;
		ByteBuffer buffer = opraPacket.getBuffer();
		char participantId = (char) buffer.get();
		char category = (char) buffer.get();
		char type = (char) buffer.get();
		char indicator = (char) buffer.get();
		int length = getMessageLength(category, indicator, buffer) - MESSAGE_HEADER_SIZE;
		int endPosition = buffer.position() + length;
		if (shouldIgnore)
		{
			buffer.position(endPosition);
			return;
		}

		Exchange exchange = getExchange(participantId);
		if (category == CATEGORY_SHORT_EQUITY_AND_INDEX_QUOTE || category == CATEGORY_LONG_EQUITY_AND_INDEX_QUOTE)
		{
			boolean isLong = category == CATEGORY_LONG_EQUITY_AND_INDEX_QUOTE;
			String symbol = ByteBufferUtil.getString(buffer, isLong ? this.tmpBuffer5 : this.tmpBuffer4);
			if (isLong) ByteBufferUtil.advancePosition(buffer, 1); // reserved
			char expMonth = (char) buffer.get();
			short expDay = ByteBufferUtil.getUnsignedByte(buffer);
			short expYear = (short) (ByteBufferUtil.getUnsignedByte(buffer) + 2000);
			char strikePriceDenominator = isLong ? (char) buffer.get() : 'A';
			double strikePrice = getPrice(isLong ? buffer.getInt() : ByteBufferUtil.getUnsignedShort(buffer), strikePriceDenominator);
			char premiumPriceDenominator = isLong ? (char) buffer.get() : 'B';
			double bidPrice = getPrice(isLong ? buffer.getInt() : ByteBufferUtil.getUnsignedShort(buffer), premiumPriceDenominator);
			int bidSize = (int) (isLong ? ByteBufferUtil.getUnsignedInt(buffer) : ByteBufferUtil.getUnsignedShort(buffer));
			double offerPrice = getPrice(isLong ? buffer.getInt() : ByteBufferUtil.getUnsignedShort(buffer), premiumPriceDenominator);
			int offerSize = (int) (isLong ? ByteBufferUtil.getUnsignedInt(buffer) : ByteBufferUtil.getUnsignedShort(buffer));
			symbol = createSymbol(symbol, expMonth, expDay, expYear, strikePrice);

			// Update BBO
			boolean excludedFromNbbo = indicator == ' ';
			int conditionCode = MdEntity.setCondition(0, Quote.CONDITION_EXCLUDED_FROM_NBBO);
			this.bbos.updateBidAndOffer(symbol, exchange, bidPrice, bidSize, offerPrice, offerSize, opraPacket.getTimestamp(), conditionCode);
			if (excludedFromNbbo) return;

			// Update NBBO
			Exchange bestBidExchange;
			char bestBidDenominatorCode;
			double bestBidPrice;
			int bestBidSize;
			Exchange bestOfferExchange;
			char bestOfferDenominatorCode;
			double bestOfferPrice;
			int bestOfferSize;
			switch (indicator)
			{
				case 'A':
					break;
				case 'B':
					this.nbbos.updateOffer(symbol, offerPrice, offerSize, exchange, opraPacket.getTimestamp(), indicator);
					break;
				case 'C':
					bestOfferExchange = getExchange((char) buffer.get());
					bestOfferDenominatorCode = (char) buffer.get();
					bestOfferPrice = getPrice(buffer.getInt(), bestOfferDenominatorCode);
					bestOfferSize = (int) ByteBufferUtil.getUnsignedInt(buffer);
					this.nbbos.updateOffer(symbol, bestOfferPrice, bestOfferSize, bestOfferExchange, opraPacket.getTimestamp(), indicator);
					break;
				case 'D':
					this.nbbos.updateOffer(symbol, 0, 0, null, opraPacket.getTimestamp(), indicator);
					break;
				case 'E':
					this.nbbos.updateBid(symbol, bidPrice, bidSize, exchange, opraPacket.getTimestamp(), indicator);
					break;
				case 'F':
					this.nbbos.updateBidAndOffer(symbol, bidPrice, bidSize, exchange, offerPrice, offerSize, exchange, opraPacket.getTimestamp(), indicator);
					break;
				case 'G':
					bestOfferExchange = getExchange((char) buffer.get());
					bestOfferDenominatorCode = (char) buffer.get();
					bestOfferPrice = getPrice(buffer.getInt(), bestOfferDenominatorCode);
					bestOfferSize = (int) ByteBufferUtil.getUnsignedInt(buffer);
					this.nbbos.updateBidAndOffer(symbol, bidPrice, bidSize, exchange, bestOfferPrice, bestOfferSize, bestOfferExchange, opraPacket.getTimestamp(), indicator);
					break;
				case 'H':
					this.nbbos.updateBidAndOffer(symbol, bidPrice, bidSize, exchange, 0, 0, null, opraPacket.getTimestamp(), indicator);
					break;
				case 'I':
					this.nbbos.updateBid(symbol, 0, 0, null, opraPacket.getTimestamp(), indicator);
					break;
				case 'J':
					this.nbbos.updateBidAndOffer(symbol, 0, 0, null, offerPrice, offerSize, exchange, opraPacket.getTimestamp(), indicator);
					break;
				case 'K':
					bestOfferExchange = getExchange((char) buffer.get());
					bestOfferDenominatorCode = (char) buffer.get();
					bestOfferPrice = getPrice(buffer.getInt(), bestOfferDenominatorCode);
					bestOfferSize = (int) ByteBufferUtil.getUnsignedInt(buffer);
					this.nbbos.updateBidAndOffer(symbol, 0, 0, null, bestOfferPrice, bestOfferSize, bestOfferExchange, opraPacket.getTimestamp(), indicator);
					break;
				case 'L':
					this.nbbos.updateBidAndOffer(symbol, 0, 0, null, 0, 0, null, opraPacket.getTimestamp(), indicator);
					break;
				case 'M':
					bestBidExchange = getExchange((char) buffer.get());
					bestBidDenominatorCode = (char) buffer.get();
					bestBidPrice = getPrice(buffer.getInt(), bestBidDenominatorCode);
					bestBidSize = (int) ByteBufferUtil.getUnsignedInt(buffer);
					this.nbbos.updateBid(symbol, bestBidPrice, bestBidSize, bestBidExchange, opraPacket.getTimestamp(), indicator);
					break;
				case 'N':
					bestBidExchange = getExchange((char) buffer.get());
					bestBidDenominatorCode = (char) buffer.get();
					bestBidPrice = getPrice(buffer.getInt(), bestBidDenominatorCode);
					bestBidSize = (int) ByteBufferUtil.getUnsignedInt(buffer);
					this.nbbos.updateBidAndOffer(symbol, bestBidPrice, bestBidSize, bestBidExchange, offerPrice, offerSize, exchange, opraPacket.getTimestamp(), indicator);
					break;
				case 'O':
					bestBidExchange = getExchange((char) buffer.get());
					bestBidDenominatorCode = (char) buffer.get();
					bestBidPrice = getPrice(buffer.getInt(), bestBidDenominatorCode);
					bestBidSize = (int) ByteBufferUtil.getUnsignedInt(buffer);
					bestOfferExchange = getExchange((char) buffer.get());
					bestOfferDenominatorCode = (char) buffer.get();
					bestOfferPrice = getPrice(buffer.getInt(), bestOfferDenominatorCode);
					bestOfferSize = (int) ByteBufferUtil.getUnsignedInt(buffer);
					this.nbbos.updateBidAndOffer(symbol, bestBidPrice, bestBidSize, bestBidExchange, bestOfferPrice, bestOfferSize, bestOfferExchange, opraPacket.getTimestamp(),
							indicator);
					break;
				case 'P':
					bestBidExchange = getExchange((char) buffer.get());
					bestBidDenominatorCode = (char) buffer.get();
					bestBidPrice = getPrice(buffer.getInt(), bestBidDenominatorCode);
					bestBidSize = (int) ByteBufferUtil.getUnsignedInt(buffer);
					this.nbbos.updateBidAndOffer(symbol, bestBidPrice, bestBidSize, bestBidExchange, 0, 0, null, opraPacket.getTimestamp(), indicator);
					break;
				default:
					break;
			}
		}
		else if (category == CATEGORY_EQUITY_INDEX_LAST_SALE)
		{
			String symbol = ByteBufferUtil.getString(buffer, this.tmpBuffer5);
			ByteBufferUtil.advancePosition(buffer, 1); // reserved
			char expMonth = (char) buffer.get();
			short expDay = ByteBufferUtil.getUnsignedByte(buffer);
			short expYear = (short) (ByteBufferUtil.getUnsignedByte(buffer) + 2000);
			char strikePriceDenominator = (char) buffer.get();
			double strikePrice = getPrice(buffer.getInt(), strikePriceDenominator);
			int volume = (int) ByteBufferUtil.getUnsignedInt(buffer);
			char premiumPriceDenominator = (char) buffer.get();
			double price = getPrice(buffer.getInt(), premiumPriceDenominator);
			ByteBufferUtil.advancePosition(buffer, 8); // trade identifier + reserved
			int saleCondition = getSaleCondition(type);
			symbol = createSymbol(symbol, expMonth, expDay, expYear, strikePrice);
			this.sales.updateWithSaleCondition(symbol, price, volume, exchange, opraPacket.getTimestamp(), saleCondition, String.valueOf(type));
		}
		else if (category == CATEGORY_OPEN_INTEREST)
		{
			String symbol = ByteBufferUtil.getString(buffer, this.tmpBuffer5);
			ByteBufferUtil.advancePosition(buffer, 1); // reserved
			char expMonth = (char) buffer.get();
			short expDay = ByteBufferUtil.getUnsignedByte(buffer);
			short expYear = (short) (ByteBufferUtil.getUnsignedByte(buffer) + 2000);
			char strikePriceDenominator = (char) buffer.get();
			double strikePrice = getPrice(buffer.getInt(), strikePriceDenominator);
			long openInterest = ByteBufferUtil.getUnsignedInt(buffer);
			symbol = createSymbol(symbol, expMonth, expDay, expYear, strikePrice);
			this.sales.updateOpenInterest(symbol, openInterest, opraPacket.getTimestamp(), "OI");
		}
		else if (category == CATEGORY_EQUITY_AND_INDEX_EOD_SUMMARY)
		{
			String symbol = ByteBufferUtil.getString(buffer, this.tmpBuffer5);
			ByteBufferUtil.advancePosition(buffer, 1); // reserved
			char expMonth = (char) buffer.get();
			short expDay = ByteBufferUtil.getUnsignedByte(buffer);
			short expYear = (short) (ByteBufferUtil.getUnsignedByte(buffer) + 2000);
			char strikePriceDenominator = (char) buffer.get();
			double strikePrice = getPrice(buffer.getInt(), strikePriceDenominator);
			long volume = ByteBufferUtil.getUnsignedInt(buffer);
			long openInterest = ByteBufferUtil.getUnsignedInt(buffer);
			char premiumPriceDenominator = (char) buffer.get();
			double openPrice = getPrice(buffer.getInt(), premiumPriceDenominator);
			double highPrice = getPrice(buffer.getInt(), premiumPriceDenominator);
			double lowPrice = getPrice(buffer.getInt(), premiumPriceDenominator);
			double lastPrice = getPrice(buffer.getInt(), premiumPriceDenominator);
			ByteBufferUtil.advancePosition(buffer, 21); // net change, underlying price info
			symbol = createSymbol(symbol, expMonth, expDay, expYear, strikePrice);
			this.sales.updateEndofDay(symbol, volume, openInterest, openPrice, highPrice, lowPrice, lastPrice, opraPacket.getTimestamp());
		}
		else if (category == CATEGORY_ADMINISTRATIVE)
		{
			String message = ByteBufferUtil.getUnboundedString(buffer, length);
			if (message.startsWith("ALERT ALERT ALERT"))
			{
				LOGGER.info(processorName + " - Exception - Received Admin Message - " + message);
			}
			else
			{
				LOGGER.info(processorName + " - Received Admin Message - " + message);
			}
		}
		else if (category == CATEGORY_CONTROL)
		{
			String message = ByteBufferUtil.getUnboundedString(buffer, length);
			LOGGER.info("Received Contrl Message - Category=" + category + " Type=" + type + " - message=" + message);
		}
		buffer.position(endPosition);
		return;
	}

	private static int getMessageLength(char category, char indicator, ByteBuffer buffer)
	{
		switch (category)
		{
			case 'a':
				return 35;
			case 'd':
				return 22;
			case 'f':
				return 64;
			case 'k':
				switch (indicator)
				{
					case 'C':
						return 45;
					case 'G':
						return 45;
					case 'K':
						return 45;
					case 'M':
						return 45;
					case 'N':
						return 45;
					case 'P':
						return 45;
					case 'O':
						return 55;
					default:
						return 35;
				}
			case 'q':
				switch (indicator)
				{
					case 'C':
						return 31;
					case 'G':
						return 31;
					case 'K':
						return 31;
					case 'M':
						return 31;
					case 'N':
						return 31;
					case 'P':
						return 31;
					case 'O':
						return 41;
					default:
						return 21;
				}
			case 'C':
				return ByteBufferUtil.getUnsignedShort(buffer) - MESSAGE_DATA_LENGTH_SIZE;
			case 'H':
				return ByteBufferUtil.getUnsignedShort(buffer) - MESSAGE_DATA_LENGTH_SIZE;
			case 'Y':
				return 19;
			default:
				return 0;
		}
	}

	private static int getSaleCondition(char type)
	{
		switch (type)
		{
			case ' ':
			case 'I':
			case 'J':
			case 'K':
			case 'L':
			case 'M':
			case 'N':
			case 'P':
			case 'Q':
			case 'S':
			case 'X':
				return MdEntity.setCondition(0, Sale.CONDITION_CODE_VWAP, Sale.CONDITION_CODE_VOLUME, Sale.CONDITION_CODE_LOW, Sale.CONDITION_CODE_HIGH, Sale.CONDITION_CODE_LAST);
			case 'A':
			case 'C': // last should beset to print before previous
			case 'E': // open should be set to next print
			case 'G':
			case 'O':
				return MdEntity.setCondition(0, Sale.CONDITION_CODE_CANCEL, Sale.CONDITION_CODE_VWAP, Sale.CONDITION_CODE_VOLUME);
			case 'B':
				return MdEntity.setCondition(0, Sale.CONDITION_CODE_LATE, Sale.CONDITION_CODE_VWAP, Sale.CONDITION_CODE_VOLUME, Sale.CONDITION_CODE_LOW, Sale.CONDITION_CODE_HIGH);
			case 'D':
				return MdEntity.setCondition(0, Sale.CONDITION_CODE_LATE, Sale.CONDITION_CODE_VWAP, Sale.CONDITION_CODE_VOLUME, Sale.CONDITION_CODE_LOW, Sale.CONDITION_CODE_HIGH,
						Sale.CONDITION_CODE_LAST);
			case 'F':
				return MdEntity.setCondition(0, Sale.CONDITION_CODE_OPEN, Sale.CONDITION_CODE_LATE, Sale.CONDITION_CODE_VWAP, Sale.CONDITION_CODE_VOLUME, Sale.CONDITION_CODE_LOW,
						Sale.CONDITION_CODE_HIGH);
			case 'H':
				return MdEntity.setCondition(0, Sale.CONDITION_CODE_OPEN, Sale.CONDITION_CODE_LATE, Sale.CONDITION_CODE_VWAP, Sale.CONDITION_CODE_VOLUME, Sale.CONDITION_CODE_LOW,
						Sale.CONDITION_CODE_HIGH, Sale.CONDITION_CODE_LAST);
			case 'R':
			case 'T':
				return MdEntity.setCondition(0, Sale.CONDITION_CODE_VWAP, Sale.CONDITION_CODE_VOLUME, Sale.CONDITION_CODE_LOW, Sale.CONDITION_CODE_HIGH);
			default:
				return 0;
		}
	}

	private static Exchange getExchange(char participantId)
	{
		return EXCHANGES[participantId - 'A'];
	}

	static double getPrice(long value, char denominatorCode)
	{
		if (denominatorCode == 'I') return value;
		return value / POWERS[denominatorCode - 'A'];
	}

	private static String createSymbol(String symbol, char expMonth, short expDay, short expYear, double strikePrice)
	{
		StringBuilder builder = new StringBuilder();
		builder.append(symbol);
		builder.append('+');
		builder.append(expYear);
		boolean isCall = 'A' <= expMonth && expMonth <= 'L';
		int month = 1 + (expMonth - 'A') % 12;
		if (month < 10) builder.append('0');
		builder.append(month);
		if (expDay < 10) builder.append('0');
		builder.append(expDay);
		builder.append(isCall ? 'C' : 'P');
		builder.append(strikePrice);
		return builder.toString();
	}

	static
	{
		EXCHANGES['A' - 'A'] = Exchange.USOP_NYSE_AMEX;
		EXCHANGES['B' - 'A'] = Exchange.USOP_BOSTON_OPTIONS_EXCHANGE;
		EXCHANGES['C' - 'A'] = Exchange.USOP_CHICAGO_BOARD_OPTIONS_EXCHANGE;
		EXCHANGES['I' - 'A'] = Exchange.USOP_INTERNATIONAL_SECURITIES_EXCHANGE;
		EXCHANGES['M' - 'A'] = Exchange.USOP_MIAMI_INTERNATIONAL_SECURITIES_EXCHANGE;
		EXCHANGES['N' - 'A'] = Exchange.USOP_NYSE_ARCA;
		EXCHANGES['O' - 'A'] = Exchange.USOP_OPRA;
		EXCHANGES['Q' - 'A'] = Exchange.USOP_NASDAQ_STOCK_MARKET;
		EXCHANGES['T' - 'A'] = Exchange.USOP_NASDAQ_OMX_BX_OPTIONS;
		EXCHANGES['W' - 'A'] = Exchange.USOP_C2;
		EXCHANGES['X' - 'A'] = Exchange.USOP_NASDAQ_OMX_PHLX;
		EXCHANGES['Z' - 'A'] = Exchange.USOP_BATS;
	}
}

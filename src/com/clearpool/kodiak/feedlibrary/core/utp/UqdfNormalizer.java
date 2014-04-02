package com.clearpool.kodiak.feedlibrary.core.utp;

import java.nio.ByteBuffer;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Logger;

import com.clearpool.kodiak.feedlibrary.caches.BboQuoteCache;
import com.clearpool.kodiak.feedlibrary.caches.IMarketSessionSettable;
import com.clearpool.kodiak.feedlibrary.caches.IMdServiceCache;
import com.clearpool.kodiak.feedlibrary.caches.NbboQuoteCache;
import com.clearpool.kodiak.feedlibrary.caches.StateCache;
import com.clearpool.kodiak.feedlibrary.callbacks.IMdLibraryCallback;
import com.clearpool.kodiak.feedlibrary.callbacks.IMdQuoteListener;
import com.clearpool.kodiak.feedlibrary.callbacks.IMdStateListener;
import com.clearpool.kodiak.feedlibrary.core.IMdNormalizer;
import com.clearpool.kodiak.feedlibrary.core.MdFeed;
import com.clearpool.kodiak.feedlibrary.core.MdFeedPacket;
import com.clearpool.kodiak.feedlibrary.core.MdFeedProps;
import com.clearpool.kodiak.feedlibrary.utils.ByteBufferUtil;
import com.clearpool.kodiak.feedlibrary.utils.MdDateUtil;
import com.clearpool.messageobjects.marketdata.Exchange;
import com.clearpool.messageobjects.marketdata.MarketSession;
import com.clearpool.messageobjects.marketdata.MarketState;
import com.clearpool.messageobjects.marketdata.MdEntity;
import com.clearpool.messageobjects.marketdata.MdServiceType;
import com.clearpool.messageobjects.marketdata.Quote;
import com.clearpool.messageobjects.marketdata.TradingState;

public class UqdfNormalizer implements IMdNormalizer, IMarketSessionSettable
{
	private static final Logger LOGGER = Logger.getLogger(UqdfNormalizer.class.getName());

	private static final char CATEGORY_QUOTE = 'Q';
	private static final char CATEGORY_ADMINISTRATIVE = 'A';
	private static final char CATEGORY_CONTROL = 'C';
	private static final char TYPE_LONG_QUOTE = 'F';
	private static final char TYPE_ADMIN_MESSAGE = 'A';
	private static final char TYPE_CROSS_SRO_TRADING_ACTION_MESSAGE = 'H';
	private static final char TYPE_ISSUE_SYMBOL_DIRECTORY_MESSAGE = 'B';
	private static final char TYPE_REG_SHO_SSPTR_INDICATOR = 'V';
	private static final char TYPE_LULD_PRICE_BAND_MESSAGE = 'P';
	private static final char TYPE_MWCB_DECLINE_LEVEL_MESSAGE = 'C';
	private static final char TYPE_MWCB_STATUS_MESSAGE = 'D';
	private static final char TYPE_MARKET_SESSION_OPEN = 'O';
	private static final char TYPE_MARKET_SESSION_CLOSE = 'C';
	private static final char NASDAQ_PARTICIPANT = 'Q';
	private static final int DEFAULT_LOT_SIZE = 100;

	private static final long PRE_MARKET_TIME = getPreMarketTime();
	private static final long OPEN_TIME = getOpenTime();
	private static final long CLOSE_TIME = getCloseTime();
	private static final long POST_MARKET_TIME = getPostMarketTime();

	private final NbboQuoteCache nbbos;
	private final BboQuoteCache bbos;
	private final StateCache states;
	private final Map<String, Integer> lotSizes;
	private final Set<String> ipoSymbols;

	private boolean receivedOpen;
	private boolean receivedClose;

	public UqdfNormalizer(Map<MdServiceType, IMdLibraryCallback> callbacks, String range)
	{
		this.nbbos = new NbboQuoteCache((IMdQuoteListener) callbacks.get(MdServiceType.NBBO), MdFeed.UQDF, range);
		this.bbos = new BboQuoteCache((IMdQuoteListener) callbacks.get(MdServiceType.BBO), MdFeed.UQDF, range);
		this.states = new StateCache((IMdStateListener) callbacks.get(MdServiceType.STATE), this, MdFeed.UQDF, range);
		this.lotSizes = getLotSizes();
		this.ipoSymbols = new HashSet<>();

		this.receivedOpen = false;
		this.receivedClose = false;
	}

	private static Map<String, Integer> getLotSizes()
	{
		Object lotSizeValues = MdFeedProps.getInstanceProperty(MdFeed.UQDF.toString(), "LOTSIZES");
		if (lotSizeValues == null) return new HashMap<String, Integer>();
		Map<String, String> stringMap = MdFeedProps.getAsMap((String) lotSizeValues);
		Map<String, Integer> map = new HashMap<String, Integer>();
		for (Entry<String, String> entry : stringMap.entrySet())
		{
			map.put(entry.getKey(), Integer.valueOf(entry.getValue()));
		}
		return map;
	}

	private static long getPreMarketTime()
	{
		Object preMarketTimeValue = MdFeedProps.getInstanceProperty(MdFeed.UQDF.toString(), "PREMARKETTIME");
		if (preMarketTimeValue == null) return MdDateUtil.createTime(new Date(), 4, 0, 0).getTime();
		String[] preMarketTimeSplit = ((String) preMarketTimeValue).split(":");
		Date premarkettime = MdDateUtil.createTime(new Date(), Integer.parseInt(preMarketTimeSplit[0]), Integer.parseInt(preMarketTimeSplit[1]), 0);
		LOGGER.info("Premarkettime=" + premarkettime);
		return premarkettime.getTime();
	}

	private static long getOpenTime()
	{
		Object openTimeValue = MdFeedProps.getInstanceProperty(MdFeed.UQDF.toString(), "OPENTIME");
		if (openTimeValue == null) return MdDateUtil.createTime(new Date(), 9, 30, 0).getTime();
		String[] openTimeSplit = ((String) openTimeValue).split(":");
		Date opentime = MdDateUtil.createTime(new Date(), Integer.parseInt(openTimeSplit[0]), Integer.parseInt(openTimeSplit[1]), 0);
		LOGGER.info("Opentime=" + opentime);
		return opentime.getTime();
	}

	private static long getCloseTime()
	{
		Object closeTimeValue = MdFeedProps.getInstanceProperty(MdFeed.UQDF.toString(), "CLOSETIME");
		if (closeTimeValue == null) return MdDateUtil.createTime(new Date(), 16, 0, 0).getTime();
		String[] closeTimeSplit = ((String) closeTimeValue).split(":");
		Date closetime = MdDateUtil.createTime(new Date(), Integer.parseInt(closeTimeSplit[0]), Integer.parseInt(closeTimeSplit[1]), 0);
		LOGGER.info("Closetime=" + closetime);
		return closetime.getTime();
	}

	private static long getPostMarketTime()
	{
		Object postMarketTimeValue = MdFeedProps.getInstanceProperty(MdFeed.UQDF.toString(), "POSTMARKETTIME");
		if (postMarketTimeValue == null) return MdDateUtil.createTime(new Date(), 20, 0, 0).getTime();
		String[] postMarketTimeSplit = ((String) postMarketTimeValue).split(":");
		Date postmarkettime = MdDateUtil.createTime(new Date(), Integer.parseInt(postMarketTimeSplit[0]), Integer.parseInt(postMarketTimeSplit[1]), 0);
		LOGGER.info("Postmarkettime=" + postmarkettime);
		return postmarkettime.getTime();
	}

	@Override
	public IMdServiceCache[] getMdServiceCaches()
	{
		return new IMdServiceCache[] { this.nbbos, this.bbos, this.states };
	}

	@Override
	public void processMessage(String processorName, MdFeedPacket packet, boolean shouldIgnore)
	{
		if (shouldIgnore) return;
		UtpPacket utpPacket = (UtpPacket) packet;
		char msgCategory = utpPacket.getMessageCategory();
		char msgType = utpPacket.getMessageType();
		long timestamp = utpPacket.getTimestamp();
		ByteBuffer buffer = utpPacket.getBuffer();

		if (msgCategory == CATEGORY_QUOTE)
		{
			boolean isLong = msgType == TYPE_LONG_QUOTE;
			String symbol = ByteBufferUtil.getString(buffer, isLong ? 11 : 5).trim();
			int lotSize = getLotSize(symbol);
			ByteBufferUtil.advancePosition(buffer, 2); // reserved, sipGeenratedUpdated
			char quoteCondition = (char) buffer.get();
			char luldBboIndicator = (char) buffer.get();
			char rlpIndicator = isLong ? (char) buffer.get() : ' ';
			char bidPriceDenominator = (char) buffer.get();
			double bidPrice = UtpUtils.getPrice(ByteBufferUtil.readAsciiLong(buffer, isLong ? 10 : 6), bidPriceDenominator);
			int bidSize = lotSize * (int) ByteBufferUtil.readAsciiLong(buffer, isLong ? 7 : 2);
			char askPriceDenominator = (char) buffer.get();
			double askPrice = UtpUtils.getPrice(ByteBufferUtil.readAsciiLong(buffer, isLong ? 10 : 6), askPriceDenominator);
			int askSize = lotSize * (int) ByteBufferUtil.readAsciiLong(buffer, isLong ? 7 : 2);
			if (isLong) ByteBufferUtil.advancePosition(buffer, 3); // currency
			char nbboAppendageIndicator = (char) buffer.get();
			char luldNbboIndicator = (char) buffer.get();
			ByteBufferUtil.advancePosition(buffer, 1); // finra adf mpid
			Exchange exchange = UtpUtils.getExchange(utpPacket.getParticipantId(), null);

			// Update BBO
			this.bbos.updateBidAndOffer(symbol, exchange, bidPrice, bidSize, askPrice, askSize, timestamp, getBboConditionCode(rlpIndicator, luldBboIndicator, quoteCondition, 0));

			// Update NBBO
			Exchange bestBidExchange;
			char bestBidPriceDenominator;
			double bestBidPrice;
			int bestBidSize;
			Exchange bestAskExchange;
			char bestAskPriceDenominator;
			int bestAskSize;

			double bestAskPrice;
			switch (nbboAppendageIndicator)
			{
				case '0':
					break;
				case '1':
					this.nbbos.updateBidAndOffer(symbol, 0, 0, null, 0, 0, null, timestamp, String.valueOf(quoteCondition));
					break;
				case '2':
					ByteBufferUtil.advancePosition(buffer, 1); // nbbo quote condition
					bestBidExchange = UtpUtils.getExchange((char) buffer.get(), null);
					bestBidPriceDenominator = (char) buffer.get();
					bestBidPrice = UtpUtils.getPrice(ByteBufferUtil.readAsciiLong(buffer, 6), bestBidPriceDenominator);
					bestBidSize = lotSize * (int) ByteBufferUtil.readAsciiLong(buffer, 2);
					ByteBufferUtil.advancePosition(buffer, 1); // reserved
					bestAskExchange = UtpUtils.getExchange((char) buffer.get(), null);
					bestAskPriceDenominator = (char) buffer.get();
					bestAskPrice = UtpUtils.getPrice(ByteBufferUtil.readAsciiLong(buffer, 6), bestAskPriceDenominator);
					bestAskSize = lotSize * (int) ByteBufferUtil.readAsciiLong(buffer, 2);
					this.nbbos.updateBidAndOffer(symbol, bestBidPrice, bestBidSize, bestBidExchange, bestAskPrice, bestAskSize, bestAskExchange, timestamp,
							String.valueOf(quoteCondition));
					break;
				case '3':
					ByteBufferUtil.advancePosition(buffer, 1); // nbbo quote condition
					bestBidExchange = UtpUtils.getExchange((char) buffer.get(), null);
					bestBidPriceDenominator = (char) buffer.get();
					bestBidPrice = UtpUtils.getPrice(ByteBufferUtil.readAsciiLong(buffer, 10), bestBidPriceDenominator);
					bestBidSize = lotSize * (int) ByteBufferUtil.readAsciiLong(buffer, 7);
					ByteBufferUtil.advancePosition(buffer, 1); // reserved
					bestAskExchange = UtpUtils.getExchange((char) buffer.get(), null);
					bestAskPriceDenominator = (char) buffer.get();
					bestAskPrice = UtpUtils.getPrice(ByteBufferUtil.readAsciiLong(buffer, 10), bestAskPriceDenominator);
					bestAskSize = lotSize * (int) ByteBufferUtil.readAsciiLong(buffer, 7);
					ByteBufferUtil.advancePosition(buffer, 3); // currency
					this.nbbos.updateBidAndOffer(symbol, bestBidPrice, bestBidSize, bestBidExchange, bestAskPrice, bestAskSize, bestAskExchange, timestamp,
							String.valueOf(quoteCondition));
					break;
				case '4':
					this.nbbos.updateBidAndOffer(symbol, bidPrice, bidSize, exchange, askPrice, askSize, exchange, timestamp, String.valueOf(quoteCondition));
					break;
				default:
					break;
			}

			// Update State
			MarketState previousState = this.states.getData(symbol);
			this.states.updateState(symbol, null, getStateConditionCode(luldNbboIndicator, (previousState == null) ? 0 : previousState.getConditionCode()), null, timestamp);
		}
		else if (msgCategory == CATEGORY_ADMINISTRATIVE)
		{
			if (msgType == TYPE_ADMIN_MESSAGE)
			{
				String message = ByteBufferUtil.getString(buffer, buffer.remaining()).trim();
				LOGGER.info(processorName + " - Received Admin Message - " + message);
				if (message.startsWith("IPO PRICE AT"))
				{
					String[] spaceSplit = message.split(" ");
					this.ipoSymbols.add(spaceSplit[4]);
				}
			}
			else if (msgType == TYPE_CROSS_SRO_TRADING_ACTION_MESSAGE)
			{
				String symbol = ByteBufferUtil.getString(buffer, 11).trim();
				char action = (char) buffer.get();
				ByteBufferUtil.advancePosition(buffer, 13); // action date/time, reason

				TradingState tradingState = null;
				switch (action)
				{
					case 'H':
						tradingState = TradingState.HALTED;
						break;
					case 'Q':
						tradingState = TradingState.AUCTION;
						break;
					case 'T':
						tradingState = TradingState.TRADING;
						break;
					case 'P':
						tradingState = TradingState.PAUSED;
						break;
					default:
						break;
				}
				MarketState previousState = this.states.getData(symbol);

				// IPO opened
				if (previousState != null && previousState.getTradingState() == TradingState.AUCTION && previousState.getMarketSession() == MarketSession.PREMARKET
						&& tradingState == TradingState.TRADING && UqdfNormalizer.OPEN_TIME <= timestamp && timestamp <= UqdfNormalizer.CLOSE_TIME) this.states
						.updateMarketSessionAndTradingState(symbol, MarketSession.NORMAL, tradingState, timestamp);
				else this.states.updateTradingState(symbol, tradingState, timestamp);
			}
			else if (msgType == TYPE_ISSUE_SYMBOL_DIRECTORY_MESSAGE)
			{
				String symbol = ByteBufferUtil.getString(buffer, 11).trim();
				ByteBufferUtil.advancePosition(buffer, 45); // Old issue symbol, Issue Name, Issue Type, Market Category, Authenticity, SSTI
				int roundLotSize = (int) ByteBufferUtil.readAsciiLong(buffer, 5);
				ByteBufferUtil.advancePosition(buffer, 1); // Financial status indicator
				this.lotSizes.put(symbol, Integer.valueOf(roundLotSize));
				this.states.updateMarketSessionAndTradingState(symbol, MarketSession.CLOSED, TradingState.TRADING, timestamp);
			}
			else if (msgType == TYPE_REG_SHO_SSPTR_INDICATOR)
			{
				String symbol = ByteBufferUtil.getString(buffer, 11).trim();
				char regShoAction = (char) buffer.get();

				MarketState previousState = this.states.getData(symbol);
				int conditionCode = (previousState == null) ? 0 : previousState.getConditionCode();
				MdEntity.unsetCondition(conditionCode, MarketState.CONDITION_SHORT_SALE_RESTRICTION);
				switch (regShoAction)
				{
					case '0':
						break;
					case '1':
					case '2':
						MdEntity.setCondition(conditionCode, MarketState.CONDITION_SHORT_SALE_RESTRICTION);
						break;
					default:
						break;
				}
				this.states.updateConditionCode(symbol, conditionCode, timestamp);
			}
			else if (msgType == TYPE_LULD_PRICE_BAND_MESSAGE)
			{
				String symbol = ByteBufferUtil.getString(buffer, 11).trim();
				ByteBufferUtil.advancePosition(buffer, 10); // luldBandIndicator, luldEffectiveTime
				char limitDownPriceDenominator = (char) buffer.get();
				double lowerBand = UtpUtils.getPrice(ByteBufferUtil.readAsciiLong(buffer, 10), limitDownPriceDenominator);
				char limitUpPriceDenominator = (char) buffer.get();
				double upperBand = UtpUtils.getPrice(ByteBufferUtil.readAsciiLong(buffer, 10), limitUpPriceDenominator);
				this.states.updateLowerAndUpperBands(symbol, lowerBand, upperBand, timestamp, false);
			}
			else if (msgType == TYPE_MWCB_DECLINE_LEVEL_MESSAGE)
			{
				char mwcbDenominator = (char) buffer.get();
				double level1Price = UtpUtils.getPrice(ByteBufferUtil.readAsciiLong(buffer, 12), mwcbDenominator);
				ByteBufferUtil.advancePosition(buffer, 3);
				double level2Price = UtpUtils.getPrice(ByteBufferUtil.readAsciiLong(buffer, 12), mwcbDenominator);
				ByteBufferUtil.advancePosition(buffer, 3);
				double level3Price = UtpUtils.getPrice(ByteBufferUtil.readAsciiLong(buffer, 12), mwcbDenominator);
				ByteBufferUtil.advancePosition(buffer, 3);

				LOGGER.info(processorName + " - MWCB S&P500 Decline - Level1=" + level1Price);
				LOGGER.info(processorName + " - MWCB S&P500 Decline - Level2=" + level2Price);
				LOGGER.info(processorName + " - MWCB S&P500 Decline - Level3=" + level3Price);
			}
			else if (msgType == TYPE_MWCB_STATUS_MESSAGE)
			{
				char mwcbStatusLevelIndicator = (char) buffer.get();
				ByteBufferUtil.advancePosition(buffer, 3);

				switch (mwcbStatusLevelIndicator)
				{
					case '1':
						LOGGER.info(processorName + " - MWCB Level 1 Breached (7%)");
						break;
					case '2':
						LOGGER.info(processorName + " - MWCB Level 2 Breached (13%)");
						break;
					case '3':
						LOGGER.info(processorName + " - MWCB Level 3 Breached (20%)");
						break;
					default:
						break;
				}
			}
		}
		else if (msgCategory == CATEGORY_CONTROL)
		{
			if (msgType == TYPE_MARKET_SESSION_OPEN && utpPacket.getParticipantId() == NASDAQ_PARTICIPANT)
			{
				this.receivedOpen = true;
				this.states.updateAllSymbols(MarketSession.NORMAL, timestamp, this.ipoSymbols);
				LOGGER.info(processorName + " - Got market session open for NASDAQ");
			}
			else if (msgType == TYPE_MARKET_SESSION_CLOSE && utpPacket.getParticipantId() == NASDAQ_PARTICIPANT)
			{
				this.receivedClose = true;
				this.states.updateAllSymbols(MarketSession.POSTMARKET, timestamp, null);
				LOGGER.info(processorName + " - Got market session close for NASDAQ");
			}
			else
			{
				LOGGER.info(processorName + " - Received Control Message Type=" + msgType);
			}
		}
	}

	private static int getBboConditionCode(char rlpIndicator, char luldIndicator, int quoteCondition, int conditionCode)
	{
		switch (rlpIndicator)
		{
			case ' ':
				break;
			case 'A':
				conditionCode = MdEntity.setCondition(conditionCode, Quote.CONDITION_BID_RLP);
				break;
			case 'B':
				conditionCode = MdEntity.setCondition(conditionCode, Quote.CONDITION_ASK_RLP);
				break;
			case 'C':
				conditionCode = MdEntity.setCondition(conditionCode, Quote.CONDITION_BID_RLP, Quote.CONDITION_ASK_RLP);
				break;
			default:
				break;
		}

		switch (luldIndicator)
		{
			case ' ':
				break;
			case 'A':
				conditionCode = MdEntity.setCondition(conditionCode, Quote.CONDITION_BID_NON_EXECUTABLE);
				break;
			case 'B':
			case 'C':
				conditionCode = MdEntity.setCondition(conditionCode, Quote.CONDITION_ASK_NON_EXECUTABLE);
				break;
			default:
				break;
		}

		switch (quoteCondition)
		{
			case 'F':
			case 'I':
			case 'L':
			case 'N':
			case 'U':
			case 'X':
			case 'Z':
				conditionCode = MdEntity.setCondition(conditionCode, Quote.CONDITION_EXCLUDED_FROM_NBBO);
				break;
			default:
				break;
		}

		return conditionCode;
	}

	private static int getStateConditionCode(char luldNbboIndicator, int conditionCode)
	{
		MdEntity.unsetCondition(conditionCode, MarketState.CONDITION_BID_NON_EXECUTABLE);
		MdEntity.unsetCondition(conditionCode, MarketState.CONDITION_ASK_NON_EXECUTABLE);
		MdEntity.unsetCondition(conditionCode, MarketState.CONDITION_BID_LIMIT_STATE);
		MdEntity.unsetCondition(conditionCode, MarketState.CONDITION_ASK_LIMIT_STATE);
		switch (luldNbboIndicator)
		{
			case ' ':
				break;
			case 'A':
				break;
			case 'B':
				conditionCode = MdEntity.setCondition(conditionCode, MarketState.CONDITION_BID_NON_EXECUTABLE);
				break;
			case 'C':
				conditionCode = MdEntity.setCondition(conditionCode, MarketState.CONDITION_ASK_NON_EXECUTABLE);
				break;
			case 'D':
				conditionCode = MdEntity.setCondition(conditionCode, MarketState.CONDITION_BID_NON_EXECUTABLE);
				conditionCode = MdEntity.setCondition(conditionCode, MarketState.CONDITION_ASK_NON_EXECUTABLE);
				break;
			case 'E':
				conditionCode = MdEntity.setCondition(conditionCode, MarketState.CONDITION_BID_LIMIT_STATE);
				break;
			case 'F':
				conditionCode = MdEntity.setCondition(conditionCode, MarketState.CONDITION_ASK_LIMIT_STATE);
				break;
			case 'G':
				conditionCode = MdEntity.setCondition(conditionCode, MarketState.CONDITION_BID_LIMIT_STATE);
				conditionCode = MdEntity.setCondition(conditionCode, MarketState.CONDITION_ASK_NON_EXECUTABLE);
				break;
			case 'H':
				conditionCode = MdEntity.setCondition(conditionCode, MarketState.CONDITION_BID_NON_EXECUTABLE);
				conditionCode = MdEntity.setCondition(conditionCode, MarketState.CONDITION_ASK_LIMIT_STATE);
				break;
			case 'I':
				conditionCode = MdEntity.setCondition(conditionCode, MarketState.CONDITION_BID_LIMIT_STATE);
				conditionCode = MdEntity.setCondition(conditionCode, MarketState.CONDITION_ASK_LIMIT_STATE);
				break;
			default:
				break;
		}
		return conditionCode;
	}

	private int getLotSize(String symbol)
	{
		if (this.lotSizes == null) return DEFAULT_LOT_SIZE;
		Integer lotSize = this.lotSizes.get(symbol);
		if (lotSize == null) return DEFAULT_LOT_SIZE;
		return lotSize.intValue();
	}

	@Override
	public MarketSession getMarketSession(long timestamp)
	{
		if (this.receivedClose) return MarketSession.CLOSED;
		else if (this.receivedOpen) return MarketSession.NORMAL;

		if (UqdfNormalizer.PRE_MARKET_TIME <= timestamp && timestamp < UqdfNormalizer.OPEN_TIME) return MarketSession.PREMARKET;
		if (UqdfNormalizer.OPEN_TIME <= timestamp && timestamp < UqdfNormalizer.CLOSE_TIME) return MarketSession.NORMAL;
		if (UqdfNormalizer.CLOSE_TIME <= timestamp && timestamp < UqdfNormalizer.POST_MARKET_TIME) return MarketSession.POSTMARKET;
		return MarketSession.CLOSED;
	}
}

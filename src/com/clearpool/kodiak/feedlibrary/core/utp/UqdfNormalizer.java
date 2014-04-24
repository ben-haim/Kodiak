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

	private static final long PRE_MARKET_OPEN_TIME = getPreMarketOpenTime();
	private static final long MARKET_OPEN_TIME = getMarketOpenTime();
	private static final long MARKET_CLOSE_TIME = getMarketCloseTime();
	private static final long POST_MARKET_CLOSE_TIME = getPostMarketCloseTime();

	private final NbboQuoteCache nbbos;
	private final BboQuoteCache bbos;
	private final StateCache states;
	private final byte[] tmpBuffer5;
	private final byte[] tmpBuffer11;
	private final Map<String, Integer> lotSizes;
	private final Set<String> ipoSymbols;

	private boolean isClosed = false;

	public UqdfNormalizer(Map<MdServiceType, IMdLibraryCallback> callbacks, String range)
	{
		this.nbbos = new NbboQuoteCache((IMdQuoteListener) callbacks.get(MdServiceType.NBBO), MdFeed.UQDF, range);
		this.bbos = new BboQuoteCache((IMdQuoteListener) callbacks.get(MdServiceType.BBO), MdFeed.UQDF, range);
		this.states = new StateCache((IMdStateListener) callbacks.get(MdServiceType.STATE), this, MdFeed.UQDF, range);
		this.tmpBuffer5 = new byte[5];
		this.tmpBuffer11 = new byte[11];
		this.lotSizes = getLotSizes();
		this.ipoSymbols = new HashSet<>();
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

	private static long getPreMarketOpenTime()
	{
		Object preMarketOpenTimeValue = MdFeedProps.getInstanceProperty(MdFeed.UQDF.toString(), "PREMARKETOPENTIME");
		if (preMarketOpenTimeValue == null) return MdDateUtil.createTime(new Date(), 4, 0, 0).getTime();
		String[] preMarketOpenTimeSplit = ((String) preMarketOpenTimeValue).split(":");
		Date premarketopentime = MdDateUtil.createTime(new Date(), Integer.parseInt(preMarketOpenTimeSplit[0]), Integer.parseInt(preMarketOpenTimeSplit[1]), 0);
		LOGGER.info("Premarketopentime=" + premarketopentime);
		return premarketopentime.getTime();
	}

	private static long getMarketOpenTime()
	{
		Object marketOpenTimeValue = MdFeedProps.getInstanceProperty(MdFeed.UQDF.toString(), "MARKETOPENTIME");
		if (marketOpenTimeValue == null) return MdDateUtil.createTime(new Date(), 9, 30, 0).getTime();
		String[] marketOpenTimeSplit = ((String) marketOpenTimeValue).split(":");
		Date marketopentime = MdDateUtil.createTime(new Date(), Integer.parseInt(marketOpenTimeSplit[0]), Integer.parseInt(marketOpenTimeSplit[1]), 0);
		LOGGER.info("Marketopentime=" + marketopentime);
		return marketopentime.getTime();
	}

	private static long getMarketCloseTime()
	{
		Object marketCloseTimeValue = MdFeedProps.getInstanceProperty(MdFeed.UQDF.toString(), "MARKETCLOSETIME");
		if (marketCloseTimeValue == null) return MdDateUtil.createTime(new Date(), 16, 0, 0).getTime();
		String[] marketCloseTimeSplit = ((String) marketCloseTimeValue).split(":");
		Date marketclosetime = MdDateUtil.createTime(new Date(), Integer.parseInt(marketCloseTimeSplit[0]), Integer.parseInt(marketCloseTimeSplit[1]), 0);
		LOGGER.info("Marketclosetime=" + marketclosetime);
		return marketclosetime.getTime();
	}

	private static long getPostMarketCloseTime()
	{
		Object postMarketCloseTimeValue = MdFeedProps.getInstanceProperty(MdFeed.UQDF.toString(), "POSTMARKETCLOSETIME");
		if (postMarketCloseTimeValue == null) return MdDateUtil.createTime(new Date(), 20, 0, 0).getTime();
		String[] postMarketCloseTimeSplit = ((String) postMarketCloseTimeValue).split(":");
		Date postmarketclosetime = MdDateUtil.createTime(new Date(), Integer.parseInt(postMarketCloseTimeSplit[0]), Integer.parseInt(postMarketCloseTimeSplit[1]), 0);
		LOGGER.info("Postmarketclosetime=" + postmarketclosetime);
		return postmarketclosetime.getTime();
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
		char participantId = utpPacket.getParticipantId();
		long timestamp = utpPacket.getTimestamp();
		ByteBuffer buffer = utpPacket.getBuffer();

		if (msgCategory == CATEGORY_QUOTE)
		{
			boolean isLong = msgType == TYPE_LONG_QUOTE;
			String symbol = ByteBufferUtil.getString(buffer, isLong ? this.tmpBuffer11 : this.tmpBuffer5);
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
			Exchange exchange = UtpUtils.getExchange(participantId, null);

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
					this.nbbos.updateBidAndOffer(symbol, 0, 0, null, 0, 0, null, timestamp, quoteCondition);
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
					this.nbbos.updateBidAndOffer(symbol, bestBidPrice, bestBidSize, bestBidExchange, bestAskPrice, bestAskSize, bestAskExchange, timestamp, quoteCondition);
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
					this.nbbos.updateBidAndOffer(symbol, bestBidPrice, bestBidSize, bestBidExchange, bestAskPrice, bestAskSize, bestAskExchange, timestamp, quoteCondition);
					break;
				case '4':
					this.nbbos.updateBidAndOffer(symbol, bidPrice, bidSize, exchange, askPrice, askSize, exchange, timestamp, quoteCondition);
					break;
				default:
					break;
			}

			// Update State
			MarketState previousState = this.states.getData(symbol);
			this.states.updateState(symbol, participantId, null, getStateConditionCode(luldNbboIndicator, (previousState == null) ? 0 : previousState.getConditionCode()), null,
					timestamp);
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
					this.ipoSymbols.add(spaceSplit[4]);
				}
			}
			else if (msgType == TYPE_CROSS_SRO_TRADING_ACTION_MESSAGE)
			{
				String symbol = ByteBufferUtil.getString(buffer, this.tmpBuffer11);
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
						&& tradingState == TradingState.TRADING && UqdfNormalizer.MARKET_OPEN_TIME <= timestamp && timestamp < UqdfNormalizer.MARKET_CLOSE_TIME) this.states
						.updateMarketSessionAndTradingState(symbol, participantId, MarketSession.NORMAL, tradingState, timestamp);
				else this.states.updateTradingState(symbol, participantId, tradingState, timestamp);
			}
			else if (msgType == TYPE_ISSUE_SYMBOL_DIRECTORY_MESSAGE)
			{
				String symbol = ByteBufferUtil.getString(buffer, this.tmpBuffer11);
				ByteBufferUtil.advancePosition(buffer, 45); // Old issue symbol, Issue Name, Issue Type, Market Category, Authenticity, SSTI
				int roundLotSize = (int) ByteBufferUtil.readAsciiLong(buffer, 5);
				ByteBufferUtil.advancePosition(buffer, 1); // Financial status indicator
				this.lotSizes.put(symbol, Integer.valueOf(roundLotSize));
				this.states.updateMarketSessionAndTradingState(symbol, participantId, MarketSession.PREMARKET, TradingState.TRADING, timestamp);
			}
			else if (msgType == TYPE_REG_SHO_SSPTR_INDICATOR)
			{
				String symbol = ByteBufferUtil.getString(buffer, this.tmpBuffer11);
				char regShoAction = (char) buffer.get();

				MarketState previousState = this.states.getData(symbol);
				int conditionCode = (previousState == null) ? 0 : previousState.getConditionCode();
				conditionCode = MdEntity.unsetCondition(conditionCode, MarketState.CONDITION_SHORT_SALE_RESTRICTION);
				switch (regShoAction)
				{
					case '0':
						break;
					case '1':
					case '2':
						conditionCode = MdEntity.setCondition(conditionCode, MarketState.CONDITION_SHORT_SALE_RESTRICTION);
						break;
					default:
						break;
				}
				this.states.updateConditionCode(symbol, participantId, conditionCode, timestamp);
			}
			else if (msgType == TYPE_LULD_PRICE_BAND_MESSAGE)
			{
				String symbol = ByteBufferUtil.getString(buffer, this.tmpBuffer11);
				ByteBufferUtil.advancePosition(buffer, 10); // luldBandIndicator, luldEffectiveTime
				char limitDownPriceDenominator = (char) buffer.get();
				double lowerBand = UtpUtils.getPrice(ByteBufferUtil.readAsciiLong(buffer, 10), limitDownPriceDenominator);
				char limitUpPriceDenominator = (char) buffer.get();
				double upperBand = UtpUtils.getPrice(ByteBufferUtil.readAsciiLong(buffer, 10), limitUpPriceDenominator);
				this.states.updateLowerAndUpperBands(symbol, participantId, lowerBand, upperBand, timestamp, false);
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
			if (msgType == TYPE_MARKET_SESSION_OPEN && participantId == NASDAQ_PARTICIPANT)
			{
				LOGGER.info(processorName + " - Got market session open for NASDAQ");
				this.states.updateAllSymbols(MarketSession.NORMAL, timestamp, this.ipoSymbols);
			}
			else if (msgType == TYPE_MARKET_SESSION_CLOSE && participantId == NASDAQ_PARTICIPANT)
			{
				LOGGER.info(processorName + " - Got market session close for NASDAQ");
				this.states.updateAllSymbols(MarketSession.POSTMARKET, timestamp, null);
			}
			else
			{
				LOGGER.info(processorName + " - Received Control Message Type=" + msgType);
			}
		}

		if (!this.isClosed && UqdfNormalizer.POST_MARKET_CLOSE_TIME <= timestamp)
		{
			this.states.updateAllSymbols(MarketSession.CLOSED, timestamp, null);
			this.isClosed = true;
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
		conditionCode = MdEntity.unsetCondition(conditionCode, MarketState.CONDITION_BID_NON_EXECUTABLE);
		conditionCode = MdEntity.unsetCondition(conditionCode, MarketState.CONDITION_ASK_NON_EXECUTABLE);
		conditionCode = MdEntity.unsetCondition(conditionCode, MarketState.CONDITION_BID_LIMIT_STATE);
		conditionCode = MdEntity.unsetCondition(conditionCode, MarketState.CONDITION_ASK_LIMIT_STATE);
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
	public MarketSession getMarketSession(char primaryListing, long timestamp)
	{
		if (UqdfNormalizer.PRE_MARKET_OPEN_TIME <= timestamp && timestamp < UqdfNormalizer.MARKET_OPEN_TIME) return MarketSession.PREMARKET;
		if (UqdfNormalizer.MARKET_OPEN_TIME <= timestamp && timestamp < UqdfNormalizer.MARKET_CLOSE_TIME) return MarketSession.NORMAL;
		if (UqdfNormalizer.MARKET_CLOSE_TIME <= timestamp && timestamp < UqdfNormalizer.POST_MARKET_CLOSE_TIME) return MarketSession.POSTMARKET;
		return MarketSession.CLOSED;
	}
}

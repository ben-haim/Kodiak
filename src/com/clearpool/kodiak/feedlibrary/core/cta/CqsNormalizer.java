package com.clearpool.kodiak.feedlibrary.core.cta;

import java.nio.ByteBuffer;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
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

public class CqsNormalizer implements IMdNormalizer, IMarketSessionSettable
{
	private static final Logger LOGGER = Logger.getLogger(CqsNormalizer.class.getName());

	private static final char CATEGORY_ADMINISTRATIVE = 'A';
	private static final char CATEGORY_BOND = 'B';
	private static final char CATEGORY_CONTROL = 'C';
	private static final char CATEGORY_EQUITY = 'E';
	private static final char CATEGORY_LOCAL = 'L';
	private static final char CATEGORY_MARKET_STATUS = 'M';
	private static final char TYPE_SHORT_QUOTE = 'D';
	private static final char TYPE_LONG_QUOTE = 'B';
	private static final char TYPE_MWCB_DECLINE_LEVEL = 'K';
	private static final char TYPE_MWCB_STATUS = 'L';
	private static final char NBBO_CONTAINED = '1';
	private static final char NBBO_NONE = '2';
	private static final char NBBO_LONG = '4';
	private static final char NBBO_SHORT = '6';
	private static final int DEFAULT_LOT_SIZE = 100;

	private static final long PRE_MARKET_OPEN_TIME = getPreMarketOpenTime();
	private static final long MARKET_OPEN_TIME = getMarketOpenTime();
	private static final long MARKET_CLOSE_TIME = getMarketCloseTime();
	private static final long POST_MARKET_CLOSE_TIME = getPostMarketCloseTime();

	private final NbboQuoteCache nbbos;
	private final BboQuoteCache bbos;
	private final StateCache states;
	private final Map<String, Integer> lotSizes;

	private boolean isPreMarketSession = false;
	private boolean isPostMarketSession = false;
	private boolean isClosed = false;

	public CqsNormalizer(Map<MdServiceType, IMdLibraryCallback> callbacks, String range)
	{
		this.nbbos = new NbboQuoteCache((IMdQuoteListener) callbacks.get(MdServiceType.NBBO), MdFeed.CQS, range);
		this.bbos = new BboQuoteCache((IMdQuoteListener) callbacks.get(MdServiceType.BBO), MdFeed.CQS, range);
		this.states = new StateCache((IMdStateListener) callbacks.get(MdServiceType.STATE), this, MdFeed.CQS, range);
		this.lotSizes = getLotSizes();
	}

	private static Map<String, Integer> getLotSizes()
	{
		Object lotSizeValues = MdFeedProps.getInstanceProperty(MdFeed.CQS.toString(), "LOTSIZES");
		if (lotSizeValues == null) return null;
		Map<String, String> stringMap = MdFeedProps.getAsMap((String) lotSizeValues);
		Map<String, Integer> map = new HashMap<String, Integer>();
		for (Entry<String, String> entry : stringMap.entrySet())
		{
			String key = entry.getKey();
			Integer value = Integer.valueOf(entry.getValue());
			map.put(key, value);
		}
		return map;
	}

	private static long getPreMarketOpenTime()
	{
		Object preMarketOpenTimeValue = MdFeedProps.getInstanceProperty(MdFeed.CQS.toString(), "PREMARKETOPENTIME");
		if (preMarketOpenTimeValue == null) return MdDateUtil.createTime(new Date(), 4, 0, 0).getTime();
		String[] preMarketOpenTimeSplit = ((String) preMarketOpenTimeValue).split(":");
		Date premarketopentime = MdDateUtil.createTime(new Date(), Integer.parseInt(preMarketOpenTimeSplit[0]), Integer.parseInt(preMarketOpenTimeSplit[1]), 0);
		LOGGER.info("Premarketopentime=" + premarketopentime);
		return premarketopentime.getTime();
	}

	private static long getMarketOpenTime()
	{
		Object marketOpenTimeValue = MdFeedProps.getInstanceProperty(MdFeed.CQS.toString(), "MARKETOPENTIME");
		if (marketOpenTimeValue == null) return MdDateUtil.createTime(new Date(), 9, 30, 0).getTime();
		String[] marketOpenTimeSplit = ((String) marketOpenTimeValue).split(":");
		Date marketopentime = MdDateUtil.createTime(new Date(), Integer.parseInt(marketOpenTimeSplit[0]), Integer.parseInt(marketOpenTimeSplit[1]), 0);
		LOGGER.info("Marketopentime=" + marketopentime);
		return marketopentime.getTime();
	}

	private static long getMarketCloseTime()
	{
		Object marketCloseTimeValue = MdFeedProps.getInstanceProperty(MdFeed.CQS.toString(), "MARKETCLOSETIME");
		if (marketCloseTimeValue == null) return MdDateUtil.createTime(new Date(), 16, 0, 0).getTime();
		String[] marketCloseTimeSplit = ((String) marketCloseTimeValue).split(":");
		Date marketclosetime = MdDateUtil.createTime(new Date(), Integer.parseInt(marketCloseTimeSplit[0]), Integer.parseInt(marketCloseTimeSplit[1]), 0);
		LOGGER.info("Marketclosetime=" + marketclosetime);
		return marketclosetime.getTime();
	}

	private static long getPostMarketCloseTime()
	{
		Object postMarketCloseTimeValue = MdFeedProps.getInstanceProperty(MdFeed.CQS.toString(), "POSTMARKETCLOSETIME");
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

		CtaPacket ctaPacket = (CtaPacket) packet;
		char msgCategory = ctaPacket.getMessageCategory();
		char msgType = ctaPacket.getMessageType();
		char participantId = ctaPacket.getParticipantId();
		long timestamp = ctaPacket.getTimestamp();
		ByteBuffer buffer = ctaPacket.getBuffer();

		if (msgCategory == CATEGORY_BOND || msgCategory == CATEGORY_LOCAL) return;
		if (msgCategory == CATEGORY_EQUITY)
		{
			if (msgType == TYPE_SHORT_QUOTE || msgType == TYPE_LONG_QUOTE)
			{
				boolean isLong = msgType == TYPE_LONG_QUOTE;
				String symbol = ByteBufferUtil.getString(buffer, isLong ? 11 : 3).trim();
				int lotSize = getLotSize(symbol);
				if (isLong) ByteBufferUtil.advancePosition(buffer, 2); // suffix, test message indicator
				char primaryListing = isLong ? (char) buffer.get() : ((ctaPacket.getMessageNetwork() == 'E') ? 'N' : 'A');
				primaryListing = (primaryListing == ' ') ? ((ctaPacket.getMessageNetwork() == 'E') ? 'N' : 'A') : primaryListing;
				if (isLong) ByteBufferUtil.advancePosition(buffer, 7); // SIP generated message ID, reserved, financial status, currency, instrument type
				if (isLong) ByteBufferUtil.advancePosition(buffer, 3); // cancel correction, settlement condition, market condition
				char quoteCondition = (char) buffer.get();
				char luldIndicator = (char) buffer.get();
				char retailInterestIndicator = isLong ? (char) buffer.get() : ' ';
				if (!isLong) ByteBufferUtil.advancePosition(buffer, 1); // reserved
				char bidPriceDenominatorIndicator = (char) buffer.get();
				double bidPrice = CtaUtils.getPrice(ByteBufferUtil.readAsciiLong(buffer, isLong ? 12 : 8), bidPriceDenominatorIndicator);
				int bidSize = lotSize * (int) ByteBufferUtil.readAsciiLong(buffer, isLong ? 7 : 3);
				if (!isLong) ByteBufferUtil.advancePosition(buffer, 1); // reserved
				char askPriceDenominatorIndicator = (char) buffer.get();
				double askPrice = CtaUtils.getPrice(ByteBufferUtil.readAsciiLong(buffer, isLong ? 12 : 8), askPriceDenominatorIndicator);
				int askSize = lotSize * (int) ByteBufferUtil.readAsciiLong(buffer, isLong ? 7 : 3);
				String finraMarketMakerId = isLong ? ByteBufferUtil.getString(buffer, 4).trim() : null;
				if (isLong) ByteBufferUtil.advancePosition(buffer, 1); // reserved
				char nbboLuldIndicator = isLong ? (char) buffer.get() : ' ';
				if (isLong) ByteBufferUtil.advancePosition(buffer, 1); // finra bbo luld indicator
				char shortSaleRestrictionIndicator = isLong ? (char) buffer.get() : ' ';
				ByteBufferUtil.advancePosition(buffer, 1); // reserved
				char nbboIndicator = (char) buffer.get();
				ByteBufferUtil.advancePosition(buffer, 1); // finra bbo indicator
				Exchange exchange = CtaUtils.getExchange(participantId, finraMarketMakerId);
				boolean isPrimaryListing = primaryListing == participantId;

				// Update lower and upper bands
				if (quoteCondition == '0')
				{
					this.states.updateLowerAndUpperBands(symbol, primaryListing, bidPrice, askPrice, timestamp, true);
				}
				else
				{
					// Update BBO
					this.bbos.updateBidAndOffer(symbol, exchange, bidPrice, bidSize, askPrice, askSize, timestamp,
							getBboConditionCode(retailInterestIndicator, luldIndicator, quoteCondition, 0));

					// Update NBBO
					if (nbboIndicator == NBBO_SHORT || nbboIndicator == NBBO_LONG)
					{
						boolean isNbboLong = nbboIndicator == NBBO_LONG;
						if (isNbboLong) ByteBufferUtil.advancePosition(buffer, 2); // reserved
						char bestBidParticipantId = (char) buffer.get();
						char bestBidPriceDenominatorIndicator = (char) buffer.get();
						double bestBidPrice = CtaUtils.getPrice(ByteBufferUtil.readAsciiLong(buffer, isNbboLong ? 12 : 8), bestBidPriceDenominatorIndicator);
						int bestBidSize = lotSize * (int) ByteBufferUtil.readAsciiLong(buffer, isNbboLong ? 7 : 3);
						String finraBestBidMarketMaker = isNbboLong ? ByteBufferUtil.getString(buffer, 4).trim() : null;
						Exchange bestBidExchange = CtaUtils.getExchange(bestBidParticipantId, finraBestBidMarketMaker);
						ByteBufferUtil.advancePosition(buffer, isNbboLong ? 3 : 1); // reserved
						char bestAskParticipantId = (char) buffer.get();
						char bestAskPriceDenominatorIndicator = (char) buffer.get();
						double bestAskPrice = CtaUtils.getPrice(ByteBufferUtil.readAsciiLong(buffer, isNbboLong ? 12 : 8), bestAskPriceDenominatorIndicator);
						int bestAskSize = lotSize * (int) ByteBufferUtil.readAsciiLong(buffer, isNbboLong ? 7 : 3);
						String finraBestAskMarketMaker = isNbboLong ? ByteBufferUtil.getString(buffer, 4).trim() : null;
						Exchange bestAskExchange = CtaUtils.getExchange(bestAskParticipantId, finraBestAskMarketMaker);
						ByteBufferUtil.advancePosition(buffer, isNbboLong ? 3 : 1); // reserved
						this.nbbos.updateBidAndOffer(symbol, bestBidPrice, bestBidSize, bestBidExchange, bestAskPrice, bestAskSize, bestAskExchange, timestamp,
								String.valueOf(quoteCondition));
					}
					else if (nbboIndicator == NBBO_CONTAINED)
					{
						this.nbbos.updateBidAndOffer(symbol, bidPrice, bidSize, exchange, askPrice, askSize, exchange, timestamp, String.valueOf(quoteCondition));
					}
					else if (nbboIndicator == NBBO_NONE)
					{
						this.nbbos.updateBidAndOffer(symbol, 0, 0, null, 0, 0, null, timestamp, String.valueOf(quoteCondition));
					}
				}

				// Update state for symbol
				MarketState previousState = this.states.getData(symbol);
				this.states.updateState(symbol, primaryListing, null,
						getStateConditionCode(nbboLuldIndicator, shortSaleRestrictionIndicator, (previousState == null) ? 0 : previousState.getConditionCode()),
						getTradingState(quoteCondition, isPrimaryListing), timestamp);
			}
		}
		else if (msgCategory == CATEGORY_ADMINISTRATIVE)
		{
			String message = ByteBufferUtil.getString(buffer, buffer.remaining()).trim();
			if (message.startsWith("ALERT ALERT ALERT"))
			{
				LOGGER.info(processorName + " - Exception - Received Admin Message - " + message);
			}
			else
			{
				LOGGER.info(processorName + " - Received Admin Message - " + message);
			}
		}
		else if (msgCategory == CATEGORY_MARKET_STATUS)
		{
			if (msgType == TYPE_MWCB_DECLINE_LEVEL)
			{
				char priceDenominatorIndicator = (char) buffer.get();
				double level1Price = CtaUtils.getPrice(ByteBufferUtil.readAsciiLong(buffer, 12), priceDenominatorIndicator);
				ByteBufferUtil.advancePosition(buffer, 3);
				double level2Price = CtaUtils.getPrice(ByteBufferUtil.readAsciiLong(buffer, 12), priceDenominatorIndicator);
				ByteBufferUtil.advancePosition(buffer, 3);
				double level3Price = CtaUtils.getPrice(ByteBufferUtil.readAsciiLong(buffer, 12), priceDenominatorIndicator);
				ByteBufferUtil.advancePosition(buffer, 3);

				LOGGER.info(processorName + " - MWCB S&P500 Decline - Level1=" + level1Price);
				LOGGER.info(processorName + " - MWCB S&P500 Decline - Level2=" + level2Price);
				LOGGER.info(processorName + " - MWCB S&P500 Decline - Level3=" + level3Price);
			}
			else if (msgType == TYPE_MWCB_STATUS)
			{
				char levelIndicator = (char) buffer.get();
				ByteBufferUtil.advancePosition(buffer, 3);
				LOGGER.info(processorName + " - MWCB Status - Level=" + levelIndicator);
			}
		}
		else if (msgCategory == CATEGORY_CONTROL)
		{
			LOGGER.info(processorName + " - Received Control Message Type=" + msgType);
		}

		if (!this.isPreMarketSession && CqsNormalizer.PRE_MARKET_OPEN_TIME <= timestamp && timestamp < CqsNormalizer.MARKET_OPEN_TIME)
		{
			this.states.updateAllSymbols(MarketSession.PREMARKET, timestamp, null);
			this.isPreMarketSession = true;
		}
		else if (!this.isPostMarketSession && CqsNormalizer.MARKET_CLOSE_TIME <= timestamp && timestamp < CqsNormalizer.POST_MARKET_CLOSE_TIME)
		{
			this.states.updateAllSymbols(MarketSession.POSTMARKET, timestamp, null);
			this.isPostMarketSession = true;
		}
		else if (!this.isClosed && CqsNormalizer.POST_MARKET_CLOSE_TIME <= timestamp)
		{
			this.states.updateAllSymbols(MarketSession.CLOSED, timestamp, null);
			this.isClosed = true;
		}
	}

	private static int getBboConditionCode(char rlpIndicator, char luldIndicator, char quoteCondition, int conditionCode)
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
				conditionCode = MdEntity.setCondition(conditionCode, Quote.CONDITION_ASK_NON_EXECUTABLE);
				break;
			case 'C':
				conditionCode = MdEntity.setCondition(conditionCode, Quote.CONDITION_BID_NON_EXECUTABLE, Quote.CONDITION_ASK_NON_EXECUTABLE);
				break;
			default:
				break;
		}

		switch (quoteCondition)
		{
			case 'R':
				break;
			case 'C':
			case 'D':
			case 'G':
			case 'I':
			case 'J':
			case 'K':
			case 'L':
			case 'M':
			case 'N':
			case 'P':
			case 'Q':
			case 'S':
			case 'T':
			case 'U':
			case 'V':
			case 'X':
			case 'Y':
			case 'Z':
			case '0':
			case '1':
			case '2':
			case '3':
			case '9':
				conditionCode = MdEntity.setCondition(conditionCode, Quote.CONDITION_EXCLUDED_FROM_NBBO);
				break;
			default:
				break;

		}
		return conditionCode;
	}

	private static int getStateConditionCode(char nbboLuldIndicator, char shortSaleRestrictionIndicator, int conditionCode)
	{
		MdEntity.unsetCondition(conditionCode, MarketState.CONDITION_BID_NON_EXECUTABLE);
		MdEntity.unsetCondition(conditionCode, MarketState.CONDITION_ASK_NON_EXECUTABLE);
		MdEntity.unsetCondition(conditionCode, MarketState.CONDITION_BID_LIMIT_STATE);
		MdEntity.unsetCondition(conditionCode, MarketState.CONDITION_ASK_LIMIT_STATE);
		switch (nbboLuldIndicator)
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

		MdEntity.unsetCondition(conditionCode, MarketState.CONDITION_SHORT_SALE_RESTRICTION);
		switch (shortSaleRestrictionIndicator)
		{
			case ' ':
				break;
			case 'A':
			case 'C':
				conditionCode = MdEntity.setCondition(conditionCode, MarketState.CONDITION_SHORT_SALE_RESTRICTION);
				break;
			default:
				break;
		}
		return conditionCode;
	}

	private static TradingState getTradingState(char quoteCondition, boolean isPrimary)
	{
		if (isPrimary)
		{
			switch (quoteCondition)
			{
				case 'R':
				case 'T':
					return TradingState.TRADING;
				case 'D':
				case 'J':
				case 'K':
				case 'P':
				case 'Q':
				case 'V':
				case 'Z':
				case '1':
				case '2':
				case '3':
					return TradingState.HALTED;
				case 'M':
					return TradingState.PAUSED;
				default:
					return TradingState.TRADING;
			}
		}
		return null;
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
		if (primaryListing == 'N')
		{
			if (CqsNormalizer.PRE_MARKET_OPEN_TIME <= timestamp && timestamp < CqsNormalizer.MARKET_CLOSE_TIME) return MarketSession.PREMARKET;
		}
		else
		{
			if (CqsNormalizer.PRE_MARKET_OPEN_TIME <= timestamp && timestamp < CqsNormalizer.MARKET_OPEN_TIME) return MarketSession.PREMARKET;
			if (CqsNormalizer.MARKET_OPEN_TIME <= timestamp && timestamp < CqsNormalizer.MARKET_CLOSE_TIME) return MarketSession.NORMAL;
		}
		if (CqsNormalizer.MARKET_CLOSE_TIME <= timestamp && timestamp < CqsNormalizer.POST_MARKET_CLOSE_TIME) return MarketSession.POSTMARKET;
		return MarketSession.CLOSED;
	}
}

package com.clearpool.kodiak.feedlibrary.core.cta;

import java.nio.ByteBuffer;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
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

	private final NbboQuoteCache nbbos;
	private final BboQuoteCache bbos;
	private final StateCache states;
	private final Map<String, Integer> lotSizes;
	private final Date closeTime;

	private boolean hasOpened;
	private boolean hasClosed;

	public CqsNormalizer(Map<MdServiceType, IMdLibraryCallback> callbacks, String range)
	{
		this.nbbos = new NbboQuoteCache((IMdQuoteListener) callbacks.get(MdServiceType.NBBO), MdFeed.CQS, range);
		this.bbos = new BboQuoteCache((IMdQuoteListener) callbacks.get(MdServiceType.BBO), MdFeed.CQS, range);
		this.states = new StateCache((IMdStateListener) callbacks.get(MdServiceType.STATE), MdFeed.CQS, range);
		this.lotSizes = getLotSizes();
		this.closeTime = getCloseTime();

		this.hasOpened = false;
		this.hasClosed = false;
	}

	private static Map<String, Integer> getLotSizes()
	{
		Object lotSizeValues = MdFeedProps.getInstanceProperty(MdFeed.CQS.toString(), "LOTSIZES");
		if (lotSizeValues == null) return null;
		Map<String, String> stringMap = MdFeedProps.getAsMap((String) lotSizeValues);
		Map<String, Integer> map = new HashMap<String, Integer>();
		for (Map.Entry<String, String> entry : stringMap.entrySet())
		{
			String key = entry.getKey();
			Integer value = Integer.valueOf(entry.getValue());
			map.put(key, value);
		}
		return map;
	}

	private static Date getCloseTime()
	{
		Object closeTimeValue = MdFeedProps.getInstanceProperty(MdFeed.CQS.toString(), "CLOSETIME");
		if (closeTimeValue == null) return MdDateUtil.createTime(new Date(), 16, 0, 0);
		String[] closeTimeSplit = ((String) closeTimeValue).split(":");
		Date closetime = MdDateUtil.createTime(new Date(), Integer.parseInt(closeTimeSplit[0]), Integer.parseInt(closeTimeSplit[1]), 0);
		LOGGER.info("Closetime=" + closetime);
		return closetime;
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
		ByteBuffer buffer = ctaPacket.getBuffer();

		if (ctaPacket.getMessageCategory() == CATEGORY_BOND || ctaPacket.getMessageCategory() == CATEGORY_LOCAL) return;
		if (ctaPacket.getMessageCategory() == CATEGORY_EQUITY)
		{
			if (ctaPacket.getMessageType() == TYPE_SHORT_QUOTE || ctaPacket.getMessageType() == TYPE_LONG_QUOTE)
			{
				boolean isLong = ctaPacket.getMessageType() == TYPE_LONG_QUOTE;
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
				Exchange exchange = CtaUtils.getExchange(ctaPacket.getParticipantId(), finraMarketMakerId);
				boolean isPrimaryListing = primaryListing == ctaPacket.getParticipantId();

				// Update lower and upper bands
				if (quoteCondition == '0')
				{
					this.states.updateLowerAndUpperBands(symbol, bidPrice, askPrice, ctaPacket.getTimestamp(), true);
				}
				else
				{
					// Update BBO
					int bboConditionCode = getBboConditionCode(retailInterestIndicator, luldIndicator, quoteCondition, 0);
					this.bbos.updateBidAndOffer(symbol, exchange, bidPrice, bidSize, askPrice, askSize, ctaPacket.getTimestamp(), bboConditionCode);

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
						this.nbbos.updateBidAndOffer(symbol, bestBidPrice, bestBidSize, bestBidExchange, bestAskPrice, bestAskSize, bestAskExchange, ctaPacket.getTimestamp(),
								String.valueOf(quoteCondition));
					}
					else if (nbboIndicator == NBBO_CONTAINED)
					{
						this.nbbos.updateBidAndOffer(symbol, bidPrice, bidSize, exchange, askPrice, askSize, exchange, ctaPacket.getTimestamp(), String.valueOf(quoteCondition));
					}
					else if (nbboIndicator == NBBO_NONE)
					{
						this.nbbos.updateBidAndOffer(symbol, 0, 0, null, 0, 0, null, ctaPacket.getTimestamp(), String.valueOf(quoteCondition));
					}
				}

				// Update state for symbol
				MarketState previousState = this.states.getData(symbol);
				int previousStateCondition = (previousState == null) ? 0 : previousState.getConditionCode();
				int stateConditionCode = getStateConditionCode(nbboLuldIndicator, shortSaleRestrictionIndicator, previousStateCondition);
				TradingState tradingState = getTradingState(quoteCondition, isPrimaryListing);
				this.states.updateState(symbol, null, stateConditionCode, tradingState, ctaPacket.getTimestamp());

				// Maybe change state for all symbols
				if (!this.hasOpened && MdDateUtil.US_OPEN_TIME.getTime() <= ctaPacket.getTimestamp() && ctaPacket.getTimestamp() < this.closeTime.getTime())
				{
					this.hasOpened = true;
				}
				else if (!this.hasClosed && ctaPacket.getTimestamp() >= this.closeTime.getTime())
				{
					this.states.updateAllSymbols(MarketSession.CLOSED, ctaPacket.getTimestamp(), null);
					this.hasClosed = true;
				}
			}
		}
		else if (ctaPacket.getMessageCategory() == CATEGORY_ADMINISTRATIVE)
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
		else if (ctaPacket.getMessageCategory() == CATEGORY_MARKET_STATUS)
		{
			if (ctaPacket.getMessageType() == TYPE_MWCB_DECLINE_LEVEL)
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
			else if (ctaPacket.getMessageType() == TYPE_MWCB_STATUS)
			{
				char levelIndicator = (char) buffer.get();
				ByteBufferUtil.advancePosition(buffer, 3);
				LOGGER.info(processorName + " - MWCB Status - Level=" + levelIndicator);
			}
		}
		else if (ctaPacket.getMessageCategory() == CATEGORY_CONTROL)
		{
			LOGGER.info(processorName + " - Received Control Message Type=" + ctaPacket.getMessageType());
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
				conditionCode = MdEntity.setCondition(conditionCode, Quote.CONDITION_EXCLUDED_FROM_NBBO);
				break;
			case 'D':
				conditionCode = MdEntity.setCondition(conditionCode, Quote.CONDITION_EXCLUDED_FROM_NBBO);
				break;
			case 'G':
				conditionCode = MdEntity.setCondition(conditionCode, Quote.CONDITION_EXCLUDED_FROM_NBBO);
				break;
			case 'I':
				conditionCode = MdEntity.setCondition(conditionCode, Quote.CONDITION_EXCLUDED_FROM_NBBO);
				break;
			case 'J':
				conditionCode = MdEntity.setCondition(conditionCode, Quote.CONDITION_EXCLUDED_FROM_NBBO);
				break;
			case 'K':
				conditionCode = MdEntity.setCondition(conditionCode, Quote.CONDITION_EXCLUDED_FROM_NBBO);
				break;
			case 'L':
				conditionCode = MdEntity.setCondition(conditionCode, Quote.CONDITION_EXCLUDED_FROM_NBBO);
				break;
			case 'M':
				conditionCode = MdEntity.setCondition(conditionCode, Quote.CONDITION_EXCLUDED_FROM_NBBO);
				break;
			case 'N':
				conditionCode = MdEntity.setCondition(conditionCode, Quote.CONDITION_EXCLUDED_FROM_NBBO);
				break;
			case 'P':
				conditionCode = MdEntity.setCondition(conditionCode, Quote.CONDITION_EXCLUDED_FROM_NBBO);
				break;
			case 'Q':
				conditionCode = MdEntity.setCondition(conditionCode, Quote.CONDITION_EXCLUDED_FROM_NBBO);
				break;
			case 'S':
				conditionCode = MdEntity.setCondition(conditionCode, Quote.CONDITION_EXCLUDED_FROM_NBBO);
				break;
			case 'T':
				conditionCode = MdEntity.setCondition(conditionCode, Quote.CONDITION_EXCLUDED_FROM_NBBO);
				break;
			case 'U':
				conditionCode = MdEntity.setCondition(conditionCode, Quote.CONDITION_EXCLUDED_FROM_NBBO);
				break;
			case 'V':
				conditionCode = MdEntity.setCondition(conditionCode, Quote.CONDITION_EXCLUDED_FROM_NBBO);
				break;
			case 'X':
				conditionCode = MdEntity.setCondition(conditionCode, Quote.CONDITION_EXCLUDED_FROM_NBBO);
				break;
			case 'Y':
				conditionCode = MdEntity.setCondition(conditionCode, Quote.CONDITION_EXCLUDED_FROM_NBBO);
				break;
			case 'Z':
				conditionCode = MdEntity.setCondition(conditionCode, Quote.CONDITION_EXCLUDED_FROM_NBBO);
				break;
			case '0':
				conditionCode = MdEntity.setCondition(conditionCode, Quote.CONDITION_EXCLUDED_FROM_NBBO);
				break;
			case '1':
				conditionCode = MdEntity.setCondition(conditionCode, Quote.CONDITION_EXCLUDED_FROM_NBBO);
				break;
			case '2':
				conditionCode = MdEntity.setCondition(conditionCode, Quote.CONDITION_EXCLUDED_FROM_NBBO);
				break;
			case '3':
				conditionCode = MdEntity.setCondition(conditionCode, Quote.CONDITION_EXCLUDED_FROM_NBBO);
				break;
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
				conditionCode = MdEntity.setCondition(conditionCode, MarketState.CONDITION_SHORT_SALE_RESTRICTION);
				break;
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
					return TradingState.TRADING;
				case 'D':
					return TradingState.HALTED;
				case 'J':
					return TradingState.HALTED;
				case 'K':
					return TradingState.HALTED;
				case 'M':
					return TradingState.PAUSED;
				case 'P':
					return TradingState.HALTED;
				case 'Q':
					return TradingState.HALTED;
				case 'T':
					return TradingState.TRADING;
				case 'V':
					return TradingState.HALTED;
				case 'Z':
					return TradingState.HALTED;
				case '1':
					return TradingState.HALTED;
				case '2':
					return TradingState.HALTED;
				case '3':
					return TradingState.HALTED;
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
	public MarketSession getMarketSession(String symbol, long timestamp)
	{
		// Everything closes at default time
		if (this.hasClosed) return MarketSession.CLOSED;
		else if (this.hasOpened) return MarketSession.NORMAL;
		if (MdDateUtil.US_OPEN_TIME.getTime() <= timestamp && timestamp <= this.closeTime.getTime()) return MarketSession.NORMAL;
		return MarketSession.CLOSED;
	}
}

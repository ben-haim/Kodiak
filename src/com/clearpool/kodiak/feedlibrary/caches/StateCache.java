package com.clearpool.kodiak.feedlibrary.caches;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.clearpool.common.symbology.ISymbolConverter;
import com.clearpool.kodiak.feedlibrary.callbacks.IMdStateListener;
import com.clearpool.kodiak.feedlibrary.core.MdFeed;
import com.clearpool.kodiak.feedlibrary.utils.symbolconverters.SymbolConverterFactory;
import com.clearpool.messageobjects.marketdata.MarketSession;
import com.clearpool.messageobjects.marketdata.MarketState;
import com.clearpool.messageobjects.marketdata.MdEntity;
import com.clearpool.messageobjects.marketdata.MdServiceType;
import com.clearpool.messageobjects.marketdata.TradingState;

public class StateCache implements IMdServiceCache
{
	private final IMdStateListener stateListener;
	private final MdFeed feedType;
	private final String range;
	private final Map<String, MarketState> states;
	private final IMarketSessionSettable marketSessionSetter;

	public StateCache(IMdStateListener iMdStateListener, IMarketSessionSettable marketSessionSetter, MdFeed feedType, String range)
	{
		this.stateListener = iMdStateListener;
		this.marketSessionSetter = marketSessionSetter;
		this.feedType = feedType;
		this.range = range;
		this.states = new HashMap<>();
	}

	public void update(MarketState state)
	{
		this.states.put(state.getSymbol(), state);
		sendState(state, state.getTimestamp());
	}

	public void updateState(String symbol, char primaryListing, MarketSession marketSession, int conditionCode, TradingState tradingState, long timestamp)
	{
		boolean isNew = false;
		MarketState state = this.states.get(symbol);
		if (state == null)
		{
			state = createState(symbol, primaryListing, timestamp);
			this.states.put(symbol, state);
			isNew = true;
		}

		boolean sendUpdate = false;
		if (marketSession != null && state.getMarketSession() != marketSession)
		{
			state.setMarketSession(marketSession);
			sendUpdate = true;
		}

		if (tradingState != null && state.getTradingState() != tradingState)
		{
			state.setTradingState(tradingState);
			sendUpdate = true;
		}

		if (state.getConditionCode() != conditionCode)
		{
			state.setConditionCode(conditionCode);
			sendUpdate = true;
		}

		if (isNew || sendUpdate)
		{
			sendState(state, timestamp);
		}
	}

	public void updateTradingState(String symbol, char primaryListing, TradingState tradingState, long timestamp)
	{
		boolean isNew = false;
		MarketState state = this.states.get(symbol);
		if (state == null)
		{
			state = createState(symbol, primaryListing, timestamp);
			this.states.put(symbol, state);
			isNew = true;
		}

		boolean sendUpdate = false;
		if (tradingState != null && state.getTradingState() != tradingState)
		{
			state.setTradingState(tradingState);
			sendUpdate = true;
		}

		if (isNew || sendUpdate)
		{
			sendState(state, timestamp);
		}
	}

	public void updateMarketSessionAndTradingState(String symbol, char primaryListing, MarketSession marketSession, TradingState tradingState, long timestamp)
	{
		boolean isNew = false;
		MarketState state = this.states.get(symbol);
		if (state == null)
		{
			state = createState(symbol, primaryListing, timestamp);
			this.states.put(symbol, state);
			isNew = true;
		}

		boolean sendUpdate = false;
		if (marketSession != null && state.getMarketSession() != marketSession)
		{
			state.setMarketSession(marketSession);
			sendUpdate = true;
		}
		if (tradingState != null && state.getTradingState() != tradingState)
		{
			state.setTradingState(tradingState);
			sendUpdate = true;
		}

		if (isNew || sendUpdate)
		{
			sendState(state, timestamp);
		}
	}

	public void updateMarketSession(String symbol, char primaryListing, MarketSession marketSession, long timestamp)
	{
		boolean isNew = false;
		MarketState state = this.states.get(symbol);
		if (state == null)
		{
			state = createState(symbol, primaryListing, timestamp);
			this.states.put(symbol, state);
			isNew = true;
		}

		boolean sendUpdate = false;
		if (marketSession != null && state.getMarketSession() != marketSession)
		{
			state.setMarketSession(marketSession);
			sendUpdate = true;
		}

		if (isNew || sendUpdate)
		{
			sendState(state, timestamp);
		}
	}

	public void updateConditionCode(String symbol, char primaryListing, int conditionCode, long timestamp)
	{
		boolean isNew = false;
		MarketState state = this.states.get(symbol);
		if (state == null)
		{
			state = createState(symbol, primaryListing, timestamp);
			this.states.put(symbol, state);
			isNew = true;
		}

		boolean sendUpdate = false;
		if (state.getConditionCode() != conditionCode)
		{
			state.setConditionCode(conditionCode);
			sendUpdate = true;
		}

		if (isNew || sendUpdate)
		{
			sendState(state, timestamp);
		}
	}

	public void updateLowerAndUpperBands(String symbol, char primaryListing, double lowerBand, double upperBand, long timestamp, boolean updateMarketSession)
	{
		boolean isNew = false;
		MarketState state = this.states.get(symbol);
		if (state == null)
		{
			state = createState(symbol, primaryListing, timestamp);
			this.states.put(symbol, state);
			isNew = true;
		}

		boolean sendUpdate = false;
		if (updateMarketSession && (state.getMarketSession() == MarketSession.PREMARKET || state.getMarketSession() == null))
		{
			if (state.getUpperBand() == 0 && state.getLowerBand() == 0 && (lowerBand != 0 || upperBand != 0))
			{
				state.setMarketSession(MarketSession.NORMAL);
			}
		}

		if (state.getLowerBand() != lowerBand)
		{
			state.setLowerBand(lowerBand);
			sendUpdate = true;
		}

		if (state.getUpperBand() != upperBand)
		{
			state.setUpperBand(upperBand);
			sendUpdate = true;
		}

		if (isNew || sendUpdate)
		{
			sendState(state, timestamp);
		}
	}

	public void updateAllSymbols(MarketSession marketSession, long timestamp, Set<String> excludedSymbols)
	{
		for (MarketState state : this.states.values())
		{

			boolean sendUpdate = false;
			if (marketSession != null && state.getMarketSession() != marketSession && (excludedSymbols == null || !excludedSymbols.contains(state.getSymbol())))
			{
				state.setMarketSession(marketSession);
				sendUpdate = true;
			}

			if (sendUpdate)
			{
				sendState(state, timestamp);
			}
		}
	}

	private void sendState(MarketState state, long timestamp)
	{
		state.setMdTimestamp(System.currentTimeMillis());
		state.setTimestamp(timestamp);
		state.setSymbolSequenceNumber(state.getSymbolSequenceNumber() + 1);
		state.setConditionCode(MdEntity.setCondition(state.getConditionCode(), MdEntity.CONDITION_FRESH));

		if (this.stateListener != null)
		{
			state = state.clone();
			this.stateListener.stateReceived(state);
		}
	}

	private MarketState createState(String symbol, char primaryListing, long timestamp)
	{
		MarketState state = new MarketState();
		state.setServiceType(MdServiceType.STATE);
		ISymbolConverter symbolConverter = SymbolConverterFactory.getConverterInstance(this.feedType);
		if (symbolConverter != null) state.setSymbol(symbolConverter.convert(symbol));
		else state.setSymbol(symbol);
		state.setMarketSession(this.marketSessionSetter.getMarketSession(primaryListing, timestamp));
		state.setTradingState(TradingState.TRADING);
		return state;
	}

	@Override
	public MdServiceType getMdServiceType()
	{
		return MdServiceType.STATE;
	}

	@Override
	public String[] getAllSymbols()
	{
		return this.states.keySet().toArray(new String[0]);
	}

	@Override
	public MarketState getData(String symbol)
	{
		return this.states.get(symbol);
	}

	@Override
	public String getRange()
	{
		return this.range;
	}

	@Override
	public Collection<String> publishAllData()
	{
		for (MarketState state : this.states.values())
		{
			if (this.stateListener != null)
			{
				state = state.clone();
				this.stateListener.stateReceived(state);
			}
		}
		return this.states.keySet();
	}
}

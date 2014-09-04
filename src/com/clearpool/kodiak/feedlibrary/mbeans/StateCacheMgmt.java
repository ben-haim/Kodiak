package com.clearpool.kodiak.feedlibrary.mbeans;

import java.util.Arrays;
import java.util.Collection;

import com.clearpool.kodiak.feedlibrary.caches.StateCache;
import com.clearpool.kodiak.feedlibrary.mbeans.format.AnnotatedMBean;
import com.clearpool.kodiak.feedlibrary.mbeans.format.MdEntityHtmlFormatter;
import com.clearpool.messageobjects.marketdata.MarketSession;
import com.clearpool.messageobjects.marketdata.MarketState;
import com.clearpool.messageobjects.marketdata.TradingState;

public class StateCacheMgmt extends AnnotatedMBean<StateCacheMgmtMBean> implements StateCacheMgmtMBean
{
	private final StateCache stateCache;

	public StateCacheMgmt(StateCache stateCache)
	{
		super(StateCacheMgmtMBean.class);
		this.stateCache = stateCache;
	}

	@Override
	public String getMdService()
	{
		return String.valueOf(this.stateCache.getMdServiceType());
	}

	@Override
	public String[] getAllSymbols()
	{
		String[] symbols = this.stateCache.getAllSymbols();
		Arrays.sort(symbols);
		return symbols;
	}

	@Override
	public String getData(String symbol)
	{
		return String.valueOf(this.stateCache.getData(symbol));
	}

	@Override
	public String getFormattedData(String symbol)
	{
		MarketState state = this.stateCache.getData(symbol);
		if (state == null) return String.valueOf(state);
		return MdEntityHtmlFormatter.formatState(state);
	}

	@Override
	public String setMarketSession(String symbol, char primaryListing, boolean isPrimaryListing, String marketSession)
	{
		if (marketSession == null) return "MarketSession value is null";
		marketSession = marketSession.trim();
		if (marketSession.length() == 0) return "MarketSession value is empty";

		MarketSession session = MarketSession.getByString(marketSession);
		if (session == null) return "Unable to find MarketSession for value=" + marketSession;

		MarketState previousState = this.stateCache.getData(symbol);
		MarketSession previousSession = previousState.getMarketSession();
		if (previousSession == session) return "MarketSession is already " + previousSession;

		this.stateCache.updateMarketSession(symbol, primaryListing, isPrimaryListing, session, System.currentTimeMillis());
		return "Updated MarketSession. Prev=" + previousSession + " New=" + session;
	}

	@Override
	public String setTradingState(String symbol, char primaryListing, boolean isPrimaryListing, String tradingState)
	{
		if (tradingState == null) return "TradingState value is null";
		tradingState = tradingState.trim();
		if (tradingState.length() == 0) return "TradingState value is empty";

		TradingState state = TradingState.getByString(tradingState);
		if (state == null) return "Unable to find TradingState for value=" + tradingState;

		MarketState previousState = this.stateCache.getData(symbol);
		TradingState previousTradingState = previousState.getTradingState();
		if (previousTradingState == state) return "TradingState is already " + previousTradingState;

		this.stateCache.updateTradingState(symbol, primaryListing, isPrimaryListing, state, System.currentTimeMillis());
		return "Updated TradingState. Prev=" + previousTradingState + " New=" + state;
	}

	@Override
	public String setConditionCode(String symbol, char primaryListing, boolean isPrimaryListing, String conditionCode)
	{
		if (conditionCode == null) return "ConditionCode value is null";
		conditionCode = conditionCode.trim();
		if (conditionCode.length() == 0) return "ConditionCode value is empty";

		int condition = Integer.parseInt(conditionCode);

		MarketState previousState = this.stateCache.getData(symbol);
		int previousConditionCode = previousState.getConditionCode();
		if (previousConditionCode == condition) return "ConditionCode is already " + previousConditionCode;

		this.stateCache.updateConditionCode(symbol, primaryListing, isPrimaryListing, condition, System.currentTimeMillis());
		return "Updated ConditionCode. Prev=" + previousConditionCode + " New=" + condition;
	}

	@Override
	public String setAllMarketSessions(String marketSession)
	{
		if (marketSession == null) return "MarketSession value is null";
		marketSession = marketSession.trim();
		if (marketSession.length() == 0) return "MarketSession value is empty";

		MarketSession session = MarketSession.getByString(marketSession);
		if (session == null) return "Unable to find MarketSession for value=" + marketSession;
		this.stateCache.updateAllSymbols(session, System.currentTimeMillis(), null);
		return "Updated all market sessions to " + session;
	}

	@Override
	public String publishAllData()
	{
		Collection<String> publishedSymbols = this.stateCache.publishAllData();
		return String.valueOf(publishedSymbols);
	}
}

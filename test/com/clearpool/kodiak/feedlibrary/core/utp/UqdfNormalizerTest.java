package com.clearpool.kodiak.feedlibrary.core.utp;

import static org.junit.Assert.assertEquals;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.clearpool.kodiak.feedlibrary.callbacks.IMdLibraryCallback;
import com.clearpool.kodiak.feedlibrary.core.MdFeed;
import com.clearpool.kodiak.feedlibrary.core.MdFeedProps;
import com.clearpool.kodiak.feedlibrary.core.TestMDQuoteListener;
import com.clearpool.kodiak.feedlibrary.core.TestMDStateListener;
import com.clearpool.kodiak.feedlibrary.core.utp.UqdfNormalizer;
import com.clearpool.kodiak.feedlibrary.core.utp.UtpPacket;
import com.clearpool.kodiak.feedlibrary.utils.ByteBufferUtil;
import com.clearpool.messageobjects.marketdata.Exchange;
import com.clearpool.messageobjects.marketdata.MarketSession;
import com.clearpool.messageobjects.marketdata.MarketState;
import com.clearpool.messageobjects.marketdata.MdServiceType;
import com.clearpool.messageobjects.marketdata.Quote;
import com.clearpool.messageobjects.marketdata.TradingState;

public class UqdfNormalizerTest
{
	private final TestMDQuoteListener bboListener = new TestMDQuoteListener();
	private final TestMDQuoteListener nbboListener = new TestMDQuoteListener();
	private final TestMDStateListener stateListener = new TestMDStateListener();
	private UqdfNormalizer normalizer;
	private long sequenceNumber;

	@Before
	public void setUp()
	{
		Map<MdServiceType, IMdLibraryCallback> callbacks = new HashMap<MdServiceType, IMdLibraryCallback>();
		callbacks.put(MdServiceType.BBO, this.bboListener);
		callbacks.put(MdServiceType.NBBO, this.nbboListener);
		callbacks.put(MdServiceType.STATE, this.stateListener);
		HashMap<String, Integer> lotsize = new HashMap<String, Integer>();
		lotsize.put("SYED", new Integer(10));
		MdFeedProps.putInstanceProperty(lotsize, MdFeed.UQDF.toString(), "LOTSIZES");
		this.normalizer = new UqdfNormalizer(callbacks, "", 0) {
			@Override
			public MarketSession getMarketSession(char primaryListing, long timestamp)
			{
				return MarketSession.NORMAL;
			}
		};
	}

	@Test
	public void testIgnore()
	{
		this.normalizer.processMessage("TEST", createUtpPacket(createMarketSessionOpen()), true);
		assertSizes(0, 0, 0);
	}

	@Test
	public void testAdminMessage()
	{
		this.normalizer.processMessage("TEST", createUtpPacket(createAdminMessage("TEST")), false);
		assertSizes(0, 0, 0);
	}

	@Test
	public void testCrossSROTradingActionMessage()
	{
		this.normalizer.processMessage("TEST", createUtpPacket(createCrossSROTradingActionMessage("AAPL", 'H')), false);
		assertSizes(1, 0, 0);
		assertState("AAPL", 2, MarketSession.NORMAL, TradingState.HALTED, 0, 0);

		this.normalizer.processMessage("TEST", createUtpPacket(createCrossSROTradingActionMessage("AAPL", 'Q')), false);
		assertSizes(1, 0, 0);
		assertState("AAPL", 2, MarketSession.NORMAL, TradingState.AUCTION, 0, 0);

		this.normalizer.processMessage("TEST", createUtpPacket(createCrossSROTradingActionMessage("AAPL", 'T')), false);
		assertSizes(1, 0, 0);
		assertState("AAPL", 2, MarketSession.NORMAL, TradingState.TRADING, 0, 0);

		this.normalizer.processMessage("TEST", createUtpPacket(createCrossSROTradingActionMessage("AAPL", 'P')), false);
		assertSizes(1, 0, 0);
		assertState("AAPL", 2, MarketSession.NORMAL, TradingState.PAUSED, 0, 0);
	}

	@Test
	public void testPreMarketAuction()
	{
		Map<MdServiceType, IMdLibraryCallback> callbacks = new HashMap<MdServiceType, IMdLibraryCallback>();
		callbacks.put(MdServiceType.BBO, this.bboListener);
		callbacks.put(MdServiceType.NBBO, this.nbboListener);
		callbacks.put(MdServiceType.STATE, this.stateListener);
		UqdfNormalizer n = new UqdfNormalizer(callbacks, "", 0) {
			@Override
			public MarketSession getMarketSession(char primaryListing, long timestamp)
			{
				return MarketSession.CLOSED;
			}
		};

		n.processMessage("TEST", createUtpPacket(createIssueSymbolDirectoryMessage("AAPL", 100)), false);
		assertSizes(1, 0, 0);
		assertState("AAPL", 2, MarketSession.PREMARKET, TradingState.TRADING, 0, 0);

		n.processMessage("TEST", createUtpPacket(createCrossSROTradingActionMessage("AAPL", 'H')), false);
		assertSizes(1, 0, 0);
		assertState("AAPL", 2, MarketSession.PREMARKET, TradingState.HALTED, 0, 0);

		n.processMessage("TEST", createUtpPacket(createCrossSROTradingActionMessage("AAPL", 'Q')), false);
		assertSizes(1, 0, 0);
		assertState("AAPL", 2, MarketSession.PREMARKET, TradingState.AUCTION, 0, 0);

		n.processMessage("TEST", createUtpPacket(createCrossSROTradingActionMessage("AAPL", 'T')), false);
		assertSizes(1, 0, 0);
		assertState("AAPL", 2, MarketSession.PREMARKET, TradingState.TRADING, 0, 0);
	}

	@Test
	public void testIssueSymbolDirectoryMessage()
	{
		this.normalizer.processMessage("TEST", createUtpPacket(createIssueSymbolDirectoryMessage("AAPL", 100)), false);
		assertSizes(1, 0, 0);
		assertState("AAPL", 2, MarketSession.PREMARKET, TradingState.TRADING, 0, 0);
	}

	@Test
	public void testRegShoSSPTRIndicator()
	{
		this.normalizer.processMessage("TEST", createUtpPacket(createRegShoShortSalePriceTestRestrictedIndicator("AAPL", '0')), false);
		assertSizes(1, 0, 0);
		assertState("AAPL", 2, MarketSession.NORMAL, TradingState.TRADING, 0, 0);

		this.normalizer.processMessage("TEST", createUtpPacket(createRegShoShortSalePriceTestRestrictedIndicator("AAPL", '1')), false);
		assertSizes(1, 0, 0);
		assertState("AAPL", 2050, MarketSession.NORMAL, TradingState.TRADING, 0, 0);

		this.normalizer.processMessage("TEST", createUtpPacket(createRegShoShortSalePriceTestRestrictedIndicator("AAPL", '0')), false);
		assertSizes(1, 0, 0);
		assertState("AAPL", 2, MarketSession.NORMAL, TradingState.TRADING, 0, 0);

		this.normalizer.processMessage("TEST", createUtpPacket(createRegShoShortSalePriceTestRestrictedIndicator("AAPL", '2')), false);
		assertSizes(1, 0, 0);
		assertState("AAPL", 2050, MarketSession.NORMAL, TradingState.TRADING, 0, 0);
	}

	@Test
	public void testLULDPriceBandMessage()
	{
		this.normalizer.processMessage("TEST", createUtpPacket(createLULDPriceBandMessage("AAPL", 500, 600)), false);
		assertSizes(1, 0, 0);
		assertState("AAPL", 2, MarketSession.NORMAL, TradingState.TRADING, 500, 600);
	}

	@Test
	public void testMWCBDeclineLevelMessage()
	{
		this.normalizer.processMessage("TEST", createUtpPacket(createMWCBDeclineLevelMessage(1, 2, 3)), false);
		assertSizes(0, 0, 0);
	}

	@Test
	public void testControlMessages()
	{
		this.normalizer.processMessage("TEST", createUtpPacket(createIssueSymbolDirectoryMessage("AAPL", 100)), false);
		assertSizes(1, 0, 0);
		assertState("AAPL", 2, MarketSession.PREMARKET, TradingState.TRADING, 0, 0);

		this.normalizer.processMessage("TEST", createUtpPacket(createMarketSessionOpen()), false);
		assertSizes(1, 0, 0);
		assertState("AAPL", 2, MarketSession.NORMAL, TradingState.TRADING, 0, 0);

		this.normalizer.processMessage("TEST", createUtpPacket(createMarketSessionClose()), false);
		assertSizes(1, 0, 0);
		assertState("AAPL", 2, MarketSession.POSTMARKET, TradingState.TRADING, 0, 0);
	}

	@Test
	public void testMWCBStatusMessage()
	{
		this.normalizer.processMessage("TEST", createUtpPacket(createMWCBStatusMessage('1')), false);
		assertSizes(0, 0, 0);
		this.normalizer.processMessage("TEST", createUtpPacket(createMWCBStatusMessage('2')), false);
		assertSizes(0, 0, 0);
		this.normalizer.processMessage("TEST", createUtpPacket(createMWCBStatusMessage('3')), false);
		assertSizes(0, 0, 0);
	}

	@Test
	public void testLongQuoteUnchangedNBBO()
	{
		this.normalizer.processMessage("TEST", createUtpPacket(createLongQuote("AAPL", 401, 403, 1, 4, 'F', ' ', ' ', '0', ' ', 401.01, 402.99, 2, 3, 'Y')), false);
		assertSizes(1, 1, 0);
		assertState("AAPL", 2, MarketSession.NORMAL, TradingState.TRADING, 0, 0);
		assertBBO("AAPL", 8194, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, '\u0000');

		char[] quoteConditions = { 'I', 'L', 'N', 'U', 'X', 'Z' };
		for (char c : quoteConditions)
		{
			this.normalizer.processMessage("TEST", createUtpPacket(createLongQuote("AAPL", 401, 403, 1, 4, c, ' ', ' ', '0', ' ', 401.01, 402.99, 2, 3, 'Y')), false);
			assertSizes(0, 1, 0);
			assertBBO("AAPL", 8194, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, '\u0000');
		}

		this.normalizer.processMessage("TEST", createUtpPacket(createLongQuote("AAPL", 401, 403, 1, 4, ' ', 'B', ' ', '0', ' ', 401.01, 402.99, 2, 3, 'Y')), false);
		assertSizes(0, 1, 0);
		assertBBO("AAPL", 32770, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, '\u0000');

		this.normalizer.processMessage("TEST", createUtpPacket(createLongQuote("AAPL", 401, 403, 1, 4, ' ', 'A', ' ', '0', ' ', 401.01, 402.99, 2, 3, 'Y')), false);
		assertSizes(0, 1, 0);
		assertBBO("AAPL", 16386, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, '\u0000');

		this.normalizer.processMessage("TEST", createUtpPacket(createLongQuote("AAPL", 401, 403, 1, 4, ' ', 'C', ' ', '0', ' ', 401.01, 402.99, 2, 3, 'Y')), false);
		assertSizes(0, 1, 0);
		assertBBO("AAPL", 32770, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, '\u0000');

		this.normalizer.processMessage("TEST", createUtpPacket(createLongQuote("AAPL", 401, 403, 1, 4, ' ', ' ', 'A', '0', ' ', 401.01, 402.99, 2, 3, 'Y')), false);
		assertSizes(0, 1, 0);
		assertBBO("AAPL", 2050, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, '\u0000');

		this.normalizer.processMessage("TEST", createUtpPacket(createLongQuote("AAPL", 401, 403, 1, 4, ' ', ' ', 'B', '0', ' ', 401.01, 402.99, 2, 3, 'Y')), false);
		assertSizes(0, 1, 0);
		assertBBO("AAPL", 4098, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, '\u0000');

		this.normalizer.processMessage("TEST", createUtpPacket(createLongQuote("AAPL", 401, 403, 1, 4, ' ', ' ', 'C', '0', ' ', 401.01, 402.99, 2, 3, 'Y')), false);
		assertSizes(0, 1, 0);
		assertBBO("AAPL", 6146, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, '\u0000');

		this.normalizer.processMessage("TEST", createUtpPacket(createLongQuote("AAPL", 401, 403, 1, 4, ' ', ' ', ' ', '0', 'A', 401.01, 402.99, 2, 3, 'Y')), false);
		assertSizes(0, 1, 0);
		assertBBO("AAPL", 2, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, '\u0000');

		this.normalizer.processMessage("TEST", createUtpPacket(createLongQuote("AAPL", 401, 403, 1, 4, ' ', ' ', ' ', '0', 'B', 401.01, 402.99, 2, 3, 'Y')), false);
		assertSizes(1, 1, 0);
		assertState("AAPL", 4098, MarketSession.NORMAL, TradingState.TRADING, 0, 0);
		assertBBO("AAPL", 2, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, '\u0000');

		this.normalizer.processMessage("TEST", createUtpPacket(createLongQuote("AAPL", 401, 403, 1, 4, ' ', ' ', ' ', '0', 'C', 401.01, 402.99, 2, 3, 'Y')), false);
		assertSizes(1, 1, 0);
		assertState("AAPL", 8194, MarketSession.NORMAL, TradingState.TRADING, 0, 0);
		assertBBO("AAPL", 2, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, '\u0000');

		this.normalizer.processMessage("TEST", createUtpPacket(createLongQuote("AAPL", 401, 403, 1, 4, ' ', ' ', ' ', '0', 'D', 401.01, 402.99, 2, 3, 'Y')), false);
		assertSizes(1, 1, 0);
		assertState("AAPL", 12290, MarketSession.NORMAL, TradingState.TRADING, 0, 0);
		assertBBO("AAPL", 2, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, '\u0000');

		this.normalizer.processMessage("TEST", createUtpPacket(createLongQuote("AAPL", 401, 403, 1, 4, ' ', ' ', ' ', '0', 'E', 401.01, 402.99, 2, 3, 'Y')), false);
		assertSizes(1, 1, 0);
		assertState("AAPL", 16386, MarketSession.NORMAL, TradingState.TRADING, 0, 0);
		assertBBO("AAPL", 2, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, '\u0000');

		this.normalizer.processMessage("TEST", createUtpPacket(createLongQuote("AAPL", 401, 403, 1, 4, ' ', ' ', ' ', '0', 'F', 401.01, 402.99, 2, 3, 'Y')), false);
		assertSizes(1, 1, 0);
		assertState("AAPL", 32770, MarketSession.NORMAL, TradingState.TRADING, 0, 0);
		assertBBO("AAPL", 2, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, '\u0000');

		this.normalizer.processMessage("TEST", createUtpPacket(createLongQuote("AAPL", 401, 403, 1, 4, ' ', ' ', ' ', '0', 'G', 401.01, 402.99, 2, 3, 'Y')), false);
		assertSizes(1, 1, 0);
		assertState("AAPL", 24578, MarketSession.NORMAL, TradingState.TRADING, 0, 0);
		assertBBO("AAPL", 2, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, '\u0000');

		this.normalizer.processMessage("TEST", createUtpPacket(createLongQuote("AAPL", 401, 403, 1, 4, ' ', ' ', ' ', '0', 'H', 401.01, 402.99, 2, 3, 'Y')), false);
		assertSizes(1, 1, 0);
		assertState("AAPL", 36866, MarketSession.NORMAL, TradingState.TRADING, 0, 0);
		assertBBO("AAPL", 2, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, '\u0000');

		this.normalizer.processMessage("TEST", createUtpPacket(createLongQuote("AAPL", 401, 403, 1, 4, ' ', ' ', ' ', '0', 'I', 401.01, 402.99, 2, 3, 'Y')), false);
		assertSizes(1, 1, 0);
		assertState("AAPL", 49154, MarketSession.NORMAL, TradingState.TRADING, 0, 0);
		assertBBO("AAPL", 2, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, '\u0000');
	}

	@Test
	public void testLongQuoteNoNBBO()
	{
		this.normalizer.processMessage("TEST", createUtpPacket(createLongQuote("AAPL", 401, 403, 1, 4, 'F', ' ', ' ', '1', ' ', 401.01, 402.99, 2, 3, 'Y')), false);
		assertSizes(1, 1, 1);
		assertState("AAPL", 2, MarketSession.NORMAL, TradingState.TRADING, 0, 0);
		assertBBO("AAPL", 8194, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, '\u0000');
		assertNBBO("AAPL", 2, 0, 0, 0, 0, null, null, 'F');

		char[] quoteConditions = { 'I', 'L', 'N', 'U', 'X', 'Z' };
		for (char c : quoteConditions)
		{
			this.normalizer.processMessage("TEST", createUtpPacket(createLongQuote("AAPL", 401, 403, 1, 4, c, ' ', ' ', '1', ' ', 401.01, 402.99, 2, 3, 'Y')), false);
			assertSizes(0, 1, 1);
			assertBBO("AAPL", 8194, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, '\u0000');
			assertNBBO("AAPL", 2, 0, 0, 0, 0, null, null, c);
		}

		this.normalizer.processMessage("TEST", createUtpPacket(createLongQuote("AAPL", 401, 403, 1, 4, ' ', 'B', ' ', '1', ' ', 401.01, 402.99, 2, 3, 'Y')), false);
		assertSizes(0, 1, 1);
		assertBBO("AAPL", 32770, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, '\u0000');
		assertNBBO("AAPL", 2, 0, 0, 0, 0, null, null, ' ');

		this.normalizer.processMessage("TEST", createUtpPacket(createLongQuote("AAPL", 401, 403, 1, 4, ' ', 'A', ' ', '1', ' ', 401.01, 402.99, 2, 3, 'Y')), false);
		assertSizes(0, 1, 1);
		assertBBO("AAPL", 16386, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, '\u0000');
		assertNBBO("AAPL", 2, 0, 0, 0, 0, null, null, ' ');

		this.normalizer.processMessage("TEST", createUtpPacket(createLongQuote("AAPL", 401, 403, 1, 4, ' ', 'C', ' ', '1', ' ', 401.01, 402.99, 2, 3, 'Y')), false);
		assertSizes(0, 1, 1);
		assertBBO("AAPL", 32770, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, '\u0000');
		assertNBBO("AAPL", 2, 0, 0, 0, 0, null, null, ' ');

		this.normalizer.processMessage("TEST", createUtpPacket(createLongQuote("AAPL", 401, 403, 1, 4, ' ', ' ', 'A', '1', ' ', 401.01, 402.99, 2, 3, 'Y')), false);
		assertSizes(0, 1, 1);
		assertBBO("AAPL", 2050, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, '\u0000');
		assertNBBO("AAPL", 2, 0, 0, 0, 0, null, null, ' ');

		this.normalizer.processMessage("TEST", createUtpPacket(createLongQuote("AAPL", 401, 403, 1, 4, ' ', ' ', 'B', '1', ' ', 401.01, 402.99, 2, 3, 'Y')), false);
		assertSizes(0, 1, 1);
		assertBBO("AAPL", 4098, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, '\u0000');
		assertNBBO("AAPL", 2, 0, 0, 0, 0, null, null, ' ');

		this.normalizer.processMessage("TEST", createUtpPacket(createLongQuote("AAPL", 401, 403, 1, 4, ' ', ' ', 'C', '1', ' ', 401.01, 402.99, 2, 3, 'Y')), false);
		assertSizes(0, 1, 1);
		assertBBO("AAPL", 6146, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, '\u0000');
		assertNBBO("AAPL", 2, 0, 0, 0, 0, null, null, ' ');

		this.normalizer.processMessage("TEST", createUtpPacket(createLongQuote("AAPL", 401, 403, 1, 4, ' ', ' ', ' ', '1', 'A', 401.01, 402.99, 2, 3, 'Y')), false);
		assertSizes(0, 1, 1);
		assertBBO("AAPL", 2, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, '\u0000');
		assertNBBO("AAPL", 2, 0, 0, 0, 0, null, null, ' ');

		this.normalizer.processMessage("TEST", createUtpPacket(createLongQuote("AAPL", 401, 403, 1, 4, ' ', ' ', ' ', '1', 'B', 401.01, 402.99, 2, 3, 'Y')), false);
		assertSizes(1, 1, 1);
		assertState("AAPL", 4098, MarketSession.NORMAL, TradingState.TRADING, 0, 0);
		assertBBO("AAPL", 2, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, '\u0000');
		assertNBBO("AAPL", 2, 0, 0, 0, 0, null, null, ' ');

		this.normalizer.processMessage("TEST", createUtpPacket(createLongQuote("AAPL", 401, 403, 1, 4, ' ', ' ', ' ', '1', 'C', 401.01, 402.99, 2, 3, 'Y')), false);
		assertSizes(1, 1, 1);
		assertState("AAPL", 8194, MarketSession.NORMAL, TradingState.TRADING, 0, 0);
		assertBBO("AAPL", 2, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, '\u0000');
		assertNBBO("AAPL", 2, 0, 0, 0, 0, null, null, ' ');

		this.normalizer.processMessage("TEST", createUtpPacket(createLongQuote("AAPL", 401, 403, 1, 4, ' ', ' ', ' ', '1', 'D', 401.01, 402.99, 2, 3, 'Y')), false);
		assertSizes(1, 1, 1);
		assertState("AAPL", 12290, MarketSession.NORMAL, TradingState.TRADING, 0, 0);
		assertBBO("AAPL", 2, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, '\u0000');
		assertNBBO("AAPL", 2, 0, 0, 0, 0, null, null, ' ');

		this.normalizer.processMessage("TEST", createUtpPacket(createLongQuote("AAPL", 401, 403, 1, 4, ' ', ' ', ' ', '1', 'E', 401.01, 402.99, 2, 3, 'Y')), false);
		assertSizes(1, 1, 1);
		assertState("AAPL", 16386, MarketSession.NORMAL, TradingState.TRADING, 0, 0);
		assertBBO("AAPL", 2, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, '\u0000');
		assertNBBO("AAPL", 2, 0, 0, 0, 0, null, null, ' ');

		this.normalizer.processMessage("TEST", createUtpPacket(createLongQuote("AAPL", 401, 403, 1, 4, ' ', ' ', ' ', '1', 'F', 401.01, 402.99, 2, 3, 'Y')), false);
		assertSizes(1, 1, 1);
		assertState("AAPL", 32770, MarketSession.NORMAL, TradingState.TRADING, 0, 0);
		assertBBO("AAPL", 2, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, '\u0000');
		assertNBBO("AAPL", 2, 0, 0, 0, 0, null, null, ' ');

		this.normalizer.processMessage("TEST", createUtpPacket(createLongQuote("AAPL", 401, 403, 1, 4, ' ', ' ', ' ', '1', 'G', 401.01, 402.99, 2, 3, 'Y')), false);
		assertSizes(1, 1, 1);
		assertState("AAPL", 24578, MarketSession.NORMAL, TradingState.TRADING, 0, 0);
		assertBBO("AAPL", 2, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, '\u0000');
		assertNBBO("AAPL", 2, 0, 0, 0, 0, null, null, ' ');

		this.normalizer.processMessage("TEST", createUtpPacket(createLongQuote("AAPL", 401, 403, 1, 4, ' ', ' ', ' ', '1', 'H', 401.01, 402.99, 2, 3, 'Y')), false);
		assertSizes(1, 1, 1);
		assertState("AAPL", 36866, MarketSession.NORMAL, TradingState.TRADING, 0, 0);
		assertBBO("AAPL", 2, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, '\u0000');
		assertNBBO("AAPL", 2, 0, 0, 0, 0, null, null, ' ');

		this.normalizer.processMessage("TEST", createUtpPacket(createLongQuote("AAPL", 401, 403, 1, 4, ' ', ' ', ' ', '1', 'I', 401.01, 402.99, 2, 3, 'Y')), false);
		assertSizes(1, 1, 1);
		assertState("AAPL", 49154, MarketSession.NORMAL, TradingState.TRADING, 0, 0);
		assertBBO("AAPL", 2, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, '\u0000');
		assertNBBO("AAPL", 2, 0, 0, 0, 0, null, null, ' ');
	}

	@Test
	public void testLongQuoteShortNBBO()
	{
		this.normalizer.processMessage("TEST", createUtpPacket(createLongQuote("AAPL", 401, 403, 1, 4, 'F', ' ', ' ', '2', ' ', 401.01, 402.99, 2, 3, 'Y')), false);
		assertSizes(1, 1, 1);
		assertState("AAPL", 2, MarketSession.NORMAL, TradingState.TRADING, 0, 0);
		assertBBO("AAPL", 8194, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, '\u0000');
		assertNBBO("AAPL", 2, 401.01, 402.99, 200, 300, Exchange.USEQ_BATS_Y_EXCHANGE, Exchange.USEQ_BATS_Y_EXCHANGE, 'F');

		char[] quoteConditions = { 'I', 'L', 'N', 'U', 'X', 'Z' };
		for (char c : quoteConditions)
		{
			this.normalizer.processMessage("TEST", createUtpPacket(createLongQuote("AAPL", 401, 403, 1, 4, c, ' ', ' ', '2', ' ', 401.01, 402.99, 2, 3, 'Y')), false);
			assertSizes(0, 1, 1);
			assertBBO("AAPL", 8194, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, '\u0000');
			assertNBBO("AAPL", 2, 401.01, 402.99, 200, 300, Exchange.USEQ_BATS_Y_EXCHANGE, Exchange.USEQ_BATS_Y_EXCHANGE, c);
		}

		this.normalizer.processMessage("TEST", createUtpPacket(createLongQuote("AAPL", 401, 403, 1, 4, ' ', 'B', ' ', '2', ' ', 401.01, 402.99, 2, 3, 'Y')), false);
		assertSizes(0, 1, 1);
		assertBBO("AAPL", 32770, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, '\u0000');
		assertNBBO("AAPL", 2, 401.01, 402.99, 200, 300, Exchange.USEQ_BATS_Y_EXCHANGE, Exchange.USEQ_BATS_Y_EXCHANGE, ' ');

		this.normalizer.processMessage("TEST", createUtpPacket(createLongQuote("AAPL", 401, 403, 1, 4, ' ', 'A', ' ', '2', ' ', 401.01, 402.99, 2, 3, 'Y')), false);
		assertSizes(0, 1, 1);
		assertBBO("AAPL", 16386, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, '\u0000');
		assertNBBO("AAPL", 2, 401.01, 402.99, 200, 300, Exchange.USEQ_BATS_Y_EXCHANGE, Exchange.USEQ_BATS_Y_EXCHANGE, ' ');

		this.normalizer.processMessage("TEST", createUtpPacket(createLongQuote("AAPL", 401, 403, 1, 4, ' ', 'C', ' ', '2', ' ', 401.01, 402.99, 2, 3, 'Y')), false);
		assertSizes(0, 1, 1);
		assertBBO("AAPL", 32770, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, '\u0000');
		assertNBBO("AAPL", 2, 401.01, 402.99, 200, 300, Exchange.USEQ_BATS_Y_EXCHANGE, Exchange.USEQ_BATS_Y_EXCHANGE, ' ');

		this.normalizer.processMessage("TEST", createUtpPacket(createLongQuote("AAPL", 401, 403, 1, 4, ' ', ' ', 'A', '2', ' ', 401.01, 402.99, 2, 3, 'Y')), false);
		assertSizes(0, 1, 1);
		assertBBO("AAPL", 2050, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, '\u0000');
		assertNBBO("AAPL", 2, 401.01, 402.99, 200, 300, Exchange.USEQ_BATS_Y_EXCHANGE, Exchange.USEQ_BATS_Y_EXCHANGE, ' ');

		this.normalizer.processMessage("TEST", createUtpPacket(createLongQuote("AAPL", 401, 403, 1, 4, ' ', ' ', 'B', '2', ' ', 401.01, 402.99, 2, 3, 'Y')), false);
		assertSizes(0, 1, 1);
		assertBBO("AAPL", 4098, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, '\u0000');
		assertNBBO("AAPL", 2, 401.01, 402.99, 200, 300, Exchange.USEQ_BATS_Y_EXCHANGE, Exchange.USEQ_BATS_Y_EXCHANGE, ' ');

		this.normalizer.processMessage("TEST", createUtpPacket(createLongQuote("AAPL", 401, 403, 1, 4, ' ', ' ', 'C', '2', ' ', 401.01, 402.99, 2, 3, 'Y')), false);
		assertSizes(0, 1, 1);
		assertBBO("AAPL", 6146, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, '\u0000');
		assertNBBO("AAPL", 2, 401.01, 402.99, 200, 300, Exchange.USEQ_BATS_Y_EXCHANGE, Exchange.USEQ_BATS_Y_EXCHANGE, ' ');

		this.normalizer.processMessage("TEST", createUtpPacket(createLongQuote("AAPL", 401, 403, 1, 4, ' ', ' ', ' ', '2', 'A', 401.01, 402.99, 2, 3, 'Y')), false);
		assertSizes(0, 1, 1);
		assertBBO("AAPL", 2, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, '\u0000');
		assertNBBO("AAPL", 2, 401.01, 402.99, 200, 300, Exchange.USEQ_BATS_Y_EXCHANGE, Exchange.USEQ_BATS_Y_EXCHANGE, ' ');

		this.normalizer.processMessage("TEST", createUtpPacket(createLongQuote("AAPL", 401, 403, 1, 4, ' ', ' ', ' ', '2', 'B', 401.01, 402.99, 2, 3, 'Y')), false);
		assertSizes(1, 1, 1);
		assertState("AAPL", 4098, MarketSession.NORMAL, TradingState.TRADING, 0, 0);
		assertBBO("AAPL", 2, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, '\u0000');
		assertNBBO("AAPL", 2, 401.01, 402.99, 200, 300, Exchange.USEQ_BATS_Y_EXCHANGE, Exchange.USEQ_BATS_Y_EXCHANGE, ' ');

		this.normalizer.processMessage("TEST", createUtpPacket(createLongQuote("AAPL", 401, 403, 1, 4, ' ', ' ', ' ', '2', 'C', 401.01, 402.99, 2, 3, 'Y')), false);
		assertSizes(1, 1, 1);
		assertState("AAPL", 8194, MarketSession.NORMAL, TradingState.TRADING, 0, 0);
		assertBBO("AAPL", 2, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, '\u0000');
		assertNBBO("AAPL", 2, 401.01, 402.99, 200, 300, Exchange.USEQ_BATS_Y_EXCHANGE, Exchange.USEQ_BATS_Y_EXCHANGE, ' ');

		this.normalizer.processMessage("TEST", createUtpPacket(createLongQuote("AAPL", 401, 403, 1, 4, ' ', ' ', ' ', '2', 'D', 401.01, 402.99, 2, 3, 'Y')), false);
		assertSizes(1, 1, 1);
		assertState("AAPL", 12290, MarketSession.NORMAL, TradingState.TRADING, 0, 0);
		assertBBO("AAPL", 2, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, '\u0000');
		assertNBBO("AAPL", 2, 401.01, 402.99, 200, 300, Exchange.USEQ_BATS_Y_EXCHANGE, Exchange.USEQ_BATS_Y_EXCHANGE, ' ');

		this.normalizer.processMessage("TEST", createUtpPacket(createLongQuote("AAPL", 401, 403, 1, 4, ' ', ' ', ' ', '2', 'E', 401.01, 402.99, 2, 3, 'Y')), false);
		assertSizes(1, 1, 1);
		assertState("AAPL", 16386, MarketSession.NORMAL, TradingState.TRADING, 0, 0);
		assertBBO("AAPL", 2, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, '\u0000');
		assertNBBO("AAPL", 2, 401.01, 402.99, 200, 300, Exchange.USEQ_BATS_Y_EXCHANGE, Exchange.USEQ_BATS_Y_EXCHANGE, ' ');

		this.normalizer.processMessage("TEST", createUtpPacket(createLongQuote("AAPL", 401, 403, 1, 4, ' ', ' ', ' ', '2', 'F', 401.01, 402.99, 2, 3, 'Y')), false);
		assertSizes(1, 1, 1);
		assertState("AAPL", 32770, MarketSession.NORMAL, TradingState.TRADING, 0, 0);
		assertBBO("AAPL", 2, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, '\u0000');
		assertNBBO("AAPL", 2, 401.01, 402.99, 200, 300, Exchange.USEQ_BATS_Y_EXCHANGE, Exchange.USEQ_BATS_Y_EXCHANGE, ' ');

		this.normalizer.processMessage("TEST", createUtpPacket(createLongQuote("AAPL", 401, 403, 1, 4, ' ', ' ', ' ', '2', 'G', 401.01, 402.99, 2, 3, 'Y')), false);
		assertSizes(1, 1, 1);
		assertState("AAPL", 24578, MarketSession.NORMAL, TradingState.TRADING, 0, 0);
		assertBBO("AAPL", 2, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, '\u0000');
		assertNBBO("AAPL", 2, 401.01, 402.99, 200, 300, Exchange.USEQ_BATS_Y_EXCHANGE, Exchange.USEQ_BATS_Y_EXCHANGE, ' ');

		this.normalizer.processMessage("TEST", createUtpPacket(createLongQuote("AAPL", 401, 403, 1, 4, ' ', ' ', ' ', '2', 'H', 401.01, 402.99, 2, 3, 'Y')), false);
		assertSizes(1, 1, 1);
		assertState("AAPL", 36866, MarketSession.NORMAL, TradingState.TRADING, 0, 0);
		assertBBO("AAPL", 2, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, '\u0000');
		assertNBBO("AAPL", 2, 401.01, 402.99, 200, 300, Exchange.USEQ_BATS_Y_EXCHANGE, Exchange.USEQ_BATS_Y_EXCHANGE, ' ');

		this.normalizer.processMessage("TEST", createUtpPacket(createLongQuote("AAPL", 401, 403, 1, 4, ' ', ' ', ' ', '2', 'I', 401.01, 402.99, 2, 3, 'Y')), false);
		assertSizes(1, 1, 1);
		assertState("AAPL", 49154, MarketSession.NORMAL, TradingState.TRADING, 0, 0);
		assertBBO("AAPL", 2, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, '\u0000');
		assertNBBO("AAPL", 2, 401.01, 402.99, 200, 300, Exchange.USEQ_BATS_Y_EXCHANGE, Exchange.USEQ_BATS_Y_EXCHANGE, ' ');
	}

	@Test
	public void testLongQuoteLongNBBO()
	{
		this.normalizer.processMessage("TEST", createUtpPacket(createLongQuote("AAPL", 401, 403, 1, 4, 'F', ' ', ' ', '3', ' ', 401.01, 402.99, 2, 3, 'Y')), false);
		assertSizes(1, 1, 1);
		assertState("AAPL", 2, MarketSession.NORMAL, TradingState.TRADING, 0, 0);
		assertBBO("AAPL", 8194, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, '\u0000');
		assertNBBO("AAPL", 2, 401.01, 402.99, 200, 300, Exchange.USEQ_BATS_Y_EXCHANGE, Exchange.USEQ_BATS_Y_EXCHANGE, 'F');

		char[] quoteConditions = { 'I', 'L', 'N', 'U', 'X', 'Z' };
		for (char c : quoteConditions)
		{
			this.normalizer.processMessage("TEST", createUtpPacket(createLongQuote("AAPL", 401, 403, 1, 4, c, ' ', ' ', '3', ' ', 401.01, 402.99, 2, 3, 'Y')), false);
			assertSizes(0, 1, 1);
			assertBBO("AAPL", 8194, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, '\u0000');
			assertNBBO("AAPL", 2, 401.01, 402.99, 200, 300, Exchange.USEQ_BATS_Y_EXCHANGE, Exchange.USEQ_BATS_Y_EXCHANGE, c);
		}

		this.normalizer.processMessage("TEST", createUtpPacket(createLongQuote("AAPL", 401, 403, 1, 4, ' ', 'B', ' ', '3', ' ', 401.01, 402.99, 2, 3, 'Y')), false);
		assertSizes(0, 1, 1);
		assertBBO("AAPL", 32770, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, '\u0000');
		assertNBBO("AAPL", 2, 401.01, 402.99, 200, 300, Exchange.USEQ_BATS_Y_EXCHANGE, Exchange.USEQ_BATS_Y_EXCHANGE, ' ');

		this.normalizer.processMessage("TEST", createUtpPacket(createLongQuote("AAPL", 401, 403, 1, 4, ' ', 'A', ' ', '3', ' ', 401.01, 402.99, 2, 3, 'Y')), false);
		assertSizes(0, 1, 1);
		assertBBO("AAPL", 16386, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, '\u0000');
		assertNBBO("AAPL", 2, 401.01, 402.99, 200, 300, Exchange.USEQ_BATS_Y_EXCHANGE, Exchange.USEQ_BATS_Y_EXCHANGE, ' ');

		this.normalizer.processMessage("TEST", createUtpPacket(createLongQuote("AAPL", 401, 403, 1, 4, ' ', 'C', ' ', '3', ' ', 401.01, 402.99, 2, 3, 'Y')), false);
		assertSizes(0, 1, 1);
		assertBBO("AAPL", 32770, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, '\u0000');
		assertNBBO("AAPL", 2, 401.01, 402.99, 200, 300, Exchange.USEQ_BATS_Y_EXCHANGE, Exchange.USEQ_BATS_Y_EXCHANGE, ' ');

		this.normalizer.processMessage("TEST", createUtpPacket(createLongQuote("AAPL", 401, 403, 1, 4, ' ', ' ', 'A', '3', ' ', 401.01, 402.99, 2, 3, 'Y')), false);
		assertSizes(0, 1, 1);
		assertBBO("AAPL", 2050, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, '\u0000');
		assertNBBO("AAPL", 2, 401.01, 402.99, 200, 300, Exchange.USEQ_BATS_Y_EXCHANGE, Exchange.USEQ_BATS_Y_EXCHANGE, ' ');

		this.normalizer.processMessage("TEST", createUtpPacket(createLongQuote("AAPL", 401, 403, 1, 4, ' ', ' ', 'B', '3', ' ', 401.01, 402.99, 2, 3, 'Y')), false);
		assertSizes(0, 1, 1);
		assertBBO("AAPL", 4098, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, '\u0000');
		assertNBBO("AAPL", 2, 401.01, 402.99, 200, 300, Exchange.USEQ_BATS_Y_EXCHANGE, Exchange.USEQ_BATS_Y_EXCHANGE, ' ');

		this.normalizer.processMessage("TEST", createUtpPacket(createLongQuote("AAPL", 401, 403, 1, 4, ' ', ' ', 'C', '3', ' ', 401.01, 402.99, 2, 3, 'Y')), false);
		assertSizes(0, 1, 1);
		assertBBO("AAPL", 6146, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, '\u0000');
		assertNBBO("AAPL", 2, 401.01, 402.99, 200, 300, Exchange.USEQ_BATS_Y_EXCHANGE, Exchange.USEQ_BATS_Y_EXCHANGE, ' ');

		this.normalizer.processMessage("TEST", createUtpPacket(createLongQuote("AAPL", 401, 403, 1, 4, ' ', ' ', ' ', '3', 'A', 401.01, 402.99, 2, 3, 'Y')), false);
		assertSizes(0, 1, 1);
		assertBBO("AAPL", 2, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, '\u0000');
		assertNBBO("AAPL", 2, 401.01, 402.99, 200, 300, Exchange.USEQ_BATS_Y_EXCHANGE, Exchange.USEQ_BATS_Y_EXCHANGE, ' ');

		this.normalizer.processMessage("TEST", createUtpPacket(createLongQuote("AAPL", 401, 403, 1, 4, ' ', ' ', ' ', '3', 'B', 401.01, 402.99, 2, 3, 'Y')), false);
		assertSizes(1, 1, 1);
		assertState("AAPL", 4098, MarketSession.NORMAL, TradingState.TRADING, 0, 0);
		assertBBO("AAPL", 2, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, '\u0000');
		assertNBBO("AAPL", 2, 401.01, 402.99, 200, 300, Exchange.USEQ_BATS_Y_EXCHANGE, Exchange.USEQ_BATS_Y_EXCHANGE, ' ');

		this.normalizer.processMessage("TEST", createUtpPacket(createLongQuote("AAPL", 401, 403, 1, 4, ' ', ' ', ' ', '3', 'C', 401.01, 402.99, 2, 3, 'Y')), false);
		assertSizes(1, 1, 1);
		assertState("AAPL", 8194, MarketSession.NORMAL, TradingState.TRADING, 0, 0);
		assertBBO("AAPL", 2, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, '\u0000');
		assertNBBO("AAPL", 2, 401.01, 402.99, 200, 300, Exchange.USEQ_BATS_Y_EXCHANGE, Exchange.USEQ_BATS_Y_EXCHANGE, ' ');

		this.normalizer.processMessage("TEST", createUtpPacket(createLongQuote("AAPL", 401, 403, 1, 4, ' ', ' ', ' ', '3', 'D', 401.01, 402.99, 2, 3, 'Y')), false);
		assertSizes(1, 1, 1);
		assertState("AAPL", 12290, MarketSession.NORMAL, TradingState.TRADING, 0, 0);
		assertBBO("AAPL", 2, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, '\u0000');
		assertNBBO("AAPL", 2, 401.01, 402.99, 200, 300, Exchange.USEQ_BATS_Y_EXCHANGE, Exchange.USEQ_BATS_Y_EXCHANGE, ' ');

		this.normalizer.processMessage("TEST", createUtpPacket(createLongQuote("AAPL", 401, 403, 1, 4, ' ', ' ', ' ', '3', 'E', 401.01, 402.99, 2, 3, 'Y')), false);
		assertSizes(1, 1, 1);
		assertState("AAPL", 16386, MarketSession.NORMAL, TradingState.TRADING, 0, 0);
		assertBBO("AAPL", 2, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, '\u0000');
		assertNBBO("AAPL", 2, 401.01, 402.99, 200, 300, Exchange.USEQ_BATS_Y_EXCHANGE, Exchange.USEQ_BATS_Y_EXCHANGE, ' ');

		this.normalizer.processMessage("TEST", createUtpPacket(createLongQuote("AAPL", 401, 403, 1, 4, ' ', ' ', ' ', '3', 'F', 401.01, 402.99, 2, 3, 'Y')), false);
		assertSizes(1, 1, 1);
		assertState("AAPL", 32770, MarketSession.NORMAL, TradingState.TRADING, 0, 0);
		assertBBO("AAPL", 2, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, '\u0000');
		assertNBBO("AAPL", 2, 401.01, 402.99, 200, 300, Exchange.USEQ_BATS_Y_EXCHANGE, Exchange.USEQ_BATS_Y_EXCHANGE, ' ');

		this.normalizer.processMessage("TEST", createUtpPacket(createLongQuote("AAPL", 401, 403, 1, 4, ' ', ' ', ' ', '3', 'G', 401.01, 402.99, 2, 3, 'Y')), false);
		assertSizes(1, 1, 1);
		assertState("AAPL", 24578, MarketSession.NORMAL, TradingState.TRADING, 0, 0);
		assertBBO("AAPL", 2, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, '\u0000');
		assertNBBO("AAPL", 2, 401.01, 402.99, 200, 300, Exchange.USEQ_BATS_Y_EXCHANGE, Exchange.USEQ_BATS_Y_EXCHANGE, ' ');

		this.normalizer.processMessage("TEST", createUtpPacket(createLongQuote("AAPL", 401, 403, 1, 4, ' ', ' ', ' ', '3', 'H', 401.01, 402.99, 2, 3, 'Y')), false);
		assertSizes(1, 1, 1);
		assertState("AAPL", 36866, MarketSession.NORMAL, TradingState.TRADING, 0, 0);
		assertBBO("AAPL", 2, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, '\u0000');
		assertNBBO("AAPL", 2, 401.01, 402.99, 200, 300, Exchange.USEQ_BATS_Y_EXCHANGE, Exchange.USEQ_BATS_Y_EXCHANGE, ' ');

		this.normalizer.processMessage("TEST", createUtpPacket(createLongQuote("AAPL", 401, 403, 1, 4, ' ', ' ', ' ', '3', 'I', 401.01, 402.99, 2, 3, 'Y')), false);
		assertSizes(1, 1, 1);
		assertState("AAPL", 49154, MarketSession.NORMAL, TradingState.TRADING, 0, 0);
		assertBBO("AAPL", 2, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, '\u0000');
		assertNBBO("AAPL", 2, 401.01, 402.99, 200, 300, Exchange.USEQ_BATS_Y_EXCHANGE, Exchange.USEQ_BATS_Y_EXCHANGE, ' ');
	}

	@Test
	public void testLongQuoteContainedNBBO()
	{
		this.normalizer.processMessage("TEST", createUtpPacket(createLongQuote("AAPL", 401, 403, 1, 4, 'F', ' ', ' ', '4', ' ', 401.01, 402.99, 2, 3, 'Y')), false);
		assertSizes(1, 1, 1);
		assertState("AAPL", 2, MarketSession.NORMAL, TradingState.TRADING, 0, 0);
		assertBBO("AAPL", 8194, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, '\u0000');
		assertNBBO("AAPL", 2, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, 'F');

		char[] quoteConditions = { 'I', 'L', 'N', 'U', 'X', 'Z' };
		for (char c : quoteConditions)
		{
			this.normalizer.processMessage("TEST", createUtpPacket(createLongQuote("AAPL", 401, 403, 1, 4, c, ' ', ' ', '4', ' ', 401.01, 402.99, 2, 3, 'Y')), false);
			assertSizes(0, 1, 1);
			assertBBO("AAPL", 8194, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, '\u0000');
			assertNBBO("AAPL", 2, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, c);
		}

		this.normalizer.processMessage("TEST", createUtpPacket(createLongQuote("AAPL", 401, 403, 1, 4, ' ', 'B', ' ', '4', ' ', 401.01, 402.99, 2, 3, 'Y')), false);
		assertSizes(0, 1, 1);
		assertBBO("AAPL", 32770, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, '\u0000');
		assertNBBO("AAPL", 2, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, ' ');

		this.normalizer.processMessage("TEST", createUtpPacket(createLongQuote("AAPL", 401, 403, 1, 4, ' ', 'A', ' ', '4', ' ', 401.01, 402.99, 2, 3, 'Y')), false);
		assertSizes(0, 1, 1);
		assertBBO("AAPL", 16386, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, '\u0000');
		assertNBBO("AAPL", 2, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, ' ');

		this.normalizer.processMessage("TEST", createUtpPacket(createLongQuote("AAPL", 401, 403, 1, 4, ' ', 'C', ' ', '4', ' ', 401.01, 402.99, 2, 3, 'Y')), false);
		assertSizes(0, 1, 1);
		assertBBO("AAPL", 32770, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, '\u0000');
		assertNBBO("AAPL", 2, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, ' ');

		this.normalizer.processMessage("TEST", createUtpPacket(createLongQuote("AAPL", 401, 403, 1, 4, ' ', ' ', 'A', '4', ' ', 401.01, 402.99, 2, 3, 'Y')), false);
		assertSizes(0, 1, 1);
		assertBBO("AAPL", 2050, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, '\u0000');
		assertNBBO("AAPL", 2, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, ' ');

		this.normalizer.processMessage("TEST", createUtpPacket(createLongQuote("AAPL", 401, 403, 1, 4, ' ', ' ', 'B', '4', ' ', 401.01, 402.99, 2, 3, 'Y')), false);
		assertSizes(0, 1, 1);
		assertBBO("AAPL", 4098, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, '\u0000');
		assertNBBO("AAPL", 2, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, ' ');

		this.normalizer.processMessage("TEST", createUtpPacket(createLongQuote("AAPL", 401, 403, 1, 4, ' ', ' ', 'C', '4', ' ', 401.01, 402.99, 2, 3, 'Y')), false);
		assertSizes(0, 1, 1);
		assertBBO("AAPL", 6146, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, '\u0000');
		assertNBBO("AAPL", 2, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, ' ');

		this.normalizer.processMessage("TEST", createUtpPacket(createLongQuote("AAPL", 401, 403, 1, 4, ' ', ' ', ' ', '4', 'A', 401.01, 402.99, 2, 3, 'Y')), false);
		assertSizes(0, 1, 1);
		assertBBO("AAPL", 2, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, '\u0000');
		assertNBBO("AAPL", 2, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, ' ');

		this.normalizer.processMessage("TEST", createUtpPacket(createLongQuote("AAPL", 401, 403, 1, 4, ' ', ' ', ' ', '4', 'B', 401.01, 402.99, 2, 3, 'Y')), false);
		assertSizes(1, 1, 1);
		assertState("AAPL", 4098, MarketSession.NORMAL, TradingState.TRADING, 0, 0);
		assertBBO("AAPL", 2, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, '\u0000');
		assertNBBO("AAPL", 2, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, ' ');

		this.normalizer.processMessage("TEST", createUtpPacket(createLongQuote("AAPL", 401, 403, 1, 4, ' ', ' ', ' ', '4', 'C', 401.01, 402.99, 2, 3, 'Y')), false);
		assertSizes(1, 1, 1);
		assertState("AAPL", 8194, MarketSession.NORMAL, TradingState.TRADING, 0, 0);
		assertBBO("AAPL", 2, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, '\u0000');
		assertNBBO("AAPL", 2, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, ' ');

		this.normalizer.processMessage("TEST", createUtpPacket(createLongQuote("AAPL", 401, 403, 1, 4, ' ', ' ', ' ', '4', 'D', 401.01, 402.99, 2, 3, 'Y')), false);
		assertSizes(1, 1, 1);
		assertState("AAPL", 12290, MarketSession.NORMAL, TradingState.TRADING, 0, 0);
		assertBBO("AAPL", 2, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, '\u0000');
		assertNBBO("AAPL", 2, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, ' ');

		this.normalizer.processMessage("TEST", createUtpPacket(createLongQuote("AAPL", 401, 403, 1, 4, ' ', ' ', ' ', '4', 'E', 401.01, 402.99, 2, 3, 'Y')), false);
		assertSizes(1, 1, 1);
		assertState("AAPL", 16386, MarketSession.NORMAL, TradingState.TRADING, 0, 0);
		assertBBO("AAPL", 2, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, '\u0000');
		assertNBBO("AAPL", 2, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, ' ');

		this.normalizer.processMessage("TEST", createUtpPacket(createLongQuote("AAPL", 401, 403, 1, 4, ' ', ' ', ' ', '4', 'F', 401.01, 402.99, 2, 3, 'Y')), false);
		assertSizes(1, 1, 1);
		assertState("AAPL", 32770, MarketSession.NORMAL, TradingState.TRADING, 0, 0);
		assertBBO("AAPL", 2, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, '\u0000');
		assertNBBO("AAPL", 2, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, ' ');

		this.normalizer.processMessage("TEST", createUtpPacket(createLongQuote("AAPL", 401, 403, 1, 4, ' ', ' ', ' ', '4', 'G', 401.01, 402.99, 2, 3, 'Y')), false);
		assertSizes(1, 1, 1);
		assertState("AAPL", 24578, MarketSession.NORMAL, TradingState.TRADING, 0, 0);
		assertBBO("AAPL", 2, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, '\u0000');
		assertNBBO("AAPL", 2, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, ' ');

		this.normalizer.processMessage("TEST", createUtpPacket(createLongQuote("AAPL", 401, 403, 1, 4, ' ', ' ', ' ', '4', 'H', 401.01, 402.99, 2, 3, 'Y')), false);
		assertSizes(1, 1, 1);
		assertState("AAPL", 36866, MarketSession.NORMAL, TradingState.TRADING, 0, 0);
		assertBBO("AAPL", 2, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, '\u0000');
		assertNBBO("AAPL", 2, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, ' ');

		this.normalizer.processMessage("TEST", createUtpPacket(createLongQuote("AAPL", 401, 403, 1, 4, ' ', ' ', ' ', '4', 'I', 401.01, 402.99, 2, 3, 'Y')), false);
		assertSizes(1, 1, 1);
		assertState("AAPL", 49154, MarketSession.NORMAL, TradingState.TRADING, 0, 0);
		assertBBO("AAPL", 2, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, '\u0000');
		assertNBBO("AAPL", 2, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, ' ');
	}

	@Test
	public void testShortQuoteUnchangedNBBO()
	{
		this.normalizer.processMessage("TEST", createUtpPacket(createShortQuote("AAPL", 401, 403, 1, 4, 'F', ' ', '0', ' ', 401.01, 402.99, 2, 3, 'Y')), false);
		assertSizes(1, 1, 0);
		assertState("AAPL", 2, MarketSession.NORMAL, TradingState.TRADING, 0, 0);
		assertBBO("AAPL", 8194, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, '\u0000');

		char[] quoteConditions = { 'I', 'L', 'N', 'U', 'X', 'Z' };
		for (char c : quoteConditions)
		{
			this.normalizer.processMessage("TEST", createUtpPacket(createShortQuote("AAPL", 401, 403, 1, 4, c, ' ', '0', ' ', 401.01, 402.99, 2, 3, 'Y')), false);
			assertSizes(0, 1, 0);
			assertBBO("AAPL", 8194, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, '\u0000');
		}

		this.normalizer.processMessage("TEST", createUtpPacket(createShortQuote("AAPL", 401, 403, 1, 4, ' ', 'B', '0', ' ', 401.01, 402.99, 2, 3, 'Y')), false);
		assertSizes(0, 1, 0);
		assertBBO("AAPL", 32770, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, '\u0000');

		this.normalizer.processMessage("TEST", createUtpPacket(createShortQuote("AAPL", 401, 403, 1, 4, ' ', 'A', '0', ' ', 401.01, 402.99, 2, 3, 'Y')), false);
		assertSizes(0, 1, 0);
		assertBBO("AAPL", 16386, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, '\u0000');

		this.normalizer.processMessage("TEST", createUtpPacket(createShortQuote("AAPL", 401, 403, 1, 4, ' ', 'C', '0', ' ', 401.01, 402.99, 2, 3, 'Y')), false);
		assertSizes(0, 1, 0);
		assertBBO("AAPL", 32770, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, '\u0000');

		this.normalizer.processMessage("TEST", createUtpPacket(createShortQuote("AAPL", 401, 403, 1, 4, ' ', ' ', '0', 'A', 401.01, 402.99, 2, 3, 'Y')), false);
		assertSizes(0, 1, 0);
		assertBBO("AAPL", 2, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, '\u0000');

		this.normalizer.processMessage("TEST", createUtpPacket(createShortQuote("AAPL", 401, 403, 1, 4, ' ', ' ', '0', 'B', 401.01, 402.99, 2, 3, 'Y')), false);
		assertSizes(1, 1, 0);
		assertState("AAPL", 4098, MarketSession.NORMAL, TradingState.TRADING, 0, 0);
		assertBBO("AAPL", 2, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, '\u0000');

		this.normalizer.processMessage("TEST", createUtpPacket(createShortQuote("AAPL", 401, 403, 1, 4, ' ', ' ', '0', 'C', 401.01, 402.99, 2, 3, 'Y')), false);
		assertSizes(1, 1, 0);
		assertState("AAPL", 8194, MarketSession.NORMAL, TradingState.TRADING, 0, 0);
		assertBBO("AAPL", 2, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, '\u0000');

		this.normalizer.processMessage("TEST", createUtpPacket(createShortQuote("AAPL", 401, 403, 1, 4, ' ', ' ', '0', 'D', 401.01, 402.99, 2, 3, 'Y')), false);
		assertSizes(1, 1, 0);
		assertState("AAPL", 12290, MarketSession.NORMAL, TradingState.TRADING, 0, 0);
		assertBBO("AAPL", 2, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, '\u0000');

		this.normalizer.processMessage("TEST", createUtpPacket(createShortQuote("AAPL", 401, 403, 1, 4, ' ', ' ', '0', 'E', 401.01, 402.99, 2, 3, 'Y')), false);
		assertSizes(1, 1, 0);
		assertState("AAPL", 16386, MarketSession.NORMAL, TradingState.TRADING, 0, 0);
		assertBBO("AAPL", 2, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, '\u0000');

		this.normalizer.processMessage("TEST", createUtpPacket(createShortQuote("AAPL", 401, 403, 1, 4, ' ', ' ', '0', 'F', 401.01, 402.99, 2, 3, 'Y')), false);
		assertSizes(1, 1, 0);
		assertState("AAPL", 32770, MarketSession.NORMAL, TradingState.TRADING, 0, 0);
		assertBBO("AAPL", 2, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, '\u0000');

		this.normalizer.processMessage("TEST", createUtpPacket(createShortQuote("AAPL", 401, 403, 1, 4, ' ', ' ', '0', 'G', 401.01, 402.99, 2, 3, 'Y')), false);
		assertSizes(1, 1, 0);
		assertState("AAPL", 24578, MarketSession.NORMAL, TradingState.TRADING, 0, 0);
		assertBBO("AAPL", 2, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, '\u0000');

		this.normalizer.processMessage("TEST", createUtpPacket(createShortQuote("AAPL", 401, 403, 1, 4, ' ', ' ', '0', 'H', 401.01, 402.99, 2, 3, 'Y')), false);
		assertSizes(1, 1, 0);
		assertState("AAPL", 36866, MarketSession.NORMAL, TradingState.TRADING, 0, 0);
		assertBBO("AAPL", 2, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, '\u0000');

		this.normalizer.processMessage("TEST", createUtpPacket(createShortQuote("AAPL", 401, 403, 1, 4, ' ', ' ', '0', 'I', 401.01, 402.99, 2, 3, 'Y')), false);
		assertSizes(1, 1, 0);
		assertState("AAPL", 49154, MarketSession.NORMAL, TradingState.TRADING, 0, 0);
		assertBBO("AAPL", 2, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, '\u0000');
	}

	@Test
	public void testShortQuoteNoNBBO()
	{
		this.normalizer.processMessage("TEST", createUtpPacket(createShortQuote("AAPL", 401, 403, 1, 4, 'F', ' ', '1', ' ', 401.01, 402.99, 2, 3, 'Y')), false);
		assertSizes(1, 1, 1);
		assertState("AAPL", 2, MarketSession.NORMAL, TradingState.TRADING, 0, 0);
		assertBBO("AAPL", 8194, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, '\u0000');
		assertNBBO("AAPL", 2, 0, 0, 0, 0, null, null, 'F');

		char[] quoteConditions = { 'I', 'L', 'N', 'U', 'X', 'Z' };
		for (char c : quoteConditions)
		{
			this.normalizer.processMessage("TEST", createUtpPacket(createShortQuote("AAPL", 401, 403, 1, 4, c, ' ', '1', ' ', 401.01, 402.99, 2, 3, 'Y')), false);
			assertSizes(0, 1, 1);
			assertBBO("AAPL", 8194, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, '\u0000');
			assertNBBO("AAPL", 2, 0, 0, 0, 0, null, null, c);
		}

		this.normalizer.processMessage("TEST", createUtpPacket(createShortQuote("AAPL", 401, 403, 1, 4, ' ', 'B', '1', ' ', 401.01, 402.99, 2, 3, 'Y')), false);
		assertSizes(0, 1, 1);
		assertBBO("AAPL", 32770, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, '\u0000');
		assertNBBO("AAPL", 2, 0, 0, 0, 0, null, null, ' ');

		this.normalizer.processMessage("TEST", createUtpPacket(createShortQuote("AAPL", 401, 403, 1, 4, ' ', 'A', '1', ' ', 401.01, 402.99, 2, 3, 'Y')), false);
		assertSizes(0, 1, 1);
		assertBBO("AAPL", 16386, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, '\u0000');
		assertNBBO("AAPL", 2, 0, 0, 0, 0, null, null, ' ');

		this.normalizer.processMessage("TEST", createUtpPacket(createShortQuote("AAPL", 401, 403, 1, 4, ' ', 'C', '1', ' ', 401.01, 402.99, 2, 3, 'Y')), false);
		assertSizes(0, 1, 1);
		assertBBO("AAPL", 32770, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, '\u0000');
		assertNBBO("AAPL", 2, 0, 0, 0, 0, null, null, ' ');

		this.normalizer.processMessage("TEST", createUtpPacket(createShortQuote("AAPL", 401, 403, 1, 4, ' ', ' ', '1', 'A', 401.01, 402.99, 2, 3, 'Y')), false);
		assertSizes(0, 1, 1);
		assertBBO("AAPL", 2, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, '\u0000');
		assertNBBO("AAPL", 2, 0, 0, 0, 0, null, null, ' ');

		this.normalizer.processMessage("TEST", createUtpPacket(createShortQuote("AAPL", 401, 403, 1, 4, ' ', ' ', '1', 'B', 401.01, 402.99, 2, 3, 'Y')), false);
		assertSizes(1, 1, 1);
		assertState("AAPL", 4098, MarketSession.NORMAL, TradingState.TRADING, 0, 0);
		assertBBO("AAPL", 2, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, '\u0000');
		assertNBBO("AAPL", 2, 0, 0, 0, 0, null, null, ' ');

		this.normalizer.processMessage("TEST", createUtpPacket(createShortQuote("AAPL", 401, 403, 1, 4, ' ', ' ', '1', 'C', 401.01, 402.99, 2, 3, 'Y')), false);
		assertSizes(1, 1, 1);
		assertState("AAPL", 8194, MarketSession.NORMAL, TradingState.TRADING, 0, 0);
		assertBBO("AAPL", 2, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, '\u0000');
		assertNBBO("AAPL", 2, 0, 0, 0, 0, null, null, ' ');

		this.normalizer.processMessage("TEST", createUtpPacket(createShortQuote("AAPL", 401, 403, 1, 4, ' ', ' ', '1', 'D', 401.01, 402.99, 2, 3, 'Y')), false);
		assertSizes(1, 1, 1);
		assertState("AAPL", 12290, MarketSession.NORMAL, TradingState.TRADING, 0, 0);
		assertBBO("AAPL", 2, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, '\u0000');
		assertNBBO("AAPL", 2, 0, 0, 0, 0, null, null, ' ');

		this.normalizer.processMessage("TEST", createUtpPacket(createShortQuote("AAPL", 401, 403, 1, 4, ' ', ' ', '1', 'E', 401.01, 402.99, 2, 3, 'Y')), false);
		assertSizes(1, 1, 1);
		assertState("AAPL", 16386, MarketSession.NORMAL, TradingState.TRADING, 0, 0);
		assertBBO("AAPL", 2, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, '\u0000');
		assertNBBO("AAPL", 2, 0, 0, 0, 0, null, null, ' ');

		this.normalizer.processMessage("TEST", createUtpPacket(createShortQuote("AAPL", 401, 403, 1, 4, ' ', ' ', '1', 'F', 401.01, 402.99, 2, 3, 'Y')), false);
		assertSizes(1, 1, 1);
		assertState("AAPL", 32770, MarketSession.NORMAL, TradingState.TRADING, 0, 0);
		assertBBO("AAPL", 2, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, '\u0000');
		assertNBBO("AAPL", 2, 0, 0, 0, 0, null, null, ' ');

		this.normalizer.processMessage("TEST", createUtpPacket(createShortQuote("AAPL", 401, 403, 1, 4, ' ', ' ', '1', 'G', 401.01, 402.99, 2, 3, 'Y')), false);
		assertSizes(1, 1, 1);
		assertState("AAPL", 24578, MarketSession.NORMAL, TradingState.TRADING, 0, 0);
		assertBBO("AAPL", 2, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, '\u0000');
		assertNBBO("AAPL", 2, 0, 0, 0, 0, null, null, ' ');

		this.normalizer.processMessage("TEST", createUtpPacket(createShortQuote("AAPL", 401, 403, 1, 4, ' ', ' ', '1', 'H', 401.01, 402.99, 2, 3, 'Y')), false);
		assertSizes(1, 1, 1);
		assertState("AAPL", 36866, MarketSession.NORMAL, TradingState.TRADING, 0, 0);
		assertBBO("AAPL", 2, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, '\u0000');
		assertNBBO("AAPL", 2, 0, 0, 0, 0, null, null, ' ');

		this.normalizer.processMessage("TEST", createUtpPacket(createShortQuote("AAPL", 401, 403, 1, 4, ' ', ' ', '1', 'I', 401.01, 402.99, 2, 3, 'Y')), false);
		assertSizes(1, 1, 1);
		assertState("AAPL", 49154, MarketSession.NORMAL, TradingState.TRADING, 0, 0);
		assertBBO("AAPL", 2, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, '\u0000');
		assertNBBO("AAPL", 2, 0, 0, 0, 0, null, null, ' ');
	}

	@Test
	public void testShortQuoteShortNBBO()
	{
		this.normalizer.processMessage("TEST", createUtpPacket(createShortQuote("AAPL", 401, 403, 1, 4, 'F', ' ', '2', ' ', 401.01, 402.99, 2, 3, 'Y')), false);
		assertSizes(1, 1, 1);
		assertState("AAPL", 2, MarketSession.NORMAL, TradingState.TRADING, 0, 0);
		assertBBO("AAPL", 8194, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, '\u0000');
		assertNBBO("AAPL", 2, 401.01, 402.99, 200, 300, Exchange.USEQ_BATS_Y_EXCHANGE, Exchange.USEQ_BATS_Y_EXCHANGE, 'F');

		char[] quoteConditions = { 'I', 'L', 'N', 'U', 'X', 'Z' };
		for (char c : quoteConditions)
		{
			this.normalizer.processMessage("TEST", createUtpPacket(createShortQuote("AAPL", 401, 403, 1, 4, c, ' ', '2', ' ', 401.01, 402.99, 2, 3, 'Y')), false);
			assertSizes(0, 1, 1);
			assertBBO("AAPL", 8194, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, '\u0000');
			assertNBBO("AAPL", 2, 401.01, 402.99, 200, 300, Exchange.USEQ_BATS_Y_EXCHANGE, Exchange.USEQ_BATS_Y_EXCHANGE, c);
		}

		this.normalizer.processMessage("TEST", createUtpPacket(createShortQuote("AAPL", 401, 403, 1, 4, ' ', 'B', '2', ' ', 401.01, 402.99, 2, 3, 'Y')), false);
		assertSizes(0, 1, 1);
		assertBBO("AAPL", 32770, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, '\u0000');
		assertNBBO("AAPL", 2, 401.01, 402.99, 200, 300, Exchange.USEQ_BATS_Y_EXCHANGE, Exchange.USEQ_BATS_Y_EXCHANGE, ' ');

		this.normalizer.processMessage("TEST", createUtpPacket(createShortQuote("AAPL", 401, 403, 1, 4, ' ', 'A', '2', ' ', 401.01, 402.99, 2, 3, 'Y')), false);
		assertSizes(0, 1, 1);
		assertBBO("AAPL", 16386, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, '\u0000');
		assertNBBO("AAPL", 2, 401.01, 402.99, 200, 300, Exchange.USEQ_BATS_Y_EXCHANGE, Exchange.USEQ_BATS_Y_EXCHANGE, ' ');

		this.normalizer.processMessage("TEST", createUtpPacket(createShortQuote("AAPL", 401, 403, 1, 4, ' ', 'C', '2', ' ', 401.01, 402.99, 2, 3, 'Y')), false);
		assertSizes(0, 1, 1);
		assertBBO("AAPL", 32770, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, '\u0000');
		assertNBBO("AAPL", 2, 401.01, 402.99, 200, 300, Exchange.USEQ_BATS_Y_EXCHANGE, Exchange.USEQ_BATS_Y_EXCHANGE, ' ');

		this.normalizer.processMessage("TEST", createUtpPacket(createShortQuote("AAPL", 401, 403, 1, 4, ' ', ' ', '2', 'A', 401.01, 402.99, 2, 3, 'Y')), false);
		assertSizes(0, 1, 1);
		assertBBO("AAPL", 2, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, '\u0000');
		assertNBBO("AAPL", 2, 401.01, 402.99, 200, 300, Exchange.USEQ_BATS_Y_EXCHANGE, Exchange.USEQ_BATS_Y_EXCHANGE, ' ');

		this.normalizer.processMessage("TEST", createUtpPacket(createShortQuote("AAPL", 401, 403, 1, 4, ' ', ' ', '2', 'B', 401.01, 402.99, 2, 3, 'Y')), false);
		assertSizes(1, 1, 1);
		assertState("AAPL", 4098, MarketSession.NORMAL, TradingState.TRADING, 0, 0);
		assertBBO("AAPL", 2, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, '\u0000');
		assertNBBO("AAPL", 2, 401.01, 402.99, 200, 300, Exchange.USEQ_BATS_Y_EXCHANGE, Exchange.USEQ_BATS_Y_EXCHANGE, ' ');

		this.normalizer.processMessage("TEST", createUtpPacket(createShortQuote("AAPL", 401, 403, 1, 4, ' ', ' ', '2', 'C', 401.01, 402.99, 2, 3, 'Y')), false);
		assertSizes(1, 1, 1);
		assertState("AAPL", 8194, MarketSession.NORMAL, TradingState.TRADING, 0, 0);
		assertBBO("AAPL", 2, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, '\u0000');
		assertNBBO("AAPL", 2, 401.01, 402.99, 200, 300, Exchange.USEQ_BATS_Y_EXCHANGE, Exchange.USEQ_BATS_Y_EXCHANGE, ' ');

		this.normalizer.processMessage("TEST", createUtpPacket(createShortQuote("AAPL", 401, 403, 1, 4, ' ', ' ', '2', 'D', 401.01, 402.99, 2, 3, 'Y')), false);
		assertSizes(1, 1, 1);
		assertState("AAPL", 12290, MarketSession.NORMAL, TradingState.TRADING, 0, 0);
		assertBBO("AAPL", 2, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, '\u0000');
		assertNBBO("AAPL", 2, 401.01, 402.99, 200, 300, Exchange.USEQ_BATS_Y_EXCHANGE, Exchange.USEQ_BATS_Y_EXCHANGE, ' ');

		this.normalizer.processMessage("TEST", createUtpPacket(createShortQuote("AAPL", 401, 403, 1, 4, ' ', ' ', '2', 'E', 401.01, 402.99, 2, 3, 'Y')), false);
		assertSizes(1, 1, 1);
		assertState("AAPL", 16386, MarketSession.NORMAL, TradingState.TRADING, 0, 0);
		assertBBO("AAPL", 2, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, '\u0000');
		assertNBBO("AAPL", 2, 401.01, 402.99, 200, 300, Exchange.USEQ_BATS_Y_EXCHANGE, Exchange.USEQ_BATS_Y_EXCHANGE, ' ');

		this.normalizer.processMessage("TEST", createUtpPacket(createShortQuote("AAPL", 401, 403, 1, 4, ' ', ' ', '2', 'F', 401.01, 402.99, 2, 3, 'Y')), false);
		assertSizes(1, 1, 1);
		assertState("AAPL", 32770, MarketSession.NORMAL, TradingState.TRADING, 0, 0);
		assertBBO("AAPL", 2, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, '\u0000');
		assertNBBO("AAPL", 2, 401.01, 402.99, 200, 300, Exchange.USEQ_BATS_Y_EXCHANGE, Exchange.USEQ_BATS_Y_EXCHANGE, ' ');

		this.normalizer.processMessage("TEST", createUtpPacket(createShortQuote("AAPL", 401, 403, 1, 4, ' ', ' ', '2', 'G', 401.01, 402.99, 2, 3, 'Y')), false);
		assertSizes(1, 1, 1);
		assertState("AAPL", 24578, MarketSession.NORMAL, TradingState.TRADING, 0, 0);
		assertBBO("AAPL", 2, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, '\u0000');
		assertNBBO("AAPL", 2, 401.01, 402.99, 200, 300, Exchange.USEQ_BATS_Y_EXCHANGE, Exchange.USEQ_BATS_Y_EXCHANGE, ' ');

		this.normalizer.processMessage("TEST", createUtpPacket(createShortQuote("AAPL", 401, 403, 1, 4, ' ', ' ', '2', 'H', 401.01, 402.99, 2, 3, 'Y')), false);
		assertSizes(1, 1, 1);
		assertState("AAPL", 36866, MarketSession.NORMAL, TradingState.TRADING, 0, 0);
		assertBBO("AAPL", 2, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, '\u0000');
		assertNBBO("AAPL", 2, 401.01, 402.99, 200, 300, Exchange.USEQ_BATS_Y_EXCHANGE, Exchange.USEQ_BATS_Y_EXCHANGE, ' ');

		this.normalizer.processMessage("TEST", createUtpPacket(createShortQuote("AAPL", 401, 403, 1, 4, ' ', ' ', '2', 'I', 401.01, 402.99, 2, 3, 'Y')), false);
		assertSizes(1, 1, 1);
		assertState("AAPL", 49154, MarketSession.NORMAL, TradingState.TRADING, 0, 0);
		assertBBO("AAPL", 2, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, '\u0000');
		assertNBBO("AAPL", 2, 401.01, 402.99, 200, 300, Exchange.USEQ_BATS_Y_EXCHANGE, Exchange.USEQ_BATS_Y_EXCHANGE, ' ');

	}

	@Test
	public void testShortQuoteLongNBBO()
	{
		this.normalizer.processMessage("TEST", createUtpPacket(createShortQuote("AAPL", 401, 403, 1, 4, 'F', ' ', '3', ' ', 401.01, 402.99, 2, 3, 'Y')), false);
		assertSizes(1, 1, 1);
		assertState("AAPL", 2, MarketSession.NORMAL, TradingState.TRADING, 0, 0);
		assertBBO("AAPL", 8194, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, '\u0000');
		assertNBBO("AAPL", 2, 401.01, 402.99, 200, 300, Exchange.USEQ_BATS_Y_EXCHANGE, Exchange.USEQ_BATS_Y_EXCHANGE, 'F');

		char[] quoteConditions = { 'I', 'L', 'N', 'U', 'X', 'Z' };
		for (char c : quoteConditions)
		{
			this.normalizer.processMessage("TEST", createUtpPacket(createShortQuote("AAPL", 401, 403, 1, 4, c, ' ', '3', ' ', 401.01, 402.99, 2, 3, 'Y')), false);
			assertSizes(0, 1, 1);
			assertBBO("AAPL", 8194, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, '\u0000');
			assertNBBO("AAPL", 2, 401.01, 402.99, 200, 300, Exchange.USEQ_BATS_Y_EXCHANGE, Exchange.USEQ_BATS_Y_EXCHANGE, c);
		}

		this.normalizer.processMessage("TEST", createUtpPacket(createShortQuote("AAPL", 401, 403, 1, 4, ' ', 'B', '3', ' ', 401.01, 402.99, 2, 3, 'Y')), false);
		assertSizes(0, 1, 1);
		assertBBO("AAPL", 32770, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, '\u0000');
		assertNBBO("AAPL", 2, 401.01, 402.99, 200, 300, Exchange.USEQ_BATS_Y_EXCHANGE, Exchange.USEQ_BATS_Y_EXCHANGE, ' ');

		this.normalizer.processMessage("TEST", createUtpPacket(createShortQuote("AAPL", 401, 403, 1, 4, ' ', 'A', '3', ' ', 401.01, 402.99, 2, 3, 'Y')), false);
		assertSizes(0, 1, 1);
		assertBBO("AAPL", 16386, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, '\u0000');
		assertNBBO("AAPL", 2, 401.01, 402.99, 200, 300, Exchange.USEQ_BATS_Y_EXCHANGE, Exchange.USEQ_BATS_Y_EXCHANGE, ' ');

		this.normalizer.processMessage("TEST", createUtpPacket(createShortQuote("AAPL", 401, 403, 1, 4, ' ', 'C', '3', ' ', 401.01, 402.99, 2, 3, 'Y')), false);
		assertSizes(0, 1, 1);
		assertBBO("AAPL", 32770, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, '\u0000');
		assertNBBO("AAPL", 2, 401.01, 402.99, 200, 300, Exchange.USEQ_BATS_Y_EXCHANGE, Exchange.USEQ_BATS_Y_EXCHANGE, ' ');

		this.normalizer.processMessage("TEST", createUtpPacket(createShortQuote("AAPL", 401, 403, 1, 4, ' ', ' ', '3', 'A', 401.01, 402.99, 2, 3, 'Y')), false);
		assertSizes(0, 1, 1);
		assertBBO("AAPL", 2, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, '\u0000');
		assertNBBO("AAPL", 2, 401.01, 402.99, 200, 300, Exchange.USEQ_BATS_Y_EXCHANGE, Exchange.USEQ_BATS_Y_EXCHANGE, ' ');

		this.normalizer.processMessage("TEST", createUtpPacket(createShortQuote("AAPL", 401, 403, 1, 4, ' ', ' ', '3', 'B', 401.01, 402.99, 2, 3, 'Y')), false);
		assertSizes(1, 1, 1);
		assertState("AAPL", 4098, MarketSession.NORMAL, TradingState.TRADING, 0, 0);
		assertBBO("AAPL", 2, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, '\u0000');
		assertNBBO("AAPL", 2, 401.01, 402.99, 200, 300, Exchange.USEQ_BATS_Y_EXCHANGE, Exchange.USEQ_BATS_Y_EXCHANGE, ' ');

		this.normalizer.processMessage("TEST", createUtpPacket(createShortQuote("AAPL", 401, 403, 1, 4, ' ', ' ', '3', 'C', 401.01, 402.99, 2, 3, 'Y')), false);
		assertSizes(1, 1, 1);
		assertState("AAPL", 8194, MarketSession.NORMAL, TradingState.TRADING, 0, 0);
		assertBBO("AAPL", 2, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, '\u0000');
		assertNBBO("AAPL", 2, 401.01, 402.99, 200, 300, Exchange.USEQ_BATS_Y_EXCHANGE, Exchange.USEQ_BATS_Y_EXCHANGE, ' ');

		this.normalizer.processMessage("TEST", createUtpPacket(createShortQuote("AAPL", 401, 403, 1, 4, ' ', ' ', '3', 'D', 401.01, 402.99, 2, 3, 'Y')), false);
		assertSizes(1, 1, 1);
		assertState("AAPL", 12290, MarketSession.NORMAL, TradingState.TRADING, 0, 0);
		assertBBO("AAPL", 2, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, '\u0000');
		assertNBBO("AAPL", 2, 401.01, 402.99, 200, 300, Exchange.USEQ_BATS_Y_EXCHANGE, Exchange.USEQ_BATS_Y_EXCHANGE, ' ');

		this.normalizer.processMessage("TEST", createUtpPacket(createShortQuote("AAPL", 401, 403, 1, 4, ' ', ' ', '3', 'E', 401.01, 402.99, 2, 3, 'Y')), false);
		assertSizes(1, 1, 1);
		assertState("AAPL", 16386, MarketSession.NORMAL, TradingState.TRADING, 0, 0);
		assertBBO("AAPL", 2, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, '\u0000');
		assertNBBO("AAPL", 2, 401.01, 402.99, 200, 300, Exchange.USEQ_BATS_Y_EXCHANGE, Exchange.USEQ_BATS_Y_EXCHANGE, ' ');

		this.normalizer.processMessage("TEST", createUtpPacket(createShortQuote("AAPL", 401, 403, 1, 4, ' ', ' ', '3', 'F', 401.01, 402.99, 2, 3, 'Y')), false);
		assertSizes(1, 1, 1);
		assertState("AAPL", 32770, MarketSession.NORMAL, TradingState.TRADING, 0, 0);
		assertBBO("AAPL", 2, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, '\u0000');
		assertNBBO("AAPL", 2, 401.01, 402.99, 200, 300, Exchange.USEQ_BATS_Y_EXCHANGE, Exchange.USEQ_BATS_Y_EXCHANGE, ' ');

		this.normalizer.processMessage("TEST", createUtpPacket(createShortQuote("AAPL", 401, 403, 1, 4, ' ', ' ', '3', 'G', 401.01, 402.99, 2, 3, 'Y')), false);
		assertSizes(1, 1, 1);
		assertState("AAPL", 24578, MarketSession.NORMAL, TradingState.TRADING, 0, 0);
		assertBBO("AAPL", 2, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, '\u0000');
		assertNBBO("AAPL", 2, 401.01, 402.99, 200, 300, Exchange.USEQ_BATS_Y_EXCHANGE, Exchange.USEQ_BATS_Y_EXCHANGE, ' ');

		this.normalizer.processMessage("TEST", createUtpPacket(createShortQuote("AAPL", 401, 403, 1, 4, ' ', ' ', '3', 'H', 401.01, 402.99, 2, 3, 'Y')), false);
		assertSizes(1, 1, 1);
		assertState("AAPL", 36866, MarketSession.NORMAL, TradingState.TRADING, 0, 0);
		assertBBO("AAPL", 2, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, '\u0000');
		assertNBBO("AAPL", 2, 401.01, 402.99, 200, 300, Exchange.USEQ_BATS_Y_EXCHANGE, Exchange.USEQ_BATS_Y_EXCHANGE, ' ');

		this.normalizer.processMessage("TEST", createUtpPacket(createShortQuote("AAPL", 401, 403, 1, 4, ' ', ' ', '3', 'I', 401.01, 402.99, 2, 3, 'Y')), false);
		assertSizes(1, 1, 1);
		assertState("AAPL", 49154, MarketSession.NORMAL, TradingState.TRADING, 0, 0);
		assertBBO("AAPL", 2, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, '\u0000');
		assertNBBO("AAPL", 2, 401.01, 402.99, 200, 300, Exchange.USEQ_BATS_Y_EXCHANGE, Exchange.USEQ_BATS_Y_EXCHANGE, ' ');
	}

	@Test
	public void testShortQuoteContainedNBBO()
	{
		this.normalizer.processMessage("TEST", createUtpPacket(createShortQuote("AAPL", 401, 403, 1, 4, 'F', ' ', '4', ' ', 401.01, 402.99, 2, 3, 'Y')), false);
		assertSizes(1, 1, 1);
		assertState("AAPL", 2, MarketSession.NORMAL, TradingState.TRADING, 0, 0);
		assertBBO("AAPL", 8194, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, '\u0000');
		assertNBBO("AAPL", 2, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, 'F');

		char[] quoteConditions = { 'I', 'L', 'N', 'U', 'X', 'Z' };
		for (char c : quoteConditions)
		{
			this.normalizer.processMessage("TEST", createUtpPacket(createShortQuote("AAPL", 401, 403, 1, 4, c, ' ', '4', ' ', 401.01, 402.99, 2, 3, 'Y')), false);
			assertSizes(0, 1, 1);
			assertBBO("AAPL", 8194, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, '\u0000');
			assertNBBO("AAPL", 2, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, c);
		}

		this.normalizer.processMessage("TEST", createUtpPacket(createShortQuote("AAPL", 401, 403, 1, 4, ' ', 'B', '4', ' ', 401.01, 402.99, 2, 3, 'Y')), false);
		assertSizes(0, 1, 1);
		assertBBO("AAPL", 32770, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, '\u0000');
		assertNBBO("AAPL", 2, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, ' ');

		this.normalizer.processMessage("TEST", createUtpPacket(createShortQuote("AAPL", 401, 403, 1, 4, ' ', 'A', '4', ' ', 401.01, 402.99, 2, 3, 'Y')), false);
		assertSizes(0, 1, 1);
		assertBBO("AAPL", 16386, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, '\u0000');
		assertNBBO("AAPL", 2, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, ' ');

		this.normalizer.processMessage("TEST", createUtpPacket(createShortQuote("AAPL", 401, 403, 1, 4, ' ', 'C', '4', ' ', 401.01, 402.99, 2, 3, 'Y')), false);
		assertSizes(0, 1, 1);
		assertBBO("AAPL", 32770, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, '\u0000');
		assertNBBO("AAPL", 2, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, ' ');

		this.normalizer.processMessage("TEST", createUtpPacket(createShortQuote("AAPL", 401, 403, 1, 4, ' ', ' ', '4', 'A', 401.01, 402.99, 2, 3, 'Y')), false);
		assertSizes(0, 1, 1);
		assertBBO("AAPL", 2, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, '\u0000');
		assertNBBO("AAPL", 2, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, ' ');

		this.normalizer.processMessage("TEST", createUtpPacket(createShortQuote("AAPL", 401, 403, 1, 4, ' ', ' ', '4', 'B', 401.01, 402.99, 2, 3, 'Y')), false);
		assertSizes(1, 1, 1);
		assertState("AAPL", 4098, MarketSession.NORMAL, TradingState.TRADING, 0, 0);
		assertBBO("AAPL", 2, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, '\u0000');
		assertNBBO("AAPL", 2, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, ' ');

		this.normalizer.processMessage("TEST", createUtpPacket(createShortQuote("AAPL", 401, 403, 1, 4, ' ', ' ', '4', 'C', 401.01, 402.99, 2, 3, 'Y')), false);
		assertSizes(1, 1, 1);
		assertState("AAPL", 8194, MarketSession.NORMAL, TradingState.TRADING, 0, 0);
		assertBBO("AAPL", 2, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, '\u0000');
		assertNBBO("AAPL", 2, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, ' ');

		this.normalizer.processMessage("TEST", createUtpPacket(createShortQuote("AAPL", 401, 403, 1, 4, ' ', ' ', '4', 'D', 401.01, 402.99, 2, 3, 'Y')), false);
		assertSizes(1, 1, 1);
		assertState("AAPL", 12290, MarketSession.NORMAL, TradingState.TRADING, 0, 0);
		assertBBO("AAPL", 2, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, '\u0000');
		assertNBBO("AAPL", 2, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, ' ');

		this.normalizer.processMessage("TEST", createUtpPacket(createShortQuote("AAPL", 401, 403, 1, 4, ' ', ' ', '4', 'E', 401.01, 402.99, 2, 3, 'Y')), false);
		assertSizes(1, 1, 1);
		assertState("AAPL", 16386, MarketSession.NORMAL, TradingState.TRADING, 0, 0);
		assertBBO("AAPL", 2, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, '\u0000');
		assertNBBO("AAPL", 2, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, ' ');

		this.normalizer.processMessage("TEST", createUtpPacket(createShortQuote("AAPL", 401, 403, 1, 4, ' ', ' ', '4', 'F', 401.01, 402.99, 2, 3, 'Y')), false);
		assertSizes(1, 1, 1);
		assertState("AAPL", 32770, MarketSession.NORMAL, TradingState.TRADING, 0, 0);
		assertBBO("AAPL", 2, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, '\u0000');
		assertNBBO("AAPL", 2, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, ' ');

		this.normalizer.processMessage("TEST", createUtpPacket(createShortQuote("AAPL", 401, 403, 1, 4, ' ', ' ', '4', 'G', 401.01, 402.99, 2, 3, 'Y')), false);
		assertSizes(1, 1, 1);
		assertState("AAPL", 24578, MarketSession.NORMAL, TradingState.TRADING, 0, 0);
		assertBBO("AAPL", 2, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, '\u0000');
		assertNBBO("AAPL", 2, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, ' ');

		this.normalizer.processMessage("TEST", createUtpPacket(createShortQuote("AAPL", 401, 403, 1, 4, ' ', ' ', '4', 'H', 401.01, 402.99, 2, 3, 'Y')), false);
		assertSizes(1, 1, 1);
		assertState("AAPL", 36866, MarketSession.NORMAL, TradingState.TRADING, 0, 0);
		assertBBO("AAPL", 2, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, '\u0000');
		assertNBBO("AAPL", 2, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, ' ');

		this.normalizer.processMessage("TEST", createUtpPacket(createShortQuote("AAPL", 401, 403, 1, 4, ' ', ' ', '4', 'I', 401.01, 402.99, 2, 3, 'Y')), false);
		assertSizes(1, 1, 1);
		assertState("AAPL", 49154, MarketSession.NORMAL, TradingState.TRADING, 0, 0);
		assertBBO("AAPL", 2, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, '\u0000');
		assertNBBO("AAPL", 2, 401, 403, 100, 400, Exchange.USEQ_NASDAQ_OMX, Exchange.USEQ_NASDAQ_OMX, ' ');
	}

	private static UtpPacket createUtpPacket(ByteBuffer buffer)
	{
		UtpPacket packet = new UtpPacket(System.nanoTime());
		packet.setBuffer(buffer);
		packet.parseHeader();
		return packet;
	}

	private void assertState(String symbol, int conditionCode, MarketSession marketSession, TradingState tradingState, double lowerBand, double upperBand)
	{
		MarketState state = this.stateListener.getState();
		assertEquals(symbol, state.getSymbol());
		assertEquals(MdServiceType.STATE, state.getServiceType());
		assertEquals(conditionCode, state.getConditionCode());
		assertEquals(marketSession, state.getMarketSession());
		assertEquals(tradingState, state.getTradingState());
		assertEquals(lowerBand, state.getLowerBand(), 0.0000001);
		assertEquals(upperBand, state.getUpperBand(), 0.0000001);
	}

	private void assertSizes(int stateSize, int bboSize, int nbboSize)
	{
		assertEquals(stateSize, this.stateListener.size());
		assertEquals(bboSize, this.bboListener.size());
		assertEquals(nbboSize, this.nbboListener.size());
	}

	private void assertBBO(String symbol, int conditionCode, double bidPrice, double askPrice, int bidSize, int askSize, Exchange bidExchange, Exchange askExchange, char condition)
	{
		Quote quote = this.bboListener.getQuote();
		assertEquals(MdServiceType.BBO, quote.getServiceType());
		assertQuoteFields(quote, symbol, conditionCode, bidPrice, askPrice, bidSize, askSize, bidExchange, askExchange, condition);
	}

	private void assertNBBO(String symbol, int conditionCode, double bidPrice, double askPrice, int bidSize, int askSize, Exchange bidExchange, Exchange askExchange, char condition)
	{
		Quote quote = this.nbboListener.getQuote();
		assertEquals(MdServiceType.NBBO, quote.getServiceType());
		assertQuoteFields(quote, symbol, conditionCode, bidPrice, askPrice, bidSize, askSize, bidExchange, askExchange, condition);
	}

	private static void assertQuoteFields(Quote quote, String symbol, int conditionCode, double bidPrice, double askPrice, int bidSize, int askSize, Exchange bidExchange,
			Exchange askExchange, char condition)
	{
		assertEquals(symbol, quote.getSymbol());
		assertEquals(conditionCode, quote.getConditionCode());
		assertEquals(bidPrice, quote.getBidPrice(), 0.0000001);
		assertEquals(askPrice, quote.getAskPrice(), 0.0000001);
		assertEquals(bidSize, quote.getBidSize());
		assertEquals(askSize, quote.getAskSize());
		assertEquals(bidExchange, quote.getBidExchange());
		assertEquals(askExchange, quote.getAskExchange());
		assertEquals(condition, quote.getCondition());
	}

	private ByteBuffer createLongQuote(String symbol, double bidPrice, double askPrice, long bidSizeLots, long askSizeLots, char quoteCondition, char luldBBOIndicator,
			char rlpIndicator, char nbboAppendageIndicator, char luldNBBOIndicator, double nbboBidPrice, double nbboAskPrice, long nbboBidSizeLots, long nbboAskSizeLots,
			char nbboParticipantId)
	{
		ByteBuffer buffer = ByteBuffer.allocate(300);
		populateHeader(buffer, 'Q', 'F', 'Q');
		ByteBufferUtil.putString(buffer, symbol, 11);
		buffer.position(buffer.position() + 2); // reserved, sipGeenratedUpdated
		ByteBufferUtil.putChar(buffer, quoteCondition);
		ByteBufferUtil.putChar(buffer, luldBBOIndicator);
		ByteBufferUtil.putChar(buffer, rlpIndicator);
		ByteBufferUtil.putChar(buffer, 'B');
		ByteBufferUtil.putLong(buffer, (long) (bidPrice * 100), 10);
		ByteBufferUtil.putLong(buffer, bidSizeLots, 7);
		ByteBufferUtil.putChar(buffer, 'B');
		ByteBufferUtil.putLong(buffer, (long) (askPrice * 100), 10);
		ByteBufferUtil.putLong(buffer, askSizeLots, 7);
		buffer.position(buffer.position() + 3); // currency
		ByteBufferUtil.putChar(buffer, nbboAppendageIndicator);
		ByteBufferUtil.putChar(buffer, luldNBBOIndicator);
		buffer.position(buffer.position() + 1); // finra adf mpid
		populateNbbo(buffer, nbboAppendageIndicator, nbboBidPrice, nbboAskPrice, nbboBidSizeLots, nbboAskSizeLots, nbboParticipantId);
		buffer.flip();
		return buffer;
	}

	private ByteBuffer createShortQuote(String symbol, double bidPrice, double askPrice, long bidSizeLots, long askSizeLots, char quoteCondition, char luldBBOIndicator,
			char nbboAppendageIndicator, char luldNBBOIndicator, double nbboBidPrice, double nbboAskPrice, long nbboBidSizeLots, long nbboAskSizeLots, char nbboParticipantId)
	{
		ByteBuffer buffer = ByteBuffer.allocate(300);
		populateHeader(buffer, 'Q', 'E', 'Q');
		ByteBufferUtil.putString(buffer, symbol, 5);
		buffer.position(buffer.position() + 2); // reserved, sipGeenratedUpdated
		ByteBufferUtil.putChar(buffer, quoteCondition);
		ByteBufferUtil.putChar(buffer, luldBBOIndicator);
		ByteBufferUtil.putChar(buffer, 'B');
		ByteBufferUtil.putLong(buffer, (long) (bidPrice * 100), 6);
		ByteBufferUtil.putLong(buffer, bidSizeLots, 2);
		ByteBufferUtil.putChar(buffer, 'B');
		ByteBufferUtil.putLong(buffer, (long) (askPrice * 100), 6);
		ByteBufferUtil.putLong(buffer, askSizeLots, 2);
		ByteBufferUtil.putChar(buffer, nbboAppendageIndicator);
		ByteBufferUtil.putChar(buffer, luldNBBOIndicator);
		buffer.position(buffer.position() + 1); // finra adf mpid
		populateNbbo(buffer, nbboAppendageIndicator, nbboBidPrice, nbboAskPrice, nbboBidSizeLots, nbboAskSizeLots, nbboParticipantId);
		buffer.flip();
		return buffer;
	}

	private ByteBuffer createAdminMessage(String message)
	{
		ByteBuffer buffer = ByteBuffer.allocate(300);
		populateHeader(buffer, 'A', 'A', 'Q');
		ByteBufferUtil.putString(buffer, message, message.length());
		buffer.flip();
		return buffer;
	}

	private ByteBuffer createCrossSROTradingActionMessage(String symbol, char action)
	{
		ByteBuffer buffer = ByteBuffer.allocate(49);
		populateHeader(buffer, 'A', 'H', 'Q');
		ByteBufferUtil.putString(buffer, symbol, 11);
		ByteBufferUtil.putChar(buffer, action);
		buffer.position(buffer.position() + 13); // action date/time, reason
		buffer.flip();
		return buffer;
	}

	private ByteBuffer createIssueSymbolDirectoryMessage(String symbol, long roundLotSize)
	{
		ByteBuffer buffer = ByteBuffer.allocate(86);
		populateHeader(buffer, 'A', 'B', 'Q');
		ByteBufferUtil.putString(buffer, symbol, 11);
		buffer.position(buffer.position() + 45); // Old issue symbol, Issue Name, Issue Type, Market Category, Authenticity, SSTI
		ByteBufferUtil.putLong(buffer, roundLotSize, 5);
		buffer.position(buffer.position() + 1); // Financial status indicator
		buffer.flip();
		return buffer;
	}

	private ByteBuffer createRegShoShortSalePriceTestRestrictedIndicator(String symbol, char regShoAction)
	{
		ByteBuffer buffer = ByteBuffer.allocate(36);
		populateHeader(buffer, 'A', 'V', 'Q');
		ByteBufferUtil.putString(buffer, symbol, 11);
		ByteBufferUtil.putChar(buffer, regShoAction);
		buffer.flip();
		return buffer;
	}

	private ByteBuffer createLULDPriceBandMessage(String symbol, double lowerBand, double upperBand)
	{
		ByteBuffer buffer = ByteBuffer.allocate(67);
		populateHeader(buffer, 'A', 'P', 'Q');
		ByteBufferUtil.putString(buffer, symbol, 11);
		buffer.position(buffer.position() + 10); // luldBandIndicator, luldEffectiveTime
		ByteBufferUtil.putChar(buffer, 'B');
		ByteBufferUtil.putLong(buffer, (long) (lowerBand * 100), 10);
		ByteBufferUtil.putChar(buffer, 'B');
		ByteBufferUtil.putLong(buffer, (long) (upperBand * 100), 10);
		buffer.flip();
		return buffer;
	}

	private ByteBuffer createMWCBDeclineLevelMessage(double l1Price, double l2Price, double l3Price)
	{
		ByteBuffer buffer = ByteBuffer.allocate(70);
		populateHeader(buffer, 'A', 'C', 'Q');
		ByteBufferUtil.putChar(buffer, 'B');
		ByteBufferUtil.putLong(buffer, (long) (l1Price * 100), 12);
		buffer.position(buffer.position() + 3);
		ByteBufferUtil.putLong(buffer, (long) (l2Price * 100), 12);
		buffer.position(buffer.position() + 3);
		ByteBufferUtil.putLong(buffer, (long) (l3Price * 100), 12);
		buffer.position(buffer.position() + 3);
		buffer.flip();
		return buffer;
	}

	private ByteBuffer createMWCBStatusMessage(char levelIndicator)
	{
		ByteBuffer buffer = ByteBuffer.allocate(28);
		populateHeader(buffer, 'A', 'D', 'Q');
		ByteBufferUtil.putChar(buffer, levelIndicator);
		buffer.position(buffer.position() + 3);
		buffer.flip();
		return buffer;
	}

	private ByteBuffer createMarketSessionOpen()
	{
		ByteBuffer buffer = ByteBuffer.allocate(24);
		populateHeader(buffer, 'C', 'O', 'Q');
		buffer.flip();
		return buffer;
	}

	private ByteBuffer createMarketSessionClose()
	{
		ByteBuffer buffer = ByteBuffer.allocate(24);
		populateHeader(buffer, 'C', 'C', 'Q');
		buffer.flip();
		return buffer;
	}

	private void populateHeader(ByteBuffer buffer, char msgCategory, char msgType, char participantId)
	{
		ByteBufferUtil.putChar(buffer, msgCategory);
		ByteBufferUtil.putChar(buffer, msgType);
		buffer.position(buffer.position() + 3); // session identifier, retrans requester
		ByteBufferUtil.putLong(buffer, this.sequenceNumber++, 8);
		ByteBufferUtil.putChar(buffer, participantId);
		buffer.position(buffer.position() + 9); // timestamp
		buffer.position(buffer.position() + 1); // reserved
	}

	private static void populateNbbo(ByteBuffer buffer, char nbboIndicator, double bidPrice, double askPrice, long bidSizeLots, long askSizeLots, char participantId)
	{
		switch (nbboIndicator)
		{
			case '0': // No National BBO change
			case '1': // No National BBO Can be Calculated
			case '2': // Short Form National BBO Appendage Attached
				buffer.position(buffer.position() + 1); // nbbo quote condition
				ByteBufferUtil.putChar(buffer, participantId);
				ByteBufferUtil.putChar(buffer, 'B');
				ByteBufferUtil.putLong(buffer, (long) (bidPrice * 100), 6);
				ByteBufferUtil.putLong(buffer, bidSizeLots, 2);
				buffer.position(buffer.position() + 1); // reserved
				ByteBufferUtil.putChar(buffer, participantId);
				ByteBufferUtil.putChar(buffer, 'B');
				ByteBufferUtil.putLong(buffer, (long) (askPrice * 100), 6);
				ByteBufferUtil.putLong(buffer, askSizeLots, 2);
				break;
			case '3': // Long Form National BBO Appendage Attached
				buffer.position(buffer.position() + 1); // nbbo quote condition
				ByteBufferUtil.putChar(buffer, participantId);
				ByteBufferUtil.putChar(buffer, 'B');
				ByteBufferUtil.putLong(buffer, (long) (bidPrice * 100), 10);
				ByteBufferUtil.putLong(buffer, bidSizeLots, 7);
				buffer.position(buffer.position() + 1); // reserved
				ByteBufferUtil.putChar(buffer, participantId);
				ByteBufferUtil.putChar(buffer, 'B');
				ByteBufferUtil.putLong(buffer, (long) (askPrice * 100), 10);
				ByteBufferUtil.putLong(buffer, askSizeLots, 7);
				buffer.position(buffer.position() + 3); // currency
				break;
			case '4': // Quote Contains All National BBO Information
				break;
			default:
				break;

		}
	}
	@Test
	public void test_GetLotSize()
	{
		Assert.assertEquals(new Integer(10) , new Integer(this.normalizer.getLotSize("SYED")));
		Assert.assertEquals(100 , this.normalizer.getLotSize("NONE"));
	}
	
}

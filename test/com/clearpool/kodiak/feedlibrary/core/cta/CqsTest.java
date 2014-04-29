package com.clearpool.kodiak.feedlibrary.core.cta;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.clearpool.commonserver.adapter.IMulticastAdapter;
import com.clearpool.kodiak.feedlibrary.callbacks.IMdQuoteListener;
import com.clearpool.kodiak.feedlibrary.callbacks.IMdStateListener;
import com.clearpool.kodiak.feedlibrary.core.MdFeed;
import com.clearpool.kodiak.feedlibrary.core.MdLibrary;
import com.clearpool.kodiak.feedlibrary.core.MdLibraryContext;
import com.clearpool.messageobjects.marketdata.MarketState;
import com.clearpool.messageobjects.marketdata.MdServiceType;
import com.clearpool.messageobjects.marketdata.Quote;

public class CqsTest implements IMdQuoteListener, IMdStateListener
{
	private static final Logger LOGGER = Logger.getLogger(CqsTest.class.getName());

	public CqsTest() throws Exception
	{
		MdLibraryContext context = new MdLibraryContext(false, 1, false, 0, true);
		MdLibrary cqsLibrary = new MdLibrary(context, MdFeed.CQS, new String[] { "1", "13" }, "127.0.0.1", "127.0.0.1", 5000, "C:\\cta");
		cqsLibrary.registerService(MdServiceType.NBBO, this);
		cqsLibrary.registerService(MdServiceType.BBO, this);
		cqsLibrary.registerService(MdServiceType.STATE, this);
		cqsLibrary.initProcessors();
		context.start();
	}

	@Override
	public void quoteReceived(Quote quote, IMulticastAdapter multicastAdapter)
	{
		System.out.println(quote.getServiceType() + " " + quote);
	}

	@Override
	public void stateReceived(MarketState state, IMulticastAdapter multicastAdapter)
	{
		System.out.println(MdServiceType.STATE + " " + state);
	}

	@SuppressWarnings("unused")
	public static void main(String[] args)
	{
		try
		{
			CqsTest server = new CqsTest();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, e.getMessage(), e);
		}
	}
}
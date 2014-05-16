package com.clearpool.kodiak.feedlibrary.core.utp;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.clearpool.kodiak.feedlibrary.callbacks.IMdQuoteListener;
import com.clearpool.kodiak.feedlibrary.callbacks.IMdStateListener;
import com.clearpool.kodiak.feedlibrary.core.MdFeed;
import com.clearpool.kodiak.feedlibrary.core.MdLibrary;
import com.clearpool.kodiak.feedlibrary.core.MdLibraryContext;
import com.clearpool.messageobjects.marketdata.MarketState;
import com.clearpool.messageobjects.marketdata.MdServiceType;
import com.clearpool.messageobjects.marketdata.Quote;

public class UqdfTest implements IMdQuoteListener, IMdStateListener
{
	private static final Logger LOGGER = Logger.getLogger(UqdfTest.class.getName());

	public UqdfTest() throws Exception
	{
		MdLibraryContext context = new MdLibraryContext(false, 1, false, 0, true);
		MdLibrary uqdfLibrary = new MdLibrary(context, MdFeed.UQDF, new String[] { "1" }, "127.0.0.1", "127.0.0.1", 0, "C:\\utp");
		uqdfLibrary.registerService(MdServiceType.NBBO, this);
		uqdfLibrary.registerService(MdServiceType.BBO, this);
		uqdfLibrary.registerService(MdServiceType.STATE, this);
		uqdfLibrary.initProcessors();
		context.start();
	}

	@Override
	public void quoteReceived(Quote quote, int channel)
	{
		System.out.println(quote.getServiceType() + " " + quote);
	}

	@Override
	public void stateReceived(MarketState state, int channel)
	{
		System.out.println(MdServiceType.STATE + " " + state);
	}

	@SuppressWarnings("unused")
	public static void main(String[] args)
	{
		try
		{
			UqdfTest server = new UqdfTest();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, e.getMessage(), e);
		}
	}
}
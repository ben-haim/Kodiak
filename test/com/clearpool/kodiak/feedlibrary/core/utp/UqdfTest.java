package com.clearpool.kodiak.feedlibrary.core.utp;

import java.io.IOException;
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

	public UqdfTest() throws IOException
	{
		MdLibraryContext context = new MdLibraryContext(1, false, 0, true, false);
		MdLibrary uqdfLibrary = new MdLibrary(context, MdFeed.UQDF, new String[] { "1" }, "127.0.0.1", "127.0.0.1", 5000, "C:\\utp");
		uqdfLibrary.registerService(MdServiceType.NBBO, this);
		uqdfLibrary.registerService(MdServiceType.BBO, this);
		uqdfLibrary.registerService(MdServiceType.STATE, this);
		uqdfLibrary.initProcessors();
		context.start();
	}

	@Override
	public void quoteReceived(Quote quote)
	{
		System.out.println(quote.getServiceType() + " " + quote);
	}

	@Override
	public void stateReceived(MarketState state)
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
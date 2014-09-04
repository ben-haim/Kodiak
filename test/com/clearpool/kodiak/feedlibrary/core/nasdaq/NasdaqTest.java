package com.clearpool.kodiak.feedlibrary.core.nasdaq;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.clearpool.kodiak.feedlibrary.callbacks.IMdBookQuoteListener;
import com.clearpool.kodiak.feedlibrary.callbacks.IMdImbalanceListener;
import com.clearpool.kodiak.feedlibrary.core.MdFeed;
import com.clearpool.kodiak.feedlibrary.core.MdLibrary;
import com.clearpool.kodiak.feedlibrary.core.MdLibraryContext;
import com.clearpool.messageobjects.marketdata.BookQuote;
import com.clearpool.messageobjects.marketdata.Imbalance;
import com.clearpool.messageobjects.marketdata.MdServiceType;

public class NasdaqTest implements IMdBookQuoteListener, IMdImbalanceListener
{
	private static final Logger LOGGER = Logger.getLogger(NasdaqTest.class.getName());

	public NasdaqTest() throws Exception
	{
		MdLibraryContext context = new MdLibraryContext(false, 1, false, 0, true);
		MdLibrary nasdaqLibrary = new MdLibrary(context, MdFeed.NASDAQ, new String[] { "1" }, "127.0.0.1", "127.0.0.1", "C:\\nasdaq");
		nasdaqLibrary.registerService(MdServiceType.BOOK_XNAS, this);
		nasdaqLibrary.registerService(MdServiceType.IMBALANCE_XNAS, this);
		nasdaqLibrary.initProcessors();
		context.start();
	}

	@Override
	public void bookQuoteReceived(BookQuote bookQuote, int channel)
	{
		System.out.println(bookQuote);
	}

	@Override
	public void imbalanceReceived(Imbalance imbalance, int channel)
	{
		System.out.println(imbalance);
	}

	@SuppressWarnings("unused")
	public static void main(String[] args)
	{
		try
		{
			NasdaqTest server = new NasdaqTest();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, e.getMessage(), e);
		}
	}
}
package com.clearpool.kodiak.feedlibrary.core.psx;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.clearpool.commonserver.adapter.IMulticastAdapter;
import com.clearpool.kodiak.feedlibrary.callbacks.IMdBookQuoteListener;
import com.clearpool.kodiak.feedlibrary.callbacks.IMdImbalanceListener;
import com.clearpool.kodiak.feedlibrary.core.MdFeed;
import com.clearpool.kodiak.feedlibrary.core.MdLibrary;
import com.clearpool.kodiak.feedlibrary.core.MdLibraryContext;
import com.clearpool.messageobjects.marketdata.BookQuote;
import com.clearpool.messageobjects.marketdata.Imbalance;
import com.clearpool.messageobjects.marketdata.MdServiceType;

public class PsxTest implements IMdBookQuoteListener, IMdImbalanceListener
{
	private static final Logger LOGGER = Logger.getLogger(PsxTest.class.getName());

	public PsxTest() throws Exception
	{
		MdLibraryContext context = new MdLibraryContext(false, 1, false, 0, true);
		MdLibrary nasdaqLibrary = new MdLibrary(context, MdFeed.PSX, new String[] { "1" }, "127.0.0.1", "127.0.0.1", 5000, "C:\\nasdaq");
		nasdaqLibrary.registerService(MdServiceType.BOOK_XPSX, this);
		nasdaqLibrary.registerService(MdServiceType.IMBALANCE_XPSX, this);
		nasdaqLibrary.initProcessors();
		context.start();
	}

	@Override
	public void bookQuoteReceived(BookQuote bookQuote, IMulticastAdapter multicastAdapter)
	{
		System.out.println(bookQuote);
	}

	@Override
	public void imbalanceReceived(Imbalance imbalance, IMulticastAdapter multicastAdapter)
	{
		System.out.println(imbalance);
	}

	@SuppressWarnings("unused")
	public static void main(String[] args)
	{
		try
		{
			PsxTest server = new PsxTest();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, e.getMessage(), e);
		}
	}
}

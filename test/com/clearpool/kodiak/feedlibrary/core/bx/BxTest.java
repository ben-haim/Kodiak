package com.clearpool.kodiak.feedlibrary.core.bx;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.clearpool.kodiak.feedlibrary.callbacks.IMdBookQuoteListner;
import com.clearpool.kodiak.feedlibrary.callbacks.IMdImbalanceListener;
import com.clearpool.kodiak.feedlibrary.core.MdFeed;
import com.clearpool.kodiak.feedlibrary.core.MdLibrary;
import com.clearpool.kodiak.feedlibrary.core.MdLibraryContext;
import com.clearpool.messageobjects.marketdata.BookQuote;
import com.clearpool.messageobjects.marketdata.Imbalance;
import com.clearpool.messageobjects.marketdata.MdServiceType;

public class BxTest implements IMdBookQuoteListner, IMdImbalanceListener
{
	private static final Logger LOGGER = Logger.getLogger(BxTest.class.getName());

	public BxTest() throws IOException
	{
		MdLibraryContext context = new MdLibraryContext(1, false, 0, true, false);
		MdLibrary nasdaqLibrary = new MdLibrary(context, MdFeed.BX, new String[] { "1" }, "127.0.0.1", "127.0.0.1", 5000, "C:\\nasdaq");
		nasdaqLibrary.registerService(MdServiceType.BOOK_XBOS, this);
		nasdaqLibrary.registerService(MdServiceType.IMBALANCE_XBOS, this);
		nasdaqLibrary.initProcessors();
		context.start();
	}

	@Override
	public void bookQuoteReceived(BookQuote bookQuote)
	{
		System.out.println(bookQuote);
	}

	@Override
	public void imbalanceReceived(Imbalance imbalance)
	{
		System.out.println(imbalance);
	}

	@SuppressWarnings("unused")
	public static void main(String[] args)
	{
		try
		{
			BxTest server = new BxTest();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, e.getMessage(), e);
		}
	}
}

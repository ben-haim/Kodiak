package com.clearpool.kodiak.feedlibrary.core.bx;

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

public class BxTest implements IMdBookQuoteListener, IMdImbalanceListener
{
	private static final Logger LOGGER = Logger.getLogger(BxTest.class.getName());

	public BxTest() throws Exception
	{
		MdLibraryContext context = new MdLibraryContext(false, 1, false, 0, true);
		MdLibrary nasdaqLibrary = new MdLibrary(context, MdFeed.BX, new String[] { "1" }, "127.0.0.1", "127.0.0.1", 0, "C:\\nasdaq");
		nasdaqLibrary.registerService(MdServiceType.BOOK_XBOS, this);
		nasdaqLibrary.registerService(MdServiceType.IMBALANCE_XBOS, this);
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
			BxTest server = new BxTest();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, e.getMessage(), e);
		}
	}
}

package com.clearpool.kodiak.feedlibrary.core.opra;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.clearpool.commonserver.adapter.IMulticastAdapter;
import com.clearpool.kodiak.feedlibrary.callbacks.IMdQuoteListener;
import com.clearpool.kodiak.feedlibrary.callbacks.IMdSaleListener;
import com.clearpool.kodiak.feedlibrary.core.MdFeed;
import com.clearpool.kodiak.feedlibrary.core.MdLibrary;
import com.clearpool.kodiak.feedlibrary.core.MdLibraryContext;
import com.clearpool.messageobjects.marketdata.MdServiceType;
import com.clearpool.messageobjects.marketdata.Quote;
import com.clearpool.messageobjects.marketdata.Sale;

public class OpraTest implements IMdQuoteListener, IMdSaleListener
{
	private static final Logger LOGGER = Logger.getLogger(OpraTest.class.getName());

	public OpraTest() throws Exception
	{
		MdLibraryContext context = new MdLibraryContext(false, 1, false, 0, true);
		MdLibrary library = new MdLibrary(context, MdFeed.OPRA, new String[] { "3" }, "127.0.0.1", "127.0.0.1", 5000, "C:\\opra");
		library.registerService(MdServiceType.NBBO, this);
		library.registerService(MdServiceType.BBO, this);
		library.registerService(MdServiceType.SALE, this);
		library.initProcessors();
		context.start();
	}

	@SuppressWarnings("unused")
	public static void main(String[] args)
	{
		try
		{
			OpraTest server = new OpraTest();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, e.getMessage(), e);
		}
	}

	@Override
	public void saleReceived(Sale sale, IMulticastAdapter multicastAdapter)
	{
		System.out.println(MdServiceType.SALE + " " + sale);
	}

	@Override
	public void quoteReceived(Quote quote, IMulticastAdapter multicastAdapter)
	{
		System.out.println(quote.getServiceType() + " " + quote);
	}
}
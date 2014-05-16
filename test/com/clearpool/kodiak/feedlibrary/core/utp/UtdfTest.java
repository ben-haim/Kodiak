package com.clearpool.kodiak.feedlibrary.core.utp;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.clearpool.kodiak.feedlibrary.callbacks.IMdSaleListener;
import com.clearpool.kodiak.feedlibrary.core.MdFeed;
import com.clearpool.kodiak.feedlibrary.core.MdLibrary;
import com.clearpool.kodiak.feedlibrary.core.MdLibraryContext;
import com.clearpool.messageobjects.marketdata.MdServiceType;
import com.clearpool.messageobjects.marketdata.Sale;

public class UtdfTest implements IMdSaleListener
{
	private static final Logger LOGGER = Logger.getLogger(UqdfTest.class.getName());

	public UtdfTest() throws Exception
	{
		MdLibraryContext context = new MdLibraryContext(false, 1, false, 0, true);
		MdLibrary utdfLibrary = new MdLibrary(context, MdFeed.UTDF, new String[] { "1" }, "127.0.0.1", "127.0.0.1", 0, "C:\\utp");
		utdfLibrary.registerService(MdServiceType.SALE, this);
		utdfLibrary.initProcessors();
		context.start();
	}

	@Override
	public void saleReceived(Sale sale, int channel)
	{
		System.out.println(MdServiceType.SALE + " " + sale);
	}

	@SuppressWarnings("unused")
	public static void main(String[] args)
	{
		try
		{
			UtdfTest server = new UtdfTest();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, e.getMessage(), e);
		}
	}
}

package com.clearpool.kodiak.feedlibrary.mbeans;

import com.clearpool.kodiak.feedlibrary.caches.BboQuoteCache;
import com.clearpool.kodiak.feedlibrary.caches.BookQuoteCache;
import com.clearpool.kodiak.feedlibrary.caches.IMdServiceCache;
import com.clearpool.kodiak.feedlibrary.caches.ImbalanceCache;
import com.clearpool.kodiak.feedlibrary.caches.NbboQuoteCache;
import com.clearpool.kodiak.feedlibrary.caches.SaleCache;
import com.clearpool.kodiak.feedlibrary.caches.StateCache;

public class MdServiceCacheMgmtFactory
{

	public static IMdServiceCacheMgmt getCacheMgmt(IMdServiceCache cache)
	{
		if (cache instanceof NbboQuoteCache)
		{
			NbboQuoteCache nbboCache = (NbboQuoteCache) cache;
			return new NbboQuoteCacheMgmt(nbboCache);
		}
		else if (cache instanceof BboQuoteCache)
		{
			BboQuoteCache bboCache = (BboQuoteCache) cache;
			return new BboQuoteCacheMgmt(bboCache);
		}
		else if (cache instanceof SaleCache)
		{
			SaleCache saleCache = (SaleCache) cache;
			return new SaleCacheMgmt(saleCache);
		}
		else if (cache instanceof StateCache)
		{
			StateCache stateCache = (StateCache) cache;
			return new StateCacheMgmt(stateCache);
		}
		else if (cache instanceof BookQuoteCache)
		{
			BookQuoteCache bookQuoteCache = (BookQuoteCache) cache;
			return new BookQuoteCacheMgmt(bookQuoteCache);
		}
		else if (cache instanceof ImbalanceCache)
		{
			ImbalanceCache imbalanceCache = (ImbalanceCache) cache;
			return new ImbalanceCacheMgmt(imbalanceCache);
		}
		return null;
	}
}
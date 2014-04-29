package com.clearpool.kodiak.feedlibrary.callbacks;

import com.clearpool.commonserver.adapter.IMulticastAdapter;
import com.clearpool.messageobjects.marketdata.Quote;

public interface IMdQuoteListener extends IMdLibraryCallback
{
	void quoteReceived(Quote quote, IMulticastAdapter multicastAdapter);
}

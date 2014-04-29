package com.clearpool.kodiak.feedlibrary.callbacks;

import com.clearpool.commonserver.adapter.IMulticastAdapter;
import com.clearpool.messageobjects.marketdata.BookQuote;

public interface IMdBookQuoteListener extends IMdLibraryCallback
{
	void bookQuoteReceived(BookQuote bookQuote, IMulticastAdapter multicastAdapter);
}

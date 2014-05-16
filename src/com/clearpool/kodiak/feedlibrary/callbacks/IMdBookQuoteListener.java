package com.clearpool.kodiak.feedlibrary.callbacks;

import com.clearpool.messageobjects.marketdata.BookQuote;

public interface IMdBookQuoteListener extends IMdLibraryCallback
{
	void bookQuoteReceived(BookQuote bookQuote, int channel);
}

package com.clearpool.kodiak.feedlibrary.callbacks;

import com.clearpool.messageobjects.marketdata.BookQuote;

public interface IMdBookQuoteListner extends IMdLibraryCallback
{
	void bookQuoteReceived(BookQuote bookQuote);
}

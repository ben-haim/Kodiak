package com.clearpool.kodiak.feedlibrary.callbacks;

import com.clearpool.messageobjects.marketdata.Sale;

public interface IMdSaleListener extends IMdLibraryCallback
{
	void saleReceived(Sale sale, int channel);
}

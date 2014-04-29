package com.clearpool.kodiak.feedlibrary.callbacks;

import com.clearpool.commonserver.adapter.IMulticastAdapter;
import com.clearpool.messageobjects.marketdata.Sale;

public interface IMdSaleListener extends IMdLibraryCallback
{
	void saleReceived(Sale sale, IMulticastAdapter multicastAdapter);
}

package com.clearpool.kodiak.feedlibrary.callbacks;

import com.clearpool.commonserver.adapter.IMulticastAdapter;
import com.clearpool.messageobjects.marketdata.Imbalance;

public interface IMdImbalanceListener extends IMdLibraryCallback
{
	void imbalanceReceived(Imbalance imbalance, IMulticastAdapter multicastAdapter);
}

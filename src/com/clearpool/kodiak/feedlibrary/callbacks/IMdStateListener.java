package com.clearpool.kodiak.feedlibrary.callbacks;

import com.clearpool.commonserver.adapter.IMulticastAdapter;
import com.clearpool.messageobjects.marketdata.MarketState;

public interface IMdStateListener extends IMdLibraryCallback
{
	public void stateReceived(MarketState state, IMulticastAdapter multicastAdapter);
}

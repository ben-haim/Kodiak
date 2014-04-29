package com.clearpool.kodiak.feedlibrary.core;

import java.util.LinkedList;
import java.util.Queue;

import com.clearpool.commonserver.adapter.IMulticastAdapter;
import com.clearpool.kodiak.feedlibrary.callbacks.IMdStateListener;
import com.clearpool.messageobjects.marketdata.MarketState;

public class TestMDStateListener implements IMdStateListener
{

	private final Queue<MarketState> queue = new LinkedList<MarketState>();

	@Override
	public void stateReceived(MarketState state, IMulticastAdapter multicastAdapter)
	{
		this.queue.add(state);
	}

	public MarketState getState()
	{
		return this.queue.remove();
	}

	public int size()
	{
		return this.queue.size();
	}

}

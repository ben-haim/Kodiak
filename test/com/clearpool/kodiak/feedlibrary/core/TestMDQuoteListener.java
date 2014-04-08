package com.clearpool.kodiak.feedlibrary.core;

import java.util.LinkedList;
import java.util.Queue;

import com.clearpool.kodiak.feedlibrary.callbacks.IMdQuoteListener;
import com.clearpool.messageobjects.marketdata.Quote;

public class TestMDQuoteListener implements IMdQuoteListener
{

	private final Queue<Quote> queue = new LinkedList<Quote>();

	@Override
	public void quoteReceived(Quote quote)
	{
		this.queue.add(quote);
	}

	public Quote getQuote()
	{
		return this.queue.remove();
	}

	public int size()
	{
		return this.queue.size();
	}
}

package com.clearpool.kodiak.feedlibrary.core;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

public class MdLibraryContext
{
	private final int selectorCount;
	private final boolean readFromSocket;
	private final int recvBufferSize;
	private final boolean useQueuedSelector;
	private final Thread[] selectorThreads;
	private Timer timer;

	private boolean publishing;

	public MdLibraryContext(int selectorCount, boolean readFromSocket, int recvBufferSize, boolean publishing, boolean useQueuedSelector) throws IOException
	{
		this(selectorCount, readFromSocket, recvBufferSize, publishing, useQueuedSelector, new Timer("MDLibraryContext Timer", true));
	}

	public MdLibraryContext(int selectorCount, boolean readFromSocket, int recvBufferSize, boolean publishing, boolean useQueuedSelector, Timer timer) throws IOException
	{
		this.selectorCount = selectorCount;
		this.readFromSocket = readFromSocket;
		this.recvBufferSize = recvBufferSize;
		this.useQueuedSelector = useQueuedSelector;

		this.setPublishing(publishing);
		if (this.readFromSocket)
		{
			this.selectorThreads = new MdSocketSelector[this.selectorCount];
			for (int i = 0; i < this.selectorThreads.length; i++)
			{
				this.selectorThreads[i] = this.useQueuedSelector ? new MdQueuedSocketSelector(String.valueOf(i), this.recvBufferSize) : new MdSocketSelector(String.valueOf(i),
						this.recvBufferSize);
			}
		}
		else this.selectorThreads = new Thread[] { new MdFileSelector() };
		this.timer = timer;
	}

	public MdSocketSelector getSocketSelectorForLine(int line)
	{
		return (this.readFromSocket) ? (MdSocketSelector) (this.selectorThreads[line % this.selectorThreads.length]) : null;
	}

	public MdFileSelector getFileSelectorForLine(int line)
	{
		return (this.readFromSocket) ? null : (MdFileSelector) (this.selectorThreads[line % this.selectorThreads.length]);
	}

	public void start()
	{
		// Start Selectors
		for (int i = 0; i < this.selectorThreads.length; i++)
		{
			this.selectorThreads[i].start();
		}
	}

	public void stop()
	{
		// Stop Selectors
		for (int i = 0; i < this.selectorThreads.length; i++)
		{
			this.selectorThreads[i].interrupt();
		}
	}

	public int getSelectorThreadCount()
	{
		return this.selectorThreads.length;
	}

	public boolean readFromSocket()
	{
		return this.readFromSocket;
	}

	public boolean isPublishing()
	{
		return this.publishing;
	}

	public void setPublishing(boolean publishing)
	{
		this.publishing = publishing;
	}

	public int getRecvBufferSize()
	{
		return this.recvBufferSize;
	}

	public void schedule(TimerTask task, long delay, long period)
	{
		if (this.timer == null) this.timer = new Timer("MDLibraryContext Timer", true);
		this.timer.schedule(task, delay, period);
	}
}
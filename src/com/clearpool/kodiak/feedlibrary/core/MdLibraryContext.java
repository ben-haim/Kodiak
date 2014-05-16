package com.clearpool.kodiak.feedlibrary.core;

import java.util.Timer;
import java.util.TimerTask;

public class MdLibraryContext
{
	private final boolean readFromSocket;
	private final int recvBufferSize;
	private final Thread[] selectorThreads;
	private Timer timer;

	private boolean publishing;

	public MdLibraryContext(boolean readFromSocket, int selectorCount, boolean useQueuedSelector, int recvBufferSize, boolean publishing) throws Exception
	{
		this(readFromSocket, selectorCount, useQueuedSelector, recvBufferSize, publishing, new Timer("MDLibraryContext Timer", true));
	}

	public MdLibraryContext(boolean readFromSocket, int selectorCount, boolean useQueuedSelector, int recvBufferSize, boolean publishing, Timer timer) throws Exception
	{
		this.readFromSocket = readFromSocket;
		this.recvBufferSize = recvBufferSize;
		if (this.readFromSocket)
		{
			this.selectorThreads = new MdSocketSelector[selectorCount];
			for (int i = 0; i < this.selectorThreads.length; i++)
			{
				this.selectorThreads[i] = useQueuedSelector ? new MdQueuedSocketSelector(String.valueOf(i), this.recvBufferSize) : new MdSocketSelector(String.valueOf(i),
						this.recvBufferSize);
			}
		}
		else this.selectorThreads = new Thread[] { new MdFileSelector() };
		this.publishing = publishing;
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
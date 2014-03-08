package com.clearpool.kodiak.feedlibrary.core;

import java.io.IOException;

public class MdLibraryContext
{
	private final int selectorCount;
	private final boolean readFromSocket;
	private final int recvBufferSize;
	private final boolean useQueuedSelector;

	private boolean publishing;
	private Thread[] selectorThreads;

	public MdLibraryContext(int selectorCount, boolean readFromSocket, int recvBufferSize, boolean publishing, boolean useQueuedSelector) throws IOException
	{
		this.selectorCount = selectorCount;
		this.readFromSocket = readFromSocket;
		this.recvBufferSize = recvBufferSize;
		this.useQueuedSelector = useQueuedSelector;

		this.setPublishing(publishing);
		init();
	}

	private void init() throws IOException
	{
		if (this.readFromSocket)
		{
			this.selectorThreads = new MdSocketSelector[this.selectorCount];
			for (int i = 0; i < this.selectorThreads.length; i++)
			{
				MdSocketSelector mdSelector = null;
				if (this.useQueuedSelector)
				{
					mdSelector = new MdQueuedSocketSelector(String.valueOf(i), this.recvBufferSize);
				}
				else
				{
					mdSelector = new MdSocketSelector(String.valueOf(i), this.recvBufferSize);
				}
				this.selectorThreads[i] = mdSelector;
			}
		}
		else
		{
			MdFileSelector fileSelector = new MdFileSelector();
			this.selectorThreads = new Thread[] { fileSelector };
		}
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
}
package com.clearpool.kodiak.feedlibrary.core;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MdQueuedSocketSelector extends MdSocketSelector
{
	static final Logger LOGGER = Logger.getLogger(MdQueuedSocketSelector.class.getName());

	private final BlockingQueue<MdSelection> selectionQueue;

	public MdQueuedSocketSelector(String name, int recvBufferSize) throws IOException
	{
		super(name, recvBufferSize);
		this.selectionQueue = new LinkedBlockingQueue<MdSelection>();
		SelectionDeliveryThread selectionDeliveryThread = new SelectionDeliveryThread(this.selectionQueue);
		selectionDeliveryThread.start();
	}

	@Override
	protected void handleMulticastSelection(SelectionKey selectedKey)
	{
		if (selectedKey.isReadable())
		{
			try
			{
				ByteBuffer buffer = ByteBuffer.allocate(1500);
				((DatagramChannel) selectedKey.channel()).receive(buffer);
				buffer.flip();
				ISelectable attachment = (ISelectable) selectedKey.attachment();
				MdSelection selection = new MdSelection(buffer, attachment, selectedKey);
				this.selectionQueue.add(selection);
			}
			catch (Exception e)
			{
				LOGGER.log(Level.SEVERE, e.getMessage(), e);
			}
		}
	}

	private class MdSelection
	{
		final ByteBuffer buffer;
		final ISelectable attachment;
		final SelectionKey selectedKey;

		public MdSelection(ByteBuffer buffer, ISelectable attachment, SelectionKey selectedKey)
		{
			this.buffer = buffer;
			this.attachment = attachment;
			this.selectedKey = selectedKey;
		}
	}

	private class SelectionDeliveryThread extends Thread
	{
		private BlockingQueue<MdSelection> queuedSelections;

		public SelectionDeliveryThread(BlockingQueue<MdSelection> queuedSelections)
		{
			this.queuedSelections = queuedSelections;
		}

		@Override
		public void run()
		{
			LinkedList<MdSelection> tempSelections = new LinkedList<>();
			while (!Thread.currentThread().isInterrupted())
			{
				try
				{
					MdSelection queuedSelection = this.queuedSelections.take();
					queuedSelection.attachment.onSelection(queuedSelection.selectedKey, queuedSelection.buffer);

					this.queuedSelections.drainTo(tempSelections);
					if (tempSelections.size() > 0)
					{
						Iterator<MdSelection> tempSelectioIterator = tempSelections.iterator();
						while (tempSelectioIterator.hasNext())
						{
							MdSelection tempSelection = tempSelectioIterator.next();
							tempSelection.attachment.onSelection(tempSelection.selectedKey, tempSelection.buffer);
							tempSelectioIterator.remove();
						}
					}

				}
				catch (Exception e)
				{
					LOGGER.log(Level.SEVERE, e.getMessage(), e);
				}
			}
			LOGGER.warning("ByteBufferThread has been interrupted.");
		}
	}
}

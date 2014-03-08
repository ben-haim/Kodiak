package com.clearpool.kodiak.feedlibrary.core;

import java.io.FileNotFoundException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.clearpool.kodiak.feedlibrary.utils.MdLogReader;


public class MdFileSelector extends Thread
{
	private static final Logger LOGGER = Logger.getLogger(MdFileSelector.class.getName());
	
	private final Map<MdLogReader, MDSelectableFileChannel> streamToChannel;
	
	public MdFileSelector()
	{
		this.streamToChannel = new HashMap<MdLogReader, MdFileSelector.MDSelectableFileChannel>();
	}
	
	@Override
	public void run()
	{
		while(this.streamToChannel.size() > 0 && !Thread.currentThread().isInterrupted())
		{
			try
			{
				Iterator<Map.Entry<MdLogReader, MDSelectableFileChannel>> entryIterator = this.streamToChannel.entrySet().iterator();
				while(entryIterator.hasNext())
				{
					Map.Entry<MdLogReader, MDSelectableFileChannel> entry = entryIterator.next();
					MdLogReader reader = entry.getKey();
					MDSelectableFileChannel channel = entry.getValue();
					if(reader.hasNext())
					{
						byte[] next = reader.next();
						ByteBuffer buffer = ByteBuffer.wrap(next);
						buffer.position(buffer.position() + 8); //skip timestamp
						channel.getAttachment().onSelection(channel.getSelectionKey(), buffer);
					}
					else
					{
						entryIterator.remove();
					}
				}
			}
			catch(Exception e)
			{
				LOGGER.log(Level.SEVERE, e.getMessage(), e);
			}
		}
	}
	
	public void registerFile(String file, Object selectionKey, ISelectable attachment) throws FileNotFoundException
	{
		MdLogReader reader = new MdLogReader(file);
		MDSelectableFileChannel channel = new MDSelectableFileChannel(selectionKey, attachment);
		this.streamToChannel.put(reader, channel);
	}
	
	private class MDSelectableFileChannel
	{
		private final Object selectionKey;
		private final ISelectable attachment;
		
		MDSelectableFileChannel(Object selectionKey, ISelectable attachment)
		{
			this.selectionKey = selectionKey;
			this.attachment = attachment;
		}

		public Object getSelectionKey()
		{
			return this.selectionKey;
		}

		public ISelectable getAttachment()
		{
			return this.attachment;
		}
	}
}
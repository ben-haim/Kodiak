package com.clearpool.kodiak.feedlibrary.core;

import com.clearpool.kodiak.feedlibrary.caches.IMdServiceCache;


public interface IMdNormalizer
{
	public void processMessage(String processorName, MdFeedPacket packet, boolean shouldIgnore);
	public IMdServiceCache[] getMdServiceCaches();
}
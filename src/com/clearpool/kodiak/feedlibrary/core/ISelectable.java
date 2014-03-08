package com.clearpool.kodiak.feedlibrary.core;

import java.nio.ByteBuffer;

public interface ISelectable
{
	public void onSelection(Object key, ByteBuffer buffer);
}

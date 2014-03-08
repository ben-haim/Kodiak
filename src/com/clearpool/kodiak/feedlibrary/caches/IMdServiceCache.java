package com.clearpool.kodiak.feedlibrary.caches;

import java.util.Collection;

import com.clearpool.messageobjects.marketdata.MdServiceType;

public interface IMdServiceCache
{
	public MdServiceType getMdServiceType();
	public String getRange();
	public String[] getAllSymbols();
	public Object getData(String symbol);
	public Collection<String> publishAllData();
}

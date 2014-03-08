package com.clearpool.kodiak.feedlibrary.mbeans;

import java.util.Arrays;

import com.clearpool.kodiak.feedlibrary.core.MdLibrary;


public class MdLibraryMgmt implements MdLibraryMgmtMBean
{
	private final MdLibrary library;
	
	public MdLibraryMgmt(MdLibrary library)
	{
		this.library = library;
	}
	
	@Override
	public String getMdFeed()
	{
		return this.library.getMdFeed().toString();
	}

	@Override
	public String getLines()
	{
		return Arrays.toString(this.library.getLines());
	}

	@Override
	public int getSelectorThreadCount()
	{
		return this.library.getSelectorThreadCount();
	}

	@Override
	public String getInterfaceA()
	{
		return this.library.getInterfaceA();
	}

	@Override
	public String getInterfaceB()
	{
		return this.library.getInterfaceB();
	}
}
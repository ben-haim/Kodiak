package com.clearpool.kodiak.feedlibrary.core;

public enum MdFeed
{
	OPRA(false),
	CQS(true),
	CTS(true),
	UQDF(true),
	UTDF(true),
	NASDAQ(false),
	BX(false),
	PSX(false),
	ARCA(false);

	private final boolean containsMultiplePacketsInBlock;

	private MdFeed(boolean containsMultiplePacketsInBlock)
	{
		this.containsMultiplePacketsInBlock = containsMultiplePacketsInBlock;
	}

	public boolean containsMultiplePacketsInBlock()
	{
		return this.containsMultiplePacketsInBlock;
	}
}
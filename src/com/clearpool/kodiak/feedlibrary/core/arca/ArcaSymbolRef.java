package com.clearpool.kodiak.feedlibrary.core.arca;

public class ArcaSymbolRef
{
	private String symbol;
	private double priceDivider;

	public String getSymbol()
	{
		return this.symbol;
	}

	public void setSymbol(String symbol)
	{
		this.symbol = symbol;
	}

	public double getPriceDivider()
	{
		return this.priceDivider;
	}

	public void setPriceDivider(double priceDivider)
	{
		this.priceDivider = priceDivider;
	}
}

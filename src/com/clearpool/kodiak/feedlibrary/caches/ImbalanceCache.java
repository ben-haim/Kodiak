package com.clearpool.kodiak.feedlibrary.caches;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.clearpool.common.symbology.ISymbolConverter;
import com.clearpool.kodiak.feedlibrary.callbacks.IMdImbalanceListener;
import com.clearpool.kodiak.feedlibrary.core.MdFeed;
import com.clearpool.kodiak.feedlibrary.utils.symbolconverters.SymbolConverterFactory;
import com.clearpool.messageobjects.marketdata.AuctionType;
import com.clearpool.messageobjects.marketdata.Exchange;
import com.clearpool.messageobjects.marketdata.Imbalance;
import com.clearpool.messageobjects.marketdata.MdEntity;
import com.clearpool.messageobjects.marketdata.MdServiceType;
import com.clearpool.messageobjects.order.Side;

public class ImbalanceCache implements IMdServiceCache
{
	private final IMdImbalanceListener imbalanceListener;
	private final MdFeed feedType;
	private final MdServiceType mdServiceType;
	private final String range;
	private final Map<String, Imbalance> imbalances;

	public ImbalanceCache(IMdImbalanceListener imbalanceListener, MdFeed feedType, MdServiceType mdServiceType, String range)
	{
		this.imbalanceListener = imbalanceListener;
		this.feedType = feedType;
		this.mdServiceType = mdServiceType;
		this.range = range;
		this.imbalances = new HashMap<String, Imbalance>();
	}

	public void updateImbalance(String symbol, long pairedVolume, long imbalanceVolume, Side imbalanceSide, long marketOrderOnlyImbalanceVolume, double referencePrice,
			double clearingPrice, double auctionOnlyClearingPrice, double ssrOnlyFillingPrice, Exchange exchange, AuctionType auctionType, long timestamp)
	{
		boolean isFirstImbalance = false;
		Imbalance imbalance = this.imbalances.get(symbol);
		if (imbalance == null)
		{
			imbalance = createImbalance(symbol);
			this.imbalances.put(symbol, imbalance);
			isFirstImbalance = true;
		}

		if (isFirstImbalance)
		{
			imbalance.setInitialImbalance(imbalanceVolume);
			imbalance.setInitialTimestamp(timestamp);
		}
		imbalance.setPairedVolume(pairedVolume);
		imbalance.setImbalanceVolume(imbalanceVolume);
		imbalance.setImbalanceSide(imbalanceSide);
		imbalance.setMarketOrderOnlyImbalanceVolume(marketOrderOnlyImbalanceVolume);
		imbalance.setReferencePrice(referencePrice);
		imbalance.setClearingPrice(clearingPrice);
		imbalance.setAuctionOnlyClearingPrice(auctionOnlyClearingPrice);
		imbalance.setSsrOnlyFillingPrice(ssrOnlyFillingPrice);
		imbalance.setExchange(exchange);
		imbalance.setAuctionType(auctionType);
		imbalance.setTimestamp(timestamp);
		imbalance.setConditionCode(0);

		sendImbalance(imbalance);
	}

	private void sendImbalance(Imbalance imbalance)
	{
		imbalance.setConditionCode(MdEntity.setCondition(imbalance.getConditionCode(), MdEntity.CONDITION_FRESH));
		imbalance.setMdTimestamp(System.currentTimeMillis());
		imbalance.setSymbolSequenceNumber(imbalance.getSymbolSequenceNumber() + 1);

		if (this.imbalanceListener != null)
		{
			imbalance = imbalance.clone();
			this.imbalanceListener.imbalanceReceived(imbalance);
		}
	}

	private Imbalance createImbalance(String symbol)
	{
		Imbalance imbalance = new Imbalance();
		ISymbolConverter converter = SymbolConverterFactory.getConverterInstance(this.feedType);
		if (converter != null) imbalance.setSymbol(converter.convert(symbol));
		else imbalance.setSymbol(symbol);
		imbalance.setServiceType(this.mdServiceType);
		return imbalance;
	}

	@Override
	public MdServiceType getMdServiceType()
	{
		return this.mdServiceType;
	}

	@Override
	public String[] getAllSymbols()
	{
		return this.imbalances.keySet().toArray(new String[0]);
	}

	@Override
	public Imbalance getData(String symbol)
	{
		return this.imbalances.get(symbol);
	}

	@Override
	public String getRange()
	{
		return this.range;
	}

	@Override
	public Collection<String> publishAllData()
	{
		for (Imbalance imbalance : this.imbalances.values())
		{
			if (this.imbalanceListener != null)
			{
				imbalance = imbalance.clone();
				this.imbalanceListener.imbalanceReceived(imbalance);
			}
		}
		return this.imbalances.keySet();
	}
}

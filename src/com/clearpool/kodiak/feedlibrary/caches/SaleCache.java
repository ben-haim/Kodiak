package com.clearpool.kodiak.feedlibrary.caches;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import com.clearpool.common.symbology.ISymbolConverter;
import com.clearpool.kodiak.feedlibrary.callbacks.IMdSaleListener;
import com.clearpool.kodiak.feedlibrary.core.MdFeed;
import com.clearpool.kodiak.feedlibrary.utils.symbolconverters.SymbolConverterFactory;
import com.clearpool.messageobjects.marketdata.Exchange;
import com.clearpool.messageobjects.marketdata.MdEntity;
import com.clearpool.messageobjects.marketdata.MdServiceType;
import com.clearpool.messageobjects.marketdata.Sale;
import com.clearpool.messageobjects.marketdata.Tick;

public class SaleCache implements IMdServiceCache
{
	private static final Logger LOGGER = Logger.getLogger(SaleCache.class.getName());

	private final IMdSaleListener saleListener;
	private final MdFeed feedType;
	private final String range;
	private final boolean isFirstOpen;
	private final Map<String, Sale> sales;

	public SaleCache(IMdSaleListener saleListener, MdFeed feedType, String range, boolean isFirstOpen)
	{
		this.saleListener = saleListener;
		this.feedType = feedType;
		this.range = range;
		this.isFirstOpen = isFirstOpen;
		this.sales = new HashMap<>();
	}

	@Override
	public MdServiceType getMdServiceType()
	{
		return MdServiceType.SALE;
	}

	public void updateWithSaleCondition(String symbol, double price, int size, Exchange exchange, long timestamp, int saleCondition, String condition)
	{
		boolean isFirstSale = false;
		Sale sale = this.sales.get(symbol);
		if (sale == null)
		{
			sale = createSale(symbol);
			this.sales.put(symbol, sale);
			isFirstSale = true;
		}

		int directionalSize = (MdEntity.isConditionSet(Sale.CONDITION_CODE_CANCEL, saleCondition)) ? -size : size;
		if (MdEntity.isConditionSet(saleCondition, Sale.CONDITION_CODE_LAST))
		{
			double tickChange = (price - sale.getPrice());
			sale.setTick((tickChange == 0) ? Tick.EVEN : ((tickChange > 0) ? Tick.UP : Tick.DOWN));
			sale.setPrice(price);
			sale.setSize(size);

			if (this.isFirstOpen && isFirstSale)
			{
				saleCondition = MdEntity.setCondition(saleCondition, Sale.CONDITION_CODE_OPEN);
			}
		}
		if (MdEntity.isConditionSet(saleCondition, Sale.CONDITION_CODE_VOLUME))
		{
			updateVolume(sale, directionalSize);
		}
		if (MdEntity.isConditionSet(saleCondition, Sale.CONDITION_CODE_VWAP))
		{
			updateVwapAndVwapVolume(sale, price, directionalSize);
		}
		if (MdEntity.isConditionSet(saleCondition, Sale.CONDITION_CODE_OPEN))
		{
			sale.setOpenPrice(price);
		}
		if (MdEntity.isConditionSet(saleCondition, Sale.CONDITION_CODE_LOW))
		{
			if (price < sale.getLowPrice() || sale.getLowPrice() == 0) sale.setLowPrice(price);
			else saleCondition = MdEntity.unsetCondition(saleCondition, Sale.CONDITION_CODE_LOW);
		}
		if (MdEntity.isConditionSet(saleCondition, Sale.CONDITION_CODE_HIGH))
		{
			if (price > sale.getHighPrice()) sale.setHighPrice(price);
			else saleCondition = MdEntity.unsetCondition(saleCondition, Sale.CONDITION_CODE_HIGH);
		}
		if (MdEntity.isConditionSet(saleCondition, Sale.CONDITION_CODE_LATEST_CLOSE))
		{
			sale.setLatestClosePrice(price);
		}

		sale.setExchange(exchange);
		sale.setTimestamp(timestamp);
		sale.setNonDisplayablePrice(price);
		sale.setNonDisplayableSize(size);
		sale.setConditionCode(MdEntity.setCondition(saleCondition, MdEntity.CONDITION_FRESH));
		sale.setCondition(condition);
		sendSale(sale);
	}

	public void updateOpenInterest(String symbol, long openInterest, long timestamp, String condition)
	{
		Sale sale = this.sales.get(symbol);
		if (sale == null)
		{
			sale = createSale(symbol);
			this.sales.put(symbol, sale);
		}

		sale.setOpenInterest(openInterest);
		sale.setTimestamp(timestamp);
		sale.setConditionCode(MdEntity.setCondition(0, Sale.CONDITION_CODE_OPEN_INTEREST, MdEntity.CONDITION_FRESH));
		sale.setCondition(condition);
		sendSale(sale);
	}

	public void updateLatestClosePrice(String symbol, Exchange exchange, double closePrice, long timestamp, String condition)
	{
		Sale sale = this.sales.get(symbol);
		if (sale == null)
		{
			sale = createSale(symbol);
			this.sales.put(symbol, sale);
		}

		sale.setExchange(exchange);
		sale.setLatestClosePrice(closePrice);
		sale.setTimestamp(timestamp);
		sale.setConditionCode(MdEntity.setCondition(0, Sale.CONDITION_CODE_LATEST_CLOSE, MdEntity.CONDITION_FRESH));
		sale.setCondition(condition);
		sendSale(sale);
	}

	public void setLatestClosePrice(String symbol, Exchange exchange, double closePrice, long timestamp, String condition)
	{
		Sale sale = this.sales.get(symbol);
		if (sale == null)
		{
			sale = createSale(symbol);
			this.sales.put(symbol, sale);
		}

		sale.setExchange(exchange);
		sale.setLatestClosePrice(closePrice);
		sale.setTimestamp(timestamp);
		sale.setConditionCode(0);
		sale.setCondition(condition);
		sendSale(sale);
	}

	public void updateEndofDay(String symbol, long volume, long openInterest, double openPrice, double highPrice, double lowPrice, double lastPrice, long timestamp)
	{
		boolean isFirstSale = false;
		Sale sale = this.sales.get(symbol);
		if (sale == null)
		{
			sale = createSale(symbol);
			this.sales.put(symbol, sale);
			isFirstSale = true;
		}

		int saleCondition = 0;
		if (sale.getVolume() != volume)
		{
			sale.setVolume(volume);
			saleCondition = MdEntity.setCondition(saleCondition, Sale.CONDITION_CODE_VOLUME);
		}
		if (sale.getOpenInterest() != openInterest)
		{
			sale.setOpenInterest(openInterest);
			saleCondition = MdEntity.setCondition(saleCondition, Sale.CONDITION_CODE_OPEN_INTEREST);
		}
		if (sale.getOpenPrice() != openPrice)
		{
			sale.setOpenPrice(openPrice);
			saleCondition = MdEntity.setCondition(saleCondition, Sale.CONDITION_CODE_OPEN);
		}
		if (sale.getHighPrice() != highPrice)
		{
			sale.setHighPrice(highPrice);
			saleCondition = MdEntity.setCondition(saleCondition, Sale.CONDITION_CODE_HIGH);
		}
		if (sale.getLowPrice() != lowPrice)
		{
			sale.setLowPrice(lowPrice);
			saleCondition = MdEntity.setCondition(saleCondition, Sale.CONDITION_CODE_LOW);
		}
		if (sale.getPrice() != lastPrice)
		{
			sale.setPrice(lastPrice);
			saleCondition = MdEntity.setCondition(saleCondition, Sale.CONDITION_CODE_LAST);
		}

		if (saleCondition > 0)
		{
			saleCondition = MdEntity.setCondition(saleCondition, Sale.CONDITION_CODE_CORRECTION);
			sale.setTimestamp(timestamp);
			sale.setConditionCode(saleCondition);
			sale.setCondition("EOD");
			sendSale(sale);
			if (isFirstSale) LOGGER.warning("Received EOD Summary for symbol=" + symbol + " but sale does not exist in cache");
		}
	}

	public void updateEndofDay(String symbol, double closePrice, double lowPrice, double highPrice, long volume, long timestamp, Exchange exchange)
	{
		boolean isFirstSale = false;
		Sale sale = this.sales.get(symbol);
		if (sale == null)
		{
			sale = createSale(symbol);
			this.sales.put(symbol, sale);
			isFirstSale = true;
		}

		int saleCondition = 0;
		if (sale.getLatestClosePrice() != closePrice)
		{
			sale.setLatestClosePrice(closePrice);
			sale.setExchange(exchange);
			saleCondition = MdEntity.setCondition(saleCondition, Sale.CONDITION_CODE_LATEST_CLOSE);
		}
		if (sale.getLowPrice() != lowPrice)
		{
			sale.setLowPrice(lowPrice);
			saleCondition = MdEntity.setCondition(saleCondition, Sale.CONDITION_CODE_LOW);
		}
		if (sale.getHighPrice() != highPrice)
		{
			sale.setHighPrice(highPrice);
			saleCondition = MdEntity.setCondition(saleCondition, Sale.CONDITION_CODE_HIGH);
		}
		if (sale.getVolume() != volume)
		{
			sale.setVolume(volume);
			saleCondition = MdEntity.setCondition(saleCondition, Sale.CONDITION_CODE_VOLUME);
		}

		if (saleCondition > 0)
		{
			saleCondition = MdEntity.setCondition(saleCondition, Sale.CONDITION_CODE_CORRECTION);
			sale.setTimestamp(timestamp);
			sale.setConditionCode(saleCondition);
			sale.setCondition("EOD");
			sendSale(sale);
			if (isFirstSale) LOGGER.warning("Received EOD Summary for symbol=" + symbol + " but sale does not exist in cache");
		}
	}

	public void cancelWithStats(String symbol, double originalPrice, int originalSize, int originalConditionCode, long timestamp, Exchange exchange, double lastPrice,
			double highPrice, double lowPrice, double openPrice, long volume)
	{
		boolean isFirstSale = false;
		Sale sale = this.sales.get(symbol);
		if (sale == null)
		{
			sale = createSale(symbol);
			this.sales.put(symbol, sale);
			isFirstSale = true;
		}

		if (!isFirstSale)
		{
			if (MdEntity.isConditionSet(originalConditionCode, Sale.CONDITION_CODE_VOLUME))
			{
				updateVolume(sale, -originalSize);
			}
			if (MdEntity.isConditionSet(originalConditionCode, Sale.CONDITION_CODE_VWAP))
			{
				updateVwapAndVwapVolume(sale, originalPrice, -originalSize);
			}
		}

		int saleCondition = MdEntity.setCondition(originalConditionCode, Sale.CONDITION_CODE_CANCEL);
		if (lastPrice != -1 && sale.getPrice() != lastPrice)
		{
			sale.setPrice(lastPrice);
			sale.setSize(0);
			saleCondition = MdEntity.setCondition(saleCondition, Sale.CONDITION_CODE_LAST);
		}
		if (highPrice != -1 && sale.getHighPrice() != highPrice)
		{
			sale.setHighPrice(highPrice);
			saleCondition = MdEntity.setCondition(saleCondition, Sale.CONDITION_CODE_HIGH);
		}
		if (lowPrice != -1 && sale.getLowPrice() != lowPrice)
		{
			sale.setLowPrice(lowPrice);
			saleCondition = MdEntity.setCondition(saleCondition, Sale.CONDITION_CODE_LOW);
		}
		if (openPrice != -1 && sale.getOpenPrice() != openPrice)
		{
			sale.setOpenPrice(openPrice);
			saleCondition = MdEntity.setCondition(saleCondition, Sale.CONDITION_CODE_OPEN);
		}
		if (volume != -1 && sale.getVolume() != volume)
		{
			sale.setVolume(volume);
			saleCondition = MdEntity.setCondition(saleCondition, Sale.CONDITION_CODE_VOLUME);
		}

		sale.setExchange(exchange);
		sale.setTimestamp(timestamp);
		sale.setNonDisplayablePrice(originalPrice);
		sale.setNonDisplayableSize(originalSize);
		sale.setConditionCode(MdEntity.setCondition(saleCondition, MdEntity.CONDITION_FRESH));
		sale.setCondition("CXL");
		sendSale(sale);
	}

	public void correctWithStats(String symbol, double originalPrice, int originalSize, int originalConditionCode, double correctedPrice, int correctedSize,
			int correctedConditionCode, long timestamp, Exchange exchange, double lastPrice, double highPrice, double lowPrice, double openPrice, long volume)
	{
		boolean isFirstSale = false;
		Sale sale = this.sales.get(symbol);
		if (sale == null)
		{
			sale = createSale(symbol);
			this.sales.put(symbol, sale);
			isFirstSale = true;
		}

		if (!isFirstSale)
		{
			if (MdEntity.isConditionSet(originalConditionCode, Sale.CONDITION_CODE_VOLUME))
			{
				updateVolume(sale, -originalSize);
			}
			if (MdEntity.isConditionSet(originalConditionCode, Sale.CONDITION_CODE_VWAP))
			{
				updateVwapAndVwapVolume(sale, originalPrice, -originalSize);
			}

			if (MdEntity.isConditionSet(correctedConditionCode, Sale.CONDITION_CODE_VOLUME))
			{
				updateVolume(sale, correctedSize);
			}
			if (MdEntity.isConditionSet(correctedConditionCode, Sale.CONDITION_CODE_VWAP))
			{
				updateVwapAndVwapVolume(sale, correctedPrice, correctedSize);
			}
		}

		int saleCondition = MdEntity.setCondition(originalConditionCode, Sale.CONDITION_CODE_CORRECTION);
		if (lastPrice != -1 && sale.getPrice() != lastPrice)
		{
			sale.setPrice(lastPrice);
			sale.setSize(0);
			saleCondition = MdEntity.setCondition(saleCondition, Sale.CONDITION_CODE_LAST);
		}
		if (highPrice != -1 && sale.getHighPrice() != highPrice)
		{
			sale.setHighPrice(highPrice);
			saleCondition = MdEntity.setCondition(saleCondition, Sale.CONDITION_CODE_HIGH);
		}
		if (lowPrice != -1 && sale.getLowPrice() != lowPrice)
		{
			sale.setLowPrice(lowPrice);
			saleCondition = MdEntity.setCondition(saleCondition, Sale.CONDITION_CODE_LOW);
		}
		if (openPrice != -1 && sale.getOpenPrice() != openPrice)
		{
			sale.setOpenPrice(openPrice);
			saleCondition = MdEntity.setCondition(saleCondition, Sale.CONDITION_CODE_OPEN);
		}
		if (volume != -1 && sale.getVolume() != volume)
		{
			sale.setVolume(volume);
			saleCondition = MdEntity.setCondition(saleCondition, Sale.CONDITION_CODE_VOLUME);
		}

		sale.setExchange(exchange);
		sale.setTimestamp(timestamp);
		sale.setNonDisplayablePrice(originalPrice);
		sale.setNonDisplayableSize(originalSize);
		sale.setConditionCode(MdEntity.setCondition(saleCondition, MdEntity.CONDITION_FRESH));
		sale.setCondition("COR");
		sendSale(sale);
	}

	private void sendSale(Sale sale)
	{
		sale.setMdTimestamp(System.currentTimeMillis());
		sale.setSymbolSequenceNumber(sale.getSymbolSequenceNumber() + 1);

		if (this.saleListener != null)
		{
			sale = sale.clone();
			this.saleListener.saleReceived(sale);
		}
	}

	private Sale createSale(String symbol)
	{
		Sale sale = new Sale();
		ISymbolConverter converter = SymbolConverterFactory.getConverterInstance(this.feedType);
		if (converter != null) sale.setSymbol(converter.convert(symbol));
		else sale.setSymbol(symbol);
		sale.setServiceType(MdServiceType.SALE);
		return sale;
	}

	private static void updateVolume(Sale sale, int directionalSize)
	{
		long newVolume = sale.getVolume() + directionalSize;
		sale.setVolume(newVolume <= 0 ? 0 : newVolume);
	}

	private static void updateVwapAndVwapVolume(Sale sale, double newPrice, int newSize)
	{
		double sum = sale.getVwap() * sale.getVwapVolume() + newPrice * newSize;
		long totalSize = sale.getVwapVolume() + newSize;
		if (sum <= 0 || totalSize <= 0)
		{
			sale.setVwap(0);
			sale.setVwapVolume(0);
		}
		else
		{
			sale.setVwap(sum / totalSize);
			sale.setVwapVolume(totalSize);
		}
	}

	public MdEntity getCachedData(String symbol)
	{
		return this.sales.get(symbol);
	}

	@Override
	public String[] getAllSymbols()
	{
		return this.sales.keySet().toArray(new String[0]);
	}

	@Override
	public Sale getData(String symbol)
	{
		return this.sales.get(symbol);
	}

	@Override
	public String getRange()
	{
		return this.range;
	}

	@Override
	public Collection<String> publishAllData()
	{
		for (Sale sale : this.sales.values())
		{
			if (this.saleListener != null)
			{
				sale = sale.clone();
				this.saleListener.saleReceived(sale);
			}
		}
		return this.sales.keySet();
	}
}

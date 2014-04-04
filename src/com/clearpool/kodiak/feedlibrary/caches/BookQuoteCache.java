package com.clearpool.kodiak.feedlibrary.caches;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Logger;

import com.clearpool.common.datastractures.Pair;
import com.clearpool.common.marketdata.ReverseDoubleComparator;
import com.clearpool.common.symbology.ISymbolConverter;
import com.clearpool.kodiak.feedlibrary.callbacks.IMdBookQuoteListener;
import com.clearpool.kodiak.feedlibrary.core.MdFeed;
import com.clearpool.kodiak.feedlibrary.utils.symbolconverters.SymbolConverterFactory;
import com.clearpool.messageobjects.marketdata.BookQuote;
import com.clearpool.messageobjects.marketdata.MdEntity;
import com.clearpool.messageobjects.marketdata.MdServiceType;
import com.clearpool.messageobjects.order.Side;

public class BookQuoteCache implements IMdServiceCache
{
	protected static final Logger LOGGER = Logger.getLogger(BookQuoteCache.class.getName());
	protected static final int NUMBER_OF_LEVELS_TO_SEND = 5;

	private final IMdBookQuoteListener bookListener;
	private final MdFeed feedType;
	private final MdServiceType mdServiceType;
	private final String range;
	private final Map<String, BookOrder> orderIdToBookOrder;
	private final Map<String, Book> symbolToBook;

	public BookQuoteCache(IMdBookQuoteListener bookListener, MdFeed feedType, MdServiceType mdServiceType, String range)
	{
		this.bookListener = bookListener;
		this.feedType = feedType;
		this.mdServiceType = mdServiceType;
		this.range = range;
		this.orderIdToBookOrder = new HashMap<String, BookOrder>();
		this.symbolToBook = new HashMap<String, Book>();
	}

	public void addOrder(String symbol, String orderId, Side side, int size, double price, String displayName, long timestamp)
	{
		BookOrder order = new BookOrder(symbol, orderId, side, size, price, displayName, timestamp);
		BookOrder oldOrder = this.orderIdToBookOrder.put(orderId, order);
		if (oldOrder != null)
		{
			LOGGER.warning("Overwrote an order that already exists. OrderId=" + orderId);
		}

		Book book = getOrCreateBook(symbol);
		book.addOrder(symbol, side, price, size, timestamp, displayName, this.mdServiceType);
	}

	public boolean cancelOrder(String orderId, int size, long timestamp)
	{
		BookOrder order = this.orderIdToBookOrder.get(orderId);
		boolean isOrderComplete = false;
		if (order == null)
		{
			LOGGER.warning("Order does not exist. OrderId=" + orderId);
		}
		else
		{
			int origSize = order.getSize();
			order.setSize(origSize - size);
			order.setTimestamp(timestamp);
			if (order.getSize() <= 0)
			{
				this.orderIdToBookOrder.remove(orderId);
				isOrderComplete = true;
			}

			size = (size > origSize) ? origSize : size;
			Book book = getOrCreateBook(order.getSymbol());
			book.cancelOrder(order, size, timestamp, isOrderComplete);
		}
		return isOrderComplete;
	}

	public void cancelOrder(String orderId, long timestamp)
	{
		BookOrder order = this.orderIdToBookOrder.remove(orderId);
		if (order == null)
		{
			LOGGER.warning("Order does not exist. OrderId=" + orderId);
		}
		else
		{
			Book book = getOrCreateBook(order.getSymbol());
			book.cancelOrder(order, order.getSize(), timestamp, true);
		}
	}

	public void replaceOrder(String origOrderId, String newOrderId, int size, double price, long timestamp)
	{
		BookOrder order = this.orderIdToBookOrder.remove(origOrderId);
		if (order == null)
		{
			LOGGER.warning("Order does not exist. OrderId=" + origOrderId);
		}
		else
		{
			Book book = getOrCreateBook(order.getSymbol());
			book.replaceOrder(order, price, size, timestamp, this.mdServiceType);

			order.setOrderId(newOrderId);
			order.setSize(size);
			order.setPrice(price);
			order.setTimestamp(timestamp);
			this.orderIdToBookOrder.put(newOrderId, order);
		}
	}

	private Book getOrCreateBook(String symbol)
	{
		Book book = this.symbolToBook.get(symbol);
		if (book == null)
		{
			book = new Book();
			this.symbolToBook.put(symbol, book);
		}
		return book;
	}

	BookQuote createBookQuote(String symbol)
	{
		BookQuote bookQuote = new BookQuote();
		bookQuote.setServiceType(this.mdServiceType);
		ISymbolConverter converter = SymbolConverterFactory.getConverterInstance(this.feedType);
		if (converter != null) bookQuote.setSymbol(converter.convert(symbol));
		else bookQuote.setSymbol(symbol);
		return bookQuote;
	}

	protected void sendBookQuote(BookQuote bookQuote)
	{
		if (this.bookListener != null)
		{
			BookQuote clonedBookQuote = bookQuote.clone();
			this.bookListener.bookQuoteReceived(clonedBookQuote);
		}
	}

	BookOrder getCachedOrder(String orderId)
	{
		return this.orderIdToBookOrder.get(orderId);
	}

	List<BookQuote> getAggregatedOrder(String symbol, Side side)
	{
		Book book = this.symbolToBook.get(symbol);
		if (book == null) return null;
		return book.getEntryList(side);
	}

	Book getSymbolToBook(String symbol)
	{
		return this.symbolToBook.get(symbol);
	}

	@Override
	public MdServiceType getMdServiceType()
	{
		return this.mdServiceType;
	}

	@Override
	public String[] getAllSymbols()
	{
		return this.symbolToBook.keySet().toArray(new String[0]);
	}

	@Override
	public Pair<List<BookQuote>, List<BookQuote>> getData(String symbol)
	{
		Book book = this.symbolToBook.get(symbol);
		List<BookQuote> bids = book == null ? new ArrayList<BookQuote>() : book.getEntryList(Side.BUY);
		List<BookQuote> asks = book == null ? new ArrayList<BookQuote>() : book.getEntryList(Side.SELL);
		return new Pair<List<BookQuote>, List<BookQuote>>(bids, asks);
	}

	public class BookOrder
	{
		private String symbol;
		private String orderId;
		private Side side;
		private int size;
		private double price;
		private String displayName;
		private long timestamp;

		public BookOrder(String symbol, String orderId, Side side, int size, double price, String displayName, long timestamp)
		{
			this.symbol = symbol;
			this.orderId = orderId;
			this.side = side;
			this.size = size;
			this.price = price;
			this.displayName = displayName;
			this.timestamp = timestamp;
		}

		public String getSymbol()
		{
			return this.symbol;
		}

		public String getOrderId()
		{
			return this.orderId;
		}

		public void setOrderId(String orderId)
		{
			this.orderId = orderId;
		}

		public Side getSide()
		{
			return this.side;
		}

		public int getSize()
		{
			return this.size;
		}

		public void setSize(int size)
		{
			this.size = size;
		}

		public double getPrice()
		{
			return this.price;
		}

		public void setPrice(double price)
		{
			this.price = price;
		}

		public String getDisplayName()
		{
			return this.displayName;
		}

		public long getTimestamp()
		{
			return this.timestamp;
		}

		public void setTimestamp(long timestamp)
		{
			this.timestamp = timestamp;
		}

		@Override
		public String toString()
		{
			StringBuilder buf = new StringBuilder();
			buf.append("OrderId:").append(this.orderId).append(" Symbol:").append(this.symbol).append(" Price:").append(this.price).append(" Side:").append(this.side)
					.append(" Size:").append(this.size);
			return buf.toString();
		}
	}

	public class Book
	{
		private final TreeMap<Double, BookQuote> bidAggregates;
		private final TreeMap<Double, BookQuote> askAggregates;

		private int symbolSequenceNumber;
		private double bidLastTopLevel;
		private double askLastTopLevel;

		public Book()
		{
			this.bidAggregates = new TreeMap<Double, BookQuote>(new ReverseDoubleComparator());
			this.askAggregates = new TreeMap<Double, BookQuote>();

			this.symbolSequenceNumber = 0;
			this.bidLastTopLevel = 0;
			this.askLastTopLevel = 0;
		}

		public void addOrder(String symbol, Side side, double price, int size, long timestamp, String displayName, MdServiceType serviceType)
		{
			TreeMap<Double, BookQuote> aggregatedOrders = (Side.BUY == side) ? this.bidAggregates : this.askAggregates;
			Double doublePrice = Double.valueOf(price);
			BookQuote bookQuote = aggregatedOrders.get(doublePrice);
			double priceLevelToRemove = 0;
			boolean isTopN = false;
			if (bookQuote == null)
			{
				bookQuote = createBookQuote(symbol);
				bookQuote.setSide(side);
				bookQuote.setPrice(price);
				bookQuote.setSize(size);
				bookQuote.setServiceType(serviceType);
				bookQuote.setDisplayName(displayName);
				bookQuote.setNumberOfOrders(1);
				aggregatedOrders.put(doublePrice, bookQuote);

				isTopN = this.isTopN(bookQuote);
				if (isTopN)
				{
					Double toBeRemoved = this.updateTopNForAdd(bookQuote);
					if (toBeRemoved != null)
					{
						priceLevelToRemove = toBeRemoved.doubleValue();
					}
				}
			}
			else
			{
				isTopN = this.isTopN(bookQuote);
				int oldSize = bookQuote.getSize();
				bookQuote.setSize(oldSize + size);
				bookQuote.setNumberOfOrders(bookQuote.getNumberOfOrders() + 1);
			}

			bookQuote.setTimestamp(timestamp);
			bookQuote.setPriceLevelToRemove(priceLevelToRemove);
			if (isTopN)
			{
				this.sendBookQuote(bookQuote);
			}
		}

		private boolean isTopN(BookQuote bookQuote)
		{
			if (Side.BUY == bookQuote.getSide())
			{
				return bookQuote.getPrice() >= this.bidLastTopLevel || this.bidAggregates.size() <= BookQuoteCache.NUMBER_OF_LEVELS_TO_SEND;
			}
			return bookQuote.getPrice() <= this.askLastTopLevel || this.askAggregates.size() <= BookQuoteCache.NUMBER_OF_LEVELS_TO_SEND;
		}

		private Double updateTopNForAdd(BookQuote bookQuote)
		{
			if (Side.BUY == bookQuote.getSide())
			{
				if (this.bidAggregates.size() > BookQuoteCache.NUMBER_OF_LEVELS_TO_SEND)
				{
					Double toRemove = Double.valueOf(this.bidLastTopLevel);
					Double newNthLevel = this.bidAggregates.lowerKey(toRemove);
					this.bidLastTopLevel = newNthLevel.doubleValue();
					return toRemove;
				}
				this.bidLastTopLevel = this.bidAggregates.lastKey().doubleValue();
			}
			else
			{
				if (this.askAggregates.size() > BookQuoteCache.NUMBER_OF_LEVELS_TO_SEND)
				{
					Double toRemove = Double.valueOf(this.askLastTopLevel);
					Double newNthLevel = this.askAggregates.lowerKey(toRemove);
					this.askLastTopLevel = newNthLevel.doubleValue();
					return toRemove;
				}
				this.askLastTopLevel = this.askAggregates.lastKey().doubleValue();
			}
			return null;
		}

		private BookQuote updateTopNForCancel(BookQuote bookQuote)
		{
			if (Side.BUY == bookQuote.getSide())
			{
				if (this.bidAggregates.size() == 0)
				{
					this.bidLastTopLevel = 0;
				}
				else if (this.bidAggregates.size() > BookQuoteCache.NUMBER_OF_LEVELS_TO_SEND)
				{
					Double newNthLevel = this.bidAggregates.higherKey(Double.valueOf(this.bidLastTopLevel));
					this.bidLastTopLevel = newNthLevel.doubleValue();
					return this.bidAggregates.get(newNthLevel);
				}
				else
				{
					this.bidLastTopLevel = this.bidAggregates.lastKey().doubleValue();
				}
			}
			else
			{
				if (this.askAggregates.size() == 0)
				{
					this.askLastTopLevel = 0;
				}
				else if (this.askAggregates.size() > BookQuoteCache.NUMBER_OF_LEVELS_TO_SEND)
				{
					Double newNthLevel = this.askAggregates.higherKey(Double.valueOf(this.askLastTopLevel));
					this.askLastTopLevel = newNthLevel.doubleValue();
					return this.askAggregates.get(newNthLevel);
				}
				else
				{
					this.askLastTopLevel = this.askAggregates.lastKey().doubleValue();
				}
			}
			return null;
		}

		private void sendBookQuote(BookQuote bookQuote)
		{
			bookQuote.setMdTimestamp(System.currentTimeMillis());
			bookQuote.setSymbolSequenceNumber(++this.symbolSequenceNumber);
			bookQuote.setConditionCode(MdEntity.setCondition(bookQuote.getConditionCode(), MdEntity.CONDITION_FRESH));
			BookQuoteCache.this.sendBookQuote(bookQuote);
		}

		public void cancelOrder(BookOrder bookOrder, int size, long timestamp, boolean isOrderComplete)
		{
			TreeMap<Double, BookQuote> aggregatedOrders = (Side.BUY == bookOrder.getSide()) ? this.bidAggregates : this.askAggregates;
			Double price = Double.valueOf(bookOrder.getPrice());
			BookQuote bookQuote = aggregatedOrders.get(price);
			BookQuote newNthLevel = null;
			boolean isTopN = false;
			if (bookQuote == null)
			{
				BookQuoteCache.LOGGER.warning("No aggregated price for symbol:" + bookOrder.getSymbol() + " price:" + bookOrder.getPrice());
			}
			else
			{
				int oldSize = bookQuote.getSize();
				int newSize = oldSize - size;
				bookQuote.setSize(newSize);
				bookQuote.setTimestamp(timestamp);
				if (isOrderComplete)
				{
					bookQuote.setNumberOfOrders(bookQuote.getNumberOfOrders() - 1);
				}

				if (newSize <= 0)
				{
					aggregatedOrders.remove(price);
					isTopN = this.isTopN(bookQuote);
					if (isTopN)
					{
						newNthLevel = this.updateTopNForCancel(bookQuote);
					}
				}
				else
				{
					isTopN = this.isTopN(bookQuote);
				}

				if (isTopN)
				{
					bookQuote.setPriceLevelToRemove(0);
					this.sendBookQuote(bookQuote);
					if (newNthLevel != null)
					{
						newNthLevel.setPriceLevelToRemove(0);
						this.sendBookQuote(newNthLevel);
					}
				}
			}
		}

		public void replaceOrder(BookOrder origOrder, double newPrice, int newSize, long timestamp, MdServiceType serviceType)
		{
			if (origOrder.getPrice() != newPrice)// different price replace
			{
				this.cancelOrder(origOrder, origOrder.getSize(), timestamp, true);
				this.addOrder(origOrder.getSymbol(), origOrder.getSide(), newPrice, newSize, timestamp, origOrder.getDisplayName(), serviceType);
			}
			else if (origOrder.getSize() != newSize)// same price, different size replace
			{
				TreeMap<Double, BookQuote> aggregatedOrders = (Side.BUY == origOrder.getSide()) ? this.bidAggregates : this.askAggregates;
				Double doublePrice = Double.valueOf(newPrice);
				BookQuote bookQuote = aggregatedOrders.get(doublePrice);
				if (bookQuote == null)
				{
					BookQuoteCache.LOGGER.warning("No aggregated price for symbol:" + origOrder.getSymbol() + " price:" + newPrice);
				}
				else
				{
					int oldSize = bookQuote.getSize();
					bookQuote.setSize(oldSize + newSize - origOrder.getSize());
					bookQuote.setTimestamp(timestamp);
					bookQuote.setPriceLevelToRemove(0);

					if (this.isTopN(bookQuote))
					{
						this.sendBookQuote(bookQuote);
					}
				}
			}
		}

		public List<BookQuote> getEntryList(Side side)
		{
			List<BookQuote> bookQuotes = new ArrayList<BookQuote>();
			Set<Entry<Double, BookQuote>> aggregatedOrders = (side == Side.BUY) ? this.bidAggregates.entrySet() : this.askAggregates.entrySet();
			Iterator<Entry<Double, BookQuote>> aggreagedOrdersIterator = aggregatedOrders.iterator();
			while (aggreagedOrdersIterator.hasNext())
			{
				bookQuotes.add(aggreagedOrdersIterator.next().getValue());
			}
			return bookQuotes;
		}

		public double getBidLastTopLevel()
		{
			return this.bidLastTopLevel;
		}

		public double getAskLastTopLevel()
		{
			return this.askLastTopLevel;
		}
	}

	@Override
	public String getRange()
	{
		return this.range;
	}

	@Override
	public Collection<String> publishAllData()
	{
		// If this is implemented, will need to think about how previous values are cleared on the client's side.
		// Potentially a clear message should be sent before publishing Should also consider how to synchronize this call with
		// live updates without having to go through the overhead of sync'ing on every live update.
		// Once this is figured out the same should be implemented for the other caches.
		return null;
	}
}

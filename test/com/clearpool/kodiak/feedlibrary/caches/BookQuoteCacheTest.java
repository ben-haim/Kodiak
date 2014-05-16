package com.clearpool.kodiak.feedlibrary.caches;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.clearpool.kodiak.feedlibrary.caches.BookQuoteCache.BookOrder;
import com.clearpool.messageobjects.marketdata.BookQuote;
import com.clearpool.messageobjects.marketdata.Exchange;
import com.clearpool.messageobjects.marketdata.MdServiceType;
import com.clearpool.messageobjects.order.Side;

@SuppressWarnings("static-method")
public class BookQuoteCacheTest
{
	private static final double PRICE_DOUBLE_PRECISION = 0.000000001;

	@Test
	public void testAddOrders() throws Exception
	{
		BookQuoteCache cache = new BookQuoteCache(null, null, MdServiceType.BOOK_XNAS, "A-Z", 0);

		// Add orders
		cache.addOrder("A", "001", Side.BUY, 100, 10.0, Exchange.USEQ_TEST.getMicCode(), 1000);
		cache.addOrder("A", "002", Side.BUY, 200, 10.0, Exchange.USEQ_TEST.getMicCode(), 1001);
		cache.addOrder("A", "003", Side.BUY, 400, 10.1, Exchange.USEQ_TEST.getMicCode(), 1002);
		cache.addOrder("A", "004", Side.SELL, 400, 10.1, Exchange.USEQ_TEST.getMicCode(), 1003);
		cache.addOrder("A", "005", Side.SELL, 400, 10.1, Exchange.USEQ_TEST.getMicCode(), 1004);
		cache.addOrder("A", "006", Side.SELL, 500, 10.5, Exchange.USEQ_TEST.getMicCode(), 1005);

		// Confirm all orders were added
		Assert.assertNotNull(cache.getCachedOrder("001"));
		Assert.assertNotNull(cache.getCachedOrder("002"));
		Assert.assertNotNull(cache.getCachedOrder("003"));
		Assert.assertNotNull(cache.getCachedOrder("004"));
		Assert.assertNotNull(cache.getCachedOrder("005"));
		Assert.assertNotNull(cache.getCachedOrder("006"));

		// Confirm aggregate
		List<BookQuote> aggregatedBuys = cache.getAggregatedOrder("A", Side.BUY);
		Assert.assertEquals(2, aggregatedBuys.size());

		BookQuote bookQuote1 = aggregatedBuys.get(0);
		Assert.assertEquals(10.1, bookQuote1.getPrice(), BookQuoteCacheTest.PRICE_DOUBLE_PRECISION);
		Assert.assertEquals(400, bookQuote1.getSize());
		Assert.assertEquals(3, bookQuote1.getSymbolSequenceNumber());
		Assert.assertEquals(1, bookQuote1.getNumberOfOrders());

		BookQuote bookQuote2 = aggregatedBuys.get(1);
		Assert.assertEquals(10.0, bookQuote2.getPrice(), BookQuoteCacheTest.PRICE_DOUBLE_PRECISION);
		Assert.assertEquals(300, bookQuote2.getSize());
		Assert.assertEquals(2, bookQuote2.getSymbolSequenceNumber());
		Assert.assertEquals(2, bookQuote2.getNumberOfOrders());

		List<BookQuote> aggregatedSells = cache.getAggregatedOrder("A", Side.SELL);
		Assert.assertEquals(2, aggregatedSells.size());

		BookQuote bookQuote3 = aggregatedSells.get(0);
		Assert.assertEquals(10.1, bookQuote3.getPrice(), BookQuoteCacheTest.PRICE_DOUBLE_PRECISION);
		Assert.assertEquals(800, bookQuote3.getSize());
		Assert.assertEquals(5, bookQuote3.getSymbolSequenceNumber());
		Assert.assertEquals(2, bookQuote3.getNumberOfOrders());

		BookQuote bookQuote4 = aggregatedSells.get(1);
		Assert.assertEquals(10.5, bookQuote4.getPrice(), BookQuoteCacheTest.PRICE_DOUBLE_PRECISION);
		Assert.assertEquals(500, bookQuote4.getSize());
		Assert.assertEquals(6, bookQuote4.getSymbolSequenceNumber());
		Assert.assertEquals(1, bookQuote4.getNumberOfOrders());
	}

	@Test
	public void testCancelOrderAll() throws Exception
	{
		BookQuoteCache cache = new BookQuoteCache(null, null, MdServiceType.BOOK_XNAS, "A-Z", 0);

		// Add orders
		cache.addOrder("A", "001", Side.BUY, 100, 10.0, Exchange.USEQ_TEST.getMicCode(), 1000);
		cache.addOrder("A", "002", Side.BUY, 200, 10.0, Exchange.USEQ_TEST.getMicCode(), 1001);
		cache.addOrder("A", "003", Side.BUY, 300, 10.0, Exchange.USEQ_TEST.getMicCode(), 1002);

		// Cancel an order that does not exist
		// nothing should happen, but a warning log
		cache.cancelOrder("004", 1003);

		// Confirm all orders were added
		Assert.assertNotNull(cache.getCachedOrder("001"));
		Assert.assertNotNull(cache.getCachedOrder("002"));
		Assert.assertNotNull(cache.getCachedOrder("003"));

		List<BookQuote> aggregatedBuys1 = cache.getAggregatedOrder("A", Side.BUY);
		BookQuote bookQuote1 = aggregatedBuys1.get(0);
		Assert.assertEquals(3, bookQuote1.getSymbolSequenceNumber());
		Assert.assertEquals(3, bookQuote1.getNumberOfOrders());

		// Full Cancel
		cache.cancelOrder("001", 1004);
		Assert.assertNull(cache.getCachedOrder("001"));

		// Confirm aggregate
		List<BookQuote> aggregatedBuys2 = cache.getAggregatedOrder("A", Side.BUY);
		System.out.println(cache.getAggregatedOrder("A", Side.BUY).size());
		BookQuote bookQuote2 = aggregatedBuys2.get(0);
		Assert.assertEquals(1, aggregatedBuys2.size());
		Assert.assertEquals(10.0, bookQuote2.getPrice(), BookQuoteCacheTest.PRICE_DOUBLE_PRECISION);
		Assert.assertEquals(500, bookQuote2.getSize());
		Assert.assertEquals(4, bookQuote2.getSymbolSequenceNumber());
		Assert.assertEquals(2, bookQuote2.getNumberOfOrders());
	}

	@Test
	public void testCancelPartial() throws Exception
	{
		BookQuoteCache cache = new BookQuoteCache(null, null, MdServiceType.BOOK_XNAS, "A-Z", 0);

		// Add orders
		cache.addOrder("A", "001", Side.BUY, 100, 10.0, Exchange.USEQ_TEST.getMicCode(), 1000);
		cache.addOrder("A", "002", Side.BUY, 200, 10.0, Exchange.USEQ_TEST.getMicCode(), 1001);
		cache.addOrder("A", "003", Side.BUY, 300, 10.0, Exchange.USEQ_TEST.getMicCode(), 1002);

		// Cancel an order that does not exist
		// nothing should happen, but a warning log
		cache.cancelOrder("004", 1003);

		// Confirm all orders were added
		Assert.assertNotNull(cache.getCachedOrder("001"));
		Assert.assertNotNull(cache.getCachedOrder("002"));
		Assert.assertNotNull(cache.getCachedOrder("003"));

		List<BookQuote> aggregatedBuys1 = cache.getAggregatedOrder("A", Side.BUY);
		BookQuote bookQuote1 = aggregatedBuys1.get(0);
		Assert.assertEquals(3, bookQuote1.getSymbolSequenceNumber());
		Assert.assertEquals(3, bookQuote1.getNumberOfOrders());

		// Partial Cancel
		cache.cancelOrder("003", 150, 1003);
		BookOrder bookOrder003 = cache.getCachedOrder("003");
		Assert.assertNotNull(bookOrder003);
		Assert.assertEquals(150, bookOrder003.getSize());

		// Confirm aggregate
		List<BookQuote> aggregatedBuys2 = cache.getAggregatedOrder("A", Side.BUY);
		bookQuote1 = aggregatedBuys2.get(0);
		Assert.assertEquals(1, aggregatedBuys2.size());
		Assert.assertEquals(10.0, bookQuote1.getPrice(), BookQuoteCacheTest.PRICE_DOUBLE_PRECISION);
		Assert.assertEquals(450, bookQuote1.getSize());
		Assert.assertEquals(4, bookQuote1.getSymbolSequenceNumber());
		Assert.assertEquals(3, bookQuote1.getNumberOfOrders());
	}

	@Test
	public void testCancelPartialInvalidSize() throws Exception
	{
		BookQuoteCache cache = new BookQuoteCache(null, null, MdServiceType.BOOK_XNAS, "A-Z", 0);

		// Add orders
		cache.addOrder("A", "001", Side.BUY, 100, 10.0, Exchange.USEQ_TEST.getMicCode(), 1000);
		cache.addOrder("A", "002", Side.BUY, 200, 10.0, Exchange.USEQ_TEST.getMicCode(), 1001);
		cache.addOrder("A", "003", Side.BUY, 300, 10.0, Exchange.USEQ_TEST.getMicCode(), 1002);

		// Confirm all orders were added
		Assert.assertNotNull(cache.getCachedOrder("001"));
		Assert.assertNotNull(cache.getCachedOrder("002"));
		Assert.assertNotNull(cache.getCachedOrder("003"));

		List<BookQuote> aggregatedBuys1 = cache.getAggregatedOrder("A", Side.BUY);
		BookQuote bookQuote1 = aggregatedBuys1.get(0);
		Assert.assertEquals(3, bookQuote1.getSymbolSequenceNumber());
		Assert.assertEquals(3, bookQuote1.getNumberOfOrders());

		// Partial Cancel, size is more than the original order's size
		cache.cancelOrder("003", 800, 1003);
		BookOrder bookOrder003 = cache.getCachedOrder("003");
		Assert.assertNull(bookOrder003);

		// Confirm aggregate
		List<BookQuote> aggregatedBuys2 = cache.getAggregatedOrder("A", Side.BUY);
		BookQuote bookQuote2 = aggregatedBuys2.get(0);
		Assert.assertEquals(1, aggregatedBuys2.size());
		Assert.assertEquals(10.0, bookQuote2.getPrice(), BookQuoteCacheTest.PRICE_DOUBLE_PRECISION);
		Assert.assertEquals(300, bookQuote2.getSize());
		Assert.assertEquals(4, bookQuote2.getSymbolSequenceNumber());
		Assert.assertEquals(2, bookQuote2.getNumberOfOrders());
	}

	@Test
	public void testReplaceOrders() throws Exception
	{
		BookQuoteCache cache = new BookQuoteCache(null, null, MdServiceType.BOOK_XNAS, "A-Z", 0);

		// Add orders
		cache.addOrder("A", "001", Side.BUY, 100, 12.0, Exchange.USEQ_TEST.getMicCode(), 1000);
		cache.addOrder("A", "002", Side.BUY, 200, 11.0, Exchange.USEQ_TEST.getMicCode(), 1001);
		cache.addOrder("A", "003", Side.BUY, 300, 10.0, Exchange.USEQ_TEST.getMicCode(), 1002);

		// Confirm all orders were added
		Assert.assertNotNull(cache.getCachedOrder("001"));
		Assert.assertNotNull(cache.getCachedOrder("002"));
		Assert.assertNotNull(cache.getCachedOrder("003"));

		List<BookQuote> aggregatedBuys1 = cache.getAggregatedOrder("A", Side.BUY);
		BookQuote bookQuote1 = aggregatedBuys1.get(0);
		BookQuote bookQuote2 = aggregatedBuys1.get(1);
		BookQuote bookQuote3 = aggregatedBuys1.get(2);

		Assert.assertEquals(1, bookQuote1.getSymbolSequenceNumber());
		Assert.assertEquals(1, bookQuote1.getNumberOfOrders());
		Assert.assertEquals(2, bookQuote2.getSymbolSequenceNumber());
		Assert.assertEquals(1, bookQuote2.getNumberOfOrders());
		Assert.assertEquals(3, bookQuote3.getSymbolSequenceNumber());
		Assert.assertEquals(1, bookQuote3.getNumberOfOrders());

		// replace by same price same size
		cache.replaceOrder("001", "004", 100, 12.0, 1003);
		Assert.assertNull(cache.getCachedOrder("001"));
		BookOrder bookOrder004 = cache.getCachedOrder("004");
		Assert.assertNotNull(bookOrder004);
		Assert.assertEquals(100, bookOrder004.getSize());

		List<BookQuote> aggregatedBuys2 = cache.getAggregatedOrder("A", Side.BUY);
		Assert.assertEquals(3, aggregatedBuys2.size());
		BookQuote bookQuote4 = aggregatedBuys2.get(0);
		BookQuote bookQuote5 = aggregatedBuys2.get(1);
		BookQuote bookQuote6 = aggregatedBuys2.get(2);

		Assert.assertEquals(12.0, bookQuote4.getPrice(), BookQuoteCacheTest.PRICE_DOUBLE_PRECISION);
		Assert.assertEquals(100, bookQuote4.getSize());
		Assert.assertEquals(1, bookQuote4.getSymbolSequenceNumber());
		Assert.assertEquals(1, bookQuote4.getNumberOfOrders());

		Assert.assertEquals(11.0, bookQuote5.getPrice(), BookQuoteCacheTest.PRICE_DOUBLE_PRECISION);
		Assert.assertEquals(200, bookQuote5.getSize());
		Assert.assertEquals(2, bookQuote5.getSymbolSequenceNumber());
		Assert.assertEquals(1, bookQuote5.getNumberOfOrders());

		Assert.assertEquals(10.0, bookQuote6.getPrice(), BookQuoteCacheTest.PRICE_DOUBLE_PRECISION);
		Assert.assertEquals(300, bookQuote6.getSize());
		Assert.assertEquals(3, bookQuote6.getSymbolSequenceNumber());
		Assert.assertEquals(1, bookQuote6.getNumberOfOrders());
	}

	@Test
	public void testReplaceOrdersDiffSize() throws Exception
	{
		BookQuoteCache cache = new BookQuoteCache(null, null, MdServiceType.BOOK_XNAS, "A-Z", 0);

		// Add orders
		cache.addOrder("A", "001", Side.BUY, 100, 12.0, Exchange.USEQ_TEST.getMicCode(), 1000);
		cache.addOrder("A", "002", Side.BUY, 200, 11.0, Exchange.USEQ_TEST.getMicCode(), 1001);
		cache.addOrder("A", "003", Side.BUY, 300, 10.0, Exchange.USEQ_TEST.getMicCode(), 1002);

		// Confirm all orders were added
		Assert.assertNotNull(cache.getCachedOrder("001"));
		Assert.assertNotNull(cache.getCachedOrder("002"));
		Assert.assertNotNull(cache.getCachedOrder("003"));

		List<BookQuote> aggregatedBuys1 = cache.getAggregatedOrder("A", Side.BUY);
		BookQuote bookQuote1 = aggregatedBuys1.get(0);
		BookQuote bookQuote2 = aggregatedBuys1.get(1);
		BookQuote bookQuote3 = aggregatedBuys1.get(2);

		Assert.assertEquals(1, bookQuote1.getSymbolSequenceNumber());
		Assert.assertEquals(1, bookQuote1.getNumberOfOrders());
		Assert.assertEquals(2, bookQuote2.getSymbolSequenceNumber());
		Assert.assertEquals(1, bookQuote2.getNumberOfOrders());
		Assert.assertEquals(3, bookQuote3.getSymbolSequenceNumber());
		Assert.assertEquals(1, bookQuote3.getNumberOfOrders());

		// replace by same price different size
		cache.replaceOrder("002", "004", 400, 11.0, 1004);
		Assert.assertNull(cache.getCachedOrder("002"));
		BookOrder bookOrder004 = cache.getCachedOrder("004");
		Assert.assertNotNull(bookOrder004);
		Assert.assertEquals(400, bookOrder004.getSize());

		List<BookQuote> aggregatedBuys2 = cache.getAggregatedOrder("A", Side.BUY);
		Assert.assertEquals(3, aggregatedBuys2.size());
		BookQuote bookQuote4 = aggregatedBuys2.get(0);
		BookQuote bookQuote5 = aggregatedBuys2.get(1);
		BookQuote bookQuote6 = aggregatedBuys2.get(2);

		Assert.assertEquals(12.0, bookQuote4.getPrice(), BookQuoteCacheTest.PRICE_DOUBLE_PRECISION);
		Assert.assertEquals(100, bookQuote4.getSize());
		Assert.assertEquals(1, bookQuote4.getSymbolSequenceNumber());
		Assert.assertEquals(1, bookQuote4.getNumberOfOrders());

		Assert.assertEquals(11.0, bookQuote5.getPrice(), BookQuoteCacheTest.PRICE_DOUBLE_PRECISION);
		Assert.assertEquals(400, bookQuote5.getSize());
		Assert.assertEquals(4, bookQuote5.getSymbolSequenceNumber());
		Assert.assertEquals(1, bookQuote5.getNumberOfOrders());

		Assert.assertEquals(10.0, bookQuote6.getPrice(), BookQuoteCacheTest.PRICE_DOUBLE_PRECISION);
		Assert.assertEquals(300, bookQuote6.getSize());
		Assert.assertEquals(3, bookQuote6.getSymbolSequenceNumber());
		Assert.assertEquals(1, bookQuote6.getNumberOfOrders());
	}

	@Test
	public void testReplaceOrdersDiffPriceDiffSize() throws Exception
	{
		BookQuoteCache cache = new BookQuoteCache(null, null, MdServiceType.BOOK_XNAS, "A-Z", 0);

		// Add orders
		cache.addOrder("A", "001", Side.BUY, 100, 12.0, Exchange.USEQ_TEST.getMicCode(), 1000);
		cache.addOrder("A", "002", Side.BUY, 200, 11.0, Exchange.USEQ_TEST.getMicCode(), 1001);
		cache.addOrder("A", "003", Side.BUY, 300, 10.0, Exchange.USEQ_TEST.getMicCode(), 1002);

		// Confirm all orders were added
		Assert.assertNotNull(cache.getCachedOrder("001"));
		Assert.assertNotNull(cache.getCachedOrder("002"));
		Assert.assertNotNull(cache.getCachedOrder("003"));

		List<BookQuote> aggregatedBuys1 = cache.getAggregatedOrder("A", Side.BUY);
		BookQuote bookQuote1 = aggregatedBuys1.get(0);
		BookQuote bookQuote2 = aggregatedBuys1.get(1);
		BookQuote bookQuote3 = aggregatedBuys1.get(2);

		Assert.assertEquals(1, bookQuote1.getSymbolSequenceNumber());
		Assert.assertEquals(1, bookQuote1.getNumberOfOrders());
		Assert.assertEquals(2, bookQuote2.getSymbolSequenceNumber());
		Assert.assertEquals(1, bookQuote2.getNumberOfOrders());
		Assert.assertEquals(3, bookQuote3.getSymbolSequenceNumber());
		Assert.assertEquals(1, bookQuote3.getNumberOfOrders());

		// replace by different price different size
		cache.replaceOrder("003", "004", 50, 13.0, 1005);
		Assert.assertNull(cache.getCachedOrder("003"));
		BookOrder bookOrder004 = cache.getCachedOrder("004");
		Assert.assertNotNull(bookOrder004);
		Assert.assertEquals(50, bookOrder004.getSize());

		List<BookQuote> aggregatedBuys2 = cache.getAggregatedOrder("A", Side.BUY);
		Assert.assertEquals(3, aggregatedBuys2.size());
		BookQuote bookQuote4 = aggregatedBuys2.get(0);
		BookQuote bookQuote5 = aggregatedBuys2.get(1);
		BookQuote bookQuote6 = aggregatedBuys2.get(2);

		Assert.assertEquals(13.0, bookQuote4.getPrice(), BookQuoteCacheTest.PRICE_DOUBLE_PRECISION);
		Assert.assertEquals(50, bookQuote4.getSize());
		Assert.assertEquals(5, bookQuote4.getSymbolSequenceNumber());
		Assert.assertEquals(1, bookQuote4.getNumberOfOrders());

		Assert.assertEquals(12.0, bookQuote5.getPrice(), BookQuoteCacheTest.PRICE_DOUBLE_PRECISION);
		Assert.assertEquals(100, bookQuote5.getSize());
		Assert.assertEquals(1, bookQuote5.getSymbolSequenceNumber());
		Assert.assertEquals(1, bookQuote5.getNumberOfOrders());

		Assert.assertEquals(11.0, bookQuote6.getPrice(), BookQuoteCacheTest.PRICE_DOUBLE_PRECISION);
		Assert.assertEquals(200, bookQuote6.getSize());
		Assert.assertEquals(2, bookQuote6.getSymbolSequenceNumber());
		Assert.assertEquals(1, bookQuote6.getNumberOfOrders());
	}

	@Test
	public void testReplaceOrdersSamePriceDiffSize() throws Exception
	{
		BookQuoteCache cache = new BookQuoteCache(null, null, MdServiceType.BOOK_XNAS, "A-Z", 0);

		double price = 10.0;
		int size1 = 100;
		// Add order
		cache.addOrder("A", "001", Side.BUY, size1, price, Exchange.USEQ_TEST.getMicCode(), 1001);
		Assert.assertNotNull(cache.getCachedOrder("001"));
		List<BookQuote> aggregatedBuys1 = cache.getAggregatedOrder("A", Side.BUY);
		BookQuote bookQuote1 = aggregatedBuys1.get(0);
		Assert.assertEquals(1, bookQuote1.getSymbolSequenceNumber());
		Assert.assertEquals(1, bookQuote1.getNumberOfOrders());

		// replace by same price different size
		int size2 = 30;
		cache.replaceOrder("001", "002", size2, price, 1002);
		Assert.assertNull(cache.getCachedOrder("001"));
		BookOrder bookOrder002 = cache.getCachedOrder("002");
		Assert.assertNotNull(bookOrder002);
		Assert.assertEquals(size2, bookOrder002.getSize());

		List<BookQuote> aggregatedBuys2 = cache.getAggregatedOrder("A", Side.BUY);
		Assert.assertEquals(1, aggregatedBuys2.size());
		BookQuote bookQuote2 = aggregatedBuys2.get(0);
		Assert.assertEquals(price, bookQuote2.getPrice(), BookQuoteCacheTest.PRICE_DOUBLE_PRECISION);
		Assert.assertEquals(size2, bookQuote2.getSize());
		Assert.assertEquals(2, bookQuote2.getSymbolSequenceNumber());// sequence number is updated
		Assert.assertEquals(1, bookQuote2.getNumberOfOrders());
	}

	@Test
	public void testReplaceOrdersSamePriceSameSize() throws Exception
	{
		BookQuoteCache cache = new BookQuoteCache(null, null, MdServiceType.BOOK_XNAS, "A-Z", 0);

		double price = 10.0;
		int size = 100;
		// Add order
		cache.addOrder("A", "001", Side.BUY, size, price, Exchange.USEQ_TEST.getMicCode(), 1001);
		Assert.assertNotNull(cache.getCachedOrder("001"));
		List<BookQuote> aggregatedBuys1 = cache.getAggregatedOrder("A", Side.BUY);
		BookQuote bookQuote1 = aggregatedBuys1.get(0);
		Assert.assertEquals(1, bookQuote1.getSymbolSequenceNumber());
		Assert.assertEquals(1, bookQuote1.getNumberOfOrders());

		// replace by same price same size
		cache.replaceOrder("001", "002", size, price, 1002);
		Assert.assertNull(cache.getCachedOrder("001"));
		BookOrder bookOrder002 = cache.getCachedOrder("002");
		Assert.assertNotNull(bookOrder002);
		Assert.assertEquals(size, bookOrder002.getSize());

		List<BookQuote> aggregatedBuys2 = cache.getAggregatedOrder("A", Side.BUY);
		Assert.assertEquals(1, aggregatedBuys2.size());
		BookQuote bookQuote2 = aggregatedBuys2.get(0);
		Assert.assertEquals(price, bookQuote2.getPrice(), BookQuoteCacheTest.PRICE_DOUBLE_PRECISION);
		Assert.assertEquals(size, bookQuote2.getSize());
		Assert.assertEquals(1, bookQuote2.getSymbolSequenceNumber());// sequence number is not updated
		Assert.assertEquals(1, bookQuote2.getNumberOfOrders());
	}

	@Test
	public void testBidTopNLevelWithAddOrder() throws Exception
	{
		BookQuoteCache cache = new BookQuoteCache(null, null, MdServiceType.BOOK_XNAS, "A-Z", 0);
		long timeNano = 1000;
		Side side = Side.BUY;
		String symbol = "A";
		int size = 100;

		// Top 5 level: 10.0
		cache.addOrder(symbol, "001", side, size, 10.0, Exchange.USEQ_TEST.getMicCode(), timeNano++);
		BookQuoteCache.Book bookForSymbol = cache.getSymbolToBook(symbol);
		Assert.assertEquals(10.0, bookForSymbol.getBidLastTopLevel(), BookQuoteCacheTest.PRICE_DOUBLE_PRECISION);

		// Top 5 level: 11.0 10.0
		cache.addOrder(symbol, "002", side, size, 11.0, Exchange.USEQ_TEST.getMicCode(), timeNano++);
		Assert.assertEquals(10.0, bookForSymbol.getBidLastTopLevel(), BookQuoteCacheTest.PRICE_DOUBLE_PRECISION);

		// Top 5 level: 11.0 10.0 8.0
		cache.addOrder(symbol, "003", side, size, 8.0, Exchange.USEQ_TEST.getMicCode(), timeNano++);
		Assert.assertEquals(8.0, bookForSymbol.getBidLastTopLevel(), BookQuoteCacheTest.PRICE_DOUBLE_PRECISION);

		// Top 5 level: 11.0 10.0 9.0 8.0
		cache.addOrder(symbol, "004", side, size, 9.0, Exchange.USEQ_TEST.getMicCode(), timeNano++);
		Assert.assertEquals(8.0, bookForSymbol.getBidLastTopLevel(), BookQuoteCacheTest.PRICE_DOUBLE_PRECISION);

		// Top 5 level: 12.0 11.0 10.0 9.0 8.0
		cache.addOrder(symbol, "005", side, size, 12.0, Exchange.USEQ_TEST.getMicCode(), timeNano++);
		Assert.assertEquals(8.0, bookForSymbol.getBidLastTopLevel(), BookQuoteCacheTest.PRICE_DOUBLE_PRECISION);

		// Top 5 level: 12.0 11.0 10.0 9.0 8.0 (7.0 is not included in top 5)
		cache.addOrder(symbol, "005", side, size, 7.0, Exchange.USEQ_TEST.getMicCode(), timeNano++);
		Assert.assertEquals(8.0, bookForSymbol.getBidLastTopLevel(), BookQuoteCacheTest.PRICE_DOUBLE_PRECISION);

		// Top 5 level: 13.0 12.0 11.0 10.0 9.0 (8.0 is removed from top 5)
		cache.addOrder(symbol, "006", side, size, 13.0, Exchange.USEQ_TEST.getMicCode(), timeNano++);
		Assert.assertEquals(9.0, bookForSymbol.getBidLastTopLevel(), BookQuoteCacheTest.PRICE_DOUBLE_PRECISION);

		// add to same prices
		cache.addOrder(symbol, "007", side, size, 7.0, Exchange.USEQ_TEST.getMicCode(), timeNano++);
		Assert.assertEquals(9.0, bookForSymbol.getBidLastTopLevel(), BookQuoteCacheTest.PRICE_DOUBLE_PRECISION);
		cache.addOrder(symbol, "008", side, size, 8.0, Exchange.USEQ_TEST.getMicCode(), timeNano++);
		Assert.assertEquals(9.0, bookForSymbol.getBidLastTopLevel(), BookQuoteCacheTest.PRICE_DOUBLE_PRECISION);
		cache.addOrder(symbol, "009", side, size, 9.0, Exchange.USEQ_TEST.getMicCode(), timeNano++);
		Assert.assertEquals(9.0, bookForSymbol.getBidLastTopLevel(), BookQuoteCacheTest.PRICE_DOUBLE_PRECISION);
		cache.addOrder(symbol, "010", side, size, 11.0, Exchange.USEQ_TEST.getMicCode(), timeNano++);
		Assert.assertEquals(9.0, bookForSymbol.getBidLastTopLevel(), BookQuoteCacheTest.PRICE_DOUBLE_PRECISION);
	}

	@Test
	public void testAskTopNLevelWithAddOrder() throws Exception
	{
		BookQuoteCache cache = new BookQuoteCache(null, null, MdServiceType.BOOK_XNAS, "A-Z", 0);
		long timeNano = 1000;
		Side side = Side.SELL;
		String symbol = "A";
		int size = 100;

		// Top 5 level: 10.0
		cache.addOrder(symbol, "001", side, size, 10.0, Exchange.USEQ_TEST.getMicCode(), timeNano++);
		BookQuoteCache.Book bookForSymbol = cache.getSymbolToBook(symbol);
		Assert.assertEquals(10.0, bookForSymbol.getAskLastTopLevel(), BookQuoteCacheTest.PRICE_DOUBLE_PRECISION);

		// Top 5 level: 10.0 11.0
		cache.addOrder(symbol, "002", side, size, 11.0, Exchange.USEQ_TEST.getMicCode(), timeNano++);
		Assert.assertEquals(11.0, bookForSymbol.getAskLastTopLevel(), BookQuoteCacheTest.PRICE_DOUBLE_PRECISION);

		// Top 5 level: 8.0 10.0 11.0
		cache.addOrder(symbol, "003", side, size, 8.0, Exchange.USEQ_TEST.getMicCode(), timeNano++);
		Assert.assertEquals(11.0, bookForSymbol.getAskLastTopLevel(), BookQuoteCacheTest.PRICE_DOUBLE_PRECISION);

		// Top 5 level: 8.0 9.0 10.0 11.0
		cache.addOrder(symbol, "004", side, size, 9.0, Exchange.USEQ_TEST.getMicCode(), timeNano++);
		Assert.assertEquals(11.0, bookForSymbol.getAskLastTopLevel(), BookQuoteCacheTest.PRICE_DOUBLE_PRECISION);

		// Top 5 level: 8.0 9.0 10.0 11.0 12.0
		cache.addOrder(symbol, "005", side, size, 12.0, Exchange.USEQ_TEST.getMicCode(), timeNano++);
		Assert.assertEquals(12.0, bookForSymbol.getAskLastTopLevel(), BookQuoteCacheTest.PRICE_DOUBLE_PRECISION);

		// Top 5 level: 7.0 8.0 9.0 10.0 11.0 (12.0 is removed from top 5)
		cache.addOrder(symbol, "005", side, size, 7.0, Exchange.USEQ_TEST.getMicCode(), timeNano++);
		Assert.assertEquals(11.0, bookForSymbol.getAskLastTopLevel(), BookQuoteCacheTest.PRICE_DOUBLE_PRECISION);

		// Top 5 level: 7.0 8.0 9.0 10.0 11.0 (13.0 is not included in top 5)
		cache.addOrder(symbol, "006", side, size, 13.0, Exchange.USEQ_TEST.getMicCode(), timeNano++);
		Assert.assertEquals(11.0, bookForSymbol.getAskLastTopLevel(), BookQuoteCacheTest.PRICE_DOUBLE_PRECISION);

		// add to same prices
		cache.addOrder(symbol, "007", side, size, 7.0, Exchange.USEQ_TEST.getMicCode(), timeNano++);
		Assert.assertEquals(11.0, bookForSymbol.getAskLastTopLevel(), BookQuoteCacheTest.PRICE_DOUBLE_PRECISION);
		cache.addOrder(symbol, "008", side, size, 8.0, Exchange.USEQ_TEST.getMicCode(), timeNano++);
		Assert.assertEquals(11.0, bookForSymbol.getAskLastTopLevel(), BookQuoteCacheTest.PRICE_DOUBLE_PRECISION);
		cache.addOrder(symbol, "009", side, size, 9.0, Exchange.USEQ_TEST.getMicCode(), timeNano++);
		Assert.assertEquals(11.0, bookForSymbol.getAskLastTopLevel(), BookQuoteCacheTest.PRICE_DOUBLE_PRECISION);
		cache.addOrder(symbol, "010", side, size, 11.0, Exchange.USEQ_TEST.getMicCode(), timeNano++);
		Assert.assertEquals(11.0, bookForSymbol.getAskLastTopLevel(), BookQuoteCacheTest.PRICE_DOUBLE_PRECISION);
	}

	@Test
	public void testBidTopNLevelWithCancelOrder() throws Exception
	{
		BookQuoteCache cache = new BookQuoteCache(null, null, MdServiceType.BOOK_XNAS, "A-Z", 0);
		long timeNano = 1000;
		Side side = Side.BUY;
		String symbol = "A";
		int size = 100;

		cache.addOrder(symbol, "000", side, size, 10.0, Exchange.USEQ_TEST.getMicCode(), timeNano++);
		BookQuoteCache.Book bookForSymbol = cache.getSymbolToBook(symbol);
		Assert.assertEquals(10.0, bookForSymbol.getBidLastTopLevel(), BookQuoteCacheTest.PRICE_DOUBLE_PRECISION);

		cache.cancelOrder("000", timeNano++);
		Assert.assertEquals(0, bookForSymbol.getBidLastTopLevel(), BookQuoteCacheTest.PRICE_DOUBLE_PRECISION);

		cache.addOrder(symbol, "001", side, size, 11.0, Exchange.USEQ_TEST.getMicCode(), timeNano++);
		cache.addOrder(symbol, "002", side, size, 12.0, Exchange.USEQ_TEST.getMicCode(), timeNano++);
		cache.addOrder(symbol, "003", side, size, 13.0, Exchange.USEQ_TEST.getMicCode(), timeNano++);
		cache.addOrder(symbol, "004", side, size, 14.0, Exchange.USEQ_TEST.getMicCode(), timeNano++);
		cache.addOrder(symbol, "005", side, size, 15.0, Exchange.USEQ_TEST.getMicCode(), timeNano++);
		cache.addOrder(symbol, "006", side, size, 16.0, Exchange.USEQ_TEST.getMicCode(), timeNano++);
		cache.addOrder(symbol, "007", side, size, 10.0, Exchange.USEQ_TEST.getMicCode(), timeNano++);

		// Top 5 level: 16.0 15.0 14.0 13.0 12.0 (11.0 10.0 are in the cache but not top 5)
		Assert.assertEquals(12.0, bookForSymbol.getBidLastTopLevel(), BookQuoteCacheTest.PRICE_DOUBLE_PRECISION);

		cache.cancelOrder("003", timeNano++);// remove 13.0
		Assert.assertEquals(11.0, bookForSymbol.getBidLastTopLevel(), BookQuoteCacheTest.PRICE_DOUBLE_PRECISION);

		cache.cancelOrder("007", timeNano++);// remove 10.0
		Assert.assertEquals(11.0, bookForSymbol.getBidLastTopLevel(), BookQuoteCacheTest.PRICE_DOUBLE_PRECISION);

		cache.cancelOrder("004", timeNano++);// remove 14.0
		Assert.assertEquals(11.0, bookForSymbol.getBidLastTopLevel(), BookQuoteCacheTest.PRICE_DOUBLE_PRECISION);

		cache.cancelOrder("001", timeNano++);// remove 11.0
		Assert.assertEquals(12.0, bookForSymbol.getBidLastTopLevel(), BookQuoteCacheTest.PRICE_DOUBLE_PRECISION);

		cache.cancelOrder("002", timeNano++);// remove 12.0
		Assert.assertEquals(15.0, bookForSymbol.getBidLastTopLevel(), BookQuoteCacheTest.PRICE_DOUBLE_PRECISION);

		cache.cancelOrder("005", timeNano++);// remove 15.0
		Assert.assertEquals(16.0, bookForSymbol.getBidLastTopLevel(), BookQuoteCacheTest.PRICE_DOUBLE_PRECISION);

		cache.cancelOrder("006", timeNano++);// remove 16.0
		Assert.assertEquals(0, bookForSymbol.getBidLastTopLevel(), BookQuoteCacheTest.PRICE_DOUBLE_PRECISION);
	}

	@Test
	public void testAskTopNLevelWithCancelOrder() throws Exception
	{
		BookQuoteCache cache = new BookQuoteCache(null, null, MdServiceType.BOOK_XNAS, "A-Z", 0);
		long timeNano = 1000;
		Side side = Side.SELL;
		String symbol = "A";
		int size = 100;

		cache.addOrder(symbol, "000", side, size, 10.0, Exchange.USEQ_TEST.getMicCode(), timeNano++);
		BookQuoteCache.Book bookForSymbol = cache.getSymbolToBook(symbol);
		Assert.assertEquals(10.0, bookForSymbol.getAskLastTopLevel(), BookQuoteCacheTest.PRICE_DOUBLE_PRECISION);

		cache.cancelOrder("000", timeNano++);
		Assert.assertEquals(0, bookForSymbol.getAskLastTopLevel(), BookQuoteCacheTest.PRICE_DOUBLE_PRECISION);

		cache.addOrder(symbol, "001", side, size, 11.0, Exchange.USEQ_TEST.getMicCode(), timeNano++);
		cache.addOrder(symbol, "002", side, size, 12.0, Exchange.USEQ_TEST.getMicCode(), timeNano++);
		cache.addOrder(symbol, "003", side, size, 13.0, Exchange.USEQ_TEST.getMicCode(), timeNano++);
		cache.addOrder(symbol, "004", side, size, 14.0, Exchange.USEQ_TEST.getMicCode(), timeNano++);
		cache.addOrder(symbol, "005", side, size, 15.0, Exchange.USEQ_TEST.getMicCode(), timeNano++);
		cache.addOrder(symbol, "006", side, size, 16.0, Exchange.USEQ_TEST.getMicCode(), timeNano++);
		cache.addOrder(symbol, "007", side, size, 10.0, Exchange.USEQ_TEST.getMicCode(), timeNano++);

		// Top 5 level: 10.0 11.0 12.0 13.0 14.0 (15.0 16.0 are in the cache but not top 5)
		Assert.assertEquals(14.0, bookForSymbol.getAskLastTopLevel(), BookQuoteCacheTest.PRICE_DOUBLE_PRECISION);

		cache.cancelOrder("006", timeNano++);// remove 16.0
		Assert.assertEquals(14.0, bookForSymbol.getAskLastTopLevel(), BookQuoteCacheTest.PRICE_DOUBLE_PRECISION);

		cache.cancelOrder("003", timeNano++);// remove 13.0
		Assert.assertEquals(15.0, bookForSymbol.getAskLastTopLevel(), BookQuoteCacheTest.PRICE_DOUBLE_PRECISION);

		cache.cancelOrder("002", timeNano++);// remove 12.0
		Assert.assertEquals(15.0, bookForSymbol.getAskLastTopLevel(), BookQuoteCacheTest.PRICE_DOUBLE_PRECISION);

		cache.cancelOrder("001", timeNano++);// remove 11.0
		Assert.assertEquals(15.0, bookForSymbol.getAskLastTopLevel(), BookQuoteCacheTest.PRICE_DOUBLE_PRECISION);

		cache.cancelOrder("007", timeNano++);// remove 10.0
		Assert.assertEquals(15.0, bookForSymbol.getAskLastTopLevel(), BookQuoteCacheTest.PRICE_DOUBLE_PRECISION);

		cache.cancelOrder("005", timeNano++);// remove 15.0
		Assert.assertEquals(14.0, bookForSymbol.getAskLastTopLevel(), BookQuoteCacheTest.PRICE_DOUBLE_PRECISION);

		cache.cancelOrder("004", timeNano++);// remove 14.0
		Assert.assertEquals(0, bookForSymbol.getAskLastTopLevel(), BookQuoteCacheTest.PRICE_DOUBLE_PRECISION);
	}
}

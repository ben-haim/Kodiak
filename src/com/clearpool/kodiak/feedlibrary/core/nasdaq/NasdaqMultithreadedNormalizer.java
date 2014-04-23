package com.clearpool.kodiak.feedlibrary.core.nasdaq;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

import com.clearpool.common.util.DateUtil;
import com.clearpool.kodiak.feedlibrary.caches.BookQuoteCache;
import com.clearpool.kodiak.feedlibrary.caches.IMdServiceCache;
import com.clearpool.kodiak.feedlibrary.caches.ImbalanceCache;
import com.clearpool.kodiak.feedlibrary.callbacks.IMdBookQuoteListener;
import com.clearpool.kodiak.feedlibrary.callbacks.IMdImbalanceListener;
import com.clearpool.kodiak.feedlibrary.callbacks.IMdLibraryCallback;
import com.clearpool.kodiak.feedlibrary.core.IMdNormalizer;
import com.clearpool.kodiak.feedlibrary.core.MdFeed;
import com.clearpool.kodiak.feedlibrary.core.MdFeedPacket;
import com.clearpool.kodiak.feedlibrary.utils.ByteBufferUtil;
import com.clearpool.messageobjects.marketdata.AuctionType;
import com.clearpool.messageobjects.marketdata.Exchange;
import com.clearpool.messageobjects.marketdata.MdServiceType;
import com.clearpool.messageobjects.order.Side;

public class NasdaqMultithreadedNormalizer implements IMdNormalizer
{
	private static final Logger LOGGER = Logger.getLogger(NasdaqMultithreadedNormalizer.class.getName());
	private static final int NUMBER_OF_THREADS = 2;
	private static final int LETTER_DIVISOR = 13;
	private static final String[] RANGES = new String[] { "A-M", "N-Z" };

	private static final byte TIMESTAMP_SECONDS = 'T';
	private static final byte SYSTEM_EVENT = 'S';
	private static final byte ADD_ORDER_NO_MPID = 'A';
	private static final byte ADD_ORDER_WITH_MPID = 'F';
	private static final byte ORDER_EXECUTED = 'E';
	private static final byte ORDER_EXECUTED_WITH_PRICE = 'C';
	private static final byte ORDER_CANCEL = 'X';
	private static final byte ORDER_DELETE = 'D';
	private static final byte ORDER_REPLACE = 'U';
	private static final byte NOII = 'I';

	private final BookQuoteCache[] bookCaches;
	private final ImbalanceCache[] imbalanceCaches;
	private final ExecutorService[] executors;
	final Map<String, Integer> orderIdToIndex;
	private final long midnight;

	private long secondsSinceMidnight;

	public NasdaqMultithreadedNormalizer(Map<MdServiceType, IMdLibraryCallback> callbacks, @SuppressWarnings("unused") String range)
	{
		this.bookCaches = new BookQuoteCache[NUMBER_OF_THREADS];
		this.imbalanceCaches = new ImbalanceCache[NUMBER_OF_THREADS];
		this.executors = new ExecutorService[NUMBER_OF_THREADS];

		for (int i = 0; i < NUMBER_OF_THREADS; i++)
		{
			this.bookCaches[i] = new BookQuoteCache((IMdBookQuoteListener) callbacks.get(MdServiceType.BOOK_XNAS), MdFeed.NASDAQ, MdServiceType.BOOK_XNAS, RANGES[i]);
			this.imbalanceCaches[i] = new ImbalanceCache((IMdImbalanceListener) callbacks.get(MdServiceType.IMBALANCE_XNAS), MdFeed.NASDAQ, MdServiceType.IMBALANCE_XNAS, RANGES[i]);
			this.executors[i] = Executors.newSingleThreadExecutor();
		}
		this.orderIdToIndex = new ConcurrentHashMap<String, Integer>();
		this.midnight = DateUtil.TODAY_MIDNIGHT_EST.getTime();

		this.secondsSinceMidnight = 0;
	}

	@Override
	public IMdServiceCache[] getMdServiceCaches()
	{
		IMdServiceCache[] caches = new IMdServiceCache[NUMBER_OF_THREADS * 2];
		for (int i = 0; i < NUMBER_OF_THREADS; i++)
		{
			caches[i * 2] = this.bookCaches[i];
			caches[i * 2 + 1] = this.imbalanceCaches[i];
		}
		return caches;
	}

	@Override
	public void processMessage(String processorName, MdFeedPacket packet, boolean shouldIgnore)
	{
		NasdaqPacket nasdaqPacket = (NasdaqPacket) packet;
		ByteBuffer buffer = nasdaqPacket.getBuffer();

		int messageLength = ByteBufferUtil.getUnsignedShort(buffer);
		if (shouldIgnore)
		{
			ByteBufferUtil.advancePosition(buffer, messageLength);
			return;
		}

		int position = buffer.position();
		byte messageType = buffer.get();
		if (messageType == ADD_ORDER_NO_MPID || messageType == ADD_ORDER_WITH_MPID)
		{
			final long timestamp = getTimestamp(ByteBufferUtil.getUnsignedInt(buffer));
			final String orderReferenceNumber = String.valueOf(buffer.getLong());
			final Side side = (buffer.get() == 'B') ? Side.BUY : Side.SELL;
			final long shares = ByteBufferUtil.getUnsignedInt(buffer);
			final String symbol = ByteBufferUtil.getString(buffer, 8);
			final double price = getPrice(ByteBufferUtil.getUnsignedInt(buffer));
			// String displayName = (messageType == ADD_ORDER_WITH_MPID) ? ByteBufferUtil.getString(buffer, 4) : Exchange.USEQ_NASDAQ_OMX.getMicCode();

			final int index = getThreadIndex(symbol);
			this.orderIdToIndex.put(orderReferenceNumber, Integer.valueOf(index));
			final BookQuoteCache bookQuoteCache = this.bookCaches[index];
			final ExecutorService executor = this.executors[index];
			executor.submit(new Runnable() {

				@Override
				public void run()
				{
					bookQuoteCache.addOrder(symbol, orderReferenceNumber, side, (int) shares, price, Exchange.USEQ_NASDAQ_OMX.getMicCode(), timestamp);
				}
			});
		}
		else if (messageType == ORDER_EXECUTED)
		{
			final long timestamp = getTimestamp(ByteBufferUtil.getUnsignedInt(buffer));
			final String orderReferenceNumber = String.valueOf(buffer.getLong());
			final long executedShares = ByteBufferUtil.getUnsignedInt(buffer);
			ByteBufferUtil.advancePosition(buffer, 8);

			final Integer index = this.orderIdToIndex.get(orderReferenceNumber);
			if (index != null)
			{
				final BookQuoteCache bookQuoteCache = this.bookCaches[index.intValue()];
				final ExecutorService executor = this.executors[index.intValue()];
				executor.submit(new Runnable() {

					@Override
					public void run()
					{
						boolean isOrderComplete = bookQuoteCache.cancelOrder(orderReferenceNumber, (int) executedShares, timestamp);
						if (isOrderComplete)
						{
							NasdaqMultithreadedNormalizer.this.orderIdToIndex.remove(orderReferenceNumber);
						}
					}
				});
			}
			else
			{
				LOGGER.severe("Unable to find orderId=" + orderReferenceNumber);
			}
		}
		else if (messageType == ORDER_EXECUTED_WITH_PRICE)
		{
			final long timestamp = getTimestamp(ByteBufferUtil.getUnsignedInt(buffer));
			final String orderReferenceNumber = String.valueOf(buffer.getLong());
			final long executedShares = ByteBufferUtil.getUnsignedInt(buffer);
			ByteBufferUtil.advancePosition(buffer, 13);

			final Integer index = this.orderIdToIndex.get(orderReferenceNumber);
			if (index != null)
			{
				final BookQuoteCache bookQuoteCache = this.bookCaches[index.intValue()];
				final ExecutorService executor = this.executors[index.intValue()];
				executor.submit(new Runnable() {

					@Override
					public void run()
					{
						boolean isOrderComplete = bookQuoteCache.cancelOrder(orderReferenceNumber, (int) executedShares, timestamp);
						if (isOrderComplete)
						{
							NasdaqMultithreadedNormalizer.this.orderIdToIndex.remove(orderReferenceNumber);
						}
					}
				});
			}
			else
			{
				LOGGER.severe("Unable to find orderId=" + orderReferenceNumber);
			}
		}
		else if (messageType == ORDER_CANCEL)
		{
			final long timestamp = getTimestamp(ByteBufferUtil.getUnsignedInt(buffer));
			final String orderReferenceNumber = String.valueOf(buffer.getLong());
			final long cancelledShares = ByteBufferUtil.getUnsignedInt(buffer);

			final Integer index = this.orderIdToIndex.get(orderReferenceNumber);
			if (index != null)
			{
				final BookQuoteCache bookQuoteCache = this.bookCaches[index.intValue()];
				final ExecutorService executor = this.executors[index.intValue()];
				executor.submit(new Runnable() {

					@Override
					public void run()
					{
						boolean isOrderComplete = bookQuoteCache.cancelOrder(orderReferenceNumber, (int) cancelledShares, timestamp);
						if (isOrderComplete)
						{
							NasdaqMultithreadedNormalizer.this.orderIdToIndex.remove(orderReferenceNumber);
						}
					}
				});
			}
			else
			{
				LOGGER.severe("Unable to find orderId=" + orderReferenceNumber);
			}
		}
		else if (messageType == ORDER_DELETE)
		{
			final long timestamp = getTimestamp(ByteBufferUtil.getUnsignedInt(buffer));
			final String orderReferenceNumber = String.valueOf(buffer.getLong());

			final Integer index = this.orderIdToIndex.get(orderReferenceNumber);
			if (index != null)
			{
				final BookQuoteCache bookQuoteCache = this.bookCaches[index.intValue()];
				final ExecutorService executor = this.executors[index.intValue()];
				executor.submit(new Runnable() {

					@Override
					public void run()
					{
						bookQuoteCache.cancelOrder(orderReferenceNumber, timestamp);
						NasdaqMultithreadedNormalizer.this.orderIdToIndex.remove(orderReferenceNumber);
					}
				});
			}
			else
			{
				LOGGER.severe("Unable to find orderId=" + orderReferenceNumber);
			}
		}
		else if (messageType == ORDER_REPLACE)
		{
			final long timestamp = getTimestamp(ByteBufferUtil.getUnsignedInt(buffer));
			final String originalOrderReferenceNumber = String.valueOf(buffer.getLong());
			final String newOrderReferenceNumber = String.valueOf(buffer.getLong());
			final long shares = ByteBufferUtil.getUnsignedInt(buffer);
			final double price = getPrice(ByteBufferUtil.getUnsignedInt(buffer));

			final Integer index = this.orderIdToIndex.get(originalOrderReferenceNumber);
			if (index != null)
			{
				final BookQuoteCache bookQuoteCache = this.bookCaches[index.intValue()];
				final ExecutorService executor = this.executors[index.intValue()];
				executor.submit(new Runnable() {

					@Override
					public void run()
					{
						bookQuoteCache.replaceOrder(originalOrderReferenceNumber, newOrderReferenceNumber, (int) shares, price, timestamp);
					}
				});
				this.orderIdToIndex.remove(originalOrderReferenceNumber);
				this.orderIdToIndex.put(newOrderReferenceNumber, index);
			}
			else
			{
				LOGGER.severe("Unable to find orderId=" + originalOrderReferenceNumber);
			}
		}
		else if (messageType == TIMESTAMP_SECONDS)
		{
			long timestampSeconds = ByteBufferUtil.getUnsignedInt(buffer);
			this.secondsSinceMidnight = this.midnight + timestampSeconds * 1000;
		}
		else if (messageType == SYSTEM_EVENT)
		{
			ByteBufferUtil.advancePosition(buffer, 4);
			char eventCode = (char) buffer.get();
			switch (eventCode)
			{
				case 'O':
					LOGGER.info("Start of Messages");
					break;
				case 'S':
					LOGGER.info("Start of System Hours");
					break;
				case 'Q':
					LOGGER.info("Start of Market Hours");
					break;
				case 'M':
					LOGGER.info("End of Market Hours");
					break;
				case 'E':
					LOGGER.info("End of System Hours");
					break;
				case 'C':
					LOGGER.info("End of System Hours");
					nasdaqPacket.setEndOfTransmission(true);
					break;
				default:
					break;
			}
		}
		else if (messageType == NOII)
		{
			final long timestamp = getTimestamp(ByteBufferUtil.getUnsignedInt(buffer));
			final long pairedShares = buffer.getLong();
			final long imbalanceShares = buffer.getLong();
			final Side imbalanceSide = getImbalanceSide((char) buffer.get());
			final String symbol = ByteBufferUtil.getString(buffer, 8);
			final double farPrice = getPrice(ByteBufferUtil.getUnsignedInt(buffer));
			final double nearPrice = getPrice(ByteBufferUtil.getUnsignedInt(buffer));
			final double currentReferencePrice = getPrice(ByteBufferUtil.getUnsignedInt(buffer));
			final AuctionType auctionType = getAuctionType((char) buffer.get());
			ByteBufferUtil.advancePosition(buffer, 1); // price variation indicator

			final int index = getThreadIndex(symbol);
			final ImbalanceCache imbalanceCache = this.imbalanceCaches[index];
			final ExecutorService executor = this.executors[index];
			executor.submit(new Runnable() {

				@Override
				public void run()
				{
					imbalanceCache.updateImbalance(symbol, pairedShares, imbalanceShares, imbalanceSide, 0, currentReferencePrice, nearPrice, farPrice, 0,
							Exchange.USEQ_NASDAQ_OMX, auctionType, timestamp);
				}
			});
		}

		buffer.position(position + messageLength);
	}

	private static int getThreadIndex(String symbol)
	{
		return symbol.charAt(0) - 'A' / LETTER_DIVISOR;
	}

	private long getTimestamp(long nanos)
	{
		return this.secondsSinceMidnight + nanos / 1000000;
	}

	private static Side getImbalanceSide(char c)
	{
		switch (c)
		{
			case 'B':
				return Side.BUY;
			case 'S':
				return Side.SELL;
			default:
				return null;
		}
	}

	private static AuctionType getAuctionType(char c)
	{
		switch (c)
		{
			case 'O':
				return AuctionType.OPENING;
			case 'C':
				return AuctionType.CLOSING;
			case 'H':
				return AuctionType.OTHER;
			default:
				return null;
		}
	}

	private static double getPrice(long value)
	{
		return value / 1E4;
	}
}
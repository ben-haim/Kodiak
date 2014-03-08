package com.clearpool.kodiak.feedlibrary.core.psx;

import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.logging.Logger;

import com.clearpool.common.util.DateUtil;
import com.clearpool.kodiak.feedlibrary.caches.BookQuoteCache;
import com.clearpool.kodiak.feedlibrary.caches.IMdServiceCache;
import com.clearpool.kodiak.feedlibrary.caches.ImbalanceCache;
import com.clearpool.kodiak.feedlibrary.callbacks.IMdBookQuoteListner;
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

public class PsxNormalizer implements IMdNormalizer
{
	private static final Logger LOGGER = Logger.getLogger(PsxNormalizer.class.getName());

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

	private final BookQuoteCache bookCache;
	private final ImbalanceCache imbalanceCache;
	private final long midnight;

	private long secondsSinceMidnight;

	public PsxNormalizer(Map<MdServiceType, IMdLibraryCallback> callbacks, String range)
	{
		this.bookCache = new BookQuoteCache((IMdBookQuoteListner) callbacks.get(MdServiceType.BOOK_XPSX), MdFeed.PSX, MdServiceType.BOOK_XPSX, range);
		this.imbalanceCache = new ImbalanceCache((IMdImbalanceListener) callbacks.get(MdServiceType.IMBALANCE_XPSX), MdFeed.PSX, MdServiceType.IMBALANCE_XPSX, range);
		this.midnight = DateUtil.TODAY_MIDNIGHT_EST.getTime();

		this.secondsSinceMidnight = 0;
	}

	@Override
	public IMdServiceCache[] getMdServiceCaches()
	{
		return new IMdServiceCache[] { this.bookCache, this.imbalanceCache };
	}

	@Override
	public void processMessage(String processorName, MdFeedPacket packet, boolean shouldIgnore)
	{
		PsxPacket psxPacket = (PsxPacket) packet;
		ByteBuffer buffer = psxPacket.getBuffer();

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
			long timestamp = getTimestamp(ByteBufferUtil.getUnsignedInt(buffer));
			String orderReferenceNumber = String.valueOf(buffer.getLong());
			Side side = (buffer.get() == 'B') ? Side.BUY : Side.SELL;
			long shares = ByteBufferUtil.getUnsignedInt(buffer);
			String symbol = ByteBufferUtil.getString(buffer, 8).trim();
			double price = getPrice(ByteBufferUtil.getUnsignedInt(buffer));
			this.bookCache.addOrder(symbol, orderReferenceNumber, side, (int) shares, price, Exchange.USEQ_NASDAQ_OMX_PHLX.getMicCode(), timestamp);
		}
		else if (messageType == ORDER_EXECUTED)
		{
			long timestamp = getTimestamp(ByteBufferUtil.getUnsignedInt(buffer));
			String orderReferenceNumber = String.valueOf(buffer.getLong());
			long executedShares = ByteBufferUtil.getUnsignedInt(buffer);
			ByteBufferUtil.advancePosition(buffer, 8);
			this.bookCache.cancelOrder(orderReferenceNumber, (int) executedShares, timestamp);
		}
		else if (messageType == ORDER_EXECUTED_WITH_PRICE)
		{
			long timestamp = getTimestamp(ByteBufferUtil.getUnsignedInt(buffer));
			String orderReferenceNumber = String.valueOf(buffer.getLong());
			long executedShares = ByteBufferUtil.getUnsignedInt(buffer);
			ByteBufferUtil.advancePosition(buffer, 13);
			this.bookCache.cancelOrder(orderReferenceNumber, (int) executedShares, timestamp);
		}
		else if (messageType == ORDER_CANCEL)
		{
			long timestamp = getTimestamp(ByteBufferUtil.getUnsignedInt(buffer));
			String orderReferenceNumber = String.valueOf(buffer.getLong());
			long cancelledShares = ByteBufferUtil.getUnsignedInt(buffer);
			this.bookCache.cancelOrder(orderReferenceNumber, (int) cancelledShares, timestamp);
		}
		else if (messageType == ORDER_DELETE)
		{
			long timestamp = getTimestamp(ByteBufferUtil.getUnsignedInt(buffer));
			String orderReferenceNumber = String.valueOf(buffer.getLong());
			this.bookCache.cancelOrder(orderReferenceNumber, timestamp);
		}
		else if (messageType == ORDER_REPLACE)
		{
			long timestamp = getTimestamp(ByteBufferUtil.getUnsignedInt(buffer));
			String originalOrderReferenceNumber = String.valueOf(buffer.getLong());
			String newOrderReferenceNumber = String.valueOf(buffer.getLong());
			long shares = ByteBufferUtil.getUnsignedInt(buffer);
			double price = getPrice(ByteBufferUtil.getUnsignedInt(buffer));
			this.bookCache.replaceOrder(originalOrderReferenceNumber, newOrderReferenceNumber, (int) shares, price, timestamp);
		}
		else if (messageType == TIMESTAMP_SECONDS)
		{
			long timestampSeconds = ByteBufferUtil.getUnsignedInt(buffer);
			this.secondsSinceMidnight = this.midnight + timestampSeconds*1000;
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
					psxPacket.setEndOfTransmission(true);
					break;
				default:
					break;
			}
		}
		else if (messageType == NOII)
		{
			long timestamp = getTimestamp(ByteBufferUtil.getUnsignedInt(buffer));
			long pairedShares = buffer.getLong();
			long imbalanceShares = buffer.getLong();
			Side imbalanceSide = getImbalanceSide((char) buffer.get());
			String symbol = ByteBufferUtil.getString(buffer, 8).trim();
			double farPrice = getPrice(ByteBufferUtil.getUnsignedInt(buffer));
			double nearPrice = getPrice(ByteBufferUtil.getUnsignedInt(buffer));
			double currentReferencePrice = getPrice(ByteBufferUtil.getUnsignedInt(buffer));
			AuctionType auctionType = getAuctionType((char) buffer.get());
			ByteBufferUtil.advancePosition(buffer, 1); // price variation indicator
			this.imbalanceCache.updateImbalance(symbol, pairedShares, imbalanceShares, imbalanceSide, 0, currentReferencePrice, nearPrice, farPrice, 0, Exchange.USEQ_NASDAQ_OMX_PHLX, auctionType, timestamp);
		}

		buffer.position(position + messageLength);
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
		BigDecimal numerator = new BigDecimal(value);
		BigDecimal denominator = new BigDecimal(10000);
		return numerator.divide(denominator).doubleValue();
	}
}
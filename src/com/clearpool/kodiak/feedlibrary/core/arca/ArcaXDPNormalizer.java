package com.clearpool.kodiak.feedlibrary.core.arca;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Vector;
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

public class ArcaXDPNormalizer implements IMdNormalizer
{
	private static final Logger LOGGER = Logger.getLogger(ArcaXDPNormalizer.class.getName());

	public static final char SIDE_BUY = 'B';
	public static final char SIDE_SELL = 'S';

	private final BookQuoteCache bookCache;
	private final ImbalanceCache imbalanceCache;
	private final Vector<ArcaSymbolRef> symbolVec;
	private final long midnight;

	private long secondsSinceMidnight;

	public ArcaXDPNormalizer(Map<MdServiceType, IMdLibraryCallback> callbacks, String range)
	{
		this.bookCache = new BookQuoteCache((IMdBookQuoteListner) callbacks.get(MdServiceType.BOOK_ARCX), MdFeed.ARCA, MdServiceType.BOOK_ARCX, range);
		this.imbalanceCache = new ImbalanceCache((IMdImbalanceListener) callbacks.get(MdServiceType.IMBALANCE_ARCX), MdFeed.ARCA, MdServiceType.IMBALANCE_ARCX, range);
		this.symbolVec = new Vector<ArcaSymbolRef>();
		this.midnight = DateUtil.TODAY_MIDNIGHT_EST.getTime();

		this.secondsSinceMidnight = 0;
	}

	@Override
	public void processMessage(String processorName, MdFeedPacket packet, boolean shouldIgnore)
	{
		ArcaPacket arcaPacket = (ArcaPacket) packet;
		ByteBuffer buffer = arcaPacket.getBuffer();

		int messageLength = ByteBufferUtil.getUnsignedShort(buffer);
		if (shouldIgnore)
		{
			ByteBufferUtil.advancePosition(buffer, messageLength);
			return;
		}

		int messageType = ByteBufferUtil.getUnsignedShort(buffer);
		switch (messageType)
		{
			case 100:
				processMsgType100(buffer);
				break;
			case 101:
				processMsgType101(buffer);
				break;
			case 102:
				processMsgType102(buffer);
				break;
			case 103:
				processMsgType103(buffer);
				break;
			case 105:
			case 106:
			case 1: // Sequence Number Reset
				LOGGER.info("Sequence Number Reset");
				break;
			case 2: // SourceTime Reference Message
				processMsgType2(buffer);
				break;
			case 3: // Symbol Index Map Message
				processMsgType3(buffer);
				break;
			default:
		}
	}

	public void processMsgType2(ByteBuffer bufferAfterMsgType)
	{
		ByteBufferUtil.advancePosition(bufferAfterMsgType, 8); // Skip SymbolIndex and SymbolSeqNum
		long timeReference = ByteBufferUtil.getUnsignedInt(bufferAfterMsgType);
		this.secondsSinceMidnight = timeReference * 1000 - this.midnight;
	}

	public void processMsgType3(ByteBuffer bufferAfterMsgType)
	{
		long symbolIndex = ByteBufferUtil.getUnsignedInt(bufferAfterMsgType);
		String symbol = ByteBufferUtil.getString(bufferAfterMsgType, 11);
		ByteBufferUtil.advancePosition(bufferAfterMsgType, 5);
		double priceDivider = Math.pow(10, ByteBufferUtil.getUnsignedByte(bufferAfterMsgType));
		if (symbolIndex >= this.symbolVec.size())
		{
			this.symbolVec.ensureCapacity((int) (symbolIndex + 1));
		}
		ArcaSymbolRef arcaSymbolRef = new ArcaSymbolRef();
		arcaSymbolRef.setSymbol(symbol);
		arcaSymbolRef.setPriceDivider(priceDivider);
		this.symbolVec.set((int) symbolIndex, arcaSymbolRef);
	}

	public void processMsgType100(ByteBuffer bufferAfterMsgType)
	{
		long sourceTimeNs = ByteBufferUtil.getUnsignedInt(bufferAfterMsgType);
		long symbolIndex = ByteBufferUtil.getUnsignedInt(bufferAfterMsgType);
		if (symbolIndex < this.symbolVec.size())
		{
			ArcaSymbolRef arcaSymbolRef = this.symbolVec.get((int) symbolIndex);
			if (arcaSymbolRef != null)
			{
				String symbol = arcaSymbolRef.getSymbol();
				ByteBufferUtil.advancePosition(bufferAfterMsgType, 4); // Skip SymbolSeqNum
				String orderId = String.valueOf(ByteBufferUtil.getUnsignedInt(bufferAfterMsgType));
				double price = ByteBufferUtil.getUnsignedInt(bufferAfterMsgType) / arcaSymbolRef.getPriceDivider();
				long volume = ByteBufferUtil.getUnsignedInt(bufferAfterMsgType);
				Side side = (char) bufferAfterMsgType.get() == SIDE_BUY ? Side.BUY : Side.SELL;
				long timeStamp = this.secondsSinceMidnight * 1000000000 + sourceTimeNs;
				this.bookCache.addOrder(symbol, orderId, side, (int) volume, price, Exchange.USEQ_NYSE_ARCA_EXCHANGE.getMicCode(), timeStamp);
			}
		}
	}

	public void processMsgType101(ByteBuffer bufferAfterMsgType)
	{
		long sourceTimeNs = ByteBufferUtil.getUnsignedInt(bufferAfterMsgType);
		long symbolIndex = ByteBufferUtil.getUnsignedInt(bufferAfterMsgType);
		if (symbolIndex < this.symbolVec.size())
		{
			ArcaSymbolRef arcaSymbolRef = this.symbolVec.get((int) symbolIndex);
			if (arcaSymbolRef != null)
			{
				ByteBufferUtil.advancePosition(bufferAfterMsgType, 4); // Skip SymbolSeqNum
				String orderId = String.valueOf(ByteBufferUtil.getUnsignedInt(bufferAfterMsgType));
				double price = ByteBufferUtil.getUnsignedInt(bufferAfterMsgType) / arcaSymbolRef.getPriceDivider();
				long volume = ByteBufferUtil.getUnsignedInt(bufferAfterMsgType);
				long timeStamp = this.secondsSinceMidnight * 1000000000 + sourceTimeNs;
				this.bookCache.replaceOrder(orderId, orderId, (int) volume, price, timeStamp);
			}
		}
	}

	public void processMsgType102(ByteBuffer bufferAfterMsgType)
	{
		long sourceTimeNs = ByteBufferUtil.getUnsignedInt(bufferAfterMsgType);
		ByteBufferUtil.advancePosition(bufferAfterMsgType, 8); // Skip SymbolIndex, SymbolSeqNum
		String orderId = String.valueOf(ByteBufferUtil.getUnsignedInt(bufferAfterMsgType));
		this.bookCache.cancelOrder(orderId, this.secondsSinceMidnight * 1000000000 + sourceTimeNs);
	}

	public void processMsgType103(ByteBuffer bufferAfterMsgType)
	{
		long sourceTimeNs = ByteBufferUtil.getUnsignedInt(bufferAfterMsgType);
		ByteBufferUtil.advancePosition(bufferAfterMsgType, 8); // Skip SymbolIndex, SymbolSeqNum
		String orderId = String.valueOf(ByteBufferUtil.getUnsignedInt(bufferAfterMsgType));
		ByteBufferUtil.advancePosition(bufferAfterMsgType, 4); // Skip Price
		long volume = ByteBufferUtil.getUnsignedInt(bufferAfterMsgType);
		this.bookCache.cancelOrder(orderId, (int) volume, this.secondsSinceMidnight * 1000000000 + sourceTimeNs);
	}

	public void processMsgType105(ByteBuffer bufferAfterMsgType)
	{
		long sourceTimeNs = ByteBufferUtil.getUnsignedInt(bufferAfterMsgType);
		long symbolIndex = ByteBufferUtil.getUnsignedInt(bufferAfterMsgType);
		if (symbolIndex < this.symbolVec.size())
		{
			ArcaSymbolRef arcaSymbolRef = this.symbolVec.get((int) symbolIndex);
			if (arcaSymbolRef != null)
			{
				ByteBufferUtil.advancePosition(bufferAfterMsgType, 4); // Skip SymbolSeqNum
				double referencePrice = ByteBufferUtil.getUnsignedInt(bufferAfterMsgType);
				long pairedQty = ByteBufferUtil.getUnsignedInt(bufferAfterMsgType);
				long totalImbalanceQty = ByteBufferUtil.getUnsignedInt(bufferAfterMsgType);
				long marketImbalanceQty = ByteBufferUtil.getUnsignedInt(bufferAfterMsgType);
				ByteBufferUtil.advancePosition(bufferAfterMsgType, 2); // Skip AuctionTime
				char auctionTypeChar = (char) bufferAfterMsgType.get();
				AuctionType auctionType = AuctionType.OTHER;
				if (auctionTypeChar == 'O') auctionType = AuctionType.OPENING;
				else if (auctionTypeChar == 'C') auctionType = AuctionType.CLOSING;
				Side side = (char) bufferAfterMsgType.get() == SIDE_BUY ? Side.BUY : Side.SELL;
				double continuousBookClearingPrice = ByteBufferUtil.getUnsignedInt(bufferAfterMsgType);
				double closingOnlyClearingPrice = ByteBufferUtil.getUnsignedInt(bufferAfterMsgType);
				double sSRFilingPrice = ByteBufferUtil.getUnsignedInt(bufferAfterMsgType);
				this.imbalanceCache.updateImbalance(arcaSymbolRef.getSymbol(), pairedQty, totalImbalanceQty, side, marketImbalanceQty, referencePrice, continuousBookClearingPrice,
						closingOnlyClearingPrice, sSRFilingPrice, Exchange.USEQ_NYSE_ARCA_EXCHANGE, auctionType, this.secondsSinceMidnight * 1000000000 + sourceTimeNs);
			}
		}
	}

	public void processMsgType106(ByteBuffer bufferAfterMsgType)
	{
		long sourceTimeNs = ByteBufferUtil.getUnsignedInt(bufferAfterMsgType);
		long symbolIndex = ByteBufferUtil.getUnsignedInt(bufferAfterMsgType);
		if (symbolIndex < this.symbolVec.size())
		{
			ArcaSymbolRef arcaSymbolRef = this.symbolVec.get((int) symbolIndex);
			if (arcaSymbolRef != null)
			{
				String symbol = arcaSymbolRef.getSymbol();
				ByteBufferUtil.advancePosition(bufferAfterMsgType, 4); // Skip SymbolSeqNum
				String orderId = String.valueOf(ByteBufferUtil.getUnsignedInt(bufferAfterMsgType));
				double price = ByteBufferUtil.getUnsignedInt(bufferAfterMsgType) / arcaSymbolRef.getPriceDivider();
				long volume = ByteBufferUtil.getUnsignedInt(bufferAfterMsgType);
				Side side = (char) bufferAfterMsgType.get() == SIDE_BUY ? Side.BUY : Side.SELL;
				this.bookCache.addOrder(symbol, orderId, side, (int) volume, price, Exchange.USEQ_NYSE_ARCA_EXCHANGE.getMicCode(), this.secondsSinceMidnight * 1000000000
						+ sourceTimeNs);
			}
		}
	}

	@Override
	public IMdServiceCache[] getMdServiceCaches()
	{
		return new IMdServiceCache[] { this.bookCache, this.imbalanceCache };
	}

}

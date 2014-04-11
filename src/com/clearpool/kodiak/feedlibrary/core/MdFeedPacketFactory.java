package com.clearpool.kodiak.feedlibrary.core;

import com.clearpool.kodiak.feedlibrary.core.arca.ArcaPacket;
import com.clearpool.kodiak.feedlibrary.core.cta.CtaPacket;
import com.clearpool.kodiak.feedlibrary.core.nasdaq.NasdaqPacket;
import com.clearpool.kodiak.feedlibrary.core.opra.OpraPacket;
import com.clearpool.kodiak.feedlibrary.core.psx.PsxPacket;
import com.clearpool.kodiak.feedlibrary.core.bx.BxPacket;
import com.clearpool.kodiak.feedlibrary.core.utp.UtpPacket;

public class MdFeedPacketFactory
{
	public static MdFeedPacket createPacket(MdFeed feed, long selectionTimeNanos)
	{
		switch (feed)
		{
			case OPRA:
				return new OpraPacket(selectionTimeNanos);
			case CQS:
			case CTS:
				return new CtaPacket(selectionTimeNanos);
			case UQDF:
			case UTDF:
				return new UtpPacket(selectionTimeNanos);
			case NASDAQ:
				return new NasdaqPacket(selectionTimeNanos);
			case BX:
				return new BxPacket(selectionTimeNanos);
			case PSX:
				return new PsxPacket(selectionTimeNanos);
			case ARCA:
				return new ArcaPacket(selectionTimeNanos);
			default:
				return null;
		}
	}
}
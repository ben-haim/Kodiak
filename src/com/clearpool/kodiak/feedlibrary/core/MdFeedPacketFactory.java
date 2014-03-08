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
	public static MdFeedPacket createPacket(MdFeed feed)
	{
		switch (feed)
		{
			case OPRA:
				return new OpraPacket();
			case CQS:
				return new CtaPacket();
			case CTS:
				return new CtaPacket();
			case UQDF:
				return new UtpPacket();
			case UTDF:
				return new UtpPacket();
			case NASDAQ:
				return new NasdaqPacket();
			case BX:
				return new BxPacket();
			case PSX:
				return new PsxPacket();
			case ARCA:
				return new ArcaPacket();
			default:
				return null;
		}
	}
}
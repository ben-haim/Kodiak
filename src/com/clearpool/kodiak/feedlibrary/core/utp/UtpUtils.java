package com.clearpool.kodiak.feedlibrary.core.utp;

import java.math.BigDecimal;

import com.clearpool.messageobjects.marketdata.Exchange;

public class UtpUtils
{
	private static final int[] POWERS = new int[] { 100, 1000, 10000 };
	private static final Exchange[] EXCHANGES = new Exchange[26];

	static
	{
		EXCHANGES['A' - 'A'] = Exchange.USEQ_NYSE_MKT;
		EXCHANGES['B' - 'A'] = Exchange.USEQ_NASDAQ_OMX_BX;
		EXCHANGES['C' - 'A'] = Exchange.USEQ_NATIONAL_STOCK_EXCHANGE;
		EXCHANGES['D' - 'A'] = Exchange.USEQ_FINRA_ADF;
		EXCHANGES['E' - 'A'] = Exchange.USEQ_SIP;
		EXCHANGES['I' - 'A'] = Exchange.USEQ_INTERNATIONAL_SECURITIES_EXCHANGE;
		EXCHANGES['J' - 'A'] = Exchange.USEQ_EDGA_EXCHANGE;
		EXCHANGES['K' - 'A'] = Exchange.USEQ_EDGX_EXCHANGE;
		EXCHANGES['M' - 'A'] = Exchange.USEQ_CHICAGO_STOCK_EXCHANGE;
		EXCHANGES['N' - 'A'] = Exchange.USEQ_NYSE_EURONEXT;
		EXCHANGES['P' - 'A'] = Exchange.USEQ_NYSE_ARCA_EXCHANGE;
		EXCHANGES['Q' - 'A'] = Exchange.USEQ_NASDAQ_OMX;
		EXCHANGES['W' - 'A'] = Exchange.USEQ_CHICAGO_BOARD_OPTIONS_EXCHANGE;
		EXCHANGES['X' - 'A'] = Exchange.USEQ_NASDAQ_OMX_PHLX;
		EXCHANGES['Y' - 'A'] = Exchange.USEQ_BATS_Y_EXCHANGE;
		EXCHANGES['Z' - 'A'] = Exchange.USEQ_BATS_EXCHANGE;
	}

	public static double getPrice(long value, char denominatorCode)
	{
		return new BigDecimal(value).divide(new BigDecimal(POWERS[denominatorCode - 'B'])).doubleValue();
	}

	public static Exchange getExchange(char participantId)
	{
		int index = participantId - 'A';
		if (index < 0) return null;
		return EXCHANGES[index];
	}

	public static Exchange getExchange(char participantId, @SuppressWarnings("unused") String finraParticipantId)
	{
		Exchange exchange = getExchange(participantId);
		if (exchange == Exchange.USEQ_FINRA_ADF) return Exchange.USEQ_FINRA_LAVAFLOW; // hacky .. will be fixed with JIRA CS-452
		return exchange;
	}
}

package com.clearpool.kodiak.feedlibrary.mbeans.format;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import com.clearpool.messageobjects.marketdata.BookQuote;
import com.clearpool.messageobjects.marketdata.Imbalance;
import com.clearpool.messageobjects.marketdata.MarketState;
import com.clearpool.messageobjects.marketdata.Quote;
import com.clearpool.messageobjects.marketdata.Sale;

public class MdEntityHtmlFormatter
{
	private static final String HEADER_COLOR = "CCE5FF";
	private static final String EVEN_COLOR = "FFFFFF";
	private static final String ODD_COLOR = "E0E0E0";

	public static String formatNbbo(Quote quote)
	{
		StringBuilder builder = new StringBuilder();
		builder.append("<table border=\"1\">");

		builder.append("<tr bgcolor=\"#" + HEADER_COLOR + "\">>");
		builder.append("<td><b>Symbol</b></td>");
		builder.append("<td><b>SymbolSeqNo</b></td>");
		builder.append("<td><b>Timestamp</b></td>");
		builder.append("<td><b>Bid Price</b></td>");
		builder.append("<td><b>Bid Size</b></td>");
		builder.append("<td><b>Bid Exchange</b></td>");
		builder.append("<td><b>Ask Price</b></td>");
		builder.append("<td><b>Ask Size</b></td>");
		builder.append("<td><b>Ask Exchange</b></td>");
		builder.append("</tr>");

		builder.append("<tr bgcolor=\"#" + EVEN_COLOR + "\">");
		builder.append("<td>").append(quote.getSymbol()).append("</td>");
		builder.append("<td>").append(quote.getSymbolSequenceNumber()).append("</td>");
		builder.append("<td>").append(new Date(quote.getTimestamp())).append("</td>");
		builder.append("<td>").append(quote.getBidPrice()).append("</td>");
		builder.append("<td>").append(quote.getBidSize()).append("</td>");
		builder.append("<td>").append(quote.getBidExchange()).append("</td>");
		builder.append("<td>").append(quote.getAskPrice()).append("</td>");
		builder.append("<td>").append(quote.getAskSize()).append("</td>");
		builder.append("<td>").append(quote.getAskExchange()).append("</td>");
		builder.append("</tr>");

		builder.append("</table>");
		return builder.toString();
	}

	public static String formatSale(Sale sale)
	{
		StringBuilder builder = new StringBuilder();
		builder.append("<table border=\"1\">");

		builder.append("<tr bgcolor=\"#" + HEADER_COLOR + "\">>");
		builder.append("<td><b>Symbol</b></td>");
		builder.append("<td><b>SymbolSeqNo</b></td>");
		builder.append("<td><b>Timestamp</b></td>");
		builder.append("<td><b>Price</b></td>");
		builder.append("<td><b>Size</b></td>");
		builder.append("<td><b>Exchange</b></td>");
		builder.append("<td><b>Condition Code</b></td>");
		builder.append("<td><b>Condition</b></td>");
		builder.append("<td><b>Volume</b></td>");
		builder.append("<td><b>Vwap</b></td>");
		builder.append("<td><b>Vwap Volume</b></td>");
		builder.append("<td><b>Open</b></td>");
		builder.append("<td><b>High</b></td>");
		builder.append("<td><b>Low</b></td>");
		builder.append("<td><b>Latest Close</b></td>");
		builder.append("<td><b>Open Interest</b></td>");
		builder.append("</tr>");

		builder.append("<tr bgcolor=\"#" + EVEN_COLOR + "\">");
		builder.append("<td>").append(sale.getSymbol()).append("</td>");
		builder.append("<td>").append(sale.getSymbolSequenceNumber()).append("</td>");
		builder.append("<td>").append(new Date(sale.getTimestamp())).append("</td>");
		builder.append("<td>").append(sale.getPrice()).append("</td>");
		builder.append("<td>").append(sale.getSize()).append("</td>");
		builder.append("<td>").append(sale.getExchange()).append("</td>");
		builder.append("<td>").append(sale.getConditionCode()).append("</td>");
		builder.append("<td>").append(sale.getCondition()).append("</td>");
		builder.append("<td>").append(sale.getVolume()).append("</td>");
		builder.append("<td>").append(sale.getVwap()).append("</td>");
		builder.append("<td>").append(sale.getVwapVolume()).append("</td>");
		builder.append("<td>").append(sale.getOpenPrice()).append("</td>");
		builder.append("<td>").append(sale.getHighPrice()).append("</td>");
		builder.append("<td>").append(sale.getLowPrice()).append("</td>");
		builder.append("<td>").append(sale.getLatestClosePrice()).append("</td>");
		builder.append("<td>").append(sale.getOpenInterest()).append("</td>");
		builder.append("</tr>");

		builder.append("</table>");
		return builder.toString();
	}

	public static String formatBbos(Collection<Quote> quotes)
	{
		ArrayList<Quote> bids = new ArrayList<Quote>();
		ArrayList<Quote> asks = new ArrayList<Quote>();
		bids.addAll(quotes);
		asks.addAll(quotes);

		Collections.sort(bids, new Comparator<Quote>() {

			@Override
			public int compare(Quote quote1, Quote quote2)
			{
				double priceDiff = quote1.getBidPrice() - quote2.getBidPrice();
				if (priceDiff > 0)
					return -1;
				else if (priceDiff < 0)
					return 1;
				else
				{
					int sizeDiff = quote1.getBidSize() - quote2.getBidSize();
					if (sizeDiff > 0)
						return -1;
					else if (sizeDiff < 0)
						return 1;
					else
					{
						long timeDiff = quote1.getTimestamp() - quote2.getTimestamp();
						if (timeDiff > 0)
							return 1;
						else if (timeDiff < 0)
							return -1;
						else
							return 0;
					}
				}
			}
		});

		Collections.sort(asks, new Comparator<Quote>() {

			@Override
			public int compare(Quote quote1, Quote quote2)
			{
				if (quote1.getAskPrice() == 0)
					return 1;
				else if (quote2.getAskPrice() == 0)
					return -1;
				else
				{
					double priceDiff = quote1.getAskPrice() - quote2.getAskPrice();
					if (priceDiff > 0)
						return 1;
					else if (priceDiff < 0)
						return -1;
					else
					{
						int sizeDiff = quote1.getAskSize() - quote2.getAskSize();
						if (sizeDiff > 0)
							return -1;
						else if (sizeDiff < 0)
							return 1;
						else
						{
							long timeDiff = quote1.getTimestamp() - quote2.getTimestamp();
							if (timeDiff > 0)
								return 1;
							else if (timeDiff < 0)
								return -1;
							else
								return 0;
						}
					}
				}
			}
		});

		StringBuilder builder = new StringBuilder();
		builder.append("<table border=\"1\">");
		builder.append("<tr bgcolor=\"#" + HEADER_COLOR + "\">>");
		builder.append("<td><b>Symbol</b></td>");
		builder.append("<td><b>Bid Timestamp</b></td>");
		builder.append("<td><b>Bid Exchange</b></td>");
		builder.append("<td><b>Bid Price</b></td>");
		builder.append("<td><b>Bid Size</b></td>");
		builder.append("<td><b>Ask Timestamp</b></td>");
		builder.append("<td><b>Ask Exchange</b></td>");
		builder.append("<td><b>Ask Price</b></td>");
		builder.append("<td><b>Ask Size</b></td>");
		builder.append("</tr>");

		for (int i = 0; i < quotes.size(); i++)
		{
			Quote bidQuote = bids.get(i);
			Quote askQuote = asks.get(i);

			String color = (i % 2 == 0) ? "\"#" + EVEN_COLOR + "\"" : "\"#" + ODD_COLOR + "\"";
			builder.append("<tr bgcolor=" + color + ">");
			builder.append("<td>").append(bidQuote.getSymbol()).append("</td>");
			builder.append("<td>").append(new Date(bidQuote.getTimestamp())).append("</td>");
			builder.append("<td>").append(bidQuote.getBidExchange()).append("</td>");
			builder.append("<td>").append(bidQuote.getBidPrice()).append("</td>");
			builder.append("<td>").append(bidQuote.getBidSize()).append("</td>");
			builder.append("<td>").append(new Date(askQuote.getTimestamp())).append("</td>");
			builder.append("<td>").append(askQuote.getAskExchange()).append("</td>");
			builder.append("<td>").append(askQuote.getAskPrice()).append("</td>");
			builder.append("<td>").append(askQuote.getAskSize()).append("</td>");
			builder.append("</tr>");
		}

		builder.append("</table>");
		return builder.toString();
	}

	public static String formatState(MarketState state)
	{
		StringBuilder builder = new StringBuilder();
		builder.append("<table border=\"1\">");

		builder.append("<tr bgcolor=\"#" + HEADER_COLOR + "\">>");
		builder.append("<td><b>Symbol</b></td>");
		builder.append("<td><b>SymbolSeqNo</b></td>");
		builder.append("<td><b>Timestamp</b></td>");
		builder.append("<td><b>Market Session</b></td>");
		builder.append("<td><b>Trading State</b></td>");
		builder.append("<td><b>Lower Band</b></td>");
		builder.append("<td><b>Upper Band</b></td>");
		builder.append("<td><b>Condition Code</b></td>");
		builder.append("</tr>");

		builder.append("<tr bgcolor=\"#" + EVEN_COLOR + "\">");
		builder.append("<td>").append(state.getSymbol()).append("</td>");
		builder.append("<td>").append(state.getSymbolSequenceNumber()).append("</td>");
		builder.append("<td>").append(new Date(state.getTimestamp())).append("</td>");
		builder.append("<td>").append(state.getMarketSession()).append("</td>");
		builder.append("<td>").append(state.getTradingState()).append("</td>");
		builder.append("<td>").append(state.getLowerBand()).append("</td>");
		builder.append("<td>").append(state.getUpperBand()).append("</td>");
		builder.append("<td>").append(state.getConditionCode()).append("</td>");
		builder.append("</tr>");

		builder.append("</table>");
		return builder.toString();
	}

	public static String formatBookQuote(List<BookQuote> bookQuoteBids, List<BookQuote> bookQuoteAsks)
	{
		StringBuilder builder = new StringBuilder();
		builder.append("<table border=\"1\">");
		builder.append("<tr bgcolor=\"#" + HEADER_COLOR + "\">>");
		builder.append("<td><b>Symbol</b></td>");
		builder.append("<td><b>Bid Timestamp</b></td>");
		builder.append("<td><b>Bid Display Name</b></td>");
		builder.append("<td><b>Bid Price</b></td>");
		builder.append("<td><b>Bid Size</b></td>");
		builder.append("<td><b># ORD</b></td>");
		builder.append("<td><b>Ask Timestamp</b></td>");
		builder.append("<td><b>Ask Display Name</b></td>");
		builder.append("<td><b>Ask Price</b></td>");
		builder.append("<td><b>Ask Size</b></td>");
		builder.append("<td><b># ORD</b></td>");
		builder.append("</tr>");

		String symbol = null;
		if (bookQuoteBids.size() > 0)
		{
			symbol = bookQuoteBids.get(0).getSymbol();
		}
		else if (bookQuoteAsks.size() > 0)
		{
			symbol = bookQuoteAsks.get(0).getSymbol();
		}

		int length = Math.max(bookQuoteBids.size(), bookQuoteAsks.size());
		for (int i = 0; i < length; i++)
		{
			String color = (i % 2 == 0) ? "\"#" + EVEN_COLOR + "\"" : "\"#" + ODD_COLOR + "\"";
			builder.append("<tr bgcolor=" + color + ">");
			builder.append("<td>").append(symbol).append("</td>");
			if (i < bookQuoteBids.size())
			{
				BookQuote bid = bookQuoteBids.get(i);
				builder.append("<td>").append(new Date(bid.getTimestamp())).append("</td>");
				builder.append("<td>").append(bid.getDisplayName()).append("</td>");
				builder.append("<td>").append(bid.getPrice()).append("</td>");
				builder.append("<td>").append(bid.getSize()).append("</td>");
				builder.append("<td>").append(bid.getNumberOfOrders()).append("</td>");
			}
			else
			{
				builder.append("<td></td>");
				builder.append("<td></td>");
				builder.append("<td></td>");
				builder.append("<td></td>");
				builder.append("<td></td>");
			}

			if (i < bookQuoteAsks.size())
			{
				BookQuote ask = bookQuoteAsks.get(i);
				builder.append("<td>").append(new Date(ask.getTimestamp())).append("</td>");
				builder.append("<td>").append(ask.getDisplayName()).append("</td>");
				builder.append("<td>").append(ask.getPrice()).append("</td>");
				builder.append("<td>").append(ask.getSize()).append("</td>");
				builder.append("<td>").append(ask.getNumberOfOrders()).append("</td>");
			}
			else
			{
				builder.append("<td></td>");
				builder.append("<td></td>");
				builder.append("<td></td>");
				builder.append("<td></td>");
				builder.append("<td></td>");
			}
			builder.append("</tr>");
		}
		builder.append("</table>");
		return builder.toString();
	}

	public static String formatImbalance(Imbalance imbalance)
	{
		StringBuilder builder = new StringBuilder();
		builder.append("<table border=\"1\">");

		builder.append("<tr bgcolor=\"#" + HEADER_COLOR + "\">>");
		builder.append("<td><b>Symbol</b></td>");
		builder.append("<td><b>SymbolSeqNo</b></td>");
		builder.append("<td><b>Timestamp</b></td>");
		builder.append("<td><b>Initial Timestamp</b></td>");
		builder.append("<td><b>Initial Imbalance</b></td>");
		builder.append("<td><b>Paired Volume</b></td>");
		builder.append("<td><b>Imbalance Volume</b></td>");
		builder.append("<td><b>Imbalance Side</b></td>");
		builder.append("<td><b>MO Imbalance Volume</b></td>");
		builder.append("<td><b>Reference Price</b></td>");
		builder.append("<td><b>Clearing Price</b></td>");
		builder.append("<td><b>AO Clearing Price</b></td>");
		builder.append("<td><b>SSR Filling Price</b></td>");
		builder.append("<td><b>Exchange</b></td>");
		builder.append("<td><b>Auction Type</b></td>");
		builder.append("<td><b>Condition Code</b></td>");
		builder.append("</tr>");

		builder.append("<tr bgcolor=\"#" + EVEN_COLOR + "\">");
		builder.append("<td>").append(imbalance.getSymbol()).append("</td>");
		builder.append("<td>").append(imbalance.getSymbolSequenceNumber()).append("</td>");
		builder.append("<td>").append(new Date(imbalance.getTimestamp())).append("</td>");
		builder.append("<td>").append(new Date(imbalance.getInitialTimestamp())).append("</td>");
		builder.append("<td>").append(imbalance.getInitialImbalance()).append("</td>");
		builder.append("<td>").append(imbalance.getPairedVolume()).append("</td>");
		builder.append("<td>").append(imbalance.getImbalanceVolume()).append("</td>");
		builder.append("<td>").append(imbalance.getImbalanceSide()).append("</td>");
		builder.append("<td>").append(imbalance.getMarketOrderOnlyImbalance()).append("</td>");
		builder.append("<td>").append(imbalance.getReferencePrice()).append("</td>");
		builder.append("<td>").append(imbalance.getClearingPrice()).append("</td>");
		builder.append("<td>").append(imbalance.getAuctionOnlyClearingPrice()).append("</td>");
		builder.append("<td>").append(imbalance.getSsrOnlyFillingPrice()).append("</td>");
		builder.append("<td>").append(imbalance.getExchange()).append("</td>");
		builder.append("<td>").append(imbalance.getAuctionType()).append("</td>");
		builder.append("<td>").append(imbalance.getConditionCode()).append("</td>");
		builder.append("</tr>");

		builder.append("</table>");
		return builder.toString();
	}
}

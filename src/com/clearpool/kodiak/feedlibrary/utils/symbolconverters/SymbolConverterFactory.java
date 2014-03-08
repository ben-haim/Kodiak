package com.clearpool.kodiak.feedlibrary.utils.symbolconverters;

import com.clearpool.common.symbology.CqsToCmsConverter;
import com.clearpool.common.symbology.DefaultSymbolConverter;
import com.clearpool.common.symbology.ISymbolConverter;
import com.clearpool.kodiak.feedlibrary.core.MdFeed;

public class SymbolConverterFactory
{
	private static final CqsToCmsConverter CQS_CONVERTER = new CqsToCmsConverter();
	private static final DefaultSymbolConverter DEFAULT_CONVERTER = new DefaultSymbolConverter();

	public static ISymbolConverter getConverterInstance(MdFeed feedType)
	{
		if (feedType == null) return null;

		switch (feedType)
		{
			case CQS:
			case CTS:
				return CQS_CONVERTER;

			default:
				return DEFAULT_CONVERTER;
		}
	}

}

package com.clearpool.kodiak.feedlibrary.utils.symbolconverters.cta;

import org.junit.Assert;
import org.junit.Test;

import com.clearpool.common.symbology.CqsToCmsConverter;
import com.clearpool.kodiak.feedlibrary.core.MdFeed;
import com.clearpool.kodiak.feedlibrary.utils.symbolconverters.SymbolConverterFactory;

@SuppressWarnings("static-method")
public class SymbolConverterFactoryTest
{

	@Test
	public void testCorrectConverterCreation()
	{
		Assert.assertTrue(SymbolConverterFactory.getConverterInstance(MdFeed.CQS) instanceof CqsToCmsConverter);
		Assert.assertEquals("AAPL", SymbolConverterFactory.getConverterInstance(MdFeed.OPRA).convert("AAPL"));
		Assert.assertEquals("AAPL", SymbolConverterFactory.getConverterInstance(MdFeed.UQDF).convert("AAPL"));
	}

	@Test
	public void testCqsPreferredSymbolConversion()
	{
		Assert.assertEquals("AA PR", SymbolConverterFactory.getConverterInstance(MdFeed.CQS).convert("AAp"));
	}

	@Test
	public void testCqsPreferredClassASymbolConversion()
	{
		Assert.assertEquals("AMP PRA", SymbolConverterFactory.getConverterInstance(MdFeed.CQS).convert("AMPpA"));
	}

	@Test
	public void testCqsPreferredClassBSymbolConversion()
	{
		Assert.assertEquals("AMP PRB", SymbolConverterFactory.getConverterInstance(MdFeed.CQS).convert("AMPpB"));
	}

	@Test
	public void testCqsClassASymbolConversion()
	{
		Assert.assertEquals("AGM A", SymbolConverterFactory.getConverterInstance(MdFeed.CQS).convert("AGM/A"));
	}

	@Test
	public void testCqsClassBSymbolConversion()
	{
		Assert.assertEquals("AGM B", SymbolConverterFactory.getConverterInstance(MdFeed.CQS).convert("AGM/B"));
	}

	@Test
	public void testCqsPreferredWhenDistributedSymbolConversion()
	{
		Assert.assertEquals("AGM PRWD", SymbolConverterFactory.getConverterInstance(MdFeed.CQS).convert("AGMp/WD"));
	}

	@Test
	public void testCqsWhenDistributedSymbolConversion()
	{
		Assert.assertEquals("AGM WD", SymbolConverterFactory.getConverterInstance(MdFeed.CQS).convert("AGM/WD"));
	}

	@Test
	public void testCqsWarrantsSymbolConversion()
	{
		Assert.assertEquals("AGM WS", SymbolConverterFactory.getConverterInstance(MdFeed.CQS).convert("AGM/WS"));
	}

	@Test
	public void testCqsWarrantsClassASymbolConversion()
	{
		Assert.assertEquals("AGM WSA", SymbolConverterFactory.getConverterInstance(MdFeed.CQS).convert("AGM/WS/A"));
	}

	@Test
	public void testCqsWarrantsClassBSymbolConversion()
	{
		Assert.assertEquals("AGM WSB", SymbolConverterFactory.getConverterInstance(MdFeed.CQS).convert("AGM/WS/B"));
	}

	@Test
	public void testCqsCalledSymbolConversion()
	{
		Assert.assertEquals("AGM CL", SymbolConverterFactory.getConverterInstance(MdFeed.CQS).convert("AGM/CL"));
	}

	@Test
	public void testCqsClassACalledSymbolConversion()
	{
		Assert.assertEquals("AGM ACL", SymbolConverterFactory.getConverterInstance(MdFeed.CQS).convert("AGM/A/CL"));
	}

	@Test
	public void testCqsPreferredCalledSymbolConversion()
	{
		Assert.assertEquals("AGM PRCL", SymbolConverterFactory.getConverterInstance(MdFeed.CQS).convert("AGMp/CL"));
	}

	@Test
	public void testCqsPreferredACalledSymbolConversion()
	{
		Assert.assertEquals("AGM PRACL", SymbolConverterFactory.getConverterInstance(MdFeed.CQS).convert("AGMpA/CL"));
	}

	@Test
	public void testCqsPreferredAWhenIssuedSymbolConversion()
	{
		Assert.assertEquals("AGM PRAWI", SymbolConverterFactory.getConverterInstance(MdFeed.CQS).convert("AGMpAw"));
	}

	@Test
	public void testCqsEmergingCompanyMarketplaceSymbolConversion()
	{
		Assert.assertEquals("AGM EC", SymbolConverterFactory.getConverterInstance(MdFeed.CQS).convert("AGM/EC"));
	}

	@Test
	public void testCqsPartialPaidSymbolConversion()
	{
		Assert.assertEquals("AGM PP", SymbolConverterFactory.getConverterInstance(MdFeed.CQS).convert("AGM/PP"));
	}

	@Test
	public void testCqsConvertibleSymbolConversion()
	{
		Assert.assertEquals("AGM CV", SymbolConverterFactory.getConverterInstance(MdFeed.CQS).convert("AGM/CV"));
	}

	@Test
	public void testCqsConvertibleCalledSymbolConversion()
	{
		Assert.assertEquals("AGM CVCL", SymbolConverterFactory.getConverterInstance(MdFeed.CQS).convert("AGM/CV/CL"));
	}

	@Test
	public void testCqsClassConvertibleCalledSymbolConversion()
	{
		Assert.assertEquals("AGM ACV", SymbolConverterFactory.getConverterInstance(MdFeed.CQS).convert("AGM/A/CV"));
	}

	@Test
	public void testCqsPreferredClassAConvertibleSymbolConversion()
	{
		Assert.assertEquals("AGM PRACV", SymbolConverterFactory.getConverterInstance(MdFeed.CQS).convert("AGMpA/CV"));
	}

	@Test
	public void testCqsRightsSymbolConversion()
	{
		Assert.assertEquals("AGM RT", SymbolConverterFactory.getConverterInstance(MdFeed.CQS).convert("AGMr"));
	}

	@Test
	public void testCqsUnitsSymbolConversion()
	{
		Assert.assertEquals("AGM U", SymbolConverterFactory.getConverterInstance(MdFeed.CQS).convert("AGM/U"));
	}

	@Test
	public void testCqsWhenIssuedSymbolConversion()
	{
		Assert.assertEquals("AGM WI", SymbolConverterFactory.getConverterInstance(MdFeed.CQS).convert("AGMw"));
	}

	@Test
	public void testCqsRightsWhenIssuedSymbolConversion()
	{
		Assert.assertEquals("AGM RTWI", SymbolConverterFactory.getConverterInstance(MdFeed.CQS).convert("AGMrw"));
	}

	@Test
	public void testCqsPreferredWhenIssuedSymbolConversion()
	{
		Assert.assertEquals("AGM PRWI", SymbolConverterFactory.getConverterInstance(MdFeed.CQS).convert("AGMpw"));
	}

	@Test
	public void testCqsClassAWhenIssuedSymbolConversion()
	{
		Assert.assertEquals("AGM AWI", SymbolConverterFactory.getConverterInstance(MdFeed.CQS).convert("AGM/Aw"));
	}

	@Test
	public void testCqsWarrantWhenIssuedSymbolConversion()
	{
		Assert.assertEquals("AGM WSWI", SymbolConverterFactory.getConverterInstance(MdFeed.CQS).convert("AGM/WSw"));
	}

	@Test
	public void testCqsTestSymbolConversion()
	{
		Assert.assertEquals("AGM TEST", SymbolConverterFactory.getConverterInstance(MdFeed.CQS).convert("AGM/TEST"));
	}

	@Test
	public void testCqsConversionCaseSensitivity()
	{
		Assert.assertNotEquals("AGM WSWI", SymbolConverterFactory.getConverterInstance(MdFeed.CQS).convert("AGM/WSW"));
	}

	@Test
	public void testNoMatch()
	{
		Assert.assertEquals("AAPL", SymbolConverterFactory.getConverterInstance(MdFeed.CQS).convert("AAPL"));
	}
}

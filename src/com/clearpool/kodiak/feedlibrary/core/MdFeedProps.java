package com.clearpool.kodiak.feedlibrary.core;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.clearpool.kodiak.feedlibrary.core.bx.BxNormalizer;
import com.clearpool.kodiak.feedlibrary.core.cta.CqsNormalizer;
import com.clearpool.kodiak.feedlibrary.core.cta.CtsNormalizer;
import com.clearpool.kodiak.feedlibrary.core.nasdaq.NasdaqNormalizer;
import com.clearpool.kodiak.feedlibrary.core.opra.OpraNormalizer;
import com.clearpool.kodiak.feedlibrary.core.psx.PsxNormalizer;
import com.clearpool.kodiak.feedlibrary.core.utp.UqdfNormalizer;
import com.clearpool.kodiak.feedlibrary.core.utp.UtdfNormalizer;

public class MdFeedProps
{
	private static final Map<String, String> props = new HashMap<String, String>();
	private static final Map<String, Object> instanceProps = new HashMap<String, Object>();

	// Keys
	public static final String SEQUENCER = "SEQUENCER";
	public static final String NORMALIZER = "NORMALIZER";
	public static final String RANGE = "RANGE";

	static
	{
		props.put("TEST.1.A", "239.9.9.10:9010");
		props.put("TEST.2.A", "239.9.9.11:9011");

		props.put("OPRA." + NORMALIZER, OpraNormalizer.class.getCanonicalName());
		props.put("OPRA.1.RANGE", "[A|A-AAPL|L]");
		props.put("OPRA.1.A", "233.43.202.1:11101");
		props.put("OPRA.1.B", "233.43.202.33:12101");
		props.put("OPRA.2.RANGE", "[AAPL|M-AEMZZ|X]");
		props.put("OPRA.2.A", "233.43.202.2:11102");
		props.put("OPRA.2.B", "233.43.202.34:12102");
		props.put("OPRA.3.RANGE", "[AEN|A-AMTZZ|X]");
		props.put("OPRA.3.A", "233.43.202.153:16125");
		props.put("OPRA.3.B", "233.43.202.185:17125");
		props.put("OPRA.4.RANGE", "[AMU|A-ATZZZ|X]");
		props.put("OPRA.4.A", "233.43.202.154:16126");
		props.put("OPRA.4.B", "233.43.202.186:17126");

		props.put("CQS." + NORMALIZER, CqsNormalizer.class.getCanonicalName());
		props.put("CQS.1.RANGE", "[A-AWZZZZ]");
		props.put("CQS.1.A", "233.200.79.0:61000");
		props.put("CQS.1.B", "233.200.79.32:61032");
		props.put("CQS.2.RANGE", "[AX-CAZZZZ]");
		props.put("CQS.2.A", "233.200.79.1:61001");
		props.put("CQS.2.B", "233.200.79.33:61033");
		props.put("CQS.3.RANGE", "[CB-CZZZZZ]");
		props.put("CQS.3.A", "233.200.79.2:61002");
		props.put("CQS.3.B", "233.200.79.34:61034");
		props.put("CQS.4.RANGE", "[D-FDZZZZ]");
		props.put("CQS.4.A", "233.200.79.3:61003");
		props.put("CQS.4.B", "233.200.79.35:61035");
		props.put("CQS.5.RANGE", "[FE-HKZZZZ]");
		props.put("CQS.5.A", "233.200.79.4:61004");
		props.put("CQS.5.B", "233.200.79.36:61036");
		props.put("CQS.6.RANGE", "[HL-KDZZZZ]");
		props.put("CQS.6.A", "233.200.79.5:61005");
		props.put("CQS.6.B", "233.200.79.37:61037");
		props.put("CQS.7.RANGE", "[KE-MJZZZZ]");
		props.put("CQS.7.A", "233.200.79.6:61006");
		props.put("CQS.7.B", "233.200.79.38:61038");
		props.put("CQS.8.RANGE", "[MK-OMZZZZ]");
		props.put("CQS.8.A", "233.200.79.7:61007");
		props.put("CQS.8.B", "233.200.79.39:61039");
		props.put("CQS.9.RANGE", "[ON-RPZZZZ]");
		props.put("CQS.9.A", "233.200.79.8:61008");
		props.put("CQS.9.B", "233.200.79.40:61040");
		props.put("CQS.10.RANGE", "[RQ-SXZZZZ]");
		props.put("CQS.10.A", "233.200.79.9:61009");
		props.put("CQS.10.B", "233.200.79.41:61041");
		props.put("CQS.11.RANGE", "[SY-UYZZZZ]");
		props.put("CQS.11.A", "233.200.79.10:61010");
		props.put("CQS.11.B", "233.200.79.42:61042");
		props.put("CQS.12.RANGE", "[UZ-ZZZZZZ]");
		props.put("CQS.12.A", "233.200.79.11:61011");
		props.put("CQS.12.B", "233.200.79.43:61043");
		props.put("CQS.13.RANGE", "[A-DOZZZZ]");
		props.put("CQS.13.A", "233.200.79.16:61016");
		props.put("CQS.13.B", "233.200.79.48:61048");
		props.put("CQS.14.RANGE", "[DP-EFPZZZ]");
		props.put("CQS.14.A", "233.200.79.17:61017");
		props.put("CQS.14.B", "233.200.79.49:61049");
		props.put("CQS.15.RANGE", "[EFQ-EWZZZZ]");
		props.put("CQS.15.A", "233.200.79.18:61018");
		props.put("CQS.15.B", "233.200.79.50:61050");
		props.put("CQS.16.RANGE", "[EX-IDZZZZ]");
		props.put("CQS.16.A", "233.200.79.19:61019");
		props.put("CQS.16.B", "233.200.79.51:61051");
		props.put("CQS.17.RANGE", "[IE-IWCZZZ]");
		props.put("CQS.17.A", "233.200.79.20:61020");
		props.put("CQS.17.B", "233.200.79.52:61052");
		props.put("CQS.18.RANGE", "[IWD-KZZZZZ]");
		props.put("CQS.18.A", "233.200.79.21:61021");
		props.put("CQS.18.B", "233.200.79.53:61053");
		props.put("CQS.19.RANGE", "[L-QLZZZZ]");
		props.put("CQS.19.A", "233.200.79.22:61022");
		props.put("CQS.19.B", "233.200.79.54:61054");
		props.put("CQS.20.RANGE", "[QM-SPXZZZ]");
		props.put("CQS.20.A", "233.200.79.23:61023");
		props.put("CQS.20.B", "233.200.79.55:61055");
		props.put("CQS.21.RANGE", "[SPY-SRZZZZ]");
		props.put("CQS.21.A", "233.200.79.24:61024");
		props.put("CQS.21.B", "233.200.79.56:61056");
		props.put("CQS.22.RANGE", "[SS-USDZZZ]");
		props.put("CQS.22.A", "233.200.79.25:61025");
		props.put("CQS.22.B", "233.200.79.57:61057");
		props.put("CQS.23.RANGE", "[USE-XLBZZZ]");
		props.put("CQS.23.A", "233.200.79.26:61026");
		props.put("CQS.23.B", "233.200.79.58:61058");
		props.put("CQS.24.RANGE", "[XLC-ZZZZZZ]");
		props.put("CQS.24.A", "233.200.79.27:61027");
		props.put("CQS.24.B", "233.200.79.59:61059");

		props.put("CTS." + NORMALIZER, CtsNormalizer.class.getCanonicalName());
		props.put("CTS.1.RANGE", "[A-AWZZZZ]");
		props.put("CTS.1.A", "233.200.79.128:62128");
		props.put("CTS.1.B", "233.200.79.160:62160");
		props.put("CTS.2.RANGE", "[AX-CAZZZZ]");
		props.put("CTS.2.A", "233.200.79.129:62129");
		props.put("CTS.2.B", "233.200.79.161:62161");
		props.put("CTS.3.RANGE", "[CB-CZZZZZ]");
		props.put("CTS.3.A", "233.200.79.130:62130");
		props.put("CTS.3.B", "233.200.79.162:62162");
		props.put("CTS.4.RANGE", "[D-FDZZZZ]");
		props.put("CTS.4.A", "233.200.79.131:62131");
		props.put("CTS.4.B", "233.200.79.163:62163");
		props.put("CTS.5.RANGE", "[FE-HKZZZZ]");
		props.put("CTS.5.A", "233.200.79.132:62132");
		props.put("CTS.5.B", "233.200.79.164:62164");
		props.put("CTS.6.RANGE", "[HL-KDZZZZ]");
		props.put("CTS.6.A", "233.200.79.133:62133");
		props.put("CTS.6.B", "233.200.79.165:62165");
		props.put("CTS.7.RANGE", "[KE-MJZZZZ]");
		props.put("CTS.7.A", "233.200.79.134:62134");
		props.put("CTS.7.B", "233.200.79.166:62166");
		props.put("CTS.8.RANGE", "[MK-OMZZZZ]");
		props.put("CTS.8.A", "233.200.79.135:62135");
		props.put("CTS.8.B", "233.200.79.167:62167");
		props.put("CTS.9.RANGE", "[ON-RPZZZZ]");
		props.put("CTS.9.A", "233.200.79.136:62136");
		props.put("CTS.9.B", "233.200.79.168:62168");
		props.put("CTS.10.RANGE", "[RQ-SXZZZZ]");
		props.put("CTS.10.A", "233.200.79.137:62137");
		props.put("CTS.10.B", "233.200.79.169:62169");
		props.put("CTS.11.RANGE", "[SY-UYZZZZ]");
		props.put("CTS.11.A", "233.200.79.138:62138");
		props.put("CTS.11.B", "233.200.79.170:62170");
		props.put("CTS.12.RANGE", "[UZ-ZZZZZZ]");
		props.put("CTS.12.A", "233.200.79.139:62139");
		props.put("CTS.12.B", "233.200.79.171:62171");
		props.put("CTS.13.RANGE", "[A-DOZZZZ]");
		props.put("CTS.13.A", "233.200.79.144:62144");
		props.put("CTS.13.B", "233.200.79.176:62176");
		props.put("CTS.14.RANGE", "[DP-EFPZZZ]");
		props.put("CTS.14.A", "233.200.79.145:62145");
		props.put("CTS.14.B", "233.200.79.177:62177");
		props.put("CTS.15.RANGE", "[EFQ-EWZZZZ]");
		props.put("CTS.15.A", "233.200.79.146:62146");
		props.put("CTS.15.B", "233.200.79.178:62178");
		props.put("CTS.16.RANGE", "[EX-IDZZZZ]");
		props.put("CTS.16.A", "233.200.79.147:62147");
		props.put("CTS.16.B", "233.200.79.179:62179");
		props.put("CTS.17.RANGE", "[IE-IWCZZZ]");
		props.put("CTS.17.A", "233.200.79.148:62148");
		props.put("CTS.17.B", "233.200.79.180:62180");
		props.put("CTS.18.RANGE", "[IWD-KZZZZZ]");
		props.put("CTS.18.A", "233.200.79.149:62149");
		props.put("CTS.18.B", "233.200.79.181:62181");
		props.put("CTS.19.RANGE", "[L-QLZZZZ]");
		props.put("CTS.19.A", "233.200.79.150:62150");
		props.put("CTS.19.B", "233.200.79.182:62182");
		props.put("CTS.20.RANGE", "[QM-SPXZZZ]");
		props.put("CTS.20.A", "233.200.79.151:62151");
		props.put("CTS.20.B", "233.200.79.183:62183");
		props.put("CTS.21.RANGE", "[SPY-SRZZZZ]");
		props.put("CTS.21.A", "233.200.79.152:62152");
		props.put("CTS.21.B", "233.200.79.184:62184");
		props.put("CTS.22.RANGE", "[SS-USDZZZ]");
		props.put("CTS.22.A", "233.200.79.153:62153");
		props.put("CTS.22.B", "233.200.79.185:62185");
		props.put("CTS.23.RANGE", "[USE-XLBZZZ]");
		props.put("CTS.23.A", "233.200.79.154:62154");
		props.put("CTS.23.B", "233.200.79.186:62186");
		props.put("CTS.24.RANGE", "[XLC-ZZZZZZ]");
		props.put("CTS.24.A", "233.200.79.155:62155");
		props.put("CTS.24.B", "233.200.79.187:62187");

		props.put("UQDF." + NORMALIZER, UqdfNormalizer.class.getName());
		props.put("UQDF.1.RANGE", "[A-CD]");
		props.put("UQDF.1.A", "224.0.17.48:55530");
		props.put("UQDF.1.B", "224.0.17.49:55531");
		props.put("UQDF.2.RANGE", "[CE-FD]");
		props.put("UQDF.2.A", "224.0.17.50:55532");
		props.put("UQDF.2.B", "224.0.17.51:55533");
		props.put("UQDF.3.RANGE", "[FE-LK]");
		props.put("UQDF.3.A", "224.0.17.52:55534");
		props.put("UQDF.3.B", "224.0.17.53:55535");
		props.put("UQDF.4.RANGE", "[LL-PB]");
		props.put("UQDF.4.A", "224.0.17.54:55536");
		props.put("UQDF.4.B", "224.0.17.55:55537");
		props.put("UQDF.5.RANGE", "[PC-SP]");
		props.put("UQDF.5.A", "224.0.17.56:55538");
		props.put("UQDF.5.B", "224.0.17.57:55539");
		props.put("UQDF.6.RANGE", "[SQ-ZZ]");
		props.put("UQDF.6.A", "224.0.17.58:55540");
		props.put("UQDF.6.B", "224.0.17.59:55541");

		props.put("UTDF." + NORMALIZER, UtdfNormalizer.class.getName());
		props.put("UTDF.1.RANGE", "[A-CD]");
		props.put("UTDF.1.A", "224.0.1.92:55542");
		props.put("UTDF.1.B", "224.0.1.93:55543");
		props.put("UTDF.2.RANGE", "[CE-FD]");
		props.put("UTDF.2.A", "224.0.1.94:55544");
		props.put("UTDF.2.B", "224.0.1.95:55545");
		props.put("UTDF.3.RANGE", "[FE-LK]");
		props.put("UTDF.3.A", "224.0.1.96:55546");
		props.put("UTDF.3.B", "224.0.1.97:55547");
		props.put("UTDF.4.RANGE", "[LL-PB]");
		props.put("UTDF.4.A", "224.0.1.98:55548");
		props.put("UTDF.4.B", "224.0.1.99:55549");
		props.put("UTDF.5.RANGE", "[PC-SP]");
		props.put("UTDF.5.A", "224.0.1.100:55550");
		props.put("UTDF.5.B", "224.0.1.101:55551");
		props.put("UTDF.6.RANGE", "[SQ-ZZ]");
		props.put("UTDF.6.A", "224.0.1.102:55552");
		props.put("UTDF.6.B", "224.0.1.103:55553");

		props.put("NASDAQ." + NORMALIZER, NasdaqNormalizer.class.getName());
		props.put("NASDAQ.1.RANGE", "[A-Z]");
		props.put("NASDAQ.1.A", "233.54.12.111:26477");
		props.put("NASDAQ.1.B", "233.86.230.111:26477");

		props.put("BX." + NORMALIZER, BxNormalizer.class.getName());
		props.put("BX.1.RANGE", "[A-Z]");
		props.put("BX.1.A", "233.54.12.40:25475");
		props.put("BX.1.B", "233.86.230.40:25475");

		props.put("PSX." + NORMALIZER, PsxNormalizer.class.getName());
		props.put("PSX.1.RANGE", "[A-Z]");
		props.put("PSX.1.A", "233.54.12.45:26477");
		props.put("PSX.1.B", "233.86.230.45:26477");

		props.put("ARCA.1.RANGE", "1:[A-Z]");
		props.put("ARCA.1.A", "224.0.59.76:11076");
		props.put("ARCA.1.B", "224.0.59.204:11204");
		props.put("ARCA.2.RANGE", "2:[A-Z]");
		props.put("ARCA.2.A", "224.0.59.77:11077");
		props.put("ARCA.2.B", "224.0.59.205:11205");
		props.put("ARCA.3.RANGE", "3:[A-Z]");
		props.put("ARCA.3.A", "224.0.59.78:11078");
		props.put("ARCA.3.B", "224.0.59.206:11206");
		props.put("ARCA.4.RANGE", "4:[A-Z]");
		props.put("ARCA.4.A", "224.0.59.79:11079");
		props.put("ARCA.4.B", "224.0.59.207:11207");

		props.put("BATS.1.RANGE", "A-AIZZZZ");
		props.put("BATS.1.A", "224.0.62.2:30001");
		props.put("BATS.1.B", "233.19.3.128:30001");
	}

	public static String getProperty(String... strings)
	{
		StringBuilder builder = new StringBuilder();
		for (String string : strings)
		{
			builder.append(string);
			builder.append('.');
		}
		builder.deleteCharAt(builder.length() - 1);
		String key = builder.toString();
		String value = props.get(key);
		return (value == null) ? null : value.trim();
	}

	public static Object getInstanceProperty(String... strings)
	{
		StringBuilder builder = new StringBuilder();
		for (String string : strings)
		{
			builder.append(string);
			builder.append('.');
		}
		builder.deleteCharAt(builder.length() - 1);
		String key = builder.toString();
		return instanceProps.get(key);
	}

	public static void putInstanceProperty(Object object, String... strings)
	{
		StringBuilder builder = new StringBuilder();
		for (String string : strings)
		{
			builder.append(string);
			builder.append('.');
		}
		builder.deleteCharAt(builder.length() - 1);
		String key = builder.toString();
		instanceProps.put(key, object);
	}

	public static Map<String, String> getAsMap(String propAsMap)
	{
		if (propAsMap == null) return null;
		Map<String, String> map = new HashMap<String, String>();
		String[] commaSplit = propAsMap.split(",");
		for (String commaItem : commaSplit)
		{
			String[] equalSplit = commaItem.split("=");
			String key = equalSplit[0];
			String value = equalSplit[1];
			map.put(key, value);
		}
		return map;
	}

	public static Set<String> getAsSet(String propAsSet)
	{
		if (propAsSet == null) return null;
		Set<String> set = new HashSet<String>();
		String[] commaSplit = propAsSet.split(",");
		for (String commaItem : commaSplit)
		{
			set.add(commaItem);
		}
		return set;
	}
}

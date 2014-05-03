package info.blockchain.wallet.ui;

import java.math.BigInteger;
import java.text.DecimalFormat;

import piuk.blockchain.android.util.WalletUtils;

public class BlockchainUtil {
	
    public static int BLOCKCHAIN_RED = 0xFFd17d7d;
    public static int BLOCKCHAIN_GREEN = 0xFF29A432;
    
    public static String ZEROBLOCK_PACKAGE = "com.phlint.android.zeroblock";
    
    private static double BTC_RATE = 448.00;

	private BlockchainUtil() { ; }

	public static String BTC2Fiat(String btc)	{
		double val = 0.0;
		
		try	{
			val = Double.parseDouble(btc);
		}
		catch(NumberFormatException nfe)	{
			val = 0.0;
		}

		DecimalFormat df = new DecimalFormat("######0.00");
		return df.format(BTC2Fiat(val));
	}

	public static String Fiat2BTC(String fiat)	{
		double val = 0.0;
		
		try	{
			val = Double.parseDouble(fiat);
		}
		catch(NumberFormatException nfe)	{
			val = 0.0;
		}

        DecimalFormat df = new DecimalFormat("####0.0000");
		return df.format(Fiat2BTC(val));
	}

	public static double BTC2Fiat(double btc)	{
		return btc * BTC_RATE;
	}

	public static double Fiat2BTC(double fiat)	{
		return fiat / BTC_RATE;
	}
	
	public static String formatBitcoin(BigInteger value) {
        DecimalFormat df = new DecimalFormat("####0.0000");
		return df.format(Double.parseDouble(WalletUtils.formatValue(value)));
	}

}

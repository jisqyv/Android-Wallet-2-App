package info.blockchain.wallet.ui;

import java.util.HashMap;

import android.content.Context;
import android.preference.PreferenceManager;
import android.content.SharedPreferences;
//import android.util.Log;

import info.blockchain.api.ExchangeRates;

public class CurrencyExchange	{

    private static CurrencyExchange instance = null;
    
    private static HashMap<String,Double> prices = null;
    private static HashMap<String,String> symbols = null;
    private static ExchangeRates fxRates = null;

    private static String strFiatCode = null;

    private static long ts = 0L;

    private static Context context = null;
    
    private CurrencyExchange()	{ ; }

	public static CurrencyExchange getInstance(Context ctx) {
		
		context = ctx;

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        strFiatCode = prefs.getString("ccurrency", "USD");

		if (instance == null) {
			fxRates = new ExchangeRates();
		    prices = new HashMap<String,Double>();
		    symbols = new HashMap<String,String>();
			String[] currencies = fxRates.getCurrencies();
	    	for(int i = 0; i < currencies.length; i++)	 {
		    	prices.put(currencies[i], Double.longBitsToDouble(prefs.getLong(currencies[i], Double.doubleToLongBits(0.0))));
		    	symbols.put(currencies[i], prefs.getString(currencies[i] + "-SYM", null));
	    	}

	    	instance = new CurrencyExchange();
		}

    	if(System.currentTimeMillis() - ts > (15 * 60 * 1000)) {

    		getExchangeRates();
    		
			String[] currencies = fxRates.getCurrencies();
            SharedPreferences.Editor editor = prefs.edit();
	    	for(int i = 0; i < currencies.length; i++)	 {
		    	if(fxRates.getLastPrice(currencies[i]) > 0.0)	{
                    editor.putLong(currencies[i], Double.doubleToRawLongBits(fxRates.getLastPrice(currencies[i])));
                    editor.putString(currencies[i] + "-SYM", fxRates.getSymbol(currencies[i]));
		    	}
	    	}
            editor.commit();

    	}

		return instance;
	}
	
    public Double getCurrencyPrice(String currency)	{
    	if(prices.containsKey(currency) && prices.get(currency) != 0.0)	{
    		return prices.get(currency);
    	}
    	else	{
            String[] currencies = getBlockchainCurrencies();
            return OtherCurrencyExchange.getInstance(context, currencies, strFiatCode).getCurrencyPrice(currency, prices.get("USD"));
//    		return OtherCurrencyExchange.getInstance(context, getBlockchainCurrencies(), ).getCurrencyPrice(currency);
    	}

    }

    public String getCurrencySymbol(String currency)	{
    	if(symbols.containsKey(currency) && symbols.get(currency) != null)	{
    		return symbols.get(currency);
    	}
    	else	{
    		return null;
    	}

    }

    public String[] getBlockchainCurrencies()	{
		return fxRates.getCurrencies();
    }

    public void localUpdate()	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		String[] currencies = fxRates.getCurrencies();
    	for(int i = 0; i < currencies.length; i++)	 {
	    	prices.put(currencies[i], Double.longBitsToDouble(prefs.getLong(currencies[i], Double.doubleToLongBits(0.0))));
	    	symbols.put(currencies[i], prefs.getString(currencies[i] + "-SYM", null));
    	}
    }

	private static void getExchangeRates() {
		ts = System.currentTimeMillis();
        DownloadFXRatesTask task = new DownloadFXRatesTask(context, fxRates);
        task.execute(new String[] { fxRates.getUrl() });
	}

}

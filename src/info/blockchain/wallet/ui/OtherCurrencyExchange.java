package info.blockchain.wallet.ui;

import java.util.ArrayList;
import java.util.HashMap;

import org.xml.sax.SAXException;

import piuk.blockchain.android.util.WalletUtils;

import android.content.Context;
import android.os.Handler;
import android.util.Xml;
//import android.util.Log;

public class OtherCurrencyExchange	{

    private static OtherCurrencyExchange instance = null;
    
	private static String[] currencies = null;
    private static HashMap<String,Double> prices = null;
    private static HashMap<String,String> names = null;

    private static long ts = 0L;

    private static Context context = null;
    
    private OtherCurrencyExchange()	{ ; }

	public static OtherCurrencyExchange getInstance(Context ctx) {
		
		context = ctx;

		if (instance == null) {
		    prices = new HashMap<String,Double>();
		    names = new HashMap<String,String>();
	    	instance = new OtherCurrencyExchange();
		}

    	if(System.currentTimeMillis() - ts > (120 * 60 * 1000)) {
    		getExchangeRates();
    	}

		return instance;
	}
	
    public Double getCurrencyPrice(String currency)	{
    	
    	if(prices.containsKey(currency) && prices.get(currency) != 0.0)	{
    		return 1.0 / ((1.0 / prices.get(currency)) * (1.0 / CurrencyExchange.getInstance(context).getCurrencyPrice("USD")));
    	}
    	else	{
    		return 0.0;
    	}

    }

    public String getCurrencyName(String currency)	{
    	
    	if(names.containsKey(currency) && names.get(currency) != null)	{
    		return names.get(currency);
    	}
    	else	{
    		return null;
    	}

    }

    public HashMap<String,String> getCurrencyNames()	{
    	return names;
    }

    public HashMap<String,Double> getCurrencyPrices()	{
    	return prices;
    }

	private static void getExchangeRates() {
		
		final Handler handler = new Handler();

		new Thread(new Runnable() {
			@Override
			public void run() {
				try {							
					final String response = WalletUtils.getURL("http://themoneyconverter.com/rss-feed/USD/rss.xml");

					handler.post(new Runnable() {
						@Override
						public void run() {
							try {
				        		TheMoneyConverterXML mcx = new TheMoneyConverterXML();
				            	try {
				            		Xml.parse(response, mcx);
				            		if(mcx.getExchangeRates() != null && mcx.getCurrencyNames() != null) {
				                		prices = mcx.getExchangeRates();
				                		names = mcx.getCurrencyNames();
				            		}
				            	} catch (SAXException se) {
				            		se.printStackTrace();
				            	}

							} catch (Exception e) {
								e.printStackTrace();
							}
							
							ts = System.currentTimeMillis();
						}
					});
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}).start();
	}

}

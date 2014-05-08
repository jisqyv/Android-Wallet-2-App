package info.blockchain.wallet.ui;
 
import android.content.Context;
import android.util.Log;

import java.net.URL;
import java.net.MalformedURLException;
import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.json.JSONException;
import org.json.JSONObject;

public class ExchangeRateUtil {
	
    private static ExchangeRateUtil instance = null;
    private static double USD = 441.0;

	private ExchangeRateUtil() { ; }

	public static ExchangeRateUtil getInstance(Context ctx) {
		
		if(instance == null) {
			instance = new ExchangeRateUtil();
		}
		
		getTicker();
		
		return instance;
	}

	private static String getTicker() {

		String fx = null;
		
        try {
            get("USD", IOUtils.toString(new URL("http://blockchain.info/ticker"), "UTF-8"));
        }
        catch(MalformedURLException mue) {
        	mue.printStackTrace();
        }
        catch(IOException ioe) {
        	ioe.printStackTrace();
        }
        
        return fx;
	}

	public double getUSD() {
		return USD;
	}

    private static void get(String currency, String data)	 {
        try {
    		JSONObject jsonObject = new JSONObject(data);
    		if(jsonObject != null)	{
    			JSONObject jsonCurr = jsonObject.getJSONObject(currency);
        		if(jsonCurr != null)	{
        			USD = jsonCurr.getDouble("last");
        			Log.d("Blockchain/Bitstamp USD", "" + USD);
        		}
    		}
    	} catch (JSONException je) {
    		;
    	}
    }

}

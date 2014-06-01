package info.blockchain.wallet.ui;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;

import info.blockchain.api.ExchangeRates;

public class DownloadFXRatesTask extends AsyncTask<String, Void, String> {
	
	private Context context = null;
	private ExchangeRates fxRates = null;
    private static HashMap<String,Double> prices = null;
    private static HashMap<String,String> symbols = null;
	
	public DownloadFXRatesTask(Context context, ExchangeRates fxRates) {
		this.context = context;
		this.fxRates = fxRates;
	    prices = new HashMap<String,Double>();
	    symbols = new HashMap<String,String>();
	}
	
    @Override
    protected String doInBackground(String... urls) {

      String response = "";

      for (int i = 0; i < urls.length; i++) {
    	String url = urls[i];  
        DefaultHttpClient client = new DefaultHttpClient();
        HttpGet httpGet = new HttpGet(url);
        try {
          HttpResponse execute = client.execute(httpGet);
          InputStream content = execute.getEntity().getContent();

          BufferedReader buffer = new BufferedReader(new InputStreamReader(content));
          String s = "";
          while ((s = buffer.readLine()) != null) {
              response += s;
          }

        } catch (Exception e) {
          e.printStackTrace();
        }
      }

      return response;
    }

    @Override
    protected void onPostExecute(String result) {

		fxRates.setData(result);
		fxRates.parse();

		String[] currencies = fxRates.getCurrencies();
    	for(int i = 0; i < currencies.length; i++)	 {
	    	prices.put(currencies[i], fxRates.getLastPrice(currencies[i]));

			if(fxRates.getSymbol(currencies[i]).equals("kr"))	 {
				symbols.put(currencies[i], "K");
			}
			else if(fxRates.getSymbol(currencies[i]).equals("CHF"))	 {
				symbols.put(currencies[i], "F");
			}
			else if(fxRates.getSymbol(currencies[i]).equals("RUB"))	 {
				symbols.put(currencies[i], "R");
			}
			else if(fxRates.getSymbol(currencies[i]).equals("zł"))	 {
				symbols.put(currencies[i], "Z");
			}
			else if(fxRates.getSymbol(currencies[i]).endsWith("$"))	 {
				symbols.put(currencies[i], "$");
			}
			else	 {
		    	symbols.put(currencies[i], fxRates.getSymbol(currencies[i]));
			}

    	}

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
    	for(int i = 0; i < currencies.length; i++)	 {
	    	if(prices.containsKey(currencies[i]) && prices.get(currencies[i]) != 0.0)	{
                editor.putLong(currencies[i], Double.doubleToRawLongBits(prices.get(currencies[i])));
                editor.putString(currencies[i] + "-SYM", symbols.get(currencies[i]));
	    	}
    	}
        editor.commit();
    }

  }

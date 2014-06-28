package info.blockchain.merchant.directory;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import android.util.Log;

import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class BTCBusinesses	{

    private static BTCBusinesses instance = null;
    private static ArrayList<BTCBusiness> businesses = null;

	private BTCBusinesses() 	{ }

	public static BTCBusinesses getInstance()	{

		if(instance == null)	{
			instance = new BTCBusinesses();
			businesses = new ArrayList<BTCBusiness>();
		}
		
		return instance;
		
	}

	public static ArrayList<BTCBusiness> getData()	{

		return businesses;
		
	}

	public static void refresh(String url) throws Exception {

		Log.d("URL", url);

		String data = getURL(url);
		Log.d("JSON data", data);
		businesses = parse(data);
		Log.d("Businesses returned", "" + businesses.size());

	}

	private static String getURL(String URL) throws Exception {
		
		final int DefaultRequestRetry = 2;
		final int DefaultRequestTimeout = 60000;

		URL url = new URL(URL);

		String error = null;
		
		for (int i = 0; i < DefaultRequestRetry; i++) {

			HttpURLConnection connection = (HttpURLConnection) url.openConnection();

			try {
				connection.setRequestMethod("GET");
				connection.setRequestProperty("charset", "utf-8");
				connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_9_0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/31.0.1650.57 Safari/537.36");

				connection.setConnectTimeout(DefaultRequestTimeout);
				connection.setReadTimeout(DefaultRequestTimeout);

				connection.setInstanceFollowRedirects(false);

				connection.connect();

				if (connection.getResponseCode() == 200)
					return IOUtils.toString(connection.getInputStream(), "UTF-8");
				else
					error = IOUtils.toString(connection.getErrorStream(), "UTF-8");
				
				Thread.sleep(5000);
			} finally {
				connection.disconnect();
			}
		}
		
		return error;
	}

    public static ArrayList<BTCBusiness> parse(String data)	{
    	
    	ArrayList<BTCBusiness> btcb = new ArrayList<BTCBusiness>();
    	
        try {
    		JSONArray jsonArray = new JSONArray(data);

    		if(jsonArray != null && jsonArray.length() > 0)	{
    			
    			for(int i = 0; i < jsonArray.length(); i++)	{
    				BTCBusiness business = new BTCBusiness();
        			JSONObject jsonObj = jsonArray.getJSONObject(i);
        			if(jsonObj.has("id"))	{
            			business.id = jsonObj.getString("id");
        			}
        			if(jsonObj.has("name"))	{
            			business.name = jsonObj.getString("name");
        			}
        			if(jsonObj.has("address"))	{
            			business.address = jsonObj.getString("address");
        			}
        			if(jsonObj.has("city"))	{
            			business.city = jsonObj.getString("city");
        			}
        			if(jsonObj.has("pcode"))	{
            			business.pcode = jsonObj.getString("pcode");
        			}
        			if(jsonObj.has("tel"))	{
            			business.tel = jsonObj.getString("tel");
        			}
        			if(jsonObj.has("web"))	{
            			business.id = jsonObj.getString("web");
        			}
        			if(jsonObj.has("lat"))	{
            			business.lat = jsonObj.getString("lat");
        			}
        			if(jsonObj.has("lon"))	{
            			business.lon = jsonObj.getString("lon");
        			}
        			if(jsonObj.has("flag"))	{
            			business.flag = jsonObj.getString("flag");
        			}
        			if(jsonObj.has("desc"))	{
            			business.desc = jsonObj.getString("desc");
        			}
        			if(jsonObj.has("distance"))	{
            			business.distance = jsonObj.getString("distance");
        			}
        			if(jsonObj.has("hc"))	{
            			business.hc = jsonObj.getString("hc");
        			}

        			btcb.add(business);
    			}

    		}
    	} catch (JSONException je) {
    		je.printStackTrace();
    	}
        
        return btcb;

    }

}

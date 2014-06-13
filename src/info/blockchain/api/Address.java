package info.blockchain.api;

import java.util.ArrayList;

import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;

import android.util.Log;

/**
* This class obtains info on a Bitcoin wallet address from Blockchain.info.
* 
*/
public class Address extends BlockchainAPI {

    private static final String TAG = "Address";

	private long balance = 0L;
	private long received = 0L;
	private long sent = 0L;

	/**
     * Constructor for this instance.
     * 
     * @param String data returned from Blockchain.info
     */
    public Address(String address) {

    	strUrl = "https://blockchain.info/address/" + address + "?format=json";
		Log.d(TAG, strUrl);

    }

    public long getBalance() {
    	return balance;
    }

    public long getSent() {
    	return sent;
    }

    public long getReceive() {
    	return received;
    }

    /**
     * Parse the data supplied to this instance.
     * 
     */
    public void parse()	{
    	
        try {
    		JSONObject add = new JSONObject(strData);
    		if(add != null)	{
    			if(add.has("final_balance"))	{
        			balance = add.getLong("final_balance");
    			}
    			if(add.has("total_received"))	{
        			received = add.getLong("total_received");
    			}
    			if(add.has("total_sent"))	{
        			sent = add.getLong("total_sent");
    			}
    		}
    	} catch (JSONException je) {
    		je.printStackTrace();
    	}

    }

}

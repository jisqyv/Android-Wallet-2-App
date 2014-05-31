package info.blockchain.api;

import org.json.JSONException;
import org.json.JSONObject;
//import android.util.Log;

/**
* Blockchain.info Receive Payments API.
* 
* <p>Each instance corresponds to a new receiving address.
* 
*/
public class ReceivePayments extends BlockchainAPI	{

    private static final String TAG = "ReceivePayments";

    private String input_address = null;

    /**
     * Constructor for this instance.
     * 
     * @param String address Bitcoin wallet address of receiving wallet.
     * 
     */
    public ReceivePayments(String address) {
    	strUrl = "https://blockchain.info/api/receive?method=create&address=" + address;
    }

    /**
     * Returns Receive Payments API receiving address.
     * 
     * @return String receiving address for this instance.
     * 
     */
    public String getInputAddress()	{
    	return input_address;
    }

    /**
     * Parse the data supplied to this instance.
     * 
     */
    public void parse()	{

        try {
    		JSONObject jsonObject = new JSONObject(strData);
    		if(jsonObject != null)	{
    			input_address = jsonObject.getString("input_address");
    		}
    	} catch (JSONException je) {
    		;
    	}

    }

}

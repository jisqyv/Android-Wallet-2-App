package info.blockchain.api;

import android.util.Log;

/**
* This class obtains info on the latest block from Blockchain.info.
* 
*/
public class Hash2Ntxid extends BlockchainAPI	{
	
    private static final String TAG = "Hash2Ntxid";

    public Hash2Ntxid(String hash) {
    	super();
    	
    	strUrl = "https://blockchain.info/q/hashtontxid/" + hash;
    }

    public String getNtxid() {
    	return strData;
    }

    /**
     * Parse the data supplied to this instance.
     * 
     */
    public void parse()	{
    	;
    }

}

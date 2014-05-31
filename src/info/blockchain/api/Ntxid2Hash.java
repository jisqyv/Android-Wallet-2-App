package info.blockchain.api;

import android.util.Log;

/**
* This class obtains info on the latest block from Blockchain.info.
* 
*/
public class Ntxid2Hash extends BlockchainAPI	{
	
    private static final String TAG = "Ntxid2Hash";

    public Ntxid2Hash(String ntxid) {
    	super();
    	
    	strUrl = "https://blockchain.info/q/ntxidtohash/" + ntxid;
    }

    public String getHash() {
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

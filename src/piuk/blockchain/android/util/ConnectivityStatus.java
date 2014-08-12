package piuk.blockchain.android.util;

import piuk.blockchain.android.R;
import piuk.blockchain.android.WalletApplication;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;

public class ConnectivityStatus {
	
	ConnectivityStatus() { ; }
	
	public static boolean hasConnectivity(Context ctx) {
		boolean ret = false;
		
 		ConnectivityManager cm = (ConnectivityManager)ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
        if(cm != null) {
    		NetworkInfo neti = cm.getActiveNetworkInfo();
        	if(neti != null) {
                if(neti.getState() == NetworkInfo.State.CONNECTED) {
                	ret = true;
                }
        	}
    	}

        return ret;
	}

	public static boolean hasWiFi(Context ctx) {
		boolean ret = false;
		
 		ConnectivityManager cm = (ConnectivityManager)ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
        if(cm != null) {
    	    if(cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI).isConnected()) {
    	        ret = true;
    	    }
    	}

        return ret;
	}

}

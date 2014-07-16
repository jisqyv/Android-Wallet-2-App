package info.blockchain.wallet.ui;
 
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class TimeOutUtil {
	
    private static long lastPin = 0L;
    private static TimeOutUtil instance = null;

	private TimeOutUtil() { ; }
	
	private static Context context = null;

	public static TimeOutUtil getInstance(Context ctx) {
		
		context = ctx;
		
		if(instance == null) {
			instance = new TimeOutUtil();

			/*
	        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
	        String strLastPin = prefs.getString("lastPin", "0");
	        lastPin = Long.parseLong(strLastPin);
	        */
		}
		
		return instance;
	}

	public void updatePin() {
		lastPin = System.currentTimeMillis();
		/*
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("lastPin", Long.toString(lastPin));
        editor.commit();
        */
	}

	public boolean isTimedOut() {
		return (System.currentTimeMillis() - lastPin) > (1000 * 60 * 5);
	}

}

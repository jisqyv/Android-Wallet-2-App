package info.blockchain.wallet.ui;

import android.os.Handler;
import android.app.Activity;
import android.util.Log;

import piuk.blockchain.android.MyRemoteWallet;
import piuk.blockchain.android.WalletApplication;

public class WalletUtil {

    private static WalletUtil instance = null;
    private static Activity activity = null;
	private static WalletApplication application = null;
	private static MyRemoteWallet remoteWallet = null;

	private WalletUtil() { ; }

	public static WalletUtil getInstance(Activity act) {
		
		if(instance == null) {
			
			Log.d("WalletUtil", "1 instance == null");

			instance = new WalletUtil();

			activity = act;

			application = (WalletApplication)activity.getApplication();
			// has remoteWallet been assigned during PIN/password validation ?
			if(remoteWallet == null) {
				Log.d("WalletUtil", "Fetching remoteWallet");
				remoteWallet = application.getRemoteWallet();
			}
			else	{
				Log.d("WalletUtil", "Returning stored remoteWallet");
			}

			if(remoteWallet == null) {
				Log.d("WalletUtil", "Refetching remoteWallet");
				fetch();
			}

		}
		
		return instance;
	}

	public static WalletUtil getRefreshedInstance(Activity act) {

		if(instance == null) {
			return getInstance(act);
		}

		instance = new WalletUtil();

		activity = act;
		
		application = (WalletApplication)activity.getApplication();
		remoteWallet = application.getRemoteWallet();
		if(remoteWallet == null) {
			fetch();
		}

		return instance;
	}

	public WalletApplication getWalletApplication() {
		return application;
	}

	public MyRemoteWallet getRemoteWallet() {
		return remoteWallet;
	}

	public boolean remoteWalletIsLoaded() {
		return (remoteWallet != null);
	}

	public static void putRemoteWallet(MyRemoteWallet wallet) {
		remoteWallet = wallet;
	}

	private static void fetch() {
		
		final Handler handler = new Handler();
		
		handler.post(new Runnable() {
			@Override
			public void run() {
				try {
					application = (WalletApplication)activity.getApplication();
					remoteWallet = application.getRemoteWallet();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});

	}

}

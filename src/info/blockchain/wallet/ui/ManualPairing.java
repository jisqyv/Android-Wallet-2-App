package info.blockchain.wallet.ui;

import java.util.UUID;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import piuk.blockchain.android.MyRemoteWallet;
import piuk.blockchain.android.R;
//import android.util.Log;
import piuk.blockchain.android.SuccessCallback;
import piuk.blockchain.android.WalletApplication;

public class ManualPairing extends Activity	{

    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

	    this.requestWindowFeature(Window.FEATURE_NO_TITLE);
//	    this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
	    this.setContentView(R.layout.manual_pairing);

	    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
	    
        final EditText uuid = (EditText)findViewById(R.id.uuid);
        final EditText pw = (EditText)findViewById(R.id.pw);

        Button bOK = (Button)findViewById(R.id.ok);
        bOK.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {

            	String strUUID = uuid.getText().toString().trim();
            	String strPW = pw.getText().toString().trim();
            	
            	if(!validateGUID(strUUID)) {
    				Toast.makeText(ManualPairing.this, R.string.invalid_wallet_identifier, Toast.LENGTH_LONG).show();
    				return;
            	}

    			if(strPW.length() < 11 || strPW.length() > 255 || strPW.length() < 11 || strPW.length() > 255) {
    				Toast.makeText(ManualPairing.this, R.string.new_account_password_length_error, Toast.LENGTH_LONG).show();
    				return;
    			}
    			
//            	setResult(RESULT_OK, (new Intent()).setAction(strUUID + strPW));
//            	finish();
    			
				pairManually(strUUID, strPW);

            }
        });

        Button bCancel = (Button)findViewById(R.id.cancel);
        bCancel.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
            	setResult(RESULT_CANCELED);
            	finish();
            }
        });

    }

	public boolean validateGUID(String input_guid)	{

		if (input_guid == null)
			return false;

		if (input_guid.length() != 36)
			return false;

		try {
			input_guid = UUID.fromString(input_guid).toString();	// Check is valid uuid format
		} catch (IllegalArgumentException e) {
			return false;
		}
		
		return true;
	}
	
	public void pairManually(final String guid, final String password) {

		final Activity activity = this;

		final WalletApplication application = (WalletApplication) getApplication();

		final Handler handler = new Handler();

		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					final String payload = MyRemoteWallet.getWalletManualPairing(guid);

					handler.post(new Runnable() {

						@Override
						public void run() {

							try {
								final MyRemoteWallet wallet = new MyRemoteWallet(payload, password);

								if(wallet == null) {
									return;
								}

								String sharedKey = wallet.getSharedKey();

								application.clearWallet();

//								PinEntryActivity.clearPrefValues(application);

								Editor edit = PreferenceManager.getDefaultSharedPreferences(activity).edit();

								edit.putString("guid", guid);
								edit.putString("sharedKey", sharedKey);

								edit.commit();

								application.checkIfWalletHasUpdated(password, guid, sharedKey, true, new SuccessCallback(){
									@Override
									public void onSuccess() {
//										registerNotifications();

								        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ManualPairing.this);
										Editor edit = prefs.edit();
										edit.putBoolean("validated", true);
										edit.putBoolean("paired", true);
										edit.commit();

							        	Intent intent = new Intent(ManualPairing.this, PinEntryActivity.class);
							        	intent.putExtra("S", "1");
										intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
							    		startActivity(intent);

										finish();
									}

									@Override
									public void onFail() {
//										finish();
										Toast.makeText(application, R.string.error_pairing_wallet, Toast.LENGTH_LONG).show();
									}
								});
							} catch (final Exception e) {
//								Toast.makeText(application, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
								Toast.makeText(application, R.string.error_pairing_wallet, Toast.LENGTH_LONG).show();

								application.writeException(e);

//								finish();
							}
						}
					});
				} catch (final Exception e) {
					e.printStackTrace();

					handler.post(new Runnable() {
						public void run() {

//							Toast.makeText(application, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
							Toast.makeText(application, R.string.error_pairing_wallet, Toast.LENGTH_LONG).show();

							application.writeException(e);

//							finish();
						}
					});
				}
			}
		}).start();
	}

}

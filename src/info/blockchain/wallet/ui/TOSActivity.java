package info.blockchain.wallet.ui;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.List;

import org.json.simple.JSONObject;
import org.spongycastle.util.encoders.Hex;

import piuk.EventListeners;
import piuk.MyRemoteWallet;
import piuk.MyWallet;
import piuk.MyRemoteWallet.SendProgress;
import piuk.blockchain.android.Constants;
import piuk.blockchain.android.R;
import piuk.blockchain.android.WalletApplication;
import piuk.blockchain.android.ui.PinEntryActivity;
import piuk.blockchain.android.ui.SendCoinsActivity;
import piuk.blockchain.android.ui.SuccessCallback;
import piuk.blockchain.android.ui.dialogs.RequestPasswordDialog;

import com.google.android.gcm.GCMRegistrar;
import com.google.bitcoin.core.Address;
import com.google.bitcoin.core.ECKey;
import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.core.TransactionOutput;
import com.google.bitcoin.core.Wallet.SendRequest;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.ImageView;
import android.widget.Toast;
import android.text.util.Linkify;
//import android.util.Log;

public class TOSActivity extends Activity	{

	private TextView tvTOS =  null;

	public static final int PBKDF2Iterations = 2000;

    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
	    this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
	    this.setContentView(R.layout.activity_tos);

        Bundle extras = getIntent().getExtras();
        if(extras != null)	{
        	;
        }

        TextView tvTOS = (TextView) findViewById(R.id.tos);
        tvTOS.setText("By tapping on 'ACCEPT', you acknowledge that you are accepting Blockchain's terms of service:\nhttps://blockchain.info");
        Linkify.addLinks(tvTOS, Linkify.WEB_URLS);

        Button btOK = ((Button)findViewById(R.id.ok));
        btOK.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {

				final ProgressDialog progressDialog = ProgressDialog.show(TOSActivity.this, "", getString(R.string.creating_account), true);
				progressDialog.show();

				final Handler handler = new Handler();

				new Thread() {
					@Override
					public void run() {
						
						final WalletApplication application = WalletUtil.getInstance(TOSActivity.this, TOSActivity.this).getWalletApplication();
						try {
							try {

								application.generateNewWallet();

							} catch (Exception e1) {

								throw new Exception("Error Generating Wallet");

							}

							final String guid = application.getRemoteWallet().getGUID();
							final String sharedKey = application.getRemoteWallet().getSharedKey();
							final String password = "123456789abc";
							final String pinCode = "1234";
//							final String email = em;

							application.getRemoteWallet().setTemporyPassword(password);

							if (!application.getRemoteWallet().remoteSave("")) {
								throw new Exception("Unknown Error Inserting wallet");
							}

							EventListeners.invokeWalletDidChange();

							handler.post(new Runnable() {
								public void run() {

									try {
										progressDialog.dismiss();

//										dismiss();

//										final AbstractWalletActivity activity = (AbstractWalletActivity) getActivity();
										Toast.makeText(TOSActivity.this.getApplication(), R.string.new_account_success, Toast.LENGTH_SHORT).show();

//										PinEntryActivity.clearPrefValues(application);

										final Editor edit = PreferenceManager.getDefaultSharedPreferences(application.getApplicationContext()).edit();
										edit.putString("guid", guid);
										edit.putString("sharedKey", sharedKey);

										if (edit.commit()) {
											handler.post(new Runnable() {

												@Override
												public void run() {

													new Thread(new Runnable(){
													    @Override
													    public void run() {
													    	
															Looper.prepare();

															//
															// Save PIN
															//
													        try {
																byte[] bytes = new byte[16];
																SecureRandom random = new SecureRandom();
																random.nextBytes(bytes);
																final String key = new String(Hex.encode(bytes), "UTF-8");
																random.nextBytes(bytes);
																final String value = new String(Hex.encode(bytes), "UTF-8");
																final JSONObject response = piuk.blockchain.android.ui.PinEntryActivity.apiStoreKey(key, value, pinCode);
																if (response.get("success") != null) {

																	edit.putString("pin_kookup_key", key);
																	edit.putString("encrypted_password", MyWallet.encrypt(application.getRemoteWallet().getTemporyPassword(), value, PBKDF2Iterations));

																	if (!edit.commit()) {
																		throw new Exception("Error Saving Preferences");
																	}
																	else {
//																		Toast.makeText(application, R.string.toast_pin_saved, Toast.LENGTH_SHORT).show();
															        	Intent intent = new Intent(TOSActivity.this, MainActivity.class);
															        	intent.putExtra("first", true);
															        	intent.putExtra("secured", false);
																		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
															    		startActivity(intent);
																	}

																}
																else {
																	Toast.makeText(application, response.toString(), Toast.LENGTH_SHORT).show();
																}
													        } catch (Exception e) {
																Toast.makeText(application, e.getStackTrace().toString(), Toast.LENGTH_SHORT).show();
													            e.printStackTrace();
													        }
															//
															//
															//
													        
															Looper.loop();

													    }
													}).start();

													//
													//
													//
													application.checkIfWalletHasUpdated(password, guid, sharedKey, true, new SuccessCallback(){

														@Override
														public void onSuccess() {	
//															activity.registerNotifications();
															
															try {
																final String regId = GCMRegistrar.getRegistrationId(TOSActivity.this);

																if (regId == null || regId.equals("")) {
																	GCMRegistrar.register(TOSActivity.this, Constants.SENDER_ID);
																} else {
																	application.registerForNotificationsIfNeeded(regId);
																}

															} catch (Exception e) {
																e.printStackTrace();
															}

														}

														@Override
														public void onFail() {
															Toast.makeText(application, R.string.toast_error_syncing_wallet, Toast.LENGTH_SHORT).show();
														}
													});
												}
											});
										} else {
											throw new Exception("Error saving preferences");
										}
									} catch (Exception e) {
										e.printStackTrace();

										application.clearWallet();

										Toast.makeText(TOSActivity.this.getApplication(), e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
									}
								}
							});
						} catch (final Exception e) {
							e.printStackTrace();

							application.clearWallet();

							handler.post(new Runnable() {
								public void run() {
									progressDialog.dismiss();

									Toast.makeText(TOSActivity.this.getApplication(), e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
								}
							});
						}

					}
				}.start();

            }
        });

        Button btKO = ((Button)findViewById(R.id.ko));
        btKO.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
    			Intent intent = new Intent(TOSActivity.this, SetupActivity.class);
    			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
    			startActivity(intent);
            }
        });

    }
}

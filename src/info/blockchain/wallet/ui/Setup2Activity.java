package info.blockchain.wallet.ui;

import java.security.SecureRandom;
import java.util.regex.Pattern;

import org.json.simple.JSONObject;
import org.spongycastle.util.encoders.Hex;

import com.google.android.gcm.GCMRegistrar;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Patterns;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.CheckBox;
import android.widget.Toast;
import android.view.KeyEvent;
import android.view.View.OnKeyListener;
import android.text.method.KeyListener;
//import android.util.Log;

import piuk.EventListeners;
import piuk.MyWallet;
import piuk.blockchain.android.Constants;
import piuk.blockchain.android.R;
import piuk.blockchain.android.WalletApplication;
import piuk.blockchain.android.ui.AbstractWalletActivity;
import piuk.blockchain.android.ui.PinEntryActivity;
import piuk.blockchain.android.ui.SuccessCallback;

public class Setup2Activity extends Activity	{

	private Pattern emailPattern = Patterns.EMAIL_ADDRESS;
	public static final int PBKDF2Iterations = 2000;

    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	    setContentView(R.layout.setup2);

	    setRequestedOrientation (ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        final EditText pin1 = ((EditText)findViewById(R.id.pin1));
        final EditText pin2 = ((EditText)findViewById(R.id.pin2));
        final EditText pin3 = ((EditText)findViewById(R.id.pin3));
        final EditText pin4 = ((EditText)findViewById(R.id.pin4));
        final EditText pin2_1 = ((EditText)findViewById(R.id.pin2_1));
        final EditText pin2_2 = ((EditText)findViewById(R.id.pin2_2));
        final EditText pin2_3 = ((EditText)findViewById(R.id.pin2_3));
        final EditText pin2_4 = ((EditText)findViewById(R.id.pin2_4));

        pin1.setOnKeyListener(new OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if(pin1.getText().length() == 1)
                    pin2.requestFocus();
                return false;
            }
        });

        pin2.setOnKeyListener(new OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if(pin2.getText().length() == 1)
                    pin3.requestFocus();
                return false;
            }
        });

        pin3.setOnKeyListener(new OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if(pin3.getText().length() == 1)
                    pin4.requestFocus();
                return false;
            }
        });

        pin4.setOnKeyListener(new OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if(pin4.getText().length() == 1)
                    pin2_1.requestFocus();
                return false;
            }
        });

        pin2_1.setOnKeyListener(new OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if(pin2_1.getText().length() == 1)
                    pin2_2.requestFocus();
                return false;
            }
        });

        pin2_2.setOnKeyListener(new OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if(pin2_2.getText().length() == 1)
                    pin2_3.requestFocus();
                return false;
            }
        });

        pin2_3.setOnKeyListener(new OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if(pin2_3.getText().length() == 1)
                    pin2_4.requestFocus();
                return false;
            }
        });

        final EditText password1 = ((EditText)findViewById(R.id.password1));
        final EditText password2 = ((EditText)findViewById(R.id.password2));

        final EditText email = ((EditText)findViewById(R.id.email));
        final EditText mobile = ((EditText)findViewById(R.id.mobile));

        final CheckBox backups = ((CheckBox)findViewById(R.id.embackups));
        final CheckBox smsalerts = ((CheckBox)findViewById(R.id.smsalerts));

        final EditText label = ((EditText)findViewById(R.id.label));
        
    	((EditText)findViewById(R.id.password1)).setVisibility(View.GONE);
    	((EditText)findViewById(R.id.password2)).setVisibility(View.GONE);

        Button confirm = ((Button)findViewById(R.id.confirm));
        confirm.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {

//        		Toast.makeText(Setup2Activity.this, password1.getText().toString() + " " + password2.getText().toString(), Toast.LENGTH_LONG).show();

            	final String pw1 = password1.getText().toString();
            	final String pw2 = password2.getText().toString();
            	final String em = email.getText().toString();
            	final String pinCode1 = pin1.getText().toString() + pin2.getText().toString() + pin3.getText().toString() + pin4.getText().toString();
            	final String pinCode2 = pin2_1.getText().toString() + pin2_2.getText().toString() + pin2_3.getText().toString() + pin2_4.getText().toString();
				
				boolean doBackups = backups.isChecked();
				boolean doSMS = smsalerts.isChecked();

            	String firstLabel = label.getText().toString();

				if(pw1.length() < 11 || pw1.length() > 255 || pw1.length() < 11 || pw1.length() > 255) {
					Toast.makeText(Setup2Activity.this, R.string.new_account_password_length_error, Toast.LENGTH_LONG).show();
					return;
				}

				if(!pw1.equals(pw2)) {
					Toast.makeText(Setup2Activity.this, R.string.new_account_password_mismatch_error, Toast.LENGTH_LONG).show();
					return;
				}

				if(em.length() > 0 && !emailPattern.matcher(em).matches()) {
					Toast.makeText(Setup2Activity.this, R.string.new_account_password_invalid_email, Toast.LENGTH_LONG).show();
					return;
				}

				if(pinCode1.length() != 4 || pinCode2.length() != 4) {
					Toast.makeText(Setup2Activity.this, "PIN code(s) invalid", Toast.LENGTH_LONG).show();
					return;
				}

				if(!pinCode1.equals(pinCode2)) {
					Toast.makeText(Setup2Activity.this, "PIN codes do not match", Toast.LENGTH_LONG).show();
					return;
				}

				final ProgressDialog progressDialog = ProgressDialog.show(Setup2Activity.this, "", getString(R.string.creating_account), true);
				progressDialog.show();

				final Handler handler = new Handler();

				new Thread() {
					@Override
					public void run() {
						
						final WalletApplication application = (WalletApplication)Setup2Activity.this.getApplication();
						try {
							try {

								application.generateNewWallet();

							} catch (Exception e1) {

								throw new Exception("Error Generating Wallet");

							}

							final String guid = application.getRemoteWallet().getGUID();
							final String sharedKey = application.getRemoteWallet().getSharedKey();
							final String password = pw1;
							final String email = em;

							application.getRemoteWallet().setTemporyPassword(password);

							if (!application.getRemoteWallet().remoteSave(email)) {
								throw new Exception("Unknown Error Inserting wallet");
							}

							EventListeners.invokeWalletDidChange();

							handler.post(new Runnable() {
								public void run() {

									try {
										progressDialog.dismiss();

//										dismiss();

//										final AbstractWalletActivity activity = (AbstractWalletActivity) getActivity();
//										Toast.makeText(activity.getApplication(), R.string.new_account_success, Toast.LENGTH_LONG).show();
										Toast.makeText(Setup2Activity.this, R.string.new_account_success, Toast.LENGTH_LONG).show();

//										PinEntryActivity.clearPrefValues(application);

										final Editor edit = PreferenceManager.getDefaultSharedPreferences(application.getApplicationContext()).edit();

										Toast.makeText(application, guid, Toast.LENGTH_LONG).show();
										edit.putString("guid", guid);
										edit.putString("sharedKey", sharedKey);

										if (edit.commit()) {
											handler.post(new Runnable() {

												@Override
												public void run() {

//													Toast.makeText(application, "Saving PIN:" + pinCode1, Toast.LENGTH_SHORT).show();
													
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
																final JSONObject response = PinEntryActivity.apiStoreKey(key, value, pinCode1);
																if (response.get("success") != null) {
																	
																	edit.putString("pin_kookup_key", key);
																	edit.putString("encrypted_password", MyWallet.encrypt(application.getRemoteWallet().getTemporyPassword(), value, PBKDF2Iterations));

																	if (!edit.commit()) {
																		throw new Exception("Error Saving Preferences");
																	}
																	else {
																		Toast.makeText(application, R.string.toast_pin_saved, Toast.LENGTH_SHORT).show();	
															        	Intent intent = new Intent(Setup2Activity.this, MainActivity.class);
																		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
															    		startActivity(intent);
																	}

																}
																else {
																	Toast.makeText(application, response.toString(), Toast.LENGTH_LONG).show();
																}
													        } catch (Exception e) {
																Toast.makeText(application, e.toString(), Toast.LENGTH_LONG).show();
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
																final String regId = GCMRegistrar.getRegistrationId(Setup2Activity.this);

																if (regId == null || regId.equals("")) {
																	GCMRegistrar.register(Setup2Activity.this, Constants.SENDER_ID);
																} else {
																	application.registerForNotificationsIfNeeded(regId);
																}

															} catch (Exception e) {
																e.printStackTrace();
															}

														}

														@Override
														public void onFail() {
															Toast.makeText(application, R.string.toast_error_syncing_wallet, Toast.LENGTH_LONG).show();
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

										Toast.makeText(Setup2Activity.this.getApplication(), e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
									}
								}
							});
						} catch (final Exception e) {
							e.printStackTrace();

							application.clearWallet();

							handler.post(new Runnable() {
								public void run() {
									progressDialog.dismiss();

									Toast.makeText(Setup2Activity.this.getApplication(), e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
								}
							});
						}
						

					}
				}.start();

            }
        });

        //
        // prompt user with temporary password
        //
        final String generatedPassword = RandomPasswordGenerator.getInstance().getPassword();

		new AlertDialog.Builder(Setup2Activity.this)
			.setIcon(R.drawable.ic_launcher).setTitle(Setup2Activity.this.getString(R.string.app_name))
			.setMessage(
					"A password has been generated for you. The password is:\n\n" + generatedPassword + "\n\n" +
					"Take note of this password immediately. We cannot recover your password for you in the event of loss" + "\n\n" +
					"We advise you to change this password to one of your choosing, minimum 11 characters." + "\n\n" +
					"Do you want to create a new password?"
					)
			.setCancelable(false)
			.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				((EditText)findViewById(R.id.password1)).setVisibility(View.VISIBLE);
				((EditText)findViewById(R.id.password2)).setVisibility(View.VISIBLE);
				((EditText)findViewById(R.id.password1)).requestFocus();
			}
			})
			.setNegativeButton("No", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				((EditText)findViewById(R.id.password1)).setVisibility(View.VISIBLE);
				((EditText)findViewById(R.id.password2)).setVisibility(View.VISIBLE);
				password1.setText(generatedPassword);
				password2.setText(generatedPassword);
				((EditText)findViewById(R.id.pin1)).requestFocus();
			}
			}
		).show();

    }
}

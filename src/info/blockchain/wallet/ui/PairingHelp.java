package info.blockchain.wallet.ui;

import java.security.SecureRandom;
import java.util.regex.Pattern;

import net.sourceforge.zbar.Symbol;

import org.json.simple.JSONObject;
import org.spongycastle.util.encoders.Hex;

import com.dm.zbar.android.scanner.ZBarConstants;
import com.dm.zbar.android.scanner.ZBarScannerActivity;
import com.google.android.gcm.GCMRegistrar;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.provider.Browser;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ActivityInfo;
import android.support.v4.content.LocalBroadcastManager;
import android.text.InputType;
//import android.util.Log;
import android.util.Patterns;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ToggleButton;
import android.view.View.OnClickListener;
import android.graphics.Rect;
import android.widget.Toast;

import piuk.blockchain.android.Constants;
import piuk.blockchain.android.MyRemoteWallet;
import piuk.blockchain.android.MyWallet;
import piuk.blockchain.android.R;
import piuk.blockchain.android.SuccessCallback;
import piuk.blockchain.android.WalletApplication;

public class PairingHelp extends Activity {
	
	private TextView tvHeader = null;
	private TextView tvFooter1 = null;
	private TextView tvFooter2 = null;
	private TextView tvWarning1 = null;
	private TextView tvWarning2 = null;
	private TextView tvBack = null;
	private TextView tvNext = null;
	private ImageView ivImage = null;
	private LinearLayout layoutScan = null;
	private LinearLayout layoutManual = null;
	
	private int level = 0;

	private static int MANUAL_PAIRING = 1;
	private static int ZBAR_SCANNER_REQUEST = 2;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

	    this.requestWindowFeature(Window.FEATURE_NO_TITLE);
//	    this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setContentView(R.layout.activity_pairing_help);

	    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
	    
        Bundle extras = getIntent().getExtras();
        if(extras != null)	{
        	level = extras.getInt("level");
        }

//		prefs = PreferenceManager.getDefaultSharedPreferences(this);

		ivImage = (ImageView)findViewById(R.id.img);
        if(level == 2)	{
        	ivImage.setImageResource(R.drawable.pairing3);
        }
        else if(level == 1)	{
        	ivImage.setImageResource(R.drawable.pairing2);
        }
        else	{
        	ivImage.setImageResource(R.drawable.pairing1);
        }

		tvHeader = (TextView)findViewById(R.id.header);
		tvHeader.setTypeface(TypefaceUtil.getInstance(this).getGravityLightTypeface());
		tvHeader.setText("connect an existing wallet");

		tvFooter1 = (TextView)findViewById(R.id.footer1);
		tvFooter1.setText("SCAN PAIRING CODE");
		tvFooter2 = (TextView)findViewById(R.id.footer2);
		tvFooter2.setText("PAIR DEVICE MANUALLY");
		
		tvWarning1 = (TextView)findViewById(R.id.warning1);
		tvWarning2 = (TextView)findViewById(R.id.warning2);
		tvWarning2.setTextColor(0xFF039BD3);
        if(level == 2)	{
    		tvWarning1.setText("Step 3");
    		tvWarning2.setText("Select 'Devices' on the left menu of the settings page and click 'Show/Hide Pairing Code'");
        }
        else if(level == 1)	{
    		tvWarning1.setText("Step 2");
    		tvWarning2.setText("On your wallet homepage click the Account Settings button");
        }
        else	{
    		tvWarning1.setText("Step 1");
    		tvWarning2.setText("Login to your Blockchain Wallet on the web by visiting blockchain.info/wallet in your web browser");
        }
        
		layoutScan = (LinearLayout)findViewById(R.id.scan);
		layoutScan.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View arg0, MotionEvent arg1) {
        		Intent intent = new Intent(PairingHelp.this, ZBarScannerActivity.class);
        		intent.putExtra(ZBarConstants.SCAN_MODES, new int[]{ Symbol.QRCODE } );
        		startActivityForResult(intent, ZBAR_SCANNER_REQUEST);
                return false;
            }
        });

		layoutManual = (LinearLayout)findViewById(R.id.manual);
		layoutManual.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View arg0, MotionEvent arg1) {
         	 	Intent intent = new Intent(PairingHelp.this, ManualPairing.class);
//         	 	startActivityForResult(intent, MANUAL_PAIRING);            	
         	 	startActivity(intent);            	
                return false;
            }
        });

		if(level == 0) {
			;
		}
		else {
			tvBack = (TextView)findViewById(R.id.back);
			tvBack.setText("<");
			tvBack.setOnTouchListener(new OnTouchListener() {
	            @Override
	            public boolean onTouch(View arg0, MotionEvent arg1) {
            	 	Intent intent = new Intent(PairingHelp.this, PairingHelp.class);
            	 	intent.putExtra("level", --level);
            	 	startActivity(intent);            	
	                return false;
	            }
	        });
		}

		if(level == 2) {
			;
		}
		else {
			tvNext = (TextView)findViewById(R.id.next);
			tvNext.setText(">");
			tvNext.setOnTouchListener(new OnTouchListener() {
	            @Override
	            public boolean onTouch(View arg0, MotionEvent arg1) {
            	 	Intent intent = new Intent(PairingHelp.this, PairingHelp.class);
            	 	intent.putExtra("level", ++level);
            	 	startActivity(intent);            	
	                return false;
	            }
	        });
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		
		if(resultCode == Activity.RESULT_OK && requestCode == ZBAR_SCANNER_REQUEST)	{
			if(data != null && data.getStringExtra(ZBarConstants.SCAN_RESULT) != null)	{
				String strResult = data.getStringExtra(ZBarConstants.SCAN_RESULT);
	        	handleQRCode(strResult);
			}
        }
		/*
		else if(resultCode == Activity.RESULT_OK && requestCode == MANUAL_PAIRING) {
			if(data != null && data.getAction() != null)	{
				String res = data.getAction();
				String uuid = res.substring(0, 36);
				String pw = res.substring(36);
//				Toast.makeText(this, "Wallet identifier:" + uuid, Toast.LENGTH_SHORT).show();
//				Toast.makeText(this, "Password:" + pw, Toast.LENGTH_SHORT).show();
//				pairManually(uuid, pw);
			}
        }
        */
        else {
        	;
        }

	}

	public void handleQRCode(String raw_code) {
		final WalletApplication application = (WalletApplication) getApplication();
		
		try {
			if (raw_code == null || raw_code.length() == 0) {
				throw new Exception("Invalid Pairing QR Code");
			}

			if (raw_code.charAt(0) != '1') {
				throw new Exception("Invalid Pairing Version Code " + raw_code.charAt(0));
			}

			final Handler handler = new Handler();

			{
				String[] components = raw_code.split("\\|", Pattern.LITERAL);

				if (components.length < 3) {
					throw new Exception("Invalid Pairing QR Code. Not enough components.");
				}

				final String guid = components[1];
				if (guid.length() != 36) {
					throw new Exception("Invalid Pairing QR Code. GUID wrong length.");
				}

				final String encrypted_data = components[2];

				new Thread(new Runnable() {

					@Override
					public void run() {
						
						Looper.prepare();

						try {
							String temp_password = MyRemoteWallet.getPairingEncryptionPassword(guid);

							String decrypted = MyWallet.decrypt(encrypted_data, temp_password, MyWallet.DefaultPBKDF2Iterations);

							String[] sharedKeyAndPassword = decrypted.split("\\|", Pattern.LITERAL);

							if (sharedKeyAndPassword.length < 2) {
								throw new Exception("Invalid Pairing QR Code. sharedKeyAndPassword Incorrect number of components.");
							}

							final String sharedKey = sharedKeyAndPassword[0];
							if (sharedKey.length() != 36) {
								throw new Exception("Invalid Pairing QR Code. sharedKey wrong length.");
							}

							final String password = new String(Hex.decode(sharedKeyAndPassword[1]), "UTF-8");
//							Toast.makeText(application, password, Toast.LENGTH_LONG).show();

							application.clearWallet();

//							PinEntryActivity.clearPrefValues(application);

							Editor edit = PreferenceManager.getDefaultSharedPreferences(PairingHelp.this).edit();

							edit.putString("guid", guid);
							edit.putString("sharedKey", sharedKey);

							edit.commit();

							handler.post(new Runnable() {

								@Override
								public void run() {
									application.checkIfWalletHasUpdated(password, guid, sharedKey, true, new SuccessCallback(){

										@Override
										public void onSuccess() {	
//											registerNotifications();

									        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(PairingHelp.this);
											Editor edit = prefs.edit();
											edit.putBoolean("validated", true);
											edit.putBoolean("paired", true);
											edit.commit();

											try {
												final String regId = GCMRegistrar.getRegistrationId(PairingHelp.this);
												if (regId == null || regId.equals("")) {
													GCMRegistrar.register(PairingHelp.this, Constants.SENDER_ID);
												} else {
													application.registerForNotificationsIfNeeded(regId);
												}
											} catch (Exception e) {
												e.printStackTrace();
											}

								        	Intent intent = new Intent(PairingHelp.this, PinEntryActivity.class);
								        	intent.putExtra("S", "1");
											intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
								    		startActivity(intent);

											finish();
										}

										@Override
										public void onFail() {
											finish();

											Toast.makeText(application, R.string.toast_error_syncing_wallet, Toast.LENGTH_LONG).show();
										}
									});
								}
							});

						} catch (final Exception e) {
							e.printStackTrace();

							handler.post(new Runnable() {
								public void run() {

									Toast.makeText(application, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();

									e.printStackTrace();

									application.writeException(e);
								}
							});
						}
						
						Looper.loop();

					}
				}).start();
			}
		} catch (Exception e) {

			Toast.makeText(application, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();

			e.printStackTrace();

			application.writeException(e);
		}
		
	}
/*
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

								if (wallet == null)
									return;
								
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

								        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(PairingHelp.this);
										Editor edit = prefs.edit();
										edit.putBoolean("validated", true);
										edit.putBoolean("paired", true);
										edit.commit();

							        	Intent intent = new Intent(PairingHelp.this, PinEntryActivity.class);
							        	intent.putExtra("S", "1");
										intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
							    		startActivity(intent);

										finish();
									}

									@Override
									public void onFail() {
										finish();
										Toast.makeText(application, R.string.error_pairing_wallet, Toast.LENGTH_LONG).show();
									}
								});
							} catch (final Exception e) {
//								Toast.makeText(application, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
								Toast.makeText(application, R.string.error_pairing_wallet, Toast.LENGTH_LONG).show();

								application.writeException(e);

								finish();
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

							finish();
						}
					});
				}
			}
		}).start();
	}
*/
}

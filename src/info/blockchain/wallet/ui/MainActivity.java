package info.blockchain.wallet.ui;

import net.sourceforge.zbar.Symbol;

import com.dm.zbar.android.scanner.ZBarConstants;
import com.dm.zbar.android.scanner.ZBarScannerActivity;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.app.ActionBar.Tab;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.provider.ContactsContract;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Gravity;
import android.view.View.OnTouchListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.Toast;

//import android.util.Log;

import piuk.blockchain.android.R;

@SuppressLint("NewApi")
public class MainActivity extends FragmentActivity implements ActionBar.TabListener {

    private static int PIN_ENTRY_ACTIVITY 	= 1;
    private static int SETUP_ACTIVITY	 	= 2;
    private static int ABOUT_ACTIVITY 		= 3;
    private static int PICK_CONTACT 		= 4;
    private static int NEARBY_MERCHANTS 	= 5;

	private ViewPager viewPager;
    private TabsPagerAdapter mAdapter;
    private ActionBar actionBar;

    private String[] tabs = null;

	private static int ZBAR_SCANNER_REQUEST = 2026;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	    setContentView(info.blockchain.wallet.ui.R.layout.activity_main);

	    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

	    tabs = new String[3];
	    tabs[0] = "Send";
	    tabs[1] = "Balance";
	    tabs[2] = "Receive";

        viewPager = (ViewPager) findViewById(info.blockchain.wallet.ui.R.id.pager);
        mAdapter = new TabsPagerAdapter(getSupportFragmentManager());

        viewPager.setAdapter(mAdapter);

        actionBar = getActionBar();
        actionBar.hide();

        //
        // masthead logo placement
        //
//        actionBar.setTitle("");
        actionBar.setDisplayOptions(actionBar.getDisplayOptions() ^ ActionBar.DISPLAY_SHOW_TITLE);
        actionBar.setLogo(R.drawable.masthead);
        actionBar.setHomeButtonEnabled(false);
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        actionBar.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#FF1B8AC7")));
        //
        // QR code logo placement (righthand side)
        //
        actionBar.setDisplayOptions(actionBar.getDisplayOptions() | ActionBar.DISPLAY_SHOW_CUSTOM);
        ImageView qr_icon = new ImageView(actionBar.getThemedContext());
        qr_icon.setImageResource(R.drawable.top_camera_icon);
        qr_icon.setScaleType(ImageView.ScaleType.FIT_XY);
        ActionBar.LayoutParams layoutParams = new ActionBar.LayoutParams(ActionBar.LayoutParams.WRAP_CONTENT, ActionBar.LayoutParams.WRAP_CONTENT, Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        layoutParams.height = 72;
        layoutParams.width = 72;
        layoutParams.rightMargin = 5;
        qr_icon.setLayoutParams(layoutParams);
        qr_icon.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
            	
        		Intent intent = new Intent(MainActivity.this, ZBarScannerActivity.class);
        		intent.putExtra(ZBarConstants.SCAN_MODES, new int[]{ Symbol.QRCODE } );
        		startActivityForResult(intent, ZBAR_SCANNER_REQUEST);

        		return false;
            }
        });
        actionBar.setCustomView(qr_icon);
        //
        actionBar.show();
                
        for (String tab : tabs) {
            actionBar.addTab(actionBar.newTab().setText(tab).setTabListener(this));

            viewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
     
                @Override
                public void onPageSelected(int position) {
                    actionBar.setSelectedNavigationItem(position);
                }
     
                @Override
                public void onPageScrolled(int arg0, float arg1, int arg2) { ; }
     
                @Override
                public void onPageScrollStateChanged(int arg0) { ; }
            });
        }
        
        viewPager.setCurrentItem(1);

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    // Handle item selection
	    switch (item.getItemId()) {
    	case R.id.action_settings:
        	Intent intent = new Intent(MainActivity.this, SetupActivity.class);
    		startActivityForResult(intent, SETUP_ACTIVITY);
    		return true;
    	case R.id.exchange_rates:
    		doExchangeRates();
    		return true;
    	/*	
    	case R.id.nearby_merchants:
			Intent intent3 = new Intent(MainActivity.this, MapActivity.class);
			startActivity(intent3);
    		return true;
    	*/
    	case R.id.send_to_friends:
    		doSend2Friends();
    		return true;
    	case R.id.action_about:
    		doAbout();
    		return true;
    	case R.id.action_old_wallet:
			Intent intent2 = new Intent(MainActivity.this, piuk.blockchain.android.ui.WalletActivity.class);
			intent2.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(intent2);
    		return true;
	    default:
	        return super.onOptionsItemSelected(item);
	    }
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if(resultCode == Activity.RESULT_OK && requestCode == ZBAR_SCANNER_REQUEST)	{

			String strResult = BitcoinAddressCheck.clean(data.getStringExtra(ZBarConstants.SCAN_RESULT));
//        	Log.d("Scan result", strResult);
			if(BitcoinAddressCheck.isValid(BitcoinAddressCheck.clean(strResult))) {
				Toast.makeText(this, strResult, Toast.LENGTH_LONG).show();

				/*
		        viewPager.setCurrentItem(0);
				SendFragment sf = (SendFragment)getSupportFragmentManager().findFragmentById(R.id.SendFragment);
			    sf.doQRScan(strResult);
			    */

			}
			else {
				Toast.makeText(this, "Invalid address", Toast.LENGTH_LONG).show();
			}

        }
		else if(resultCode == Activity.RESULT_CANCELED && requestCode == ZBAR_SCANNER_REQUEST) {
//            Toast.makeText(this, R.string.camera_unavailable, Toast.LENGTH_SHORT).show();
		}
		else {
			;
		}
		
	}

	@Override
    public void onTabReselected(Tab tab, FragmentTransaction ft) { ; }
 
    @Override
    public void onTabSelected(Tab tab, FragmentTransaction ft) { viewPager.setCurrentItem(tab.getPosition()); }
 
    @Override
    public void onTabUnselected(Tab tab, FragmentTransaction ft) { ; }

    private void doExchangeRates()	{
        if(hasZeroBlock())	{
            Intent intent = getPackageManager().getLaunchIntentForPackage(BlockchainUtil.ZEROBLOCK_PACKAGE);
            startActivity(intent);
        }
        else	{
        	Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + BlockchainUtil.ZEROBLOCK_PACKAGE));
        	startActivity(intent);
        }
    }

    private boolean hasZeroBlock()	{
    	PackageManager pm = this.getPackageManager();
    	try	{
    		pm.getPackageInfo(BlockchainUtil.ZEROBLOCK_PACKAGE, 0);
    		return true;
    	}
    	catch(NameNotFoundException nnfe)	{
    		return false;
    	}
    }

    private void doAbout()	{
    	Intent intent = new Intent(MainActivity.this, AboutActivity.class);
		startActivityForResult(intent, ABOUT_ACTIVITY);
    }

    private void doSend2Friends()	{
    	Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
    	intent.setType(ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE);
//    	intent.setType(ContactsContract.CommonDataKinds.Email.CONTENT_TYPE);
    	startActivityForResult(intent, PICK_CONTACT);
    }
    
	public final void toast(final int textResId, final Object... formatArgs) {
		toast(textResId, 0, Toast.LENGTH_SHORT, formatArgs);
	}

	public final void longToast(final int textResId, final Object... formatArgs) {
		toast(textResId, 0, Toast.LENGTH_LONG, formatArgs);
	}
	
	public final void toast(final String text, final Object... formatArgs) {
		toast(text, 0, Toast.LENGTH_SHORT, formatArgs);
	}

	public final void longToast(final String text, final Object... formatArgs) {
		toast(text, 0, Toast.LENGTH_LONG, formatArgs);
	}
/*
	public void validatePIN(final WalletApplication application, final String PIN) {

		final Handler handler = new Handler();

		final Activity activity = this;

		new Thread(new Runnable() {
			public void run() {

				String pin_lookup_key = PreferenceManager.getDefaultSharedPreferences(application).getString("pin_kookup_key", null);
				String encrypted_password = PreferenceManager.getDefaultSharedPreferences(application).getString("encrypted_password", null);

				try {
					final JSONObject response = PinEntryActivity.apiGetValue(pin_lookup_key, PIN);

					String decryptionKey = (String) response.get("success");
					if (decryptionKey != null) {	
						application.didEncounterFatalPINServerError = false;

						String password = MyWallet.decrypt(encrypted_password, decryptionKey, PinEntryActivity.PBKDF2Iterations);

						application.checkIfWalletHasUpdatedAndFetchTransactions(password, new SuccessCallback() {
							@Override
							public void onSuccess() {
								handler.post(new Runnable() {
									public void run() {															
										Toast.makeText(application, "PIN Verified", Toast.LENGTH_SHORT).show();	

//										disableKeyPad(false);

										if (application.needsWalletRekey()) {
											RekeyWalletDialog.show(getSupportFragmentManager(), application, new SuccessCallback() {
												@Override
												public void onSuccess() {													
//													finish();
												}

												@Override
												public void onFail() {													
//													finish();
												}
											});
										} else {
//											finish();
										}
									}
								});
							}

							@Override
							public void onFail() {
								handler.post(new Runnable() {
									public void run() {
//										disableKeyPad(false);

										Toast.makeText(application, R.string.toast_wallet_decryption_failed, Toast.LENGTH_LONG).show();	

										try {
//											clearPrefValues(application);
											
											Editor editor = PreferenceManager.getDefaultSharedPreferences(application).edit();

											editor.remove("pin_kookup_key");
											editor.remove("encrypted_password");

											if (!editor.commit()) {
												throw new Exception("Error Saving Preferences");
											}

										} catch (Exception e) {
											e.printStackTrace();
										}

//										begin();
									}
								});
							}
						});
					} else if (response.get("error") != null) {

						//Even though we received an error it is a valid response
						//So no fatal
						application.didEncounterFatalPINServerError = false;

						//"code" == 2 means the PIN is incorrect
						if (!response.containsKey("code") || ((Number)response.get("code")).intValue() != 2) {
//							clearPrefValues(application);
							
							Editor editor = PreferenceManager.getDefaultSharedPreferences(application).edit();

							editor.remove("pin_kookup_key");
							editor.remove("encrypted_password");

							if (!editor.commit()) {
								throw new Exception("Error Saving Preferences");
							}

						}

						handler.post(new Runnable() {
							public void run() {
//								disableKeyPad(false);

								Toast.makeText(application, (String)response.get("error"), Toast.LENGTH_SHORT).show();	

//								begin();
							}
						});
					} else {
						throw new Exception("Unknown Error");
					}
				} catch (final Exception e) {
					e.printStackTrace();

					application.didEncounterFatalPINServerError = true;

					handler.post(new Runnable() {
						public void run() {
							try {
//								disableKeyPad(false);

								AlertDialog.Builder builder = new AlertDialog.Builder(activity);

								builder.setCancelable(false);

								builder.setMessage(R.string.pin_server_error_description);

								builder.setTitle(R.string.pin_server_error);

								builder.setPositiveButton(R.string.try_again, new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog, int id) {
										validatePIN(application, PIN);

										dialog.dismiss();

//										begin();
									}
								});
								builder.setNegativeButton(R.string.pin_server_error_enter_password_manually, new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog, int id) {
										dialog.dismiss();

										RequestPasswordDialog.show(
												getSupportFragmentManager(),
												new SuccessCallback() {  
													public void onSuccess() {
														finish();
													}
													public void onFail() {	
														Toast.makeText(application, R.string.password_incorrect, Toast.LENGTH_LONG).show();

//														begin();
													}
												}, RequestPasswordDialog.PasswordTypeMain);

									}
								});

								AlertDialog dialog = builder.create();

								dialog.show();

//								begin();
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
					});
				}
			}
		}).start();

	}
*/
}

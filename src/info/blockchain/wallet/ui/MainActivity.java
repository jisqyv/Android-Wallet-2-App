package info.blockchain.wallet.ui;

import java.security.Security;
//import java.security.Provider;
import java.util.Locale;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences.Editor;
import android.content.pm.ActivityInfo;
import android.app.ActionBar.Tab;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.view.KeyEvent;
//import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Gravity;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.view.View.OnTouchListener;
import android.widget.ImageView;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.widget.DrawerLayout;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import android.os.StrictMode;
import android.os.Build.VERSION;
//import android.util.Log;

import info.blockchain.wallet.ui.SendFragment;
import piuk.blockchain.android.R;
//import piuk.blockchain.android.SharedCoin;
import piuk.blockchain.android.WalletApplication;

import net.sourceforge.zbar.Symbol;

import com.dm.zbar.android.scanner.ZBarConstants;
import com.dm.zbar.android.scanner.ZBarScannerActivity;

@SuppressLint("NewApi")
public class MainActivity extends FragmentActivity implements ActionBar.TabListener, SendFragment.OnCompleteListener {

    private static int ABOUT_ACTIVITY 		= 1;
    private static int PICK_CONTACT 		= 2;
    private static int SETTINGS_ACTIVITY	= 3;
    private static int ADDRESSBOOK_ACTIVITY	= 4;
    private static int MERCHANT_ACTIVITY	= 5;

	private ViewPager viewPager = null;
    private TabsPagerAdapter mAdapter = null;
    private ActionBar actionBar = null;

	private boolean isDrawerOpen = false;

    private String[] tabs = null;

	private static int ZBAR_SCANNER_REQUEST = 2026;

	long lastMesssageTime = 0;

	private WalletApplication application;
	
	private boolean returningFromActivity = false;
	
	public static final String INTENT_EXTRA_ADDRESS = "address";

	private String strUri = null;
	
	//
	//
	//
	private DrawerLayout mDrawerLayout = null;
	private ListView mDrawerList = null;
	private ActionBarDrawerToggle mDrawerToggle = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
	    setContentView(R.layout.activity_main);
	    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
	    
    	Locale locale = new Locale("en", "US");
        Locale.setDefault(locale);
        
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy); 

        boolean isFirst = false;
        boolean isSecured = false;
        boolean isDismissed = false;
        Bundle extras = getIntent().getExtras();
        if(extras != null)	{
        	isFirst = extras.getBoolean("first");
        	isDismissed = extras.getBoolean("dismissed");
        	strUri = extras.getString("INTENT_URI");
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean isValidated = false;
        isValidated = prefs.getBoolean("validated", false);
    	isSecured = prefs.getBoolean("PWSecured", false) && prefs.getBoolean("EmailBackups", false) ? true : false;
        boolean isPaired = prefs.getBoolean("paired", false);
        boolean isVirgin = prefs.getBoolean("virgin", false);

        if(isValidated || isSecured || isDismissed || isPaired || !isVirgin) {
        	;
        }
        else if(!isSecured && isFirst) {
			Editor edit = PreferenceManager.getDefaultSharedPreferences(this).edit();
			edit.putBoolean("first", false);
			edit.commit();

			Intent intent = new Intent(this, SecureWallet.class);
			intent.putExtra("first", true);
			startActivity(intent);
        }
        else if(!isSecured && !isFirst) {
			Intent intent = new Intent(this, SecureWallet.class);
			intent.putExtra("first", false);
			startActivity(intent);
        }
        else {
			Intent intent = new Intent(this, SetupActivity.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(intent);
        }

	    tabs = new String[3];
	    tabs[0] = "Send";
	    tabs[1] = "Balance";
	    tabs[2] = "Receive";

        viewPager = (ViewPager) findViewById(R.id.pager);
        mAdapter = new TabsPagerAdapter(getSupportFragmentManager());
        viewPager.setAdapter(mAdapter);

        actionBar = getActionBar();
        actionBar.hide();

        //
        // masthead logo placement
        //
//        actionBar.setTitle("");
        actionBar.setDisplayOptions(actionBar.getDisplayOptions() | ActionBar.DISPLAY_SHOW_CUSTOM);
        
        LinearLayout layout_icons = new LinearLayout(actionBar.getThemedContext());
        ActionBar.LayoutParams layoutParams = new ActionBar.LayoutParams(ActionBar.LayoutParams.WRAP_CONTENT, ActionBar.LayoutParams.WRAP_CONTENT, Gravity.RIGHT | Gravity.CENTER_VERTICAL);
	    if(!DeviceUtil.getInstance(this).isSmallScreen()) {
	        layoutParams.height = 72;
	    }
	    else {
	        layoutParams.height = 30;
	    }
        layoutParams.width = (layoutParams.height * 2) + 30 + 60;
        layout_icons.setLayoutParams(layoutParams);
        layout_icons.setOrientation(LinearLayout.HORIZONTAL);

        ActionBar.LayoutParams imgParams = new ActionBar.LayoutParams(ActionBar.LayoutParams.WRAP_CONTENT, ActionBar.LayoutParams.WRAP_CONTENT, Gravity.CENTER_VERTICAL);
        imgParams.height = layoutParams.height;
        imgParams.width = layoutParams.height;
        imgParams.rightMargin = 30;

        final ImageView qr_icon = new ImageView(actionBar.getThemedContext());
        qr_icon.setImageResource(R.drawable.top_camera_icon);
        qr_icon.setScaleType(ImageView.ScaleType.FIT_XY);
        qr_icon.setLayoutParams(imgParams);
        qr_icon.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

				mDrawerLayout.closeDrawer(mDrawerList);
				actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        		Intent intent = new Intent(MainActivity.this, ZBarScannerActivity.class);
        		intent.putExtra(ZBarConstants.SCAN_MODES, new int[] { Symbol.QRCODE } );
        		startActivityForResult(intent, ZBAR_SCANNER_REQUEST);

        		return false;
            }
        });

        application = WalletUtil.getInstance(this).getWalletApplication();
        
        final ImageView refresh_icon = new ImageView(actionBar.getThemedContext());
        refresh_icon.setImageResource(R.drawable.refresh_icon);
        refresh_icon.setScaleType(ImageView.ScaleType.FIT_XY);
        refresh_icon.setLayoutParams(imgParams);
        refresh_icon.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
				Toast.makeText(MainActivity.this, R.string.refreshing, Toast.LENGTH_LONG).show();
        		try {
            		WalletUtil.getInstance(MainActivity.this).getWalletApplication().doMultiAddr(false, null);
        		}
        		catch(Exception e) {
            		Toast.makeText(MainActivity.this, e.toString(), Toast.LENGTH_SHORT).show();
        		}

        		return false;
            }
        });
        
        LinearLayout filler_layout = new LinearLayout(actionBar.getThemedContext());
        ActionBar.LayoutParams fillerParams = new ActionBar.LayoutParams(ActionBar.LayoutParams.WRAP_CONTENT, ActionBar.LayoutParams.WRAP_CONTENT, Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        fillerParams.height = 72;
        fillerParams.width = 60;
        filler_layout.setLayoutParams(fillerParams);
        
        layout_icons.addView(refresh_icon);
        layout_icons.addView(filler_layout);
        layout_icons.addView(qr_icon);

        if(android.os.Build.VERSION.SDK_INT >= 21)	{
        	actionBar.setDisplayOptions(actionBar.getDisplayOptions() | ActionBar.DISPLAY_SHOW_TITLE);
        }
        else	{
        	actionBar.setDisplayOptions(actionBar.getDisplayOptions() ^ ActionBar.DISPLAY_SHOW_TITLE);
        }
        actionBar.setDisplayUseLogoEnabled(true);
        actionBar.setLogo(R.drawable.masthead);
        actionBar.setHomeButtonEnabled(false);
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
//        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#FF1B8AC7")));
        
        actionBar.setCustomView(layout_icons);
        //
        actionBar.show();
        
        //
        //
        //
		mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
		mDrawerList = (ListView) findViewById(R.id.drawer_list);
		mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, R.drawable.ic_drawer, R.string.drawer_open, R.string.drawer_close) {
			
			public void onDrawerClosed(View view) {
				getActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
			    invalidateOptionsMenu();
				isDrawerOpen = false;
			}

			public void onDrawerOpened(View view) {
				getActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
			    invalidateOptionsMenu();
				isDrawerOpen = true;
			    }

			public void onDrawerSlide(View drawerView, float slideOffset) {
				if(isDrawerOpen) {
	                if(slideOffset < .99)	{
	    				getActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
	                }
				}
				else {
	                if(slideOffset > .01)	{
	    				getActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
	                }
				}
            }
			
			};

		// hide settings menu
//		invalidateOptionsMenu();

		mDrawerLayout.setDrawerListener(mDrawerToggle);
//		ArrayAdapter<String> adapter = new ArrayAdapter<String>(getBaseContext(), R.layout.drawer_list_item, getResources().getStringArray(R.array.menus));
		NavDrawerListAdapter adapter = new NavDrawerListAdapter(getBaseContext());
		mDrawerList.setAdapter(adapter);
		actionBar.setHomeButtonEnabled(true);
		actionBar.setDisplayHomeAsUpEnabled(true);
		mDrawerList.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

				if(position == 2) {
					doMerchantDirectory();
				}
				else if(position == 3) {
					doAddressBook();
				}
				else if(position == 4) {
					doExchangeRates();
				}
				else if(position == 5) {
					doSettings();
				}
				else {
					;
				}
				
				if(position > 1) {
					// Closing the drawer
					mDrawerLayout.closeDrawer(mDrawerList);

					getActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
				    invalidateOptionsMenu();
				}

			}
		});

        for (String tab : tabs) {
            actionBar.addTab(actionBar.newTab().setText(tab).setTabListener(this));
        }

        viewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {

            @Override
            public void onPageSelected(int position) {
//				mDrawerLayout.closeDrawer(mDrawerList);
				actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
                actionBar.setSelectedNavigationItem(position);

                if(position == 1) {
                    refresh_icon.setVisibility(View.VISIBLE);
                }
                else {
                    refresh_icon.setVisibility(View.INVISIBLE);
                }
            }

            @Override
            public void onPageScrolled(int arg0, float arg1, int arg2) { ; }

            @Override
            public void onPageScrollStateChanged(int arg0) { ; }
        });

        viewPager.setCurrentItem(1);
        
//        BlockchainUtil.getInstance(this);
        
/*
        
							application.sharedCoinGetInfo(new SuccessCallback() {

								public void onSuccess() {			
									SharedCoin sharedCoin = application.getSharedCoin();
					                Log.d("SharedCoin", "SharedCoin getInfo: onSuccess ");
					                Log.d("SharedCoin", "SharedCoin getInfo isEnabled " + sharedCoin.isEnabled());
					                Log.d("SharedCoin", "SharedCoin getInfo getFeePercent " + sharedCoin.getFeePercent());
					                Log.d("SharedCoin", "SharedCoin getInfo getMaximumInputValue " + sharedCoin.getMaximumInputValue());
					                Log.d("SharedCoin", "SharedCoin getInfo getMaximumOfferNumberOfInputs " + sharedCoin.getMaximumOfferNumberOfInputs());
					                Log.d("SharedCoin", "SharedCoin getInfo getMaximumOfferNumberOfOutputs " + sharedCoin.getMaximumOfferNumberOfOutputs());
					                Log.d("SharedCoin", "SharedCoin getInfo getMaximumOutputValue " + sharedCoin.getMaximumOutputValue());
					                Log.d("SharedCoin", "SharedCoin getInfo getMinSupportedVersion " + sharedCoin.getMinSupportedVersion());
					                Log.d("SharedCoin", "SharedCoin getInfo getMinimumFee " + sharedCoin.getMinimumFee());
					                Log.d("SharedCoin", "SharedCoin getInfo getMinimumInputValue " + sharedCoin.getMinimumInputValue());
					                Log.d("SharedCoin", "SharedCoin getInfo getMinimumOutputValue " + sharedCoin.getMinimumOutputValue());
					                Log.d("SharedCoin", "SharedCoin getInfo getMinimumOutputValueExcludeFee " + sharedCoin.getMinimumOutputValueExcludeFee());
					                Log.d("SharedCoin", "SharedCoin getInfo getRecommendedIterations " + sharedCoin.getRecommendedIterations());
					                Log.d("SharedCoin", "SharedCoin getInfo getRecommendedMaxIterations " + sharedCoin.getRecommendedMaxIterations());
					                Log.d("SharedCoin", "SharedCoin getInfo getRecommendedMinIterations " + sharedCoin.getRecommendedMinIterations());
					                Log.d("SharedCoin", "SharedCoin getInfo getToken " + sharedCoin.getToken());

					                if (sharedCoin.isEnabled()) {

					                	Log.d("SharedCoin", "is enabled");

					                    List<String> fromAddresses = new ArrayList<String>();
					                    fromAddresses.add("1NrMxHrinbQsEo5N7MvfMmo3skhEyH5TrK");
					                    String toAddress = "1FoNEBtcqSA9k7iXqvoEPZnQi7FvDrmpEp";
					                    BigInteger amount =  new BigInteger("1000000");
					                    application.sendSharedCoin(fromAddresses, toAddress, amount);
					            		
					                	List<String> shared_coin_seeds = new ArrayList<String>();
					            		shared_coin_seeds.add("sharedcoin-seed:a43790c285abb25bf80ed0008f1abbe1738f");	
					            		//application.sharedCoinRecoverSeeds(shared_coin_seeds);

					                }
								}
								
								public void onFail() {			
					                Log.d("SharedCoin", "SharedCoin getInfo: onFail ");						
								}
							});            	
*/        

	}
	
	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		mDrawerToggle.syncState();
	}

	@Override
	public void onComplete() {
		handleNavigateTo();
		
        if(strUri != null)	{
//			Toast.makeText(MainActivity.this, strUri, Toast.LENGTH_LONG).show();
			Intent intent = new Intent("info.blockchain.wallet.ui.SendFragment.BTC_ADDRESS_SCAN");
		    intent.putExtra("BTC_ADDRESS", strUri);
		    LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
		    intent = null;
		    strUri = null;
		    new android.os.Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    viewPager.setCurrentItem(0, true);
                }
            }, 1000);
        }

	}

	void handleNavigateTo() {
		Intent intent = getIntent();
		String navigateTo = intent.getStringExtra("navigateTo");
		if (navigateTo != null) {
			if (navigateTo.equals("merchantDirectory")) {
				doMerchantDirectory();
			} else if (navigateTo.equals("scanReceiving")) {
    			Intent intent2 = new Intent(MainActivity.this, ZBarScannerActivity.class);
    			intent2.putExtra(ZBarConstants.SCAN_MODES, new int[] { Symbol.QRCODE } );
    			startActivityForResult(intent2, ZBAR_SCANNER_REQUEST);	
			}
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
	}

	@Override
	protected void onResume() {
		super.onResume();

		application.setIsPassedPinScreen(true);

		if(TimeOutUtil.getInstance().isTimedOut()) {
        	Intent intent = new Intent(MainActivity.this, PinEntryActivity.class);
			String navigateTo = getIntent().getStringExtra("navigateTo");
			intent.putExtra("navigateTo", navigateTo);   
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        	intent.putExtra("verified", true);
    		startActivity(intent);
		}
		else {
			TimeOutUtil.getInstance().updatePin();
		}

	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		application.setIsPassedPinScreen(false);
	}
/*
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
*/
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		if (mDrawerToggle.onOptionsItemSelected(item)) {
			return true;
		}
		/*
		else {
		    switch (item.getItemId()) {
	    	case R.id.action_about:
	    		doAbout();
	    		return true;
	    	default:
		        return super.onOptionsItemSelected(item);
		    }
		}
		*/
		
        return super.onOptionsItemSelected(item);

	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		application.setIsScanning(false);

		if(resultCode == Activity.RESULT_OK && requestCode == ZBAR_SCANNER_REQUEST)	{
			String strResult = data.getStringExtra(ZBarConstants.SCAN_RESULT);

        	if(strResult != null) {

		        viewPager.setCurrentItem(0);

				Intent intent = new Intent("info.blockchain.wallet.ui.SendFragment.BTC_ADDRESS_SCAN");
			    intent.putExtra("BTC_ADDRESS", strResult);
			    LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        	}
			else {
				Toast.makeText(this, R.string.invalid_bitcoin_address, Toast.LENGTH_LONG).show();
			}

        }
		else if(resultCode == Activity.RESULT_CANCELED && requestCode == ZBAR_SCANNER_REQUEST) {
//          Toast.makeText(this, R.string.camera_unavailable, Toast.LENGTH_SHORT).show();
		}
		else {
    		//
    		// SecurityException fix
    		//
			Security.addProvider(new org.spongycastle.jce.provider.BouncyCastleProvider());
		}
	}

	@Override
    public boolean onKeyDown(int keyCode, KeyEvent event) { 
        if(keyCode == KeyEvent.KEYCODE_BACK) {
        	
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setMessage(R.string.ask_you_sure_exit).setCancelable(false);
			AlertDialog alert = builder.create();

			alert.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.yes), new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					application.setIsPassedPinScreen(false);
					
//					finish();

					final Intent relaunch = new Intent(MainActivity.this, Exit.class)
							.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
							| Intent.FLAG_ACTIVITY_CLEAR_TASK
							| Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
					startActivity(relaunch);
					
					dialog.dismiss();
				}}); 

			alert.setButton(AlertDialog.BUTTON_NEGATIVE, getString(R.string.cancel), new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					
					dialog.dismiss();
				}});

			alert.show();
        	
            return true;
        }
        else	{
        	;
        }

        return false;
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

    private void doMerchantDirectory()	{
    	if (!application.isGeoEnabled()) {
    		EnableGeo.displayGPSPrompt(this);
    	}
    	else {
    		/*
    		Provider[] providers = Security.getProviders();
    		for(int i = 0; i < providers.length; i++)	{
    			System.out.println(providers[i].getName());
    		}
    		*/
    		//
    		// SecurityException fix
    		//
    		Security.removeProvider("SC");

    		TimeOutUtil.getInstance().updatePin();
        	Intent intent = new Intent(MainActivity.this, info.blockchain.merchant.directory.MapActivity.class);
    		startActivityForResult(intent, MERCHANT_ACTIVITY);
    	}
    }

    private void doSettings()	{
		TimeOutUtil.getInstance().updatePin();
    	Intent intent = new Intent(MainActivity.this, info.blockchain.wallet.ui.SettingsActivity.class);
		startActivityForResult(intent, SETTINGS_ACTIVITY);
    }

    private void doAddressBook()	{
		TimeOutUtil.getInstance().updatePin();
    	Intent intent = new Intent(MainActivity.this, info.blockchain.wallet.ui.AddressBookActivity.class);
		startActivityForResult(intent, ADDRESSBOOK_ACTIVITY);
    }

    private void doSend2Friends()	{
    	Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
    	intent.setType(ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE);
//    	intent.setType(ContactsContract.CommonDataKinds.Email.CONTENT_TYPE);
    	startActivityForResult(intent, PICK_CONTACT);
    }

}

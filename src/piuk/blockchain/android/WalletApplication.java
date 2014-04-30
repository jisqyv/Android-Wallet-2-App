/*
 * Copyright 2011-2012 the original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package piuk.blockchain.android;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.Application;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.*;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.ConnectivityManager;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Pair;
import android.widget.Toast;
import com.google.bitcoin.core.*;
import com.google.bitcoin.core.Wallet.AutosaveEventListener;
import com.google.bitcoin.store.WalletExtensionSerializer;
import com.google.bitcoin.store.WalletProtobufSerializer;

import org.apache.commons.io.IOUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.spongycastle.util.encoders.Hex;

import piuk.EventListeners;
import piuk.MyRemoteWallet;
import piuk.MyRemoteWallet.NotModfiedException;
import piuk.MyWallet;
import piuk.blockchain.android.R;
import piuk.blockchain.android.service.BlockchainServiceImpl;
import piuk.blockchain.android.service.WebsocketService;
import piuk.blockchain.android.ui.AbstractWalletActivity;
import piuk.blockchain.android.ui.PinEntryActivity;
import piuk.blockchain.android.ui.SuccessCallback;
import piuk.blockchain.android.ui.dialogs.RekeyWalletDialog;
import piuk.blockchain.android.util.ErrorReporter;
import piuk.blockchain.android.util.RandomOrgGenerator;
import piuk.blockchain.android.util.WalletUtils;

import java.io.File;
import java.io.FileInputStream; 
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.security.MessageDigest;
import java.security.Security;
import java.text.SimpleDateFormat;
import java.util.Map.Entry;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@SuppressLint("SimpleDateFormat")
public class WalletApplication extends Application {
	private MyRemoteWallet blockchainWallet;

	private final Handler handler = new Handler();
	private Timer timer;
	public int decryptionErrors = 0;
	private Intent blockchainServiceIntent;
	private Intent websocketServiceIntent;
	public boolean didEncounterFatalPINServerError = false;
	public Wallet bitcoinjWallet;
	public Pair<Block, Integer> blockExplorerBlockPair;
	public long earliestKeyTime;
	private volatile boolean checkWalletStatusScheduled = false;

	private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver()
	{
		@Override
		public void onReceive(final Context context, final Intent intent)
		{
			final String action = intent.getAction();

			handler.post(new Runnable() {
				public void run() {
					if (ConnectivityManager.CONNECTIVITY_ACTION.equals(action))
					{
						boolean hasConnectivity = !intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false);

						if (hasConnectivity) {
							checkWalletStatus(null);
						}
					}	
				}
			});
		}
	};

	public void clearWallet() {

		if (this.isInP2PFallbackMode())
			this.leaveP2PMode();

		Editor edit = PreferenceManager.getDefaultSharedPreferences(
				this).edit();

		edit.remove("guid");
		edit.remove("sharedKey");

		edit.commit();

		this.blockchainWallet = null;
		this.didEncounterFatalPINServerError = false;
		this.decryptionErrors = 0;

		this.deleteLocalWallet();
	}

	public void connect() {
		if (timer != null) {
			try {
				timer.cancel();

				timer.purge();

				timer = null;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		final IntentFilter intentFilter = new IntentFilter();

		intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);

		registerReceiver(broadcastReceiver, intentFilter);

		if (!WebsocketService.isRunning)
			startService(websocketServiceIntent);
	}

	public Integer getLatestHeightFromBlockExplorer() throws Exception {
		return Integer.valueOf(WalletUtils.getURL("http://blockexplorer.com/q/getblockcount"));
	}

	public Pair<Block, Integer> getLatestBlockHeaderFromBlockExplorer(Integer blockHeight) throws Exception {

		String hash = WalletUtils.getURL("http://blockexplorer.com/q/getblockhash/"+blockHeight);

		JSONObject obj = (JSONObject) new JSONParser().parse(WalletUtils.getURL("http://blockexplorer.com/rawblock/"+hash));

		Block block = new Block(Constants.NETWORK_PARAMETERS);

		block.version = ((Number)obj.get("ver")).longValue();
		block.prevBlockHash = new Sha256Hash((String)obj.get("prev_block"));
		block.merkleRoot = new Sha256Hash((String)obj.get("mrkl_root"));
		block.time = ((Number)obj.get("time")).longValue();		
		block.difficultyTarget = ((Number)obj.get("bits")).longValue();		
		block.nonce = ((Number)obj.get("nonce")).longValue();		

		block.hash = new Sha256Hash((String)obj.get("hash"));

		block.headerParsed = true;
		block.transactionsParsed = true;
		block.headerBytesValid = true;
		block.transactionBytesValid = true;

		return new Pair<Block, Integer>(block, blockHeight);
	}

	public long estimateFirstSeenFromBlockExplorer() throws Exception {
		SimpleDateFormat format = new SimpleDateFormat("yyyy-mm-dd hh:mm:ss");

		long earliest = 0;
		for (ECKey key : bitcoinjWallet.getKeys()) {
			try {

				String url = "http://blockexplorer.com/q/addressfirstseen/" + key.toAddress(Constants.NETWORK_PARAMETERS).toString();

				String response = WalletUtils.getURL(url);

				Date date = null;
				if (response.contains("Never")) {
					date = new Date();
				} else {
					date = format.parse(response);
				}

				if (earliest == 0) {
					earliest = date.getTime();
				} else if (date.getTime() < earliest) {
					earliest = date.getTime();
				}
			} catch (Exception e) {
				e.printStackTrace(); 
			}
		}

		return earliest;
	}

	//BitcoinJ Temp Wallet
	public void saveBitcoinJWallet()
	{

		if (bitcoinjWallet == null)
			return;

		try
		{
			File walletFile = getFileStreamPath(Constants.WALLET_FILENAME_PROTOBUF);

			bitcoinjWallet.saveToFile(walletFile);
		}
		catch (final IOException x)
		{
			throw new RuntimeException(x);
		}
	}

	private static final class WalletAutosaveEventListener implements AutosaveEventListener
	{
		public boolean caughtException(final Throwable throwable)
		{

			throwable.printStackTrace();
			return true;
		}

		public void onBeforeAutoSave(final File file)
		{
		}

		public void onAfterAutoSave(final File file)
		{
		}
	}

	//BitcoinJ Temp Wallet
	private void loadBitcoinJWallet()
	{

		File walletFile = getFileStreamPath(Constants.WALLET_FILENAME_PROTOBUF);

		if (walletFile.exists())
		{
			FileInputStream walletStream = null;

			try
			{
				walletStream = new FileInputStream(walletFile);

				WalletProtobufSerializer serializer = new WalletProtobufSerializer();

				serializer.setWalletExtensionSerializer(new WalletExtensionSerializer() {
					public Wallet newWallet(NetworkParameters params) {
						return new MyWallet.WalletOverride(params);
					}
				});


				Wallet wallet = serializer.readWallet(walletStream); 

				if (wallet.getKeychainSize() > 0) {

					bitcoinjWallet = wallet;

					bitcoinjWallet.autosaveToFile(walletFile, 1, TimeUnit.SECONDS, new WalletAutosaveEventListener());
				}

			}
			catch (final Exception e)
			{
				e.printStackTrace();
			}
			finally
			{
				if (walletStream != null)
				{
					try
					{
						walletStream.close();
					}
					catch (final IOException x)
					{
						x.printStackTrace();
					}
				}
			}
		}
	}

	public String getDeviceID() {
		try {
			Class<?> c = Class.forName("android.os.SystemProperties");           
			Method get = c.getMethod("get", String.class, String.class );       
			return (String) (get.invoke(c, "ro.serialno", ""));  
		} catch (Exception e) {
			return null;
		}
	}
	public void deleteBitcoinJLocalData() {
		try {
			//Delete the wallet file
			File bitcoinJFile = getFileStreamPath(Constants.WALLET_FILENAME_PROTOBUF);

			if (bitcoinJFile.exists()) {
				bitcoinJFile.delete();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		try {
			//Clear the blockchain file (we need to rescan)
			File blockChainFile = new File(getDir("blockstore", Context.MODE_WORLD_READABLE | Context.MODE_WORLD_WRITEABLE), Constants.BLOCKCHAIN_FILENAME);

			if (blockChainFile.exists())
				blockChainFile.delete();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void startBlockchainService()
	{
		if (blockchainWallet == null)
			return;

		try {
			if (bitcoinjWallet == null) {
				deleteBitcoinJLocalData();

				this.bitcoinjWallet = blockchainWallet.getBitcoinJWallet();
			} 

			new Thread(new Runnable() {

				@Override
				public void run() {
					earliestKeyTime = 0;
					blockExplorerBlockPair = null;

					try {
						earliestKeyTime = estimateFirstSeenFromBlockExplorer();
					} catch (Exception e) {
						e.printStackTrace(); 
					}

					/*if (earliestKeyTime > 0) {
						try {

							Integer height = getLatestHeightFromBlockExplorer();

							long elapsedSeconds = ((System.currentTimeMillis() - earliestKeyTime) / 1000);

							for (int ii = 0; ii < 10; ++ii) {								
								//One block every 10 minutes (600 seconds)
								int estimatedHeight = height - (ii*10000) - (int)(elapsedSeconds / 600);

								Pair<Block, Integer> pair = getLatestBlockHeaderFromBlockExplorer(estimatedHeight);

								if (pair.first.getTime().getTime() < earliestKeyTime) {
									blockExplorerBlockPair = pair;
									break;
								}
							}
						} catch (Exception e1) {
							e1.printStackTrace();
						}
					}*/

					if (!isBitcoinJServiceRunning())
						startService(blockchainServiceIntent);

					EventListeners.invokeWalletDidChange();
				}

			}).start();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void leaveP2PMode() {
		this.bitcoinjWallet = null;

		stopService(blockchainServiceIntent);

		deleteBitcoinJLocalData();

		EventListeners.invokeWalletDidChange();
	}

	public void disconnectSoon() {

		try {
			if (timer == null) {
				timer = new Timer(); 

				timer.schedule(new TimerTask() {
					@Override
					public void run() {
						handler.post(new Runnable() {
							public void run() {
								try {
									if (WebsocketService.isRunning)
										stopService(websocketServiceIntent);

									AbstractWalletActivity.lastDisplayedNetworkError = 0;

									if (isInP2PFallbackMode())
										saveBitcoinJWallet();

									unregisterReceiver(broadcastReceiver);

								} catch (Exception e) {
									e.printStackTrace();
								}
							}
						});
					}
				}, 5000);

				timer.schedule(new TimerTask() {
					@Override
					public void run() {
						handler.post(new Runnable() {
							public void run() {
								try {
									blockchainWallet = null;
									didEncounterFatalPINServerError = false;
									decryptionErrors = 0;

								} catch (Exception e) {
									e.printStackTrace();
								}
							}
						});
					}
				}, 120000);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void generateNewWallet() throws Exception {
		this.blockchainWallet = new MyRemoteWallet();

		this.decryptionErrors = 0;

		this.didEncounterFatalPINServerError = false;
	}

	public void checkWalletStatus(final AbstractWalletActivity activity) {

		boolean passwordSaved = PreferenceManager.getDefaultSharedPreferences(this).contains("encrypted_password");

		if (this.isInP2PFallbackMode()) {
			if (!this.isBitcoinJServiceRunning()) {
				startService(blockchainServiceIntent);
			}	
		} 

		if (blockchainWallet != null && decryptionErrors == 0 && (passwordSaved || didEncounterFatalPINServerError)) {
			if (!blockchainWallet.isUptoDate(Constants.MultiAddrTimeThreshold)) {
				if (checkWalletStatusScheduled) {
					return;
				}
				checkWalletStatusScheduled = true;

				handler.postDelayed(new Runnable() {
					@Override
					public void run() {
						if (blockchainWallet != null) {
							checkIfWalletHasUpdatedAndFetchTransactions(blockchainWallet.getTemporyPassword());
						}

						checkWalletStatusScheduled = false;
					} 
				}, 2500);
			} 
		} else if (blockchainWallet == null || decryptionErrors > 0 || !passwordSaved) {

			if (activity == null || PinEntryActivity.active)
				return;

			//Remove old password 
			String old_password = PreferenceManager.getDefaultSharedPreferences(this).getString("password", null);

			if (old_password != null) {
				decryptLocalWallet(readLocalWallet(), old_password);

				PreferenceManager.getDefaultSharedPreferences(this).edit().remove("password").commit();
			}

			handler.post(new Runnable() {
				@Override
				public void run() {	
					if (!PinEntryActivity.active) {

						Intent intent = new Intent(activity, PinEntryActivity.class);

						activity.startActivity(intent);
					}
				}
			});
		}	
	}


	@Override
	public void onCreate() {
		super.onCreate();

		ErrorReporter.getInstance().init(this);

		blockchainServiceIntent = new Intent(this, BlockchainServiceImpl.class);
		websocketServiceIntent = new Intent(this, WebsocketService.class);

		System.setProperty("device_name", "android");

		try {
			PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);

			System.setProperty("device_version", pInfo.versionName);
		} catch (NameNotFoundException e1) {
			e1.printStackTrace();
		}

		try { 
			// Need to save session cookie for kaptcha
			CookieHandler.setDefault(new CookieManager());

			Security.addProvider(new org.spongycastle.jce.provider.BouncyCastleProvider());
		} catch (Throwable e) {
			e.printStackTrace();
		}

		seedFromRandomOrg();

		loadBitcoinJWallet();

		connect();
	}

	public MyRemoteWallet getRemoteWallet() {
		return blockchainWallet;
	}


	public String getGUID() {
		return PreferenceManager.getDefaultSharedPreferences(this).getString("guid", null);
	}

	public long getLastTriedToRegisterForNotifications() {
		return PreferenceManager.getDefaultSharedPreferences(this).getLong("last_notification_register", 0);
	}

	public boolean needsWalletRekey() { 
		MyRemoteWallet wallet = getRemoteWallet();

		if (wallet == null || wallet.isNew())
			return false;

		List<String> insecure_addresses = RekeyWalletDialog.getPossiblyInsecureAddresses(wallet);

		return !getHasAskedToRekeyWallet() && insecure_addresses.size() > 0 && !RekeyWalletDialog.hasKnownAndroidAddresses(wallet);
	}

	public boolean getHasAskedToRekeyWallet() { 
		return PreferenceManager.getDefaultSharedPreferences(this).getBoolean("has_asked_rekeyed_wallet4", false);
	}

	public boolean setHasAskedRekeyedWallet(boolean value) { 
		return PreferenceManager.getDefaultSharedPreferences(this).edit().putBoolean("has_asked_rekeyed_wallet4", value).commit();
	}

	public boolean hasRegisteredForNotifications(String guid) {
		String registered_guid = PreferenceManager.getDefaultSharedPreferences(this).getString("registered_guid", null);

		return registered_guid != null && registered_guid.equals(guid); 
	}

	public boolean setLastRegisteredForNotifications(long time) {
		Editor edit = PreferenceManager
				.getDefaultSharedPreferences(
						this
						.getApplicationContext())
						.edit(); 

		edit.putLong("last_notification_register", time);

		return edit.commit();
	}

	public boolean setRegisteredForNotifications(String guid) {
		Editor edit = PreferenceManager
				.getDefaultSharedPreferences(
						this
						.getApplicationContext())
						.edit();

		edit.putString("registered_guid", guid);

		return edit.commit();
	}

	public void registerForNotificationsIfNeeded(final String registration_id) {

		if (blockchainWallet == null)
			return;

		if (!blockchainWallet.isNew() && !hasRegisteredForNotifications(getGUID())) {

			if (getLastTriedToRegisterForNotifications() > System.currentTimeMillis()-30000) {
				System.out.println("Registered Recently");
				return;
			}

			setLastRegisteredForNotifications(System.currentTimeMillis());

			new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						if (blockchainWallet.registerNotifications(registration_id)) {
							setRegisteredForNotifications(getGUID());
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}

			}).start();
		} else {
			System.out.println("New wallet or already Registered");
		}
	}

	private boolean isBitcoinJServiceRunning() {
		ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
		for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
			if (BlockchainServiceImpl.class.getName().equals(service.service.getClassName())) {
				return true;
			}
		}
		return false;
	}

	public boolean isInP2PFallbackMode() {
		return bitcoinjWallet != null;
	}

	public void unRegisterForNotifications(final String registration_id) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					if (blockchainWallet.unregisterNotifications(registration_id)) {
						setRegisteredForNotifications(null);
					}
				} catch (Exception e) {
					e.printStackTrace(); 
				}
			}

		}).start();
	}

	public String getSharedKey() {
		return PreferenceManager.getDefaultSharedPreferences(this).getString(
				"sharedKey", null);
	}

	public void notifyWidgets() {
		final Context context = getApplicationContext();

		// notify widgets
		final AppWidgetManager appWidgetManager = AppWidgetManager
				.getInstance(context);
		for (final AppWidgetProviderInfo providerInfo : appWidgetManager
				.getInstalledProviders()) {
			// limit to own widgets
			if (providerInfo.provider.getPackageName().equals(
					context.getPackageName())) {
				final Intent intent = new Intent(
						AppWidgetManager.ACTION_APPWIDGET_UPDATE);
				intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS,
						appWidgetManager.getAppWidgetIds(providerInfo.provider));
				context.sendBroadcast(intent);
			}
		}
	}

	public synchronized String readExceptionLog() {
		try {
			FileInputStream multiaddrCacheFile = openFileInput(Constants.EXCEPTION_LOG);

			return IOUtils.toString(multiaddrCacheFile);

		} catch (IOException e1) {
			e1.printStackTrace();

			return null;
		}
	}

	public synchronized void writeException(Exception e) {
		try {
			FileOutputStream file = openFileOutput(Constants.EXCEPTION_LOG,
					MODE_APPEND);

			PrintStream stream = new PrintStream(file);

			e.printStackTrace(stream);

			stream.close();

			file.close();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}

	public synchronized void writeMultiAddrCache(String repsonse) {
		if (blockchainWallet == null)
			return;

		try {
			FileOutputStream file = openFileOutput(blockchainWallet.getGUID()
					+ Constants.MULTIADDR_FILENAME, Constants.WALLET_MODE);

			file.write(repsonse.getBytes());

			file.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public synchronized void checkIfWalletHasUpdatedAndFetchTransactions(final String password) {
		checkIfWalletHasUpdatedAndFetchTransactions(password, null);
	}

	public synchronized void checkIfWalletHasUpdatedAndFetchTransactions(final String password, final SuccessCallback callbackFinal) {
		checkIfWalletHasUpdated(password, true, callbackFinal);
	}

	public synchronized void checkIfWalletHasUpdated(final String password, boolean fetchTransactions, final SuccessCallback callbackFinal) {
		if (getGUID() == null || getSharedKey() == null) {
			if (callbackFinal != null) callbackFinal.onFail();
			return;
		}

		checkIfWalletHasUpdated(password, getGUID(), getSharedKey(), fetchTransactions, callbackFinal);
	}

	public void seedFromRandomOrg() {
		new Thread(new Runnable() {
			public void run() {
				try {
					MyWallet.extra_seed = RandomOrgGenerator.getRandomBytes(32);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}).start();
	}

	public synchronized void checkIfWalletHasUpdated(final String password, final String guid, final String sharedKey, final boolean fetchTransactions, final SuccessCallback callbackFinal) {

		System.out.println("checkIfWalletHasUpdatedAndFetchTransactions()");

		final WalletApplication application = this;

		new Thread(new Runnable() {
			public void run() {
				JSONObject walletPayloadObj = null;
				SuccessCallback callback = callbackFinal;

				String localWallet = null;

				try {
					if (blockchainWallet == null) {
						System.out.println("Read Local Wallet");

						localWallet = readLocalWallet();

						//First try and restore the local cache
						if (decryptLocalWallet(localWallet, password)) {	
							if (callback != null)  {
								readLocalMultiAddr();  

								handler.post(new Runnable() {
									public void run() {
										callbackFinal.onSuccess();
									};
								});

								callback = null;
							}
							return;
						} else {
							walletPayloadObj = MyRemoteWallet.getWalletPayload(guid, sharedKey);	
						}
					} else {
						walletPayloadObj = MyRemoteWallet.getWalletPayload(guid, sharedKey, blockchainWallet.getChecksum());		
					}

				} catch (NotModfiedException e) {

					if (blockchainWallet != null) {
						if (callback != null)  {
							handler.post(new Runnable() {
								public void run() {
									callbackFinal.onSuccess();
								};
							});
							callback = null;
						}

						if (fetchTransactions && !blockchainWallet.isUptoDate(Constants.MultiAddrTimeThreshold)) {
							doMultiAddr(true);
						} 

						return;
					} 
				} catch (final Exception e) {
					e.printStackTrace();
				}

				if (walletPayloadObj == null) {
					if (callback != null)  {
						handler.post(new Runnable() {
							public void run() {
								callbackFinal.onFail();
							};
						});
						callback = null;
					}

					if (fetchTransactions && blockchainWallet != null) {
						doMultiAddr(true);
					}

					return;
				}

				try {
					if (blockchainWallet == null) {
						blockchainWallet = new MyRemoteWallet(walletPayloadObj, password);
					} else {						
						blockchainWallet.setTemporyPassword(password);

						blockchainWallet.setPayload(walletPayloadObj);
					}

					decryptionErrors = 0;

					if (callback != null)  {
						handler.post(new Runnable() {
							public void run() {
								callbackFinal.onSuccess();
							};
						});
						callback = null;
					}

					EventListeners.invokeWalletDidChange();

				} catch (Exception e) {
					e.printStackTrace();

					deleteLocalWallet();

					try {
						PinEntryActivity.clearPrefValues(application);
					} catch (Exception e1) {
						e1.printStackTrace();
					}

					decryptionErrors++;

					blockchainWallet = null;

					if (callback != null)  {
						handler.post(new Runnable() {
							public void run() {
								Toast.makeText(WalletApplication.this,
										R.string.toast_wallet_decryption_failed,
										Toast.LENGTH_LONG).show();

								callbackFinal.onFail();
							};
						});
						callback = null;
					}

					EventListeners.invokeWalletDidChange();

					writeException(e);

					return;
				}

				if (decryptionErrors > 0)
					return;

				localSaveWallet();

				try {
					// Copy our labels into the address book
					if (blockchainWallet.getLabelMap() != null) {
						for (Entry<String, String> labelObj : blockchainWallet
								.getLabelMap().entrySet()) {
							AddressBookProvider.setLabel(getContentResolver(),
									labelObj.getKey(), labelObj.getValue());
						}
					}
				} catch (Exception e) {
					e.printStackTrace();

					writeException(e);
				}

				try {
					// Get the balance and transaction
					if (fetchTransactions)
						doMultiAddr(true);

				} catch (Exception e) {
					e.printStackTrace();

					writeException(e);

					handler.post(new Runnable() {
						public void run() {
							Toast.makeText(WalletApplication.this,
									R.string.toast_error_syncing_wallet,
									Toast.LENGTH_LONG).show();
						}
					});
				}
			}
		}).start();
	}

	private AtomicBoolean isRunningMultiAddr = new AtomicBoolean(false);


	public void doMultiAddr(final boolean notifications) {
		doMultiAddr(notifications, null);
	}

	public void doMultiAddr(final boolean notifications, final SuccessCallback callback) {
		final MyRemoteWallet blockchainWallet = this.blockchainWallet;

		if (blockchainWallet == null) {
			if (callback != null)
				callback.onFail();

			return;
		}

		if (!isRunningMultiAddr.compareAndSet(false, true)) {
			if (callback != null)
				callback.onFail();

			return;
		}

		new Thread(new Runnable() {
			public void run() {
				try {
					String multiAddr = null;

					try {
						multiAddr = blockchainWallet.doMultiAddr(notifications);
					} catch (Exception e) {
						e.printStackTrace(); 

						try {
							//Sleep for a bit and retry
							Thread.sleep(5000);
						} catch (InterruptedException e1) {
							e1.printStackTrace();
						}

						try {
							multiAddr = blockchainWallet.doMultiAddr(notifications);
						} catch (Exception e1) {
							e1.printStackTrace(); 

							EventListeners.invokeOnMultiAddrError();

							if (callback != null)
								callback.onFail();

							return;
						}
					}

					if (callback != null)
						callback.onSuccess();

					try {
						writeMultiAddrCache(multiAddr);
					} catch (Exception e) {
						e.printStackTrace();
					}

					//After multi addr the currency is set
					if (blockchainWallet.getLocalCurrencyCode() != null)
						setCurrency(blockchainWallet.getLocalCurrencyCode());

					handler.post(new Runnable() {
						public void run() {
							notifyWidgets();
						}
					});
				} finally {
					isRunningMultiAddr.set(false);
				}
			}
		}).start();
	}

	public static interface AddAddressCallback {
		public void onSavedAddress(String address);

		public void onError(String reason);
	}

	public void saveWallet(final SuccessCallback callback) {

		if (this.isInP2PFallbackMode()) {
			callback.onFail();
			return;
		};

		new Thread() {
			@Override
			public void run() {
				try {
					if (blockchainWallet.remoteSave()) {
						handler.post(new Runnable() {
							public void run() {

								callback.onSuccess();

								notifyWidgets();
							}
						});
					} else {
						handler.post(new Runnable() {
							public void run() {
								callback.onFail();
							}
						});
					}

				} catch (Exception e) {
					e.printStackTrace();

					writeException(e);

					handler.post(new Runnable() {
						public void run() {
							callback.onFail();

							Toast.makeText(WalletApplication.this,
									R.string.toast_error_syncing_wallet,
									Toast.LENGTH_LONG).show();
						}
					});
				}
			}
		}.start();
	}

	public void addKeyToWallet(final ECKey key, final String address, final String label, final int tag,
			final AddAddressCallback callback) {

		if (blockchainWallet == null) {
			callback.onError("Wallet null.");
			return;
		}

		if (isInP2PFallbackMode()) {
			callback.onError("Error saving wallet.");
			return;
		}

		try {
			final boolean success = blockchainWallet.addKey(key, address, label);
			if (success) {
				if (tag != 0) {
					blockchainWallet.setTag(address, tag);
				}

				localSaveWallet();

				saveWallet(new SuccessCallback() {
					@Override
					public void onSuccess() {
						checkIfWalletHasUpdated(blockchainWallet.getTemporyPassword(), false, new SuccessCallback() {
							@Override
							public void onSuccess() {	
								try {
									ECKey key = blockchainWallet.getECKey(address);									
									if (key != null && key.toAddress(NetworkParameters.prodNet()).toString().equals(address)) {
										callback.onSavedAddress(address);
									} else {
										blockchainWallet.removeKey(key);

										callback.onError("WARNING! Wallet saved but address doesn't seem to exist after re-read.");
									}
								} catch (Exception e) {
									blockchainWallet.removeKey(key);

									callback.onError("WARNING! Error checking if ECKey is valid on re-read.");
								}
							}

							@Override
							public void onFail() {
								blockchainWallet.removeKey(key);

								callback.onError("WARNING! Error checking if address was correctly saved.");
							}
						});
					}

					@Override
					public void onFail() {
						blockchainWallet.removeKey(key);

						callback.onError("Error saving wallet");
					}
				});
			} else {
				callback.onError("addKey returned false");
			}

		} catch (Exception e) {
			e.printStackTrace();

			writeException(e);

			callback.onError(e.getLocalizedMessage());
		}
	}

	public void setAddressLabel(final String address, final String label) {
		if (blockchainWallet == null)
			return;

		checkIfWalletHasUpdatedAndFetchTransactions(blockchainWallet.getTemporyPassword(), new SuccessCallback() {

			@Override
			public void onSuccess() {
				try {
					blockchainWallet.addLabel(address, label);

					new Thread() {
						@Override
						public void run() {
							try {
								blockchainWallet.remoteSave();

								System.out.println("invokeWalletDidChange()");

								EventListeners.invokeWalletDidChange();
							} catch (Exception e) {
								e.printStackTrace(); 

								writeException(e);

								handler.post(new Runnable() {
									public void run() {
										Toast.makeText(WalletApplication.this,
												R.string.toast_error_syncing_wallet,
												Toast.LENGTH_LONG).show();
									}
								});
							}
						}
					}.start();
				} catch (Exception e) {
					e.printStackTrace();

					handler.post(new Runnable() {
						@Override
						public void run() {
							Toast.makeText(WalletApplication.this,
									R.string.error_setting_label, Toast.LENGTH_LONG).show();
						}
					});
				}
			}

			@Override
			public void onFail() {
				handler.post(new Runnable() {
					public void run() {
						Toast.makeText(WalletApplication.this,
								R.string.toast_error_syncing_wallet,
								Toast.LENGTH_LONG).show();
					}
				});
			}
		});


	}

	public boolean setCurrency(String currency) { 
		return PreferenceManager.getDefaultSharedPreferences(this).edit().putString(Constants.PREFS_KEY_EXCHANGE_CURRENCY, currency).commit();
	}

	public boolean setShouldDisplayLocalCurrency(boolean value) { 
		return PreferenceManager.getDefaultSharedPreferences(this).edit().putBoolean("should_display_local_currency", value).commit();
	}

	public boolean getShouldDisplayLocalCurrency() { 
		return PreferenceManager.getDefaultSharedPreferences(this).getBoolean("should_display_local_currency", false);
	}

	public boolean readLocalMultiAddr() {
		if (blockchainWallet == null)
			return false;

		try {
			// Restore the multi address cache
			FileInputStream multiaddrCacheFile = openFileInput(blockchainWallet
					.getGUID() + Constants.MULTIADDR_FILENAME);

			String multiAddr = IOUtils.toString(multiaddrCacheFile);

			blockchainWallet.parseMultiAddr(multiAddr, false);

			if (blockchainWallet.getLocalCurrencyCode() != null)
				setCurrency(blockchainWallet.getLocalCurrencyCode());

			return true;

		} catch (Exception e) {
			writeException(e);

			e.printStackTrace();

			return false;
		}
	}


	public String makeWalletChecksum(String payload) {
		try {
			return new String(Hex.encode(MessageDigest.getInstance("SHA-256").digest(payload.getBytes("UTF-8"))));
		} catch (Exception e) {}

		return null;
	}

	public String readLocalWallet() { 
		try {
			// Read the wallet from local file
			FileInputStream file = openFileInput(Constants.WALLET_FILENAME);

			return IOUtils.toString(file, "UTF-8");
		} catch (Exception e) {}

		return null;
	}

	public boolean deleteLocalWallet() {
		try {
			if (deleteFile(Constants.WALLET_FILENAME)) {
				System.out.println("Removed Local Wallet");
			} else {
				System.out.println("Error Removing Local Wallet");
			}
		} catch (Exception e) {
			writeException(e);

			e.printStackTrace();
		}  

		return false;
	}

	public boolean decryptLocalWallet(String payload, String password) {
		try {
			MyRemoteWallet wallet = new MyRemoteWallet(payload, password);

			if (wallet.getGUID().equals(getGUID())) {
				this.blockchainWallet = wallet;

				this.decryptionErrors = 0;

				EventListeners.invokeWalletDidChange();

				return true;
			} else {
				return false;
			}
		} catch (Exception e) {
			writeException(e);

			e.printStackTrace();
		}

		return false;
	}

	public void localSaveWallet() {
		if (blockchainWallet == null)
			return;

		try {
			if (blockchainWallet.isNew())
				return;

			FileOutputStream file = openFileOutput(
					Constants.WALLET_FILENAME, Constants.WALLET_MODE);

			file.write(blockchainWallet.getPayload().getBytes());

			file.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public Address determineSelectedAddress() {
		if (blockchainWallet == null)
			return null;

		final String[] addresses = blockchainWallet.getActiveAddresses();

		if (addresses.length == 0)
			return null;

		final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		final String selectedAddress = prefs.getString(Constants.PREFS_KEY_SELECTED_ADDRESS, null);

		if (selectedAddress != null) {
			for (final String address : addresses) {
				if (address.equals(selectedAddress)) {
					try {
						return new Address(Constants.NETWORK_PARAMETERS, address);
					} catch (WrongNetworkException e) {
						e.printStackTrace();
					} catch (AddressFormatException e) {
						e.printStackTrace();
					}
				}
			}
		}

		try {
			return new Address(Constants.NETWORK_PARAMETERS, addresses[0]);
		} catch (WrongNetworkException e) {
			e.printStackTrace();
		} catch (AddressFormatException e) {
			e.printStackTrace();
		}

		return null;
	}

	public final int applicationVersionCode() {
		try {
			return getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
		} catch (NameNotFoundException x) {
			return 0;
		}
	}

	public final String applicationVersionName() {
		try {
			return getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
		} catch (NameNotFoundException x) {
			return "unknown";
		}
	}
}

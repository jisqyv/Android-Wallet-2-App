/*
 * Copyright 2011-2013 the original author or authors.
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

package piuk.blockchain.android.service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import piuk.EventListeners;
import piuk.blockchain.android.Constants;
import piuk.blockchain.android.WalletApplication;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.net.ConnectivityManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.preference.PreferenceManager;
import android.text.format.DateUtils;
import android.util.Log;
import android.util.Pair;

import com.google.bitcoin.core.AbstractPeerEventListener;
import com.google.bitcoin.core.Address;
import com.google.bitcoin.core.Block;
import com.google.bitcoin.core.BlockChain;
import com.google.bitcoin.core.CheckpointManager;
import com.google.bitcoin.core.Peer;
import com.google.bitcoin.core.PeerEventListener;
import com.google.bitcoin.core.PeerGroup;
import com.google.bitcoin.core.StoredBlock;
import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.core.TransactionConfidence.ConfidenceType;
import com.google.bitcoin.core.Wallet;
import com.google.bitcoin.core.WalletEventListener;
import com.google.bitcoin.discovery.DnsDiscovery;
import com.google.bitcoin.discovery.PeerDiscovery;
import com.google.bitcoin.discovery.PeerDiscoveryException;
import com.google.bitcoin.store.BlockStore;
import com.google.bitcoin.store.BlockStoreException;
import com.google.bitcoin.store.BoundedOverheadBlockStore;
import com.google.bitcoin.store.SPVBlockStore;


/**
 * @author Andreas Schildbach
 */
public class BlockchainServiceImpl extends android.app.Service implements BlockchainService
{
	private WalletApplication application;
	private SharedPreferences prefs;

	private BlockStore blockStore;
	private File blockChainFile;
	public BlockChain blockChain;
	private PeerGroup peerGroup;

	private final Handler delayHandler = new Handler();
	private WakeLock wakeLock;

	private PeerConnectivityListener peerConnectivityListener;
	private NotificationManager nm;
	private static final int NOTIFICATION_ID_CONNECTED = 0;
	private static final int NOTIFICATION_ID_COINS_RECEIVED = 1;

	private final List<Address> notificationAddresses = new LinkedList<Address>();
	private int bestChainHeightEver;

	private static final int MAX_LAST_CHAIN_HEIGHTS = 10;
	private static final int IDLE_TIMEOUT_MIN = 2;

	private static final long APPWIDGET_THROTTLE_MS = DateUtils.SECOND_IN_MILLIS;

	private static final String TAG = BlockchainServiceImpl.class.getSimpleName();

	private final Timer timer = new Timer();
	private Wallet wallet;

	private final WalletEventListener walletEventListener = new ThrottelingWalletChangeListener(APPWIDGET_THROTTLE_MS)
	{

		@Override
		public void onThrotteledWalletChanged()
		{
			//EventListeners.invokeWalletDidChange();
		}

		@Override
		public void onCoinsSent(Wallet wallet, Transaction tx, BigInteger prevBalance, BigInteger newBalance) {
			timer.purge();

			timer.schedule(new TimerTask() {

				@Override
				public void run() {
					EventListeners.invokeOnTransactionsChanged();
				}
			}, Constants.BLOCKCHAIN_STATE_BROADCAST_THROTTLE_MS);	
		}

		@Override
		public void onCoinsReceived(final Wallet wallet, final Transaction tx, final BigInteger prevBalance, final BigInteger newBalance)
		{			
			timer.purge();

			timer.schedule(new TimerTask() {

				@Override
				public void run() {
					EventListeners.invokeOnTransactionsChanged();
				}
			}, Constants.BLOCKCHAIN_STATE_BROADCAST_THROTTLE_MS);
		}
	};

	public void rebroadcastTransactions(final long delay) {
		delayHandler.postDelayed(new Runnable() {

			@Override
			public void run() {

				try {
					if (!peerGroup.isRunning())
						return;

					if (wallet == null)
						return;

					for (Transaction tx : wallet.getTransactionsByTime()) {
						if (tx.getConfidence().getConfidenceType() == ConfidenceType.NOT_SEEN_IN_CHAIN) {							
							broadcastTransaction(tx);
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

		}, delay);
	}

	private final class PeerConnectivityListener extends AbstractPeerEventListener implements OnSharedPreferenceChangeListener
	{
		private AtomicBoolean stopped = new AtomicBoolean(false);

		public PeerConnectivityListener()
		{
			prefs.registerOnSharedPreferenceChangeListener(this);
		}

		public void stop()
		{
			stopped.set(true);

			prefs.unregisterOnSharedPreferenceChangeListener(this);

			nm.cancel(NOTIFICATION_ID_CONNECTED);
		}

		@Override
		public void onPeerConnected(final Peer peer, final int peerCount) { }

		@Override
		public void onPeerDisconnected(final Peer peer, final int peerCount) { }

		@Override
		public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) { }
	}

	private final PeerEventListener blockchainDownloadListener = new AbstractPeerEventListener()
	{
		private final AtomicLong lastMessageTime = new AtomicLong(0);

		@Override
		public void onBlocksDownloaded(final Peer peer, final Block block, final int blocksLeft)
		{
			delayHandler.removeCallbacksAndMessages(null);

			final long now = System.currentTimeMillis();

			if (now - lastMessageTime.get() > Constants.BLOCKCHAIN_STATE_BROADCAST_THROTTLE_MS)
				delayHandler.post(runnable);
			else
				delayHandler.postDelayed(runnable, Constants.BLOCKCHAIN_STATE_BROADCAST_THROTTLE_MS);
		}

		private final Runnable runnable = new Runnable()
		{
			public void run()
			{
				lastMessageTime.set(System.currentTimeMillis());

				sendBroadcastBlockchainState(ACTION_BLOCKCHAIN_STATE_DOWNLOAD_OK);
			}
		};
	};

	private final BroadcastReceiver connectivityReceiver = new BroadcastReceiver()
	{
		private boolean hasConnectivity;
		private boolean hasStorage = true;

		@Override
		public void onReceive(final Context context, final Intent intent)
		{
			final String action = intent.getAction();

			if (ConnectivityManager.CONNECTIVITY_ACTION.equals(action))
			{
				hasConnectivity = !intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false);
				final String reason = intent.getStringExtra(ConnectivityManager.EXTRA_REASON);
				Log.i(TAG, "network is " + (hasConnectivity ? "up" : "down") + (reason != null ? ": " + reason : ""));

				check();
			}
			else if (Intent.ACTION_DEVICE_STORAGE_LOW.equals(action))
			{
				hasStorage = false;
				Log.i(TAG, "device storage low");

				check();
			}
			else if (Intent.ACTION_DEVICE_STORAGE_OK.equals(action))
			{
				hasStorage = true;
				Log.i(TAG, "device storage ok");

				check();
			}
		}

		private void check()
		{
			try {
				final boolean hasEverything = hasConnectivity && hasStorage && application.isInP2PFallbackMode();

				if (hasEverything && peerGroup == null)
				{
					Log.d(TAG, "acquiring wakelock");
					wakeLock.acquire();


					Log.i(TAG, "starting peergroup");
					peerGroup = new PeerGroup(Constants.NETWORK_PARAMETERS, blockChain);

					if (application.earliestKeyTime > 0)
						peerGroup.setFastCatchupTimeSecs(application.earliestKeyTime / 1000);

					peerGroup.addWallet(wallet);
					peerGroup.setUserAgent(Constants.USER_AGENT, application.applicationVersionName());
					peerGroup.addEventListener(peerConnectivityListener);

					//Default Bitcoind
					final int maxConnectedPeers = 8;

					final String trustedPeerHost = prefs.getString(Constants.PREFS_KEY_TRUSTED_PEER, "").trim();

					final boolean hasTrustedPeer = false;
					final boolean connectTrustedPeerOnly = false;

					peerGroup.addPeerDiscovery(new PeerDiscovery()
					{
						private final PeerDiscovery normalPeerDiscovery = new DnsDiscovery(Constants.NETWORK_PARAMETERS);

						public InetSocketAddress[] getPeers(final long timeoutValue, final TimeUnit timeoutUnit) throws PeerDiscoveryException
						{
							final List<InetSocketAddress> peers = new LinkedList<InetSocketAddress>();

							boolean needsTrimPeersWorkaround = false;

							if (hasTrustedPeer)
							{
								final InetSocketAddress addr = new InetSocketAddress(trustedPeerHost, Constants.NETWORK_PARAMETERS.port);
								if (addr.getAddress() != null)
								{
									peers.add(addr);
									needsTrimPeersWorkaround = true;
								}
							}

							if (!connectTrustedPeerOnly)
								peers.addAll(Arrays.asList(normalPeerDiscovery.getPeers(timeoutValue, timeoutUnit)));

							// workaround because PeerGroup will shuffle peers
							if (needsTrimPeersWorkaround)
								while (peers.size() >= maxConnectedPeers)
									peers.remove(peers.size() - 1);

							return peers.toArray(new InetSocketAddress[0]);
						}

						public void shutdown()
						{
							normalPeerDiscovery.shutdown();
						}
					});

					// start peergroup
					peerGroup.start();
					peerGroup.startBlockChainDownload(blockchainDownloadListener);

					rebroadcastTransactions(Constants.REBROADCAST_TIME);
				}
				else if (!hasEverything && peerGroup != null)
				{

					Log.i(TAG, "stopping peergroup");
					peerGroup.removeEventListener(peerConnectivityListener);

					if (wallet != null)
						peerGroup.removeWallet(wallet);

					peerGroup.stop();
					peerGroup = null;

					Log.d(TAG, "releasing wakelock");
					wakeLock.release();
				}

				final int download = (hasConnectivity ? 0 : ACTION_BLOCKCHAIN_STATE_DOWNLOAD_NETWORK_PROBLEM)
						| (hasStorage ? 0 : ACTION_BLOCKCHAIN_STATE_DOWNLOAD_STORAGE_PROBLEM);

				sendBroadcastBlockchainState(download);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	};

	private final BroadcastReceiver tickReceiver = new BroadcastReceiver()
	{
		private int lastChainHeight = 0;
		private final List<Integer> lastDownloadedHistory = new LinkedList<Integer>();

		@Override
		public void onReceive(final Context context, final Intent intent)
		{
			final int chainHeight = blockChain.getBestChainHeight();

			if (lastChainHeight > 0)
			{
				final int downloaded = chainHeight - lastChainHeight;

				// push number of downloaded blocks
				lastDownloadedHistory.add(0, downloaded);

				// trim
				while (lastDownloadedHistory.size() > MAX_LAST_CHAIN_HEIGHTS)
					lastDownloadedHistory.remove(lastDownloadedHistory.size() - 1);

				// print
				final StringBuilder builder = new StringBuilder();
				for (final int lastDownloaded : lastDownloadedHistory)
				{
					if (builder.length() > 0)
						builder.append(',');
					builder.append(lastDownloaded);
				}
				Log.i(TAG, "Number of blocks downloaded: " + builder);

				// determine if download is idling
				boolean isIdle = false;
				if (lastDownloadedHistory.size() >= IDLE_TIMEOUT_MIN)
				{
					isIdle = true;
					for (int i = 0; i < IDLE_TIMEOUT_MIN; i++)
					{
						if (lastDownloadedHistory.get(i) > 0)
						{
							isIdle = false;
							break;
						}
					}
				}

				// if idling, shutdown service
				if (isIdle)
				{
					Log.i(TAG, "end of block download detected, stopping service");
					stopSelf();
				}
			}

			lastChainHeight = chainHeight;
		}
	};

	public class LocalBinder extends Binder
	{
		public BlockchainService getService()
		{
			return BlockchainServiceImpl.this;
		}
	}

	private final IBinder mBinder = new LocalBinder();

	@Override
	public IBinder onBind(final Intent intent)
	{
		Log.d(TAG, ".onBind()");

		return mBinder;
	}

	@Override
	public boolean onUnbind(final Intent intent)
	{
		Log.d(TAG, ".onUnbind()");

		return super.onUnbind(intent);
	}

	@Override
	public void onCreate()
	{
		Log.d(TAG, ".onCreate()");

		super.onCreate();


		try {
			nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
			application = (WalletApplication) getApplication();
			wallet = application.bitcoinjWallet;
			prefs = PreferenceManager.getDefaultSharedPreferences(this); 
 
			final String lockName = getPackageName() + " blockchain sync";

			final PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
			wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, lockName);

			final int versionCode = application.applicationVersionCode();
			prefs.edit().putInt(Constants.PREFS_KEY_LAST_VERSION, versionCode).commit();

			bestChainHeightEver = prefs.getInt(Constants.PREFS_KEY_BEST_CHAIN_HEIGHT_EVER, 0);

			peerConnectivityListener = new PeerConnectivityListener(); 

			if (wallet == null) {
				stopSelf();
				return;
			}

			sendBroadcastPeerState(0);

			final IntentFilter intentFilter = new IntentFilter();
			intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
			intentFilter.addAction(Intent.ACTION_DEVICE_STORAGE_LOW);
			intentFilter.addAction(Intent.ACTION_DEVICE_STORAGE_OK);
			registerReceiver(connectivityReceiver, intentFilter);

			blockChainFile = new File(getDir("blockstore", Context.MODE_WORLD_READABLE | Context.MODE_WORLD_WRITEABLE), Constants.BLOCKCHAIN_FILENAME);
			boolean blockChainFileExists = blockChainFile.exists();

			if (!blockChainFileExists)
			{
				Log.d(TAG, "blockchain does not exist, resetting wallet");

				wallet.clearTransactions(0);
			}

			try
			{
				blockStore = new SPVBlockStore(Constants.NETWORK_PARAMETERS, blockChainFile);
				blockStore.getChainHead();

				if (!blockChainFileExists && application.blockExplorerBlockPair != null)
				{
					try
					{            
						StoredBlock stored = new StoredBlock(application.blockExplorerBlockPair.first, BigInteger.valueOf(Long.MAX_VALUE), application.blockExplorerBlockPair.second);

						blockStore.put(stored);
						blockStore.setChainHead(stored);
					}
					catch (final Exception x)
					{
						x.printStackTrace();
					}
				}
			}
			catch (final BlockStoreException x)
			{
				try
				{
					blockStore = new BoundedOverheadBlockStore(Constants.NETWORK_PARAMETERS, blockChainFile);
					blockStore.getChainHead(); 
				}
				catch (final BlockStoreException x2)
				{
					blockChainFile.delete();

					x2.printStackTrace();
					throw new Error("blockstore cannot be created", x2);
				}
			}

			Log.i(TAG, "using " + blockStore.getClass().getName());

			try
			{
				blockChain = new BlockChain(Constants.NETWORK_PARAMETERS, wallet, blockStore);
			}
			catch (final BlockStoreException x)
			{
				throw new Error("blockchain cannot be created", x);
			}

			wallet.addEventListener(walletEventListener);

			registerReceiver(tickReceiver, new IntentFilter(Intent.ACTION_TIME_TICK));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public int onStartCommand(final Intent intent, final int flags, final int startId)
	{
		if (BlockchainService.ACTION_CANCEL_COINS_RECEIVED.equals(intent.getAction()))
		{
			notificationAddresses.clear();

			nm.cancel(NOTIFICATION_ID_COINS_RECEIVED);
		}

		return START_NOT_STICKY;
	}

	@Override
	public void onDestroy()
	{
		try {
			Log.d(TAG, ".onDestroy()");

			try {
				unregisterReceiver(tickReceiver);
			} catch (Exception e) {
				e.printStackTrace();
			}

			if (wallet != null)
				wallet.removeEventListener(walletEventListener);

			if (peerGroup != null)
			{
				peerGroup.removeEventListener(peerConnectivityListener);

				if (wallet != null)
					peerGroup.removeWallet(wallet);

				peerGroup.stop();

				//peerGroup.stopAndWait();

				Log.i(TAG, "peergroup stopped");
			}

			if (peerConnectivityListener != null)
				peerConnectivityListener.stop();

			try {
				if (connectivityReceiver != null)
					unregisterReceiver(connectivityReceiver);
			} catch (Exception e) {
				e.printStackTrace();
			}

			try {
				removeBroadcastPeerState();
				removeBroadcastBlockchainState();
			} catch (Exception e) {
				e.printStackTrace();
			}

			if (prefs != null)
				prefs.edit().putInt(Constants.PREFS_KEY_BEST_CHAIN_HEIGHT_EVER, bestChainHeightEver).commit();

			if (delayHandler != null)
				delayHandler.removeCallbacksAndMessages(null);

			try
			{
				if (blockStore != null)
					blockStore.close();
			}
			catch (final Exception e)
			{
				throw new RuntimeException(e);
			}

			if (application.isInP2PFallbackMode())
				application.saveBitcoinJWallet();

			if (wakeLock != null && wakeLock.isHeld())
			{
				Log.d(TAG, "wakelock still held, releasing");
				wakeLock.release();
			}

			if (blockChainFile != null && !application.isInP2PFallbackMode())
			{
				Log.d(TAG, "removing blockchain");
				blockChainFile.delete();
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		super.onDestroy();
	}

	@Override
	public void onLowMemory()
	{
		Log.w(TAG, "low memory detected, stopping service");
		stopSelf();
	}

	public void broadcastTransaction(final Transaction tx)
	{
		if (peerGroup != null)
			peerGroup.broadcastTransaction(tx);
	}

	public List<Peer> getConnectedPeers()
	{
		if (peerGroup != null)
			return peerGroup.getConnectedPeers();
		else
			return null;
	}

	public List<StoredBlock> getRecentBlocks(final int maxBlocks)
	{
		final List<StoredBlock> blocks = new ArrayList<StoredBlock>(maxBlocks);

		try
		{
			StoredBlock block = blockChain.getChainHead();

			while (block != null)
			{
				blocks.add(block);

				if (blocks.size() >= maxBlocks)
					break;

				block = block.getPrev(blockStore);
			}
		}
		catch (final BlockStoreException x)
		{
			// swallow
		}

		return blocks;
	}

	private void sendBroadcastPeerState(final int numPeers)
	{
		final Intent broadcast = new Intent(ACTION_PEER_STATE);
		broadcast.setPackage(getPackageName());
		broadcast.putExtra(ACTION_PEER_STATE_NUM_PEERS, numPeers);
		sendStickyBroadcast(broadcast);
	}

	private void removeBroadcastPeerState()
	{
		removeStickyBroadcast(new Intent(ACTION_PEER_STATE));
	}

	private void sendBroadcastBlockchainState(final int download)
	{
		final StoredBlock chainHead = blockChain.getChainHead();

		final Intent broadcast = new Intent(ACTION_BLOCKCHAIN_STATE);
		broadcast.setPackage(getPackageName());
		broadcast.putExtra(ACTION_BLOCKCHAIN_STATE_BEST_CHAIN_DATE, chainHead.getHeader().getTime());
		broadcast.putExtra(ACTION_BLOCKCHAIN_STATE_BEST_CHAIN_HEIGHT, chainHead.getHeight());
		broadcast.putExtra(ACTION_BLOCKCHAIN_STATE_REPLAYING, chainHead.getHeight() < bestChainHeightEver);
		broadcast.putExtra(ACTION_BLOCKCHAIN_STATE_DOWNLOAD, download);

		sendStickyBroadcast(broadcast);
	}

	private void removeBroadcastBlockchainState()
	{
		removeStickyBroadcast(new Intent(ACTION_BLOCKCHAIN_STATE));
	}
}

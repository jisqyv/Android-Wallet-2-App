package info.blockchain.wallet.ui;

import android.app.Activity;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.google.bitcoin.core.ECKey;
import com.google.bitcoin.core.NetworkParameters;

import piuk.EventListeners;
import piuk.MyRemoteWallet;
import piuk.blockchain.android.Constants;
import piuk.blockchain.android.WalletApplication;
import piuk.blockchain.android.WalletApplication.AddAddressCallback;
import piuk.blockchain.android.ui.SuccessCallback;

public class AddressManager {
	private MyRemoteWallet blockchainWallet = null;
	private WalletApplication application = null;
	private Activity activity = null;

	public AddressManager(MyRemoteWallet remoteWallet, WalletApplication application, Activity activity) {
		this.blockchainWallet = remoteWallet;
		this.application = application;
		this.activity = activity;
	}	
	
	public void setAddressLabel(final String address, final String label,
			final Runnable checkIfWalletHasUpdatedAndFetchTransactionsFail,
			final Runnable settingLabelFail,
			final Runnable syncingWalletFail) {
		if (blockchainWallet == null)
			return;

		application.checkIfWalletHasUpdatedAndFetchTransactions(blockchainWallet.getTemporyPassword(), new SuccessCallback() {

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

								application.writeException(e);

								application.getHandler().post(syncingWalletFail);
							}
						}
					}.start();
				} catch (Exception e) {
					e.printStackTrace();

					application.getHandler().post(settingLabelFail);
				}
			}

			@Override
			public void onFail() {
				application.getHandler().post(checkIfWalletHasUpdatedAndFetchTransactionsFail);
			}
		});
	}
	
	public void newAddress(final AddAddressCallback callback) {
		final ECKey key = application.getRemoteWallet().generateECKey();			
		addKeyToWallet(key, key.toAddress(NetworkParameters.prodNet()).toString(), null, 0, callback);
	}

	public void addKeyToWallet(final ECKey key, final String address, final String label, final int tag,
			final AddAddressCallback callback) {

		if (blockchainWallet == null) {
			callback.onError("Wallet null.");
			return;
		}

		if (application.isInP2PFallbackMode()) {
			callback.onError("Error saving wallet.");
			return;
		}

		try {
			final boolean success = blockchainWallet.addKey(key, address, label);
			if (success) {
				application.localSaveWallet();

				application.saveWallet(new SuccessCallback() {
					@Override
					public void onSuccess() {
						application.checkIfWalletHasUpdated(blockchainWallet.getTemporyPassword(), false, new SuccessCallback() {
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

			application.writeException(e);

			callback.onError(e.getLocalizedMessage());
		}
	}
	
	
	public boolean deleteAddressBook(final String address) {
		try {
			if (blockchainWallet == null)
				return true;

			blockchainWallet.deleteAddressBook(address);
			
			application.saveWallet(new SuccessCallback() {
				@Override
				public void onSuccess() {
					EventListeners.invokeWalletDidChange();
				}

				@Override
				public void onFail() {
				}
			});
			return true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return false;
	}
	
	public boolean archiveAddress(final String address) {
		return setAddressTag(address, 2);
	}

	public boolean unArchiveAddress(final String address) {
		return setAddressTag(address, 0);
	}

	private boolean setAddressTag(final String address, long tag) {
		try {
			if (blockchainWallet == null)
				return true;

			blockchainWallet.setTag(address, tag);

			application.saveWallet(new SuccessCallback() {
				@Override
				public void onSuccess() {
					EventListeners.invokeWalletDidChange();
				}

				@Override
				public void onFail() {
				}
			});
			return true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return false;
	}
	
	public void setDefaultAddress(final String address) {
		final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
		prefs.edit().putString(Constants.PREFS_KEY_SELECTED_ADDRESS, address.toString()).commit();
	}

}
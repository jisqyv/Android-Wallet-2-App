package info.blockchain.wallet.ui;

import android.widget.Toast;

import com.google.bitcoin.core.ECKey;
import com.google.bitcoin.core.NetworkParameters;

import piuk.EventListeners;
import piuk.MyRemoteWallet;
import piuk.blockchain.android.WalletApplication;
import piuk.blockchain.android.WalletApplication.AddAddressCallback;
import piuk.blockchain.android.ui.SuccessCallback;

public class AddressManager {
	private MyRemoteWallet blockchainWallet;
	private WalletApplication application;
	
	public AddressManager(MyRemoteWallet remoteWallet, WalletApplication application) {
		this.blockchainWallet = remoteWallet;
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
}
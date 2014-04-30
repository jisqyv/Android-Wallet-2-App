package piuk.blockchain.android.ui.dialogs;

import java.lang.ref.WeakReference;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.bitcoin.core.ECKey;
import com.google.bitcoin.core.NetworkParameters;
import com.google.bitcoin.core.Transaction;

import piuk.MyRemoteWallet;
import piuk.MyWallet;
import piuk.MyRemoteWallet.SendProgress;
import piuk.blockchain.android.R;
import piuk.blockchain.android.WalletApplication;
import piuk.blockchain.android.WalletApplication.AddAddressCallback;
import piuk.blockchain.android.ui.EditAddressBookEntryFragment;
import piuk.blockchain.android.ui.SuccessCallback;
import piuk.blockchain.android.ui.SendCoinsFragment.FeePolicy;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

public class RekeyWalletDialog extends DialogFragment {
	private static final String FRAGMENT_TAG = RekeyWalletDialog.class.getName();
	private static List<WeakReference<DialogFragment>> fragmentRefs = new ArrayList<WeakReference<DialogFragment>>();
	private WalletApplication application;
	private SuccessCallback callback = null;

	public static void hide() {
		for (WeakReference<DialogFragment> fragmentRef : fragmentRefs) {
			if (fragmentRef != null && fragmentRef.get() != null) {
				try {
					fragmentRef.get().dismissAllowingStateLoss();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	public static RekeyWalletDialog show(final FragmentManager fm, WalletApplication application, SuccessCallback callback) {
		final DialogFragment prev = (DialogFragment) fm.findFragmentById(R.layout.ask_to_rekey_wallet_dialog);

		final FragmentTransaction ft = fm.beginTransaction();

		if (prev != null) {
			prev.dismiss();
			ft.remove(prev);
		}

		ft.addToBackStack(null);

		final RekeyWalletDialog newFragment = instance();

		newFragment.application = application;

		newFragment.callback = callback;

		newFragment.show(ft, FRAGMENT_TAG);

		return newFragment;
	}

	static RekeyWalletDialog instance() {
		final RekeyWalletDialog fragment = new RekeyWalletDialog();

		fragmentRefs.add(new WeakReference<DialogFragment>(fragment));

		return fragment;
	}

	public void dismissDialogWithError(String error) {

		application.setHasAskedRekeyedWallet(false);

		callback.onFail();

		Toast.makeText(application.getApplicationContext(),
				error,
				Toast.LENGTH_LONG).show();

		dismiss();
	}


	public void dismissDialogWithError(int error) {

		application.setHasAskedRekeyedWallet(false);

		callback.onFail();

		Toast.makeText(application.getApplicationContext(),
				error,
				Toast.LENGTH_LONG).show();

		dismiss();
	}


	public void dismissDialogWithSuccess() {

		application.setHasAskedRekeyedWallet(true);

		callback.onSuccess();

		Toast.makeText(application.getApplicationContext(),
				R.string.success_rekeying_wallet,
				Toast.LENGTH_LONG).show();

		dismiss();
	}

	public static boolean hasKnownAndroidAddresses(final MyRemoteWallet wallet) {
		for (Map<String, Object> keyMap : wallet.getKeysMap()) {

			if (keyMap != null) {
				String created_device_name = (String)keyMap.get("created_device_name");

				//Only archive keys which are not watch only
				if (created_device_name != null && created_device_name.equals("android")) {
					return true;
				}
			}
		} 

		return false;
	}

	public static List<String> getPossiblyInsecureAddresses(final MyRemoteWallet wallet) {
		final List<String> insecure_addresses = new ArrayList<String>();


		for (Map<String, Object> keyMap  : wallet.getKeysMap()) {	
			//Only include active addresses
			if (keyMap.get("tag") == null || (Long) keyMap.get("tag") == 0) {
				//Only archive keys which are not watch only
				if (keyMap.get("priv") != null && keyMap.get("created_device_name") == null) {
					insecure_addresses.add((String) keyMap.get("addr"));
				}
			}
		} 

		return insecure_addresses;
	}

	public void unarchive(final MyRemoteWallet wallet, final List<String> addresses) {
		for (String address : addresses) {
			wallet.setTag(address, 0);
		}
	}

	public void archive(final MyRemoteWallet wallet, final List<String> addresses) {
		for (String address : addresses) {
			wallet.setTag(address, 2);
		}
	}


	public void reKeyWallet(final MyRemoteWallet wallet, final Handler handler) {
		final BigInteger baseFee = wallet.getBaseFee();

		application.doMultiAddr(false, new SuccessCallback() {
			@Override
			public void onSuccess() {

				final List<String> insecure_addresses = getPossiblyInsecureAddresses(wallet);

				if (insecure_addresses.size() == 0) {
					handler.post(new Runnable() {
						@Override
						public void run() {
							dismissDialogWithSuccess();
						}
					});
					return;
				}

				ECKey key = wallet.generateECKey();
				
				//Generate a new address which we will sweep to
				application.addKeyToWallet(key, key.toAddress(NetworkParameters.prodNet()).toString(), null, 0, new AddAddressCallback() {
					public void onSavedAddress(final String new_address) {
						
						BigInteger balanceOfInsecure = BigInteger.ZERO;
						for (String insecure_address : insecure_addresses) {
							balanceOfInsecure = balanceOfInsecure.add(wallet.getBalance(insecure_address));
						}

						final BigInteger balanceOfInsecureFinal = balanceOfInsecure;

						//If the wallet balance is ZERO no need to sweep anything
						//So just save the wallet and dismiss
						if (balanceOfInsecure.compareTo(BigInteger.ZERO) == 0) {
							archive(wallet, insecure_addresses);

							application.saveWallet(new SuccessCallback() {
								@Override
								public void onSuccess() {
									handler.post(new Runnable() {
										public void run() {
											dismissDialogWithSuccess();
										}
									});	
								}

								@Override
								public void onFail() {
									handler.post(new Runnable() {
										public void run() {
											unarchive(wallet, insecure_addresses);

											dismissDialogWithError(R.string.error_rekeying_wallet);
										}
									});	
								}
							});

							//If the wallet balance is less than 2x the base fee don't try and sweep as its a waste of money
						} else if (balanceOfInsecure.compareTo(baseFee.multiply(BigInteger.valueOf(2))) < 0) {
							handler.post(new Runnable() {
								@Override
								public void run() {
									dismissDialogWithError("Insufficient Balance To Rekey Wallet");

									//Set it to true anyway because we will keep failing
									application.setHasAskedRekeyedWallet(true);
								}
							});
						} else {
							//Else save and sweep the wallet
							String[] type = new String[0];
							wallet.sendCoinsAsync(insecure_addresses.toArray(type), new_address, balanceOfInsecureFinal.subtract(baseFee), FeePolicy.FeeForce, baseFee, new SendProgress() {
								@Override
								public boolean onReady(
										Transaction tx,
										BigInteger fee,
										FeePolicy feePolicy,
										long priority) {
									return true;
								}

								@Override
								public void onSend(Transaction tx, String message) {
									handler.post(new Runnable() {
										public void run() {
											//Archive the old addresses
											archive(wallet, insecure_addresses);

											application.saveWallet(new SuccessCallback() {
												@Override
												public void onFail() {
													handler.post(new Runnable() {
														@Override
														public void run() {
															dismissDialogWithError(R.string.error_rekeying_wallet);
															
															//Set it to true anyway because we will keep failing
															application.setHasAskedRekeyedWallet(true);
														}
													});
												}

												@Override
												public void onSuccess() {
													handler.post(new Runnable() {
														@Override
														public void run() {
															dismissDialogWithSuccess();
														}
													});
												}
											});	
										}
									});	
								}

								@Override
								public ECKey onPrivateKeyMissing(String address) {
									return null;
								}

								@Override
								public void onError(final String message) {
									handler.post(new Runnable() {
										public void run() {
											dismissDialogWithError(message);
										}
									});		
								}

								@Override
								public void onProgress(String message) {
									//Ignore
								}
							});	
						}
					}

					public void onError(String reason) {
						handler.post(new Runnable() {
							@Override
							public void run() {
								dismissDialogWithError(R.string.error_rekeying_wallet);
							}
						});
					}
				});
			}

			@Override
			public void onFail() {				
				handler.post(new Runnable() {
					@Override
					public void run() {
						dismissDialogWithError(R.string.error_rekeying_wallet);
					}
				});
			}
		});
	}

	@Override
	public Dialog onCreateDialog(final Bundle savedInstanceState) {
		super.onCreateDialog(savedInstanceState);

		final FragmentActivity activity = getActivity();
		final LayoutInflater inflater = LayoutInflater.from(activity);

		final Builder dialog = new AlertDialog.Builder(new ContextThemeWrapper(activity, R.style.Theme_Dialog))
		.setTitle(R.string.security_announcement);

		final View view = inflater.inflate(R.layout.ask_to_rekey_wallet_dialog, null);		

		dialog.setView(view);

		dialog.setCancelable(false);

		final Button rekeyButton = (Button) view.findViewById(R.id.rekey_continue_button);
		final Button ignoreButton = (Button) view.findViewById(R.id.rekey_ignore_button);
		final Handler handler = new Handler();

		rekeyButton.setClickable(true);
		ignoreButton.setClickable(true);

		rekeyButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				rekeyButton.setClickable(false);
				ignoreButton.setClickable(false);

				final MyRemoteWallet wallet = application.getRemoteWallet();

				if (wallet == null || wallet.isNew() || application.decryptionErrors > 0)
					return;

				if (wallet.isDoubleEncrypted() && wallet.temporySecondPassword == null) {
					RequestPasswordDialog.show(getFragmentManager(), new SuccessCallback() {
						public void onSuccess() {
							reKeyWallet(wallet, handler);
						}

						public void onFail() {
							dismissDialogWithError(R.string.error_rekeying_wallet);
						}
					}, RequestPasswordDialog.PasswordTypeSecond);
				} else {
					reKeyWallet(wallet, handler);
				}
			}
		});

		ignoreButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				rekeyButton.setClickable(false);
				ignoreButton.setClickable(false);

				application.setHasAskedRekeyedWallet(true);

				callback.onFail();
			}
		});

		Dialog d = dialog.create();

		WindowManager.LayoutParams lp = new WindowManager.LayoutParams();

		lp.dimAmount = 0;
		lp.width = WindowManager.LayoutParams.FILL_PARENT;
		lp.height = WindowManager.LayoutParams.WRAP_CONTENT;

		d.show();

		d.getWindow().setAttributes(lp);

		d.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

		return d;
	}	
}

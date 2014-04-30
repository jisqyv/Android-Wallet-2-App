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

package piuk.blockchain.android.ui.dialogs;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;

import piuk.EventListeners;
import piuk.blockchain.android.Constants;
import piuk.blockchain.android.R;
import piuk.blockchain.android.WalletApplication;
import piuk.blockchain.android.ui.AbstractWalletActivity;
import piuk.blockchain.android.ui.PinEntryActivity;
import piuk.blockchain.android.ui.SuccessCallback;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences.Editor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Patterns;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

/**
 * @author Andreas Schildbach
 */
public final class NewAccountDialog extends DialogFragment {
	private static final String FRAGMENT_TAG = NewAccountDialog.class.getName();
	private static List<WeakReference<NewAccountDialog>> fragmentRefs = new ArrayList<WeakReference<NewAccountDialog>>();

	private WalletApplication application;

	public static Bitmap loadBitmap(String url) throws MalformedURLException,
	IOException {
		Bitmap bitmap = null;

		final byte[] data = IOUtils.toByteArray(new URL(url).openStream());
		BitmapFactory.Options options = new BitmapFactory.Options();

		bitmap = BitmapFactory.decodeByteArray(data, 0, data.length, options);

		return bitmap;
	}


	public static void hide() {
		for (WeakReference<NewAccountDialog> fragmentRef : fragmentRefs) {
			if (fragmentRef != null && fragmentRef.get() != null) {
				try {
					fragmentRef.get().dismissAllowingStateLoss();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	public static NewAccountDialog show(final FragmentManager fm, WalletApplication application) {
		final DialogFragment prev = (DialogFragment) fm
				.findFragmentById(R.layout.new_account_dialog);

		final FragmentTransaction ft = fm.beginTransaction();

		if (prev != null) {
			prev.dismiss();
			ft.remove(prev);
		}

		ft.addToBackStack(null);

		final NewAccountDialog newFragment = instance();

		newFragment.application = application;

		newFragment.show(ft, FRAGMENT_TAG);

		return newFragment;
	}

	@Override
	public void onCancel(DialogInterface dialog) {	
		WelcomeDialog.show(getFragmentManager(), getActivity(), application);
	}


	static NewAccountDialog instance() {
		final NewAccountDialog fragment = new NewAccountDialog();

		fragmentRefs.add(new WeakReference<NewAccountDialog>(fragment));

		return fragment;
	}

	@Override
	public Dialog onCreateDialog(final Bundle savedInstanceState) {
		super.onCreateDialog(savedInstanceState);

		final FragmentActivity activity = getActivity();
		final LayoutInflater inflater = LayoutInflater.from(activity);

		final Builder dialog = new AlertDialog.Builder(new ContextThemeWrapper(activity, R.style.Theme_Dialog))
		.setTitle(R.string.new_account_title);

		final View view = inflater.inflate(R.layout.new_account_dialog, null);		

		dialog.setView(view);

		final Button createButton = (Button) view.findViewById(R.id.create_button);
		final TextView passwordField = (TextView) view.findViewById(R.id.password);
		final TextView passwordField2 = (TextView) view.findViewById(R.id.password2);
		final TextView emailField = (TextView) view.findViewById(R.id.email);

		final Pattern emailPattern = Patterns.EMAIL_ADDRESS; // API level 8+
		Account[] accounts = AccountManager.get(getActivity().getApplicationContext()).getAccounts();
		for (Account account : accounts) {
			if (emailPattern.matcher(account.name).matches()) {
				emailField.setText(account.name);
				break;
			}
		}

		createButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				final WalletApplication application = (WalletApplication) getActivity()
						.getApplication();

				if (passwordField.getText().length() < 11
						|| passwordField.getText().length() > 255) {
					Toast.makeText(application,
							R.string.new_account_password_length_error,
							Toast.LENGTH_LONG).show();
					return;
				}

				if (!passwordField2.getText().toString()
						.equals(passwordField2.getText().toString())) {
					Toast.makeText(application,
							R.string.new_account_password_mismatch_error,
							Toast.LENGTH_LONG).show();
					return;
				}

				if (emailField.getText().length() > 0 && !emailPattern.matcher(emailField.getText()).matches()) {
					Toast.makeText(application,
							R.string.new_account_password_invalid_email,
							Toast.LENGTH_LONG).show();
					return;
				}

				final ProgressDialog progressDialog = ProgressDialog.show(
						getActivity(), "",
						getString(R.string.creating_account), true);

				progressDialog.show();

				final Handler handler = new Handler();

				new Thread() {
					@Override
					public void run() {
						try {
							try {
								application.generateNewWallet();
							} catch (Exception e1) {
								throw new Exception("Error Generating Wallet");
							}

							final String guid = application.getRemoteWallet().getGUID();
							final String sharedKey = application.getRemoteWallet().getSharedKey();
							final String password = passwordField.getText().toString();
							final String email = emailField.getText().toString();

							application.getRemoteWallet().setTemporyPassword(password);

							if (!application.getRemoteWallet().remoteSave(email)) {
								throw new Exception("Unknown Error Inserting wallet");
							}

							EventListeners.invokeWalletDidChange();

							handler.post(new Runnable() {
								public void run() {
									try {
										progressDialog.dismiss();

										dismiss();

										final AbstractWalletActivity activity = (AbstractWalletActivity) getActivity();

										Toast.makeText(activity.getApplication(),
												R.string.new_account_success,
												Toast.LENGTH_LONG).show();

										PinEntryActivity.clearPrefValues(application);

										Editor edit = PreferenceManager
												.getDefaultSharedPreferences(
														application
														.getApplicationContext())
														.edit();

										edit.putString("guid", guid);
										edit.putString("sharedKey", sharedKey);

										if (edit.commit()) {
											handler.post(new Runnable() {

												@Override
												public void run() {
													application.checkIfWalletHasUpdated(password, guid, sharedKey, true, new SuccessCallback(){

														@Override
														public void onSuccess() {	
															activity.registerNotifications();
														}

														@Override
														public void onFail() {
															Toast.makeText(application, R.string.toast_error_syncing_wallet, Toast.LENGTH_LONG)
															.show();
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

										Toast.makeText(activity.getApplication(),
												e.getLocalizedMessage(),
												Toast.LENGTH_LONG).show();
									}
								}
							});
						} catch (final Exception e) {
							e.printStackTrace();

							application.clearWallet();

							handler.post(new Runnable() {
								public void run() {
									progressDialog.dismiss();

									Toast.makeText(activity.getApplication(),
											e.getLocalizedMessage(),
											Toast.LENGTH_LONG).show();
								}
							});
						}
					}
				}.start();
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

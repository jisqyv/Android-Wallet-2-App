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

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.text.InputType;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.TextView;
import android.view.Gravity;
import android.widget.Toast;
import piuk.blockchain.android.EventListeners;
import piuk.blockchain.android.MyRemoteWallet;
import piuk.blockchain.android.R;
import piuk.blockchain.android.WalletApplication;
import piuk.blockchain.android.ui.AbstractWalletActivity;
import piuk.blockchain.android.SuccessCallback;

/**
 * @author Andreas Schildbach
 */
public final class RequestForgotPasswordDialog extends DialogFragment {
	private static final String FRAGMENT_TAG = RequestForgotPasswordDialog.class.getName();
	private SuccessCallback callback = null;
	private static List<WeakReference<RequestForgotPasswordDialog>> fragmentRefs = new ArrayList<WeakReference<RequestForgotPasswordDialog>>();

	private static String passwordResult;

	public static String getPasswordResult() {
		String t = passwordResult; 

		passwordResult = null;

		return t;
	}
	
	public static void hide() {
		for (WeakReference<RequestForgotPasswordDialog> fragmentRef : fragmentRefs) {
			RequestForgotPasswordDialog ref = fragmentRef.get();
			
			if (ref != null) {
				try {
					ref.dismissAllowingStateLoss();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	public static DialogFragment show(final FragmentManager fm, SuccessCallback callback) {

		final DialogFragment prev = (DialogFragment) fm.findFragmentById(R.layout.forgot_password_dialog);

		final FragmentTransaction ft = fm.beginTransaction();

		if (prev != null) {
			prev.dismiss();
			ft.remove(prev);
		}

		ft.addToBackStack(null);

		final RequestForgotPasswordDialog newFragment = instance();

		newFragment.show(ft, FRAGMENT_TAG);

		newFragment.callback = callback;
		
		return newFragment;
	}

	private static RequestForgotPasswordDialog instance() {
		final RequestForgotPasswordDialog fragment = new RequestForgotPasswordDialog();

		fragmentRefs.add(new WeakReference<RequestForgotPasswordDialog>(fragment));

		return fragment;
	}

	@Override
	public void onCancel(DialogInterface dialog) {
		callback.onFail();
	}

	@Override
	public Dialog onCreateDialog(final Bundle savedInstanceState) {
		final FragmentActivity activity = (FragmentActivity) getActivity();

		final WalletApplication application = (WalletApplication) activity
				.getApplication();

		final LayoutInflater inflater = LayoutInflater.from(activity);

		final Builder dialog = new AlertDialog.Builder(new ContextThemeWrapper(activity, R.style.Theme_Dialog));

		dialog.setTitle(R.string.main_password_title);

		final View view = inflater.inflate(R.layout.forgot_password_dialog, null);

		dialog.setView(view);

		final TextView passwordField = (TextView) view.findViewById(R.id.password_field);

		final TextView titleTextView = (TextView) view.findViewById(R.id.title_text_view);

		titleTextView.setText(R.string.main_password_text);

		final Button continueButton = (Button) view.findViewById(R.id.password_continue);
		final Button cancelButton = (Button) view.findViewById(R.id.cancel);

		continueButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				
				try {
					if (passwordField.getText().toString().trim() == null || passwordField.getText().toString().trim().length() < 1) {
 						callback.onFail();
					}
					
 					String localWallet = application.readLocalWallet();
 					if (!application.decryptLocalWallet(localWallet, passwordField.getText().toString().trim())) {
 						callback.onFail();
 						return;
 					}
 					
					String password = passwordField.getText().toString().trim();

					application.checkIfWalletHasUpdatedAndFetchTransactions(password, new SuccessCallback() {
						@Override
						public void onSuccess() {
							dismiss();
							callback.onSuccess();
						}

						@Override
						public void onFail() {
							dismiss();
							callback.onFail();
						}
					});
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});

		cancelButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				dismiss();
			}
		});

		Dialog d = dialog.create();

		WindowManager.LayoutParams lp = new WindowManager.LayoutParams();

		lp.dimAmount = 0;
		lp.width = WindowManager.LayoutParams.FILL_PARENT;
		lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
//		lp.gravity = Gravity.BOTTOM;
		
		d.show();

		d.getWindow().setAttributes(lp);

		d.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

		return d;
	}
	
}

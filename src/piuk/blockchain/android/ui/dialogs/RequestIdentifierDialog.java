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
import java.util.UUID;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import piuk.blockchain.android.R;
import piuk.blockchain.android.ui.InvalidGUIDException;

/**
 * @author Andreas Schildbach
 */
public final class RequestIdentifierDialog extends DialogFragment {
	private static final String FRAGMENT_TAG = RequestIdentifierDialog.class
			.getName();

	public interface SuccessCallback {
		public void onSuccess(String guid);

		public void onFail(String message);
	}

	private SuccessCallback callback = null;
	private static List<WeakReference<RequestIdentifierDialog>> fragmentRefs = new ArrayList<WeakReference<RequestIdentifierDialog>>();

	public static void hide() {
		for (WeakReference<RequestIdentifierDialog> fragmentRef : fragmentRefs) {
			if (fragmentRef != null && fragmentRef.get() != null) {
				try {
					fragmentRef.get().dismissAllowingStateLoss();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}
	public static DialogFragment show(final FragmentManager fm,
			SuccessCallback callback) {

		final DialogFragment prev = (DialogFragment) fm.findFragmentById(R.layout.wallet_identifier_dialog);

		final FragmentTransaction ft = fm.beginTransaction();

		if (prev != null) {
			prev.dismiss();
			ft.remove(prev);
		}

		ft.addToBackStack(null);

		final RequestIdentifierDialog newFragment = instance();

		newFragment.show(ft, FRAGMENT_TAG);

		newFragment.callback = callback;

		return newFragment;
	}

	private static RequestIdentifierDialog instance() {
		final RequestIdentifierDialog fragment = new RequestIdentifierDialog();

		fragmentRefs.add(new WeakReference<RequestIdentifierDialog>(fragment));

		return fragment;
	}

	@Override
	public void onCancel(DialogInterface dialog) {
		callback.onFail("User Cancelled");
	}

	public static void validateGUIDorThrow(String input_guid) throws InvalidGUIDException {
		if (input_guid == null)
			throw new InvalidGUIDException();

		if (input_guid.length() != 36)
			throw new InvalidGUIDException();
		try {
			input_guid = UUID.fromString(input_guid).toString(); //Check is valid uuid format
		} catch (IllegalArgumentException e) {
			throw new InvalidGUIDException();
		}
	}

	@Override
	public Dialog onCreateDialog(final Bundle savedInstanceState) {
		final FragmentActivity activity = getActivity();
		final LayoutInflater inflater = LayoutInflater.from(activity);

		final Builder dialog = new AlertDialog.Builder(new ContextThemeWrapper(activity, R.style.Theme_Dialog));

		dialog.setTitle(R.string.wallet_identifier_title);

		final View view = inflater.inflate(R.layout.wallet_identifier_dialog, null);

		dialog.setView(view);

		final Button continueButton = (Button) view.findViewById(R.id.identifier_continue);

		continueButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {

				try {
					final TextView identifierField = (TextView) view.findViewById(R.id.identifier_field);

					final String guid = identifierField.getText().toString();

					validateGUIDorThrow(guid); 

					if (callback != null)
						callback.onSuccess(guid);

					dismiss();
				} catch (Exception e) {
					e.printStackTrace();

					dismiss();

					if (callback != null)
						callback.onFail(e.getLocalizedMessage());
				}
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

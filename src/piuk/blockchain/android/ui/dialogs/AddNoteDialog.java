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
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import piuk.Hash;
import piuk.MyRemoteWallet;
import piuk.blockchain.android.R;
import piuk.blockchain.android.WalletApplication;
import piuk.blockchain.android.ui.SuccessCallback;

/**
 * @author Andreas Schildbach
 */
public final class AddNoteDialog extends DialogFragment {
	private static final String FRAGMENT_TAG = AddNoteDialog.class
			.getName();
	private static List<WeakReference<AddNoteDialog>> fragmentRefs = new ArrayList<WeakReference<AddNoteDialog>>();

	private String tx;

	public static void hide() {
		for (WeakReference<AddNoteDialog> fragmentRef : fragmentRefs) {
			if (fragmentRef != null && fragmentRef.get() != null) {
				try {
					fragmentRef.get().dismissAllowingStateLoss();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	public static DialogFragment showDialog(final FragmentManager fm, String tx) {

		final DialogFragment prev = (DialogFragment) fm
				.findFragmentById(R.layout.add_note_dialog);

		final FragmentTransaction ft = fm.beginTransaction();

		if (prev != null) {
			prev.dismiss();
			ft.remove(prev);
		}

		ft.addToBackStack(null);

		final AddNoteDialog newFragment = instance();

		newFragment.show(ft, FRAGMENT_TAG);

		newFragment.tx = tx;

		return newFragment;
	}

	private static AddNoteDialog instance() {
		final AddNoteDialog fragment = new AddNoteDialog();

		fragmentRefs.add(new WeakReference<AddNoteDialog>(fragment));

		return fragment;
	}

	@Override
	public Dialog onCreateDialog(final Bundle savedInstanceState) {
		final FragmentActivity activity = getActivity();
		final LayoutInflater inflater = LayoutInflater.from(activity);
		final WalletApplication application = (WalletApplication) activity.getApplication();
		final MyRemoteWallet wallet = application.getRemoteWallet();

		if (wallet == null)
			return null;
		
		final Builder dialog = new AlertDialog.Builder(new ContextThemeWrapper(activity, R.style.Theme_Dialog));		

		String existingNote = wallet.getTxNotes().get(tx);
		
		final View view = inflater.inflate(R.layout.add_note_dialog, null);

		dialog.setView(view);

		final EditText noteView = (EditText)view.findViewById(R.id.edit_note_field);
		final Button saveButton = (Button)view.findViewById(R.id.save_note_button);

		if (existingNote != null) { 
			noteView.setText(existingNote);
			
			dialog.setTitle(R.string.edit_note);
		} else {
			dialog.setTitle(R.string.add_note);
		}
		
		saveButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {

				try {
					String text = noteView.getText().toString().trim();

					if (text.length() == 0) {
						Toast.makeText(activity.getApplication(),
								R.string.please_enter_note,
								Toast.LENGTH_SHORT).show();
						return;
					}

					if (wallet.addTxNote(new Hash(tx), text)) {
						application.saveWallet(new SuccessCallback() {
							@Override
							public void onSuccess() {

								Toast.makeText(activity.getApplication(),
										R.string.note_saved,
										Toast.LENGTH_SHORT).show();

								dismiss();
							}

							@Override
							public void onFail() {
								
								Toast.makeText(activity.getApplication(),
										R.string.toast_error_syncing_wallet,
										Toast.LENGTH_SHORT).show();

								dismiss();
							}
						});
					}
				} catch (Exception e) {
					e.printStackTrace();

					Toast.makeText(activity.getApplication(),
							e.getLocalizedMessage(),
							Toast.LENGTH_SHORT).show();
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

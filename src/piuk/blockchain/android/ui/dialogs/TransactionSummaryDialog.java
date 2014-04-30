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
import java.math.BigInteger;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.spongycastle.util.encoders.Hex;

import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.core.TransactionConfidence.ConfidenceType;
import com.google.bitcoin.core.TransactionInput;
import com.google.bitcoin.core.TransactionOutput;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.AlertDialog.Builder;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.text.SpannableString;
import android.text.style.UnderlineSpan;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import piuk.MyRemoteWallet;
import piuk.MyTransaction;
import piuk.blockchain.android.Constants;
import piuk.blockchain.android.R;
import piuk.blockchain.android.WalletApplication;
import piuk.blockchain.android.util.WalletUtils;

/**
 * @author Andreas Schildbach
 */
public final class TransactionSummaryDialog extends DialogFragment
{
	private static final String FRAGMENT_TAG = TransactionSummaryDialog.class.getName();
	private static List<WeakReference<TransactionSummaryDialog>> fragmentRefs = new ArrayList<WeakReference<TransactionSummaryDialog>>();

	private WalletApplication application;

	private Transaction tx;

	private static final String WebROOT = "https://"+Constants.BLOCKCHAIN_DOMAIN+"/tx-summary";

	private FragmentActivity activity;

	private DateFormat dateFormat;

	@Override
	public void onAttach(final Activity activity)
	{
		super.onAttach(activity);

		this.activity = (FragmentActivity) activity;

		dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
	}

	public static void hide() {
		for (WeakReference<TransactionSummaryDialog> fragmentRef : fragmentRefs) {
			if (fragmentRef != null && fragmentRef.get() != null) {
				try {
					fragmentRef.get().dismissAllowingStateLoss();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	public static TransactionSummaryDialog show(final FragmentManager fm, WalletApplication application, Transaction tx) {
		final DialogFragment prev = (DialogFragment) fm
				.findFragmentById(R.layout.transaction_summary_fragment);

		final FragmentTransaction ft = fm.beginTransaction();

		if (prev != null) {
			prev.dismiss();
			ft.remove(prev);
		}

		ft.addToBackStack(null);

		final TransactionSummaryDialog newFragment = instance();

		newFragment.application = application;

		newFragment.show(ft, FRAGMENT_TAG);

		newFragment.tx = tx;

		return newFragment;
	}


	static TransactionSummaryDialog instance() {
		final TransactionSummaryDialog fragment = new TransactionSummaryDialog();

		fragmentRefs.add(new WeakReference<TransactionSummaryDialog>(fragment));

		return fragment;
	}

	public static JSONObject getTransactionSummary(long txIndex, String guid, long result) throws Exception {
		String url = WebROOT + "/"+ txIndex + "?guid="+guid+"&result="+result+"&format=json";

		String response = WalletUtils.getURL(url);	

		return (JSONObject) new JSONParser().parse(response);
	}

	@Override
	public Dialog onCreateDialog(final Bundle savedInstanceState) {
		super.onCreateDialog(savedInstanceState);

		final FragmentActivity activity = getActivity();

		final LayoutInflater inflater = LayoutInflater.from(activity);

		final Builder dialog = new AlertDialog.Builder(new ContextThemeWrapper(activity, R.style.Theme_Dialog))
		.setTitle(R.string.transaction_summary_title);

		final LinearLayout view = (LinearLayout)inflater.inflate(R.layout.transaction_summary_fragment, null);		

		dialog.setView(view);

		try {
			final MyRemoteWallet wallet = application.getRemoteWallet();

			if (wallet == null) {
				return null;
			}
			
			BigInteger totalOutputValue = BigInteger.ZERO;
			for (TransactionOutput output : tx.getOutputs()) {
				totalOutputValue = totalOutputValue.add(output.getValue());
			}

			final TextView resultDescriptionView = (TextView) view.findViewById(R.id.result_description);
			final TextView toView = (TextView) view.findViewById(R.id.transaction_to);
			final TextView toViewLabel = (TextView) view.findViewById(R.id.transaction_to_label);
			final View toViewContainer = (View) view.findViewById(R.id.transaction_to_container);
			final TextView hashView = (TextView) view.findViewById(R.id.transaction_hash);
			final TextView transactionTimeView = (TextView) view.findViewById(R.id.transaction_date);
			final TextView confirmationsView = (TextView) view.findViewById(R.id.transaction_confirmations);
			final TextView noteView = (TextView) view.findViewById(R.id.transaction_note);
			final Button addNoteButton = (Button) view.findViewById(R.id.add_note_button);
			final TextView feeView = (TextView) view.findViewById(R.id.transaction_fee);
			final View feeViewContainer = view.findViewById(R.id.transaction_fee_container);
			final TextView valueNowView = (TextView) view.findViewById(R.id.transaction_value);
			final View valueNowContainerView = view.findViewById(R.id.transaction_value_container);

			String to = null;
			for (TransactionOutput output : tx.getOutputs()) {
				try {
					String toAddress = output.getScriptPubKey().getToAddress().toString();
					if (!wallet.isAddressMine(toAddress)) {
						to = toAddress;
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			String from = null;
			for (TransactionInput input : tx.getInputs()) {
				try {
					String fromAddress = input.getFromAddress().toString();
					if (!wallet.isAddressMine(fromAddress)) {
						from = fromAddress;
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			long realResult = 0; 
			int confirmations = 0;

			if (tx instanceof MyTransaction) {
				MyTransaction myTx = (MyTransaction)tx;

				realResult = myTx.getResult().longValue();

				if (wallet.getLatestBlock() != null) {
					confirmations = wallet.getLatestBlock().getHeight() - myTx.getHeight() + 1;
				}

			} else if (application.isInP2PFallbackMode()) {				
				realResult = tx.getValue(application.bitcoinjWallet).longValue();

				if (tx.getConfidence().getConfidenceType() == ConfidenceType.BUILDING)
					confirmations = tx.getConfidence().getDepthInBlocks();
			}

			final long finalResult = realResult;

			if (realResult <= 0) {
				toViewLabel.setText(R.string.transaction_fragment_to);

				if (to == null) {
					((LinearLayout)toViewContainer.getParent()).removeView(toViewContainer);
				} else {
					toView.setText(to);
				}
			} else {
				toViewLabel.setText(R.string.transaction_fragment_from);

				if (from == null) {
					((LinearLayout)toViewContainer.getParent()).removeView(toViewContainer);
				} else {
					toView.setText(from);
				}
			}

			//confirmations view
			if (confirmations> 0) {
				confirmationsView.setText(""+confirmations);
			} else {
				confirmationsView.setText("Unconfirmed");
			}

			//Hash String view
			final String hashString = new String(Hex.encode(tx.getHash().getBytes()), "UTF-8");

			hashView.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					Intent browserIntent = new Intent(
							Intent.ACTION_VIEW,
							Uri.parse("https:/"+Constants.BLOCKCHAIN_DOMAIN+"/tx/"+hashString));

					startActivity(browserIntent);
				}
			});
			
			
			//Notes View
			String note = wallet.getTxNotes().get(hashString);

			if (note == null) {
				addNoteButton.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						dismiss();

						AddNoteDialog.showDialog(getFragmentManager(), hashString);
					}
				});

				view.removeView(noteView);
			} else {
				view.removeView(addNoteButton);

				noteView.setText(note);

				noteView.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						dismiss();

						AddNoteDialog.showDialog(getFragmentManager(), hashString);
					}
				});
			}
			
			addNoteButton.setEnabled(!application.isInP2PFallbackMode());

			SpannableString content = new SpannableString(hashString);
			content.setSpan(new UnderlineSpan(), 0, content.length(), 0);
			hashView.setText(content);
			

			if (realResult > 0 && from != null)
				resultDescriptionView.setText(this.getString(R.string.transaction_fragment_amount_you_received, WalletUtils.formatValue(BigInteger.valueOf(realResult))));
			else if (realResult < 0 && to != null)
				resultDescriptionView.setText(this.getString(R.string.transaction_fragment_amount_you_sent, WalletUtils.formatValue(BigInteger.valueOf(realResult))));
			else
				resultDescriptionView.setText(this.getString(R.string.transaction_fragment_amount_you_moved, WalletUtils.formatValue(totalOutputValue)));

			final Date time = tx.getUpdateTime();

			transactionTimeView.setText(dateFormat.format(time));

			//These will be made visible again later once information is fetched from server
			feeViewContainer.setVisibility(View.GONE);
			valueNowContainerView.setVisibility(View.GONE);
			
			if (tx instanceof MyTransaction) {
				MyTransaction myTx = (MyTransaction)tx;

				final long txIndex = myTx.getTxIndex();
				
				final Handler handler = new Handler();

				new Thread(new Runnable() {
					@Override
					public void run() {
						try {							
							final JSONObject obj = getTransactionSummary(txIndex, wallet.getGUID(), finalResult);

							handler.post(new Runnable() {
								@Override
								public void run() {
									try {
										if (obj.get("fee") != null) {
											feeViewContainer.setVisibility(View.VISIBLE);

											feeView.setText(WalletUtils.formatValue(BigInteger.valueOf(Long.valueOf(obj.get("fee").toString()))) + " BTC");
										}

										if (obj.get("confirmations") != null) {
											int confirmations = ((Number)obj.get("confirmations")).intValue();

											confirmationsView.setText(""+confirmations);
										}

										String result_local = (String) obj.get("result_local");
										String result_local_historical = (String) obj.get("result_local_historical");

										if (result_local != null && result_local.length() > 0) {
											valueNowContainerView.setVisibility(View.VISIBLE);

											if (result_local_historical == null || result_local_historical.length() == 0 || result_local_historical.equals(result_local)) {
												valueNowView.setText(result_local);
											} else {
												valueNowView.setText(getString(R.string.value_now_ten, result_local, result_local_historical));
											}
										}
									} catch (Exception e) {
										e.printStackTrace();
									}
								}
							});
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}).start();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

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

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

package piuk.blockchain.android.ui;

import java.math.BigInteger;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;

import com.google.bitcoin.core.Address;
import com.google.bitcoin.core.NetworkParameters;
import com.google.bitcoin.core.ScriptException;
import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.core.TransactionConfidence;
import com.google.bitcoin.core.TransactionConfidence.ConfidenceType;

import piuk.EventListeners;
import piuk.MyTransaction;
import piuk.blockchain.android.AddressBookProvider;
import piuk.blockchain.android.R;
import piuk.blockchain.android.WalletApplication;
import piuk.blockchain.android.ui.dialogs.TransactionSummaryDialog;
import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ListFragment;
import android.text.format.DateUtils;
import android.view.ContextMenu;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.AdapterContextMenuInfo;


/**
 * @author Andreas Schildbach
 */
public final class WalletTransactionsFragment extends ListFragment {
	private WalletApplication application;
	private Activity activity;
	private ArrayAdapter<Transaction> adapter;
	private final Handler handler = new Handler();

	private final EventListeners.EventListener eventListener = new EventListeners.EventListener() {
		@Override
		public String getDescription() {
			return "Wallet Transactions Listener";
		}

		@Override
		public void onCoinsSent(final Transaction tx, final long result) {
			setAdapterContent();
		};

		@Override
		public void onCoinsReceived(final Transaction tx, final long result) {
			setAdapterContent();		
		};


		@Override
		public void onTransactionsChanged() {
			setAdapterContent();
		};


		@Override
		public void onWalletDidChange() {	
			setAdapterContent();
		}
	};

	@Override
	public void onAttach(final Activity activity) {
		super.onAttach(activity);

		this.activity = activity;
		application = (WalletApplication) activity.getApplication();
	}

	public void initAdapter() {
		adapter = new ArrayAdapter<Transaction>(activity, 0) {
			final DateFormat dateFormat = android.text.format.DateFormat
					.getDateFormat(activity);
			final DateFormat timeFormat = android.text.format.DateFormat
					.getTimeFormat(activity);
			final int colorSignificant = getResources().getColor(
					R.color.significant);
			final int colorInsignificant = getResources().getColor(
					R.color.insignificant);
			final int colorSent = getResources().getColor(
					R.color.color_sent);
			final int colorReceived = getResources().getColor(
					R.color.color_received);

			@Override
			public View getView(final int position, View row,
					final ViewGroup parent) {
				if (row == null)
					row = getLayoutInflater(null).inflate(R.layout.transaction_row, null);

				final CurrencyAmountView rowValue = (CurrencyAmountView) row
						.findViewById(R.id.transaction_row_value);

				final TextView rowLabel = (TextView) row
						.findViewById(R.id.transaction_row_address);

				final TextView rowTime = (TextView) row
						.findViewById(R.id.transaction_row_time);

								
				try {
					synchronized(adapter) {
						final Transaction tx = getItem(position);

						if (tx.getHash() == null) {
							rowLabel.setTextColor(Color.BLACK);
							rowLabel.setText(R.string.no_transactions);
							rowLabel.setTypeface(Typeface.DEFAULT);
							rowLabel.setTextColor(Color.BLACK);

							rowTime.setVisibility(View.INVISIBLE);
							rowValue.setVisibility(View.INVISIBLE);
							rowLabel.setGravity(Gravity.CENTER);

							return row;
						} else {

							rowTime.setVisibility(View.VISIBLE);
							rowValue.setVisibility(View.VISIBLE);
							rowLabel.setGravity(Gravity.LEFT);

							final TransactionConfidence confidence = tx.getConfidence();
							final ConfidenceType confidenceType = confidence
									.getConfidenceType();

							try {
								BigInteger value = null;

								if (tx instanceof MyTransaction) {
									value = ((MyTransaction)tx).getResult();
								} else {
									value = tx.getValue(application.bitcoinjWallet);
								}

								final boolean sent = value.signum() < 0;

								final int textColor;
								if (confidenceType == ConfidenceType.NOT_SEEN_IN_CHAIN) {
									textColor = colorInsignificant;
								} else if (confidenceType == ConfidenceType.BUILDING) {
									textColor = colorSignificant;
								} else if (confidenceType == ConfidenceType.NOT_IN_BEST_CHAIN) {
									textColor = colorSignificant;
								} else if (confidenceType == ConfidenceType.DEAD) {
									textColor = Color.RED;
								} else {
									textColor = colorInsignificant;
								}

								final String address;
								if (sent)
									if (tx.getOutputs().size() == 0)
										address = "Unknown";
									else
										address = tx.getOutputs().get(0)
										.getScriptPubKey().getToAddress()
										.toString();
								else if (tx.getInputs().size() == 0)
									address = "Generation";
								else
									address = tx.getInputs().get(0).getFromAddress()
									.toString();

								String label = null;
								if (tx instanceof MyTransaction
										&& ((MyTransaction) tx).getTag() != null)
									label = ((MyTransaction) tx).getTag();
								else
									label = AddressBookProvider.resolveLabel(
											activity.getContentResolver(), address);

								final Date time = tx.getUpdateTime();
								rowTime.setText(time != null ? (DateUtils.isToday(time
										.getTime()) ? timeFormat.format(time)
												: dateFormat.format(time)) : null);
								rowTime.setTextColor(textColor);

								rowLabel.setTextColor(textColor);
								rowLabel.setText(label != null ? label : address);
								rowLabel.setTypeface(label != null ? Typeface.DEFAULT
										: Typeface.MONOSPACE);

								rowValue.setCurrencyCode(null);
								rowValue.setAmountSigned(true);
								rowValue.setTextColor(textColor);
								rowValue.setAmount(value);

								if (sent) {
									rowValue.setTextColor(colorSent);
								} else {
									rowValue.setTextColor(colorReceived);
								}

								return row;
							} catch (final ScriptException x) {
								throw new RuntimeException(x);
							}
						}
					}
				} catch (Exception e) {
					e.printStackTrace();

					rowLabel.setTextColor(Color.BLACK);
					rowLabel.setText(R.string.unknown_error);
					rowLabel.setTypeface(Typeface.DEFAULT);					
					rowLabel.setTextColor(Color.BLACK);

					rowLabel.setGravity(Gravity.CENTER);
					rowTime.setVisibility(View.INVISIBLE);
					rowValue.setVisibility(View.INVISIBLE);

					return row;
				}
			}
		};

		setAdapterContent();

		setListAdapter(adapter);
	}

	public  void setAdapterContent() {
		try {
			synchronized(adapter) {

				if (application.getRemoteWallet() == null) {
					return;
				}

				adapter.clear();  

				List<Transaction> transactions = null;

				if (application.isInP2PFallbackMode())
					transactions = application.bitcoinjWallet.getTransactionsByTime();
				else
					transactions = (List)application.getRemoteWallet().getMyTransactions();

				if (transactions == null) {
					adapter.clear();
					return;
				}

				synchronized(transactions) {
					if (transactions.size() > 0) {

						int ii = 0;
						for (Transaction transaction : transactions) {
							if (ii >= 50)
								break;

							adapter.add(transaction);
							++ii;
						}  
					} else {
						adapter.add(new MyTransaction(NetworkParameters.prodNet(), 0, null));
					}
				}
			}
		} catch (Exception e) { 
			e.printStackTrace();
		}
	}

	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		EventListeners.addEventListener(eventListener);

		initAdapter();
	}
	
	@Override
	public void onDestroy() {		
		super.onDestroy();
		EventListeners.removeEventListener(eventListener);
	}

	@Override
	public void onListItemClick(final ListView l, final View v,
			final int position, final long id) {
		synchronized(adapter) {
			final Transaction tx = adapter.getItem(position);

			if (tx == null) {
				return;
			}

			if (tx.getHash() == null) {
				return;
			}

			TransactionSummaryDialog.show(getFragmentManager(), application, tx);
		}
	}

	// workaround http://code.google.com/p/android/issues/detail?id=20065
	private static View lastContextMenuView;

	@Override
	public void onCreateContextMenu(final ContextMenu menu, final View v,
			final ContextMenuInfo menuInfo) {
		activity.getMenuInflater().inflate(
				R.menu.wallet_transactions_context, menu);

		lastContextMenuView = v;
	}

	@Override
	public boolean onContextItemSelected(final MenuItem item) {
		final AdapterContextMenuInfo menuInfo = (AdapterContextMenuInfo) item
				.getMenuInfo();
		final ListAdapter adapter = ((ListView) lastContextMenuView)
				.getAdapter();
		final MyTransaction tx = (MyTransaction) adapter.getItem(menuInfo.position);

		switch (item.getItemId()) {
		case R.id.wallet_transactions_context_edit_address:
			editAddress(tx);
			return true;
		default:
			return false;
		}
	}

	private void editAddress(final MyTransaction tx) {
		try {
			if (tx.getHash() == null) {
				return;
			}

			final boolean sent = tx.getResult().signum() < 0;

			Address address = null;
			if (sent) {
				if (tx.getOutputs().size() == 0)
					return;

				address = tx.getOutputs().get(0).getScriptPubKey()
						.getToAddress();
			} else {
				if (tx.getInputs().size() == 0)
					return;

				address = tx.getInputs().get(0).getFromAddress();
			}

			EditAddressBookEntryFragment.edit(getFragmentManager(),
					address.toString());
		} catch (final ScriptException x) {
			// ignore click
			x.printStackTrace();
		}
	}
}
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
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.bitcoin.core.Address;
import com.google.bitcoin.core.AddressFormatException;
import com.google.bitcoin.core.ECKey;
import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.core.TransactionOutput;
import com.google.bitcoin.core.Wallet.BalanceType;
import com.google.bitcoin.core.Wallet.SendRequest;

import piuk.BitcoinAddress;
import piuk.BitcoinURI;
import piuk.MyRemoteWallet;
import piuk.MyRemoteWallet.SendProgress;
import piuk.blockchain.android.R;
import piuk.blockchain.android.Constants;
import piuk.blockchain.android.WalletApplication;
import piuk.blockchain.android.service.BlockchainService;
import piuk.blockchain.android.service.BlockchainServiceImpl;
import piuk.blockchain.android.ui.AbstractWalletActivity.QrCodeDelagate;
import piuk.blockchain.android.ui.CurrencyAmountView.Listener;
import piuk.blockchain.android.ui.SendCoinsActivity.OnChangedSendTypeListener;
import piuk.blockchain.android.ui.dialogs.RequestPasswordDialog;
import piuk.blockchain.android.util.WalletUtils;
import piuk.EventListeners;

/**
 * @author Andreas Schildbach
 */
public final class SendCoinsFragment extends Fragment
{
	private WalletApplication application;
	private final Handler handler = new Handler();
	private Runnable sentRunnable;

	private AutoCompleteTextView receivingAddressView;
	private View receivingAddressErrorView;
	private View feeContainerView;
	private View sendCoinsFromContainer;
	private Spinner sendCoinsFromSpinner;
	private CurrencyAmountView availableView;
	private View availableViewContainer;
	private CurrencyAmountView feeAmountView;
	private View sendTypeDescriptionContainer;
	private TextView sendTypeDescription;
	private ImageView sendTypeDescriptionIcon;
	private BlockchainServiceImpl service;

	private CurrencyAmountView amountView;
	private Button viewGo;
	private Button viewCancel;
	private String sendType;

	public static enum FeePolicy {
		FeeOnlyIfNeeded,
		FeeForce,
		FeeNever
	}

	private State state = State.INPUT;

	private enum State
	{
		INPUT, SENDING, SENT
	}

	private final ServiceConnection serviceConnection = new ServiceConnection()
	{
		public void onServiceConnected(final ComponentName name, final IBinder binder)
		{
			service = (BlockchainServiceImpl) ((BlockchainServiceImpl.LocalBinder) binder).getService();
		}

		public void onServiceDisconnected(final ComponentName name)
		{
			service = null;
		}
	};

	private final TextWatcher textWatcher = new TextWatcher()
	{
		public void afterTextChanged(final Editable s) {
			updateView();
		}

		public void beforeTextChanged(final CharSequence s, final int start, final int count, final int after)
		{
		}

		public void onTextChanged(final CharSequence s, final int start, final int before, final int count)
		{
		}
	};

	private final Listener listener = new Listener()
	{
		public void changed()
		{
			updateView();
		}

		public void done()
		{
			viewGo.requestFocusFromTouch();
		}
	};

	@Override
	public void onCreate(final Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		final Activity activity = getActivity();

		application = (WalletApplication) activity.getApplication();

		activity.bindService(new Intent(activity, BlockchainServiceImpl.class), serviceConnection, Context.BIND_AUTO_CREATE);
	}

	public abstract class RightDrawableOnTouchListener implements OnTouchListener {
		Drawable drawable;
		private int fuzz = 40;

		/**
		 * @param keyword
		 */
		public RightDrawableOnTouchListener(TextView view) {
			super();
			final Drawable[] drawables = view.getCompoundDrawables();
			if (drawables != null && drawables.length == 4)
				this.drawable = drawables[2];
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see android.view.View.OnTouchListener#onTouch(android.view.View, android.view.MotionEvent)
		 */
		@Override
		public boolean onTouch(final View v, final MotionEvent event) {

			if (event.getAction() == MotionEvent.ACTION_DOWN && drawable != null) {
				System.out.println("event " + event);

				final int x = (int) event.getX();
				final int y = (int) event.getY();

				final Rect bounds = drawable.getBounds();
				System.out.println("bounds " + bounds);

				if (x >= (v.getRight() - bounds.width() - fuzz) && x <= (v.getRight() - v.getPaddingRight() + fuzz)
						&& y >= (v.getPaddingTop() - fuzz) && y <= (v.getHeight() - v.getPaddingBottom()) + fuzz) {
					return onDrawableTouch(event);
				}
			}
			return false;
		}

		public abstract boolean onDrawableTouch(final MotionEvent event);

	}


	@Override
	public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState)
	{
		final SendCoinsActivity activity = (SendCoinsActivity) getActivity();

		final View view = inflater.inflate(R.layout.send_coins_fragment, container);

		final MyRemoteWallet wallet = application.getRemoteWallet();

		if (wallet == null)
			return view;

		BigInteger available = null;

		if (application.isInP2PFallbackMode()) {
			try {
				available = application.bitcoinjWallet.getBalance(BalanceType.ESTIMATED);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			available = wallet.getBalance();
		}

		receivingAddressView = (AutoCompleteTextView) view.findViewById(R.id.send_coins_receiving_address);
		feeContainerView = view.findViewById(R.id.send_coins_fee_container);
		sendCoinsFromContainer = view.findViewById(R.id.send_coins_from_container);
		sendCoinsFromSpinner = (Spinner)view.findViewById(R.id.send_coins_from_spinner);
		feeAmountView = (CurrencyAmountView)view.findViewById(R.id.send_coins_fee);
		sendTypeDescriptionContainer = view.findViewById(R.id.send_type_description_container);
		sendTypeDescription = (TextView)view.findViewById(R.id.send_type_description);
		sendTypeDescriptionIcon	= (ImageView)view.findViewById(R.id.send_type_description_icon);

		{
			//Construct the from drop down list
			String[] activeAddresses = wallet.getActiveAddresses();
			Map<String, String> labelMap = wallet.getLabelMap();

			List<Pair<String, String>> pairs = new ArrayList<Pair<String, String>>();
			for (String address : activeAddresses) {

				String label = labelMap.get(address);

				if (label == null || label.length() == 0) {
					label = "No Label";
				}

				BigInteger balance = wallet.getBalance(address);

				if (balance.compareTo(BigInteger.ZERO) > 0) {
					label += " (" + WalletUtils.formatValue(balance) + " BTC)";

					pairs.add(new Pair<String, String>(address, label));
				}
			}

			pairs.add(0, new Pair<String, String>("Any Address", "Any Address"));

			sendCoinsFromSpinner.setAdapter(new SpinAdapter(activity, android.R.layout.simple_list_item_1, pairs));
		}

		feeAmountView.setAmount(wallet.getBaseFee());

		StringPairAdapter adapter = new StringPairAdapter(this.getLabelList());

		receivingAddressView.setAdapter(adapter);
		receivingAddressView.addTextChangedListener(textWatcher);

		receivingAddressView.setOnTouchListener(new RightDrawableOnTouchListener(receivingAddressView) {
			@Override
			public boolean onDrawableTouch(final MotionEvent event) {

				activity.showQRReader(activity.new QrCodeDelagate() {
					@Override
					public void didReadQRCode(String data) {
						activity.handleScanURI(data);
					}
				});

				return true;
			}
		});

		receivingAddressErrorView = view.findViewById(R.id.send_coins_receiving_address_error);

		availableViewContainer = view.findViewById(R.id.send_coins_available_container);
		availableView = (CurrencyAmountView) view.findViewById(R.id.send_coins_available);
		availableView.setAmount(available);

		amountView = (CurrencyAmountView) view.findViewById(R.id.send_coins_amount);
		amountView.setListener(listener);
		amountView.setContextButton(R.drawable.ic_input_calculator, new OnClickListener()
		{
			public void onClick(final View v)
			{
				final FragmentTransaction ft = getFragmentManager().beginTransaction();
				final Fragment prev = getFragmentManager().findFragmentByTag(AmountCalculatorFragment.FRAGMENT_TAG);
				if (prev != null)
					ft.remove(prev);
				ft.addToBackStack(null);
				final DialogFragment newFragment = new AmountCalculatorFragment(new AmountCalculatorFragment.Listener()
				{
					public void use(final BigInteger amount)
					{
						amountView.setAmount(amount);
					}
				});
				newFragment.show(ft, AmountCalculatorFragment.FRAGMENT_TAG);
			}
		});

		viewGo = (Button) view.findViewById(R.id.send_coins_go);
		viewGo.setOnClickListener(new OnClickListener()
		{
			final SendProgress progress = new SendProgress() {
				public void onSend(final Transaction tx, final String message) {
					handler.post(new Runnable() {
						public void run() {
							state = State.SENT;

							activity.longToast(message);

							Intent intent = activity.getIntent();
							intent.putExtra("tx", tx.getHash());
							activity.setResult(Activity.RESULT_OK, intent);

							activity.finish();

							updateView();
						}
					});

					try {
						Thread.sleep(2000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}

					application.doMultiAddr(true);
				}

				public void onError(final String message) {
					handler.post(new Runnable() {
						public void run() {

							System.out.println("On Error");

							if (message != null)
								activity.longToast(message);

							state = State.INPUT;

							updateView();
						}
					});
				}

				public void onProgress(final String message) {
					handler.post(new Runnable() {
						public void run() {
							state = State.SENDING;

							updateView();
						}
					});
				}

				public boolean onReady(Transaction tx, BigInteger fee, FeePolicy feePolicy, long priority) {

					boolean containsOutputLessThanThreshold = false;
					for (TransactionOutput output : tx.getOutputs()) {
						if (output.getValue().compareTo(Constants.FEE_THRESHOLD_MIN) < 0) {
							containsOutputLessThanThreshold = true;
							break;
						}
					}

					if (feePolicy != FeePolicy.FeeNever && fee.compareTo(BigInteger.ZERO) == 0) {
						if (tx.bitcoinSerialize().length > 1000 || containsOutputLessThanThreshold) {
							makeTransaction(FeePolicy.FeeForce);
							return false;
						} else if (priority < 97600000L) {
							handler.post(new Runnable() {
								public void run() {
									AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
									builder.setMessage(R.string.ask_for_fee)
									.setCancelable(false);

									AlertDialog alert = builder.create();

									alert.setButton(AlertDialog.BUTTON_NEUTRAL, getString(R.string.continue_without_fee), new DialogInterface.OnClickListener() {
										public void onClick(DialogInterface dialog, int id) {
											makeTransaction(FeePolicy.FeeNever);
											dialog.dismiss();
										} }); 

									alert.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.add_fee), new DialogInterface.OnClickListener() {
										public void onClick(DialogInterface dialog, int id) {
											makeTransaction(FeePolicy.FeeForce);

											dialog.dismiss();
										}}); 

									alert.setButton(AlertDialog.BUTTON_NEGATIVE, getString(R.string.cancel), new DialogInterface.OnClickListener() {
										public void onClick(DialogInterface dialog, int id) {
											dialog.dismiss();
										}});

									alert.show();
								}
							});

							handler.post(new Runnable() {
								public void run() {
									state = State.INPUT;
									updateView();
								}
							});
							return false;
						}
					}

					return true;
				}


				public ECKey onPrivateKeyMissing(final String address) {

					if (SendCoinsActivity.temporaryPrivateKeys.containsKey(address)) {
						return SendCoinsActivity.temporaryPrivateKeys.get(address);
					}
					
					handler.post(new Runnable() {
						public void run() {
							AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
							builder.setMessage(getString(R.string.ask_for_private_key, address))
							.setCancelable(false)
							.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog, int id) {
									activity.scanPrivateKeyAddress = address;


									activity.showQRReader(activity.new QrCodeDelagate() {
										@Override
										public void didReadQRCode(String data) throws Exception {
											activity.handleScanPrivateKey(data);
										}
									});
								}
							})
							.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog, int id) {

									synchronized (SendCoinsActivity.temporaryPrivateKeys) {
										SendCoinsActivity.temporaryPrivateKeys.notify();
									}

									dialog.cancel();
								}
							});

							AlertDialog alert = builder.create();

							alert.show();
						}
					});

					try {
						synchronized (SendCoinsActivity.temporaryPrivateKeys) {
							SendCoinsActivity.temporaryPrivateKeys.wait();
						}
					} catch (InterruptedException e) {
						e.printStackTrace();
					}

					return SendCoinsActivity.temporaryPrivateKeys.get(address);
				}
			};

			public void send(Address receivingAddress, BigInteger fee, FeePolicy feePolicy) {

				if (application.getRemoteWallet() == null)
					return;

				if (sendType != null && !sendType.equals(SendCoinsActivity.SendTypeQuickSend) && application.isInP2PFallbackMode()) {
					activity.longToast(R.string.only_quick_supported);
					return;
				}

				String[] from;
				if (sendType != null && sendType.equals(SendCoinsActivity.SendTypeCustomSend)) {
					Pair<String, String> selected = (Pair<String, String>) sendCoinsFromSpinner.getSelectedItem();

					if (selected.first.equals("Any Address")) {
						from = wallet.getActiveAddresses();
					} else {
						from = new String[] {selected.first.toString()};
					}
				} else { 
					from = wallet.getActiveAddresses();
				}

				final BigInteger amount;

				if (sendType != null && sendType.equals(SendCoinsActivity.SendTypeSharedSend)) {
					BigDecimal amountDecimal = BigDecimal.valueOf(amountView.getAmount().doubleValue());

					//Add the fee
					amount = amountDecimal.add(amountDecimal.divide(BigDecimal.valueOf(100)).multiply(BigDecimal.valueOf(wallet.getSharedFee()))).toBigInteger();
				} else {
					amount = amountView.getAmount();
				} 

				final WalletApplication application = (WalletApplication) getActivity().getApplication();

				if (application.isInP2PFallbackMode()) {

					final long blockchainLag = System.currentTimeMillis() - service.blockChain.getChainHead().getHeader().getTime().getTime();

					final boolean blockchainUptodate = blockchainLag < Constants.BLOCKCHAIN_UPTODATE_THRESHOLD_MS;

					if (!blockchainUptodate) {
						activity.longToast(R.string.blockchain_not_upto_date);
						return;
					}

					// create spend
					final SendRequest sendRequest = SendRequest.to(receivingAddress, amountView.getAmount());

					sendRequest.fee = fee;

					new Thread(new Runnable()
					{
						public void run()
						{
							final Transaction transaction = application.bitcoinjWallet.sendCoinsOffline(sendRequest);


							handler.post(new Runnable()
							{
								public void run()
								{
									if (transaction != null)
									{
										state = State.SENDING;

										updateView();

										service.broadcastTransaction(transaction);

										state = State.SENT;

										activity.longToast(R.string.wallet_transactions_fragment_tab_sent);

										Intent intent = activity.getIntent();
										intent.putExtra("tx", transaction.getHash());
										activity.setResult(Activity.RESULT_OK, intent);

										activity.finish();

										updateView();

										EventListeners.invokeOnTransactionsChanged();
									}
									else
									{
										state = State.INPUT;

										updateView();

										activity.longToast(R.string.send_coins_error_msg);
									}
								}
							});
						}
					}).start();
				} else {
					application.getRemoteWallet().sendCoinsAsync(from, receivingAddress.toString(), amount, feePolicy, fee, progress);
				}
			}

			public void makeTransaction(FeePolicy feePolicy) {

				if (application.getRemoteWallet() == null)
					return;

				try {
					MyRemoteWallet wallet = application.getRemoteWallet();

					BigInteger baseFee = wallet.getBaseFee();

					BigInteger fee = null;

					if (feePolicy == FeePolicy.FeeNever) {
						fee = BigInteger.ZERO;
					} else if (feePolicy == FeePolicy.FeeForce) {
						fee = baseFee;
					} else if (sendType != null && sendType.equals(SendCoinsActivity.SendTypeCustomSend)) {
						feePolicy = FeePolicy.FeeOnlyIfNeeded;
						fee = feeAmountView.getAmount();
					} else {
						fee = (wallet.getFeePolicy() == 1) ? baseFee : BigInteger.ZERO;
					}

					final BigInteger finalFee = fee;
					final FeePolicy finalFeePolicy = feePolicy;

					if (sendType != null && sendType.equals(SendCoinsActivity.SendTypeSharedSend)) {

						new Thread(new Runnable() {
							@Override
							public void run() {
								try {
									final String addressString = MyRemoteWallet.generateSharedAddress(getToAddress());

									handler.post(new Runnable() {
										@Override
										public void run() {
											try {
												send(new Address(Constants.NETWORK_PARAMETERS, addressString), finalFee, finalFeePolicy);
											} catch (Exception e) {
												e.printStackTrace();

												Toast.makeText(application, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
											}
										}
									});
								} catch (final Exception e) {
									handler.post(new Runnable() {

										@Override
										public void run() {
											e.printStackTrace();

											Toast.makeText(application, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
										}
									});
								}
							}

						}).start();

					} else {
						String addressString = getToAddress();

						Address receivingAddress = new Address(Constants.NETWORK_PARAMETERS, addressString);

						send(receivingAddress, fee, feePolicy);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			public void onClick(final View v)
			{
				if (application.getRemoteWallet() == null)
					return;

				MyRemoteWallet remoteWallet = application.getRemoteWallet();

				if (remoteWallet.isDoubleEncrypted() && remoteWallet.temporySecondPassword == null) {
					RequestPasswordDialog.show(getFragmentManager(), new SuccessCallback() {

						public void onSuccess() {
							makeTransaction(FeePolicy.FeeOnlyIfNeeded);
						}

						public void onFail() {
							Toast.makeText(application, R.string.send_no_password_error, Toast.LENGTH_LONG).show();
						}
					}, RequestPasswordDialog.PasswordTypeSecond);
				} else {
					makeTransaction(FeePolicy.FeeOnlyIfNeeded);
				}
			}
		});

		viewCancel = (Button) view.findViewById(R.id.send_coins_cancel);
		viewCancel.setOnClickListener(new OnClickListener()
		{
			public void onClick(final View v)
			{
				activity.setResult(Activity.RESULT_CANCELED);

				activity.finish();
			}
		});

		activity.setOnChangedSendTypeListener(new OnChangedSendTypeListener() {
			@Override
			public void onChangedSendType(String type) {
				sendType = type;

				feeContainerView.setVisibility(View.GONE);
				sendCoinsFromContainer.setVisibility(View.GONE);
				availableViewContainer.setVisibility(View.VISIBLE);
				sendTypeDescriptionContainer.setVisibility(View.GONE);

				if (type.equals(SendCoinsActivity.SendTypeCustomSend)) {
					feeContainerView.setVisibility(View.VISIBLE);
					sendCoinsFromContainer.setVisibility(View.VISIBLE);
					availableViewContainer.setVisibility(View.GONE);
				} else if (type.equals(SendCoinsActivity.SendTypeSharedSend)) {					
					sendTypeDescriptionContainer.setVisibility(View.VISIBLE);
					sendTypeDescription.setText(getString(R.string.shared_send_description, wallet.getSharedFee()+"%"));
					sendTypeDescriptionIcon.setImageResource(R.drawable.ic_icon_shared);
				}
			}
		});

		updateView();

		return view;
	}

	protected void onServiceBound()
	{
		System.out.println("service bound");
	}

	protected void onServiceUnbound()
	{
		System.out.println("service unbound");
	}

	@Override
	public void onResume() {

		super.onResume();
	}

	@Override
	public void onDestroyView()
	{
		super.onDestroyView();

		((SendCoinsActivity)getActivity()).setOnChangedSendTypeListener(null);

		handler.removeCallbacks(sentRunnable);

		getActivity().unbindService(serviceConnection);
	}


	@Override
	public void onDestroy()
	{
		super.onDestroy();

		if (application.getRemoteWallet() == null)
			return;

		//Clear the second password
		MyRemoteWallet remoteWallet = application.getRemoteWallet();

		remoteWallet.setTemporySecondPassword(null);
	}

	public class SpinAdapter extends ArrayAdapter<Pair<String, String>>{

		private Context context;

		public SpinAdapter(Context context, int textViewResourceId,
				List<Pair<String, String>> values) {
			super(context, textViewResourceId, values);
			this.context = context;
		}


		// And the "magic" goes here
		// This is for the "passive" state of the spinner
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			// I created a dynamic TextView here, but you can reference your own  custom layout for each spinner item
			TextView label = new TextView(context);
			label.setTextColor(Color.BLACK);

			final Pair<String, String> pair = getItem(position);

			// Then you can get the current item using the values array (Users array) and the current position
			// You can NOW reference each method you has created in your bean object (User class)
			label.setText(pair.first);

			// And finally return your dynamic (or custom) view for each spinner item
			return label;
		}

		// And here is when the "chooser" is popped up
		// Normally is the same view, but you can customize it if you want
		@Override
		public View getDropDownView(int position, View convertView,
				ViewGroup parent) {

			View row = getLayoutInflater(null).inflate(R.layout.simple_dropdown_item_2line, parent, false);

			final Pair<String, String> pair = getItem(position);

			((TextView) row.findViewById(android.R.id.text1)).setText(pair.second);

			((TextView) row.findViewById(android.R.id.text2)).setText(pair.first);

			return row;
		}
	}

	public class StringPairAdapter extends ArrayAdapter<Pair<String, String>>
	{
		public StringPairAdapter(List<Pair<String, String>> data)
		{
			super(getActivity(), R.layout.simple_dropdown_item_2line, data);
		}

		@Override
		public View getView(final int position, View row, final ViewGroup parent) {

			if (row == null) {
				row = getLayoutInflater(null).inflate(R.layout.simple_dropdown_item_2line, parent, false);
			}

			final Pair<String, String> pair = getItem(position);

			((TextView) row.findViewById(android.R.id.text1)).setText(pair.first);

			((TextView) row.findViewById(android.R.id.text2)).setText(pair.second);

			return row;
		}
	}

	public List<Pair<String, String>> getLabelList() {
		List<Pair<String, String>> array = new ArrayList<Pair<String, String>>();

		if (application.getRemoteWallet() == null) {
			return array;
		}

		Map<String, String> labelMap = application.getRemoteWallet().getLabelMap();

		synchronized(labelMap) {
			for (Map.Entry<String, String> entry : labelMap.entrySet()) {
				array.add(new Pair<String, String>(entry.getValue(), entry.getKey()) {
					public String toString() {
						return first.toString();
					}
				});
			}
		}

		return array;
	}

	public String getToAddress() {
		final String userEntered = receivingAddressView.getText().toString().trim();
		if (userEntered.length() > 0) {
			try {
				new Address(Constants.NETWORK_PARAMETERS, userEntered);

				return userEntered;
			} catch (AddressFormatException e) {
				List<Pair<String, String>> labels = this.getLabelList();

				for (Pair<String, String> label : labels) {
					if (label.first.toLowerCase(Locale.ENGLISH).equals(userEntered.toLowerCase(Locale.ENGLISH))) {
						try {
							new Address(Constants.NETWORK_PARAMETERS, label.second);

							return label.second;
						} catch (AddressFormatException e1) {}
					}
				}
			}
		}

		return null;
	}

	private void updateView()
	{

		String address = getToAddress();

		if (receivingAddressView.getText().toString().trim().length() == 0 || address != null) {
			receivingAddressErrorView.setVisibility(View.GONE);	
		} else {
			receivingAddressErrorView.setVisibility(View.VISIBLE);
		}

		final BigInteger amount = amountView.getAmount();
		final boolean validAmount = amount != null && amount.signum() > 0;

		receivingAddressView.setEnabled(state == State.INPUT);

		amountView.setEnabled(state == State.INPUT);

		viewGo.setEnabled(state == State.INPUT && address != null && validAmount);
		if (state == State.INPUT)
			viewGo.setText(R.string.send_coins_fragment_button_send);
		else if (state == State.SENDING)
			viewGo.setText(R.string.send_coins_sending_msg);
		else if (state == State.SENT)
			viewGo.setText(R.string.send_coins_sent_msg);

		viewCancel.setEnabled(state != State.SENDING);
		viewCancel.setText(state != State.SENT ? R.string.button_cancel : R.string.send_coins_fragment_button_back);
	}

	public void update(final String receivingAddress, final BigInteger amount)
	{
		if (receivingAddressView == null)
			return;

		receivingAddressView.setText(receivingAddress);

		flashReceivingAddress();

		if (amount != null)
			amountView.setAmount(amount);

		if (receivingAddress != null && amount == null)
			amountView.requestFocus();

		updateView();
	}

	private Runnable resetColorRunnable = new Runnable()
	{
		public void run()
		{
			receivingAddressView.setTextColor(Color.parseColor("#888888"));
		}
	};

	public void flashReceivingAddress()
	{
		receivingAddressView.setTextColor(Color.parseColor("#cc5500"));
		handler.removeCallbacks(resetColorRunnable);
		handler.postDelayed(resetColorRunnable, 500);
	}
}

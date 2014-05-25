package info.blockchain.wallet.ui;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import piuk.EventListeners;
import piuk.MyRemoteWallet;
import piuk.MyRemoteWallet.SendProgress;
import piuk.blockchain.android.Constants;
import piuk.blockchain.android.R;
import piuk.blockchain.android.WalletApplication;
import piuk.blockchain.android.service.BlockchainServiceImpl;
import piuk.blockchain.android.ui.SendCoinsActivity;
import piuk.blockchain.android.ui.SuccessCallback;
import piuk.blockchain.android.ui.dialogs.RequestPasswordDialog;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.provider.ContactsContract;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.InputType;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextWatcher;
import android.text.style.RelativeSizeSpan;
import android.text.style.SuperscriptSpan;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnTouchListener;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ImageView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ImageButton;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView.OnEditorActionListener;
import android.provider.ContactsContract.CommonDataKinds;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.content.BroadcastReceiver;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.util.Log;
import net.sourceforge.zbar.Symbol;

import com.dm.zbar.android.scanner.ZBarConstants;
import com.dm.zbar.android.scanner.ZBarScannerActivity;
import com.google.bitcoin.core.Address;
import com.google.bitcoin.core.ECKey;
import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.core.TransactionOutput;
import com.google.bitcoin.core.Utils;
import com.google.bitcoin.core.Wallet.SendRequest;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;
//import com.google.bitcoin.uri.BitcoinURI;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.android.Contents;
import com.google.zxing.client.android.encode.QRCodeEncoder;

public class SendFragment extends Fragment   {
	private static final String SendTypeQuickSend = "Quick Send";
	private static final String SendTypeCustomSend = "Custom Send";
	private static final String SendTypeSharedCoin = "Shared Coin";

	private static int SIMPLE_SEND = 1;
	private static int CUSTOM_SEND = 2;
	private static int SHARED_SEND = 3;

	private static int PICK_CONTACT = 10;
	private static int SELECT_INTL_PREFIX = 11;
	private static int ZBAR_SCANNER_REQUEST = 2026;

	private static int CURRENT_SEND = SIMPLE_SEND;
	
	private LinearLayout lastSendingAddress = null;
	
	private boolean addressesOn = false;
	private boolean contactsOn = true;
	private boolean phoneContactsOn = false;
	
	private View rootView = null;

    private EditText edAmount1 = null;
    private TextView tvAmount2 = null;
    private EditText edAddress = null;
    private TextView tvCurrency = null;
    private LinearLayout summary = null;
    private LinearLayout summary2 = null;
    
    private TextView tvAmount = null;
    private TextView tvAmountBis = null;
    private TextView tvArrow = null;
    private TextView tvAddress = null;
    private TextView tvAddressBis = null;
	private String strCurrentFiatSymbol = "$";
	private String strCurrentFiatCode = "USD";
    
    private LinearLayout simple_spend = null;
    private LinearLayout custom_spend = null;

	private boolean isMagic = false;
    private View oldView = null;
    private LinearLayout parent = null;
    private LinearLayout magic = null;
    private int children = 0;
    private View childIcons = null;
    private View childList = null;
    private ListView magicList = null;

    private ImageView ivAddresses = null;
    private ImageView ivContacts = null;
    private ImageView ivPhoneContacts = null;

    private Button btSend = null;
    private ImageView ivCheck = null;
    private TextView tvSentPrompt = null;
    
    private List<HashMap<String,String>> magicData = null;
    private List<HashMap<String,String>> filteredDisplayList = null;
	private MagicAdapter adapter = null;
	private String currentSelectedAddress = null;

	private boolean isBTC = false;

	private WalletApplication application;
	private final Handler handler = new Handler();
	private Runnable sentRunnable;
	private String sendType;
	private BlockchainServiceImpl service;
	
	private List<String> activeAddresses;
	private Map<String,String> labels;
	private List<Map<String, Object>> addressBookMapList;
	private String emailOrNumber;
	boolean sendViaEmail;
	boolean sentViaSMS;
	private CustomSend cs;
	
	public static final String ACTION_INTENT = "info.blockchain.wallet.ui.SendFragment.BTC_ADDRESS_SCAN";

    protected BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(ACTION_INTENT.equals(intent.getAction())) {

                String address = intent.getStringExtra("BTC_ADDRESS");
                
                /*
                if(BitcoinAddressCheck.isValidAddress(address)) {
            		Toast.makeText(getActivity(), "is valid address", Toast.LENGTH_SHORT).show();

            		if(isMagic) {
            			removeMagicList();
            		}

                    InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(edAddress.getWindowToken(), 0);

                    edAddress.setText(address);

                    edAmount1.requestFocus();
                    edAmount1.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
                    imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
                }
                else if(BitcoinAddressCheck.isUri(address)) {
            		Toast.makeText(getActivity(), "is uri", Toast.LENGTH_SHORT).show();

            		if(isMagic) {
            			removeMagicList();
            		}

                    String btc_address = BitcoinAddressCheck.getAddress(address);
                    String btc_amount = BitcoinAddressCheck.getAmount(address);
                    
                    edAddress.setText(btc_address);
                    InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(edAddress.getWindowToken(), 0);

                    edAmount1.requestFocus();
                    edAmount1.setText(Double.toString(Double.parseDouble(btc_amount) / 100000000.0));
                    edAmount1.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
                    imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);

                    isBTC = true;
            	    tvCurrency.setTypeface(TypefaceUtil.getInstance(getActivity()).getBTCTypeface());
            		tvCurrency.setText(Character.toString((char)TypefaceUtil.getInstance(getActivity()).getBTCSymbol()));
                    
                }
                else {
            		Toast.makeText(getActivity(), "not processed", Toast.LENGTH_SHORT).show();
                }
                */

    			doScanInput(address);

            }
        }
    };

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

    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    	
        IntentFilter filter = new IntentFilter(ACTION_INTENT);
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(receiver, filter);

		final MainActivity activity = (MainActivity) getActivity();
		application = (WalletApplication) activity.getApplication();
		activity.bindService(new Intent(activity, BlockchainServiceImpl.class), serviceConnection, Context.BIND_AUTO_CREATE);
    	sendType = SendTypeQuickSend;
    	sendViaEmail = false;
    	sentViaSMS = false;
    	
        rootView = inflater.inflate(R.layout.fragment_send, container, false);
        
    	simple_spend = (LinearLayout)rootView.findViewById(R.id.send_container);
    	custom_spend = (LinearLayout)rootView.findViewById(R.id.custom_spend);
    	custom_spend.setVisibility(View.GONE);
    	
    	CURRENT_SEND = SIMPLE_SEND;

        tvAmount = (TextView)rootView.findViewById(R.id.amount);
        tvAmount.setVisibility(View.INVISIBLE);
        tvAmountBis = (TextView)rootView.findViewById(R.id.amount_bis);
        tvAmountBis.setVisibility(View.INVISIBLE);
        tvArrow = (TextView)rootView.findViewById(R.id.arrow);
        tvArrow.setVisibility(View.INVISIBLE);
        tvAddress = (TextView)rootView.findViewById(R.id.sending_address);
        tvAddress.setVisibility(View.INVISIBLE);
        tvAddressBis = (TextView)rootView.findViewById(R.id.sending_address_bis);
        tvAddressBis.setVisibility(View.INVISIBLE);

        summary = (LinearLayout)rootView.findViewById(R.id.summary);
        summary.setVisibility(View.INVISIBLE);
        summary2 = (LinearLayout)rootView.findViewById(R.id.summary2);
        summary2.setVisibility(View.INVISIBLE);

        btSend = (Button)rootView.findViewById(R.id.send);
        ivCheck = ((ImageButton)rootView.findViewById(R.id.sent_check));
        ivCheck.setVisibility(View.GONE);
        tvSentPrompt = (TextView)rootView.findViewById(R.id.sent_prompt);
        tvSentPrompt.setVisibility(View.GONE);
        
        tvCurrency = (TextView)rootView.findViewById(R.id.currency);
        tvCurrency.setTypeface(TypefaceUtil.getInstance(getActivity()).getBTCTypeface());
        tvCurrency.setText(Character.toString((char)TypefaceUtil.getInstance(getActivity()).getBTCSymbol()));
        tvCurrency.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            	if(isBTC) {
            		tvCurrency.setText(strCurrentFiatSymbol);
            		String tmp = edAmount1.getText().toString();
            		if(tmp.length() < 1) {
            			tmp = "0.0000";
            		}
            		String tmp2 = tvAmount2.getText().toString().substring(0, tvAmount2.getText().toString().length() - 4);
            		try {
            			double d = Double.parseDouble(tmp2);
            			if(0.0 == d) {
            				tmp2 = "";
            			}
            		}
            		catch(Exception e) {
            			tmp2 = "";
            		}
            		edAmount1.setText(tmp2);
                    tvAmount2.setText(tmp + " BTC");
            	}
            	else {
            	    tvCurrency.setTypeface(TypefaceUtil.getInstance(getActivity()).getBTCTypeface());
            		tvCurrency.setText(Character.toString((char)TypefaceUtil.getInstance(getActivity()).getBTCSymbol()));
            		String tmp = edAmount1.getText().toString(); 
            		if(tmp.length() < 1) {
            			tmp = "0.00";
            		}
            		String tmp2 = tvAmount2.getText().toString().substring(0, tvAmount2.getText().toString().length() - 4);
            		try {
            			double d = Double.parseDouble(tmp2);
            			if(0.0 == d) {
            				tmp2 = "";
            			}
            		}
            		catch(Exception e) {
            			tmp2 = "";
            		}
                    edAmount1.setText(tmp2);
                    tvAmount2.setText(tmp + " " + strCurrentFiatCode);
            	}
            	isBTC = isBTC ? false : true;
            }
        });

        final ImageView clear_input = (ImageView)rootView.findViewById(R.id.clear);

    	LinearLayout divider1 = (LinearLayout)rootView.findViewById(R.id.divider1);
    	divider1.setBackgroundColor(BlockchainUtil.BLOCKCHAIN_RED);
    	LinearLayout divider2 = (LinearLayout)rootView.findViewById(R.id.divider2);
    	divider2.setBackgroundColor(BlockchainUtil.BLOCKCHAIN_RED);

//        ((TextView)rootView.findViewById(R.id.direction)).setTypeface(TypefaceUtil.getInstance(getActivity()).getGravityBoldTypeface());
//        ((TextView)rootView.findViewById(R.id.direction)).setText(Character.toString((char)0x2192));
        ((ImageView)rootView.findViewById(R.id.direction)).setImageResource(R.drawable.red_arrow);
        ((TextView)rootView.findViewById(R.id.currency)).setText(strCurrentFiatSymbol);
        ((TextView)rootView.findViewById(R.id.currency)).setTypeface(TypefaceUtil.getInstance(getActivity()).getGravityBoldTypeface());

//        initMagicList();
        initAddressBookList();

        btSend = ((Button)rootView.findViewById(R.id.send));
        btSend.setVisibility(View.INVISIBLE);
        btSend.setOnClickListener(new Button.OnClickListener() {

			final SendProgress progress = new SendProgress() {
				public void onSend(final Transaction tx, final String message) {
					handler.post(new Runnable() {
						public void run() {
							application.getRemoteWallet().setState(MyRemoteWallet.State.SENT);
							activity.longToast(message);

							Intent intent = activity.getIntent();
							intent.putExtra("tx", tx.getHash());
							activity.setResult(Activity.RESULT_OK, intent);

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

							application.getRemoteWallet().setState(MyRemoteWallet.State.INPUT);

							updateView();
						}
					});
				}

				public void onProgress(final String message) {
					handler.post(new Runnable() {
						public void run() {
							application.getRemoteWallet().setState(MyRemoteWallet.State.SENDING);

							updateView();
						}
					});
				}

				public boolean onReady(Transaction tx, BigInteger fee, MyRemoteWallet.FeePolicy feePolicy, long priority) {

					boolean containsOutputLessThanThreshold = false;
					for (TransactionOutput output : tx.getOutputs()) {
						if (output.getValue().compareTo(Constants.FEE_THRESHOLD_MIN) < 0) {
							containsOutputLessThanThreshold = true;
							break;
						}
					}

					if (feePolicy != MyRemoteWallet.FeePolicy.FeeNever && fee.compareTo(BigInteger.ZERO) == 0) {
						if (tx.bitcoinSerialize().length > 1000 || containsOutputLessThanThreshold) {
							makeTransaction(MyRemoteWallet.FeePolicy.FeeForce);
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
											makeTransaction(MyRemoteWallet.FeePolicy.FeeNever);
											dialog.dismiss();
										} }); 

									alert.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.add_fee), new DialogInterface.OnClickListener() {
										public void onClick(DialogInterface dialog, int id) {
											makeTransaction(MyRemoteWallet.FeePolicy.FeeForce);

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
									application.getRemoteWallet().setState(MyRemoteWallet.State.INPUT);
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
									//TODO:
									/*
									activity.scanPrivateKeyAddress = address;


									activity.showQRReader(activity.new QrCodeDelagate() {
										@Override
										public void didReadQRCode(String data) throws Exception {
											activity.handleScanPrivateKey(data);
										}
									});
									*/
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
        	
			final SendProgress progressEmailSMS = new SendProgress() {
				public void onSend(final Transaction tx, final String message) {
					handler.post(new Runnable() {
						public void run() {
							application.getRemoteWallet().setState(MyRemoteWallet.State.SENT);
							activity.longToast(message);

							Intent intent = activity.getIntent();
							intent.putExtra("tx", tx.getHash());
							activity.setResult(Activity.RESULT_OK, intent);

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

							application.getRemoteWallet().setState(MyRemoteWallet.State.INPUT);

							updateView();
						}
					});
				}

				public void onProgress(final String message) {
					handler.post(new Runnable() {
						public void run() {
							application.getRemoteWallet().setState(MyRemoteWallet.State.SENDING);

							updateView();
						}
					});
				}

				@Override
				public boolean onReady(Transaction tx, BigInteger fee,
						MyRemoteWallet.FeePolicy feePolicy, long priority) {
					// TODO Auto-generated method stub
					return false;
				}

				@Override
				public ECKey onPrivateKeyMissing(String address) {
					// TODO Auto-generated method stub
					return null;
				}
			};

			public void makeTransaction(MyRemoteWallet.FeePolicy feePolicy) {

				if (application.getRemoteWallet() == null)
					return;

				try {
					MyRemoteWallet wallet = application.getRemoteWallet();

					BigInteger baseFee = wallet.getBaseFee();

					BigInteger fee = null;

					if (feePolicy == MyRemoteWallet.FeePolicy.FeeNever) {
						fee = BigInteger.ZERO;
					} else if (feePolicy == MyRemoteWallet.FeePolicy.FeeForce) {
						fee = baseFee;
					} else if (sendType != null && sendType.equals(SendTypeCustomSend)) {
						feePolicy = MyRemoteWallet.FeePolicy.FeeOnlyIfNeeded;
						fee = cs.getFee();
					} else {
						fee = (wallet.getFeePolicy() == 1) ? baseFee : BigInteger.ZERO;
					}

					final BigInteger finalFee = fee;
					final MyRemoteWallet.FeePolicy finalFeePolicy = feePolicy;

					if (sendType != null && sendType.equals(SendTypeSharedCoin)) {

						new Thread(new Runnable() {
							@Override
							public void run() {
								try {
									final String addressString = MyRemoteWallet.generateSharedAddress(application.getRemoteWallet().getToAddress(edAddress.getText().toString()));

									handler.post(new Runnable() {
										@Override
										public void run() {
											try {
												sharedSend(new Address(Constants.NETWORK_PARAMETERS, addressString), finalFee, finalFeePolicy);
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
						String addressString = application.getRemoteWallet().getToAddress(edAddress.getText().toString());

						Address receivingAddress = new Address(Constants.NETWORK_PARAMETERS, addressString);

						if (sendType != null && sendType == SendTypeQuickSend) {
							quickSend(receivingAddress, fee, feePolicy);
						} else if (sendType != null && sendType == SendTypeCustomSend) {
							customSend(receivingAddress, fee, feePolicy);
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			public void quickSend(Address receivingAddress, BigInteger fee, MyRemoteWallet.FeePolicy feePolicy) {
				if (application.getRemoteWallet() == null)
					return;

				final BigInteger amount = getBTCEnteredOutputValue(edAmount1);
				final WalletApplication application = (WalletApplication) getActivity().getApplication();
				if (application.isInP2PFallbackMode()) {

					final long blockchainLag = System.currentTimeMillis() - service.blockChain.getChainHead().getHeader().getTime().getTime();

					final boolean blockchainUptodate = blockchainLag < Constants.BLOCKCHAIN_UPTODATE_THRESHOLD_MS;

					if (!blockchainUptodate) {
						activity.longToast(R.string.blockchain_not_upto_date);
						return;
					}

					// create spend
					final SendRequest sendRequest = SendRequest.to(receivingAddress, getBTCEnteredOutputValue(edAmount1));
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
										application.getRemoteWallet().setState(MyRemoteWallet.State.SENDING);

										updateView();

										service.broadcastTransaction(transaction);

										application.getRemoteWallet().setState(MyRemoteWallet.State.SENT);

										activity.longToast(R.string.wallet_transactions_fragment_tab_sent);

										Intent intent = activity.getIntent();
										intent.putExtra("tx", transaction.getHash());
										activity.setResult(Activity.RESULT_OK, intent);

										updateView();

										EventListeners.invokeOnTransactionsChanged();

									}
									else
									{
										application.getRemoteWallet().setState(MyRemoteWallet.State.INPUT);

										updateView();

										activity.longToast(R.string.send_coins_error_msg);
									}
								}
							});
						}
					}).start();
				} else {
					application.getRemoteWallet().simpleSendCoinsAsync(receivingAddress.toString(), amount, feePolicy, fee, progress);
					
					//
					//
					//
					btSend.setVisibility(View.GONE);
			        ivCheck.setVisibility(View.VISIBLE);
			        tvSentPrompt.setVisibility(View.VISIBLE);

				}
			}
			
			public void customSend(Address receivingAddress, BigInteger fee, MyRemoteWallet.FeePolicy feePolicy) {
				if (application.getRemoteWallet() == null)
					return;

				if (sendType != null && !sendType.equals(SendTypeQuickSend) && application.isInP2PFallbackMode()) {
					activity.longToast(R.string.only_quick_supported);
					return;
				}
				//TODO:
				/*
				Pair<String, String> selected = (Pair<String, String>) sendCoinsFromSpinner.getSelectedItem();

				if (selected.first.equals("Any Address")) {
					from = application.getRemoteWallet().getActiveAddresses();
				} else {
					from = new String[] {selected.first.toString()};
				}
				*/
				String[] from = application.getRemoteWallet().getActiveAddresses();
				final BigInteger amount = getBTCEnteredOutputValue(edAmount1);
				final WalletApplication application = (WalletApplication) getActivity().getApplication();

				application.getRemoteWallet().sendCoinsAsync(from, cs.getReceivingAddresses(), feePolicy, fee, cs.getChangeAddress(), progress);
				
				//
				//
				//
				btSend.setVisibility(View.GONE);
		        ivCheck.setVisibility(View.VISIBLE);
		        tvSentPrompt.setVisibility(View.VISIBLE);

			}
			
			public void sharedSend(Address receivingAddress, BigInteger fee, MyRemoteWallet.FeePolicy feePolicy) {
				if (application.getRemoteWallet() == null)
					return;

				if (sendType != null && !sendType.equals(SendTypeQuickSend) && application.isInP2PFallbackMode()) {
					activity.longToast(R.string.only_quick_supported);
					return;
				}
				String[] from = application.getRemoteWallet().getActiveAddresses();

				BigDecimal amountDecimal = BigDecimal.valueOf(getBTCEnteredOutputValue(edAmount1).doubleValue());
				//Add the fee
				final BigInteger amount = amountDecimal.add(amountDecimal.divide(BigDecimal.valueOf(100)).multiply(BigDecimal.valueOf(application.getRemoteWallet().getSharedFee()))).toBigInteger();

				//application.getRemoteWallet().sendCoinsAsync(from, receivingAddress.toString(), amount, feePolicy, fee, progress);
				
				//
				//
				//
				btSend.setVisibility(View.GONE);
		        ivCheck.setVisibility(View.VISIBLE);
		        tvSentPrompt.setVisibility(View.VISIBLE);
			}

            public void onClick(View v) {
				if (application.getRemoteWallet() == null)
					return;

				final MyRemoteWallet remoteWallet = application.getRemoteWallet();

				if (remoteWallet.isDoubleEncrypted() && remoteWallet.temporySecondPassword == null) {
					RequestPasswordDialog.show(getFragmentManager(), new SuccessCallback() {

						public void onSuccess() {							
							if(sendViaEmail && emailOrNumber != null && emailOrNumber.contains("@")) {	

				            	try {
									remoteWallet.sendCoinsEmail(emailOrNumber, getBTCEnteredOutputValue(edAmount1), progressEmailSMS);
								} catch (Exception e) {
									e.printStackTrace();
								}
							} else if (sentViaSMS && emailOrNumber != null) {								
								try {
									String numberFormated = emailOrNumber.replaceAll("\\D+","");	
									numberFormated = "+"+numberFormated;
									Log.d("sendCoinsSMS", "numberFormated: "+ numberFormated);
									remoteWallet.sendCoinsSMS(numberFormated, getBTCEnteredOutputValue(edAmount1), progressEmailSMS);										
								} catch (Exception e) {
									e.printStackTrace();
								}								
							} else {
								makeTransaction(MyRemoteWallet.FeePolicy.FeeOnlyIfNeeded);
							}
						}

						public void onFail() {
							Toast.makeText(application, R.string.send_no_password_error, Toast.LENGTH_LONG).show();
						}
					}, RequestPasswordDialog.PasswordTypeSecond);
				} else {
					if(sendViaEmail && emailOrNumber != null && emailOrNumber.contains("@")) {	
						try {
							remoteWallet.sendCoinsEmail(emailOrNumber, getBTCEnteredOutputValue(edAmount1), progressEmailSMS);
						} catch (Exception e) {
							e.printStackTrace();
						}
					} else if (sentViaSMS && emailOrNumber != null) {								
						try {
							String numberFormated = emailOrNumber.replaceAll("\\D+","");	
							numberFormated = "+"+numberFormated;
							Log.d("sendCoinsSMS", "numberFormated: "+ numberFormated);
							remoteWallet.sendCoinsSMS(numberFormated, getBTCEnteredOutputValue(edAmount1), progressEmailSMS);										
						} catch (Exception e) {
							e.printStackTrace();
						}								
					} else {
						makeTransaction(MyRemoteWallet.FeePolicy.FeeOnlyIfNeeded);
					}
				}

            }

        });

        tvAmount2 = ((TextView)rootView.findViewById(R.id.amount2));
        tvAmount2.setText("0.0000 BTC");
        edAmount1 = ((EditText)rootView.findViewById(R.id.amount1));
        edAmount1.setFocusableInTouchMode(true);
        edAmount1.setOnEditorActionListener(new OnEditorActionListener() {
		    @Override
		    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
		        if(actionId == EditorInfo.IME_ACTION_DONE) {
		        	
		        	if(sendType == SendTypeCustomSend) {

		        		doCustomSend();

		        	}
		        	else {

			        	summary.setVisibility(View.VISIBLE);
			        	summary2.setVisibility(View.VISIBLE);
			        	tvAddress.setVisibility(View.VISIBLE);
			        	tvAddressBis.setVisibility(View.VISIBLE);
			        	tvArrow.setVisibility(View.VISIBLE);
			        	tvAmount.setVisibility(View.VISIBLE);
			        	tvAmountBis.setVisibility(View.VISIBLE);
			        	
//			        	final WalletApplication application = (WalletApplication)getActivity().getApplication();
//	 		    		MyRemoteWallet wallet = application.getRemoteWallet();
//	 		    		Map<String,String> labels = wallet.getLabelMap();
	 		            if(currentSelectedAddress != null) {
	 		            	tvAddressBis.setText(currentSelectedAddress.substring(0, 20) + "...");
	 		            }
	 		            else {
	 		            	tvAddressBis.setVisibility(View.GONE);
	 		            }
	 //					Toast.makeText(application, "BTC going to:" + destination, Toast.LENGTH_LONG).show();
			        	
			        	if(edAddress.getText().toString().length() > 15) {
				        	tvAddress.setText(edAddress.getText().toString().substring(0, 15) + "...");
			        	}
			        	else {
				        	tvAddress.setText(edAddress.getText().toString());
			        	}

			        	tvArrow.setText(Character.toString((char)0x2192));

			        	String amount1 = edAmount1.getText().toString();
			        	if(amount1 == null || amount1.length() < 1) {
			        		amount1 = "0.00";
			        	}
			        	String amount2 = tvAmount2.getText().toString().substring(0, tvAmount2.getText().toString().length() - 4);	// buggy
			        	if(isBTC) {
			        		amount1 += " BTC";
			        		amount2 += " " + strCurrentFiatCode;
			        	}
			        	else {
			        		amount1 += " " + strCurrentFiatCode;
			        		amount2 += " BTC";
			        	}
			        	SpannableStringBuilder a1 = new SpannableStringBuilder(amount1);
			        	SpannableStringBuilder a2 = new SpannableStringBuilder(amount2);
			        	a1.setSpan(new SuperscriptSpan(), amount1.length() - 4, amount1.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			        	a1.setSpan(new RelativeSizeSpan((float)0.50), amount1.length() - 4, amount1.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			        	a2.setSpan(new SuperscriptSpan(), amount2.length() - 4, amount2.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			        	a2.setSpan(new RelativeSizeSpan((float)0.50), amount2.length() - 4, amount2.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			        	tvAmount.setText(a1);
			        	tvAmountBis.setText(a2);

		            	btSend.setVisibility(View.VISIBLE);

			        	edAmount1.clearFocus();
		                InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
		                imm.hideSoftInputFromWindow(edAmount1.getWindowToken(), 0);

		        	}

		        }
		        return false;
		    }
		});

        edAmount1.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {

            	if(ivCheck.getVisibility() == View.VISIBLE) {
            		clearSent();
            	}

            }
        });

        edAmount1.addTextChangedListener(new TextWatcher()	{

        	public void afterTextChanged(Editable s) {
        		if((edAddress.getText().toString() != null && edAddress.getText().toString().length() > 0) || (edAmount1.getText().toString() != null && edAmount1.getText().toString().length() > 0)) {
        			
        			if(isBTC)	{
            			tvAmount2.setText(BlockchainUtil.BTC2Fiat(edAmount1.getText().toString()) + " " + strCurrentFiatCode);
        			}
        			else	{
//                		tvAmount2.setTypeface(TypefaceUtil.getInstance(getActivity()).getBTCTypeface());
        				tvAmount2.setText(BlockchainUtil.Fiat2BTC(edAmount1.getText().toString()) + " BTC");
        			}

        			clear_input.setVisibility(View.VISIBLE);
        		}
        		else {
        			clear_input.setVisibility(View.INVISIBLE);
        		}
        	}

        	public void beforeTextChanged(CharSequence s, int start, int count, int after)	{ ; }
        
        	public void onTextChanged(CharSequence s, int start, int before, int count)	{ ; }
        });

        /*
        edAmount1.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            public void onFocusChange(View v, boolean hasFocus) {
                if(hasFocus) {
                    getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                }
            }
        });
        */

        edAmount1.setOnFocusChangeListener(new OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(hasFocus) {
                    if(edAmount1.getText().toString() != null && edAmount1.getText().toString().length() > 0) {
            			edAmount1.setText("");
                    	/*
                    	try {
                    		Double val = Double.parseDouble(edAmount1.getText().toString());
                    		if(val == 0.0) {
                    			edAmount1.setText("");
                    		}
                    	}
                    	catch(NumberFormatException nfe) {
                			edAmount1.setText("");
                    	}
                    	*/
                    }
                }
            }
        });

        edAddress = ((EditText)rootView.findViewById(R.id.address));
        edAddress.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
            	
            	if(!isMagic) {
            		
            		displayMagicList();

            	}

            	if(ivCheck.getVisibility() == View.VISIBLE) {
            		clearSent();
            	}
            }
        });

        edAddress.addTextChangedListener(new TextWatcher()	{

        	public void afterTextChanged(Editable s) {
        		if((edAddress.getText().toString() != null && edAddress.getText().toString().length() > 0) || (edAmount1.getText().toString() != null && edAmount1.getText().toString().length() > 0)) {
        			clear_input.setVisibility(View.VISIBLE);
        		}
        		else {
        			clear_input.setVisibility(View.INVISIBLE);
        		}
        	}

        	public void beforeTextChanged(CharSequence s, int start, int count, int after)	{ ; }
        
        	public void onTextChanged(CharSequence s, int start, int before, int count)	{        		
        		String inputAddress = edAddress.getText().toString();
        		int len = edAddress.getText().length();
        		List<HashMap<String,String>> filtered = new ArrayList<HashMap<String,String>>();
        		
        		for (HashMap<String,String> row : magicData) {
        			String labelOrAddress = row.get("labelOrAddress");
        		    if (len <= labelOrAddress.length()) {
            			if(inputAddress.equalsIgnoreCase((String) labelOrAddress.subSequence(0, len))) {
            				filtered.add(row);
            			}
        		    }
        		}

        		if (BitcoinAddressCheck.isValidAddress(inputAddress)) {
            		currentSelectedAddress = inputAddress;                        			
        		} else {
            		currentSelectedAddress = null;                        			
        		}
                filteredDisplayList = filtered;
                if(adapter != null)	{
            		adapter.notifyDataSetChanged();
                }
                
                //clear emailOrNumber if made change to name in edit text
                emailOrNumber = "";
            	sendViaEmail = false;
            	sentViaSMS = false;
            }
        });

        edAddress.setOnEditorActionListener(new OnEditorActionListener() {
		    @Override
		    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
		        if(actionId == EditorInfo.IME_ACTION_NEXT) {
		        	
		        	if(isMagic) {
		        		removeMagicList();
		        	}

	                InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
	                imm.hideSoftInputFromWindow(edAddress.getWindowToken(), 0);
	                edAmount1.requestFocus();
	                edAmount1.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
	                imm.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, 0);

		        }
		        return false;
		    }
		});

        clear_input.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
            	edAddress.setText("");
            	if(isBTC) {
                	edAmount1.setText("0.00");
                	tvAmount2.setText("0.00");
            	}
            	else {
                	edAmount1.setText("0.00");
                	tvAmount2.setText("0.00");
            	}
            	
                summary.setVisibility(View.INVISIBLE);
                summary2.setVisibility(View.INVISIBLE);
                tvAmount.setText("");
                tvAmount.setVisibility(View.INVISIBLE);
                tvAmountBis.setText("");
                tvAmountBis.setVisibility(View.INVISIBLE);
                tvArrow.setText("");
                tvArrow.setVisibility(View.INVISIBLE);
                tvAddress.setText("");
                tvAddress.setVisibility(View.INVISIBLE);
                tvAddressBis.setText("");
                tvAddressBis.setVisibility(View.INVISIBLE);

                btSend.setText("Send money");
                btSend.setVisibility(View.INVISIBLE);

                ivCheck.setVisibility(View.GONE);
                tvSentPrompt.setVisibility(View.GONE);

            	if(!isMagic) {
                	displayMagicList();
            	}

                clear_input.setVisibility(View.INVISIBLE);

                return false;
            }
        });
        clear_input.setVisibility(View.INVISIBLE);

        final ImageView imgSimpleSend = ((ImageView)rootView.findViewById(R.id.simple));
        final ImageView imgCustomSend = ((ImageView)rootView.findViewById(R.id.custom));
//        final ImageView imgSharedSend = ((ImageView)rootView.findViewById(R.id.shared));
        final LinearLayout layoutSimpleSend = ((LinearLayout)rootView.findViewById(R.id.simple_bg));
        final LinearLayout layoutCustomSend = ((LinearLayout)rootView.findViewById(R.id.custom_bg));
//        final LinearLayout layoutSharedSend = ((LinearLayout)rootView.findViewById(R.id.shared_bg));
        
        final int color_spend_selected = 0xff808080;
        final int color_spend_unselected = 0xffa0a0a0;
        
    	imgSimpleSend.setBackgroundColor(color_spend_selected);
    	imgCustomSend.setBackgroundColor(color_spend_unselected);
//    	imgSharedSend.setBackgroundColor(color_spend_unselected);
    	layoutSimpleSend.setBackgroundColor(color_spend_selected);
    	layoutCustomSend.setBackgroundColor(color_spend_unselected);
//    	layoutSharedSend.setBackgroundColor(color_spend_unselected);

        imgSimpleSend.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
            	sendType = SendTypeQuickSend;

            	imgSimpleSend.setBackgroundColor(color_spend_selected);
            	imgCustomSend.setBackgroundColor(color_spend_unselected);
//            	imgSharedSend.setBackgroundColor(color_spend_unselected);
            	layoutSimpleSend.setBackgroundColor(color_spend_selected);
            	layoutCustomSend.setBackgroundColor(color_spend_unselected);
//            	layoutSharedSend.setBackgroundColor(color_spend_unselected);

            	doSimpleSend();

                return true;
            }
        });

        imgCustomSend.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
            	sendType = SendTypeCustomSend;

            	imgSimpleSend.setBackgroundColor(color_spend_unselected);
            	imgCustomSend.setBackgroundColor(color_spend_selected);
//            	imgSharedSend.setBackgroundColor(color_spend_unselected);
            	layoutSimpleSend.setBackgroundColor(color_spend_unselected);
            	layoutCustomSend.setBackgroundColor(color_spend_selected);
//            	layoutSharedSend.setBackgroundColor(color_spend_unselected);
            	
//    			doCustomSend();

                return true;
            }
        });

        /*
        imgSharedSend.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
            	sendType = SendTypeSharedCoin;            	

            	imgSimpleSend.setBackgroundColor(color_spend_unselected);
            	imgCustomSend.setBackgroundColor(color_spend_unselected);
            	imgSharedSend.setBackgroundColor(color_spend_selected);
            	layoutSimpleSend.setBackgroundColor(color_spend_unselected);
            	layoutCustomSend.setBackgroundColor(color_spend_unselected);
            	layoutSharedSend.setBackgroundColor(color_spend_selected);

    			doSharedCoin();

                return true;
            }
        });
        */

        return rootView;
    }

    public BigInteger getBTCEnteredOutputValue(EditText edAmount) {
		String amountString = edAmount.getText().toString().trim();
    	if(! isBTC) {
    		return BlockchainUtil.bitcoinAmountStringToBigInteger(BlockchainUtil.Fiat2BTC(amountString));
    	} else {
    		return BlockchainUtil.bitcoinAmountStringToBigInteger(amountString);
    	}
    }    
    
    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        
        Log.d("BlockchainWallet", "setUserVisible");

        /*
        if(isVisibleToUser) {
        	if(isMagic) {
        		removeMagicList();
        	}
        	displayMagicList();
        	doSimpleSend();
        }
        else {
        	;
        }
        */

    }

    @Override
    public void onResume() {
    	super.onResume();

        Log.d("BlockchainWallet", "onResume");

		removeMagicList();
    	displayMagicList();

    }

	@Override
	public void onDestroyView()
	{
		super.onDestroyView();

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
		
	    LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(receiver);
	      
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if(resultCode == Activity.RESULT_OK && requestCode == ZBAR_SCANNER_REQUEST)	{

			String address = data.getStringExtra(ZBarConstants.SCAN_RESULT);
//        	Log.d("Scan result", strResult);
			
			doScanInput(address);

        }
		else if(resultCode == Activity.RESULT_CANCELED && requestCode == ZBAR_SCANNER_REQUEST) {
//            Toast.makeText(this, R.string.camera_unavailable, Toast.LENGTH_SHORT).show();
        }
		else if(requestCode == PICK_CONTACT && resultCode == Activity.RESULT_OK) {

			if (data != null) {

				Uri uri = data.getData();

		        if (uri != null) {

		    	    Cursor cur = getActivity().getContentResolver().query(uri, null, null, null, null);

                	String strEmail = null;
                	String strNumber = null;

		    	    try 
		    	    {
		                while(cur.moveToNext())
		                {
		                	strEmail = strNumber = null;
		                	
		                    String id = cur.getString(cur.getColumnIndex(Contacts._ID));
		                    String strName = cur.getString(cur.getColumnIndex(Contacts.DISPLAY_NAME));
		                    
//		                    strImageURI = cur.getString(cur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_URI));

		                    Cursor ce = getActivity().getContentResolver().query(CommonDataKinds.Email.CONTENT_URI, null, CommonDataKinds.Email.CONTACT_ID +" = ?", new String[]{id}, null);
		                    while(ce.moveToNext())
		                    {
		                        strEmail = ce.getString(ce.getColumnIndex(CommonDataKinds.Email.ADDRESS));
		                        strEmail = (strEmail.equals("null")) ? null : strEmail;
		                    }
		                    ce.close();

		                    Cursor cn = getActivity().getContentResolver().query(CommonDataKinds.Phone.CONTENT_URI, null, CommonDataKinds.Phone.CONTACT_ID +" = ?", new String[]{id}, null);
		                    while(cn.moveToNext())
		                    {
		                        int type = cn.getInt(cn.getColumnIndex(CommonDataKinds.Phone.TYPE));
		                        if (type == Phone.TYPE_MOBILE)
		                        {
		                            strNumber = cn.getString(cn.getColumnIndex(CommonDataKinds.Phone.NUMBER));
		                            strNumber = (strNumber.equals("null")) ? null : strNumber;
		                        }
		                    }
		                    cn.close();
		                    
		                    if(strEmail != null && strEmail.equals("null"))	{
		                    	strEmail = null;
		                    }
		                    if(strNumber != null && strNumber.equals("null"))	{
		                    	strNumber = null;
		                    }
		                    if(strName != null && strName.equals("null"))	{
		                    	strName = "";
		                    }


		                    if(strEmail != null && strNumber != null)	{
		                    		//
		                    		// choose send method here
		                    		//
		                    		
		                    		final String em = strEmail;
		                    		final String sms = strNumber;
		                    		final String name = strName;
		                    		
		                			new AlertDialog.Builder(getActivity())
		                            .setIcon(R.drawable.ic_launcher).setTitle("Send Bitcoins to a Friend")
		                            .setMessage("Send Bitcoins to " + strName + " via which method?")
		                            .setPositiveButton(em, new DialogInterface.OnClickListener() {
//		                              @Override
		                              public void onClick(DialogInterface dialog, int which) {
		                            	  
				                    		edAddress.setText(name);
				                    		emailOrNumber = em;
				                        	sendViaEmail = true;
				                        	sentViaSMS = false;
				                        	
				                    		// go out via email here
				                    		Toast.makeText(getActivity(), em, Toast.LENGTH_SHORT).show();
		                              }
		                           })
		                            .setNegativeButton(sms, new DialogInterface.OnClickListener() {
//		                              @Override
		                              public void onClick(DialogInterface dialog, int which) {
		                            	  
				                    		edAddress.setText(name);
				                        	sendViaEmail = false;
				                        	sentViaSMS = true;
				                        	
				                    		emailOrNumber = sms;	
				                        	if (sms.substring(0, 2).equals("00") || sms.charAt(0) == '+') {
					                    		Log.d("emailOrNumber", "setSMSNumber: " + emailOrNumber);
				                        	} else {
				                    			doSelectInternationalPrefix();				                        		
				                        	}
				                        					                    		
				                    		// go out via sms here
				                    		Toast.makeText(getActivity(), sms, Toast.LENGTH_SHORT).show();
		                              }
		                            }
		                            ).show();

		                    	}
		                    	else if(strEmail != null)	{
		                    		//
		                    		// send via email here
		                    		//
		                    		Toast.makeText(getActivity(), strEmail, Toast.LENGTH_SHORT).show();
		                    		
		                    		edAddress.setText(strName);
		                    		emailOrNumber = strEmail;
		                        	sendViaEmail = true;
		                        	sentViaSMS = false;
		                    		// go out via email here
			                    }
		                    	else if(strNumber != null)	{
		                    		//
		                    		// send via sms here
		                    		//
		                    		Toast.makeText(getActivity(), strNumber, Toast.LENGTH_SHORT).show();
		                    		
		                    		edAddress.setText(strName);
		                    		emailOrNumber = strNumber;
		                        	if (strNumber.substring(0, 2).equals("00") || strNumber.charAt(0) == '+') {
			                    		Log.d("emailOrNumber", "setSMSNumber: " + emailOrNumber);
		                        	} else {
		                    			doSelectInternationalPrefix();				                        		
		                        	}
		                        	sendViaEmail = false;
		                        	sentViaSMS = true;
		                    		//go out via sms here
		                    		
			                    }
		                    	else
		                    	{
		                    		// this will be replaced by proper model dialog by Bill w/ official text
		                    		Toast.makeText(getActivity(), "To use this service select a contact with an email address or a mobile phone number. Thank you.", Toast.LENGTH_SHORT).show();
		                    	}

	                    	//
	                    	//
	                    	//
		            		if(isMagic) {
		            			removeMagicList();
		            		}
	                        InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
	                        imm.hideSoftInputFromWindow(edAddress.getWindowToken(), 0);
		                        edAmount1.requestFocus();
		                        edAmount1.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
		                        imm.showSoftInput(edAmount1, InputMethodManager.SHOW_FORCED);

		                }
		    	    }
		    	    finally
		    	    {
		    	        cur.close();
		    	    }
		        
		        }
		    }

		}
		else if(resultCode == Activity.RESULT_OK && requestCode == SELECT_INTL_PREFIX) {
    		//Toast.makeText(getActivity(), "prefix returned:" + data.getAction(), Toast.LENGTH_SHORT).show();

    		String region = "US";
    		PhoneNumberUtil p = PhoneNumberUtil.getInstance();
    		PhoneNumber pn;
    		try {
        		//emailOrNumber = "+442012345678";
    			pn = p.parse(emailOrNumber, region);
    			String nationalnumber = String.valueOf(pn.getNationalNumber());
        		emailOrNumber = "+" + data.getAction() + nationalnumber;
        		Log.d("emailOrNumber", "setSMSNumber with prefix: " + emailOrNumber);
    		} catch (NumberParseException e1) {
    			// TODO Auto-generated catch block
    			e1.printStackTrace();
    		}
	      }
		else if(resultCode == Activity.RESULT_CANCELED && requestCode == SELECT_INTL_PREFIX) {
	  		final Context context = getActivity().getApplicationContext();

    		String region = context.getResources().getConfiguration().locale.getCountry();
    		PhoneNumberUtil p = PhoneNumberUtil.getInstance();
    		PhoneNumber pn;
    		try {
				pn = p.parse(emailOrNumber, region);
				String nationalnumber = String.valueOf(pn.getNationalNumber());
	    		emailOrNumber = "+" + pn.getCountryCode() + nationalnumber;
        		Log.d("emailOrNumber", "setSMSNumber default to local: " + emailOrNumber);
    		} catch (NumberParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	      }
		else {
			;
		}
		
	}
	
    private Bitmap generateQRCode(String uri) {

        Bitmap bitmap = null;
        int qrCodeDimension = 150;

        QRCodeEncoder qrCodeEncoder = new QRCodeEncoder(uri, null, Contents.Type.TEXT, BarcodeFormat.QR_CODE.toString(), qrCodeDimension);

    	try {
            bitmap = qrCodeEncoder.encodeAsBitmap();
        } catch (WriterException e) {
            e.printStackTrace();
        }
    	
    	return bitmap;
    }

    public static void sendViewToBack(final View child) {
    	if(child != null) {
            final ViewGroup parent = (ViewGroup)child.getParent();
            if (null != parent) {
                parent.removeView(child);
                parent.addView(child, 0);
            }
    	}
    }

    private class MagicAdapter extends BaseAdapter {
    	
		private LayoutInflater inflater = null;

	    MagicAdapter() {
	        inflater = (LayoutInflater)getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}

		@Override
		public int getCount() {
			if(filteredDisplayList != null) {
				return filteredDisplayList.size();
			}
			else {
				return 0;
			}
		}

		@Override
		public String getItem(int position) {
			HashMap<String,String> row = filteredDisplayList.get(position);
			return row.get("labelOrAddress");
		}

		@Override
		public long getItemId(int position) {
			return position;
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {

			View view;
	        
	        if (convertView == null) {
	            view = inflater.inflate(R.layout.magic_entry, parent, false);
	        } else {
	            view = convertView;
	        }

	        ((TextView)view.findViewById(R.id.p1)).setTypeface(TypefaceUtil.getInstance(getActivity()).getGravityBoldTypeface());
	        HashMap<String,String> row = filteredDisplayList.get(position);
	        if(row.get("label") != null) {
		        ((TextView)view.findViewById(R.id.p1)).setTypeface(TypefaceUtil.getInstance(getActivity()).getGravityBoldTypeface());
	        }
	        else {
		        ((TextView)view.findViewById(R.id.p1)).setTypeface(TypefaceUtil.getInstance(getActivity()).getGravityLightTypeface());
	        }

	        // Names, Labels in black, addresses in GREY
	        if(row.get("label") != null) {
		        ((TextView)view.findViewById(R.id.p1)).setTextColor(Color.BLACK);
	        }
	        else {
		        ((TextView)view.findViewById(R.id.p1)).setTextColor(0xFF616161);
	        }
	        
	        String labelOrAddress = BlockchainUtil.formatAddress(row.get("labelOrAddress"), 15) ;
	        ((TextView)view.findViewById(R.id.p1)).setText(labelOrAddress);
	        
	        if (contactsOn) {
		        String address = BlockchainUtil.formatAddress(row.get("address"), 15) ;
		        ((TextView)view.findViewById(R.id.p2)).setText(address);
	        } else {
		        ((TextView)view.findViewById(R.id.p2)).setText(row.get("amount"));	        	
	        }

	        return view;
		}

    }

    private void initMagicList() {

		final WalletApplication application = (WalletApplication)getActivity().getApplication();
		MyRemoteWallet wallet = application.getRemoteWallet();
		activeAddresses = Arrays.asList(wallet.getActiveAddresses());
		labels = wallet.getLabelMap();
        
        magicData =  new ArrayList<HashMap<String,String>>();
        
        filteredDisplayList = new ArrayList<HashMap<String,String>>();

        for(int i = 0; i < activeAddresses.size(); i++) {
		    String address = activeAddresses.get(i);
        	String amount = "0.000";
		    BigInteger finalBalance = wallet.getBalance(address);	
		    if (finalBalance != null)
		    	amount = BlockchainUtil.formatBitcoin(finalBalance);

		        HashMap<String,String> row = new HashMap<String,String>();
		        
		        String label = labels.get(address);
		        String labelOrAddress;
		        if (label != null) {
		            row.put("label", label.toString());	
		            labelOrAddress = label;
		        } else {
		        	labelOrAddress = address;
		        }
		        row.put("address", address.toString());
		        row.put("amount", amount);
		        row.put("labelOrAddress", labelOrAddress);

				magicData.add(row);    
						
	        	filteredDisplayList.add(row);
        }

    }
    
    private void initAddressBookList() {
 		final WalletApplication application = (WalletApplication)getActivity().getApplication();
 		MyRemoteWallet wallet = application.getRemoteWallet();
 		
        magicData =  new ArrayList<HashMap<String,String>>();

        addressBookMapList = wallet.getAddressBookMap();
        filteredDisplayList = new ArrayList<HashMap<String,String>>();

        if (addressBookMapList != null) {
  		    for (Iterator<Map<String, Object>> iti = addressBookMapList.iterator(); iti.hasNext();) {
 		    	Map<String, Object> addressBookMap = iti.next();
 		    	Object address = addressBookMap.get("addr");
 		    	Object label = addressBookMap.get("label");

 		        HashMap<String,String> row = new HashMap<String,String>();
 		        row.put("label", label.toString());
 		        row.put("address", address.toString());
		        row.put("labelOrAddress", label.toString());

    			magicData.add(row);
	         	filteredDisplayList.add(row);
 		    }

        }
        
     }

    private void displayMagicList() {
    	LayoutInflater inflater = (LayoutInflater)getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

    	isMagic = true;
		
		final int colorOn = 0xFF9d9d9d;
		final int colorOff = 0xFFb6b6b6;
		
		//
		//
		//
		if(rootView == null) {
	        rootView = inflater.inflate(R.layout.fragment_receive, null, false);
		}
		((TextView)rootView.findViewById(R.id.currency)).setText("");
        ((TextView)rootView.findViewById(R.id.currency)).setBackgroundColor(0xFF232223);

		//
        // add view with my_addresses and contacts
        //
        magic = ((LinearLayout)rootView.findViewById(R.id.magic_input));
        oldView = ((LinearLayout)magic.findViewById(R.id.magic_bis));
        parent = (LinearLayout)oldView.getParent();
        oldView.setVisibility(View.GONE);
		childIcons = inflater.inflate(R.layout.magic, null);
        ivAddresses = (ImageView)childIcons.findViewById(R.id.addresses);
        ivAddresses.setImageResource(R.drawable.my_addresses);
        ivAddresses.setBackgroundColor(colorOff);
        ivContacts = (ImageView)childIcons.findViewById(R.id.contacts);
        ivContacts.setImageResource(R.drawable.address_book);
        ivContacts.setBackgroundColor(colorOn);
        ivPhoneContacts = (ImageView)childIcons.findViewById(R.id.phone_contacts);
        ivPhoneContacts.setImageResource(R.drawable.phone_contacts);
        ivPhoneContacts.setBackgroundColor(colorOff);
        addressesOn = false;
        contactsOn = true;
        phoneContactsOn = false;
        ivAddresses.setOnClickListener(new View.OnClickListener() {        
            @Override
                public void onClick(View view) {
            		if(!addressesOn) {
            			addressesOn = true;
            			contactsOn = false;
            			phoneContactsOn = false;
                        ivAddresses.setBackgroundColor(colorOn);
                        ivContacts.setBackgroundColor(colorOff);
                        ivPhoneContacts.setBackgroundColor(colorOff);
            		}
            		initMagicList();
            		adapter.notifyDataSetChanged();                            		
                }
        });
        ivContacts.setOnClickListener(new View.OnClickListener() {        
            @Override
                public void onClick(View view) {
            		if(!contactsOn) {
            			contactsOn = true;
            			phoneContactsOn = false;
            			addressesOn = false;
                        ivAddresses.setBackgroundColor(colorOff);
                        ivContacts.setBackgroundColor(colorOn);
                        ivPhoneContacts.setBackgroundColor(colorOff);
            		}
            		initAddressBookList();
            		adapter.notifyDataSetChanged();                            		
                }
        });
        ivPhoneContacts.setOnClickListener(new View.OnClickListener() {        
            @Override
                public void onClick(View view) {
            		if(!phoneContactsOn) {
            			contactsOn = false;
            			phoneContactsOn = true;
            			addressesOn = false;
                        ivAddresses.setBackgroundColor(colorOff);
                        ivContacts.setBackgroundColor(colorOff);
                        ivPhoneContacts.setBackgroundColor(colorOn);
            		}
            		try {
						doSend2Friends();
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
//            		adapter.notifyDataSetChanged();                            		
                }
        });

        final ImageView qr_scan = (ImageView)childIcons.findViewById(R.id.qr_icon);
        qr_scan.setVisibility(View.INVISIBLE);
        /*
        qr_scan.setBackgroundColor(colorOff);
        qr_scan.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
            	
                switch (event.getAction())	{
                	case android.view.MotionEvent.ACTION_DOWN:
                	case android.view.MotionEvent.ACTION_MOVE:
                		Toast.makeText(getActivity(), "Show QR reader", Toast.LENGTH_SHORT).show();
                		Log.d("QR icon", "DOWN");
                		qr_scan.setBackgroundColor(colorOn);                		
                		Intent intent = new Intent(getActivity(), ZBarScannerActivity.class);
                		intent.putExtra(ZBarConstants.SCAN_MODES, new int[]{ Symbol.QRCODE } );
                		startActivityForResult(intent, ZBAR_SCANNER_REQUEST);
                		break;
                	case android.view.MotionEvent.ACTION_UP:
                	case android.view.MotionEvent.ACTION_CANCEL:
                		Log.d("QR icon", "UP or CANCEL");
                		qr_scan.setBackgroundColor(colorOff);
                		break;
                	}

                return false;
            }
        });
        */

//	    parent.addView(child, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
	    parent.addView(childIcons);
	    children++;
	    
    	LinearLayout divider1 = (LinearLayout)childIcons.findViewById(R.id.divider1);
    	divider1.setBackgroundColor(BlockchainUtil.BLOCKCHAIN_RED);

        //
        // add view with list
        //
		childList = inflater.inflate(R.layout.magic2, null);
    	divider1 = (LinearLayout)childList.findViewById(R.id.divider1);
    	divider1.setBackgroundColor(BlockchainUtil.BLOCKCHAIN_RED);
        magicList = ((ListView)childList.findViewById(R.id.magicList));
        magicList.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
	    parent.addView(childList);
	    children++;

        magicList.setOnItemClickListener(new OnItemClickListener() {
	        public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3)	{
//                Toast.makeText(getActivity(), keys.get(position), Toast.LENGTH_SHORT).show();
                InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(edAddress.getWindowToken(), 0);

                HashMap<String, String> map = filteredDisplayList.get(position);
                String labelOrAddress = map.get("labelOrAddress");
            	edAddress.setText(labelOrAddress);         	                	               
            	currentSelectedAddress = map.get("address");

                removeMagicList();
                edAmount1.requestFocus();
                edAmount1.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
                imm.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, 0);

            	if(isBTC) {
            	    tvCurrency.setTypeface(TypefaceUtil.getInstance(getActivity()).getBTCTypeface());
            		tvCurrency.setText(Character.toString((char)TypefaceUtil.getInstance(getActivity()).getBTCSymbol()));
            	}
            	else {
            	    tvCurrency.setTypeface(TypefaceUtil.getInstance(getActivity()).getBTCTypeface());
            		tvCurrency.setText(strCurrentFiatSymbol);
            	}
                
            }
        });

        adapter = new MagicAdapter();
        magicList.setAdapter(adapter);

        LinearLayout container = ((LinearLayout)rootView.findViewById(R.id.send_container));
        sendViewToBack(container);
        
//	    parent.bringToFront();
//	    parent.requestLayout();
//	    parent.invalidate();
    }

    private void removeMagicList() {
		isMagic = false;

        ((TextView)rootView.findViewById(R.id.currency)).setBackgroundColor(0xFFFFFFFF);
        ((TextView)rootView.findViewById(R.id.currency)).setText(strCurrentFiatSymbol);
        
        if(parent != null) {
            parent.removeViews(parent.getChildCount() - children, children);
            children = 0;
            oldView.setVisibility(View.VISIBLE);
        }

        if(addressesOn) {
            initMagicList();
        }
        else {
    		initAddressBookList();
        }

    }

    private void doSimpleSend() {
    	if(magic != null) {
    		magic.setVisibility(View.VISIBLE);
    	}

    	simple_spend.setVisibility(View.VISIBLE);
    	custom_spend.setVisibility(View.GONE);
        LinearLayout container = ((LinearLayout)rootView.findViewById(R.id.send_container));
        sendViewToBack(container);
    	CURRENT_SEND = SIMPLE_SEND;
    }

    private void doCustomSend() {
    	if(isMagic) {
    		removeMagicList();
    	}

    	/*
    	if(magic != null) {
    		magic.setVisibility(View.GONE);
    	}
    	*/

    	simple_spend.setVisibility(View.GONE);
    	custom_spend.setVisibility(View.VISIBLE);
    	CURRENT_SEND = CUSTOM_SEND;
    	
    	lastSendingAddress = null;

    	final LayoutInflater inflater = (LayoutInflater)getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

    	LinearLayout layout_custom_spend = (LinearLayout)rootView.findViewById(R.id.custom_spend);

    	// all 'sending address' entries go here:
    	LinearLayout layout_froms = (LinearLayout)layout_custom_spend.findViewById(R.id.froms);
    	// first 'sending address':
        LinearLayout layout_from = (LinearLayout)inflater.inflate(R.layout.layout_custom_segment, layout_custom_spend, false);
    	// additional 'sending address':
        LinearLayout layout_from2 = (LinearLayout)inflater.inflate(R.layout.layout_custom_segment, layout_custom_spend, false);
    	// 'fee':
        LinearLayout layout_fee = (LinearLayout)inflater.inflate(R.layout.layout_custom_segment, layout_custom_spend, false);
    	// 'change address':
        LinearLayout layout_change = (LinearLayout)inflater.inflate(R.layout.layout_custom_segment, layout_custom_spend, false);

        // remove any previous views
        if(layout_custom_spend.getChildCount() > 1) {
            layout_custom_spend.removeViews(1, layout_custom_spend.getChildCount() - 1);
        }
        layout_froms.removeAllViews();

        //
        // 'FROM' layout
        //
        TextView tvSpend = new TextView(getActivity());
    	tvSpend.setTextColor(0xFF3eb6e2);
    	tvSpend.setTypeface(null, Typeface.BOLD);
        tvSpend.setText("FROM");
        tvSpend.setTextSize(12);
        tvSpend.setPadding(5, 5, 5, 5);
        tvSpend.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        LayoutParams layout_params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        tvSpend.setLayoutParams(layout_params);
    	((LinearLayout)layout_from.findViewById(R.id.divider1)).setBackgroundColor(0xFF3eb6e2);
    	((LinearLayout)layout_from.findViewById(R.id.p1)).setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);
    	((LinearLayout)layout_from.findViewById(R.id.p1)).addView(tvSpend);

    	/*
        TextView tvSendingAddress = new TextView(getActivity());
        tvSendingAddress.setId(ViewIdGenerator.generateViewId());
        tvSendingAddress.setText("Walking around money");
        tvSendingAddress.setTextSize(16);
        tvSendingAddress.setPadding(5, 5, 5, 5);
        tvSendingAddress.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
        layout_params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        tvSendingAddress.setLayoutParams(layout_params);
    	((LinearLayout)layout_from.findViewById(R.id.p2)).setGravity(Gravity.LEFT|Gravity.CENTER_VERTICAL);
    	((LinearLayout)layout_from.findViewById(R.id.p2)).addView(tvSendingAddress);
    	*/

        final EditText edAddress = new EditText(getActivity());
        edAddress.setId(ViewIdGenerator.generateViewId());
        edAddress.setText("");
        edAddress.setTextSize(16);
        edAddress.setTextColor(Color.BLACK);
        edAddress.setPadding(5, 5, 5, 5);
        edAddress.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
        layout_params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        edAddress.setLayoutParams(layout_params);
    	((LinearLayout)layout_from.findViewById(R.id.p2)).setGravity(Gravity.LEFT|Gravity.CENTER_VERTICAL);
    	((LinearLayout)layout_from.findViewById(R.id.p2)).addView(edAddress);

    	final EditText edAmount = new EditText(getActivity());
        edAmount.setId(ViewIdGenerator.generateViewId());
        edAmount.setText("0.00");
        edAmount.setTextSize(16);
        edAmount.setTextColor(Color.BLACK);
        edAmount.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        edAmount.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        edAmount.setTextColor(BlockchainUtil.BLOCKCHAIN_RED);
        edAmount.setLayoutParams(layout_params);
    	((LinearLayout)layout_from.findViewById(R.id.p3)).setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
    	((LinearLayout)layout_from.findViewById(R.id.p3)).addView(edAmount);

    	/*
    	ImageButton ibPlus = new ImageButton(getActivity());
    	ibPlus.setImageResource(R.drawable.plus_icon);
    	((LinearLayout)layout_from.findViewById(R.id.plus)).addView(ibPlus);
        ibPlus.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
            	addSendingAddress();
            }
        });
        */

    	((LinearLayout)layout_custom_spend.findViewById(R.id.froms)).addView(layout_from);
    	lastSendingAddress = layout_from;
    	
    	/*
    	 * 
    	 */

        // second send address
        TextView tvSpend2 = new TextView(getActivity());
        tvSpend2.setText("");
        tvSpend2.setTextSize(12);
        tvSpend2.setPadding(5, 5, 5, 5);
        tvSpend2.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        layout_params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        tvSpend2.setLayoutParams(layout_params);
    	((LinearLayout)layout_from2.findViewById(R.id.divider1)).setBackgroundColor(0xFF3eb6e2);
    	((LinearLayout)layout_from2.findViewById(R.id.p1)).setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
    	((LinearLayout)layout_from2.findViewById(R.id.p1)).addView(tvSpend2);
    	
        final EditText edAddress2 = new EditText(getActivity());
        edAddress2.setId(ViewIdGenerator.generateViewId());
        edAddress2.setText("");
        edAddress2.setTextSize(16);
        edAddress2.setTextColor(Color.BLACK);
        edAddress2.setPadding(5, 5, 5, 5);
        edAddress2.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
        layout_params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        edAddress.setLayoutParams(layout_params);
    	((LinearLayout)layout_from2.findViewById(R.id.p2)).setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
    	((LinearLayout)layout_from2.findViewById(R.id.p2)).addView(edAddress2);

    	final EditText edAmount2 = new EditText(getActivity());
        edAmount2.setId(ViewIdGenerator.generateViewId());
        edAmount2.setText("0.00");
        edAmount2.setTextSize(16);
        edAmount2.setTextColor(Color.BLACK);
        edAmount2.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        edAmount2.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        edAmount2.setTextColor(BlockchainUtil.BLOCKCHAIN_RED);
        edAmount2.setLayoutParams(layout_params);
    	((LinearLayout)layout_from2.findViewById(R.id.p3)).setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
    	((LinearLayout)layout_from2.findViewById(R.id.p3)).addView(edAmount2);

    	((LinearLayout)layout_custom_spend.findViewById(R.id.froms)).addView(layout_from2);

    	/*
    	 * 
    	 */
    	
        //
        // 'FEE' layout
        //
        TextView tvFee = new TextView(getActivity());
        tvFee.setId(ViewIdGenerator.generateViewId());
    	tvFee.setTextColor(0xFFFF0000);
    	tvFee.setTypeface(null, Typeface.BOLD);
        tvFee.setText("FEE");
        tvFee.setTextSize(12);
        tvFee.setPadding(5, 5, 5, 5);
        tvFee.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        layout_params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        tvFee.setLayoutParams(layout_params);
    	((LinearLayout)layout_fee.findViewById(R.id.divider1)).setBackgroundColor(BlockchainUtil.BLOCKCHAIN_RED);
    	((LinearLayout)layout_fee.findViewById(R.id.p1)).setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
    	((LinearLayout)layout_fee.findViewById(R.id.p1)).addView(tvFee);
    	final EditText edFee = new EditText(getActivity());
        edFee.setId(ViewIdGenerator.generateViewId());
        edFee.setText("0.005");
        edFee.setTextSize(16);
        edFee.setTextColor(Color.BLACK);
        edFee.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        edFee.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
        edFee.setLayoutParams(layout_params);
    	((LinearLayout)layout_fee.findViewById(R.id.p2)).addView(edFee);
        TextView tvFee3 = new TextView(getActivity());
        tvFee3.setText("0.005 BTC");
        tvFee3.setTextSize(16);
        tvFee3.setTextColor(BlockchainUtil.BLOCKCHAIN_RED);
        tvFee3.setPadding(5, 5, 5, 5);
        tvFee3.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        layout_params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        tvFee3.setLayoutParams(layout_params);
    	((LinearLayout)layout_fee.findViewById(R.id.p3)).setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
    	((LinearLayout)layout_fee.findViewById(R.id.p3)).addView(tvFee3);
    	layout_fee.setPadding(0, 10, 0, 0);
    	((LinearLayout)layout_custom_spend.findViewById(R.id.custom_spend)).addView(layout_fee);

    	/*
        //
        // 'CHANGE' layout
        //
        TextView tvChange = new TextView(getActivity());
        tvChange.setTypeface(null, Typeface.BOLD);
        tvChange.setText("CHANGE");
        tvChange.setTextSize(12);
        tvChange.setPadding(5, 5, 5, 5);
        tvChange.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        layout_params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        tvChange.setLayoutParams(layout_params);
    	((LinearLayout)layout_change.findViewById(R.id.divider1)).setBackgroundColor(0xFF808080);
    	((LinearLayout)layout_change.findViewById(R.id.p1)).setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);
    	((LinearLayout)layout_change.findViewById(R.id.p1)).addView(tvChange);
        TextView tvChange2 = new TextView(getActivity());
        tvChange2.setText("Savings address");
        tvChange2.setTextSize(16);
        tvChange2.setPadding(5, 5, 5, 5);
        tvChange2.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
    	((LinearLayout)layout_change.findViewById(R.id.p2)).setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
    	((LinearLayout)layout_change.findViewById(R.id.p2)).addView(tvChange2);
        TextView tvChange3 = new TextView(getActivity());
        tvChange3.setText("1.965 BTC");
        tvChange3.setTextSize(16);
        tvChange3.setTextColor(BlockchainUtil.BLOCKCHAIN_GREEN);
        tvChange3.setPadding(5, 5, 5, 5);
        tvChange3.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
    	((LinearLayout)layout_change.findViewById(R.id.p3)).setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);
    	((LinearLayout)layout_change.findViewById(R.id.p3)).addView(tvChange3);
    	layout_change.setPadding(0, 10, 0, 0);
    	((LinearLayout)layout_custom_spend.findViewById(R.id.custom_spend)).addView(layout_change);
    	*/
    	
    	Button btConfirm = new Button(getActivity());
    	btConfirm.setText("OK");
    	btConfirm.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        btConfirm.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {

            	//
            	// get data entered by user and pass custom send instance
            	//
            	cs = new CustomSend();
            	
            	if(edAddress.getText().toString() != null && edAddress.getText().toString().length() > 0 &&
            			edAmount.getText().toString() != null && edAmount.getText().toString().length() > 0 &&
            			Double.parseDouble(edAmount.getText().toString()) > 0.0) {
            		cs.addReceivingAddress(edAddress.getText().toString(), getBTCEnteredOutputValue(edAmount));
            		
            	}
            	
            	if(edAddress2.getText().toString() != null && edAddress2.getText().toString().length() > 0 &&
            			edAmount2.getText().toString() != null && edAmount2.getText().toString().length() > 0 &&
            			Double.parseDouble(edAmount2.getText().toString()) > 0.0) {
            		cs.addReceivingAddress(edAddress2.getText().toString(), getBTCEnteredOutputValue(edAmount2));
            	}
            	
            	if(edFee.getText().toString() != null && edFee.getText().toString().length() > 0 &&
            			Double.parseDouble(edFee.getText().toString()) > 0.0) {
            		cs.setFee(getBTCEnteredOutputValue(edFee));
            	}

            	//
            	//
            	//
            	HashMap<String, BigInteger> addresses = cs.getReceivingAddresses();
            	Set<String> keys = addresses.keySet();
            	for (Iterator iterator = keys.iterator(); iterator.hasNext();) {
                    String s = (String)iterator.next();
            		Toast.makeText(getActivity(), "Address:" + s + ", amount:" + addresses.get(s), Toast.LENGTH_SHORT).show();
                }
        		Toast.makeText(getActivity(), "Fee:" + cs.getFee(), Toast.LENGTH_SHORT).show();
        		//
        		//
        		//

            	//
            	// take 'cs' here and initiate custom send
            	//

            }
        });
    	((LinearLayout)layout_custom_spend.findViewById(R.id.custom_spend)).addView(btConfirm);

        LinearLayout container = ((LinearLayout)rootView.findViewById(R.id.custom_spend));
        sendViewToBack(container);

    }

    private void addSendingAddress() {
    	
    	if(lastSendingAddress != null) {
        	((LinearLayout)lastSendingAddress.findViewById(R.id.plus)).getChildAt(0).setVisibility(View.INVISIBLE);
    	}

    	final LayoutInflater inflater = (LayoutInflater)getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

    	LinearLayout layout_custom_spend = (LinearLayout)rootView.findViewById(R.id.custom_spend);
    	// additional 'sending address':
        LinearLayout layout_from2 = (LinearLayout)inflater.inflate(R.layout.layout_custom_segment, layout_custom_spend, false);

        // second send address
        TextView tvSpend = new TextView(getActivity());
        tvSpend.setText("");
        tvSpend.setTextSize(12);
        tvSpend.setPadding(5, 5, 5, 5);
        tvSpend.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        LayoutParams layout_params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        tvSpend.setLayoutParams(layout_params);
    	((LinearLayout)layout_from2.findViewById(R.id.divider1)).setBackgroundColor(0xFF3eb6e2);
    	((LinearLayout)layout_from2.findViewById(R.id.p1)).setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
    	((LinearLayout)layout_from2.findViewById(R.id.p1)).addView(tvSpend);
    	
        TextView tvSendingAddress = new TextView(getActivity());
        tvSendingAddress.setId(ViewIdGenerator.generateViewId());
        tvSendingAddress.setText("Lukewarm storage");
        tvSendingAddress.setTextSize(16);
        tvSendingAddress.setPadding(5, 5, 5, 5);
        tvSendingAddress.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
        layout_params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        tvSendingAddress.setLayoutParams(layout_params);
    	((LinearLayout)layout_from2.findViewById(R.id.p2)).setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
    	((LinearLayout)layout_from2.findViewById(R.id.p2)).addView(tvSendingAddress);

    	EditText edAmount = new EditText(getActivity());
        edAmount.setId(ViewIdGenerator.generateViewId());
        edAmount.setText("0.00");
        edAmount.setTextSize(16);
        edAmount.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        edAmount.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        edAmount.setTextColor(BlockchainUtil.BLOCKCHAIN_RED);
        edAmount.setLayoutParams(layout_params);
    	((LinearLayout)layout_from2.findViewById(R.id.p3)).setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
    	((LinearLayout)layout_from2.findViewById(R.id.p3)).addView(edAmount);
    	
    	ImageButton ibPlus = new ImageButton(getActivity());
    	ibPlus.setImageResource(R.drawable.plus_icon);
    	((LinearLayout)layout_from2.findViewById(R.id.plus)).addView(ibPlus);
        ibPlus.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
            	addSendingAddress();
            }
        });

    	((LinearLayout)layout_custom_spend.findViewById(R.id.froms)).addView(layout_from2);
    	lastSendingAddress = layout_from2;
    }

    private void doSharedCoin() {
    	CURRENT_SEND = SHARED_SEND;
    }

    private void doSend2Friends() throws Exception	{
    	Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
    	intent.setData(ContactsContract.Contacts.CONTENT_URI);
    	startActivityForResult(intent, PICK_CONTACT);    	
    }

    private void clearSent()	{
        summary.setVisibility(View.INVISIBLE);
        summary2.setVisibility(View.INVISIBLE);
        tvAmount.setText("");
        tvAmount.setVisibility(View.INVISIBLE);
        tvAmountBis.setText("");
        tvAmountBis.setVisibility(View.INVISIBLE);
        tvArrow.setText("");
        tvArrow.setVisibility(View.INVISIBLE);
        tvAddress.setText("");
        tvAddress.setVisibility(View.INVISIBLE);
        tvAddressBis.setText("");
        tvAddressBis.setVisibility(View.INVISIBLE);

        btSend.setText("Send money");
        btSend.setVisibility(View.INVISIBLE);

        ivCheck.setVisibility(View.GONE);
        tvSentPrompt.setVisibility(View.GONE);
    }

	private void updateView()
	{
		/*
		String address = getToAddress();

		if (receivingAddressView.getText().toString().trim().length() == 0 || address != null) {
			receivingAddressErrorView.setVisibility(View.GONE);	
		} else {
			receivingAddressErrorView.setVisibility(View.VISIBLE);
		}

		final BigInteger amount = amountView.getAmount();
		final boolean validAmount = amount != null && amount.signum() > 0;

		MyRemoteWallet.State state = application.getRemoteWallet().getState();
		receivingAddressView.setEnabled(state == MyRemoteWallet.State.INPUT);

		amountView.setEnabled(state == MyRemoteWallet.State.INPUT);

		viewGo.setEnabled(state == MyRemoteWallet.State.INPUT && address != null && validAmount);
		if (state == MyRemoteWallet.State.INPUT)
			viewGo.setText(R.string.send_coins_fragment_button_send);
		else if (state == MyRemoteWallet.State.SENDING)
			viewGo.setText(R.string.send_coins_sending_msg);
		else if (state == MyRemoteWallet.State.SENT)
			viewGo.setText(R.string.send_coins_sent_msg);

		viewCancel.setEnabled(state != MyRemoteWallet.State.SENDING);
		viewCancel.setText(state != MyRemoteWallet.State.SENT ? R.string.button_cancel : R.string.send_coins_fragment_button_back);
		*/
	}
	
    private void doSelectInternationalPrefix()	{
		Intent intent = new Intent(getActivity(), InternationalPrefixActivity.class);
    	startActivityForResult(intent, SELECT_INTL_PREFIX);
    }
    
    private void doScanInput(String address)	{
        if(BitcoinAddressCheck.isValidAddress(address)) {
    		Toast.makeText(getActivity(), "is valid address", Toast.LENGTH_SHORT).show();

    		if(isMagic) {
    			removeMagicList();
    		}

            InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(edAddress.getWindowToken(), 0);
            imm.hideSoftInputFromWindow(edAmount1.getWindowToken(), 0);

            edAddress.setText(address);

            edAmount1.requestFocus();
            edAmount1.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
            imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
        }
        else if(BitcoinAddressCheck.isUri(address)) {
    		Toast.makeText(getActivity(), "is uri", Toast.LENGTH_SHORT).show();

    		if(isMagic) {
    			removeMagicList();
    		}

            String btc_address = BitcoinAddressCheck.getAddress(address);
            String btc_amount = BitcoinAddressCheck.getAmount(address);
            
            edAddress.setText(btc_address);
            InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(edAddress.getWindowToken(), 0);
            imm.hideSoftInputFromWindow(edAmount1.getWindowToken(), 0);

            edAmount1.requestFocus();
            edAmount1.setText(Double.toString(Double.parseDouble(btc_amount) / 100000000.0));
            edAmount1.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
            imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);

            isBTC = true;
    	    tvCurrency.setTypeface(TypefaceUtil.getInstance(getActivity()).getBTCTypeface());
    		tvCurrency.setText(Character.toString((char)TypefaceUtil.getInstance(getActivity()).getBTCSymbol()));
            
        }
        else {
    		Toast.makeText(getActivity(), "not processed", Toast.LENGTH_SHORT).show();
        }
    }

}

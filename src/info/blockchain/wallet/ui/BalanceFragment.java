package info.blockchain.wallet.ui;

import java.math.BigInteger;
import java.text.DecimalFormat;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONObject;

import piuk.EventListeners;
import piuk.MyRemoteWallet;
import piuk.MyTransaction;
import piuk.MyTransactionInput;
import piuk.blockchain.android.WalletApplication;
import piuk.blockchain.android.util.WalletUtils;

import com.google.bitcoin.core.Address;
import com.google.bitcoin.core.ScriptException;
import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.core.TransactionInput;
import com.google.bitcoin.core.TransactionOutput;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.android.Contents;
import com.google.zxing.client.android.encode.QRCodeEncoder;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Bitmap;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Gravity;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.AdapterView;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.view.View.OnTouchListener;
import android.view.animation.AnimationUtils;
import android.view.animation.Animation;
import android.util.Log;

@SuppressLint("NewApi")
public class BalanceFragment extends Fragment   {

	private View rootView = null;
	private LinearLayout balanceLayout = null;
	private LinearLayout balance_extLayout = null;
	private LinearLayout balance_extHiddenLayout = null;
	private TextView tViewCurrencySymbol = null;
	private TextView tViewAmount1 = null;
	private TextView tViewAmount2 = null;
	private ListView txList = null;
	private Animation slideUp = null;
	private Animation slideDown = null;
	private boolean isSwipedDown = false;
//    private Typeface btc_font = null;
    private String[] addressLabels = null;
    private boolean[] addressLabelTxsDisplayed = null;
    private String[] addressAmounts = null;
	private TransactionAdapter adapter = null;
	private boolean isBTC = true;
	//private Map<String, List<TxBitmap>> address2TxBitmapList;	
	private BigInteger totalInputsValue = BigInteger.ZERO;
	private BigInteger totalOutputsValue = BigInteger.ZERO;

	private WalletApplication application;

	private EventListeners.EventListener eventListener = new EventListeners.EventListener() {
		@Override
		public String getDescription() {
			setAdapterContent();
			return "Wallet Balance Listener";
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
		
		@Override
		public void onCurrencyChanged() {
			setAdapterContent();
		};
	};

	public void setAdapterContent() {
		if (application == null)
			return;
		MyRemoteWallet remoteWallet = application.getRemoteWallet();
		if (remoteWallet == null) {
			return;
		}

		addressLabels = remoteWallet.getActiveAddresses();
		if (addressLabels == null)
			return;

		addressAmounts = new String[addressLabels.length];
		addressLabelTxsDisplayed = new boolean[addressLabels.length];
		for (int i = 0; i < addressLabelTxsDisplayed.length; i++) {
			addressLabelTxsDisplayed[i] = false;
		}		

		Map<String, String> labelMap = remoteWallet.getLabelMap();

		Map<String, JSONObject> multiAddrBalancesRoot = remoteWallet.getMultiAddrBalancesRoot();
		for (int i = 0; i < addressLabels.length; i++) {
			String address = addressLabels[i];

		    if (multiAddrBalancesRoot != null) {
			    JSONObject addressRoot = multiAddrBalancesRoot.get(address);	    
			    BigInteger finalBalance = BigInteger.valueOf(((Number)addressRoot.get("final_balance")).longValue());			    
			    addressAmounts[i] = BlockchainUtil.formatBitcoin(finalBalance);
		    }

		    String label = labelMap.get(address);
		    if (label != null) {
		    	addressLabels[i] = label;	
		    }  		   		    
	    }

		String[] activeAddresses = remoteWallet.getActiveAddresses();
	    final List<MyTransaction> transactionsList = remoteWallet.getTransactions();
	    for (final MyTransaction transaction : transactionsList) {
		    Log.d("transactionHash: ", transaction.getHashAsString());
		    BigInteger result = transaction.getResult();
	    	List<TransactionOutput> transactionOutputs = transaction.getOutputs();
	    	List<TransactionInput> transactionInputs = transaction.getInputs();	 

	    	for (TransactionOutput transactionOutput : transactionOutputs) {
	        	try {
	        		com.google.bitcoin.core.Script script = transactionOutput.getScriptPubKey();
	        		String addr = null;
	        		if (script != null)
	        			addr = script.getToAddress().toString();
	        		
	        		if (addr != null && Arrays.asList(activeAddresses).contains(addr)) {
	        		    totalInputsValue = totalInputsValue.add(transactionOutput.getValue());
	        		}
	            } catch (ScriptException e) {
	                e.printStackTrace();
	            } catch (Exception e) {
	                e.printStackTrace();
	            }			    		
	    	}
	    	    		
	    	for (TransactionInput transactionInput : transactionInputs) {
	        	try {
	        		Address addr = transactionInput.getFromAddress();
	        		if (addr != null && Arrays.asList(activeAddresses).contains(addr.toString())) {
		        		MyTransactionInput ti = (MyTransactionInput)transactionInput;				        			
	        		    totalOutputsValue = totalOutputsValue.add(ti.getValue());
	        		}
	            } catch (ScriptException e) {
	                e.printStackTrace();
	            } catch (Exception e) {
	                e.printStackTrace();
	            }			    		
	    	}
	    } 
	    
		BigInteger balance = remoteWallet.getBalance();
        tViewAmount1.setText(BlockchainUtil.formatBitcoin(balance));
        tViewAmount2.setText("$" + BlockchainUtil.BTC2Fiat(WalletUtils.formatValue(balance)));
    }
	
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		final Activity activity = getActivity();
		application = (WalletApplication) activity.getApplication();

        rootView = inflater.inflate(info.blockchain.wallet.ui.R.layout.fragment_balance, container, false);
        
        slideUp = AnimationUtils.loadAnimation(getActivity().getApplicationContext(), R.anim.slide_up);
        slideDown = AnimationUtils.loadAnimation(getActivity().getApplicationContext(), R.anim.slide_down);
        
//        btc_font = TypefaceUtil.getInstance(getActivity()).getBTCTypeface();
//        btc_bold_font = TypefaceUtil.getInstance(getActivity()).getBTCBoldTypeface();

        tViewCurrencySymbol = (TextView)rootView.findViewById(R.id.currency_symbol);
        tViewCurrencySymbol.setTypeface(TypefaceUtil.getInstance(getActivity()).getBTCTypeface());
        tViewCurrencySymbol.setText(Character.toString((char)TypefaceUtil.getInstance(getActivity()).getBTCSymbol()));
        tViewCurrencySymbol.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            	if(isBTC) {
            		tViewCurrencySymbol.setText("$");
            		String tmp = tViewAmount1.getText().toString(); 
            		tViewAmount1.setText(tViewAmount2.getText().toString().substring(1));
            		tViewAmount2.setTypeface(TypefaceUtil.getInstance(getActivity()).getBTCTypeface());
                    tViewAmount2.setText(Character.toString((char)TypefaceUtil.getInstance(getActivity()).getBTCSymbol()) + tmp);
            	}
            	else {
                    tViewCurrencySymbol.setText(Character.toString((char)TypefaceUtil.getInstance(getActivity()).getBTCSymbol()));
            		String tmp = tViewAmount1.getText().toString(); 
                    tViewAmount1.setText(tViewAmount2.getText().toString().substring(1));
                    tViewAmount2.setText("$" + tmp);
            	}
            	isBTC = isBTC ? false : true;

            	adapter.notifyDataSetChanged();
            }
        });

        tViewAmount1 = (TextView)rootView.findViewById(R.id.amount1);
        tViewAmount1.setTypeface(TypefaceUtil.getInstance(getActivity()).getGravityBoldTypeface());
        tViewAmount1.setText("0");

        tViewAmount2 = (TextView)rootView.findViewById(R.id.amount2);
        tViewAmount2.setTypeface(TypefaceUtil.getInstance(getActivity()).getGravityBoldTypeface());
        tViewAmount2.setText("$" + BlockchainUtil.BTC2Fiat("0"));

        txList = (ListView)rootView.findViewById(R.id.txList);

        adapter = new TransactionAdapter();
        txList.setAdapter(adapter);
        txList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, final View view, int position, long id) {
    	    	final LinearLayout balance_extLayout = (LinearLayout)view.findViewById(R.id.balance_ext);
    	    	final LinearLayout balance_extHiddenLayout = (LinearLayout)view.findViewById(R.id.balance_ext_hidden);

    	    	if(balance_extHiddenLayout.getVisibility() == View.VISIBLE) {
    	    		
    	    		addressLabelTxsDisplayed[position] = false;

    	    		if(balance_extHiddenLayout.getChildCount() > 1) {
        		        balance_extHiddenLayout.removeViews(1, balance_extHiddenLayout.getChildCount() - 1);
    	    		}

    	            ((ImageView)view.findViewById(R.id.address_type)).setImageResource(R.drawable.address_inactive);

    		        balance_extLayout.startAnimation(slideUp);
    		        balance_extLayout.setVisibility(View.GONE);
    		        balance_extHiddenLayout.setVisibility(View.GONE);
    		    	System.gc();
    	    	}
    	    	else {
    	    		addressLabelTxsDisplayed[position] = true;
    	    		
    	            ((ImageView)view.findViewById(R.id.address_type)).setImageResource(R.drawable.address_active);

    	        	System.gc();
    	    		doDisplaySubList(view, position);
    	    	}
            }
        });
//	    txList.setDivider(getActivity().getResources().getDrawable(R.drawable.list_divider));

        balance_extHiddenLayout = (LinearLayout)rootView.findViewById(R.id.balance_ext_hidden);
        balance_extHiddenLayout.setVisibility(View.GONE);

        balanceLayout = (LinearLayout)rootView.findViewById(R.id.balance);
		balanceLayout.setOnTouchListener(new OnSwipeTouchListener(getActivity()) {
		    public void onSwipeBottom() {
		    	if(!isSwipedDown) {
		    		isSwipedDown = true;
		    		
//			        Toast.makeText(BalanceFragment.this.getActivity(), "bottom", Toast.LENGTH_SHORT).show();
			        balance_extHiddenLayout.setVisibility(View.VISIBLE);
			        balance_extLayout.setVisibility(View.VISIBLE);
			        balance_extLayout.startAnimation(slideDown);
			        
	    	        ((LinearLayout)rootView.findViewById(R.id.divider)).setVisibility(View.GONE);
	    	        
	    	        LinearLayout progression_sent = ((LinearLayout)balance_extLayout.findViewById(R.id.progression_sent));
	    	        ((TextView)progression_sent.findViewById(R.id.total_type)).setTypeface(TypefaceUtil.getInstance(getActivity()).getRobotoTypeface());
	    	        ((TextView)progression_sent.findViewById(R.id.total_type)).setTextColor(Color.BLACK);
	    	        ((TextView)progression_sent.findViewById(R.id.total_type)).setText("Total Sent");
	    	        ((TextView)progression_sent.findViewById(R.id.amount)).setTypeface(TypefaceUtil.getInstance(getActivity()).getRobotoTypeface());
	    	        ((TextView)progression_sent.findViewById(R.id.amount)).setTextColor(Color.BLACK);	    	        
	    	        ((TextView)progression_sent.findViewById(R.id.amount)).setText(BlockchainUtil.formatBitcoin(totalOutputsValue) + " BTC");	    	        
	    	        ((ProgressBar)progression_sent.findViewById(R.id.bar)).setMax(100);

	    	        LinearLayout progression_received = ((LinearLayout)balance_extLayout.findViewById(R.id.progression_received));
	    	        ((TextView)progression_received.findViewById(R.id.total_type)).setTypeface(TypefaceUtil.getInstance(getActivity()).getRobotoTypeface());
	    	        ((TextView)progression_received.findViewById(R.id.total_type)).setTextColor(Color.BLACK);
	    	        ((TextView)progression_received.findViewById(R.id.total_type)).setText("Total Received");
	    	        ((TextView)progression_received.findViewById(R.id.amount)).setTypeface(TypefaceUtil.getInstance(getActivity()).getRobotoTypeface());
	    	        ((TextView)progression_received.findViewById(R.id.amount)).setTextColor(Color.BLACK);
	    	        ((TextView)progression_received.findViewById(R.id.amount)).setText(BlockchainUtil.formatBitcoin(totalInputsValue) + " BTC");
	    	        ((ProgressBar)progression_received.findViewById(R.id.bar)).setMax(100);

	    	        if (totalOutputsValue.doubleValue() > 0 || totalInputsValue.doubleValue() > 0) {        	
	    	            ((ProgressBar)progression_sent.findViewById(R.id.bar)).setProgress((int)((totalOutputsValue.doubleValue() / (totalOutputsValue.doubleValue() + totalInputsValue.doubleValue())) * 100));
		    	        ((ProgressBar)progression_sent.findViewById(R.id.bar)).setProgressDrawable(getResources().getDrawable(R.drawable.progress_red));
	    	            ((ProgressBar)progression_received.findViewById(R.id.bar)).setProgress((int)((totalInputsValue.doubleValue() / (totalOutputsValue.doubleValue() + totalInputsValue.doubleValue())) * 100));
		    	        ((ProgressBar)progression_received.findViewById(R.id.bar)).setProgressDrawable(getResources().getDrawable(R.drawable.progress_green));
	    	        }
		    	}
		    }

		});

        balance_extLayout = (LinearLayout)rootView.findViewById(R.id.balance_ext);
		balance_extLayout.setOnTouchListener(new OnSwipeTouchListener(getActivity()) {
		    public void onSwipeTop() {
		    	isSwipedDown = false;

    	        ((LinearLayout)rootView.findViewById(R.id.divider)).setVisibility(View.VISIBLE);

//		        Toast.makeText(BalanceFragment.this.getActivity(), "top", Toast.LENGTH_SHORT).show();
		        balance_extLayout.startAnimation(slideUp);
		        balance_extLayout.setVisibility(View.GONE);
		        balance_extHiddenLayout.setVisibility(View.GONE);
		    }
		});
        balance_extLayout.setVisibility(View.GONE);

		EventListeners.addEventListener(eventListener);

        return rootView;
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);

        if(isVisibleToUser) {
        	System.gc();
        }
        else {
        	;
        }
    }

    @Override
    public void onResume() {
    	super.onResume();

    	System.gc();

    }

	@Override
	public void onDestroy() {
		super.onDestroy();

		EventListeners.removeEventListener(eventListener);
	}

    private class TransactionAdapter extends BaseAdapter {
    	
		private LayoutInflater inflater = null;

	    TransactionAdapter() {
	        inflater = (LayoutInflater)getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}

		@Override
		public int getCount() {
			if(addressLabels != null) {
				return addressLabels.length;
			}
			else {
				return 0;
			}
		}

		@Override
		public String getItem(int position) {
	        return addressLabels[position];
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			
			Log.d("List refresh", "" + position);

			View view;
	        
	        if (convertView == null) {
	            view = inflater.inflate(R.layout.txs_layout, parent, false);
	        } else {
	            view = convertView;
	        }
	        
	    	LinearLayout balance_extLayout = (LinearLayout)view.findViewById(R.id.balance_ext);
	    	LinearLayout balance_extHiddenLayout = (LinearLayout)view.findViewById(R.id.balance_ext_hidden);
	        balance_extLayout.setVisibility(View.GONE);
	        balance_extHiddenLayout.setVisibility(View.GONE);

	        String amount = null;
	        DecimalFormat df = null;
	        if(isBTC) {
	        	df = new DecimalFormat("######0.0000");
	        	amount = df.format(Double.parseDouble(addressAmounts[position]));
	        }
	        else {
//	        	df = new DecimalFormat("######0.00");
	        	amount = BlockchainUtil.BTC2Fiat(addressAmounts[position]);
	        }

	        ((TextView)view.findViewById(R.id.address)).setTypeface(TypefaceUtil.getInstance(getActivity()).getGravityBoldTypeface());
	        ((TextView)view.findViewById(R.id.address)).setText(addressLabels[position].length() > 22 ? addressLabels[position].substring(0, 22) + "..." : addressLabels[position]);
	        ((TextView)view.findViewById(R.id.amount)).setTypeface(TypefaceUtil.getInstance(getActivity()).getRobotoBoldTypeface());
	        ((TextView)view.findViewById(R.id.amount)).setText(amount);
	        ((TextView)view.findViewById(R.id.currency_code)).setText(isBTC ? "BTC" : "USD");
	        
	        if(addressLabelTxsDisplayed[position]) {
				Log.d("List refresh sub", "" + position);
		    	System.gc();
		        doDisplaySubList(view, position);
	        }

	        return view;
		}

    }

    public void doDisplaySubList(final View view, int position) {
    	final LinearLayout balance_extLayout = (LinearLayout)view.findViewById(R.id.balance_ext);
    	final LinearLayout balance_extHiddenLayout = (LinearLayout)view.findViewById(R.id.balance_ext_hidden);

    	MyRemoteWallet remoteWallet = application.getRemoteWallet();
		if (remoteWallet == null) {
			return;
		}

	    final String[] activeAddresses = remoteWallet.getActiveAddresses();
    	final String address = activeAddresses[position];

    	ImageView qr_icon = ((ImageView)balance_extLayout.findViewById(R.id.balance_qr_icon));
        qr_icon.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

      			android.content.ClipboardManager clipboard = (android.content.ClipboardManager)getActivity().getSystemService(android.content.Context.CLIPBOARD_SERVICE);
      		    android.content.ClipData clip = android.content.ClipData.newPlainText("Address", address);
      		    clipboard.setPrimaryClip(clip);
     			Toast.makeText(getActivity(), "Address copied to clipboard:" + address, Toast.LENGTH_LONG).show();

     			Intent intent = new Intent(getActivity(), QRActivity.class);
      	        intent.putExtra("BTC_ADDRESS", address);
      	        startActivity(intent);

                return true;
            }
        });

		final Map<String, JSONObject> multiAddrBalancesRoot = remoteWallet.getMultiAddrBalancesRoot();

		final JSONObject addressRoot = multiAddrBalancesRoot.get(address);
	    final BigInteger totalReceived = BigInteger.valueOf(((Number)addressRoot.get("total_received")).longValue());
	    final BigInteger totalSent = BigInteger.valueOf(((Number)addressRoot.get("total_sent")).longValue());
	    Log.d("totalReceived: ", "totalReceived: " + totalReceived);

	    Log.d("totalSent: ", "totalSent: " + totalSent);
        LinearLayout progression_sent = ((LinearLayout)balance_extLayout.findViewById(R.id.progression_sent));
        ((TextView)progression_sent.findViewById(R.id.total_type)).setTypeface(TypefaceUtil.getInstance(getActivity()).getRobotoTypeface());
        ((TextView)progression_sent.findViewById(R.id.total_type)).setTextColor(0xFF9b9b9b);
        ((TextView)progression_sent.findViewById(R.id.total_type)).setText("SENT");
        ((TextView)progression_sent.findViewById(R.id.amount)).setTypeface(TypefaceUtil.getInstance(getActivity()).getRobotoTypeface());
        ((TextView)progression_sent.findViewById(R.id.amount)).setTextColor(0xFF9b9b9b);
        ((TextView)progression_sent.findViewById(R.id.amount)).setText(BlockchainUtil.formatBitcoin(totalSent) + " BTC");
        ((ProgressBar)progression_sent.findViewById(R.id.bar)).setMax(100);

        LinearLayout progression_received = ((LinearLayout)balance_extLayout.findViewById(R.id.progression_received));
        ((TextView)progression_received.findViewById(R.id.total_type)).setTypeface(TypefaceUtil.getInstance(getActivity()).getRobotoTypeface());
        ((TextView)progression_received.findViewById(R.id.total_type)).setTextColor(0xFF9b9b9b);
        ((TextView)progression_received.findViewById(R.id.total_type)).setText("RECEIVED");
        ((TextView)progression_received.findViewById(R.id.amount)).setTypeface(TypefaceUtil.getInstance(getActivity()).getRobotoTypeface());
        ((TextView)progression_received.findViewById(R.id.amount)).setTextColor(0xFF9b9b9b);
        ((TextView)progression_received.findViewById(R.id.amount)).setText(BlockchainUtil.formatBitcoin(totalReceived) + " BTC");
        ((ProgressBar)progression_received.findViewById(R.id.bar)).setMax(100);

        if (totalSent.doubleValue() > 0 || totalReceived.doubleValue() > 0) {        	
            ((ProgressBar)progression_sent.findViewById(R.id.bar)).setProgress((int)((totalSent.doubleValue() / (totalSent.doubleValue() + totalReceived.doubleValue())) * 100));
            ((ProgressBar)progression_sent.findViewById(R.id.bar)).setProgressDrawable(getResources().getDrawable(R.drawable.progress_red2));
            ((ProgressBar)progression_received.findViewById(R.id.bar)).setProgress((int)((totalReceived.doubleValue() / (totalSent.doubleValue() + totalReceived.doubleValue())) * 100));
            ((ProgressBar)progression_received.findViewById(R.id.bar)).setProgressDrawable(getResources().getDrawable(R.drawable.progress_green2));
        } 

		View child = null;
        LayoutInflater inflater = (LayoutInflater)getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        final List<MyTransaction> transactionsList = remoteWallet.getTransactions();

	    boolean isSending = true;
	    for (final MyTransaction transaction : transactionsList) {
		    Log.d("transactionHash: ", transaction.getHashAsString());
		    BigInteger result = transaction.getResult();
	    	List<TransactionOutput> transactionOutputs = transaction.getOutputs();
	    	List<TransactionInput> transactionInputs = transaction.getInputs();	 
		    List<Map.Entry<String, String>> addressValueEntryList = new ArrayList<Map.Entry<String, String>>();

	    	if (result.signum() == 1) {
			    isSending = false;
		    	boolean isAddressPartofTransaction = false;
		    	for (TransactionOutput transactionOutput : transactionOutputs) {
		        	try {
		        		com.google.bitcoin.core.Script script = transactionOutput.getScriptPubKey();
		        		String addr = null;
		        		if (script != null)
		        			addr = script.getToAddress().toString();
		        		
		        		if (addr != null && addr.equals(address)) {
		        			isAddressPartofTransaction = true;
		        			break;
		        		}
		            } catch (ScriptException e) {
		                e.printStackTrace();
		            } catch (Exception e) {
		                e.printStackTrace();
		            }			    		
		    	}
		    	
		    	if (transactionInputs != null && isAddressPartofTransaction) {
			    	for (TransactionInput transactionInput : transactionInputs) {
			        	try {
			        		Address addr = transactionInput.getFromAddress();
			        		if (addr != null) {
			        		    Log.d("transactionInput: ", addr.toString());
				        		MyTransactionInput ti = (MyTransactionInput)transactionInput;
			        			String value = BlockchainUtil.formatBitcoin(ti.getValue()) + " BTC";
			        			Map.Entry<String, String> entry = new AbstractMap.SimpleEntry<String, String>(addr.toString(), value);
			        			addressValueEntryList.add(entry);
			        		}
			            } catch (ScriptException e) {
			                e.printStackTrace();
			            } catch (Exception e) {
			                e.printStackTrace();
			            }
			    	}				
				}	    		
	    	} else {
	    		isSending = true;
		    	boolean isAddressPartofTransaction = false;
		    	for (TransactionInput transactionInput : transactionInputs) {
		        	try {
		        		Address addr = transactionInput.getFromAddress();

		        		if (addr != null && addr.toString().equals(address)) {
		        			isAddressPartofTransaction = true;
		        			break;
		        		}
		            } catch (ScriptException e) {
		                e.printStackTrace();
		            } catch (Exception e) {
		                e.printStackTrace();
		            }			    		
		    	}
		    	
				if (transactionOutputs != null && isAddressPartofTransaction) {
			    	for (TransactionOutput transactionOutput : transactionOutputs) {
			        	try {
			        		com.google.bitcoin.core.Script script = transactionOutput.getScriptPubKey();
			        		Address addr = null;
			        		if (script != null)
			        			addr = script.getToAddress();
			        		
			        		if (addr != null) {
			        		    Log.d("transactionOutput: ", addr.toString());
			        			String value = BlockchainUtil.formatBitcoin(transactionOutput.getValue()) + " BTC";
			        			Map.Entry<String, String> entry = new AbstractMap.SimpleEntry<String, String>(addr.toString(), value);
			        			addressValueEntryList.add(entry);			        			
			        		}
			            } catch (ScriptException e) {
			                e.printStackTrace();
			            } catch (Exception e) {
			                e.printStackTrace();
			            }
			    	}
				}
	    	}
	    
        	if (addressValueEntryList.size() == 0)
        		continue;        	
        				
			child = inflater.inflate(R.layout.tx_layout, null);

	        ((TextView)child.findViewById(R.id.ts)).setTypeface(TypefaceUtil.getInstance(getActivity()).getGravityBoldTypeface());
	        
	        long ts = transaction.getTime().getTime()/1000;
	        ((TextView)child.findViewById(R.id.ts)).setText(DateUtil.getInstance().formatted(ts));

	        if (isSending) {
		        TxBitmap txBitmap = new TxBitmap(getActivity(), result, addressValueEntryList);
		        ((ImageView)child.findViewById(R.id.txbitmap)).setImageBitmap(txBitmap.createArrowsBitmap(200, TxBitmap.SENDING, addressValueEntryList.size()));
		        ((ImageView)child.findViewById(R.id.address)).setImageBitmap(txBitmap.createListBitmap(200));
		        ((TextView)child.findViewById(R.id.amount)).setTypeface(TypefaceUtil.getInstance(getActivity()).getGravityBoldTypeface());
		        ((TextView)child.findViewById(R.id.amount)).setTextColor(BlockchainUtil.BLOCKCHAIN_RED);
	        } else {
		        TxBitmap txBitmap = new TxBitmap(getActivity(), result, addressValueEntryList);
		        ((ImageView)child.findViewById(R.id.txbitmap)).setImageBitmap(txBitmap.createArrowsBitmap(200, TxBitmap.RECEIVING, addressValueEntryList.size()));
		        ((ImageView)child.findViewById(R.id.address)).setImageBitmap(txBitmap.createListBitmap(200));
		        ((TextView)child.findViewById(R.id.amount)).setTypeface(TypefaceUtil.getInstance(getActivity()).getGravityBoldTypeface());
		        ((TextView)child.findViewById(R.id.amount)).setTextColor(BlockchainUtil.BLOCKCHAIN_GREEN);
	        }
	        
	        if(isBTC) {
		        ((TextView)child.findViewById(R.id.amount)).setText(BlockchainUtil.formatBitcoin(result) + " BTC");
	        }
	        else {
		        ((TextView)child.findViewById(R.id.amount)).setText((BlockchainUtil.BTC2Fiat(WalletUtils.formatValue(result)) + " USD"));
	        }
	        
			child.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(Intent.ACTION_VIEW , Uri.parse("https://blockchain.info/tx/"+transaction.getHashAsString()));
                    startActivity(intent);
                }
            });

			balance_extHiddenLayout.addView(child);
		}

		balance_extHiddenLayout.setVisibility(View.VISIBLE);
        balance_extLayout.setVisibility(View.VISIBLE);
//        balance_extLayout.startAnimation(slideDown);
    }

    private Bitmap generateQRCode(String uri) {

        Bitmap bitmap = null;
        int qrCodeDimension = 380;

        QRCodeEncoder qrCodeEncoder = new QRCodeEncoder(uri, null, Contents.Type.TEXT, BarcodeFormat.QR_CODE.toString(), qrCodeDimension);

    	try {
            bitmap = qrCodeEncoder.encodeAsBitmap();
        } catch (WriterException e) {
            e.printStackTrace();
        }
    	
    	return bitmap;
    }

}

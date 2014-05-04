package info.blockchain.wallet.ui;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashMap;

import net.sourceforge.zbar.Symbol;

import piuk.MyRemoteWallet;
import piuk.blockchain.android.WalletApplication;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
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
import android.view.View.OnTouchListener;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
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
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.database.Cursor;
import android.util.Log;

import com.dm.zbar.android.scanner.ZBarConstants;
import com.dm.zbar.android.scanner.ZBarScannerActivity;
import com.google.bitcoin.core.Utils;
import com.google.bitcoin.uri.BitcoinURI;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.android.Contents;
import com.google.zxing.client.android.encode.QRCodeEncoder;

public class SendFragment extends Fragment   {
	
	private static int SIMPLE_SEND = 1;
	private static int CUSTOM_SEND = 2;
	private static int SHARED_SEND = 3;

	private static int PICK_CONTACT = 10;

	private static int CURRENT_SEND = SIMPLE_SEND;
	
	private LinearLayout lastSendingAddress = null;
	
	private boolean addressesOn = true;
	private boolean contactsOn = true;
	private boolean phoneContactsOn = true;
	
	private View rootView = null;

    private EditText edAmount1 = null;
    private TextView tvAmount2 = null;
    private EditText edAddress = null;
    private TextView tvCurrency = null;
    private LinearLayout summary = null;
    
    private TextView tvAmount = null;
    private TextView tvAmountBis = null;
    private TextView tvArrow = null;
    private TextView tvAddress = null;

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
    
    private HashMap<String,String> magicData = null;
    private ArrayList<String> keys = null;
	private MagicAdapter adapter = null;

	private boolean isBTC = false;

	private WalletApplication application;

	private static int ZBAR_SCANNER_REQUEST = 2026;

    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		final Activity activity = getActivity();
		application = (WalletApplication) activity.getApplication();

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

        summary = (LinearLayout)rootView.findViewById(R.id.summary);
        summary.setVisibility(View.INVISIBLE);

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
            		tvCurrency.setText("$");
            		String tmp = edAmount1.getText().toString();
            		if(tmp.length() < 1) {
            			tmp = "0.00";
            		}
//            		edAmount1.setText(tvAmount2.getText().toString().substring(1));
            		edAmount1.setText(tvAmount2.getText().toString().substring(0, tvAmount2.getText().toString().length() - 4));
//            		tvAmount2.setTypeface(TypefaceUtil.getInstance(getActivity()).getBTCTypeface());
                    tvAmount2.setText(tmp + " BTC");
            	}
            	else {
            	    tvCurrency.setTypeface(TypefaceUtil.getInstance(getActivity()).getBTCTypeface());
            		tvCurrency.setText(Character.toString((char)TypefaceUtil.getInstance(getActivity()).getBTCSymbol()));
            		String tmp = edAmount1.getText().toString(); 
            		if(tmp.length() < 1) {
            			tmp = "0.00";
            		}
                    edAmount1.setText(tvAmount2.getText().toString().substring(0, tvAmount2.getText().toString().length() - 4));
                    tvAmount2.setText(tmp + " USD");
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
        ((TextView)rootView.findViewById(R.id.currency)).setText("$");
        ((TextView)rootView.findViewById(R.id.currency)).setTypeface(TypefaceUtil.getInstance(getActivity()).getGravityBoldTypeface());

        initMagicList();

        btSend = ((Button)rootView.findViewById(R.id.send));
        btSend.setVisibility(View.INVISIBLE);
        btSend.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                BigInteger amount = BigInteger.ZERO;
                if (isBTC) {
                	amount = bitcoinAmountStringToBigInteger(edAmount1.getText().toString().trim());
                } else {
                	String btcAmount = BlockchainUtil.Fiat2BTC(edAmount1.getText().toString());
                	amount = bitcoinAmountStringToBigInteger(btcAmount);
                }
                Log.d("edAmount1", "edAmount1" + edAmount1.getText().toString());
                String address = edAddress.getText().toString();                
                if (amount != null) {
                    Log.d("amount", "amount" + "amountamount: " + amount);
    				MyRemoteWallet wallet = application.getRemoteWallet();
    				String[] from = wallet.getActiveAddresses();
    				wallet.sendCoinsAsync(from, address, amount, MyRemoteWallet.FeePolicy.FeeOnlyIfNeeded, BigInteger.ZERO, null);

                	edAddress.setText("");
                	edAmount1.setText("");
                	tvAmount2.setText("");

            		Toast.makeText(getActivity(), "Send", Toast.LENGTH_SHORT).show();
		        	summary.setVisibility(View.INVISIBLE);
            		/*
            		btSend.setTextColor(BlockchainUtil.BLOCKCHAIN_GREEN);
            		btSend.setText(Character.toString((char)0x2713));
    	        	btSend.setClickable(false);
    	        	*/
            		btSend.setVisibility(View.GONE);
                    ivCheck.setVisibility(View.VISIBLE);
                    tvSentPrompt.setVisibility(View.VISIBLE);
                                    	
                }
            }
        });

        tvAmount2 = ((TextView)rootView.findViewById(R.id.amount2));
        tvAmount2.setText("0.00 USD");
        edAmount1 = ((EditText)rootView.findViewById(R.id.amount1));
        edAmount1.setOnEditorActionListener(new OnEditorActionListener() {
		    @Override
		    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
		        if(actionId == EditorInfo.IME_ACTION_DONE) {
		        	
		        	summary.setVisibility(View.VISIBLE);
		        	tvAddress.setVisibility(View.VISIBLE);
		        	tvArrow.setVisibility(View.VISIBLE);
		        	tvAmount.setVisibility(View.VISIBLE);
		        	tvAmountBis.setVisibility(View.VISIBLE);
		        	
		        	if(edAddress.getText().toString().length() > 15) {
			        	tvAddress.setText(edAddress.getText().toString().subSequence(0, 15) + "...");
		        	}
		        	else {
			        	tvAddress.setText(edAddress.getText().toString());
		        	}

		        	tvArrow.setText(Character.toString((char)0x2192));

		        	String amount1 = edAmount1.getText().toString();
		        	String amount2 = tvAmount2.getText().toString().substring(0, tvAmount2.getText().toString().length() - 4);
		        	if(isBTC) {
		        		amount1 += " BTC";
		        		amount2 += " USD";
		        	}
		        	else {
		        		amount1 += " USD";
		        		amount2 += " BTC";
		        	}
		        	SpannableStringBuilder a1 = new SpannableStringBuilder(amount1);
		        	SpannableStringBuilder a2 = new SpannableStringBuilder(amount2);
		        	a1.setSpan(new SuperscriptSpan(), amount1.length() - 4, amount1.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		        	a1.setSpan(new RelativeSizeSpan((float)0.75), amount1.length() - 4, amount1.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		        	a2.setSpan(new SuperscriptSpan(), amount2.length() - 4, amount2.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		        	a2.setSpan(new RelativeSizeSpan((float)0.75), amount2.length() - 4, amount2.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		        	tvAmount.setText(a1);
		        	tvAmountBis.setText(a2);

	            	btSend.setVisibility(View.VISIBLE);

		        }
		        return false;
		    }
		});

        edAmount1 = ((EditText)rootView.findViewById(R.id.amount1));
        edAmount1.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {

            	if(ivCheck.getVisibility() == View.VISIBLE) {
            		clearSent();
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
        		
        		String[] sKeys = magicData.keySet().toArray(new String[0]);
        		int len = edAddress.getText().length();
        		ArrayList<String> filtered = new ArrayList<String>();
        		for (int i = 0; i < sKeys.length; i++)	{
	                if (len <= sKeys[i].length())	{
	                	if(edAddress.getText().toString().equalsIgnoreCase((String)sKeys[i].subSequence(0, len))) {
	                		filtered.add(sKeys[i]);
	                	}
	                }
        		}
                keys = filtered;
                if(adapter != null)	{
            		adapter.notifyDataSetChanged();
                }
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

        edAmount1.addTextChangedListener(new TextWatcher()	{

        	public void afterTextChanged(Editable s) {
        		if((edAddress.getText().toString() != null && edAddress.getText().toString().length() > 0) || (edAmount1.getText().toString() != null && edAmount1.getText().toString().length() > 0)) {
        			
        			if(isBTC)	{
            			tvAmount2.setText(BlockchainUtil.BTC2Fiat(edAmount1.getText().toString()) + " USD");
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

        clear_input.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
            	edAddress.setText("");
            	edAmount1.setText("");
            	tvAmount2.setText("");
            	
                summary.setVisibility(View.INVISIBLE);
                tvAmount.setText("");
                tvAmount.setVisibility(View.INVISIBLE);
                tvAmountBis.setText("");
                tvAmountBis.setVisibility(View.INVISIBLE);
                tvArrow.setText("");
                tvArrow.setVisibility(View.INVISIBLE);
                tvAddress.setText("");
                tvAddress.setVisibility(View.INVISIBLE);

                btSend.setText("Send money");
                btSend.setVisibility(View.INVISIBLE);

                ivCheck.setVisibility(View.GONE);
                tvSentPrompt.setVisibility(View.GONE);

            	if(!isMagic) {
                	displayMagicList();
            	}

                return false;
            }
        });
        clear_input.setVisibility(View.INVISIBLE);

        final ImageButton imgSimpleSend = ((ImageButton)rootView.findViewById(R.id.simple));
        final ImageButton imgCustomSend = ((ImageButton)rootView.findViewById(R.id.custom));
        final ImageButton imgSharedSend = ((ImageButton)rootView.findViewById(R.id.shared));
        /*
        final TextView tvSimpleSend = ((TextView)rootView.findViewById(R.id.label_simple));
        final TextView tvCustomSend = ((TextView)rootView.findViewById(R.id.label_custom));
        final TextView tvSharedSend = ((TextView)rootView.findViewById(R.id.label_shared));
        */
        
        final int color_spend_selected = 0xff808080;
        final int color_spend_unselected = 0xffa0a0a0;
        
    	imgSimpleSend.setBackgroundColor(color_spend_selected);
    	imgCustomSend.setBackgroundColor(color_spend_unselected);
    	imgSharedSend.setBackgroundColor(color_spend_unselected);
    	/*
    	tvSimpleSend.setBackgroundColor(color_spend_selected);
    	tvCustomSend.setBackgroundColor(color_spend_unselected);
    	tvSharedSend.setBackgroundColor(color_spend_unselected);
    	*/

        imgSimpleSend.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

            	imgSimpleSend.setBackgroundColor(color_spend_selected);
            	imgCustomSend.setBackgroundColor(color_spend_unselected);
            	imgSharedSend.setBackgroundColor(color_spend_unselected);
            	/*
            	tvSimpleSend.setBackgroundColor(color_spend_selected);
            	tvCustomSend.setBackgroundColor(color_spend_unselected);
            	tvSharedSend.setBackgroundColor(color_spend_unselected);
            	*/
            	doSimpleSend();

            	/*
                switch (event.getAction())	{
                	case android.view.MotionEvent.ACTION_DOWN:
                	case android.view.MotionEvent.ACTION_MOVE:
                    	imgSimpleSend.setBackgroundColor(0xff3a3a3a);
                    	doSimpleSend();
                    	break;
                	case android.view.MotionEvent.ACTION_UP:
                	case android.view.MotionEvent.ACTION_CANCEL:
                		Log.d("QR icon", "UP or CANCEL");
                    	imgSimpleSend.setBackgroundColor(0xffF3F3F3);
                		break;
            	}
            	*/

                return true;
            }
        });

        imgCustomSend.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

            	imgSimpleSend.setBackgroundColor(color_spend_unselected);
            	imgCustomSend.setBackgroundColor(color_spend_selected);
            	imgSharedSend.setBackgroundColor(color_spend_unselected);
            	/*
            	tvSimpleSend.setBackgroundColor(color_spend_unselected);
            	tvCustomSend.setBackgroundColor(color_spend_selected);
            	tvSharedSend.setBackgroundColor(color_spend_unselected);
            	*/
//    			doCustomSend();

            	/*
                switch (event.getAction())	{
            		case android.view.MotionEvent.ACTION_DOWN:
            		case android.view.MotionEvent.ACTION_MOVE:
            			imgCustomSend.setBackgroundColor(0xff3a3a3a);
            			doCustomSend();
            			break;
            		case android.view.MotionEvent.ACTION_UP:
            		case android.view.MotionEvent.ACTION_CANCEL:
            			Log.d("QR icon", "UP or CANCEL");
            			imgCustomSend.setBackgroundColor(0xffF3F3F3);
            			break;
                	}
                */

                return true;
            }
        });

        imgSharedSend.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

            	imgSimpleSend.setBackgroundColor(color_spend_unselected);
            	imgCustomSend.setBackgroundColor(color_spend_unselected);
            	imgSharedSend.setBackgroundColor(color_spend_selected);
            	/*
            	tvSimpleSend.setBackgroundColor(color_spend_unselected);
            	tvCustomSend.setBackgroundColor(color_spend_unselected);
            	tvSharedSend.setBackgroundColor(color_spend_selected);
            	*/
    			doSharedSend();

            	/*
                switch (event.getAction())	{
            		case android.view.MotionEvent.ACTION_DOWN:
            		case android.view.MotionEvent.ACTION_MOVE:
            			imgSharedSend.setBackgroundColor(0xff3a3a3a);
            			doSharedSend();
            			break;
            		case android.view.MotionEvent.ACTION_UP:
            		case android.view.MotionEvent.ACTION_CANCEL:
            			Log.d("QR icon", "UP or CANCEL");
            			imgSharedSend.setBackgroundColor(0xffF3F3F3);
            			break;
                	}
                */

                return true;
            }
        });

        return rootView;
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
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if(resultCode == Activity.RESULT_OK && requestCode == ZBAR_SCANNER_REQUEST)	{

			String strResult = BitcoinAddressCheck.clean(data.getStringExtra(ZBarConstants.SCAN_RESULT));
//        	Log.d("Scan result", strResult);
			if(BitcoinAddressCheck.isValid(BitcoinAddressCheck.clean(strResult))) {
	            edAddress.setText(strResult);
	            
	            if(isMagic) {
	            	removeMagicList();
	            }

                InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(edAddress.getWindowToken(), 0);
                edAmount1.requestFocus();
                edAmount1.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
                imm.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, 0);

			}
			else {
				Toast.makeText(getActivity(), "Invalid address", Toast.LENGTH_LONG).show();
			}

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
		                    
		                    if(strEmail != null || strNumber != null)
		                    {
		                    	//
		                    	// add hooks here
		                    	//
		    	        		Toast.makeText(getActivity(), "Name:" + strName + ",Email:" + strEmail + ",Number:" + strNumber, Toast.LENGTH_SHORT).show();
		                    }
		                    else
		                    {
		                    	// inform user that an email or a cell no. is needed
		                    }

		                }
		    	    }
		    	    finally
		    	    {
		    	        cur.close();
		    	    }
		        
		        }
		    }

		}
		else {
			;
		}
		
	}

	public BigInteger bitcoinAmountStringToBigInteger(String amount) {
		if (isValidAmount(amount))
			return Utils.toNanoCoins(amount);
		else
			return null;
	}
	
	private boolean isValidAmount(String amount) {
		try {
			if (amount.length() > 0) {
				final BigInteger nanoCoins = Utils.toNanoCoins(amount);
				if (nanoCoins.signum() >= 0)
					return true;
			}
		} catch (final Exception x) {
		}

		return false;
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
			if(keys != null) {
				return keys.size();
			}
			else {
				return 0;
			}
		}

		@Override
		public String getItem(int position) {
			return keys.get(position);
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
	        if(!keys.get(position).startsWith("1Z")) {
		        ((TextView)view.findViewById(R.id.p1)).setTypeface(TypefaceUtil.getInstance(getActivity()).getGravityBoldTypeface());
	        }
	        else {
		        ((TextView)view.findViewById(R.id.p1)).setTypeface(TypefaceUtil.getInstance(getActivity()).getGravityLightTypeface());
	        }

	        // Names, Labels in black, addresses in GREY
	        if(!keys.get(position).startsWith("1Z")) {
		        ((TextView)view.findViewById(R.id.p1)).setTextColor(Color.BLACK);
	        }
	        else {
		        ((TextView)view.findViewById(R.id.p1)).setTextColor(0xFF616161);
	        }
	        ((TextView)view.findViewById(R.id.p1)).setText(keys.get(position));
	        ((TextView)view.findViewById(R.id.p2)).setText(magicData.get(keys.get(position)));

	        return view;
		}

    }

    private void initMagicList() {

    	/*
        magicData = new HashMap<String,String>();
        
        if(addressesOn) {
            magicData.put("Cold Storage", "11 BTC");
            magicData.put("1Zs4532d76HB...", "0.3 BTC");
            magicData.put("Walk around money", "0.3 BTC");
    	}
    	else {
            magicData.put("Alice", "1Zs4532d76HB...");
            magicData.put("Bob", "18Uj9vBd76HB...");
            magicData.put("Kebab shop guy", "1Wx55328k4B...");
    	}
        
        String[] sKeys = magicData.keySet().toArray(new String[0]);
        keys = new ArrayList<String>(Arrays.asList(sKeys));
        */
    	
		final WalletApplication application = (WalletApplication)getActivity().getApplication();
		MyRemoteWallet wallet = application.getRemoteWallet();
		String[] from = wallet.getActiveAddresses();

        magicData = new HashMap<String,String>();
        
        for(int i = 0; i < from.length; i++) {
        	magicData.put(from[i], "0.000 BTC");
        }

        String[] sKeys = magicData.keySet().toArray(new String[0]);
        keys = new ArrayList<String>(Arrays.asList(sKeys));

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
        ivAddresses.setBackgroundColor(colorOn);
        ivContacts = (ImageView)childIcons.findViewById(R.id.contacts);
        ivContacts.setImageResource(R.drawable.address_book);
        ivContacts.setBackgroundColor(colorOff);
        ivPhoneContacts = (ImageView)childIcons.findViewById(R.id.phone_contacts);
        ivPhoneContacts.setImageResource(R.drawable.phone_contacts);
        ivPhoneContacts.setBackgroundColor(colorOff);
        addressesOn = true;
        contactsOn = false;
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
            		initMagicList();
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
//            		initMagicList();
            		if(isMagic) {
            			removeMagicList();
            		}
            		doSend2Friends();
//            		adapter.notifyDataSetChanged();                            		
                }
        });

        final ImageView qr_scan = (ImageView)childIcons.findViewById(R.id.qr_icon);
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

                return true;
            }
        });
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
                Toast.makeText(getActivity(), keys.get(position), Toast.LENGTH_SHORT).show();
                InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(edAddress.getWindowToken(), 0);
                edAddress.setText(keys.get(position));
                removeMagicList();
                edAmount1.requestFocus();
                edAmount1.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
                imm.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, 0);
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
        ((TextView)rootView.findViewById(R.id.currency)).setText("$");
        
        if(parent != null) {
            parent.removeViews(parent.getChildCount() - children, children);
            children = 0;
            oldView.setVisibility(View.VISIBLE);
        }

        initMagicList();
    }

    private void doSimpleSend() {
    	simple_spend.setVisibility(View.VISIBLE);
    	custom_spend.setVisibility(View.GONE);
        LinearLayout container = ((LinearLayout)rootView.findViewById(R.id.send_container));
        sendViewToBack(container);
    	CURRENT_SEND = SIMPLE_SEND;
    }

    private void doCustomSend() {
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
    	
        TextView tvSendingAddress = new TextView(getActivity());
        tvSendingAddress.setText("Walking around money");
        tvSendingAddress.setTextSize(16);
        tvSendingAddress.setPadding(5, 5, 5, 5);
        tvSendingAddress.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
        layout_params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        tvSendingAddress.setLayoutParams(layout_params);
    	((LinearLayout)layout_from.findViewById(R.id.p2)).setGravity(Gravity.LEFT|Gravity.CENTER_VERTICAL);
    	((LinearLayout)layout_from.findViewById(R.id.p2)).addView(tvSendingAddress);

    	EditText edAmount = new EditText(getActivity());
        edAmount.setText("0.00");
        edAmount.setTextSize(16);
        edAmount.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        edAmount.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        edAmount.setTextColor(BlockchainUtil.BLOCKCHAIN_RED);
        edAmount.setLayoutParams(layout_params);
    	((LinearLayout)layout_from.findViewById(R.id.p3)).setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
    	((LinearLayout)layout_from.findViewById(R.id.p3)).addView(edAmount);
    	
    	ImageButton ibPlus = new ImageButton(getActivity());
    	ibPlus.setImageResource(R.drawable.plus_icon);
    	((LinearLayout)layout_from.findViewById(R.id.plus)).addView(ibPlus);
        ibPlus.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
            	addSendingAddress();
            }
        });

    	((LinearLayout)layout_custom_spend.findViewById(R.id.froms)).addView(layout_from);
    	lastSendingAddress = layout_from;

        //
        // 'FEE' layout
        //
        TextView tvFee = new TextView(getActivity());
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
    	EditText edFee = new EditText(getActivity());
        edFee.setText("0.005");
        edFee.setTextSize(16);
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
        tvSendingAddress.setText("Lukewarm storage");
        tvSendingAddress.setTextSize(16);
        tvSendingAddress.setPadding(5, 5, 5, 5);
        tvSendingAddress.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
        layout_params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        tvSendingAddress.setLayoutParams(layout_params);
    	((LinearLayout)layout_from2.findViewById(R.id.p2)).setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
    	((LinearLayout)layout_from2.findViewById(R.id.p2)).addView(tvSendingAddress);

    	EditText edAmount = new EditText(getActivity());
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

    private void doSharedSend() {
    	CURRENT_SEND = SHARED_SEND;
    }

    private void doSend2Friends()	{
    	Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
//    	intent.setType(ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE);
//    	intent.setType(ContactsContract.CommonDataKinds.Email.CONTENT_TYPE);
    	intent.setData(ContactsContract.Contacts.CONTENT_URI);
    	startActivityForResult(intent, PICK_CONTACT);
    }

    private void clearSent()	{
        summary.setVisibility(View.INVISIBLE);
        tvAmount.setText("");
        tvAmount.setVisibility(View.INVISIBLE);
        tvAmountBis.setText("");
        tvAmountBis.setVisibility(View.INVISIBLE);
        tvArrow.setText("");
        tvArrow.setVisibility(View.INVISIBLE);
        tvAddress.setText("");
        tvAddress.setVisibility(View.INVISIBLE);

        btSend.setText("Send money");
        btSend.setVisibility(View.INVISIBLE);

        ivCheck.setVisibility(View.GONE);
        tvSentPrompt.setVisibility(View.GONE);
    }

}

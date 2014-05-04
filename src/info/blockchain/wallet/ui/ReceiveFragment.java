package info.blockchain.wallet.ui;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashMap;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnTouchListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ImageView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.TextView.OnEditorActionListener;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.text.InputType;
import android.text.SpannableStringBuilder;
import android.text.style.RelativeSizeSpan;
import android.text.style.SuperscriptSpan;
import android.text.style.StyleSpan;
import android.text.Spannable;
import android.util.Log;

import com.google.bitcoin.uri.BitcoinURI;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.android.Contents;
import com.google.zxing.client.android.encode.QRCodeEncoder;

public class ReceiveFragment extends Fragment   {

	private boolean addressesOn = true;
	private boolean contactsOn = true;
	
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
    private ImageView ivReceivingQR = null;

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

    private ImageView ivCheck = null;

    private HashMap<String,String> magicData = null;
    private ArrayList<String> keys = null;
	private MagicAdapter adapter = null;

	private boolean isBTC = false;

    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        rootView = inflater.inflate(R.layout.fragment_receive, container, false);

        tvAmount = (TextView)rootView.findViewById(R.id.amount);
        tvAmount.setVisibility(View.INVISIBLE);
        tvAmountBis = (TextView)rootView.findViewById(R.id.amount_bis);
        tvAmountBis.setVisibility(View.INVISIBLE);
        tvArrow = (TextView)rootView.findViewById(R.id.arrow);
        tvArrow.setVisibility(View.INVISIBLE);
        tvAddress = (TextView)rootView.findViewById(R.id.receiving_address);
        tvAddress.setVisibility(View.INVISIBLE);

        summary = (LinearLayout)rootView.findViewById(R.id.summary);
        summary.setVisibility(View.INVISIBLE);

        ivReceivingQR = (ImageView)rootView.findViewById(R.id.qr);
        ivReceivingQR.setVisibility(View.INVISIBLE);
        
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
    	divider1.setBackgroundColor(BlockchainUtil.BLOCKCHAIN_GREEN);
    	LinearLayout divider2 = (LinearLayout)rootView.findViewById(R.id.divider2);
    	divider2.setBackgroundColor(BlockchainUtil.BLOCKCHAIN_GREEN);

//        ((TextView)rootView.findViewById(R.id.direction)).setTypeface(TypefaceUtil.getInstance(getActivity()).getGravityBoldTypeface());
//        ((TextView)rootView.findViewById(R.id.direction)).setText(Character.toString((char)0x2192));
        ((ImageView)rootView.findViewById(R.id.direction)).setImageResource(R.drawable.green_arrow);
        ((TextView)rootView.findViewById(R.id.currency)).setText("$");
        ((TextView)rootView.findViewById(R.id.currency)).setTypeface(TypefaceUtil.getInstance(getActivity()).getGravityBoldTypeface());
        ((ImageView)rootView.findViewById(R.id.qr)).setImageBitmap(generateQRCode(BitcoinURI.convertToBitcoinURI("18nkx4epNwy4nEfFWZEtdBucwtj5TdSAm", BigInteger.valueOf(300000L), "", "")));

        initMagicList();

        /*
        btReceive = ((Button)rootView.findViewById(R.id.receive));
        btReceive.setVisibility(View.INVISIBLE);
        btReceive.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
        		Toast.makeText(getActivity(), "Receive", Toast.LENGTH_SHORT).show();
            }
        });
        */

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
		        	ivReceivingQR.setVisibility(View.VISIBLE);
		        	
		        	tvAddress.setText(edAddress.getText().toString());
		        	tvArrow.setText(Character.toString((char)0x2192));

		        	String amount1 = edAmount1.getText().toString();
		        	String amount2 = tvAmount2.getText().toString().substring(0, tvAmount2.getText().toString().length() - 4);
		        	long btcValue;
		        	double value;
		        	if(isBTC) {
		            	value = Math.round(Double.parseDouble(amount1) * 100000000.0);
		            	btcValue = (Double.valueOf(value)).longValue();
		        		amount1 += " BTC";
		        		amount2 += " USD";
		        	}
		        	else {
		            	value = Math.round(Double.parseDouble(amount2) * 100000000.0);
		            	btcValue = (Double.valueOf(value)).longValue();
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

		            ivReceivingQR.setImageBitmap(generateQRCode(BitcoinURI.convertToBitcoinURI(edAddress.getText().toString(), BigInteger.valueOf(btcValue), "", "")));

		        }
		        return false;
		    }
		});

        edAmount1 = ((EditText)rootView.findViewById(R.id.amount1));
        edAmount1.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
            	
            	if(ivReceivingQR.getVisibility() == View.VISIBLE) {
            		clearReceive();
            	}
            		
            }
        });

        edAddress = ((EditText)rootView.findViewById(R.id.address));
        edAddress.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
            	
            	if(ivReceivingQR.getVisibility() == View.VISIBLE) {
            		clearReceive();
            	}

            	if(!isMagic) {
            		
            		displayMagicList();

            	}
            	else {
//            		removeMagicList();
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
                
//                btReceive.setVisibility(View.INVISIBLE);
                ivReceivingQR.setVisibility(View.INVISIBLE);

            	if(!isMagic) {
                	displayMagicList();
            	}

                return false;
            }
        });
        clear_input.setVisibility(View.INVISIBLE);

        return rootView;
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);

        if(isVisibleToUser) {
        	if(isMagic) {
        		removeMagicList();
        	}
        	displayMagicList();
        }
        else {
        	;
        }

    }

    @Override
    public void onResume() {
    	super.onResume();

    	if(isMagic) {
    		removeMagicList();
    	}
    	displayMagicList();
    }

    private Bitmap generateQRCode(String uri) {

        Bitmap bitmap = null;
        int qrCodeDimension = 200;

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
        ivContacts.setVisibility(View.GONE);
        ivPhoneContacts = (ImageView)childIcons.findViewById(R.id.phone_contacts);
        ivPhoneContacts.setVisibility(View.GONE);
        /*
        ivContacts = (ImageView)childIcons.findViewById(R.id.contacts);
        ivContacts.setImageResource(R.drawable.my_contacts_icon);
        ivContacts.setBackgroundColor(colorOff);
        */
        addressesOn = true;
        contactsOn = false;
        ivAddresses.setOnClickListener(new View.OnClickListener() {        
            @Override
                public void onClick(View view) {
            		if(!addressesOn) {
            			addressesOn = true;
            			contactsOn = false;
                        ivAddresses.setBackgroundColor(colorOn);
//                        ivContacts.setBackgroundColor(colorOff);
            		}
            		initMagicList();
            		adapter.notifyDataSetChanged();                            		
                }
        });
        /*
        ivContacts.setOnClickListener(new View.OnClickListener() {        
            @Override
                public void onClick(View view) {
            		if(!contactsOn) {
            			contactsOn = true;
            			addressesOn = false;
                        ivAddresses.setBackgroundColor(colorOff);
                        ivContacts.setBackgroundColor(colorOn);
            		}
            		initMagicList();
            		adapter.notifyDataSetChanged();                            		
                }
        });
        */

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
    	divider1.setBackgroundColor(BlockchainUtil.BLOCKCHAIN_GREEN);

        //
        // add view with list
        //
		childList = inflater.inflate(R.layout.magic2, null);
    	divider1 = (LinearLayout)childList.findViewById(R.id.divider1);
    	divider1.setBackgroundColor(BlockchainUtil.BLOCKCHAIN_GREEN);
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

        LinearLayout container = ((LinearLayout)rootView.findViewById(R.id.qr_container));
        sendViewToBack(container);
        
	    parent.bringToFront();
	    parent.requestLayout();
	    parent.invalidate();
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


    private void clearReceive()	{
        summary.setVisibility(View.INVISIBLE);
        tvAmount.setText("");
        tvAmount.setVisibility(View.INVISIBLE);
        tvAmountBis.setText("");
        tvAmountBis.setVisibility(View.INVISIBLE);
        tvArrow.setText("");
        tvArrow.setVisibility(View.INVISIBLE);
        tvAddress.setText("");
        tvAddress.setVisibility(View.INVISIBLE);
        ivReceivingQR.setVisibility(View.INVISIBLE);
    }

}

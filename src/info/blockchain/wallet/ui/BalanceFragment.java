package info.blockchain.wallet.ui;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.android.Contents;
import com.google.zxing.client.android.encode.QRCodeEncoder;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.drawable.BitmapDrawable;
import android.support.v4.app.Fragment;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
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
import android.widget.ArrayAdapter;
import android.widget.AdapterView;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.view.View.OnTouchListener;
import android.view.animation.AnimationUtils;
import android.view.animation.Animation;
import android.view.inputmethod.InputMethodManager;
import android.util.Log;

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
    private String[] values = null;
    private boolean[] valuesDisplayed = null;
    private String[] amounts1 = null;
    private String[] amounts2 = null;
	private TransactionAdapter adapter = null;
	private boolean isBTC = true;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

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
        tViewAmount1.setText("24.1223");

        tViewAmount2 = (TextView)rootView.findViewById(R.id.amount2);
        tViewAmount1.setTypeface(TypefaceUtil.getInstance(getActivity()).getRobotoLightTypeface());
        tViewAmount2.setText("$" + BlockchainUtil.BTC2Fiat("24.1223"));

        txList = (ListView)rootView.findViewById(R.id.txList);
        values = new String[] {
        		"1AMdUn91Z1tAhy7XwF38BfQ5C63pmcdQH3",
        		"Cold storage",
        		"Merchant account",
        		"1Cr8FHbwZkcmUWTdZuzukYeSbaSGivUEU6",
                };
        valuesDisplayed = new boolean[] {
        		false,
        		false,
        		false,
        		false,
                };
        amounts1 = new String[] {
        		"0.67",
        		"20.0001",
        		"3.45",
        		"0.00227",
                };
        amounts2 = new String[] {
        		"8802.48",
        		"145",
        		"4608.63",
        		"1",
                };

        adapter = new TransactionAdapter();
        txList.setAdapter(adapter);
        txList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, final View view, int position, long id) {
    	    	final LinearLayout balance_extLayout = (LinearLayout)view.findViewById(R.id.balance_ext);
    	    	final LinearLayout balance_extHiddenLayout = (LinearLayout)view.findViewById(R.id.balance_ext_hidden);

    	    	if(balance_extHiddenLayout.getVisibility() == View.VISIBLE) {
    	    		
    	    		valuesDisplayed[position] = false;
    	    		
    	    		if(balance_extHiddenLayout.getChildCount() > 1) {
        		        balance_extHiddenLayout.removeViews(1, balance_extHiddenLayout.getChildCount() - 1);
    	    		}

    		        balance_extLayout.startAnimation(slideUp);
    		        balance_extLayout.setVisibility(View.GONE);
    		        balance_extHiddenLayout.setVisibility(View.GONE);
    	    	}
    	    	else {
    	    		valuesDisplayed[position] = true;
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
	    	        ((TextView)progression_sent.findViewById(R.id.amount)).setText("2.5000 BTC");
	    	        ((ProgressBar)progression_sent.findViewById(R.id.bar)).setMax(100);
	    	        ((ProgressBar)progression_sent.findViewById(R.id.bar)).setProgress((int)((2.5 / (2.5 + 26.6223)) * 100));
	    	        ((ProgressBar)progression_sent.findViewById(R.id.bar)).setProgressDrawable(getResources().getDrawable(R.drawable.progress_red));

	    	        LinearLayout progression_received = ((LinearLayout)balance_extLayout.findViewById(R.id.progression_received));
	    	        ((TextView)progression_received.findViewById(R.id.total_type)).setTypeface(TypefaceUtil.getInstance(getActivity()).getRobotoTypeface());
	    	        ((TextView)progression_received.findViewById(R.id.total_type)).setTextColor(Color.BLACK);
	    	        ((TextView)progression_received.findViewById(R.id.total_type)).setText("Total Received");
	    	        ((TextView)progression_received.findViewById(R.id.amount)).setTypeface(TypefaceUtil.getInstance(getActivity()).getRobotoTypeface());
	    	        ((TextView)progression_received.findViewById(R.id.amount)).setTextColor(Color.BLACK);
	    	        ((TextView)progression_received.findViewById(R.id.amount)).setText("26.6223 BTC");
	    	        ((ProgressBar)progression_received.findViewById(R.id.bar)).setMax(100);
	    	        ((ProgressBar)progression_received.findViewById(R.id.bar)).setProgress((int)((26.6223 / (2.5 + 26.6223)) * 100));
	    	        ((ProgressBar)progression_received.findViewById(R.id.bar)).setProgressDrawable(getResources().getDrawable(R.drawable.progress_green));
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

        return rootView;
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);

        if(isVisibleToUser) {
        	;
        }
        else {
        	;
        }
    }

    @Override
    public void onResume() {
    	super.onResume();

    }

    private class TransactionAdapter extends BaseAdapter {
    	
		private LayoutInflater inflater = null;

	    TransactionAdapter() {
	        inflater = (LayoutInflater)getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}

		@Override
		public int getCount() {
			return values.length;
		}

		@Override
		public String getItem(int position) {
	        return values[position];
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
	        	amount = df.format(Double.parseDouble(amounts1[position]));
	        }
	        else {
//	        	df = new DecimalFormat("######0.00");
	        	amount = BlockchainUtil.BTC2Fiat(amounts1[position]);
	        }

	        if(position % 2 == 0) {
		        ((ImageView)view.findViewById(R.id.address_type)).setImageResource(R.drawable.address_active);
	        }
	        else {
		        ((ImageView)view.findViewById(R.id.address_type)).setImageResource(R.drawable.address_inactive);
	        }

	        ((TextView)view.findViewById(R.id.address)).setTypeface(TypefaceUtil.getInstance(getActivity()).getGravityBoldTypeface());
	        ((TextView)view.findViewById(R.id.address)).setText(values[position].length() > 15 ? values[position].substring(0, 15) + "..." : values[position]);
	        ((TextView)view.findViewById(R.id.amount)).setTypeface(TypefaceUtil.getInstance(getActivity()).getRobotoBoldTypeface());
	        ((TextView)view.findViewById(R.id.amount)).setText(amount);
	        ((TextView)view.findViewById(R.id.currency_code)).setText(isBTC ? "BTC" : "USD");
	        
	        if(valuesDisplayed[position]) {
				Log.d("List refresh sub", "" + position);
		        doDisplaySubList(view, position);
	        }

	        return view;
		}

    }

    public void doDisplaySubList(final View view, int position) {
    	final LinearLayout balance_extLayout = (LinearLayout)view.findViewById(R.id.balance_ext);
    	final LinearLayout balance_extHiddenLayout = (LinearLayout)view.findViewById(R.id.balance_ext_hidden);

        balance_extLayout.setOnLongClickListener(new View.OnLongClickListener() {
      	  public boolean onLongClick(View view) {
//    			Toast.makeText(PaymentFragment.this.getActivity(), "Address copied:" + input_address, Toast.LENGTH_LONG).show();

    			android.content.ClipboardManager clipboard = (android.content.ClipboardManager)getActivity().getSystemService(android.content.Context.CLIPBOARD_SERVICE);
      		    android.content.ClipData clip = android.content.ClipData.newPlainText("Address", "1NMNj5tcwKkqRyHQepxfh1YvLVvBc3Jruq");
      		    clipboard.setPrimaryClip(clip);
     			Toast.makeText(getActivity(), "Address copied to clipboard:" + "1NMNj5tcwKkqRyHQepxfh1YvLVvBc3Jruq", Toast.LENGTH_LONG).show();

            	Bitmap bm = generateQRCode("1NMNj5tcwKkqRyHQepxfh1YvLVvBc3Jruq");

            	View toastView = getActivity().getLayoutInflater().inflate(R.layout.toast, (ViewGroup)getActivity().findViewById(R.id.toastLayout));
        		ImageView imageView = (ImageView)toastView.findViewById(R.id.image);
        		imageView.setImageBitmap(bm);
        		//            imageView.setBackgroundDrawable(bitmapDrawable);
        		TextView textView = (TextView)toastView.findViewById(R.id.text);
        		textView.setText("Yes, a Toast with an image!");
        		Toast toast = new Toast(getActivity());
        		toast.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
        		toast.setDuration(Toast.LENGTH_LONG);
        		toast.setView(toastView);
        		toast.show();

        		return true;
      	  }
      	});

        LinearLayout progression_sent = ((LinearLayout)balance_extLayout.findViewById(R.id.progression_sent));
        ((TextView)progression_sent.findViewById(R.id.total_type)).setTypeface(TypefaceUtil.getInstance(getActivity()).getRobotoTypeface());
        ((TextView)progression_sent.findViewById(R.id.total_type)).setTextColor(Color.BLACK);
        ((TextView)progression_sent.findViewById(R.id.total_type)).setText("TOTAL SENT");
        ((TextView)progression_sent.findViewById(R.id.amount)).setTypeface(TypefaceUtil.getInstance(getActivity()).getRobotoTypeface());
        ((TextView)progression_sent.findViewById(R.id.amount)).setTextColor(Color.BLACK);
        ((TextView)progression_sent.findViewById(R.id.amount)).setText("0.8251 BTC");
        ((ProgressBar)progression_sent.findViewById(R.id.bar)).setMax(100);
        ((ProgressBar)progression_sent.findViewById(R.id.bar)).setProgress((int)((0.8251 / (2.55 + 0.8251)) * 100));
        ((ProgressBar)progression_sent.findViewById(R.id.bar)).setProgressDrawable(getResources().getDrawable(R.drawable.progress_red2));

        LinearLayout progression_received = ((LinearLayout)balance_extLayout.findViewById(R.id.progression_received));
        ((TextView)progression_received.findViewById(R.id.total_type)).setTypeface(TypefaceUtil.getInstance(getActivity()).getRobotoTypeface());
        ((TextView)progression_received.findViewById(R.id.total_type)).setTextColor(Color.BLACK);
        ((TextView)progression_received.findViewById(R.id.total_type)).setText("TOTAL RECEIVED");
        ((TextView)progression_received.findViewById(R.id.amount)).setTypeface(TypefaceUtil.getInstance(getActivity()).getRobotoTypeface());
        ((TextView)progression_received.findViewById(R.id.amount)).setTextColor(Color.BLACK);
        ((TextView)progression_received.findViewById(R.id.amount)).setText("2.5500 BTC");
        ((ProgressBar)progression_received.findViewById(R.id.bar)).setMax(100);
        ((ProgressBar)progression_received.findViewById(R.id.bar)).setProgress((int)((2.55 / (2.55 + 0.8251)) * 100));
        ((ProgressBar)progression_received.findViewById(R.id.bar)).setProgressDrawable(getResources().getDrawable(R.drawable.progress_green2));

		View child = null;
        LayoutInflater inflater = (LayoutInflater)getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        for (int i = 0; i < 4; i++) {
			child = inflater.inflate(R.layout.tx_layout, null);

	        ((TextView)child.findViewById(R.id.ts)).setTypeface(TypefaceUtil.getInstance(getActivity()).getGravityBoldTypeface());
	        
	        long ts = 0L;
	        if(i == 0) {
	        	ts = System.currentTimeMillis() / 1000;
	        }
	        else if(i == 1) {
	        	ts = 1396857561;
	        }
	        else if(i == 2) {
	        	ts = 1396457468;
	        }
	        else {
	        	ts = 1394595077;
	        }
	        ((TextView)child.findViewById(R.id.ts)).setText(DateUtil.getInstance().formatted(ts));

	        /*
	        ((TextView)child.findViewById(R.id.type)).setTypeface(TypefaceUtil.getInstance(getActivity()).getGravityBoldTypeface());
	        ((TextView)child.findViewById(R.id.type)).setTextColor(i % 2 == 0 ? BlockchainUtil.BLOCKCHAIN_GREEN : BlockchainUtil.BLOCKCHAIN_RED);
	        ((TextView)child.findViewById(R.id.type)).setText(i % 2 == 0 ? "RECEIVED" : "SENT");
	        */
	        ((ImageView)child.findViewById(R.id.txbitmap)).setImageBitmap(TxBitmap.getInstance(getActivity()).createArrowsBitmap(200, i % 2 == 0 ? TxBitmap.RECEIVING : TxBitmap.SENDING, i % 2 == 0 ? 1 : 3));
	        ((ImageView)child.findViewById(R.id.address)).setImageBitmap(TxBitmap.getInstance(getActivity()).createListBitmap(200, i % 2 == 0 ? 1 : 3));
	        ((TextView)child.findViewById(R.id.amount)).setTypeface(TypefaceUtil.getInstance(getActivity()).getGravityBoldTypeface());
	        ((TextView)child.findViewById(R.id.amount)).setTextColor(i % 2 == 0 ? BlockchainUtil.BLOCKCHAIN_GREEN : BlockchainUtil.BLOCKCHAIN_RED);
	        if(isBTC) {
				Log.d("List refresh sub", "isBTC");
		        ((TextView)child.findViewById(R.id.amount)).setText(i % 2 == 0 ? "1.000 BTC" : "0.670 BTC");
	        }
	        else {
				Log.d("List refresh sub", "!isBTC");
		        ((TextView)child.findViewById(R.id.amount)).setText(i % 2 == 0 ? (BlockchainUtil.BTC2Fiat("1.0") + " USD") : (BlockchainUtil.BTC2Fiat("0.67") + " USD"));
	        }
	        
			child.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(Intent.ACTION_VIEW , Uri.parse("https://blockchain.info/"));
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

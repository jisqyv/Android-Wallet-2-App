package info.blockchain.wallet.ui;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashMap;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.text.Editable;
import android.text.TextWatcher;
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
//import android.util.Log;

import com.google.bitcoin.uri.BitcoinURI;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.android.Contents;
import com.google.zxing.client.android.encode.QRCodeEncoder;

public class SendReceiveFragment extends Fragment   {

	private boolean isSend = true;
	
	private boolean addressesOn = true;
	private boolean contactsOn = true;
	
	private View rootView = null;
	
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

    private HashMap<String,String> magicData = null;
    private ArrayList<String> keys = null;
	private MagicAdapter adapter = null;

    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

    	ViewPager mPager = (ViewPager)getActivity().findViewById(R.id.pager);
    	isSend = (mPager.getCurrentItem() == 0) ? true : false;
    	
    	if(isSend) {
            rootView = inflater.inflate(R.layout.fragment_send, container, false);
    	}
    	else {
            rootView = inflater.inflate(R.layout.fragment_receive, container, false);
    	}

        ImageView qr_scan = (ImageView)rootView.findViewById(R.id.qr_icon);
        qr_scan.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
        		Toast.makeText(getActivity(), "Send/Receive QR reader", Toast.LENGTH_SHORT).show();
                return false;
            }
        });

    	if(isSend) {
        	LinearLayout divider1 = (LinearLayout)rootView.findViewById(R.id.divider1);
        	divider1.setBackgroundColor(BlockchainUtil.BLOCKCHAIN_RED);
        	LinearLayout divider2 = (LinearLayout)rootView.findViewById(R.id.divider2);
        	divider2.setBackgroundColor(BlockchainUtil.BLOCKCHAIN_RED);

            ((TextView)rootView.findViewById(R.id.direction)).setText("To");
            ((TextView)rootView.findViewById(R.id.currency)).setText("$");
 
            final ImageButton imgSimpleSend = ((ImageButton)rootView.findViewById(R.id.simple));
            final ImageButton imgCustomSend = ((ImageButton)rootView.findViewById(R.id.custom));
            final ImageButton imgSharedSend = ((ImageButton)rootView.findViewById(R.id.shared));
            imgSimpleSend.setOnTouchListener(new OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    	imgSimpleSend.setBackgroundColor(0xff3a3a3a);
                        return true;
                    }
                    else if (event.getAction() == MotionEvent.ACTION_UP) {
                    	imgSimpleSend.setBackgroundColor(0xff000000);
                        return true;
                    }
                    else if(event.getAction() == MotionEvent.ACTION_CANCEL)	{
                    	imgSimpleSend.setBackgroundColor(0xff000000);
                        return true;
                    }
                    else	{
                    	;
                    }
                    return false;
                }
            });

            imgCustomSend.setOnTouchListener(new OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    	imgCustomSend.setBackgroundColor(0xff3a3a3a);
                        return true;
                    }
                    else if (event.getAction() == MotionEvent.ACTION_UP) {
                    	imgCustomSend.setBackgroundColor(0xff000000);
                        return true;
                    }
                    else if(event.getAction() == MotionEvent.ACTION_CANCEL)	{
                    	imgCustomSend.setBackgroundColor(0xff000000);
                        return true;
                    }
                    else	{
                    	;
                    }
                    return false;
                }
            });

            imgSharedSend.setOnTouchListener(new OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    	imgSharedSend.setBackgroundColor(0xff3a3a3a);
                        return true;
                    }
                    else if (event.getAction() == MotionEvent.ACTION_UP) {
                    	imgSharedSend.setBackgroundColor(0xff000000);
                        return true;
                    }
                    else if(event.getAction() == MotionEvent.ACTION_CANCEL)	{
                    	imgSharedSend.setBackgroundColor(0xff000000);
                        return true;
                    }
                    else	{
                    	;
                    }
                    return false;
                }
            });

    	}
    	else {
        	LinearLayout divider1 = (LinearLayout)rootView.findViewById(R.id.divider1);
        	divider1.setBackgroundColor(BlockchainUtil.BLOCKCHAIN_GREEN);
        	LinearLayout divider2 = (LinearLayout)rootView.findViewById(R.id.divider2);
        	divider2.setBackgroundColor(BlockchainUtil.BLOCKCHAIN_GREEN);

            ((TextView)rootView.findViewById(R.id.direction)).setText("To");
            ((TextView)rootView.findViewById(R.id.currency)).setText("$");
            ((ImageView)rootView.findViewById(R.id.qr)).setImageBitmap(generateQRCode(BitcoinURI.convertToBitcoinURI("18nkx4epNwy4nEfFWZEtdBucwtj5TdSAm", BigInteger.valueOf(300000L), "", "")));

            initMagicList();

            final EditText edAddress = ((EditText)rootView.findViewById(R.id.address));
            edAddress.setOnClickListener(new Button.OnClickListener() {
                public void onClick(View v) {
                	
                	if(!isMagic) {

                		isMagic = true;
                		
                		//
                		//
                		//
                        ((TextView)rootView.findViewById(R.id.currency)).setText("");
                        ((TextView)rootView.findViewById(R.id.currency)).setBackgroundColor(0xFF232223);

                		//
                        // add view with my_addresses and contacts
                        //
                        magic = ((LinearLayout)rootView.findViewById(R.id.magic_input));
                        oldView = ((LinearLayout)magic.findViewById(R.id.magic_bis));
                        parent = (LinearLayout)oldView.getParent();
                        parent.removeView(oldView);
                        oldView.setVisibility(View.GONE);
                		childIcons = inflater.inflate(R.layout.magic, null);
                        addressesOn = true;
                        contactsOn = false;
                        ivAddresses.setOnClickListener(new View.OnClickListener() {        
                            @Override
                                public void onClick(View view) {
                            		if(!addressesOn) {
                            			addressesOn = true;
                            			contactsOn = false;
                                        ivAddresses.setBackgroundColor(0xFF9d9d9d);
                                        ivContacts.setBackgroundColor(0xFFb6b6b6);
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
                            			addressesOn = false;
                                        ivAddresses.setBackgroundColor(0xFFb6b6b6);
                                        ivContacts.setBackgroundColor(0xFF9d9d9d);
                            		}
                            		initMagicList();
                            		adapter.notifyDataSetChanged();                            		
                                }
                            });
//                	    parent.addView(child, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                	    parent.addView(childIcons);
                	    children++;
                	    
                    	LinearLayout divider1 = (LinearLayout)childIcons.findViewById(R.id.divider1);
                    	divider1.setBackgroundColor(BlockchainUtil.BLOCKCHAIN_GREEN);

                        //
                        // add view with list
                        //
                		childList = inflater.inflate(R.layout.magic2, null);
                        magicList = ((ListView)childList.findViewById(R.id.magicList));
                        magicList.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
                	    parent.addView(childList);
                	    children++;
                	    
                        magicList.setOnItemClickListener(new OnItemClickListener() {
                	        public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3)	{
                                Toast.makeText(getActivity(), keys.get(position), Toast.LENGTH_SHORT).show();
                                edAddress.setText(keys.get(position));
                                removeMagicList();
                            }
                        });

                    	divider1 = (LinearLayout)childList.findViewById(R.id.divider1);
                    	divider1.setBackgroundColor(BlockchainUtil.BLOCKCHAIN_GREEN);

                        adapter = new MagicAdapter();
                        magicList.setAdapter(adapter);

                        LinearLayout container = ((LinearLayout)rootView.findViewById(R.id.qr_container));
                        sendViewToBack(container);
                        
                	    parent.bringToFront();
                	    parent.requestLayout();
                	    parent.invalidate();
                	}
                	else {
                		removeMagicList();
                	}
                		
                }
            });

            edAddress.addTextChangedListener(new TextWatcher()	{

            	public void afterTextChanged(Editable s) { ; }

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
            		adapter.notifyDataSetChanged();
                	}
            });
    	}

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
        final ViewGroup parent = (ViewGroup)child.getParent();
        if (null != parent) {
            parent.removeView(child);
            parent.addView(child, 0);
        }
    }

    private class MagicAdapter extends BaseAdapter {
    	
		private LayoutInflater inflater = null;

	    MagicAdapter() {
	        inflater = (LayoutInflater)getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}

		@Override
		public int getCount() {
			return keys.size();
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
		        ((TextView)view.findViewById(R.id.p1)).setTextColor(0xFF818689);
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

    private void removeMagicList() {
		isMagic = false;

        ((TextView)rootView.findViewById(R.id.currency)).setBackgroundColor(0xFFFFFFFF);
        ((TextView)rootView.findViewById(R.id.currency)).setText("$");

        parent.removeViews(parent.getChildCount() - children, children);
        children = 0;
        oldView.setVisibility(View.VISIBLE);
        parent.addView(oldView);
        
        initMagicList();
    }

}

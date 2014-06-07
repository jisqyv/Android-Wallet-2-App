package info.blockchain.wallet.ui;
 
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import piuk.MyRemoteWallet;
import piuk.blockchain.android.R;
 
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnTouchListener;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;
 
public class AddressBookActivity extends Activity {

	private ArrayList<String> allAddresses = null;
	private Map<String, String> labelMap = null;
	private AddressAdapter adapter = null;
    private List<Map<String, Object>> addressBookMapList = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.addressbook);
        
        //
        //
        //
		MyRemoteWallet remoteWallet = WalletUtil.getInstance(this, this).getRemoteWallet();
		String[] activeAddresses = remoteWallet.getActiveAddresses();

		allAddresses = new ArrayList<String>();
        for(int i = 0; i < activeAddresses.length; i++)	{
        	allAddresses.add("A" + activeAddresses[i]);
        }

		labelMap = remoteWallet.getLabelMap();
        //
        //
        //

        ListView listView = (ListView)findViewById(R.id.listview);
        adapter = new AddressAdapter();
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, final View view, int position, long id) {
				Toast.makeText(AddressBookActivity.this, allAddresses.get(position), Toast.LENGTH_LONG).show();
            }
        });
        
        final ImageView imgArchived = ((ImageView)findViewById(R.id.archived));
        final ImageView imgActive = ((ImageView)findViewById(R.id.active));
        final ImageView imgSending = ((ImageView)findViewById(R.id.sending));
        final LinearLayout layoutArchived = ((LinearLayout)findViewById(R.id.archived_bg));
        final LinearLayout layoutActive = ((LinearLayout)findViewById(R.id.active_bg));
        final LinearLayout layoutSending = ((LinearLayout)findViewById(R.id.sending_bg));
        
        final int color_spend_selected = 0xff808080;
        final int color_spend_unselected = 0xffa0a0a0;
        
    	imgArchived.setBackgroundColor(color_spend_unselected);
    	imgActive.setBackgroundColor(color_spend_selected);
    	imgSending.setBackgroundColor(color_spend_unselected);
    	layoutArchived.setBackgroundColor(color_spend_unselected);
    	layoutActive.setBackgroundColor(color_spend_selected);
    	layoutSending.setBackgroundColor(color_spend_unselected);

        layoutArchived.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
				imgArchived.setBackgroundColor(color_spend_selected);
            	imgActive.setBackgroundColor(color_spend_unselected);
            	imgSending.setBackgroundColor(color_spend_unselected);
            	layoutArchived.setBackgroundColor(color_spend_selected);
            	layoutActive.setBackgroundColor(color_spend_unselected);
            	layoutSending.setBackgroundColor(color_spend_unselected);
            	
            	initArchivedList();

                return false;
            }
        });

        layoutActive.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
				imgArchived.setBackgroundColor(color_spend_unselected);
            	imgActive.setBackgroundColor(color_spend_selected);
            	imgSending.setBackgroundColor(color_spend_unselected);
            	layoutArchived.setBackgroundColor(color_spend_unselected);
            	layoutActive.setBackgroundColor(color_spend_selected);
            	layoutSending.setBackgroundColor(color_spend_unselected);

            	initActiveList();

                return false;
            }
        });

        layoutSending.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
				imgArchived.setBackgroundColor(color_spend_unselected);
            	imgActive.setBackgroundColor(color_spend_unselected);
            	imgSending.setBackgroundColor(color_spend_selected);
            	layoutArchived.setBackgroundColor(color_spend_unselected);
            	layoutActive.setBackgroundColor(color_spend_unselected);
            	layoutSending.setBackgroundColor(color_spend_selected);

            	initSendingList();

                return false;
            }
        });

    }

    private void initArchivedList() {
		MyRemoteWallet remoteWallet = WalletUtil.getInstance(this, this).getRemoteWallet();
		String[] archivedAddresses = remoteWallet.getArchivedAddresses();

		allAddresses = new ArrayList<String>();
        for(int i = 0; i < archivedAddresses.length; i++)	{
        	allAddresses.add("R" + archivedAddresses[i]);
        }

		labelMap = remoteWallet.getLabelMap();
		adapter.notifyDataSetChanged();
    }

    private void initActiveList() {
		MyRemoteWallet remoteWallet = WalletUtil.getInstance(this, this).getRemoteWallet();
		String[] activeAddresses = remoteWallet.getActiveAddresses();

		allAddresses = new ArrayList<String>();
        for(int i = 0; i < activeAddresses.length; i++)	{
        	allAddresses.add("A" + activeAddresses[i]);
        }

		labelMap = remoteWallet.getLabelMap();
		adapter.notifyDataSetChanged();
    }

    private void initSendingList() {
		MyRemoteWallet remoteWallet = WalletUtil.getInstance(this, this).getRemoteWallet();
        addressBookMapList = remoteWallet.getAddressBookMap();

		allAddresses = new ArrayList<String>();
	    for (Iterator<Map<String, Object>> iti = addressBookMapList.iterator(); iti.hasNext();) {
	    	Map<String, Object> addressBookMap = iti.next();
	    	String address = (String)addressBookMap.get("addr");
	    	allAddresses.add("S" + address);
	    }

		labelMap = remoteWallet.getLabelMap();
		adapter.notifyDataSetChanged();
    }

    private class AddressAdapter extends BaseAdapter {
    	
		private LayoutInflater inflater = null;

	    AddressAdapter() {
	        inflater = (LayoutInflater)AddressBookActivity.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}

		@Override
		public int getCount() {
			return allAddresses.size();
		}

		@Override
		public String getItem(int position) {
	        return "";
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			
			View view = null;
	        
	        if (convertView == null) {
	            view = inflater.inflate(R.layout.address_list, parent, false);
	        } else {
	            view = convertView;
	        }
	        
	        String type = allAddresses.get(position).substring(0, 1);
	        String addr = allAddresses.get(position).substring(1);
	        
    	    String label = labelMap.get(addr);
    	    if (label == null) {
    	    	label = "Unlabeled";	
    	    }
    	    
    	    if(type.equals("A")) {
//                ((ImageView)view.findViewById(R.id.type)).setImageResource(R.drawable.ic_launcher);
    	    }
    	    else if(type.equals("S")) {
//                ((ImageView)view.findViewById(R.id.type)).setImageResource(R.drawable.address_book);
    	    }
    	    else {
//                ((ImageView)view.findViewById(R.id.type)).setImageResource(R.drawable.blockchain_logo);
    	    }
	        ((TextView)view.findViewById(R.id.txt1)).setText(label);
	        ((TextView)view.findViewById(R.id.txt2)).setText(addr);

	        return view;
		}

    }

}

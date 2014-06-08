package info.blockchain.wallet.ui;
 
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.google.bitcoin.core.Transaction;

import piuk.EventListeners;
import piuk.MyRemoteWallet;
import piuk.blockchain.android.R;
import piuk.blockchain.android.WalletApplication;
import piuk.blockchain.android.WalletApplication.AddAddressCallback;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnTouchListener;
import android.view.View.OnCreateContextMenuListener;
import android.view.MenuInflater;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.Toast;
import android.util.Log;
 
public class AddressBookActivity extends Activity {

	private ArrayList<String> allAddresses = null;
	private Map<String, String> labelMap = null;
	private AddressAdapter adapter = null;
    private List<Map<String, Object>> addressBookMapList = null;
    private AddressManager addressManager = null;
    private WalletApplication application = null;
    
    private static enum DisplayedAddresses {
		SendingAddresses,
		ActiveAddresses,
		ArchivedAddresses
	}
	
    private DisplayedAddresses displayedAddresses = null;
    
	private EventListeners.EventListener eventListener = new EventListeners.EventListener() {
		@Override
		public String getDescription() {
			return "AddressBookActivity Listener";
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
	
	public  void setAdapterContent() {
		if (displayedAddresses == DisplayedAddresses.ActiveAddresses) {
			initActiveList();	
		} else if (displayedAddresses == DisplayedAddresses.ArchivedAddresses) {
			initArchivedList();	

		} else if (displayedAddresses == DisplayedAddresses.SendingAddresses) {
			initSendingList();	
		} 
	}
	
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
        
        displayedAddresses = DisplayedAddresses.ActiveAddresses;
        
		labelMap = remoteWallet.getLabelMap();
        //
        //
        //

        ListView listView = (ListView)findViewById(R.id.listview);
        listView.setLongClickable(true);
        adapter = new AddressAdapter();
        listView.setAdapter(adapter);
        /*
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, final View view, int position, long id) {
				Toast.makeText(AddressBookActivity.this, allAddresses.get(position), Toast.LENGTH_LONG).show();
            }
        });
        */
        listView.setOnCreateContextMenuListener(new OnCreateContextMenuListener() {
            @Override 
            public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
                MenuInflater inflater = getMenuInflater();
                inflater.inflate(R.menu.address_list, menu);
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

		final AddressBookActivity activity = this;
		application = (WalletApplication) this.getApplication();    	
        addressManager = new AddressManager(remoteWallet, application, activity);        
		EventListeners.addEventListener(eventListener);
    }

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(info.blockchain.wallet.ui.R.menu.addressbook, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    switch (item.getItemId()) {
	    	case R.id.new_address:
	    		addressManager.newAddress(new AddAddressCallback() {

	    			public void onSavedAddress(String address) {
	    				Toast.makeText(AddressBookActivity.this, getString(R.string.toast_generated_address, address), Toast.LENGTH_LONG).show();
	    			}

	    			public void onError(String reason) {
	    				Toast.makeText(AddressBookActivity.this, reason, Toast.LENGTH_LONG).show();

	    			}
	    		});
	    		
	    		Toast.makeText(AddressBookActivity.this, "generate new address", Toast.LENGTH_LONG).show();
	    		return true;
	    	default:
	    		return super.onOptionsItemSelected(item);
	    }
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
	    EventListeners.removeEventListener(eventListener);
	}
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
	    AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)menuInfo;
		Toast.makeText(AddressBookActivity.this, "" + info.position, Toast.LENGTH_LONG).show();
	    menu.removeItem(R.id.edit_label);
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
	    AdapterContextMenuInfo menuInfo = (AdapterContextMenuInfo)item.getMenuInfo(); 
	    String address = allAddresses.get(menuInfo.position).substring(1);
		
	    switch (item.getItemId()) {
	    	case R.id.edit_label:
	    		Toast.makeText(AddressBookActivity.this, "edit label", Toast.LENGTH_LONG).show();
	    		return true;
	    	case R.id.archive_address:
	    		addressManager.archiveAddress(address);
	    		return true;
	    	case R.id.unarchive_address:
	    		addressManager.unArchiveAddress(address);
	    		return true;
	    	case R.id.remove_address:
	    		addressManager.deleteAddressBook(address);
	    		return true;
	    	case R.id.qr_code:
	    		Toast.makeText(AddressBookActivity.this, "qr code address", Toast.LENGTH_LONG).show();
	    		return true;
	    	case R.id.default_address:
	    		addressManager.setDefaultAddress(address);
	    		return true;
	    	default:
	    		return super.onContextItemSelected(item);
	    }
	}

    private void initArchivedList() {
        displayedAddresses = DisplayedAddresses.ArchivedAddresses;

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
        displayedAddresses = DisplayedAddresses.ActiveAddresses;

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
        displayedAddresses = DisplayedAddresses.SendingAddresses;

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

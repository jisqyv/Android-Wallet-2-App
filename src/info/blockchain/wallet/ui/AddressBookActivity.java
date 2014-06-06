package info.blockchain.wallet.ui;
 
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import piuk.MyRemoteWallet;
 
import android.app.Activity;
import android.os.Bundle;
import android.widget.ListView;
import android.widget.SimpleAdapter;
 
public class AddressBookActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.addressbook);

		MyRemoteWallet remoteWallet = WalletUtil.getInstance(this, this).getRemoteWallet();
		String[] addresses = remoteWallet.getActiveAddresses();
//	    List<String> activeAddresses = Arrays.asList(addressLabels);
		Map<String, String> labelMap = remoteWallet.getLabelMap();
		String label = null;
        String address = null;

        List<HashMap<String,String>> addressList = new ArrayList<HashMap<String,String>>();
 
        for(int i = 0; i < addresses.length; i++){
            HashMap<String, String> hm = new HashMap<String,String>();
            
    	    label = labelMap.get(addresses[i]);
    	    if (label == null) {
    	    	label = addresses[i].substring(0, 15) + "...";	
    	    }
    	    else {
    	    	address = addresses[i];
    	    }

            hm.put("txt1", label);
            hm.put("txt2", address);
            hm.put("type", Integer.toString(R.drawable.ic_launcher));
            addressList.add(hm);
        }

        // Keys used in Hashmap
        String[] from = { "type","txt1","txt2" };
 
        // Ids of views in listview_layout
        int[] to = { R.id.type, R.id.txt1, R.id.txt2 };
 
        // Instantiating an adapter to store each items
        // R.layout.listview_layout defines the layout of each item
        SimpleAdapter adapter = new SimpleAdapter(getBaseContext(), addressList, R.layout.address_list, from, to);
 
        // Getting a reference to listview of main.xml layout file
        ListView listView = (ListView)findViewById(R.id.listview);
 
        // Setting the adapter to the listView
        listView.setAdapter(adapter);
    }
}

package info.blockchain.wallet.ui;

import android.app.ListActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

public class AddressBookActivity extends ListActivity {

	  public void onCreate(Bundle icicle) {

		  super.onCreate(icicle);

		  String[] values = new String[] { "Android", "iPhone", "WindowsMobile",
		  	"Blackberry", "WebOS", "Ubuntu", "Windows7", "Max OS X",
		    "Linux", "OS/2" };

//		  ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, R.layout.address_list, R.id.label, values);
//		  setListAdapter(adapter);
	  }

	  @Override
	  protected void onListItemClick(ListView l, View v, int position, long id) {
		  String item = (String) getListAdapter().getItem(position);
		  Toast.makeText(this, item + " selected", Toast.LENGTH_LONG).show();
	  }

} 

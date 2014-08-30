package info.blockchain.wallet.ui;
 
import java.util.ArrayList;
 
import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import piuk.blockchain.android.R;
 
public class NavDrawerListAdapter extends BaseAdapter {
     
    private Context context = null;
    
    public NavDrawerListAdapter(Context context){
        this.context = context;
    }
 
    @Override
    public int getCount() {
        return 6;
    }
 
    @Override
    public Object getItem(int position) {       
        return null;
    }
 
    @Override
    public long getItemId(int position) {
        return position;
    }
 
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
    	
        ImageView imgIcon = null;
        TextView txtTitle = null;
        TextView txtTitle2 = null;

        if(convertView == null) {
            LayoutInflater mInflater = (LayoutInflater)context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
            
            if(position == 0) {
                convertView = mInflater.inflate(R.layout.drawer_list_item_header, null);
                convertView.setClickable(false);
                convertView.setActivated(false);
                
                txtTitle = (TextView) convertView.findViewById(R.id.title);
                txtTitle.setText("My Bitcoin Wallet");

            }
            else if(position == 1) {
                convertView = mInflater.inflate(R.layout.drawer_list_item_header2, null);
                convertView.setClickable(false);
                convertView.setActivated(false);
                
                txtTitle = (TextView) convertView.findViewById(R.id.title);
                txtTitle.setText("Quick Links");

            }
            else {
                convertView = mInflater.inflate(R.layout.drawer_list_item_enhanced, null);
                
                imgIcon = (ImageView) convertView.findViewById(R.id.icon);
                txtTitle = (TextView) convertView.findViewById(R.id.title);
                txtTitle2 = (TextView) convertView.findViewById(R.id.title2);
                
                switch(position) {
                case 2:
                    imgIcon.setImageResource(R.drawable.merchant_icon);        
                    txtTitle.setText("Merchant Directory");
                    txtTitle2.setText("Find bitcoin merchants near you");
                	break;
                case 3:
                    imgIcon.setImageResource(R.drawable.address_book_icon);        
                    txtTitle.setText("Address Book");
                    txtTitle2.setText("Manage your bitcoin addresses and add contacts");
                	break;
                case 4:
                    imgIcon.setImageResource(R.drawable.zb_icon);        
                    txtTitle.setText("Price & Charts");
                    txtTitle2.setText("The latest bitcoin price, news, and charts");
                	break;
                case 5:
                    imgIcon.setImageResource(R.drawable.settings_icon);        
                    txtTitle.setText("Wallet Settings");
//                    txtTitle2.setText("Set your currency, adjust security settings, and backup your wallet");
                    txtTitle2.setText("Currencies, security settings, and wallet backups");
                	break;
                }

            }
        }

        return convertView;
    }
 
}

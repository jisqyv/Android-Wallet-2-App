package info.blockchain.wallet.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.net.Uri;
import android.os.Bundle;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.preference.Preference;
import android.preference.CheckBoxPreference;
import android.preference.PreferenceActivity;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.Preference.OnPreferenceChangeListener;
import android.view.KeyEvent;
//import android.util.Log;
import android.widget.Toast;

public class SettingsActivity extends PreferenceActivity {

    /** Called when the activity is first created. */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setTitle(R.string.app_name);
        	addPreferencesFromResource(info.blockchain.wallet.ui.R.xml.settings);

        	final String guid = WalletUtil.getInstance(this, this).getRemoteWallet().getGUID();

        	Preference guidPref = (Preference) findPreference("guid");
        	guidPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
        		public boolean onPreferenceClick(Preference preference) {

          			android.content.ClipboardManager clipboard = (android.content.ClipboardManager)SettingsActivity.this.getSystemService(android.content.Context.CLIPBOARD_SERVICE);
          		    android.content.ClipData clip = android.content.ClipData.newPlainText("Blockchain identifier", guid);
          		    clipboard.setPrimaryClip(clip);
         			Toast.makeText(SettingsActivity.this, "Identifier copied to clipboard", Toast.LENGTH_LONG).show();

        			return true;
        		}
        	});

        	Preference fiatPref = (Preference) findPreference("fiat");
        	fiatPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
        		public boolean onPreferenceClick(Preference preference) {
        	    	Intent intent = new Intent(SettingsActivity.this, CurrencySelector.class);
        			startActivity(intent);
        			return true;
        		}
        	});
        	
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	String res = null;
    	if(data != null)	{
    		if(data.getAction() != null)	{
    			res = data.getAction();
    		}
    	}

    }

}

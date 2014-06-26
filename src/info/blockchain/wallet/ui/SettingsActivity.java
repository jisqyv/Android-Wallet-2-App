package info.blockchain.wallet.ui;

import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.util.Date;

import piuk.blockchain.android.MyRemoteWallet;
import piuk.blockchain.android.Constants;
import piuk.blockchain.android.R;
import piuk.blockchain.android.WalletApplication;
import piuk.blockchain.android.util.Iso8601Format;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.net.Uri;
import android.os.Bundle;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.preference.Preference;
import android.preference.CheckBoxPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.Preference.OnPreferenceChangeListener;
import android.view.KeyEvent;
import android.view.View;
//import android.util.Log;
import android.widget.Toast;

public class SettingsActivity extends PreferenceActivity {

	private static final int DIALOG_EXPORT_KEYS = 1;

    /** Called when the activity is first created. */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setTitle(R.string.app_name);
        	addPreferencesFromResource(R.xml.settings);

        	SharedPreferences sp = getPreferenceScreen().getSharedPreferences();
//        	final String guid = WalletUtil.getInstance(this, this).getRemoteWallet().getGUID();
        	WalletApplication application = (WalletApplication)getApplication();
        	final String guid = application.getRemoteWallet().getGUID();

        	Preference guidPref = (Preference) findPreference("guid");
            guidPref.setSummary(guid);
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
        	
        	Preference backupPref = (Preference) findPreference("backup");
        	backupPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
        		public boolean onPreferenceClick(Preference preference) {
        			showDialog(DIALOG_EXPORT_KEYS);
        			return true;
        		}
        	});
        	
        	final Activity setupActivity = this;
        	
        	Preference unpairPref = (Preference) findPreference("unpair");
        	unpairPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
        		public boolean onPreferenceClick(Preference preference) {
					AlertDialog.Builder builder = new AlertDialog.Builder(setupActivity);
					builder.setMessage(R.string.ask_you_sure_unpair)
					.setCancelable(false);

					AlertDialog alert = builder.create();

					alert.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.yes), new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							Editor edit = PreferenceManager.getDefaultSharedPreferences(SettingsActivity.this).edit();
							edit.clear();
							edit.commit();

				        	Intent intent = new Intent(SettingsActivity.this, SetupActivity.class);
							intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
				    		startActivity(intent);

							dialog.dismiss();
						}}); 

					alert.setButton(AlertDialog.BUTTON_NEGATIVE, getString(R.string.cancel), new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							
							dialog.dismiss();
						}});

					alert.show();
        			
        			return true;
        		}
        	});
        	
    }

	private Dialog createExportKeysDialog()
	{
		final View view = getLayoutInflater().inflate(R.layout.export_keys_dialog, null);

		final AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setInverseBackgroundForced(true);
		builder.setTitle(R.string.export_keys_dialog_title);
		builder.setView(view);
		builder.setPositiveButton(R.string.export_keys_dialog_button_export, new Dialog.OnClickListener()
		{
			public void onClick(final DialogInterface dialog, final int which)
			{
				exportPrivateKeys();
			}
		});

		builder.setNegativeButton(R.string.button_cancel, new Dialog.OnClickListener()
		{
			public void onClick(final DialogInterface dialog, final int which)
			{
				dialog.dismiss();
			}
		});


		final AlertDialog dialog = builder.create();

		return dialog;
	}
	
	private void mailPrivateKeys(final File file)
	{
		final Intent intent = new Intent(Intent.ACTION_SEND);
		intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.export_keys_dialog_mail_subject));
		intent.putExtra(Intent.EXTRA_TEXT,
				getString(R.string.export_keys_dialog_mail_text) + "\n\n" + String.format(Constants.WEBMARKET_APP_URL, getPackageName()) + "\n\n"
						+ Constants.SOURCE_URL + '\n');
		intent.setType("x-bitcoin/private-keys");
		intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
		startActivity(Intent.createChooser(intent, getString(R.string.export_keys_dialog_mail_intent_chooser)));
	}
	
	private void exportPrivateKeys()
	{
		WalletApplication application = WalletUtil.getInstance(this, this).getWalletApplication();

		if (application.getRemoteWallet() == null)
			return;

		try
		{
			Constants.EXTERNAL_WALLET_BACKUP_DIR.mkdirs();
			final File file = new File(Constants.EXTERNAL_WALLET_BACKUP_DIR, "wallet-"
					+ Iso8601Format.newDateFormat().format(new Date()) + ".aes.json");

			final Writer cipherOut = new FileWriter(file);
			cipherOut.write(application.getRemoteWallet().getPayload());
			cipherOut.close();

			final AlertDialog.Builder dialog = new AlertDialog.Builder(this).setInverseBackgroundForced(true).setMessage(
					getString(R.string.export_keys_dialog_success, file));
			dialog.setPositiveButton(R.string.export_keys_dialog_button_archive, new Dialog.OnClickListener()
			{
				public void onClick(final DialogInterface dialog, final int which)
				{
					mailPrivateKeys(file);
				}
			});
			dialog.setNegativeButton(R.string.button_dismiss, null);
			dialog.show();
		}
		catch (final Exception x)
		{
			new AlertDialog.Builder(this).setInverseBackgroundForced(true).setIcon(android.R.drawable.ic_dialog_alert)
			.setTitle(R.string.import_export_keys_dialog_failure_title)
			.setMessage(getString(R.string.export_keys_dialog_failure, x.getMessage())).setNeutralButton(R.string.button_dismiss, null)
			.show();

			x.printStackTrace();
		}
	}
	
	@Override
	protected Dialog onCreateDialog(final int id) {
		if (id == DIALOG_EXPORT_KEYS) {
			return createExportKeysDialog();
		} else {
			return null;
		}
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

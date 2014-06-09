package info.blockchain.wallet.ui;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.json.simple.JSONObject;
import org.spongycastle.util.encoders.Hex;

import com.google.android.gcm.GCMRegistrar;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.Patterns;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.CheckBox;
import android.widget.Toast;
import android.view.KeyEvent;
import android.view.View.OnKeyListener;
import android.text.method.KeyListener;
//import android.util.Log;

import piuk.EventListeners;
import piuk.MyRemoteWallet;
import piuk.MyWallet;
import piuk.blockchain.android.Constants;
import piuk.blockchain.android.R;
import piuk.blockchain.android.WalletApplication;
import piuk.blockchain.android.ui.AbstractWalletActivity;
import piuk.blockchain.android.ui.PinEntryActivity;
import piuk.blockchain.android.ui.SuccessCallback;

public class Setup2Activity extends Activity	{

	private Pattern emailPattern = Patterns.EMAIL_ADDRESS;
	public static final int PBKDF2Iterations = 2000;

    private AddressManager addressManager = null;

    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	    setContentView(R.layout.setup2);

	    setRequestedOrientation (ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        final EditText pin1 = ((EditText)findViewById(R.id.pin1));
        final EditText pin2 = ((EditText)findViewById(R.id.pin2));
        final EditText pin3 = ((EditText)findViewById(R.id.pin3));
        final EditText pin4 = ((EditText)findViewById(R.id.pin4));
        final EditText pin2_1 = ((EditText)findViewById(R.id.pin2_1));
        final EditText pin2_2 = ((EditText)findViewById(R.id.pin2_2));
        final EditText pin2_3 = ((EditText)findViewById(R.id.pin2_3));
        final EditText pin2_4 = ((EditText)findViewById(R.id.pin2_4));

        pin1.setOnKeyListener(new OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if(pin1.getText().length() == 1)
                    pin2.requestFocus();
                return false;
            }
        });

        pin2.setOnKeyListener(new OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if(pin2.getText().length() == 1)
                    pin3.requestFocus();
                return false;
            }
        });

        pin3.setOnKeyListener(new OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if(pin3.getText().length() == 1)
                    pin4.requestFocus();
                return false;
            }
        });

        pin4.setOnKeyListener(new OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if(pin4.getText().length() == 1)
                    pin2_1.requestFocus();
                return false;
            }
        });

        pin2_1.setOnKeyListener(new OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if(pin2_1.getText().length() == 1)
                    pin2_2.requestFocus();
                return false;
            }
        });

        pin2_2.setOnKeyListener(new OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if(pin2_2.getText().length() == 1)
                    pin2_3.requestFocus();
                return false;
            }
        });

        pin2_3.setOnKeyListener(new OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if(pin2_3.getText().length() == 1)
                    pin2_4.requestFocus();
                return false;
            }
        });

        final EditText password1 = ((EditText)findViewById(R.id.password1));
        final EditText password2 = ((EditText)findViewById(R.id.password2));

        final EditText email = ((EditText)findViewById(R.id.email));
        final EditText mobile = ((EditText)findViewById(R.id.mobile));

        final CheckBox backups = ((CheckBox)findViewById(R.id.embackups));
        final CheckBox smsalerts = ((CheckBox)findViewById(R.id.smsalerts));

        final EditText label = ((EditText)findViewById(R.id.label));
        
    	((EditText)findViewById(R.id.password1)).setVisibility(View.GONE);
    	((EditText)findViewById(R.id.password2)).setVisibility(View.GONE);


		WalletApplication application = (WalletApplication)this.getApplication();
		MyRemoteWallet remoteWallet = application.getRemoteWallet();
    	List<String> activeAddresses = Arrays.asList(remoteWallet.getActiveAddresses());		
    	final String firstAddress = activeAddresses.get(0);
        addressManager = new AddressManager(remoteWallet, application, this);        

        
        Button confirm = ((Button)findViewById(R.id.confirm));
        confirm.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {

//        		Toast.makeText(Setup2Activity.this, password1.getText().toString() + " " + password2.getText().toString(), Toast.LENGTH_LONG).show();

            	final String pw1 = password1.getText().toString();
            	final String pw2 = password2.getText().toString();
            	final String em = email.getText().toString();
            	final String pinCode1 = pin1.getText().toString() + pin2.getText().toString() + pin3.getText().toString() + pin4.getText().toString();
            	final String pinCode2 = pin2_1.getText().toString() + pin2_2.getText().toString() + pin2_3.getText().toString() + pin2_4.getText().toString();
				
				boolean doBackups = backups.isChecked();
				boolean doSMS = smsalerts.isChecked();

            	String firstLabel = label.getText().toString();

				if(pw1.length() < 11 || pw1.length() > 255 || pw1.length() < 11 || pw1.length() > 255) {
					Toast.makeText(Setup2Activity.this, R.string.new_account_password_length_error, Toast.LENGTH_LONG).show();
					return;
				}

				if(!pw1.equals(pw2)) {
					Toast.makeText(Setup2Activity.this, R.string.new_account_password_mismatch_error, Toast.LENGTH_LONG).show();
					return;
				}

				if(em.length() > 0 && !emailPattern.matcher(em).matches()) {
					Toast.makeText(Setup2Activity.this, R.string.new_account_password_invalid_email, Toast.LENGTH_LONG).show();
					return;
				}

				if(pinCode1.length() != 4 || pinCode2.length() != 4) {
					Toast.makeText(Setup2Activity.this, "PIN code(s) invalid", Toast.LENGTH_LONG).show();
					return;
				}

				if(!pinCode1.equals(pinCode2)) {
					Toast.makeText(Setup2Activity.this, "PIN codes do not match", Toast.LENGTH_LONG).show();
					return;
				}
            	
				if(pinCode1.equals("0000")) {
					Toast.makeText(Setup2Activity.this, "0000 is not a valid PIN code", Toast.LENGTH_LONG).show();
					return;
				}
				
				
				//
				// 
				//
            	
				if (firstLabel != null && firstLabel.length() > 0) {
		    		addressManager.setAddressLabel(firstAddress, firstLabel, new Runnable() {
						public void run() {
							Log.d("setAddressLabel", "setAddressLabel " + R.string.toast_error_syncing_wallet);								
						}
					}, new Runnable() {
						public void run() {
							Log.d("setAddressLabel", "setAddressLabel " + R.string.error_setting_label);								
						}
					}, new Runnable() {
						public void run() {
							Log.d("setAddressLabel", "setAddressLabel " + R.string.toast_error_syncing_wallet);								
						}
					});
				}

			
				// upon success:
            	// save "verified" = true to prefs
            	
            	
            	


            }
        });

    }
}

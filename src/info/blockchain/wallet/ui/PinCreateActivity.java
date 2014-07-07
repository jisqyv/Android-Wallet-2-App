package info.blockchain.wallet.ui;

import java.security.SecureRandom;
import java.util.regex.Pattern;

import org.json.simple.JSONObject;
import org.spongycastle.util.encoders.Hex;

import com.dm.zbar.android.scanner.ZBarConstants;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ActivityInfo;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.util.Linkify;
import android.util.Log;
import android.util.Patterns;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnKeyListener;
import android.view.View.OnTouchListener;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ToggleButton;
import android.view.View.OnClickListener;
import android.widget.Toast;

import piuk.blockchain.android.MyRemoteWallet;
import piuk.blockchain.android.MyWallet;
import piuk.blockchain.android.R;
import piuk.blockchain.android.SuccessCallback;
import piuk.blockchain.android.WalletApplication;

public class PinCreateActivity extends Activity {

	private TextView tvHeader = null;
	private TextView tvFooter = null;
	private TextView tvWarning1 = null;
	private TextView tvWarning2 = null;

	private TextView tvTOS = null;

	private String strPIN = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

	    this.requestWindowFeature(Window.FEATURE_NO_TITLE);
//	    this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setContentView(R.layout.activity_pin_create2);

	    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

	    /*
        Bundle extras = getIntent().getExtras();
        if(extras != null)	{
            if(extras.getString("P") != null && extras.getString("P").length() == 4)	{
            	strPIN = extras.getString("P");
            }
            else	{
	        	Intent intent = new Intent(CreateWallet.this, SetupActivity.class);
				intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
	    		startActivity(intent);
            }
        }
        */

		tvHeader = (TextView)findViewById(R.id.header);
		tvHeader.setTypeface(TypefaceUtil.getInstance(this).getGravityLightTypeface());
		tvHeader.setText("create new wallet");

		tvFooter = (TextView)findViewById(R.id.footer);
		tvFooter.setText("It is very important that you enable the suggested security features. Enabling these features ensures you are always in full control of your bitcoin and your wallet at all times. This message will continue to display until you enable all suggested features.");

		tvWarning1 = (TextView)findViewById(R.id.warning1);
		tvWarning1.setText("Set your pin code");

		tvWarning2 = (TextView)findViewById(R.id.warning2);
		tvWarning2.setTextColor(0xFF039BD3);
		tvWarning2.setText("Enter a 4-digit code that will be easy for you to remember but not easily guessed by anyone else.");
		
        tvTOS = (TextView) findViewById(R.id.tos_text);
        tvTOS.setText("By tapping on 'ACCEPT', you acknowledge that you are accepting Blockchain's terms of service: https://blockchain.info");
        Linkify.addLinks(tvTOS, Linkify.WEB_URLS);

        final EditText pin1 = ((EditText)findViewById(R.id.pin1));
        final EditText pin2 = ((EditText)findViewById(R.id.pin2));
        final EditText pin3 = ((EditText)findViewById(R.id.pin3));
        final EditText pin4 = ((EditText)findViewById(R.id.pin4));

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

        pin2.addTextChangedListener(new TextWatcher()	{
        	public void afterTextChanged(Editable s) {
        		if(pin2.getText().toString().length() == 0) {
                    pin1.requestFocus();
        		}
        	}
        	public void beforeTextChanged(CharSequence s, int start, int count, int after)	{ ; }
        	public void onTextChanged(CharSequence s, int start, int before, int count)	{ ; }
        });

        pin3.setOnKeyListener(new OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if(pin3.getText().length() == 1)
                    pin4.requestFocus();
                return false;
            }
        });

        pin3.addTextChangedListener(new TextWatcher()	{
        	public void afterTextChanged(Editable s) {
        		if(pin3.getText().toString().length() == 0) {
                    pin2.requestFocus();
        		}
        	}
        	public void beforeTextChanged(CharSequence s, int start, int count, int after)	{ ; }
        	public void onTextChanged(CharSequence s, int start, int before, int count)	{ ; }
        });

        pin4.addTextChangedListener(new TextWatcher()	{
        	public void afterTextChanged(Editable s) {
        		if(pin4.getText().toString().length() == 0) {
                    pin3.requestFocus();
        		}
        	}
        	public void beforeTextChanged(CharSequence s, int start, int count, int after)	{ ; }
        	public void onTextChanged(CharSequence s, int start, int before, int count)	{ ; }
        });


	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {

		super.onActivityResult(requestCode, resultCode, data);

		/*
		if(resultCode == Activity.RESULT_OK && requestCode == EDIT_PASSWORD)	{



        }
		else if(resultCode == Activity.RESULT_OK && requestCode == EDIT_EMAIL) {
			


		}
		else {


		}
		*/
		
	}

}

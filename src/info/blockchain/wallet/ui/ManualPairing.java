package info.blockchain.wallet.ui;

import java.util.UUID;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import piuk.blockchain.android.R;
//import android.util.Log;

public class ManualPairing extends Activity	{

    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

	    this.requestWindowFeature(Window.FEATURE_NO_TITLE);
//	    this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
	    this.setContentView(R.layout.manual_pairing);

	    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
	    
        final EditText uuid = (EditText)findViewById(R.id.uuid);
        final EditText pw = (EditText)findViewById(R.id.pw);

        Button bOK = (Button)findViewById(R.id.ok);
        bOK.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {

            	String strUUID = uuid.getText().toString().trim();
            	String strPW = pw.getText().toString().trim();
            	
            	if(!validateGUID(strUUID)) {
    				Toast.makeText(ManualPairing.this, R.string.invalid_wallet_identifier, Toast.LENGTH_LONG).show();
    				return;
            	}

    			if(strPW.length() < 11 || strPW.length() > 255 || strPW.length() < 11 || strPW.length() > 255) {
    				Toast.makeText(ManualPairing.this, R.string.new_account_password_length_error, Toast.LENGTH_LONG).show();
    				return;
    			}
    			
            	setResult(RESULT_OK, (new Intent()).setAction(strUUID + strPW));
            	finish();

            }
        });

        Button bCancel = (Button)findViewById(R.id.cancel);
        bCancel.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
            	setResult(RESULT_CANCELED);
            	finish();
            }
        });
        
        InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        uuid.setFocusable(true);
        uuid.requestFocus();
        imm.showSoftInput(uuid, InputMethodManager.SHOW_FORCED);

    }

	public boolean validateGUID(String input_guid)	{

		if (input_guid == null)
			return false;

		if (input_guid.length() != 36)
			return false;

		try {
			input_guid = UUID.fromString(input_guid).toString();	// Check is valid uuid format
		} catch (IllegalArgumentException e) {
			return false;
		}
		
		return true;
	}

}

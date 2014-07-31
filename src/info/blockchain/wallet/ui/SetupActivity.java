package info.blockchain.wallet.ui;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.preference.PreferenceManager;
import android.content.SharedPreferences.Editor;
//import android.util.Log;

import piuk.blockchain.android.R;

public class SetupActivity extends Activity		{

    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	    
	    this.requestWindowFeature(Window.FEATURE_NO_TITLE);
//	    this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
	    this.setContentView(R.layout.setup);

	    setRequestedOrientation (ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
	    
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(SetupActivity.this);
		Editor edit = prefs.edit();
		edit.putBoolean("virgin", true);
		edit.commit();

        Button imgCreate = ((Button)findViewById(R.id.create));
        imgCreate.setTypeface(TypefaceUtil.getInstance(this).getGravityBoldTypeface());
        imgCreate.setTextColor(0xFF1B8AC7);
        imgCreate.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
    			Intent intent = new Intent(SetupActivity.this, info.blockchain.wallet.ui.PinCreateActivity.class);
    			startActivity(intent);
            }
        });

        Button imgPair = ((Button)findViewById(R.id.pair));
        imgPair.setTypeface(TypefaceUtil.getInstance(this).getGravityLightTypeface());
        imgPair.setTextColor(0xFF808080);
        imgPair.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
//    			Intent intent = new Intent(SetupActivity.this, info.blockchain.wallet.ui.PairingActivity.class);
    			Intent intent = new Intent(SetupActivity.this, info.blockchain.wallet.ui.PairingHelp.class);
    			startActivity(intent);
            }
        });

    }

}

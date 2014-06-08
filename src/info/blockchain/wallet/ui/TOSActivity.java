package info.blockchain.wallet.ui;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.security.SecureRandom;

import org.json.simple.JSONObject;
import org.spongycastle.util.encoders.Hex;

import piuk.EventListeners;
import piuk.MyRemoteWallet;
import piuk.MyWallet;
import piuk.MyRemoteWallet.SendProgress;
import piuk.blockchain.android.Constants;
import piuk.blockchain.android.R;
import piuk.blockchain.android.WalletApplication;
import piuk.blockchain.android.ui.PinEntryActivity;
import piuk.blockchain.android.ui.SendCoinsActivity;
import piuk.blockchain.android.ui.SuccessCallback;
import piuk.blockchain.android.ui.dialogs.RequestPasswordDialog;

import com.google.android.gcm.GCMRegistrar;
import com.google.bitcoin.core.Address;
import com.google.bitcoin.core.ECKey;
import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.core.TransactionOutput;
import com.google.bitcoin.core.Wallet.SendRequest;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.ImageView;
import android.widget.Toast;
import android.text.util.Linkify;
//import android.util.Log;

public class TOSActivity extends Activity	{

	private TextView tvTOS =  null;

	public static final int PBKDF2Iterations = 2000;

    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
	    this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
	    this.setContentView(R.layout.activity_tos);

        Bundle extras = getIntent().getExtras();
        if(extras != null)	{
        	;
        }

        TextView tvTOS = (TextView) findViewById(R.id.tos);
        tvTOS.setText("By tapping on 'ACCEPT', you acknowledge that you are accepting Blockchain's terms of service:\nhttps://blockchain.info");
        Linkify.addLinks(tvTOS, Linkify.WEB_URLS);

        Button btOK = ((Button)findViewById(R.id.ok));
        btOK.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
            	/*
    			Intent intent = new Intent(TOSActivity.this, Setup2Activity.class);
    			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
    			startActivity(intent);
    			*/

    			Intent intent = new Intent(TOSActivity.this, SecureYourWalletActivity.class);
    			intent.putExtra("first", true);
    			startActivity(intent);

            }
        });

        Button btKO = ((Button)findViewById(R.id.ko));
        btKO.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
    			Intent intent = new Intent(TOSActivity.this, SetupActivity.class);
    			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
    			startActivity(intent);
            }
        });

    }
}

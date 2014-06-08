package info.blockchain.wallet.ui;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;
//import android.util.Log;

import piuk.blockchain.android.R;

public class SecureYourWalletActivity extends Activity	{

	private Button bSecure = null;
	
    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
//	    setContentView(R.layout.activity_secure);

	    this.requestWindowFeature(Window.FEATURE_NO_TITLE);
//	    this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
	    this.setContentView(R.layout.activity_secure);

	    boolean firstTime = false;
        Bundle extras = getIntent().getExtras();
        if(extras != null)	{
        	firstTime = extras.getBoolean("first");
        }

        TextView tvHeader = (TextView)findViewById(R.id.header);
        TextView tvText = (TextView)findViewById(R.id.text);
    	tvHeader.setTypeface(TypefaceUtil.getInstance(this).getGravityLightTypeface());
        if(firstTime)	{
        	tvHeader.setBackgroundColor(0xFFF8E586);
        	tvHeader.setText("Your Blockchain Wallet is Ready");
        	tvText.setText("You can instantly and immediately start sending and receiving Bitcoins using this wallet. However, we highly recommend securing your wallet by tapping the blue button below. Securing your wallet is quick, easy and provides a bunch of benefits.\n\n-Automatic backups of your Bitcoin balance\n-Secure your wallet with a custom PIN code\n-Access your wallet and funds from any device, anytime");
        }
        else	{
        	tvHeader.setBackgroundColor(0xFFFB5B59);
        	tvHeader.setText("Your Funds Are At Risk!");
        	tvText.setText("If you lose your phone your funds will also be lost forever. Please secure your wallet right now to enable automatic backups.");
        }

        bSecure = (Button)findViewById(R.id.secure);
        bSecure.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
    			Intent intent = new Intent(SecureYourWalletActivity.this, SecureYourWalletActivity.class);
    			intent.putExtra("first", false);
    			startActivity(intent);
            }
        });

    }

	@Override
	public boolean dispatchTouchEvent(MotionEvent event) {
	    Rect dialogBounds = new Rect();
	    getWindow().getDecorView().getHitRect(dialogBounds);

	    if(!dialogBounds.contains((int) event.getX(), (int) event.getY()) && event.getAction() == MotionEvent.ACTION_DOWN) {
	    	return false;
	    }
	    else {
		    return super.dispatchTouchEvent(event);
	    }
	}

}

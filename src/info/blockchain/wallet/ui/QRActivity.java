package info.blockchain.wallet.ui;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.android.Contents;
import com.google.zxing.client.android.encode.QRCodeEncoder;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.ImageView;
//import android.util.Log;

public class QRActivity extends Activity	{

	private ImageView ivQR = null;
	private TextView tvBTCAddress = null;

    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	    setContentView(R.layout.layout_qr_dialog);

	    String btcAddress = null;
        Bundle extras = getIntent().getExtras();
        if(extras != null)	{
        	btcAddress = extras.getString("BTC_ADDRESS");
        }
        
    	Bitmap bm = generateQRCode(btcAddress);
	    ivQR = (ImageView)findViewById(R.id.qr);
		ivQR.setImageBitmap(bm);
	    tvBTCAddress = (TextView)findViewById(R.id.btc_address);
	    tvBTCAddress.setText(btcAddress);

	    /*
        bOK = (Button)findViewById(R.id.confirm);
        bOK.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
            	
            	String strReceivingAddress = receivingAddressView.getEditableText().toString();
            	String strReceivingName = receivingNameView.getEditableText().toString();
            	boolean push_notifications = sPushNotifications.isChecked();
            	int currency = spCurrencies.getSelectedItemPosition();
	        	currencies = getResources().getStringArray(R.array.currencies);

	        	if(BitcoinAddressCheck.isValid(BitcoinAddressCheck.clean(strReceivingAddress))) {
		            editor.putString("receiving_address", strReceivingAddress);
		            editor.putString("receiving_name", strReceivingName);
		            editor.putBoolean("push_notifications", push_notifications);
		            
		            if(currency == currencies.length - 1) {
			            editor.putString("currency", "ZZZ");
		            }
		            else {
			            editor.putString("currency", currencies[currency].substring(currencies[currency].length() - 3));
			            editor.remove("ocurrency");
			            strOtherCurrency = null;
		            }

		            editor.commit();
	            	finish();
	        	}
	        	else {
					Toast.makeText(SettingsActivity.this, R.string.invalid_btc_address, Toast.LENGTH_LONG).show();
	        	}

            }
        });

        bCancel = (Button)findViewById(R.id.cancel);
        bCancel.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
            	finish();
            }
        });
        
        */

    }
/*
	@Override
    public boolean onKeyDown(int keyCode, KeyEvent event) { 
        if(keyCode == KeyEvent.KEYCODE_BACK) {
            return true;
        }
        else	{
        	;
        }

        return false;
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
*/
    private Bitmap generateQRCode(String uri) {

        Bitmap bitmap = null;
        int qrCodeDimension = 440;

        QRCodeEncoder qrCodeEncoder = new QRCodeEncoder(uri, null, Contents.Type.TEXT, BarcodeFormat.QR_CODE.toString(), qrCodeDimension);

    	try {
            bitmap = qrCodeEncoder.encodeAsBitmap();
        } catch (WriterException e) {
            e.printStackTrace();
        }
    	
    	return bitmap;
    }

}

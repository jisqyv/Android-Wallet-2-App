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
    }

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

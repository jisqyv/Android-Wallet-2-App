package info.blockchain.wallet.ui;

import piuk.blockchain.android.R;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.android.Contents;
import com.google.zxing.client.android.encode.QRCodeEncoder;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.ImageView;
//import android.util.Log;
import android.widget.Toast;

public class TxActivity extends Activity	{

	private TextView tvLabelConfirmations = null;
	private TextView tvLabelAmount = null;
	private TextView tvLabelFee = null;
	private TextView tvLabelTx = null;
	private TextView tvValueConfirmations = null;
	private TextView tvValueAmount = null;
	private TextView tvValueFee = null;
	private TextView tvValueTx = null;
	
	private String strTxHash = null;
	private boolean isSending = false;
	private String strResult = null;
	private long ts = 0L;

    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	    setContentView(R.layout.layout_tx);

        Bundle extras = getIntent().getExtras();
        if(extras != null)	{
        	strTxHash = extras.getString("TX");
        	isSending = extras.getBoolean("SENDING");
        	strResult = extras.getString("RESULT");
        	ts = extras.getLong("TS");
        }

        tvLabelConfirmations = (TextView)findViewById(R.id.confirm_label);
        tvLabelAmount = (TextView)findViewById(R.id.amount_label);
        tvLabelFee = (TextView)findViewById(R.id.fee_label);
        tvLabelTx = (TextView)findViewById(R.id.tx_label);
        tvValueConfirmations = (TextView)findViewById(R.id.confirm_value);
        tvValueAmount = (TextView)findViewById(R.id.amount_value);
        tvValueFee = (TextView)findViewById(R.id.fee_value);
        tvValueTx = (TextView)findViewById(R.id.tx_value);

        tvLabelConfirmations.setText("Confirmations");
        if(isSending)	{
            tvLabelAmount.setText("Amount sent");
        }
        else	{
            tvLabelAmount.setText("Amount received");
        }
        tvLabelFee.setText("Transaction fee");
        tvLabelTx.setText("Transaction hash");

        tvValueConfirmations.setText("");
        tvValueAmount.setText(strResult + " BTC");
        tvValueFee.setText("");
        tvValueTx.setText(strTxHash);
        
    	tvValueTx.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
            	
      			android.content.ClipboardManager clipboard = (android.content.ClipboardManager)TxActivity.this.getSystemService(android.content.Context.CLIPBOARD_SERVICE);
      		    android.content.ClipData clip = android.content.ClipData.newPlainText("Hash", strTxHash);
      		    clipboard.setPrimaryClip(clip);
     			Toast.makeText(TxActivity.this, "Hash copied to clipboard", Toast.LENGTH_LONG).show();

                return false;
            }
        });
    }
}

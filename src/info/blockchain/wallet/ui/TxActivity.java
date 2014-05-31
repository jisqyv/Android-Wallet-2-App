package info.blockchain.wallet.ui;

import android.app.Activity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.TextView;
import android.widget.LinearLayout;
import android.os.AsyncTask;
import android.widget.Toast;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import info.blockchain.api.LatestBlock;
import info.blockchain.api.Transaction;

import piuk.blockchain.android.R;

public class TxActivity extends Activity	{

	private TextView tvLabelConfirmations = null;
	private TextView tvLabelAmount = null;
	private TextView tvLabelFee = null;
	private TextView tvLabelTx = null;
	private TextView tvValueConfirmations = null;
	private TextView tvValueAmount = null;
	private TextView tvValueFee = null;
	private TextView tvValueTx = null;
	private TextView tvResult = null;
	private TextView tvTS = null;
	private TextView tvFrom = null;
	private TextView tvTo = null;
	
	private String strTxHash = null;
	private boolean isSending = false;
	private String strResult = null;
	private long height = -1L;
	private long latest_block = -1L;
	private long ts = 0L;
	
	private LatestBlock latestBlock = null;
	private Transaction transaction = null;

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
        
        latestBlock = new LatestBlock();
        transaction = new Transaction(strTxHash);

        tvLabelConfirmations = (TextView)findViewById(R.id.confirm_label);
        tvLabelAmount = (TextView)findViewById(R.id.amount_label);
        tvLabelFee = (TextView)findViewById(R.id.fee_label);
        tvLabelTx = (TextView)findViewById(R.id.tx_label);
        tvValueConfirmations = (TextView)findViewById(R.id.confirm_value);
        tvValueAmount = (TextView)findViewById(R.id.amount_value);
        tvValueFee = (TextView)findViewById(R.id.fee_value);
        tvValueTx = (TextView)findViewById(R.id.tx_value);
        tvResult = (TextView)findViewById(R.id.result);
        tvTS = (TextView)findViewById(R.id.ts);
        tvFrom = (TextView)findViewById(R.id.from);
        tvTo = (TextView)findViewById(R.id.to);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssz");
        tvTS.setText(sdf.format(new Date(ts * 1000)));
        
        tvLabelConfirmations.setText("Confirmations");
        if(isSending)	{
            tvLabelAmount.setText("Amount sent");
            tvResult.setText("SENT " + strResult + " BTC");
            tvResult.setBackgroundResource(R.drawable.rounded_view_red);
            tvTS.setTextColor(getResources().getColor(R.color.blockchain_red));
            tvFrom.setTextColor(getResources().getColor(R.color.blockchain_red));
            tvTo.setTextColor(getResources().getColor(R.color.blockchain_red));
            ((LinearLayout)findViewById(R.id.div1)).setBackgroundResource(R.color.blockchain_red);
            ((LinearLayout)findViewById(R.id.div2)).setBackgroundResource(R.color.blockchain_red);
            ((LinearLayout)findViewById(R.id.div3)).setBackgroundResource(R.color.blockchain_red);
            ((LinearLayout)findViewById(R.id.div4)).setBackgroundResource(R.color.blockchain_red);
        }
        else	{
            tvLabelAmount.setText("Amount received");
            tvResult.setText("RECEIVED " + strResult + " BTC");
            tvResult.setBackgroundResource(R.drawable.rounded_view_green);
            tvFrom.setTextColor(getResources().getColor(R.color.blockchain_green));
            tvTo.setTextColor(getResources().getColor(R.color.blockchain_green));
            tvTS.setTextColor(getResources().getColor(R.color.blockchain_green));
            ((LinearLayout)findViewById(R.id.div1)).setBackgroundResource(R.color.blockchain_green);
            ((LinearLayout)findViewById(R.id.div2)).setBackgroundResource(R.color.blockchain_green);
            ((LinearLayout)findViewById(R.id.div3)).setBackgroundResource(R.color.blockchain_green);
            ((LinearLayout)findViewById(R.id.div4)).setBackgroundResource(R.color.blockchain_green);
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
    	
        DownloadTask task = new DownloadTask();
        task.execute(new String[] { transaction.getUrl(), latestBlock.getUrl() });
    }
    
    private class DownloadTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... urls) {

          String responseTx = "";
          String responseBlock = "";

          for (int i = 0; i < urls.length; i++) {
        	String url = urls[i];  
            DefaultHttpClient client = new DefaultHttpClient();
            HttpGet httpGet = new HttpGet(url);
            try {
              HttpResponse execute = client.execute(httpGet);
              InputStream content = execute.getEntity().getContent();

              BufferedReader buffer = new BufferedReader(new InputStreamReader(content));
              String s = "";
              while ((s = buffer.readLine()) != null) {
            	  if(i == 0) {
                      responseTx += s;
            	  }
            	  else {
                      responseBlock += s;
            	  }
              }

            } catch (Exception e) {
              e.printStackTrace();
            }
          }

          return responseTx + "\\|" + responseBlock;
        }

        @Override
        protected void onPostExecute(String result) {
        	Log.d("TxActivity", result);
        	
        	String[] results = result.split("\\|");

        	transaction.setData(results[0]);
        	transaction.parse();
        	height = transaction.getHeight();
        	Log.d("TxActivity", "Latest block:" + latest_block);
        	Log.d("TxActivity", "Height:" + height);

        	tvValueFee.setText(BlockchainUtil.formatBitcoin(BigInteger.valueOf(transaction.getFee())) + " BTC");

        	latestBlock.setData(results[1]);
        	latestBlock.parse();
        	latest_block = latestBlock.getLatestBlock();
        	Log.d("TxActivity", "Latest block:" + latest_block);
        	Log.d("TxActivity", "Height:" + height);

        	if(height > 01L && latest_block > 0L) {
            	tvValueConfirmations.setText(Long.toString((latest_block - height) + 1));
        	}

        }

      }

}

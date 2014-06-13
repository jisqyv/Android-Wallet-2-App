package info.blockchain.wallet.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.LinearLayout;
import android.os.AsyncTask;
import android.widget.ImageView;
import android.widget.Toast;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import info.blockchain.api.LatestBlock;
import info.blockchain.api.Transaction;
import info.blockchain.api.Transaction.xPut;
import piuk.Hash;
import piuk.MyRemoteWallet;
import piuk.blockchain.android.R;
import piuk.blockchain.android.WalletApplication;
import piuk.blockchain.android.ui.SuccessCallback;

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
	private TextView tvFromAddress = null;
	private TextView tvToAddress = null;

	private TextView tvNoteLabel = null;
	private TextView tvValueNote = null;

	private ImageView ivFromAddress = null;
	private ImageView ivToAddress = null;
	private LinearLayout txNoteRowLayout = null;

	private String strTxHash = null;
	private boolean isSending = false;
	private String strResult = null;
	private long height = -1L;
	private long latest_block = -1L;
	private long ts = 0L;
	
	private LatestBlock latestBlock = null;
	private Transaction transaction = null;

	private Map<String,String> labels = null;

	private AddressManager addressManager = null;
	private MyRemoteWallet remoteWallet = null;
	private WalletApplication application = null;
	private boolean isDialogDisplayed = false;

    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	    setContentView(info.blockchain.wallet.ui.R.layout.layout_tx);

        Bundle extras = getIntent().getExtras();
        if(extras != null)	{
        	strTxHash = extras.getString("TX");
        	isSending = extras.getBoolean("SENDING");
        	strResult = extras.getString("RESULT");
        	ts = extras.getLong("TS");
        }

		labels = WalletUtil.getInstance(this,  this).getRemoteWallet().getLabelMap();

		application = WalletUtil.getInstance(this, this).getWalletApplication();
		remoteWallet =  WalletUtil.getInstance(this, this).getRemoteWallet();
        addressManager = new AddressManager(remoteWallet, application, this);        

		
        latestBlock = new LatestBlock();
        transaction = new Transaction(strTxHash);

        ivFromAddress = (ImageView)findViewById(R.id.add_address_from);
        ivFromAddress.setVisibility(View.INVISIBLE);
        ivToAddress = (ImageView)findViewById(R.id.add_address_to);
        ivToAddress.setVisibility(View.INVISIBLE);

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
        tvFromAddress = (TextView)findViewById(R.id.from_address);
        tvToAddress = (TextView)findViewById(R.id.to_address);

        tvNoteLabel = (TextView)findViewById(R.id.tx_note_label);
        tvValueNote = (TextView)findViewById(R.id.tx_note_value);
        tvNoteLabel.setText("Transaction Note");
        tvValueNote.setText(remoteWallet.getTxNote(strTxHash));
        txNoteRowLayout = (LinearLayout)findViewById(R.id.txNoteRowLayout);
        txNoteRowLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            	promptDialogForAddNoteToTx();
            }
        });
        
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
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
    
    private void promptDialogForAddNoteToTx() {
       	if (isDialogDisplayed)
    		return;
       	
    	AlertDialog.Builder alert = new AlertDialog.Builder(TxActivity.this);

		alert.setTitle(R.string.edit_note);
		alert.setMessage(R.string.enter_note_below);

		final EditText input = new EditText(TxActivity.this);
		input.setHint(remoteWallet.getTxNote(strTxHash));
		alert.setView(input);

		String txNote = remoteWallet.getTxNote(strTxHash);
		String alertPositiveButtonText;
		if (txNote == null) 
			alertPositiveButtonText = getString(R.string.add);
		else 
			alertPositiveButtonText = getString(R.string.update);
			
		alert.setPositiveButton(alertPositiveButtonText, new DialogInterface.OnClickListener() {
		public void onClick(DialogInterface dialog, int whichButton) {
			final DialogInterface d = dialog;
			final String note = input.getText().toString();
   			try {
   				if (remoteWallet.addTxNote(strTxHash, note)) {
   					application.saveWallet(new SuccessCallback() {
   						@Override
   						public void onSuccess() {
   	   	 	 	 	        tvValueNote.setText(note);
   	 	 	 				d.dismiss();
   	 	 	 				isDialogDisplayed = false;
   	 	 	 				Toast.makeText(TxActivity.this.getApplication(),
   									R.string.note_saved,
   									Toast.LENGTH_SHORT).show();
   						}

   						@Override
   						public void onFail() { 									
   	   	 	 	 	        tvValueNote.setText(note);
   	 	 	 				d.dismiss();
   	 	 	 				isDialogDisplayed = false;
   							Toast.makeText(TxActivity.this.getApplication(),
   									R.string.toast_error_syncing_wallet,
   									Toast.LENGTH_SHORT).show();
   						}
   					});
   				} 						 	         			
   			} catch (Exception e) {
   	 			Toast.makeText(TxActivity.this, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
   				e.printStackTrace();
   			} 	   			   
		  }
		});

		alert.setNegativeButton(R.string.delete, new DialogInterface.OnClickListener() {
 			  public void onClick(DialogInterface dialog, int whichButton) {
 					final DialogInterface d = dialog;
 	   				if (remoteWallet.deleteTxNote(strTxHash)) {
 	   					application.saveWallet(new SuccessCallback() {
 	   						@Override
 	   						public void onSuccess() {
 	   	   	 	 	 	        tvValueNote.setText(null);
 	   	 	 	 				d.dismiss();
 	   	 	 	 				isDialogDisplayed = false;
 	   							Toast.makeText(TxActivity.this.getApplication(),
 	   									R.string.note_deleted,
 	   									Toast.LENGTH_SHORT).show();
 	   						}

 	   						@Override
 	   						public void onFail() { 									
 	   	   	 	 	 	        tvValueNote.setText(null);
 	   	 	 	 				d.dismiss();
 	   	 	 	 				isDialogDisplayed = false;
 	   							Toast.makeText(TxActivity.this.getApplication(),
 	   									R.string.toast_error_syncing_wallet,
 	   									Toast.LENGTH_SHORT).show();
 	   						}
 	   					});
 	   				} 	 				  
 			  }
		});

		alert.setOnCancelListener(new DialogInterface.OnCancelListener() {         
		    @Override
		    public void onCancel(DialogInterface dialog) {
	 				isDialogDisplayed = false;
		    }
		});
		
    	isDialogDisplayed = true;
		alert.show();  
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
        
        private void promptDialogForAddToAddressBook(final String address) {
        	if (isDialogDisplayed)
        		return;
           	
        	AlertDialog.Builder alert = new AlertDialog.Builder(TxActivity.this);

 			alert.setTitle(R.string.add_to_address_book);
 			alert.setMessage(R.string.set_label_below);

 			// Set an EditText view to get user input 
 			final EditText input = new EditText(TxActivity.this);
 			alert.setView(input);

 			alert.setPositiveButton(R.string.add, new DialogInterface.OnClickListener() {
 			public void onClick(DialogInterface dialog, int whichButton) {
 	   			  String label = input.getText().toString();
 	 				if (addressManager.canAddAddressBookEntry(address, label)) {
 						addressManager.handleAddAddressBookEntry(address, label);
 	         			Toast.makeText(TxActivity.this, R.string.added_to_address_book, Toast.LENGTH_LONG).show();
 	 				} else {
 	 		    		Toast.makeText(TxActivity.this, R.string.address_already_exist, Toast.LENGTH_LONG).show();
 	 				}			

 	 				dialog.dismiss();
 	 				isDialogDisplayed = false;
			  }
 			});

 			alert.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
 	 			  public void onClick(DialogInterface dialog, int whichButton) {
 						dialog.dismiss();
 	 	 				isDialogDisplayed = false;
 	 			  }
 			});

        	isDialogDisplayed = true;
 			alert.show();  
        }
        
        @Override
        protected void onPostExecute(String result) {
        	
        	String[] results = result.split("\\|");

        	transaction.setData(results[0]);
        	transaction.parse();
        	height = transaction.getHeight();

        	tvValueFee.setText(BlockchainUtil.formatBitcoin(BigInteger.valueOf(transaction.getFee())) + " BTC");
        	
        	String from;
        	String to = null;
        	if(labels.get(transaction.getInputs().get(0).addr) != null) {
        		from = labels.get(transaction.getInputs().get(0).addr);
                ivFromAddress.setVisibility(View.GONE);
        	}
        	else {
        		from = transaction.getInputs().get(0).addr;
        		final String address = from;
        		ivFromAddress.setVisibility(View.VISIBLE);
                ivFromAddress.setOnTouchListener(new OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                    	promptDialogForAddToAddressBook(address);            			
                        return true;
                    }
                });

        	}
        	if(from.length() > 25) {
        		from = from.substring(0, 25) + "...";
        	}

        	if(labels.get(transaction.getOutputs().get(0).addr) != null) {
        		to = labels.get(transaction.getOutputs().get(0).addr);
        		ivToAddress.setVisibility(View.GONE);
        	}
        	else {
        		to = transaction.getOutputs().get(0).addr;
        		final String address = to;
        		ivToAddress.setVisibility(View.VISIBLE);
                ivToAddress.setOnTouchListener(new OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                    	promptDialogForAddToAddressBook(address);            			
                        return false;
                    }
                });

        	}
        	if(to.length() > 25) {
        		to = to.substring(0, 25) + "...";
        	}

        	tvFromAddress.setText(from);
        	tvFromAddress.setOnTouchListener(new OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                	
          			android.content.ClipboardManager clipboard = (android.content.ClipboardManager)TxActivity.this.getSystemService(android.content.Context.CLIPBOARD_SERVICE);
          		    android.content.ClipData clip = android.content.ClipData.newPlainText("Address", transaction.getInputs().get(0).addr);
          		    clipboard.setPrimaryClip(clip);
         			Toast.makeText(TxActivity.this, "Address copied to clipboard", Toast.LENGTH_LONG).show();

                    return false;
                }
            });
        	tvToAddress.setText(to);
        	tvToAddress.setOnTouchListener(new OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                	
          			android.content.ClipboardManager clipboard = (android.content.ClipboardManager)TxActivity.this.getSystemService(android.content.Context.CLIPBOARD_SERVICE);
          		    android.content.ClipData clip = android.content.ClipData.newPlainText("Address", transaction.getOutputs().get(0).addr);
          		    clipboard.setPrimaryClip(clip);
         			Toast.makeText(TxActivity.this, "Address copied to clipboard", Toast.LENGTH_LONG).show();

                    return false;
                }
            });
        	
        	latestBlock.setData(results[1]);
        	latestBlock.parse();
        	latest_block = latestBlock.getLatestBlock();

        	if(height > 01L && latest_block > 0L) {
            	tvValueConfirmations.setText(Long.toString((latest_block - height) + 1));
        	}

        }

      }

}

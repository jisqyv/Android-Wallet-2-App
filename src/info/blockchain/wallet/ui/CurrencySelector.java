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
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import android.widget.AdapterView.OnItemSelectedListener;
//import android.util.Log;

import piuk.blockchain.android.R;

public class CurrencySelector extends Activity	{

	private SelectedSpinner spCurrencies = null;
	private String[] currencies = null;
	private Button bOK = null;
	private Button bCancel = null;
    private ArrayAdapter<CharSequence> spAdapter = null;
	
	private SharedPreferences prefs = null;
    private SharedPreferences.Editor editor = null;

    private static boolean displayOthers = false;
	private String strOtherCurrency = null;

	private static int OTHER_CURRENCY_ACTIVITY = 1;

    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	    setContentView(R.layout.activity_currency);
	    
        Bundle extras = getIntent().getExtras();
        if(extras != null)	{
        	strOtherCurrency = extras.getString("ocurrency");
        }

        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        editor = prefs.edit();

        OtherCurrencyExchange.getInstance(this);

        spCurrencies = (SelectedSpinner)findViewById(R.id.receive_coins_default_currency);
        spAdapter = ArrayAdapter.createFromResource(this, R.array.currencies, android.R.layout.simple_spinner_item);
    	spAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item); 
    	spCurrencies.setAdapter(spAdapter);

    	spCurrencies.setOnItemSelectedListener(new OnItemSelectedListener()	{
	    	public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3)	{
	    		if(!displayOthers && arg2 == spAdapter.getCount() - 1)	{
	    			displayOthers = true;
	        		Intent intent = new Intent(CurrencySelector.this, OtherCurrencyActivity.class);
	        		intent.putExtra("ocurrency", strOtherCurrency);
	        		startActivityForResult(intent, OTHER_CURRENCY_ACTIVITY);
	    		}
	    	}
	        public void onNothingSelected(AdapterView<?> arg0) {
	        	;
	        }
    	});

        bOK = (Button)findViewById(R.id.confirm);
        bOK.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
            	
            	int currency = spCurrencies.getSelectedItemPosition();
	        	currencies = getResources().getStringArray(R.array.currencies);

	            if(currency == currencies.length - 1) {
		            editor.putString("ccurrency", "ZZZ");
	            }
	            else {
		            editor.putString("ccurrency", currencies[currency].substring(currencies[currency].length() - 3));
		            editor.remove("ocurrency");
		            strOtherCurrency = null;
	            }

	            editor.commit();
            	finish();

            }
        });


        bCancel = (Button)findViewById(R.id.cancel);
        bCancel.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
            	finish();
            }
        });

        initValues();

    }

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		
		if(resultCode == Activity.RESULT_OK && requestCode == OTHER_CURRENCY_ACTIVITY) {
			if(data != null && data.getAction() != null && data.getAction().length() > 0) {
				String ocurrencyMsg = OtherCurrencyExchange.getInstance(this).getCurrencyNames().get(data.getAction()) + " - " + data.getAction();
	            Toast.makeText(this, ocurrencyMsg, Toast.LENGTH_LONG).show();
		        prefs = PreferenceManager.getDefaultSharedPreferences(this);
		        editor = prefs.edit();
	            editor.putString("ocurrency", data.getAction());
	            editor.commit();
	            strOtherCurrency = data.getAction();
			}
			else {
				;
			}
			displayOthers = false;
        }
		else if(resultCode == Activity.RESULT_CANCELED && requestCode == OTHER_CURRENCY_ACTIVITY) {
			displayOthers = false;
		}
        else {
        	;
        }

	}

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

    private void initValues() {
    	currencies = getResources().getStringArray(R.array.currencies);
    	String strCurrency = prefs.getString("ccurrency", "USD");
    	int sel = -1;
    	for(int i = 0; i < currencies.length; i++) {
    		if(currencies[i].endsWith(strCurrency)) {
    	        spCurrencies.setSelection(i);
    	        sel = i;
    	        break;
    		}
    	}
    	if(sel == -1) {
	        spCurrencies.setSelection(currencies.length - 1);
    	}

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

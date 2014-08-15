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
	private Button bOK = null;
	private Button bCancel = null;
    private ArrayAdapter<CharSequence> spAdapter = null;
	
	private SharedPreferences prefs = null;
    private SharedPreferences.Editor editor = null;
    
	private static String[] currencies = {
	    "United States Dollar - USD",
	    "Euro - EUR",
	    "British Pound Sterling - GBP",
	    "Indian Rupee - INR",
	    "Australian Dollar - AUD",
	    "Canadian Dollar - CAD",
	    "Arab Emirates Dirham - AED",
	    "Argentine Peso - ARS",
	    "Aruban Florin - AWG",
	    "Convertible Mark - BAM",
	    "Barbadian Dollar - BBD",
	    "Bangladeshi Taka - BDT",
	    "Bulgarian Lev - BGN",
	    "Bahraini Dinar - BHD",
	    "Bermudian Dollar - BMD",
	    "Bolivian Boliviano - BOB",
	    "Brazilian Real - BRL",
	    "Bahamian Dollar - BSD",
	    "Swiss Franc - CHF",
	    "Chilean Peso - CLP",
	    "Chinese Yuan - CNY",
	    "Colombian Peso - COP",
	    "Czech Koruna - CZK",
	    "Danish Krone - DKK",
	    "Dominican Peso - DOP",
	    "Egyptian Pound - EGP",
	    "Fijian Dollar - FJD",
	    "Ghana Cedi - GHS",
	    "Gambian Dalasi - GMD",
	    "Guatemalan Quetzal - GTQ",
	    "Hong Kong Dollar - HKD",
	    "Croatian Kuna - HRK",
	    "Hungarian Forint - HUF",
	    "Indonesian Rupiah - IDR",
	    "Israeli Sheqel - ILS",
	    "Icelandic Krona - ISK",
	    "Jamaican Dollar - JMD",
	    "Jordanian Dinar - JOD",
	    "Japanese Yen - JPY",
	    "Kenyan Shilling - KES",
	    "Cambodian Riel - KHR",
	    "South Korean Won - KRW",
	    "Kuwaiti Dinar - KWD",
	    "Lao Kip - LAK",
	    "Lebanese Pound - LBP",
	    "Sri Lankan Rupee - LKR",
	    "Lithuanian Litas - LTL",
	    "Moroccan Dirham - MAD",
	    "Moldovan Leu - MDL",
	    "Malagasy Ariary - MGA",
	    "Macedonian Denar - MKD",
	    "Mauritian Rupee - MUR",
	    "Maldivian Rufiyaa - MVR",
	    "Mexican Peso - MXN",
	    "Malaysian Ringgit - MYR",
	    "Namibian Dollar - NAD",
	    "Nigerian Naira - NGN",
	    "Norwegian Krone - NOK",
	    "Nepalese Rupee - NPR",
	    "New Zealand Dollar - NZD",
	    "Omani Rial - OMR",
	    "Panamanian Balboa - PAB",
	    "Peruvian Sol - PEN",
	    "Philippine Peso - PHP",
	    "Pakistani Rupee - PKR",
	    "Polish Zloty - PLN",
	    "Paraguayan Guaraní - PYG",
	    "Qatari Riyal - QAR",
	    "Romanian Leu - RON",
	    "Serbian Dinar - RSD",
	    "Russian Rouble - RUB",
	    "Saudi Riyal - SAR",
	    "Seychellois Rupee - SCR",
	    "Swedish Krona - SEK",
	    "Singapore Dollar - SGD",
	    "Syrian Pound - SYP",
	    "Thai Baht - THB",
	    "Tunisian Dinar - TND",
	    "Turkish Lira - TRY",
	    "Taiwanese Dollar - TWD",
	    "Ukraine Hryvnia - UAH",
	    "Ugandan Shilling - UGX",
	    "Uruguayan Peso - UYU",
	    "Venezuelan Bolívar - VEF",
	    "Vietnamese Dong - VND",
	    "Central African Franc - XAF",
	    "East Caribbean Dollar - XCD",
	    "West African Franc - XOF",
	    "CFP Franc - XPF",
	    "South African Rand - ZAR"
		};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	    setContentView(R.layout.activity_currency);
	    
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        editor = prefs.edit();

        OtherCurrencyExchange.getInstance(this);

        spCurrencies = (SelectedSpinner)findViewById(R.id.receive_coins_default_currency);
        spAdapter = new ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, currencies);
    	spCurrencies.setAdapter(spAdapter);

        bOK = (Button)findViewById(R.id.confirm);
        bOK.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
            	int currency = spCurrencies.getSelectedItemPosition();
	            editor.putString("ccurrency", currencies[currency].substring(currencies[currency].length() - 3));
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
	        spCurrencies.setSelection(0);
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

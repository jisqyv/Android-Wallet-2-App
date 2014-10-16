package info.blockchain.wallet.ui;

import java.security.SecureRandom;

import org.apache.commons.lang.RandomStringUtils;
import org.json.simple.JSONObject;
import org.spongycastle.util.encoders.Hex;

import com.google.android.gcm.GCMRegistrar;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.content.pm.ActivityInfo;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.util.Linkify;
//import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.View.OnKeyListener;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.CheckBox;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;
import android.widget.TextView.OnEditorActionListener;
import piuk.blockchain.android.Constants;
import piuk.blockchain.android.EventListeners;
import piuk.blockchain.android.MyWallet;
import piuk.blockchain.android.R;
import piuk.blockchain.android.SuccessCallback;
import piuk.blockchain.android.WalletApplication;

public class PinCreateActivity extends Activity {

	private TextView tvHeader = null;
	private TextView tvWarning1 = null;
	private TextView tvWarning2 = null;

	private TextView tvTOS = null;

	private LinearLayout tosLayout = null;
	private LinearLayout tosPinConfirm = null;

	private CheckBox cbAccept = null;

	private String strPIN = null;
	
    private EditText pin1 = null;
    private EditText pin2 = null;
    private EditText pin3 = null;
    private EditText pin4 = null;
    private EditText pin1_2 = null;
    private EditText pin2_2 = null;
    private EditText pin3_2 = null;
    private EditText pin4_2 = null;
    
    private TextWatcher twPin1 = null;
    private TextWatcher twPin2 = null;
    private TextWatcher twPin3 = null;
    private TextWatcher twPin4 = null;
    private TextWatcher twPin1_2 = null;
    private TextWatcher twPin2_2 = null;
    private TextWatcher twPin3_2 = null;
    private TextWatcher twPin4_2 = null;

    public static final int PBKDF2Iterations = 2000;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

	    this.requestWindowFeature(Window.FEATURE_NO_TITLE);
//	    this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
	    if(!DeviceUtil.getInstance(this).isSmallScreen()) {
			setContentView(R.layout.activity_pin_create2);
	    }
	    else {
			setContentView(R.layout.activity_pin_create_small);
	    }
	    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        cbAccept = (CheckBox)findViewById(R.id.tos_accept);

        tvTOS = (TextView)findViewById(R.id.tos_text);
        tvTOS.setText(R.string.accept_tos);
        Linkify.addLinks(tvTOS, Linkify.WEB_URLS);

        tosLayout = (LinearLayout) findViewById(R.id.tos);
    	tosLayout.setVisibility(View.INVISIBLE);

    	tosPinConfirm = (LinearLayout) findViewById(R.id.confirm_pin);
    	tosPinConfirm.setVisibility(View.INVISIBLE);

		tvHeader = (TextView)findViewById(R.id.header);
		tvHeader.setTypeface(TypefaceUtil.getInstance(this).getGravityLightTypeface());
		tvHeader.setText(R.string.create_new_wallet);

		tvWarning1 = (TextView)findViewById(R.id.warning1);
		tvWarning1.setText(R.string.set_your_pin);

		tvWarning2 = (TextView)findViewById(R.id.warning2);
		tvWarning2.setTextColor(0xFF039BD3);
		tvWarning2.setText(R.string.set_your_pin2);

        pin1 = ((EditText)findViewById(R.id.pin1));
        pin2 = ((EditText)findViewById(R.id.pin2));
        pin3 = ((EditText)findViewById(R.id.pin3));
        pin4 = ((EditText)findViewById(R.id.pin4));

        pin1_2 = ((EditText)findViewById(R.id.pin1_2));
        pin2_2 = ((EditText)findViewById(R.id.pin2_2));
        pin3_2 = ((EditText)findViewById(R.id.pin3_2));
        pin4_2 = ((EditText)findViewById(R.id.pin4_2));
        
        pin2.setVisibility(View.INVISIBLE);
        pin3.setVisibility(View.INVISIBLE);
        pin4.setVisibility(View.INVISIBLE);

        pin1_2.setVisibility(View.INVISIBLE);
        pin2_2.setVisibility(View.INVISIBLE);
        pin3_2.setVisibility(View.INVISIBLE);
        pin4_2.setVisibility(View.INVISIBLE);

        pin1.postDelayed(new Runnable() {
     	   @Override
     	   public void run() {
     	     InputMethodManager keyboard = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
     	     keyboard.showSoftInput(pin1, 0);
     	   }
   	   	}, 50);
        
        final long DELAY = 500;
        
        pin1.setOnKeyListener(new OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                return false;
            }
        });

        twPin1 = new TextWatcher()	{
        	public void afterTextChanged(Editable s) {
        		if(pin1.getText().toString().length() == 0)	{
        			resetPinDisplay();
        		}
        		else if(pin1.getText().toString().length() == 1) {
                    pin2.setVisibility(View.VISIBLE);
                    pin1.postDelayed(new Runnable() {
                  	   @Override
                  	   public void run() {
                         pin2.requestFocus();
                  	   }
                	}, DELAY);

                }
        	}
        	public void beforeTextChanged(CharSequence s, int start, int count, int after)	{
            }
        	public void onTextChanged(CharSequence s, int start, int before, int count)	{ 
            }
        };
        pin1.addTextChangedListener(twPin1);

        twPin2 = new TextWatcher()	{
        	public void afterTextChanged(Editable s) {
        		if(pin2.getText().toString().length() == 0)	{
        			resetPinDisplay();
        		}
                else if(pin2.getText().toString().length() == 1) {
                    pin3.setVisibility(View.VISIBLE);
                    pin2.postDelayed(new Runnable() {
                   	   @Override
                   	   public void run() {
                          pin3.requestFocus();
                   	   }
                 	}, DELAY);
                }
        	}
        	public void beforeTextChanged(CharSequence s, int start, int count, int after)	{
            }
        	public void onTextChanged(CharSequence s, int start, int before, int count)	{ 
            }
        };
        pin2.addTextChangedListener(twPin2);

        twPin3 = new TextWatcher()	{
        	public void afterTextChanged(Editable s) {
        		if(pin3.getText().toString().length() == 0)	{
        			resetPinDisplay();
        		}
                else if(pin3.getText().toString().length() == 1) {
                    pin4.setVisibility(View.VISIBLE);
                    pin3.postDelayed(new Runnable() {
                   	   @Override
                   	   public void run() {
                          pin4.requestFocus();
                   	   }
                 	}, DELAY);
                }
        	}
        	public void beforeTextChanged(CharSequence s, int start, int count, int after)	{
            }
        	public void onTextChanged(CharSequence s, int start, int before, int count)	{ 
            }
        };
        pin3.addTextChangedListener(twPin3);

        twPin4 = new TextWatcher()	{
        	public void afterTextChanged(Editable s) {
        		if(pin4.getText().toString().length() == 0)	{
        			resetPinDisplay();
        		}
                else if(pin4.getText().toString().length() == 1) {
                    pin1_2.setVisibility(View.VISIBLE);
                	tosLayout.setVisibility(View.VISIBLE);
                	tosPinConfirm.setVisibility(View.VISIBLE);
            		tvWarning2.setText(R.string.confirm_your_pin);
                    pin4.postDelayed(new Runnable() {
                   	   @Override
                   	   public void run() {
                          pin1_2.requestFocus();
                   	   }
                 	}, DELAY);
                }
        	}
        	public void beforeTextChanged(CharSequence s, int start, int count, int after)	{
            }
        	public void onTextChanged(CharSequence s, int start, int before, int count)	{ 
            }
        };
        pin4.addTextChangedListener(twPin4);

        twPin1_2 = new TextWatcher()	{
        	public void afterTextChanged(Editable s) {
        		if(pin1_2.getText().toString().length() == 0)	{
        			resetPinDisplay();
        		}
                else if(pin2.getText().toString().length() == 1) {
                    pin2_2.setVisibility(View.VISIBLE);
                    pin1_2.postDelayed(new Runnable() {
                   	   @Override
                   	   public void run() {
                          pin2_2.requestFocus();
                   	   }
                 	}, DELAY);
                }
        	}
        	public void beforeTextChanged(CharSequence s, int start, int count, int after)	{
            }
        	public void onTextChanged(CharSequence s, int start, int before, int count)	{ 
            }
        };
        pin1_2.addTextChangedListener(twPin1_2);

        twPin2_2 = new TextWatcher()	{
        	public void afterTextChanged(Editable s) {
        		if(pin2_2.getText().toString().length() == 0)	{
        			resetPinDisplay();
        		}
                else if(pin2_2.getText().toString().length() == 1) {
                    pin3_2.setVisibility(View.VISIBLE);
                    pin2_2.postDelayed(new Runnable() {
                   	   @Override
                   	   public void run() {
                          pin3_2.requestFocus();
                   	   }
                 	}, DELAY);
                }
        	}
        	public void beforeTextChanged(CharSequence s, int start, int count, int after)	{
            }
        	public void onTextChanged(CharSequence s, int start, int before, int count)	{ 
            }
        };
        pin2_2.addTextChangedListener(twPin2_2);

        twPin3_2 = new TextWatcher()	{
        	public void afterTextChanged(Editable s) {
        		if(pin3_2.getText().toString().length() == 0)	{
        			resetPinDisplay();
        		}
                else if(pin3_2.getText().toString().length() == 1) {
                    pin4_2.setVisibility(View.VISIBLE);
                    pin3_2.postDelayed(new Runnable() {
                   	   @Override
                   	   public void run() {
                          pin4_2.requestFocus();
                   	   }
                 	}, DELAY);
                }
        	}
        	public void beforeTextChanged(CharSequence s, int start, int count, int after)	{
            }
        	public void onTextChanged(CharSequence s, int start, int before, int count)	{ 
            }
        };
        pin3_2.addTextChangedListener(twPin3_2);

        twPin4_2 = new TextWatcher()	{
        	public void afterTextChanged(Editable s) {
        		if(pin4_2.getText().toString().length() == 0)	{
        			resetPinDisplay();
        		}
                else if(pin4_2.getText().toString().length() == 1) {
                	tosLayout.setVisibility(View.VISIBLE);
                }
        	}
        	public void beforeTextChanged(CharSequence s, int start, int count, int after)	{
            }
        	public void onTextChanged(CharSequence s, int start, int before, int count)	{ 
            }
        };
        pin4_2.addTextChangedListener(twPin4_2);

        pin4_2.setOnEditorActionListener(new OnEditorActionListener() {
		    @Override
		    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
		        if(actionId == EditorInfo.IME_ACTION_DONE) {
                	InputMethodManager imm = (InputMethodManager)PinCreateActivity.this.getSystemService(Context.INPUT_METHOD_SERVICE);
	                imm.hideSoftInputFromWindow(pin4_2.getWindowToken(), 0);
		        }
		        return false;
		    }
		});

        cbAccept.setOnClickListener(new OnClickListener() {
        	  @Override
        	  public void onClick(View v) {
          		if(cbAccept.isChecked()) {
          			final String p1 = pin1.getText().toString() +  pin2.getText().toString() +  pin3.getText().toString() +  pin4.getText().toString();
          			final String p2 = pin1_2.getText().toString() +  pin2_2.getText().toString() +  pin3_2.getText().toString() +  pin4_2.getText().toString();
          			
          			if(p1.equals("0000") || p2.equals("0000")) {
          				
          				resetPinDisplay();

						Toast.makeText(PinCreateActivity.this.getApplication(), R.string.zero_pin, Toast.LENGTH_SHORT).show();
						return;
          			}
          			
          			if(p1 != null && p2 != null && p1.length() == 4 && p2.length() == 4 && p1.equals(p2)) {

        				final ProgressDialog progressDialog = ProgressDialog.show(PinCreateActivity.this, "", getString(R.string.creating_account), true);
        				progressDialog.show();

        				final Handler handler = new Handler();

        				new Thread() {
        					@Override
        					public void run() {
        						
        						Looper.prepare();
        						
        						final WalletApplication application = WalletUtil.getInstance(PinCreateActivity.this).getWalletApplication();
        						try {
        							try {
        								application.generateNewWallet();

        							} catch (Exception e1) {

        								throw new Exception("Error Generating Wallet");

        							}

        							final String guid = application.getRemoteWallet().getGUID();
        							final String sharedKey = application.getRemoteWallet().getSharedKey();
        							final String pinCode = p1;
        							final String password = pinCode + pinCode + pinCode;

        							application.getRemoteWallet().setTemporyPassword(password);

        							if (!application.getRemoteWallet().remoteSave("")) {
        								throw new Exception("Unknown Error Inserting wallet");
        							}

        							EventListeners.invokeWalletDidChange();

        							handler.post(new Runnable() {
        								public void run() {

        									try {
        										progressDialog.dismiss();

//        										dismiss();

//        										final AbstractWalletActivity activity = (AbstractWalletActivity) getActivity();
        										Toast.makeText(PinCreateActivity.this.getApplication(), R.string.new_account_success, Toast.LENGTH_SHORT).show();

//        										PinEntryActivity.clearPrefValues(application);

        										final Editor edit = PreferenceManager.getDefaultSharedPreferences(application.getApplicationContext()).edit();
        										edit.putString("guid", guid);
        										edit.putString("sharedKey", sharedKey);

        										if (edit.commit()) {
        											handler.post(new Runnable() {

        												@Override
        												public void run() {

        													new Thread(new Runnable(){
        													    @Override
        													    public void run() {
        													    	
        															Looper.prepare();

        															//
        															// Save PIN
        															//
        													        try {
        																byte[] bytes = new byte[16];
        																SecureRandom random = new SecureRandom();
        																random.nextBytes(bytes);
        																final String key = new String(Hex.encode(bytes), "UTF-8");
        																random.nextBytes(bytes);
        																final String value = new String(Hex.encode(bytes), "UTF-8");
        																final JSONObject response = piuk.blockchain.android.ui.PinEntryActivity.apiStoreKey(key, value, pinCode);
        																if (response.get("success") != null) {

        																	edit.putString("pin_kookup_key", key);
        																	edit.putString("encrypted_password", MyWallet.encrypt(application.getRemoteWallet().getTemporyPassword(), value, PBKDF2Iterations));

        																	if (!edit.commit()) {
        																		throw new Exception("Error Saving Preferences");
        																	}
        																	else {
        																		TimeOutUtil.getInstance().updatePin();
        																		
        																		application.setTemporyPIN(pinCode);

        															        	Intent intent = new Intent(PinCreateActivity.this, SecureWallet.class);
        																		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        															    		startActivity(intent);
        																	}

        																}
        																else {
        																	Toast.makeText(application, response.toString(), Toast.LENGTH_SHORT).show();
        																}
        													        } catch (Exception e) {
        																Toast.makeText(application, e.getStackTrace().toString(), Toast.LENGTH_SHORT).show();
        													            e.printStackTrace();
        													        }
        															//
        															//
        															//
        													        
        															Looper.loop();

        													    }
        													}).start();

        													//
        													//
        													//
        													application.checkIfWalletHasUpdated(password, guid, sharedKey, true, new SuccessCallback(){

        														@Override
        														public void onSuccess() {	
//        															activity.registerNotifications();
        															
        															try {
        																final String regId = GCMRegistrar.getRegistrationId(PinCreateActivity.this);

        																if (regId == null || regId.equals("")) {
        																	GCMRegistrar.register(PinCreateActivity.this, Constants.SENDER_ID);
        																} else {
        																	application.registerForNotificationsIfNeeded(regId);
        																}

        															} catch (Exception e) {
        																e.printStackTrace();
        															}

        														}

        														@Override
        														public void onFail() {
        															Toast.makeText(application, R.string.toast_error_syncing_wallet, Toast.LENGTH_SHORT).show();
        														}
        													});
        												}
        											});
        										} else {
        											throw new Exception("Error saving preferences");
        										}
        									} catch (Exception e) {
        										e.printStackTrace();

        										application.clearWallet();

        										Toast.makeText(PinCreateActivity.this.getApplication(), e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
        									}
        								}
        							});
        						} catch (final Exception e) {
        							e.printStackTrace();

        							application.clearWallet();

        							handler.post(new Runnable() {
        								public void run() {
        									progressDialog.dismiss();

        									Toast.makeText(PinCreateActivity.this.getApplication(), e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
        								}
        							});
        						}

        						Looper.loop();

        					}
        				}.start();

          			}
          			else	{
          				resetPinDisplay();
        				Toast.makeText(PinCreateActivity.this, R.string.enter_matching_pins, Toast.LENGTH_LONG).show();
          			}

            	}
        	  }

          });


	}

	void resetPinDisplay()	{
		pin1.removeTextChangedListener(twPin1);
		pin2.removeTextChangedListener(twPin2);
		pin3.removeTextChangedListener(twPin3);
		pin4.removeTextChangedListener(twPin4);
		pin1_2.removeTextChangedListener(twPin1_2);
		pin2_2.removeTextChangedListener(twPin2_2);
		pin3_2.removeTextChangedListener(twPin3_2);
		pin4_2.removeTextChangedListener(twPin4_2);
		
		pin1.setText("");
		pin2.setText("");
		pin3.setText("");
		pin4.setText("");
		pin1_2.setText("");
		pin2_2.setText("");
		pin3_2.setText("");
		pin4_2.setText("");
		
		pin1.addTextChangedListener(twPin1);
		pin2.addTextChangedListener(twPin2);
		pin3.addTextChangedListener(twPin3);
		pin4.addTextChangedListener(twPin4);
		pin1_2.addTextChangedListener(twPin1_2);
		pin2_2.addTextChangedListener(twPin2_2);
		pin3_2.addTextChangedListener(twPin3_2);
		pin4_2.addTextChangedListener(twPin4_2);

        pin2.setVisibility(View.INVISIBLE);
        pin3.setVisibility(View.INVISIBLE);
        pin4.setVisibility(View.INVISIBLE);
        pin1_2.setVisibility(View.INVISIBLE);
        pin2_2.setVisibility(View.INVISIBLE);
        pin3_2.setVisibility(View.INVISIBLE);
        pin4_2.setVisibility(View.INVISIBLE);
        
		cbAccept.setChecked(false);
    	tosLayout.setVisibility(View.INVISIBLE);
    	tosPinConfirm.setVisibility(View.INVISIBLE);
    	tvWarning2.setText(R.string.set_your_pin2);

        pin1.requestFocus();
	}

}

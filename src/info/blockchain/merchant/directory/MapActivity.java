package info.blockchain.merchant.directory;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;

import android.app.ActionBar;
import android.app.Activity;
import android.app.ProgressDialog;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.View.OnTouchListener;
import android.text.util.Linkify;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.net.Uri;
import android.util.Log;

import piuk.blockchain.android.R;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.GoogleMap.InfoWindowAdapter;
import com.google.android.gms.maps.GoogleMap.OnInfoWindowClickListener;

public class MapActivity extends Activity implements LocationListener	{

	private GoogleMap map = null;
	private LocationManager locationManager = null;
	private String provider = null;
    private Location location = null;
    private Location currLocation = null;
	private static final long MIN_TIME = 400;
	private static final float MIN_DISTANCE = 1000;
	private Marker mSelf = null;

	private static final int HEADING_CAFE = 1;
	private static final int HEADING_BAR = 2;
	private static final int HEADING_RESTAURANT = 3;
	private static final int HEADING_SPEND = 4;
	private static final int HEADING_ATM = 5;
	
	private int color_category_selected = 0xffFFFFFF;
    private int color_category_unselected = 0xffF1F1F1;

    private ImageView imgCafe = null;
    private LinearLayout layoutCafe = null;
    private ImageView imgDrink = null;
    private LinearLayout layoutDrink = null;
    private ImageView imgEat = null;
    private LinearLayout layoutEat = null;
    private ImageView imgSpend = null;
    private LinearLayout layoutSpend = null;
    private ImageView imgATM = null;
    private LinearLayout layoutATM = null;
    
    private boolean cafeSelected = true;
    private boolean drinkSelected = true;
    private boolean eatSelected = true;
    private boolean spendSelected = true;
    private boolean atmSelected = true;

	private ProgressDialog progress = null;
	
	private double selfLat = 0.0;
	private double selfLng = 0.0;
	
	private HashMap<String,BTCBusiness> markerValues = null;
	
	private String strJSONData = null;
	public static ArrayList<BTCBusiness> btcb = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_map);

        ActionBar actionBar = getActionBar();
        actionBar.hide();
//        actionBar.setDisplayOptions(actionBar.getDisplayOptions() | ActionBar.DISPLAY_SHOW_CUSTOM);
        actionBar.setDisplayOptions(actionBar.getDisplayOptions() ^ ActionBar.DISPLAY_SHOW_TITLE);
        actionBar.setLogo(R.drawable.masthead);
        actionBar.setHomeButtonEnabled(false);
//        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        actionBar.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#FF1B8AC7")));
        actionBar.show();

    	markerValues = new HashMap<String,BTCBusiness>();
    	btcb = new ArrayList<BTCBusiness>();

		locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
		/*
    	boolean geoGPSEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    	boolean geoNetEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    	if (!geoGPSEnabled && !geoNetEnabled) {
    		displayGPSPrompt(this);
    	}
    	*/
		locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, MIN_TIME, MIN_DISTANCE, this);

		map = ((MapFragment)getFragmentManager().findFragmentById(R.id.map)).getMap();
		map.setMyLocationEnabled(true);
        map.setInfoWindowAdapter(new InfoWindowAdapter() {
 
            // Use default InfoWindow frame
            @Override
            public View getInfoWindow(Marker arg0) {
                return null;
            }
 
            // Defines the contents of the InfoWindow
            @Override
            public View getInfoContents(Marker arg0) {
            	
                View v = getLayoutInflater().inflate(R.layout.info_window_layout, null);
                
                if(arg0 == null)
                	return v;

                if(markerValues == null || markerValues.size() < 1)
                	return v;

                LatLng latLng = arg0.getPosition();
                
                Log.d("BlockchainMerchantDirectory", "" + arg0.getId());
                
                BTCBusiness b = markerValues.get(arg0.getId());

                TextView tvName = (TextView) v.findViewById(R.id.tv_name);
                tvName.setText(b.name);

                TextView tvAddress = (TextView) v.findViewById(R.id.tv_address);
                tvAddress.setText(b.address);

                TextView tvCity = (TextView) v.findViewById(R.id.tv_city);
                tvCity.setText(b.city + " " + b.pcode);

                TextView tvTel = (TextView) v.findViewById(R.id.tv_tel);
                tvTel.setText(b.tel);
//                Linkify.addLinks(tvTel, Linkify.PHONE_NUMBERS);
                
                if(markerValues.get(arg0.getId()).web != null) {
                    TextView tvWeb = (TextView) v.findViewById(R.id.tv_web);
                    tvWeb.setText(b.web);
//                    Linkify.addLinks(tvWeb, Linkify.WEB_URLS);
                }

                TextView tvDesc = (TextView) v.findViewById(R.id.tv_desc);
                tvDesc.setText(b.desc);

                TextView tvDistance = (TextView) v.findViewById(R.id.tv_distance);
                tvDistance.setText(markerValues.get(arg0.getId()).distance);
                Double distance = Double.parseDouble(markerValues.get(arg0.getId()).distance);
                if(distance < 1.0) {
                	distance *= 1000;
                	DecimalFormat df = new DecimalFormat("###");
                    tvDistance.setText(df.format(distance) + " meters");
                }
                else {
                	DecimalFormat df = new DecimalFormat("#####.#");
                    tvDistance.setText(df.format(distance) + "km");
                }

                return v;
            }
        });

        map.setOnInfoWindowClickListener(new OnInfoWindowClickListener() {
            @Override
            public void onInfoWindowClick(final Marker marker) {

     			AlertDialog.Builder alert = new AlertDialog.Builder(MapActivity.this);
                alert.setTitle("Merchant Info");
                alert.setPositiveButton("Directions",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface arg0, int arg1) {
                     			Intent intent = new Intent(Intent.ACTION_VIEW);
                     			// http://maps.google.com/?saddr=34.052222,-118.243611&daddr=37.322778,-122.031944
                     			intent.setData(Uri.parse("http://maps.google.com/?saddr=" +
                     					mSelf.getPosition().latitude + "," + mSelf.getPosition().longitude +
                     					"&daddr=" + markerValues.get(marker.getId()).lat + "," + markerValues.get(marker.getId()).lon
                     					));
                     			startActivity(intent);
                            }
                        });
                if(markerValues.get(marker.getId()).tel != null) {
                    alert.setNeutralButton("Call",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface arg0, int arg1) {
                                	Intent intent = new Intent(Intent.ACTION_DIAL);
                                	intent.setData(Uri.parse("tel:" + markerValues.get(marker.getId()).tel));
                                	startActivity(intent);
                                }
                            });
                }
                /*
                if(markerValues.get(marker.getId()).web != null) {
                    alert.setNegativeButton("Web",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface arg0, int arg1) {
                         			Intent intent = new Intent(Intent.ACTION_VIEW);
                         			intent.setData(Uri.parse(markerValues.get(marker.getId()).web));
                         			startActivity(intent);
                                }
                            });
                }
                */
                alert.show();

            }
        });

	    imgCafe = ((ImageView)findViewById(R.id.cafe));
	    layoutCafe = ((LinearLayout)findViewById(R.id.layout_cafe));
	    imgDrink = ((ImageView)findViewById(R.id.drink));
	    layoutDrink = ((LinearLayout)findViewById(R.id.layout_drink));
	    imgEat = ((ImageView)findViewById(R.id.eat));
	    layoutEat = ((LinearLayout)findViewById(R.id.layout_eat));
	    imgSpend = ((ImageView)findViewById(R.id.spend));
	    layoutSpend = ((LinearLayout)findViewById(R.id.layout_spend));
	    imgATM = ((ImageView)findViewById(R.id.atm));
	    layoutATM = ((LinearLayout)findViewById(R.id.layout_atm));
	    imgCafe.setBackgroundColor(color_category_selected);
	    layoutCafe.setBackgroundColor(color_category_selected);
	    imgDrink.setBackgroundColor(color_category_selected);
	    layoutDrink.setBackgroundColor(color_category_selected);
	    imgEat.setBackgroundColor(color_category_selected);
	    layoutEat.setBackgroundColor(color_category_selected);
	    imgSpend.setBackgroundColor(color_category_selected);
	    layoutSpend.setBackgroundColor(color_category_selected);
	    imgATM.setBackgroundColor(color_category_selected);
	    layoutATM.setBackgroundColor(color_category_selected);
	    
        layoutCafe.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
            	imgCafe.setBackgroundColor(cafeSelected ? color_category_unselected : color_category_selected);
            	layoutCafe.setBackgroundColor(cafeSelected ? color_category_unselected : color_category_selected);
            	cafeSelected = cafeSelected ? false : true;
            	drawData(false);
                return false;
            }
        });

        layoutDrink.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
            	imgDrink.setBackgroundColor(drinkSelected ? color_category_unselected : color_category_selected);
            	layoutDrink.setBackgroundColor(drinkSelected ? color_category_unselected : color_category_selected);
            	drinkSelected = drinkSelected ? false : true;
            	drawData(false);
                return false;
            }
        });

        layoutEat.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
            	imgEat.setBackgroundColor(eatSelected ? color_category_unselected : color_category_selected);
            	layoutEat.setBackgroundColor(eatSelected ? color_category_unselected : color_category_selected);
            	eatSelected = eatSelected ? false : true;
            	drawData(false);
                return false;
            }
        });

        layoutSpend.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
            	imgSpend.setBackgroundColor(spendSelected ? color_category_unselected : color_category_selected);
            	layoutSpend.setBackgroundColor(spendSelected ? color_category_unselected : color_category_selected);
            	spendSelected = spendSelected ? false : true;
            	drawData(false);
                return false;
            }
        });

        layoutATM.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
            	imgATM.setBackgroundColor(atmSelected ? color_category_unselected : color_category_selected);
            	layoutATM.setBackgroundColor(atmSelected ? color_category_unselected : color_category_selected);
            	atmSelected = atmSelected ? false : true;
            	drawData(false);
                return false;
            }
        });

		currLocation = new Location(LocationManager.NETWORK_PROVIDER);
		currLocation.setLatitude(51.45783091);
		currLocation.setLongitude(-2.58755);

		map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(51.45783091, -2.58755), 15));

		drawData(true);
		mSelf = map.addMarker(new MarkerOptions().position(new LatLng(51.45783091, -2.58755)).title("You are here").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
		
	}

	@Override
	public void onLocationChanged(Location location) {

		LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());

		mSelf.setPosition(latLng);
		selfLat = mSelf.getPosition().latitude;
		selfLng = mSelf.getPosition().longitude;

		currLocation = location;
		CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 15);
//		CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(new LatLng(currLocation.getLatitude(), currLocation.getLongitude()), 15);
		map.animateCamera(cameraUpdate);
		locationManager.removeUpdates(this);
		
		drawData(true);

	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) { }

	@Override
	public void onProviderEnabled(String provider) { }

	@Override
	public void onProviderDisabled(String provider) { }

    @Override
    public void onResume() {
    	super.onResume();

    }

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.dir_main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    switch (item.getItemId()) {
    	case R.id.list_view:
    		doListView();
    		return true;
	    default:
	        return super.onOptionsItemSelected(item);
	    }
	}

    private void drawData(final boolean fetch) {
    	
		map.clear();

		final Handler handler = new Handler(Looper.getMainLooper());

		new Thread(new Runnable() {
			@Override
			public void run() {
				
				Looper.prepare();

				try {
					if(fetch) {
						// http://46.149.17.91/cgi-bin/btcd.pl?ULAT=48.82784&ULON=2.3569&D=40000&K=1
						final String url = "http://46.149.17.91/cgi-bin/btcd.pl?ULAT=" + selfLat + "&ULON=" + selfLng + "&D=40000&K=1";
//	         			Toast.makeText(MainActivity.this, url, Toast.LENGTH_SHORT).show();
//	         			Log.d("BlockchainMerchantDirectory", url);
	         			strJSONData = FetchData.getURL(url);
//	         			Toast.makeText(MainActivity.this, strJSONData, Toast.LENGTH_SHORT).show();
//	         			Log.d("BlockchainMerchantDirectory", strJSONData);
					}

					handler.post(new Runnable() {
						@Override
						public void run() {

							try {
								
								btcb = ParseData.parse(strJSONData);
								
								if(btcb.size() > 0) {
				         			Log.d("BlockchainMerchantDirectory", "list size=" + btcb.size());
									
//									markerValues.clear();
									
									BTCBusiness b = null;

				         			for(int i = 0; i < btcb.size(); i++) {
				         				
				         				b = btcb.get(i);

				            			BitmapDescriptor bmd = null;
				            			
				            			switch(Integer.parseInt(b.hc)) {
				            				case HEADING_CAFE:
				            					bmd = cafeSelected ? BitmapDescriptorFactory.fromResource(R.drawable.marker_cafe) : null;
				            					break;
				            				case HEADING_BAR:
				            					bmd = drinkSelected ? BitmapDescriptorFactory.fromResource(R.drawable.marker_drink) : null;
				            					break;
				            				case HEADING_RESTAURANT:
				            					bmd = eatSelected ? BitmapDescriptorFactory.fromResource(R.drawable.marker_eat) : null;
				            					break;
				            				case HEADING_SPEND:
				            					bmd = spendSelected ? BitmapDescriptorFactory.fromResource(R.drawable.marker_spend) : null;
				            					break;
				            				case HEADING_ATM:
				            					bmd = atmSelected ? BitmapDescriptorFactory.fromResource(R.drawable.marker_atm) : null;
				            					break;
				            				default:
				            					bmd = cafeSelected ? BitmapDescriptorFactory.fromResource(R.drawable.marker_cafe) : null;
				            					break;
				            				}
				            			
				            			if(bmd != null) {
					         				Marker marker = map.addMarker(new MarkerOptions()
					         		        .position(new LatLng(Double.parseDouble(b.lat), Double.parseDouble(b.lon)))
					         		        .icon(bmd));
					         				
					         				markerValues.put(marker.getId(), b);
				            			}

				         			}
									
								}

							} catch (Exception e) {
								e.printStackTrace();
							}
							
						}
					});
				} catch (Exception e) {
					e.printStackTrace();
				}
				
				Looper.loop();

			}
		}).start();
	}

    private void doListView() {
    	Intent intent = new Intent(MapActivity.this, ListActivity.class);
    	intent.putExtra("ULAT", Double.toString(mSelf.getPosition().latitude));
    	intent.putExtra("ULON", Double.toString(mSelf.getPosition().longitude));
		startActivity(intent);
    }

	public void displayGPSPrompt(final Activity activity) {

    	final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        
    	final String action = Settings.ACTION_LOCATION_SOURCE_SETTINGS;
    
        final String message = "Enable either GPS or any other location"
            + " service to find current location.  Click OK to go to"
            + " location services settings to let you do so.";
 
        builder.setMessage(message)
            .setPositiveButton("OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface d, int id) {
                        activity.startActivity(new Intent(action));
                        d.dismiss();
                    }
            })
            .setNegativeButton("Cancel",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface d, int id) {
                        d.cancel();
                    }
            });

        builder.create().show();
    }

}

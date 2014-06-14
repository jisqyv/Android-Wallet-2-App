package info.blockchain.wallet.ui;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Matrix;
import android.util.Log;
import android.content.res.Resources;
//import android.util.Log;

public class DeviceUtil {
	
	private static DeviceUtil instance = null;
	
	private static Context context = null;
	
	private static float REG_RES = 2.0f;

	private static float scale = 0.0f;

	private DeviceUtil() { ; }

	public static DeviceUtil getInstance(Context ctx) {
		
		context = ctx;
		
		if(instance == null) {
			Resources resources = context.getResources();
			scale = resources.getDisplayMetrics().density;
			instance = new DeviceUtil();
		}
		
		return instance;
	}

	public float getScale() {
		return scale;
	}

	public boolean isHiRes() {
		return (scale > REG_RES);
	}

}

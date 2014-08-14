package info.blockchain.wallet.ui;
 
import piuk.blockchain.android.R;
import android.app.ProgressDialog;
import android.content.Context;

public class ProgressUtil {

	private static Context context = null;
    private static ProgressUtil instance = null;
	private static ProgressDialog progressDialog = null;
	
	private ProgressUtil() { ; }

	public static ProgressUtil getInstance(Context ctx) {
		
		context = ctx;

		if(instance == null) {
			instance = new ProgressUtil();
		}
		
		return instance;
	}

	public boolean isShowing() {
		return progressDialog != null;
	}

	public void show() {
		if (progressDialog == null || (progressDialog != null && !progressDialog.isShowing())) {
			progressDialog = new ProgressDialog(context);
			progressDialog.setCancelable(true);
			progressDialog.setIndeterminate(true);
			progressDialog.setTitle(R.string.validating);
			progressDialog.setMessage(context.getString(R.string.please_wait));
			progressDialog.show();
		}
	}

	public void forceShow() {
		progressDialog = new ProgressDialog(context);
		progressDialog.setCancelable(true);
		progressDialog.setIndeterminate(true);
		progressDialog.setTitle(R.string.validating);
		progressDialog.setMessage(context.getString(R.string.please_wait));
		progressDialog.show();
	}

	public void close() {
		try {
	        if (progressDialog != null && progressDialog.isShowing()) {
			    progressDialog.dismiss();
			    progressDialog = null;
	        }
	    } catch (final IllegalArgumentException e) {
	    	;
	    } catch (final Exception e) {
	    	;
	    } finally {
	    	progressDialog = null;
	    }  
	}

}


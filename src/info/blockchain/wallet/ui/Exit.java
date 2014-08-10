package info.blockchain.wallet.ui;
 
import android.app.Activity;
import android.os.Bundle;
 
public class Exit extends Activity {
 
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		finish();

//		System.exit(0);
	}
 
}

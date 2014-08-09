package info.blockchain.wallet.ui;

import com.google.bitcoin.core.Transaction;

public class TxNotifUtil {
	
	private static Transaction transaction = null;
	private static TxNotifUtil instance = null;

	private TxNotifUtil() { ; }

	public static TxNotifUtil getInstance() {
		
		if(instance == null) {
			instance = new TxNotifUtil();
		}
		
		return instance;
	}

	public void setTx(final Transaction tx) {
		transaction = tx;
	}

	public Transaction getTx() {
		return transaction;
	}

	public void clear() {
		transaction = null;
	}

}

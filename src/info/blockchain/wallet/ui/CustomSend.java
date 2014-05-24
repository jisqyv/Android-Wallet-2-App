package info.blockchain.wallet.ui;

import java.math.BigInteger;
import java.util.HashMap;

//import android.util.Log;

public class CustomSend {
	
	private HashMap<String, BigInteger> receivingAddresses;
	private BigInteger fee = null;
	private String changeAddress = null;

	public CustomSend() {
		receivingAddresses = new HashMap<String, BigInteger>();
	}

	public void addReceivingAddress(String address, BigInteger amount) {
		receivingAddresses.put(address, amount);
	}
	
	public void setReceivingAddresses(HashMap<String, BigInteger> addresses) {
		receivingAddresses = addresses;
	}

	public HashMap<String, BigInteger> getReceivingAddresses() {
		return receivingAddresses;
	}

	public void setFee(BigInteger fee) {
		this.fee = fee;
	}

	public BigInteger getFee() {
		return fee;
	}

	public void setChangeAddress(String address) {
		changeAddress = address;
	}

	public String getChangeAddress() {
		return changeAddress;
	}

}

package info.blockchain.wallet.ui;

import java.util.HashMap;

//import android.util.Log;

public class CustomSend {
	
	private HashMap<String, Double> receivingAddresses;
	private Double fee = 0.0;
	private String changeAddress = null;

	public CustomSend() {
		receivingAddresses = new HashMap<String, Double>();
	}

	public void addReceivingAddress(String address, double amount) {
		receivingAddresses.put(address, amount);
	}
	
	public void setReceivingAddresses(HashMap<String, Double> addresses) {
		receivingAddresses = addresses;
	}

	public HashMap<String, Double> getReceivingAddresses() {
		return receivingAddresses;
	}

	public void setFee(double fee) {
		this.fee = fee;
	}

	public double getFee() {
		return fee;
	}

	public void setChangeAddress(String address) {
		changeAddress = address;
	}

	public String getChangeAddress() {
		return changeAddress;
	}

}

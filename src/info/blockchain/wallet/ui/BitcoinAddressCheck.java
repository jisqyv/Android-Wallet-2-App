package info.blockchain.wallet.ui;

import com.google.bitcoin.uri.BitcoinURI;
import com.google.bitcoin.uri.BitcoinURIParseException;
import com.google.bitcoin.core.Address;
import com.google.bitcoin.core.AddressFormatException;
import com.google.bitcoin.core.NetworkParameters;
import com.google.bitcoin.core.WrongNetworkException;

public class BitcoinAddressCheck {
	
	private BitcoinAddressCheck() { ; }

	public static String validate(final String btcaddress) {
		
		if(isValid(btcaddress)) {
			return btcaddress;
		}
		else {
			String address = clean(btcaddress);
			if(address != null) {
				return address;
			}
			else {
				return null;
			}
		}
	}

	private static String clean(final String btcaddress) {
		
		String ret = null;
		BitcoinURI uri = null;
		
		try {
			uri = new BitcoinURI(btcaddress);
			ret = uri.getAddress().toString();
		}
		catch(BitcoinURIParseException bupe) {
			ret = null;
		}
		
		return ret;
	}

	private static boolean isValid(final String btcaddress) {

		boolean ret = false;
		Address address = null;
		
		try {
			address = new Address(NetworkParameters.prodNet(), btcaddress);
			if(address != null) {
				ret = true;
			}
		}
		catch(WrongNetworkException wne) {
			ret = false;
		}
		catch(AddressFormatException afe) {
			ret = false;
		}

		return ret;
	}

}

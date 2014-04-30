/*
 * Copyright 2011-2012 the original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */


package piuk;

import java.math.BigInteger;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.JSONParser;
import org.spongycastle.util.encoders.Hex;

import piuk.blockchain.android.Constants;
import piuk.blockchain.android.ui.SendCoinsFragment;
import piuk.blockchain.android.ui.SendCoinsFragment.FeePolicy;
import piuk.blockchain.android.ui.SuccessCallback;
import piuk.blockchain.android.util.WalletUtils;
import android.util.Pair;

import com.google.bitcoin.core.ECKey;
import com.google.bitcoin.core.NetworkParameters;
import com.google.bitcoin.core.Sha256Hash;
import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.core.Transaction.SigHash;
import com.google.bitcoin.core.TransactionInput;
import com.google.bitcoin.core.TransactionOutput;
import com.google.bitcoin.core.Utils;
import com.google.bitcoin.core.Wallet;


@SuppressWarnings("unchecked")
public class MyRemoteWallet extends MyWallet {
	private static final String WebROOT = "https://"+Constants.BLOCKCHAIN_DOMAIN+"/";
	private static final String ApiCode = "25a6ad13-1633-4dfb-b6ee-9b91cdf0b5c3";

	private String _checksum;
	private boolean _isNew = false;
	private MyBlock latestBlock;
	private long lastMultiAddress;
	private BigInteger final_balance = BigInteger.ZERO;
	private BigInteger total_received = BigInteger.ZERO;
	private BigInteger total_sent = BigInteger.ZERO;

	private String language = "en";
	private String btcCurrencyCode = "USD";
	private String localCurrencyCode = "BTC";
	private boolean sync_pubkeys = true;
	private double localCurrencyConversion;
	private double btcCurrencyConversion;

	private Map<String, JSONObject> multiAddrBalancesRoot;
	private JSONObject multiAddrRoot;

	private double sharedFee;
	private List<MyTransaction> transactions = Collections.synchronizedList(new ArrayList<MyTransaction>());
	public byte[] extra_seed;

	public MyBlock getLatestBlock() {
		return latestBlock;
	}


	public void setFinal_balance(BigInteger final_balance) {
		this.final_balance = final_balance;
	}


	public void setTotal_received(BigInteger total_received) {
		this.total_received = total_received;
	}


	public void setTotal_sent(BigInteger total_sent) {
		this.total_sent = total_sent;
	}


	public long getLastMultiAddress() {
		return lastMultiAddress;
	}

	public void setLatestBlock(MyBlock latestBlock) {
		this.latestBlock = latestBlock;
	}

	public BigInteger getFinal_balance() {
		return final_balance;
	}


	public BigInteger getTotal_received() {
		return total_received;
	}

	public BigInteger getTotal_sent() {
		return total_sent;
	}


	public String getLocalCurrencyCode() {
		return localCurrencyCode;
	}


	public double getLocalCurrencyConversion() {
		return localCurrencyConversion;
	}


	public Map<String, JSONObject> getMultiAddrBalancesRoot() {
		return multiAddrBalancesRoot;
	}
	
	public JSONObject getMultiAddrRoot() {
		return multiAddrRoot;
	}


	public void setMultiAddrRoot(JSONObject multiAddrRoot) {
		this.multiAddrRoot = multiAddrRoot;
	}


	public double getSharedFee() {
		return sharedFee;
	}

	public boolean isAddressMine(String address) {
		for (Map<String, Object> map : this.getKeysMap()) {
			String addr = (String) map.get("addr");

			if (address.equals(addr))
				return true;
		}

		return false;
	}

	public static class Latestblock {
		int height;
		int block_index;
		Hash hash;
		long time;
	}

	public synchronized BigInteger getBalance() {
		return final_balance;
	}


	public static String generateSharedAddress(String destination) throws Exception {
		StringBuilder args = new StringBuilder();

		args.append("address=" + destination);
		args.append("&shared=true");
		args.append("&format=plain");
		args.append("&method=create");

		final String response = postURL("https://"+Constants.BLOCKCHAIN_DOMAIN+"/api/receive", args.toString());

		JSONObject object = (JSONObject) new JSONParser().parse(response);

		return (String)object.get("input_address");
	}

	public synchronized BigInteger getBalance(String address) {
		if (this.multiAddrBalancesRoot != null && this.multiAddrBalancesRoot.containsKey(address)) {
			return BigInteger.valueOf(((Number)this.multiAddrBalancesRoot.get(address).get("final_balance")).longValue());	
		}

		return BigInteger.ZERO;
	}

	public boolean isNew() {
		return _isNew;
	}

	public MyRemoteWallet() throws Exception {
		super();

		this.temporyPassword = null;

		this._checksum  = null;

		this._isNew = true;
	}


	public MyRemoteWallet(JSONObject walletJSONObj, String password) throws Exception {
		super(walletJSONObj.get("payload").toString(), password);
		
		handleWalletPayloadObj(walletJSONObj);
		
		this.temporyPassword = password;

		this._checksum  = new String(Hex.encode(MessageDigest.getInstance("SHA-256").digest(walletJSONObj.get("payload").toString().getBytes("UTF-8"))));

		this._isNew = false;
	}
	
	public MyRemoteWallet( String base64Payload, String password) throws Exception {
		super(base64Payload, password);

		this.temporyPassword = password;

		this._checksum  = new String(Hex.encode(MessageDigest.getInstance("SHA-256").digest(base64Payload.getBytes("UTF-8"))));

		this._isNew = false;
	}

	private static String fetchURL(String URL) throws Exception {
		return WalletUtils.getURL(URL);
	}

	public static String postURL(String request, String urlParameters) throws Exception {
		return WalletUtils.postURL(request, urlParameters);
	}

	@Override
	public synchronized boolean addKey(ECKey key, String address, String label) throws Exception {
		boolean success = super.addKey(key, address, label);

		EventListeners.invokeWalletDidChange();

		return success;
	}


	@Override
	public synchronized boolean addKey(ECKey key, String address, String label, String device_name, String device_version) throws Exception {
		boolean success = super.addKey(key, address, label, device_name, device_version);

		EventListeners.invokeWalletDidChange();

		return success;
	}

	@Override
	public void setTag(String address, long tag) {
		super.setTag(address, tag);

		EventListeners.invokeWalletDidChange();
	}

	@Override
	public synchronized boolean addWatchOnly(String address, String source) throws Exception {
		boolean success = super.addWatchOnly(address, source);

		EventListeners.invokeWalletDidChange();

		return success;
	}

	@Override
	public synchronized boolean removeKey(ECKey key) {
		boolean success = super.removeKey(key);

		EventListeners.invokeWalletDidChange();

		return success;
	}

	public List<MyTransaction> getMyTransactions() {
		return transactions;
	}

	public boolean addTransaction(MyTransaction tx) {

		for (MyTransaction existing_tx : transactions) {
			if (existing_tx.getTxIndex() == tx.getTxIndex())
				return false;
		}

		this.transactions.add(tx);

		return true;
	}

	public boolean prependTransaction(MyTransaction tx) {

		for (MyTransaction existing_tx : transactions) {
			if (existing_tx.getTxIndex() == tx.getTxIndex())
				return false;
		}

		this.transactions.add(0, tx);

		return true;
	}

	public BigInteger getBaseFee() {
		BigInteger baseFee = null; 
		if (getFeePolicy() == -1) {
			baseFee = Utils.toNanoCoins("0.0001");
		} else if (getFeePolicy() == 1) {
			baseFee = Utils.toNanoCoins("0.0005");
		} else {
			baseFee = Utils.toNanoCoins("0.0001");
		}

		return baseFee;
	}

	public List<MyTransaction> getTransactions() {
		return this.transactions;
	}

	public void parseMultiAddr(String response, boolean notifications) throws Exception {

		transactions.clear();

		BigInteger previousBalance = final_balance;

		Map<String, Object> top = (Map<String, Object>) JSONValue.parse(response);

		this.multiAddrRoot = (JSONObject) top;
		
		Map<String, Object> info_obj = (Map<String, Object>) top.get("info");

		Map<String, Object> block_obj = (Map<String, Object>) info_obj.get("latest_block");

		if (block_obj != null) {
			Sha256Hash hash = new Sha256Hash(Hex.decode((String)block_obj.get("hash")));
			int blockIndex = ((Number)block_obj.get("block_index")).intValue();
			int blockHeight = ((Number)block_obj.get("height")).intValue();
			long time = ((Number)block_obj.get("time")).longValue();

			MyBlock block = new MyBlock();

			block.height = blockHeight;
			block.hash = hash;
			block.blockIndex = blockIndex;
			block.time = time;

			this.latestBlock = block;
		}

		List<JSONObject> multiAddrBalances = (List<JSONObject>) top.get("addresses");

		Map<String, JSONObject> multiAddrBalancesRoot = new HashMap<String, JSONObject>();

		for (JSONObject obj : multiAddrBalances) {
			multiAddrBalancesRoot.put((String) obj.get("address"), obj);
		}

		this.multiAddrBalancesRoot = multiAddrBalancesRoot;

		Map<String, Object> symbol_local = (Map<String, Object>) info_obj.get("symbol_local");

		boolean didUpdateCurrency = false;

		if (symbol_local != null && symbol_local.containsKey("code")) {
			String currencyCode = (String) symbol_local.get("code");
			Double currencyConversion = (Double) symbol_local.get("conversion");

			if (currencyConversion == null)
				currencyConversion = 0d;

			if (this.localCurrencyCode == null || !this.localCurrencyCode.equals(currencyCode) || this.localCurrencyConversion != currencyConversion) {
				this.localCurrencyCode = currencyCode;
				this.localCurrencyConversion = currencyConversion;
				didUpdateCurrency = true;
			}
		}


		Map<String, Object> symbol_btc = (Map<String, Object>) info_obj.get("symbol_btc");
		if (symbol_btc != null && symbol_btc.containsKey("code")) {
			String currencyCode = (String) symbol_local.get("code");
			Double currencyConversion = (Double) symbol_local.get("conversion");

			if (currencyConversion == null)
				currencyConversion = 0d;

			if (this.btcCurrencyCode == null || !this.btcCurrencyCode.equals(currencyCode) || this.btcCurrencyConversion != currencyConversion) {
				this.btcCurrencyCode = currencyCode;
				this.btcCurrencyConversion = currencyConversion;
				//didUpdateCurrency = true;
			}
		}

		if (didUpdateCurrency) {
			EventListeners.invokeCurrencyDidChange();
		}

		if (top.containsKey("mixer_fee")) {
			sharedFee = ((Number)top.get("mixer_fee")).doubleValue();
		}

		Map<String, Object> wallet_obj = (Map<String, Object>) top.get("wallet");

		this.final_balance = BigInteger.valueOf(((Number)wallet_obj.get("final_balance")).longValue());
		this.total_sent = BigInteger.valueOf(((Number)wallet_obj.get("total_sent")).longValue());
		this.total_received = BigInteger.valueOf(((Number)wallet_obj.get("total_received")).longValue());

		List<Map<String, Object>> transactions = (List<Map<String, Object>>) top.get("txs");

		MyTransaction newestTransaction = null;
		if (transactions != null) {
			for (Map<String, Object> transactionDict : transactions) {
				MyTransaction tx = MyTransaction.fromJSONDict(transactionDict);

				if (tx == null)
					continue;

				if (newestTransaction == null)
					newestTransaction = tx;

				addTransaction(tx);
			}
		}

		if (notifications) {
			if (this.final_balance.compareTo(previousBalance) != 0 && newestTransaction != null) {
				if (newestTransaction.getResult().compareTo(BigInteger.ZERO) >= 0)
					EventListeners.invokeOnCoinsReceived(newestTransaction, newestTransaction.getResult().longValue());
				else
					EventListeners.invokeOnCoinsSent(newestTransaction, newestTransaction.getResult().longValue());
			}
		} else {
			EventListeners.invokeOnTransactionsChanged();
		}
	}

	public boolean isUptoDate(long time) {
		long now = System.currentTimeMillis();

		if (lastMultiAddress < now - time) {
			return false;
		} else {
			return true;
		}
	}

	public synchronized String doMultiAddr(boolean notifications) throws Exception {
		String url =  WebROOT + "multiaddr?active=" + StringUtils.join(getActiveAddresses(), "|")+"&symbol_btc="+btcCurrencyCode+"&symbol_local="+localCurrencyCode;

		String response = fetchURL(url);

		parseMultiAddr(response, notifications);

		lastMultiAddress = System.currentTimeMillis();

		return response;
	}

	public synchronized void doMultiAddr(boolean notifications, SuccessCallback callback) {
		try {
			String url =  WebROOT + "multiaddr?active=" + StringUtils.join(getActiveAddresses(), "|")+"&symbol_btc="+btcCurrencyCode+"&symbol_local="+localCurrencyCode;

			String response = fetchURL(url);

			parseMultiAddr(response, notifications);

			lastMultiAddress = System.currentTimeMillis();

			callback.onSuccess();
		} catch (Exception e) {
			e.printStackTrace();

			callback.onFail();
		}
	}


	public interface SendProgress {
		//Return false to cancel
		public boolean onReady(Transaction tx, BigInteger fee, SendCoinsFragment.FeePolicy feePolicy, long priority);
		public void onSend(Transaction tx, String message);

		//Return true to cancel the transaction or false to continue without it
		public ECKey onPrivateKeyMissing(String address);

		public void onError(String message);
		public void onProgress(String message);
	}


	private List<MyTransactionOutPoint> filter(List<MyTransactionOutPoint> unspent, List<ECKey> tempKeys, boolean askForPrivateKeys, final SendProgress progress) throws Exception {		
		List<MyTransactionOutPoint> filtered = new ArrayList<MyTransactionOutPoint>();

		Set<String> alreadyAskedFor = new HashSet<String>();

		for (MyTransactionOutPoint output : unspent) {
			BitcoinScript script = new BitcoinScript(output.getScriptBytes());

			String addr = script.getAddress().toString();

			Map<String, Object> keyMap = findKey(addr);

			if (keyMap.get("priv") == null) {
				if (askForPrivateKeys && alreadyAskedFor.add(addr)) {

					ECKey key = progress.onPrivateKeyMissing(addr);

					if (key != null) {
						filtered.add(output);

						tempKeys.add(key);
					}
				}
			} else {
				filtered.add(output);
			}
		}

		return filtered;
	}

	public void sendCoinsAsync(final String toAddress, final BigInteger amount, final FeePolicy feePolicy, final BigInteger fee, final SendProgress progress) {
		sendCoinsAsync(getActiveAddresses(), toAddress, amount, feePolicy, fee, progress);
	}

	public void sendCoinsAsync(final String[] from, final String toAddress, final BigInteger amount, final FeePolicy feePolicy, final BigInteger fee, final SendProgress progress) {

		new Thread() {
			@Override
			public void run() {
				final List<ECKey> tempKeys = new ArrayList<ECKey>();

				try {

					//Construct a new transaction
					progress.onProgress("Getting Unspent Outputs");

					List<MyTransactionOutPoint> allUnspent = getUnspentOutputPoints(from);

					Pair<Transaction, Long> pair = null;

					progress.onProgress("Constructing Transaction");

					try {
						//Try without asking for watch only addresses
						List<MyTransactionOutPoint> unspent = filter(allUnspent, tempKeys, false, progress);

						pair = makeTransaction(unspent, toAddress, amount, fee);

						//Transaction cancelled
						if (pair == null)
							return;
					} catch (InsufficientFundsException e) {

						//Try with asking for watch only
						List<MyTransactionOutPoint> unspent = filter(allUnspent, tempKeys, true, progress);

						pair = makeTransaction(unspent, toAddress, amount, fee);

						//Transaction cancelled
						if (pair == null)
							return;
					}

					Transaction tx = pair.first;
					Long priority = pair.second; 

					//If returns false user cancelled
					//Probably because they want to recreate the transaction with different fees
					if (!progress.onReady(tx, fee, feePolicy, priority))
						return;

					progress.onProgress("Signing Inputs");

					Wallet wallet = new Wallet(NetworkParameters.prodNet());
					for (TransactionInput input : tx.getInputs()) {

						try {
							byte[] scriptBytes = input.getOutpoint().getConnectedPubKeyScript();

							String address = new BitcoinScript(scriptBytes).getAddress().toString();

							ECKey key = getECKey(address);
							if (key != null) {
								wallet.addKey(key);
							}
						} catch (Exception e) {}
					}

					wallet.addKeys(tempKeys);

					//Now sign the inputs
					tx.signInputs(SigHash.ALL, wallet);

					progress.onProgress("Broadcasting Transaction");

					String response = pushTx(tx);

					progress.onSend(tx, response);

				} catch (Exception e) {
					e.printStackTrace();

					progress.onError(e.getLocalizedMessage());

				} 
			}
		}.start();
	}

	//Rerutns response message
	public String pushTx(Transaction tx) throws Exception {

		String hexString = new String(Hex.encode(tx.bitcoinSerialize()));

		if (hexString.length() > 16384)
			throw new Exception("Blockchain wallet's cannot handle transactions over 16kb in size. Please try splitting your transaction");

		String response = postURL(WebROOT + "pushtx", "tx="+hexString);

		return response;
	}

	public static class InsufficientFundsException extends Exception {
		private static final long serialVersionUID = 1L;

		public InsufficientFundsException(String string) {
			super(string);
		}
	}

	//You must sign the inputs
	public Pair<Transaction, Long> makeTransaction(List<MyTransactionOutPoint> unspent, String toAddress, BigInteger amount, BigInteger fee) throws Exception {

		long priority = 0;

		if (unspent == null || unspent.size() == 0)
			throw new InsufficientFundsException("No free outputs to spend.");

		if (fee == null)
			fee = BigInteger.ZERO;

		if (amount == null || amount.compareTo(BigInteger.ZERO) <= 0)
			throw new Exception("You must provide an amount");

		//Construct a new transaction
		Transaction tx = new Transaction(params);

		//Add the output
		BitcoinScript toOutputScript = BitcoinScript.createSimpleOutBitoinScript(new BitcoinAddress(toAddress));

		TransactionOutput output = new TransactionOutput(params, null, amount, toOutputScript.getProgram());

		tx.addOutput(output);

		//Now select the appropriate inputs
		BigInteger valueSelected = BigInteger.ZERO;
		BigInteger valueNeeded =  amount.add(fee);
		BigInteger minFreeOutputSize = BigInteger.valueOf(1000000);

		MyTransactionOutPoint firstOutPoint = null;

		for (MyTransactionOutPoint outPoint : unspent) {

			BitcoinScript script = new BitcoinScript(outPoint.getScriptBytes());

			if (script.getOutType() == BitcoinScript.ScriptOutTypeStrange)
				continue;


			MyTransactionInput input = new MyTransactionInput(params, null, new byte[0], outPoint);

			input.outpoint = outPoint;

			tx.addInput(input);

			valueSelected = valueSelected.add(outPoint.value);

			priority += outPoint.value.longValue() * outPoint.confirmations;

			if (firstOutPoint == null)
				firstOutPoint = outPoint;

			if (valueSelected.compareTo(valueNeeded) == 0 || valueSelected.compareTo(valueNeeded.add(minFreeOutputSize)) >= 0)
				break;
		}

		//Check the amount we have selected is greater than the amount we need
		if (valueSelected.compareTo(valueNeeded) < 0) {
			throw new InsufficientFundsException("Insufficient Funds");
		}

		BigInteger change = valueSelected.subtract(amount).subtract(fee);

		//Now add the change if there is any
		if (change.compareTo(BigInteger.ZERO) > 0) {
			BitcoinScript inputScript = new BitcoinScript(firstOutPoint.getConnectedPubKeyScript());

			//Return change to the first address
			BitcoinScript change_script = BitcoinScript.createSimpleOutBitoinScript(inputScript.getAddress());

			TransactionOutput change_output = new TransactionOutput(params, null, change, change_script.getProgram());

			tx.addOutput(change_output);
		}

		long estimatedSize = tx.bitcoinSerialize().length + (114 * tx.getInputs().size());

		priority /= estimatedSize;

		return new Pair<Transaction, Long>(tx, priority);
	}

	public static List<MyTransactionOutPoint> getUnspentOutputPoints(String[] from) throws Exception {

		StringBuffer buffer =  new StringBuffer(WebROOT + "unspent?active=");

		int ii = 0;
		for (String address : from) {
			buffer.append(address);

			if (ii < from.length-1)
				buffer.append("|");

			++ii;
		}

		List<MyTransactionOutPoint> outputs = new ArrayList<MyTransactionOutPoint>();

		String response = fetchURL(buffer.toString());

		Map<String, Object> root = (Map<String, Object>) JSONValue.parse(response);

		List<Map<String, Object>> outputsRoot = (List<Map<String, Object>>) root.get("unspent_outputs");

		for (Map<String, Object> outDict : outputsRoot) {

			byte[] hashBytes = Hex.decode((String)outDict.get("tx_hash"));

			ArrayUtils.reverse(hashBytes);

			Sha256Hash txHash = new Sha256Hash(hashBytes);

			int txOutputN = ((Number)outDict.get("tx_output_n")).intValue();
			BigInteger value = BigInteger.valueOf(((Number)outDict.get("value")).longValue());
			byte[] scriptBytes = Hex.decode((String)outDict.get("script"));
			int confirmations = ((Number)outDict.get("confirmations")).intValue();

			//Contrstuct the output
			MyTransactionOutPoint outPoint = new MyTransactionOutPoint(txHash, txOutputN, value, scriptBytes);

			outPoint.setConfirmations(confirmations);

			outputs.add(outPoint);
		}

		return outputs;
	}

	/**
	 * Register this account/device pair within the server.
	 * @throws Exception 
	 *
	 */
	public boolean registerNotifications(final String regId) throws Exception {
		if (_isNew) return false;

		StringBuilder args = new StringBuilder();

		args.append("guid=" + getGUID());
		args.append("&sharedKey=" + getSharedKey());
		args.append("&method=register-android-device");
		args.append("&payload="+URLEncoder.encode(regId));
		args.append("&length="+regId.length());

		String response = postURL(WebROOT + "wallet", args.toString());

		return response != null && response.length() > 0;
	}

	/** k
	 * Unregister this account/device pair within the server.
	 * @throws Exception 
	 */
	public boolean unregisterNotifications(final String regId) throws Exception {    
		if (_isNew) return false;

		StringBuilder args = new StringBuilder();

		args.append("guid=" + getGUID());
		args.append("&sharedKey=" + getSharedKey());
		args.append("&method=unregister-android-device");
		args.append("&payload="+URLEncoder.encode(regId));
		args.append("&length="+regId.length());

		String response = postURL(WebROOT + "wallet", args.toString());

		return response != null && response.length() > 0;
	}

	public JSONObject getAccountInfo() throws Exception {    
		if (_isNew) return null;

		StringBuilder args = new StringBuilder();

		args.append("guid=" + getGUID());
		args.append("&sharedKey=" + getSharedKey());
		args.append("&method=get-info");;

		String response = postURL(WebROOT + "wallet", args.toString());

		return (JSONObject) new JSONParser().parse(response);
	}

	public boolean updateRemoteLocalCurrency(String currency_code) throws Exception {    
		if (_isNew) return false;

		localCurrencyCode = currency_code;
		
		StringBuilder args = new StringBuilder();

		args.append("guid=" + getGUID());
		args.append("&sharedKey=" + getSharedKey());
		args.append("&payload=" + currency_code);
		args.append("&length=" + currency_code.length());
		args.append("&method=update-currency");;

		String response = postURL(WebROOT + "wallet", args.toString());

		return response != null;
	}

	/**
	 * Get the tempoary paring encryption password
	 * @throws Exception 
	 *
	 */
	public static String getPairingEncryptionPassword(final String guid) throws Exception {
		StringBuilder args = new StringBuilder();

		args.append("guid=" + guid);
		args.append("&method=pairing-encryption-password");

		return postURL(WebROOT + "wallet", args.toString());
	}

	public static BigInteger getAddressBalance(final String address) throws Exception {
		return new BigInteger(fetchURL(WebROOT + "q/addressbalance/"+address));
	}

	public static String getWalletManualPairing(final String guid) throws Exception {
		StringBuilder args = new StringBuilder();

		args.append("guid=" + guid);
		args.append("&method=pairing-encryption-password");

		String response = fetchURL(WebROOT + "wallet/" + guid + "?format=json&resend_code=false");

		JSONObject object = (JSONObject) new JSONParser().parse(response);

		String payload = (String) object.get("payload");
		if (payload == null || payload.length() == 0) {
			throw new Exception("Error Fetching Wallet Payload");
		}

		return payload;
	}

	public synchronized boolean remoteSave() throws Exception {
		return remoteSave(null);
	}

	public synchronized boolean remoteSave(String email) throws Exception {

		String payload = this.getPayload();

		String old_checksum = this._checksum;
		this._checksum  = new String(Hex.encode(MessageDigest.getInstance("SHA-256").digest(payload.getBytes("UTF-8"))));

		String method = _isNew ? "insert" : "update";

		String urlEncodedPayload = URLEncoder.encode(payload);

		StringBuilder args = new StringBuilder();
		args.append("guid=");
		args.append(URLEncoder.encode(this.getGUID(), "utf-8"));
		args.append("&sharedKey=");
		args.append(URLEncoder.encode(this.getSharedKey(), "utf-8"));
		args.append("&payload=");
		args.append(urlEncodedPayload);
		args.append("&method=");
		args.append(method);
		args.append("&length=");
		args.append(payload.length());
		args.append("&checksum=");
		args.append(URLEncoder.encode(_checksum, "utf-8"));

		if (sync_pubkeys) {
			args.append("&active=");
			args.append(StringUtils.join(getActiveAddresses(), "|"));
		}

		if (email != null && email.length() > 0) {
			args.append("&email=");
			args.append(URLEncoder.encode(email, "utf-8"));
		}

		args.append("&device=");
		args.append("android");

		if (_isNew) {
			args.append("&api_code=");
			args.append(ApiCode);
		}

		if (old_checksum != null && old_checksum.length() > 0)
		{
			args.append("&old_checksum=");
			args.append(old_checksum);
		}

		postURL(WebROOT + "wallet", args.toString());

		_isNew = false;

		return true;
	}

	public void remoteDownload() {

	}

	public String getChecksum() {
		return _checksum;
	}


	public synchronized String setPayload(JSONObject walletJSONObj) throws Exception {
		handleWalletPayloadObj(walletJSONObj);
		
		return setPayload(walletJSONObj.get("payload").toString());
	}
	
	public synchronized String setPayload(String payload) throws Exception {
		MyRemoteWallet tempWallet = new MyRemoteWallet(payload, temporyPassword);

		this.root = tempWallet.root;
		this.rootContainer = tempWallet.rootContainer;

		if (this.temporySecondPassword != null && !this.validateSecondPassword(temporySecondPassword)) {
			this.temporySecondPassword = null;
		}

		this._checksum = tempWallet._checksum;

		_isNew = false;

		return payload;
	}

	public static class NotModfiedException extends Exception {
		private static final long serialVersionUID = 1L;
	}

	
	public void handleWalletPayloadObj(JSONObject obj) {
		Map<String, Object> symbol_local = (Map<String, Object>) obj.get("symbol_local");

		boolean didUpdateCurrency = false;

		if (symbol_local != null && symbol_local.containsKey("code")) {
			String currencyCode = (String) symbol_local.get("code");
			Double currencyConversion = (Double) symbol_local.get("conversion");

			if (currencyConversion == null)
				currencyConversion = 0d;

			if (this.localCurrencyCode == null || !this.localCurrencyCode.equals(currencyCode) || this.localCurrencyConversion != currencyConversion) {
				this.localCurrencyCode = currencyCode;
				this.localCurrencyConversion = currencyConversion;
				didUpdateCurrency = true;
			}
		}


		Map<String, Object> symbol_btc = (Map<String, Object>) obj.get("symbol_btc");
		if (symbol_btc != null && symbol_btc.containsKey("code")) {
			String currencyCode = (String) symbol_local.get("code");
			Double currencyConversion = (Double) symbol_local.get("conversion");

			if (currencyConversion == null)
				currencyConversion = 0d;

			if (this.btcCurrencyCode == null || !this.btcCurrencyCode.equals(currencyCode) || this.btcCurrencyConversion != currencyConversion) {
				this.btcCurrencyCode = currencyCode;
				this.btcCurrencyConversion = currencyConversion;
				//didUpdateCurrency = true;
			}
		}

		if (didUpdateCurrency) {
			EventListeners.invokeCurrencyDidChange();
		}

		if (obj.containsKey("sync_pubkeys")) {
			sync_pubkeys = Boolean.valueOf(obj.get("sync_pubkeys").toString());
		}
	}
	
	public static JSONObject getWalletPayload(String guid, String sharedKey, String checkSumString) throws Exception {
		String response = postURL(WebROOT + "wallet", "method=wallet.aes.json&guid="+guid+"&sharedKey="+sharedKey+"&checksum="+checkSumString+"&format=json");

		if (response == null) {
			throw new Exception("Error downloading wallet");
		}

		JSONObject obj = (JSONObject) new JSONParser().parse(response);

		String payload = obj.get("payload").toString();
		
		if (payload == null) {
			throw new Exception("Error downloading wallet");
		}

		if (payload.equals("Not modified")) {
			throw new NotModfiedException();
		}

		return obj;
	}

	public static JSONObject getWalletPayload(String guid, String sharedKey) throws Exception {
		String response = postURL(WebROOT + "wallet","method=wallet.aes.json&guid="+guid+"&sharedKey="+sharedKey+"&format=json");

		if (response == null) {
			throw new Exception("Error downloading wallet");
		}

		JSONObject obj = (JSONObject) new JSONParser().parse(response);

		String payload = obj.get("payload").toString();

		if (payload == null) {
			throw new Exception("Error downloading wallet");
		}

		return obj;
	}

}

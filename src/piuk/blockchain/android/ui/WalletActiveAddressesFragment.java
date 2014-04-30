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

package piuk.blockchain.android.ui;

import java.util.Map;

import piuk.EventListeners;
import piuk.MyRemoteWallet;
import piuk.blockchain.android.Constants;
import piuk.blockchain.android.R;
import piuk.blockchain.android.util.QrDialog;
import piuk.blockchain.android.util.WalletUtils;

import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.uri.BitcoinURI;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView.AdapterContextMenuInfo;

public final class WalletActiveAddressesFragment extends WalletAddressesFragment
{
	public WalletActiveAddressesFragment() {
		super(0);

		eventListener = new EventListeners.EventListener() {
			@Override
			public String getDescription() {
				return "Wallet Active Addresses Listener";
			}

			@Override
			public void onCoinsSent(final Transaction tx, final long result) {
				setAdapterContent();
			};

			@Override
			public void onCoinsReceived(final Transaction tx, final long result) {
				setAdapterContent();		
			};

			@Override
			public void onTransactionsChanged() {
				setAdapterContent();
			};


			@Override
			public void onWalletDidChange() {	
				setAdapterContent();
			}
		};
	}

	@Override
	public void onViewCreated(final View view, final Bundle savedInstanceState)
	{
		super.onViewCreated(view, savedInstanceState);

		registerForContextMenu(getListView());
	}

	public void onHide() {
		this.unregisterForContextMenu(getListView());
	}

	@Override
	public void onCreateContextMenu(final ContextMenu menu, final View v, final ContextMenuInfo info)
	{
		activity.getMenuInflater().inflate(R.menu.wallet_active_addresses_context, menu);
	}

	@Override
	public boolean onContextItemSelected(final MenuItem item)
	{
		try {
			final AdapterContextMenuInfo menuInfo = (AdapterContextMenuInfo) item.getMenuInfo();

			final Map<String, Object> map = (Map<String, Object>) getListView().getAdapter().getItem(menuInfo.position);

			final String address = (String) map.get("addr");

			switch (item.getItemId())
			{
			case R.id.wallet_addresses_context_archive:
			{
				MyRemoteWallet remoteWallet = application.getRemoteWallet();

				if (remoteWallet == null)
					return true;

				remoteWallet.setTag(address, 2);

				application.saveWallet(new SuccessCallback() {
					@Override
					public void onSuccess() {
						EventListeners.invokeWalletDidChange();
					}

					@Override
					public void onFail() {
					}
				});
				return true;
			}
			case R.id.wallet_addresses_context_edit:
			{
				EditAddressBookEntryFragment.edit(getFragmentManager(), address.toString());
				return true;
			}

			case R.id.wallet_addresses_context_show_qr:
			{
				final String uri = BitcoinURI.convertToBitcoinURI(address, null, null, null);
				final int size = (int) (256 * getResources().getDisplayMetrics().density);
				new QrDialog(activity, WalletUtils.getQRCodeBitmap(uri, size)).show();
				return true;
			}

			case R.id.wallet_addresses_context_copy_to_clipboard:
			{
				AbstractWalletActivity.handleCopyToClipboard(activity, address.toString());
				return true;
			}

			case R.id.wallet_addresses_context_default:
			{
				handleDefault(address);
				return true;
			}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return false;
	}

	private void handleDefault(final String address)
	{
		final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
		prefs.edit().putString(Constants.PREFS_KEY_SELECTED_ADDRESS, address.toString()).commit();
	}

}

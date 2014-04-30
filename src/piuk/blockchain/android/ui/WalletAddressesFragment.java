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

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

import piuk.EventListeners;
import piuk.MyRemoteWallet;
import piuk.blockchain.android.AddressBookProvider;
import piuk.blockchain.android.R;
import piuk.blockchain.android.WalletApplication;
import piuk.blockchain.android.util.WalletUtils;
import android.app.Activity;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ListFragment;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class WalletAddressesFragment extends ListFragment
{
	protected WalletApplication application;
	protected Activity activity;
	private int tag_filter = 0;
	private ArrayAdapter<Map<String, Object>> adapter;
	private Map<String, String> labelMap;
	protected EventListeners.EventListener eventListener;
	

	public WalletAddressesFragment() {
		super();
	}

	public WalletAddressesFragment(int tag_filter) {
		super();

		this.tag_filter = tag_filter;
	}

	@Override
	public void onCreate(final Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		activity = getActivity();
		application = (WalletApplication) activity.getApplication();

		EventListeners.addEventListener(eventListener);

		initAdapter();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		EventListeners.removeEventListener(eventListener);
	}

	@Override
	public void onResume()
	{
		super.onResume();

		activity.getContentResolver().registerContentObserver(AddressBookProvider.CONTENT_URI, true, contentObserver);

		setAdapterContent();
	}

	@Override
	public void onPause()
	{
		activity.getContentResolver().unregisterContentObserver(contentObserver);

		super.onPause();
	}

	@Override
	public void onListItemClick(final ListView l, final View v, final int position, final long id)
	{
		final Map<String, Object> map = (Map<String, Object>) getListView().getAdapter().getItem(position);

		final String address = (String) map.get("addr");

		EditAddressBookEntryFragment.edit(getFragmentManager(), address);
	}


	public  void setAdapterContent() {
		try {
			synchronized(adapter) {

				labelMap = null;

				if (application.getRemoteWallet() == null) {
					return;
				}

				adapter.clear();  

				try {
					List<Map<String, Object>> keysMap = application.getRemoteWallet().getKeysMap();
										
					synchronized(application.getRemoteWallet()) {
						for (Map<String, Object> map : keysMap) {

							int tag = 0;
							if (map.get("tag") != null)
								tag = ((Number)map.get("tag")).intValue();

							if (tag != tag_filter)
								continue;

							if (map.get("label") != null)
								adapter.insert(map, 0);
							else
								adapter.add(map);
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		} catch (Exception e) { 
			e.printStackTrace();
		}
	}

	public void initAdapter() {
		adapter = new ArrayAdapter<Map<String, Object>>(activity, 0) {
			final Resources res = getResources();
			@Override
			public View getView(final int position, View row, final ViewGroup parent) {

				MyRemoteWallet wallet = application.getRemoteWallet();

				if (labelMap == null) {					
					if (wallet != null)
						labelMap = wallet.getLabelMap();
				}

				final Map<String, Object> map = (Map<String, Object>) getItem(position);

				String address = (String)map.get("addr");

				if (row == null)
					row = getLayoutInflater(null).inflate(R.layout.address_book_row, null);

				final TextView addressView = (TextView) row.findViewById(R.id.address_book_row_address);

				addressView.setText(address.toString());

				final TextView labelView = (TextView) row.findViewById(R.id.address_book_row_label);

				String label = null;

				if (labelMap != null)
					label = labelMap.get(address);

				if (label != null) {
					labelView.setText(label);
				}
				else {
					labelView.setText(R.string.wallet_addresses_fragment_unlabeled);
				}

				final TextView watchOnly = (TextView) row.findViewById(R.id.address_book_watch_only);

				String priv = (String)map.get("priv");
				if (priv == null) {
					watchOnly.setTextColor(Color.RED);
					watchOnly.setVisibility(View.VISIBLE);
				} else {
					watchOnly.setVisibility(View.GONE);
				}

				final TextView balanceView = (TextView) row.findViewById(R.id.address_book_balance_view);

				if (wallet == null) {
					balanceView.setVisibility(View.GONE);	
				} else {
					final BigInteger balance = wallet.getBalance(address);
					
					if (balance == null) {
						balanceView.setVisibility(View.GONE);	
					} else {
						balanceView.setVisibility(View.VISIBLE);	
						balanceView.setText(WalletUtils.formatValue(balance) + " BTC");
					}
				}

				return row;
			}
		};

		setListAdapter(adapter);
	}

	private final Handler handler = new Handler();

	private final ContentObserver contentObserver = new ContentObserver(handler)
	{
		@Override
		public void onChange(final boolean selfChange)
		{
			try {
				handler.post(new Runnable() {
					public void run() {
						setAdapterContent();
					}
				});
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	};
}

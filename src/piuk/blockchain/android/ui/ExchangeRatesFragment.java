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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.DecimalFormat;

import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v4.widget.SimpleCursorAdapter.ViewBinder;
import android.view.View;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import piuk.EventListeners;
import piuk.blockchain.android.R;
import piuk.blockchain.android.Constants;
import piuk.blockchain.android.ExchangeRatesProvider;
import piuk.blockchain.android.WalletApplication;

/**
 * @author Andreas Schildbach
 */
public final class ExchangeRatesFragment extends ListFragment implements
LoaderManager.LoaderCallbacks<Cursor> {
	private WalletApplication application;
	private SharedPreferences prefs;
	private SimpleCursorAdapter adapter;
	private final DecimalFormat format = new DecimalFormat("#.##");

	private final EventListeners.EventListener walletEventListener = new EventListeners.EventListener() {
		@Override
		public String getDescription() {
			return "Exchange Rates Listener";
		}

		@Override
		public void onWalletDidChange() {
			try {
				updateView();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	};

	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		application = (WalletApplication) getActivity().getApplication();

		EventListeners.addEventListener(walletEventListener);
	}

	@Override
	public void onActivityCreated(final Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());

		setEmptyText(getString(R.string.exchange_rates_fragment_empty_text));

		adapter = new SimpleCursorAdapter(getActivity(),
				R.layout.exchange_rate_row, null, new String[] {
			ExchangeRatesProvider.KEY_CURRENCY_CODE,
			ExchangeRatesProvider.KEY_EXCHANGE_RATE_15M,
			ExchangeRatesProvider.KEY_EXCHANGE_RATE_24H,
			ExchangeRatesProvider.KEY_EXCHANGE_RATE_SYMBOL}, new int[] {
			R.id.exchange_rate_currency_code,
			R.id.exchange_rate_value,
			R.id.exchange_up_down,
			R.id.exchange_rate_symbol}, 0);

		adapter.setViewBinder(new ViewBinder() {
			public boolean setViewValue(final View view, final Cursor cursor,
					final int columnIndex) {

				String columnName = cursor.getColumnName(columnIndex);

				if (columnName.equals(ExchangeRatesProvider.KEY_CURRENCY_CODE)) {
					return false;
				} else if (columnName.equals(ExchangeRatesProvider.KEY_EXCHANGE_RATE_15M)) {
					final TextView valueView = (TextView) view;

					final double _15m = cursor.getDouble(columnIndex);

					valueView.setText(format.format(_15m));
					
					return true;
				} else if (columnName.equals(ExchangeRatesProvider.KEY_EXCHANGE_RATE_15M)) {
					final TextView valueView = (TextView) view;

					final String symbol = cursor.getString(columnIndex);

					valueView.setText(symbol);
					
					return true;
				} else if (columnName.equals(ExchangeRatesProvider.KEY_EXCHANGE_RATE_24H)) {
					final ImageView image = (ImageView) view;
					
					double _15MValue = cursor.getDouble(cursor.getColumnIndex(ExchangeRatesProvider.KEY_EXCHANGE_RATE_15M));
					double _24HValue = cursor.getDouble(columnIndex);

					if (_15MValue > _24HValue) {
						image.setImageResource(R.drawable.icon_up_green);
					} else if (_15MValue < _24HValue) {
						image.setImageResource(R.drawable.icon_down_red);
					} else {
						image.setImageResource(R.drawable.icon_right_black);
					}

					return true;
				}
				
				return false;
			}
		});

		setListAdapter(adapter);

		getLoaderManager().initLoader(0, null, this);
	}

	@Override
	public void onResume() {
		super.onResume();

		updateView();
	}

	@Override
	public void onDestroy() {
		EventListeners.removeEventListener(walletEventListener);

		super.onDestroy();
	}

	@Override
	public void onListItemClick(final ListView l, final View v,
			final int position, final long id) {
		final Cursor cursor = (Cursor) adapter.getItem(position);
		final String currencyCode = cursor
				.getString(cursor
						.getColumnIndexOrThrow(ExchangeRatesProvider.KEY_CURRENCY_CODE));

		prefs.edit()
		.putString(Constants.PREFS_KEY_EXCHANGE_CURRENCY, currencyCode)
		.commit();

		final WalletBalanceFragment walletBalanceFragment = (WalletBalanceFragment) getFragmentManager()
				.findFragmentById(R.id.wallet_balance_fragment);

		if (walletBalanceFragment != null) {
			walletBalanceFragment.updateView();
		}

		System.out.println("Set Currency Code " + currencyCode);
		
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					application.setShouldDisplayLocalCurrency(true);
					
					application.getRemoteWallet().updateRemoteLocalCurrency(currencyCode);

					application.doMultiAddr(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

		}).start();

		getActivity().finish();
	}

	private void updateView() {
		final ListAdapter adapter = getListAdapter();
		if (adapter != null)
			((BaseAdapter) adapter).notifyDataSetChanged();
	}

	public Loader<Cursor> onCreateLoader(final int id, final Bundle args) {
		return new CursorLoader(getActivity(),
				ExchangeRatesProvider.CONTENT_URI, null, null, null, null);
	}

	public void onLoadFinished(final Loader<Cursor> loader, final Cursor data) {
		adapter.swapCursor(data);
	}

	public void onLoaderReset(final Loader<Cursor> loader) {
		adapter.swapCursor(null);
	}
}

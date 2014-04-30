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

package piuk.blockchain.android;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import piuk.blockchain.android.util.WalletUtils;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.provider.BaseColumns;


public class ExchangeRatesProvider extends ContentProvider {
	public static final Uri CONTENT_URI = Uri.parse("content://"
			+ Constants.PACKAGE_NAME + '.' + "exchange_rates");

	public static final String KEY_CURRENCY_CODE = "currency_code";
	public static final String KEY_EXCHANGE_RATE_15M = "exchange_rate_15m";
	public static final String KEY_EXCHANGE_RATE_24H = "exchange_rate_24h";
	public static final String KEY_EXCHANGE_RATE_SYMBOL = "exchange_rate_symbol";

	private Map<String, Rate> exchangeRates = null;

	@Override
	public boolean onCreate() {
		return true;
	}

	@Override
	public Cursor query(final Uri uri, final String[] projection,
			final String selection, final String[] selectionArgs,
			final String sortOrder) {

		if (exchangeRates == null) {
			exchangeRates = getExchangeRates();

			if (exchangeRates == null)
				return null;
		}

		final MatrixCursor cursor = new MatrixCursor(new String[] {
				BaseColumns._ID, KEY_CURRENCY_CODE, KEY_EXCHANGE_RATE_15M, KEY_EXCHANGE_RATE_24H, KEY_EXCHANGE_RATE_SYMBOL });

		if (selection == null) {
			for (final Map.Entry<String, Rate> entry : exchangeRates
					.entrySet())
				cursor.newRow().add(entry.getKey().hashCode())
				.add(entry.getKey()).add(entry.getValue()._15m).add(entry.getValue()._24hr).add(entry.getValue().symbol);
		} else if (selection.equals(KEY_CURRENCY_CODE)) {

			if (selectionArgs == null)
				return null;

			final String code = selectionArgs[0];
			final Rate rate = exchangeRates.get(code);

			if (rate == null)
				return null;

			cursor.newRow().add(code.hashCode()).add(code).add(rate._15m).add(rate._24hr).add(rate.symbol);
		}

		return cursor;
	}

	@Override
	public Uri insert(final Uri uri, final ContentValues values) {
		throw new UnsupportedOperationException();
	}

	@Override
	public int update(final Uri uri, final ContentValues values,
			final String selection, final String[] selectionArgs) {
		throw new UnsupportedOperationException();
	}

	@Override
	public int delete(final Uri uri, final String selection,
			final String[] selectionArgs) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getType(final Uri uri) {
		throw new UnsupportedOperationException();
	}

	public static class Rate {
		public String symbol;
		public double _15m;
		public double _24hr;
	}

	private static Map<String, Rate> getExchangeRates() {
		try {
			String response = WalletUtils.getURL("http://"+Constants.BLOCKCHAIN_DOMAIN+"/ticker");

			final Map<String, Rate> rates = new LinkedHashMap<String, Rate>();

			@SuppressWarnings("unchecked")
			final Map<String, JSONObject> root = (Map<String, JSONObject>) new JSONParser().parse(response);

			List<Map.Entry<String, JSONObject>> entries = new ArrayList<Map.Entry<String, JSONObject>>(root.entrySet());

			Collections.reverse(entries);

			for (Map.Entry<String, JSONObject> entry : entries) {
				String code = entry.getKey();

				Rate rate = new Rate();

				if (entry.getValue().get("15m") != null)
					rate._15m = ((Number)entry.getValue().get("15m")).doubleValue();

				if (entry.getValue().get("24h") != null)
					rate._24hr = ((Number)entry.getValue().get("24h")).doubleValue();

				if (entry.getValue().get("symbol") != null)
					rate.symbol = entry.getValue().get("symbol").toString();

				rates.put(code, rate);
			}

			return rates;
		} catch (final Exception e) {
			e.printStackTrace();
		} 

		return null;
	}
}

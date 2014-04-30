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

import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import piuk.blockchain.android.R;
import piuk.blockchain.android.util.ActionBarFragment;

public final class ExchangeRatesActivity extends AbstractWalletActivity {
	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.exchange_rates_content);

		final ActionBarFragment actionBar = getActionBarFragment();

		actionBar.setPrimaryTitle(R.string.exchange_rates_activity_title);

		actionBar.setBack(new OnClickListener() {
			public void onClick(final View v) {
				finish();
			}
		});
		
	}
}

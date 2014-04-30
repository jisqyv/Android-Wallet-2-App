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

import java.util.ArrayList;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.text.ClipboardManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Toast;

import com.google.bitcoin.core.Address;
import com.google.bitcoin.core.AddressFormatException;
import com.google.bitcoin.core.ECKey;
import com.google.bitcoin.core.NetworkParameters;
import com.google.bitcoin.core.WrongNetworkException;
import com.google.bitcoin.uri.BitcoinURIParseException;

import piuk.BitcoinAddress;
import piuk.BitcoinURI;
import piuk.MyRemoteWallet;
import piuk.MyWallet;
import piuk.blockchain.android.R;
import piuk.blockchain.android.Constants;
import piuk.blockchain.android.WalletApplication.AddAddressCallback;
import piuk.blockchain.android.ui.AbstractWalletActivity.QrCodeDelagate;
import piuk.blockchain.android.ui.dialogs.RequestPasswordDialog;
import piuk.blockchain.android.util.ActionBarFragment;
import piuk.blockchain.android.util.ViewPagerTabs;

/**
 * @author Andreas Schildbach
 */
public final class WalletAddressesActivity extends AbstractWalletActivity {
	public static void start(final Context context, final boolean sending) {
		final Intent intent = new Intent(context, WalletAddressesActivity.class);
		intent.putExtra(EXTRA_SENDING, sending);
		context.startActivity(intent);
	}

	private static final String EXTRA_SENDING = "sending";

	private WalletActiveAddressesFragment activeAddressesFragment;
	private WalletArchivedAddressesFragment archivedAddressesFragment;
	private SendingAddressesFragment sendingAddressesFragment;
	int pagerPosition = 0;
	private final Handler handler = new Handler();

	@Override
	public boolean onCreateOptionsMenu(final Menu menu)
	{
		super.onCreateOptionsMenu(menu);

		getMenuInflater().inflate(R.menu.addresses_menu, menu);

		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(final Menu menu)
	{
		super.onPrepareOptionsMenu(menu);

		{
			MenuItem item = menu.findItem(R.id.addresses_menu_generate);

			item.setVisible(pagerPosition == 0);
		}

		{
			MenuItem item = menu.findItem(R.id.addresses_menu_scan_watch_only);

			item.setVisible(pagerPosition == 0);
		}

		{
			MenuItem item = menu.findItem(R.id.addresses_menu_paste);

			item.setVisible(pagerPosition == 2);
		}

		{
			MenuItem item = menu.findItem(R.id.addresses_menu_scan_uri);

			item.setVisible(pagerPosition == 2);
		}

		return true;
	}


	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		switch (item.getItemId()) {
		case R.id.addresses_menu_generate:
			handleAddAddress();
			return true;
		case R.id.addresses_menu_paste:
			handlePasteClipboard();
			return true;
		case R.id.addresses_menu_scan_uri:

			showQRReader(new QrCodeDelagate() {
				@Override
				public void didReadQRCode(String contents) throws Exception {
					try {
						final BitcoinAddress address;

						if (contents.matches("[a-zA-Z0-9]*")) {
							address = new BitcoinAddress(contents);
						} else {
							final BitcoinURI bitcoinUri = new BitcoinURI(contents);
							address = bitcoinUri.getAddress();
						}

						handler.postDelayed(new Runnable() {
							public void run() {
								EditAddressBookEntryFragment.edit(
										getSupportFragmentManager(), address.toString());
							}
						}, 500);
					} catch (final AddressFormatException x) {
						errorDialog(R.string.send_coins_uri_parse_error_title, contents);
					} catch (final BitcoinURIParseException x) {
						errorDialog(R.string.send_coins_uri_parse_error_title, contents);
					}
				}
			});

			return true;
		case R.id.addresses_menu_scan_watch_only:
			if (application.getRemoteWallet() == null)
				return false;

			System.out.println("showQRReader()");

			showQRReader(new QrCodeDelagate() {
				@Override
				public void didReadQRCode(String data) throws Exception {
					handleAddWatchOnly(data);
				}
			});
			return true;
		case R.id.addresses_menu_scan_private_key:
			if (application.getRemoteWallet() == null)
				return false;

			showQRReader(new QrCodeDelagate() {
				@Override
				public void didReadQRCode(String data) throws Exception {
					handleScanPrivateKey(data);
				}
			});
			return true;
		}



		return false;
	}

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.address_book_content);

		final ActionBarFragment actionBar = getActionBarFragment();

		actionBar.setPrimaryTitle(R.string.address_book_activity_title);

		actionBar.setBack(new OnClickListener() {
			public void onClick(final View v) {
				finish();
			}
		});

		actionBar.addButton(android.R.drawable.ic_menu_more).setOnClickListener(
				new OnClickListener() {
					public void onClick(final View v) {
						openOptionsMenu();
					}
				});

		final ViewPager pager = (ViewPager) findViewById(R.id.address_book_pager);

		if (pager != null) {
			final ViewPagerTabs pagerTabs = (ViewPagerTabs) findViewById(R.id.address_book_pager_tabs);
			pagerTabs.addTabLabels(R.string.address_book_list_receiving_title,
					R.string.address_book_list_archived_title,
					R.string.address_book_list_sending_title);

			final ProxyOnPageChangeListener pagerListener = new ProxyOnPageChangeListener(
					pagerTabs) {
				@Override
				public void onPageSelected(final int position) {					
					super.onPageSelected(position);

					pagerPosition = position;
				}
			};

			final PagerAdapter pagerAdapter = new PagerAdapter(
					getSupportFragmentManager());

			pager.getCurrentItem();

			pager.setAdapter(pagerAdapter);
			pager.setOnPageChangeListener(pagerListener);
			final int position = getIntent().getBooleanExtra(EXTRA_SENDING,
					true) == true ? 2 : 0;
			pager.setCurrentItem(position);
			pager.setPageMargin(2);
			pager.setPageMarginDrawable(R.color.background_less_bright);

			pagerListener.onPageSelected(position);
			pagerListener.onPageScrolled(position, 0, 0);

			archivedAddressesFragment = new WalletArchivedAddressesFragment();
			activeAddressesFragment = new WalletActiveAddressesFragment();
			sendingAddressesFragment = new SendingAddressesFragment();
		}

		updateFragments();
	}

	private void updateFragments() {

		if (application.getRemoteWallet() == null)
			return;

		final String[] addressesArray = application.getRemoteWallet().getActiveAddresses();
		final ArrayList<Address> addresses = new ArrayList<Address>(addressesArray.length);

		for (final String address : addressesArray) {
			try {
				addresses.add(new Address(Constants.NETWORK_PARAMETERS, address));
			} catch (WrongNetworkException e) {
				e.printStackTrace();
			} catch (AddressFormatException e) {
				e.printStackTrace();
			}
		}

		sendingAddressesFragment.setWalletAddresses(addresses);
	}


	private class PagerAdapter extends FragmentStatePagerAdapter {
		public PagerAdapter(final FragmentManager fm) {
			super(fm);
		}

		@Override
		public int getCount() {
			return 3;
		}

		@Override
		public Fragment getItem(final int position) {
			if (position == 0)
				return activeAddressesFragment;
			else if (position == 1)
				return archivedAddressesFragment;
			else
				return sendingAddressesFragment;
		}
	}

	private void handlePasteClipboard() {
		final ClipboardManager clipboardManager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);

		if (clipboardManager.hasText()) {
			final String text = clipboardManager.getText().toString().trim();

			try {
				final Address address = new Address(
						Constants.NETWORK_PARAMETERS, text);
				EditAddressBookEntryFragment.edit(getSupportFragmentManager(),
						address.toString());
			} catch (final AddressFormatException x) {
				toast(R.string.send_coins_parse_address_error_msg);
			}
		} else {
			toast(R.string.address_book_msg_clipboard_empty);
		}
	}


	private void reallyGenerateAddress() {

		ECKey key = application.getRemoteWallet().generateECKey();

		application.addKeyToWallet(key, key.toAddress(NetworkParameters.prodNet()).toString(), null, 0,
				new AddAddressCallback() {

			public void onSavedAddress(String address) {
				Toast.makeText(self, getString(R.string.toast_generated_address, address), Toast.LENGTH_LONG).show();

				EditAddressBookEntryFragment.edit(
						getSupportFragmentManager(), address);

				updateFragments();
			}

			public void onError(String reason) {
				Toast.makeText(self, reason, Toast.LENGTH_LONG).show();

				updateFragments();
			}
		});
	}

	private void handleAddAddress() {
		if (application.getRemoteWallet() == null)
			return;

		final MyRemoteWallet remoteWallet = application.getRemoteWallet();

		if (remoteWallet == null)
			return;

		application.checkIfWalletHasUpdatedAndFetchTransactions(remoteWallet.getTemporyPassword(), new SuccessCallback() {
			@Override
			public void onSuccess() {
				if (remoteWallet.isDoubleEncrypted() == false) {
					reallyGenerateAddress();
				} else {
					if (remoteWallet.temporySecondPassword == null) {
						RequestPasswordDialog.show(
								getSupportFragmentManager(),
								new SuccessCallback() {

									public void onSuccess() {
										reallyGenerateAddress();
									}

									public void onFail() {
										Toast.makeText(
												getApplication(),
												R.string.generate_key_no_password_error,
												Toast.LENGTH_LONG)
												.show();
									}
								}, RequestPasswordDialog.PasswordTypeSecond);
					} else {
						reallyGenerateAddress();
					}
				}
			}

			@Override
			public void onFail() {
				Toast.makeText(
						getApplication(),
						R.string.toast_error_syncing_wallet,
						Toast.LENGTH_LONG)
						.show();
			}
		});

		updateFragments();
	}

	private class ProxyOnPageChangeListener implements OnPageChangeListener {
		private final OnPageChangeListener onPageChangeListener;

		public ProxyOnPageChangeListener(
				final OnPageChangeListener onPageChangeListener) {
			this.onPageChangeListener = onPageChangeListener;
		}

		public void onPageScrolled(final int position,
				final float positionOffset, final int positionOffsetPixels) {
			onPageChangeListener.onPageScrolled(position, positionOffset,
					positionOffsetPixels);
		}

		public void onPageSelected(final int position) {
			onPageChangeListener.onPageSelected(position);
		}

		public void onPageScrollStateChanged(final int state) {
			onPageChangeListener.onPageScrollStateChanged(state);
		}
	}
}

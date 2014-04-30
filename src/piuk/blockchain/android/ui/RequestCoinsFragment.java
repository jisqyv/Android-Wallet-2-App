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

import java.io.DataOutputStream;
import java.math.BigInteger;
import java.net.URL;

import org.apache.commons.io.IOUtils;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.bitcoin.core.Address;
import com.google.bitcoin.uri.BitcoinURI;

import piuk.BitcoinAddress;
import piuk.MyRemoteWallet;
import piuk.blockchain.android.R;
import piuk.blockchain.android.WalletApplication;
import piuk.blockchain.android.ui.CurrencyAmountView.Listener;
import piuk.blockchain.android.util.ActionBarFragment;
import piuk.blockchain.android.util.QrDialog;
import piuk.blockchain.android.util.WalletUtils;

/**
 * @author Andreas Schildbach
 */
public final class RequestCoinsFragment extends Fragment {
	private WalletApplication application;

	private ImageView qrView;
	private Bitmap qrCodeBitmap;
	private CurrencyAmountView amountView;
	private Button generateSharedButton;


	@Override
	public View onCreateView(final LayoutInflater inflater,
			final ViewGroup container, final Bundle savedInstanceState) {

		application = (WalletApplication) getActivity().getApplication();

		final View view = inflater.inflate(R.layout.request_coins_fragment,
				container);

		qrView = (ImageView) view.findViewById(R.id.request_coins_qr);
		qrView.setOnClickListener(new OnClickListener() {
			public void onClick(final View v) {
				new QrDialog(getActivity(), qrCodeBitmap).show();
			}
		});


		final Handler handler = new Handler();

		generateSharedButton = (Button)view
				.findViewById(R.id.generate_shared_address);

		generateSharedButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				new Thread() {
					public void run() {
						try {
							final String address = MyRemoteWallet.generateSharedAddress(determineAddressStr());

							new BitcoinAddress(address);

							handler.post(new Runnable() {

								@Override
								public void run() {
									try {
										updateView(address);

										Toast.makeText(application, "Generated new shared address", Toast.LENGTH_SHORT)
										.show();
									} catch (Exception e) {
										e.printStackTrace();
									}
								}
							});
						} catch (final Exception e) {
							e.printStackTrace();

							handler.post(new Runnable() {

								@Override
								public void run() {									
									Toast.makeText(application, e.getLocalizedMessage(), Toast.LENGTH_SHORT)
									.show();
								}
							});
						}
					}
				}.start();
			}
		});

		amountView = (CurrencyAmountView) view
				.findViewById(R.id.request_coins_amount);
		amountView.setListener(new Listener() {
			public void changed() {
				updateView(determineAddressStr());
			}

			public void done() {
			}
		});
		amountView.setContextButton(R.drawable.ic_input_calculator,
				new OnClickListener() {
			public void onClick(final View v) {
				final FragmentTransaction ft = getFragmentManager()
						.beginTransaction();
				final Fragment prev = getFragmentManager()
						.findFragmentByTag(
								AmountCalculatorFragment.FRAGMENT_TAG);
				if (prev != null)
					ft.remove(prev);
				ft.addToBackStack(null);
				final DialogFragment newFragment = new AmountCalculatorFragment(
						new AmountCalculatorFragment.Listener() {
							public void use(final BigInteger amount) {
								amountView.setAmount(amount);
							}
						});
				newFragment.show(ft,
						AmountCalculatorFragment.FRAGMENT_TAG);
			}
		});

		return view;
	}

	@Override
	public void onAttach(final Activity activity) {
		super.onAttach(activity);

		final ActionBarFragment actionBar = ((AbstractWalletActivity) activity)
				.getActionBarFragment();

		actionBar.addButton(R.drawable.ic_action_share).setOnClickListener(
				new OnClickListener() {
					public void onClick(final View v) {
						startActivity(Intent
								.createChooser(
										new Intent(Intent.ACTION_SEND)
										.putExtra(Intent.EXTRA_TEXT,
												determineAddressStr())
												.setType("text/plain"),
												getActivity()
												.getString(
														R.string.request_coins_share_dialog_title)));
					}
				});

		actionBar.addButton(R.drawable.ic_action_address_book).setOnClickListener(
				new OnClickListener() {
					public void onClick(final View v) {
						WalletAddressesActivity.start(getActivity(), true);
					}
				}); 
	}

	@Override
	public void onResume() {
		super.onResume();

		updateView(determineAddressStr());
	}

	@Override
	public void onPause() {
		super.onPause();
	}

	private void updateView(String address) {
		if (address == null)
			return;

		final BigInteger amount = amountView.getAmount();

		address = BitcoinURI.convertToBitcoinURI(address, amount, null, null)
				.toString();

		if (qrCodeBitmap != null)
			qrCodeBitmap.recycle();

		final int size = (int) (256 * getResources().getDisplayMetrics().density);
		qrCodeBitmap = WalletUtils.getQRCodeBitmap(address, size);
		qrView.setImageBitmap(qrCodeBitmap);
	}

	private String determineAddressStr() {
		final Address address = application.determineSelectedAddress();

		if (address == null)
			return null;

		return address.toString();
	}
}

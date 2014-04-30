/*
 * Copyright 2011-2013 the original author or authors.
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

import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;

import com.google.bitcoin.core.Transaction;

import piuk.EventListeners;
import piuk.blockchain.android.Constants;
import piuk.blockchain.android.R;
import piuk.blockchain.android.service.BlockchainService;

import android.app.Activity;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * @author Andreas Schildbach
 */
public final class BlockchainStateFragment extends Fragment implements OnSharedPreferenceChangeListener
{
	private Activity activity;
	private TextView progressView;

	private int download;
	private Date bestChainDate;

	private final Handler delayMessageHandler = new Handler();
	
	private final EventListeners.EventListener eventListener = new EventListeners.EventListener() {
		@Override
		public String getDescription() {
			return "Blockchain State Listener";
		}

		@Override
		public void onWalletDidChange() {	
			updateView();
		}
	};

	private final class BlockchainBroadcastReceiver extends BroadcastReceiver
	{
		public AtomicBoolean active = new AtomicBoolean(false);

		@Override
		public void onReceive(final Context context, final Intent intent)
		{
			download = intent.getIntExtra(BlockchainService.ACTION_BLOCKCHAIN_STATE_DOWNLOAD, BlockchainService.ACTION_BLOCKCHAIN_STATE_DOWNLOAD_OK);
			bestChainDate = (Date) intent.getSerializableExtra(BlockchainService.ACTION_BLOCKCHAIN_STATE_BEST_CHAIN_DATE);

			if (active.get())
				updateView();
		}
	}

	private final BlockchainBroadcastReceiver broadcastReceiver = new BlockchainBroadcastReceiver();

	@Override
	public void onAttach(final Activity activity)
	{
		super.onAttach(activity);

		this.activity = activity;
		PreferenceManager.getDefaultSharedPreferences(activity);
	}

	@Override
	public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState)
	{
		EventListeners.addEventListener(eventListener);

		final View view = inflater.inflate(R.layout.blockchain_state_fragment, container);

		progressView = (TextView) view.findViewById(R.id.blockchain_state_progress);

		return view;
	}

	@Override
	public void onResume()
	{
		super.onResume();

		activity.registerReceiver(broadcastReceiver, new IntentFilter(BlockchainService.ACTION_BLOCKCHAIN_STATE));
		broadcastReceiver.active.set(true);

		updateView();
	}

	@Override
	public void onPause()
	{
		broadcastReceiver.active.set(false);
		activity.unregisterReceiver(broadcastReceiver);

		super.onPause();
	}

	@Override
	public void onDestroy()
	{
		EventListeners.removeEventListener(eventListener);

		delayMessageHandler.removeCallbacksAndMessages(null);

		super.onDestroy();
	}

	private void updateView()
	{
		final boolean showProgress;

		AbstractWalletActivity activity = (AbstractWalletActivity) this.getActivity();

		if (activity == null)
			return;
		
		if (!activity.application.isInP2PFallbackMode()) {
			showProgress = false;
		} else if (download != BlockchainService.ACTION_BLOCKCHAIN_STATE_DOWNLOAD_OK)
		{
			showProgress = true;

			if ((download & BlockchainService.ACTION_BLOCKCHAIN_STATE_DOWNLOAD_STORAGE_PROBLEM) != 0)
				progressView.setText(R.string.blockchain_state_progress_problem_storage);
			else if ((download & BlockchainService.ACTION_BLOCKCHAIN_STATE_DOWNLOAD_NETWORK_PROBLEM) != 0)
				progressView.setText(R.string.blockchain_state_progress_problem_network);
		}
		else if (bestChainDate != null)
		{
			final long blockchainLag = System.currentTimeMillis() - bestChainDate.getTime();
			final boolean blockchainUptodate = blockchainLag < Constants.BLOCKCHAIN_UPTODATE_THRESHOLD_MS;

			showProgress = !blockchainUptodate;

			final String downloading = getString(R.string.blockchain_state_progress_downloading);
			final String stalled = getString(R.string.blockchain_state_progress_stalled);

			final String stalledText;
			if (blockchainLag < 2 * DateUtils.DAY_IN_MILLIS)
			{
				final long hours = blockchainLag / DateUtils.HOUR_IN_MILLIS;
				progressView.setText(getString(R.string.blockchain_state_progress_hours, downloading, hours));
				stalledText = getString(R.string.blockchain_state_progress_hours, stalled, hours);
			}
			else if (blockchainLag < 2 * DateUtils.WEEK_IN_MILLIS)
			{
				final long days = blockchainLag / DateUtils.DAY_IN_MILLIS;
				progressView.setText(getString(R.string.blockchain_state_progress_days, downloading, days));
				stalledText = getString(R.string.blockchain_state_progress_days, stalled, days);
			}
			else if (blockchainLag < 90 * DateUtils.DAY_IN_MILLIS)
			{
				final long weeks = blockchainLag / DateUtils.WEEK_IN_MILLIS;
				progressView.setText(getString(R.string.blockchain_state_progress_weeks, downloading, weeks));
				stalledText = getString(R.string.blockchain_state_progress_weeks, stalled, weeks);
			}
			else
			{
				final long months = blockchainLag / (30 * DateUtils.DAY_IN_MILLIS);
				progressView.setText(getString(R.string.blockchain_state_progress_months, downloading, months));
				stalledText = getString(R.string.blockchain_state_progress_months, stalled, months);
			}

			delayMessageHandler.removeCallbacksAndMessages(null);
			delayMessageHandler.postDelayed(new Runnable()
			{
				public void run()
				{
					progressView.setText(stalledText);
				}
			}, Constants.BLOCKCHAIN_DOWNLOAD_THRESHOLD_MS);
		}
		else
		{
			showProgress = false;
		}

		progressView.setVisibility(activity.application.isInP2PFallbackMode() ? View.VISIBLE : View.GONE);

		if (activity.application.isInP2PFallbackMode() && !showProgress) {
			progressView.setText(R.string.running_in_p2p_mode);
		} 

		getView().setVisibility(activity.application.isInP2PFallbackMode() ? View.VISIBLE : View.GONE);

	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		// TODO Auto-generated method stub

	}
}

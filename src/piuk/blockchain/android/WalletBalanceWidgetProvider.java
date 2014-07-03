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

import info.blockchain.wallet.ui.BlockchainUtil;
import info.blockchain.wallet.ui.MainActivity;
import info.blockchain.wallet.ui.PinEntryActivity;

import java.math.BigInteger;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.RemoteViews;
import piuk.blockchain.android.R;

/**
 * @author Andreas Schildbach
 */
public class WalletBalanceWidgetProvider extends AppWidgetProvider {
	final public static String ACTION_WIDGET_SEND_SCREEN ="piuk.blockchain.android.intent.action.ACTION_WIDGET_SEND_SCREEN";
	final public static String ACTION_WIDGET_SCAN_RECEIVING ="piuk.blockchain.android.intent.action.ACTION_WIDGET_SCAN_RECEIVING";
	final public static String ACTION_WIDGET_REFRESH_BALANCE ="piuk.blockchain.android.intent.action.ACTION_WIDGET_REFRESH_BALANCE";

	@Override
	public void onUpdate(final Context context, final AppWidgetManager appWidgetManager, final int[] appWidgetIds) {
		Log.d("wiiiiget", "wiiiiget onUpdate");
		final RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.wallet_balance_widget_content);
		
		WalletBalanceWidgetProvider.setBalance(context, remoteViews);
		
        // register for button event
		registerButtons(context, remoteViews);
		pushWidgetUpdate(context, remoteViews);
	}
	
	public static void setBalance(final Context context, final RemoteViews remoteViews) {
		try {
			final WalletApplication application = (WalletApplication) context.getApplicationContext();
			final String balanceStr;
			if (application.getRemoteWallet() == null) {
				balanceStr = BlockchainUtil.formatBitcoin(BigInteger.ZERO);
			} else {
				BigInteger balance = application.getRemoteWallet().getFinal_balance();
				balanceStr = BlockchainUtil.formatBitcoin(balance);
			}
			remoteViews.setTextViewText(R.id.widget_wallet_balance, balanceStr);			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void registerButtons(Context context, RemoteViews remoteViews) {
		remoteViews.setOnClickPendingIntent(R.id.scan_button,
                buildButtonPendingIntent(context, ACTION_WIDGET_SCAN_RECEIVING));
		
		remoteViews.setOnClickPendingIntent(R.id.refresh_button,
                buildButtonPendingIntent(context, ACTION_WIDGET_REFRESH_BALANCE));

		remoteViews.setOnClickPendingIntent(R.id.send_button,
                buildButtonPendingIntent(context, ACTION_WIDGET_SEND_SCREEN));
	}
	
	public static void pushWidgetUpdate(Context context, RemoteViews remoteViews) {
		ComponentName myWidget = new ComponentName(context,	WalletBalanceWidgetProvider.class);
		AppWidgetManager manager = AppWidgetManager.getInstance(context);
		manager.updateAppWidget(myWidget, remoteViews);
	}
	
    public static PendingIntent buildButtonPendingIntent(Context context, String action) {
        Intent intent = new Intent();
        intent.setAction(action);
        return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

	@Override
	public void onReceive(Context context, Intent intent) {
		String action = intent.getAction();
		RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.wallet_balance_widget_content);

		if (action.equals(WalletBalanceWidgetProvider.ACTION_WIDGET_SEND_SCREEN)) {
            //final Intent navigateIntent = new Intent(context, PinEntryActivity.class);
            final Intent navigateIntent = new Intent(context, MainActivity.class);

            navigateIntent.putExtra("navigateTo", "sendScreen");            
            remoteViews.setOnClickPendingIntent(R.id.widget_frame,
                            PendingIntent.getActivity(context, 0, navigateIntent, 0));            
            navigateIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            context.startActivity(navigateIntent);
            
		} else if (action.equals(WalletBalanceWidgetProvider.ACTION_WIDGET_SCAN_RECEIVING)) {
            //final Intent navigateIntent = new Intent(context, PinEntryActivity.class);
            final Intent navigateIntent = new Intent(context, MainActivity.class);

            navigateIntent.putExtra("navigateTo", "scanReceiving");            
            remoteViews.setOnClickPendingIntent(R.id.widget_frame,
                            PendingIntent.getActivity(context, 0, navigateIntent, 0));            
            navigateIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            context.startActivity(navigateIntent);            
		
		} else if (action.equals(WalletBalanceWidgetProvider.ACTION_WIDGET_REFRESH_BALANCE)) {
			WalletBalanceWidgetProvider.setBalance(context, remoteViews);
		}
		
		// re-registering for click listener
		registerButtons(context, remoteViews);
		WalletBalanceWidgetProvider.pushWidgetUpdate(context.getApplicationContext(), remoteViews);
	}
}

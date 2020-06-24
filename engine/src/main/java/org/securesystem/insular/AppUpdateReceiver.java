package org.securesystem.insular;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.securesystem.insular.provisioning.IslandProvisioning;
import org.securesystem.insular.util.Users;

/**
 * Handle {@link Intent#ACTION_MY_PACKAGE_REPLACED}
 *
 * Created by Oasis on 2017/7/20.
 */
public class AppUpdateReceiver extends BroadcastReceiver {

	@Override public void onReceive(final Context context, final Intent intent) {
		// Currently, just blindly start the device owner provisioning, since it is idempotent, at least at present.
		if (Intent.ACTION_MY_PACKAGE_REPLACED.equals(intent.getAction()))
			if (Users.isOwner()) IslandProvisioning.startOwnerUserPostProvisioningIfNeeded(context);
	}
}

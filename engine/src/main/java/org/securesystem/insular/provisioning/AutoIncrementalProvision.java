package org.securesystem.insular.provisioning;

import com.oasisfeng.android.os.Loopers;
import org.securesystem.insular.engine.BuildConfig;
import org.securesystem.insular.util.ProfileUser;
import org.securesystem.insular.util.Users;
import com.oasisfeng.pattern.PseudoContentProvider;
import com.oasisfeng.perf.Performances;
import com.oasisfeng.perf.Stopwatch;

/**
 * Perform incremental provision
 *
 * Created by Oasis on 2017/11/21.
 */
public class AutoIncrementalProvision extends PseudoContentProvider {

	@Override public boolean onCreate() {
		final Stopwatch stopwatch = Performances.startUptimeStopwatch();
		if (Users.isOwner()) {
			Loopers.addIdleTask(() -> IslandProvisioning.startOwnerUserPostProvisioningIfNeeded(context()));
		} else if (Users.isProfileManagedByIsland()) {	// False if profile is not enabled yet. (during the broadcast ACTION_PROFILE_PROVISIONING_COMPLETE)
			final Thread thread = new Thread(this::startInProfile);
			thread.setPriority(Thread.MIN_PRIORITY);
			thread.start();
		}
		if (BuildConfig.DEBUG) Performances.check(stopwatch, 5, "IncPro.MainThread");
		return false;
	}

	@ProfileUser private void startInProfile() {
		final Stopwatch stopwatch = Performances.startUptimeStopwatch();
		IslandProvisioning.performIncrementalProfileOwnerProvisioningIfNeeded(context());
		if (BuildConfig.DEBUG) Performances.check(stopwatch, 10, "IncPro.WorkerThread");
	}
}

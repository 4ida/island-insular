package org.securesystem.insular.shuttle;

import android.content.ComponentName;
import org.securesystem.insular.shuttle.IUnbinder;

interface IServiceConnection {
    oneway void onServiceConnected(in ComponentName name, in IBinder service, in IUnbinder unbinder);
    oneway void onServiceDisconnected(in ComponentName name);
    oneway void onServiceFailed();
}

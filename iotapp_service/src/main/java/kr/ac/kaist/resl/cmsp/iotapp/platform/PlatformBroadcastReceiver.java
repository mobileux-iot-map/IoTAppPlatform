package kr.ac.kaist.resl.cmsp.iotapp.platform;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Created by shheo on 15. 4. 7.
 */
public class PlatformBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        startService(context);
    }

    public static void startService(Context context) {
        Intent myIntent = new Intent(context, PlatformService.class);
        context.startService(myIntent);
    }

    public static void stopService(Context context) {
        Intent myIntent = new Intent(context, PlatformService.class);
        context.stopService(myIntent);
    }
}

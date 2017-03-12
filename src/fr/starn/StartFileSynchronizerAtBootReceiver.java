package fr.starn;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class StartFileSynchronizerAtBootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
			Intent i = new Intent();
			i.setAction("fr.starn.FileSynchronizerService");
			context.startService(i);
        }
    }
}
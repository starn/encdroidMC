package fr.starn;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class FileSynchronizerAlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
          context.startService(new Intent(context, FileSynchronizerService.class));
    }
}

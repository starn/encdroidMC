package org.mrpdaemon.android.encdroidmc.asyncTasks;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.support.v4.app.NotificationCompat;
 
public class NotificationHelper {
    private Context mContext;
    private int NOTIFICATION_ID = 1;
    private static int nbNotif;
//    private Notification mNotification;
    private NotificationManager mNotificationManager;
    private PendingIntent mContentIntent;
    public CharSequence mContentTitle;
    private NotificationCompat.Builder builder;
    
    public long sum;
    private long max;
    private int percent;
    
    private boolean isCanceled;
    
    
    public NotificationHelper(Context context)
    {
        mContext = context;
        NOTIFICATION_ID=nbNotif;
        nbNotif++;
        createNotification();
    }
 
    /**
     * Put the notification into the status bar
     */
    public void createNotification() {
        //get the notification manager
        mNotificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
 
        //create the notification
        int icon = android.R.drawable.stat_sys_download;
        CharSequence tickerText = "Please wait..."; //Initial text that appears in the status bar
        long when = System.currentTimeMillis();
        
        
//        mNotification = new Notification(icon, tickerText, when);
// 
//        //create the content which is shown in the notification pulldown
//        mContentTitle = "Copie en cours"; //Full title of the notification in the pull down
//        CharSequence contentText = "0% complete"; //Text of the notification in the pull down
// 
//        //you have to set a PendingIntent on a notification to tell the system what you want it to do when the notification is selected
//        //I don't want to use this here so I'm just creating a blank one
//        Intent notificationIntent = new Intent();
//        mContentIntent = PendingIntent.getActivity(mContext, 0, notificationIntent, 0);
// 
//        //add the additional content and intent to the notification
//        mNotification.setLatestEventInfo(mContext, mContentTitle, contentText, mContentIntent);
// 
//        //make this notification appear in the 'Ongoing events' section
//        mNotification.flags = Notification.FLAG_ONGOING_EVENT;
        
        builder = new NotificationCompat.Builder(mContext)
        .setContentText("")
        .setContentTitle("zzz")
        .setSmallIcon(icon)
        .setAutoCancel(false)
        .setOngoing(true)
        .setOnlyAlertOnce(true);
//        .addAction(running ? R.drawable.ic_action_pause 
//                           : R.drawable.ic_action_play,
//                   running ? context.getString(R.string.pause)
//                           : context.getString(R.string.start),
//                   startPendingIntent)
//        .addAction(R.drawable.ic_action_stop, context.getString(R.string.stop),
//                stopPendingIntent);


 
    }

    public void show(){
        //show the notification
        mNotificationManager.notify(NOTIFICATION_ID, builder.build());

    }
    
    /**
     * Receives progress updates from the background task and updates the status bar notification appropriately
     * @param percentageComplete
     */
    public void progressUpdate() {
        CharSequence contentText = percent + "% complete";
        builder.setContentText(contentText);
        mNotificationManager.notify(NOTIFICATION_ID, builder.build());
    }
 
    public void incrementProgressBy(long num){
    	sum+=num;
    	int newPercent =100-Math.round((float)(max-sum)/max*100);
    	if (percent!=newPercent){
    		percent=newPercent;
    		progressUpdate();
    	}
    }
    
    public void setProgress(long value){
    	sum=value;
    	int newPercent =100-Math.round((float)(max-sum)/max*100);
    	if (percent!=newPercent){
    		percent=newPercent;
    		progressUpdate();
    	}
    }
    
    /**
     * called when the background task is complete, this removes the notification from the status bar.
     * We could also use this to add a new ‘task complete’ notification
     */
    public void completed()    {
        //remove the notification from the status bar
        mNotificationManager.cancel(NOTIFICATION_ID);
    }
    
    public void setMax(long max){
    	this.max=max;
    }
    
    public long getMax(){
    	return max;
    }
    
    public void dismiss(){
    	mNotificationManager.cancel(NOTIFICATION_ID);
    	isCanceled=true;
    }
    
    public boolean isShowing(){
    	return !isCanceled;
    }
    
    public long getProgress() {
    	return sum;
    }
    
    public void setTitle(String title){
        builder.setContentTitle(title);
        mNotificationManager.notify(NOTIFICATION_ID, builder.build());
    }
}

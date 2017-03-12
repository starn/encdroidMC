package fr.starn;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.mrpdaemon.android.encdroidmc.ConfigSyncActivity;
import org.mrpdaemon.android.encdroidmc.EDApplication;
import org.mrpdaemon.android.encdroidmc.EDDBHelper;
import org.mrpdaemon.android.encdroidmc.EDVolume;
import org.mrpdaemon.android.encdroidmc.EDVolumeListActivity;
import org.mrpdaemon.android.encdroidmc.R;
import org.mrpdaemon.android.encdroidmc.asyncTasks.ImportFileTask;
import org.mrpdaemon.android.encdroidmc.fileProvider.EncdroidFileProvider;
import org.mrpdaemon.sec.encfs.EncFSInvalidPasswordException;
import org.mrpdaemon.sec.encfs.EncFSVolume;
import org.mrpdaemon.sec.encfs.EncFSVolumeBuilder;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;

public class FileSynchronizerService  extends Service {
	private static List<FileSynchronizerRule> rules = new ArrayList<FileSynchronizerRule>();
	private static FileSynchronizerService instance;
	private static boolean syncIsRunning;
	EDApplication mApp;
	
	
	
	@Override
	public void onCreate() {
		EDDBHelper dbHelper = new EDDBHelper(this);
		if (EDApplication.volumeList==null) EDApplication.volumeList = dbHelper.getVolumes();
		
		AlarmManager alarmMgr = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
		PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, new Intent(this, FileSynchronizerAlarmReceiver.class),PendingIntent.FLAG_CANCEL_CURRENT);
		// Use inexact repeating which is easier on battery (system can phase events and not wake at exact times)
		alarmMgr.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,  AlarmManager.INTERVAL_HOUR, AlarmManager.INTERVAL_HOUR, pendingIntent);
		instance = this;
		mApp = (EDApplication) getApplication();
		List<FileSynchronizerRule> syncRules = mApp.getDbHelper().getSyncRules();
		FileSynchronizerService.refreshRules(syncRules);
	}
	

	public static FileSynchronizerService getInstance(){
		return instance;
	}

	public static void refreshRules(List<FileSynchronizerRule> rules){
		FileSynchronizerService.rules = rules;
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		//System.out.println("******* onStartCommand");
		if (rules == null || rules.size()==0) {
			return 0;
		}
		for (FileSynchronizerRule rule: rules){
	        WifiManager wifiManager = (WifiManager)this.getSystemService(Context.WIFI_SERVICE);
	        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
	        boolean wifiConnection = wifiInfo.getIpAddress()!=0;
	        //System.out.println(wifiConnection);
			if (!rule.getSyncOnlyOnWifiBool() || (rule.getSyncOnlyOnWifiBool()&& wifiConnection)) {
				try {
					launchRule(rule.getId());
//					SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
//					refreshNotifyBar(rules.size()+" sync - Last sync: "+sdf.format(new Date()));
				} catch (EncFSInvalidPasswordException e){
					FileSynchronizerService.getInstance().refreshNotifyBar("Invalid password");
				} catch (Throwable t){
					t.printStackTrace();
					refreshNotifyBar("Sync error: "+t.getMessage());
				}
			}
			else {
				//System.out.println("rule not launched: no wifi");
			}
		}
		

		
		return 0;
	}
	
	public void hideNotifyBar(){
		if (rules.size()==0){
			NotificationManager mNotificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
			mNotificationManager.cancel(999);
		}
	}
	
	public void launchRule(int ruleID ) throws Throwable{
		if (ruleID==-1) refreshNotifyBar("You must save the rule before run");
		if (syncIsRunning) return;
		syncIsRunning = true;
			FileSynchronizerRule rule = getRuleFromId(ruleID);
			EDVolume v = getVolumeFromName(rule.getVolumeNameToSync());
			if (v.getVolume()==null){
//				//unlock volume is necessary
//				Intent dialogIntent = new Intent(this, EDVolumeListActivity.class);
//				dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//				dialogIntent.putExtra("ctx", "fileSynchronizer");
//				dialogIntent.putExtra("ruleID", ruleID);
//				dialogIntent.putExtra("volumeName", rule.getVolumeNameToSync());
//				dialogIntent.putExtra("password",rule.getVolumePassword() );
//				System.out.println("launch fileBrowser activity");
//				startActivity(dialogIntent);
				
				//instanciate file provider and volume
				String providerParams=v.getSerializedFileProviderParams();
				int idProvider = v.getFileProviderId();
				EncdroidFileProvider fileProvider = EncdroidFileProvider.getProviderById(idProvider);
				if (instance==null) {
					//provider not installed, should not be possible
					return;
				}
				fileProvider.setParamValues(EncdroidFileProvider.unserializeParams( providerParams));
				try {
					fileProvider.init("/");
				} catch (Exception e){
					throw new RuntimeException(e);
				}
				fileProvider.setReady(true);
				fileProvider.changeRootPath(v.getPath());
				EncFSVolume volume = new EncFSVolumeBuilder()
					.withFileProvider(fileProvider,v.getName())
					.withPbkdf2Provider(mApp.getNativePBKDF2Provider())
					.withPassword(rule.getVolumePassword()).buildVolume();
				v.unlock(volume);
				
			} else {
				sync(ruleID);
			}

		syncIsRunning = false;
	}
	
	public FileSynchronizerRule getRuleFromId(int id){
		for (FileSynchronizerRule rule: rules){
			if (rule.getId()==id) return rule;
		}
		return null;
	}

	

	/**
	 * make sure the volume is already open before launching this method.
	 * else, launch launchRule(String volumeNameToSync, int ruleID ) instead
	 */
	public void sync(int ruleID){
		//System.out.println("******* sync rule "+ruleID);
		refreshNotifyBar("run sync "+ruleID);
		FileSynchronizerRule rule = getRuleById(ruleID);
		if (getVolumeFromName(rule.getVolumeNameToSync()).getVolume()==null) {
			System.out.println("??? volume not opened ??");
			return;
		}
		EncFSVolume volume = getVolumeFromName(rule.getVolumeNameToSync()).getVolume();
		//File f = new File(rule.getLocalPathToSync());
		ImportFileTask mAsyncTask = new ImportFileTask(this.getApplicationContext(),volume, new File(rule.getLocalPathToSync()) ,rule.getVolumePathToSync(), null,new Boolean(rule.getDeleteSrcFileAfterSync()));
		mAsyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		refreshNotifyBar("finished: sync "+ruleID);
//	    if (rules.size()>0){
//	    	SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
//	    	refreshNotifyBar(rules.size()+" sync - Last sync: "+sdf.format(new Date())+" - Frequency: Half hour");
//	    }

	}
	
	
	private static FileSynchronizerRule getRuleById(int id){
		for (FileSynchronizerRule rule: rules){
			if (rule.getId()==id) return rule;
		}
		return null;
	}

	private EDVolume getVolumeFromName(String volumeName){
		EDApplication mApp = (EDApplication) getApplication();
		List<EDVolume>  volumes = mApp.getVolumeList();
		for (EDVolume v: volumes){
			if (v.getName().equals(volumeName)){
				return v;
			}
		}
		return null;
	}
	   
	@Override
	public void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
	}


	

	public void refreshNotifyBar(String txt) {
	    
	    NotificationCompat.Builder  builder = new NotificationCompat.Builder(this)        
        .setContentTitle("EncdroidMC")
        .setContentText(txt)
        .setSmallIcon(R.drawable.ic_launcher);
        
	    //builder.setOngoing(true);
	    
	    if (rules.size()>0){
	    	builder.setContentIntent(PendingIntent.getActivity(this, 0, new Intent(this, ConfigSyncActivity.class), 0));
	    }
        
	    NotificationManager mNotificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
	    mNotificationManager.notify(999, builder.build());
	    
	    
	    
	}


	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}





}

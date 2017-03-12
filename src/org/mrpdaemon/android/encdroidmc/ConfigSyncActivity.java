package org.mrpdaemon.android.encdroidmc;

import java.io.File;
import java.util.List;

import org.mrpdaemon.android.encdroidmc.asyncTasks.ImportFileTask;
import org.mrpdaemon.android.encdroidmc.fileProvider.EncdroidFileProvider;
import org.mrpdaemon.sec.encfs.EncFSInvalidPasswordException;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.text.method.PasswordTransformationMethod;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import fr.starn.FileSynchronizerRule;
import fr.starn.FileSynchronizerService;

public class ConfigSyncActivity extends Activity  implements OnItemSelectedListener,OnTouchListener {
	String[] rules= new String[0];  
	public List<FileSynchronizerRule> syncRules;
	Spinner spin1;
	boolean userSelect = false;
	private int maxID;
	
	EditText volumeName;
	EditText volumePath;
	EditText password;
	CheckBox onlyWifiCheckBox;
	CheckBox removeAfterCheckBox ;
	EditText localFolder ;
	EditText syncID ;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_config_sync);
		

		
		volumeName = (EditText) findViewById(R.id.volumeName);
		//volumeName.setFocusable(false);
		
		volumePath = (EditText) findViewById(R.id.volumePath);
		//volumePath.setFocusable(false);
		
		password = (EditText) findViewById(R.id.volumePassword);
		password.setTransformationMethod(PasswordTransformationMethod.getInstance());
		password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD); 
		
		onlyWifiCheckBox = ((CheckBox) findViewById(R.id.onlyWifi));
		removeAfterCheckBox = ((CheckBox) findViewById(R.id.removeAfter));
		localFolder = ((EditText) findViewById(R.id.localFolder));
		syncID = ((EditText) findViewById(R.id.syncID));
		syncID.setFocusable(false);
		
		
		spin1=(Spinner) findViewById(R.id.selectedRule);
		ArrayAdapter<String> adapter=new ArrayAdapter<String>(ConfigSyncActivity.this, android.R.layout.simple_spinner_item, rules);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spin1.setAdapter(adapter);
		spin1.setOnTouchListener(this);
		spin1.setOnItemSelectedListener((OnItemSelectedListener) this);
		
		refresh(null);

		
	}
	
	private void refresh(String selectedLocalPathToSync){

		
		
		EDApplication mApp = (EDApplication) getApplication();
		syncRules = mApp.getDbHelper().getSyncRules();


		int i = 0;
		int selectedPosition = 0;
		String volumeNameValue = "";
		String volumePathValue = "";
		if (getIntent() != null && getIntent().getExtras() != null ){
			volumeNameValue = getIntent().getExtras().getString("volumeName");
			volumePathValue = getIntent().getExtras().getString("volumePath");
		}
		volumeName.setText(volumeNameValue);
		volumePath.setText(volumePathValue);
		password.setText("");
		onlyWifiCheckBox.setChecked(true);
		removeAfterCheckBox.setChecked(false);
		localFolder.setText("");
		syncID.setText("-1");
//		if ("".equals(volumeNameValue)){
//			rules=new String[syncRules.size()];
//		} else {
//			rules=new String[syncRules.size()+1];
//			rules[0] = "New rule";
//			i++;
//		}
		rules=new String[syncRules.size()+1];
		rules[0] = "New rule";
		i++;
		
		maxID = 0;
		for (FileSynchronizerRule rule: syncRules){
			rules[i]=rule.getLocalPathToSync();
			if ((selectedLocalPathToSync==null && i==0) || (selectedLocalPathToSync != null && selectedLocalPathToSync.equals(rule.getLocalPathToSync()))){
				selectedPosition=i;
				volumeName.setText(rule.getVolumeNameToSync());
				volumePath.setText(rule.getVolumePathToSync());
				password.setText(rule.getVolumePassword());
				onlyWifiCheckBox.setChecked(new Boolean(rule.getSyncOnlyOnWifi()));
				removeAfterCheckBox.setChecked( new Boolean(rule.getDeleteSrcFileAfterSync()));
				localFolder.setText(rule.getLocalPathToSync());
				syncID.setText(""+rule.getId());
				if (rule.getId()>maxID) maxID = rule.getId();
			}
			i++;
		}
		ArrayAdapter<String> adapter=new ArrayAdapter<String>(ConfigSyncActivity.this, android.R.layout.simple_spinner_item, rules);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spin1.setAdapter(adapter);
		adapter.notifyDataSetChanged();
		
		userSelect=false;
		if (spin1.getSelectedItemPosition() != selectedPosition) spin1.setSelection(selectedPosition);
	}

	
	public void localFolderButtonClick(View v) {
	    switch (v.getId()) {
	      case R.id.localFolderButton:
	    	Intent startFileChooser = new Intent(this, EDFileChooserActivity.class);
	  		startFileChooser.putExtra(EDFileChooserActivity.PARAM_KEY_MODE, EDFileChooserActivity.FOLDER_PICKER_MODE);
			startFileChooser.putExtra(EDFileChooserActivity.PARAM_KEY_PROVIDER, EncdroidFileProvider.getLocalFileSystemProvider());
			startActivityForResult(startFileChooser, 0);
	        break;
//	      case R.id.button2:
//	        doSomething2();
//	        break;
	      }
	}
	
	
	public void saveButtonClick(View v) {
		int id = Integer.parseInt((((TextView) findViewById(R.id.syncID)).getText()).toString());
		if (id != -1){
			EDApplication mApp = (EDApplication) getApplication();
			mApp.getDbHelper().removeRule(id);
		}
		String onlyWifi = ((CheckBox) findViewById(R.id.onlyWifi)).isChecked()?"true":"false";
		String removeAfter = ((CheckBox) findViewById(R.id.removeAfter)).isChecked()?"true":"false";
		FileSynchronizerRule rule = new FileSynchronizerRule(maxID+1,((TextView) findViewById(R.id.volumeName)).getText().toString() , ((TextView) findViewById(R.id.volumePassword)).getText().toString(), ((TextView) findViewById(R.id.volumePath)).getText().toString(), removeAfter,  onlyWifi,  ((TextView) findViewById(R.id.localFolder)).getText().toString());
		EDApplication mApp = (EDApplication) getApplication();
		mApp.getDbHelper().addRule(rule);
		refresh(localFolder.getText().toString());
		syncRules = mApp.getDbHelper().getSyncRules();
		FileSynchronizerService.refreshRules(syncRules);
		
		Intent i = new Intent();
		i.setAction("fr.starn.FileSynchronizerService");
		this.startService(i);
	}
	
	public void runButtonClick(View v) {
		int id = Integer.parseInt((((TextView) findViewById(R.id.syncID)).getText()).toString());
		if (id==-1){
			Toast.makeText(getApplicationContext(), "You cannot run a rule that has not been saved", Toast.LENGTH_LONG).show();
			return;
		}
		
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				try {
					int id = Integer.parseInt((((TextView) findViewById(R.id.syncID)).getText()).toString());
					FileSynchronizerService.getInstance().launchRule( id);
				} catch (EncFSInvalidPasswordException e){
					FileSynchronizerService.getInstance().refreshNotifyBar("Invalid password");
				} catch (Throwable t){
					t.printStackTrace();
					FileSynchronizerService.getInstance().refreshNotifyBar("Sync error: "+t.getMessage());
				}
			}
		}).start();

	}
	
	public void deleteButtonClick(View v) {
		int id = Integer.parseInt((((TextView) findViewById(R.id.syncID)).getText()).toString());
		if (id==-1){
			Toast.makeText(getApplicationContext(), "You cannot remove a rule that does not yet exist", Toast.LENGTH_LONG).show();
			return;
		}
		EDApplication mApp = (EDApplication) getApplication();
		mApp.getDbHelper().removeRule(id);
		refresh(null);
	}
	
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		//getMenuInflater().inflate(R.menu.config_sync, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	
	
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		switch (requestCode) {
		case 0:
			TextView localFolder = (TextView) findViewById(R.id.localFolder);
			if (data==null || data.getExtras()==null) return;
			String choosedFolder = data.getExtras().getString(EDFileChooserActivity.PARAM_KEY_PATH);
			EncdroidFileProvider originProvider = (EncdroidFileProvider)data.getSerializableExtra(EDFileChooserActivity.PARAM_KEY_PROVIDER);
			String importPath = new File( originProvider.getAbsolutePath(choosedFolder)).getAbsolutePath();
			localFolder.setText(importPath);
		}
	}


	@Override
	public void onItemSelected(AdapterView<?> parent, View view, int position,
			long id) {
		//Toast.makeText(getApplicationContext(), "You have Chosen :"+rules[position], Toast.LENGTH_LONG).show();
		if (userSelect) { 
			refresh(rules[position]);
		}
		
	}


	@Override
	public void onNothingSelected(AdapterView<?> parent) {
				
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
        userSelect = true;
        return false;
	}
}

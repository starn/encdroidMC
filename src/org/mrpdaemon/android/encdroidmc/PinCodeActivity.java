package org.mrpdaemon.android.encdroidmc;

import org.mrpdaemon.android.encdroidmc.tools.KeyValueBean;

import fr.starn.Password;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Looper;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.text.method.PasswordTransformationMethod;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

public class PinCodeActivity extends Activity implements Runnable{
	EditText password;
	private String context;
	
	private String pin1;
	private String pin2;
	
	public static boolean screenHasBeenOff = true;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_pin_code);
		

		
		password = (EditText) findViewById(R.id.pinCode);
		password.setTransformationMethod(PasswordTransformationMethod.getInstance());
		password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD); 

		password.addTextChangedListener(new TextWatcher() {
			
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				if (password.getText().toString().length()==4) okAction();
				
			}
			
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void afterTextChanged(Editable s) {
				// TODO Auto-generated method stub
				
			}
		});
		
		
		this.context = "";
		if ( this.getIntent() != null &&  this.getIntent().getExtras() != null && this.getIntent().getExtras().getString("context") != null ) this.context = this.getIntent().getExtras().getString("context");
		
		if ("create".equals(this.context)) {
			
			if (hasPinCode(this)){
				this.setTitle("PIN code management");
				password.setVisibility(View.GONE);
				setPadVisibility(false);
			} else {
				this.setTitle("Enter a PIN code");
				findViewById(R.id.removePin).setVisibility(View.GONE);
			}
		} else {
			this.setTitle("PIN code");
			findViewById(R.id.removePin).setVisibility(View.GONE);
		}
	}
	
	private void setPadVisibility(boolean visible){
		int value = visible ? View.VISIBLE:View.GONE;
		findViewById(R.id.pin0).setVisibility(value);
		findViewById(R.id.pin1).setVisibility(value);
		findViewById(R.id.pin2).setVisibility(value);
		findViewById(R.id.pin3).setVisibility(value);
		findViewById(R.id.pin4).setVisibility(value);
		findViewById(R.id.pin5).setVisibility(value);
		findViewById(R.id.pin6).setVisibility(value);
		findViewById(R.id.pin7).setVisibility(value);
		findViewById(R.id.pin8).setVisibility(value);
		findViewById(R.id.pin9).setVisibility(value);
		findViewById(R.id.pinClear).setVisibility(value);
		findViewById(R.id.pinok).setVisibility(value);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		//getMenuInflater().inflate(R.menu.pin_code, menu);
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
	
	public void clic0(View v){
		password.setText(password.getText()+"0");
	}
	
	public void clic1(View v){
		password.setText(password.getText()+"1");
	}
	public void clic2(View v){
		password.setText(password.getText()+"2");
	}
	public void clic3(View v){
		password.setText(password.getText()+"3");
	}
	public void clic4(View v){
		password.setText(password.getText()+"4");
	}
	public void clic5(View v){
		password.setText(password.getText()+"5");
	}
	public void clic6(View v){
		password.setText(password.getText()+"6");
	}
	public void clic7(View v){
		password.setText(password.getText()+"7");
	}
	public void clic8(View v){
		password.setText(password.getText()+"8");
	}
	public void clic9(View v){
		password.setText(password.getText()+"9");
	}
	
	public void removePin(View v){
		EDDBHelper dbHelper = new EDDBHelper(this);
		dbHelper.removeKeyValue("pinHash");
		this.finish();
	}
	
	
	public void clicok(View v){
		okAction();
	}
	
	public void okAction(){
		if ("create".equals(this.context) && (pin1==null || "".equals(pin1))) {
			this.setTitle("New PIN code again");
			Toast.makeText(getApplicationContext(),"New PIN code again", Toast.LENGTH_LONG).show();
			pin1 = password.getText().toString();
			password.setText("");
		}
		else if ("create".equals(this.context) && !"".equals(pin1)) {
			this.setTitle("Enter again your PIN code");
			pin2 = password.getText().toString();
			if (pin1 == null || !pin1.equals(pin2)){
				Toast.makeText(getApplicationContext(),"PIN verification failed. try again", Toast.LENGTH_LONG).show();
				setTitle("Enter a PIN code");
				password.setText("");
				pin1="";
			} else {
				//save
				EDDBHelper dbHelper = new EDDBHelper(this);
				dbHelper.removeKeyValue("pinHash");
				try {
					dbHelper.insertKeyValue("pinHash",Password.getSaltedHash( pin1));
					Toast.makeText(getApplicationContext(),"PIN code saved. Volume passwords encrypted.", Toast.LENGTH_LONG).show();
				} catch (Exception e){
					Toast.makeText(getApplicationContext(),"error: "+e.getMessage(), Toast.LENGTH_SHORT).show();
				}
				this.finish();
			}
		}
		else if ("".equals(this.context)) {
			//check pin in thread
			setPadVisibility(false);
			new Thread(this).start();
		}
		
	}
	
	public void pinOK(){
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
            	setPadVisibility(true);
        		screenHasBeenOff=false;
        		password.setText("");
        		PinCodeActivity.this.finish();
        		return;
            }
        });
	}
	
	public void pinKO(){
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
            	setPadVisibility(true);
        		Toast.makeText(getApplicationContext(),"Bad PIN code", Toast.LENGTH_SHORT).show();
        		password.setText("");
            }
        });
		

	}
	
	public void clicClear(View v){
		password.setText("");
	}
	
	public static boolean needPinCode(Context ctx){
		if (hasPinCode(ctx) && screenHasBeenOff){
			return true;
		}
		return false;
	}
	
	private static boolean hasPinCode(Context ctx){
		EDDBHelper dbHelper = new EDDBHelper(ctx);
		KeyValueBean hash = dbHelper.getKeyValue("pinHash");
		return  (hash != null);
	}

	@Override
	public void run() {
		Looper.prepare();
		EDDBHelper dbHelper = new EDDBHelper(this);
		String dbhash = dbHelper.getKeyValueValue("pinHash");
		boolean ok =false;
		try {
			ok = Password.check(password.getText().toString(), dbhash);
			if (ok) {
				pinOK();
			}
		} catch (Exception e){
			e.printStackTrace();
		}
		if (!ok) pinKO();
	}
	
}

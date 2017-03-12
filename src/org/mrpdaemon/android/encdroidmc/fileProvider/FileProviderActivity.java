/*
 * encdroid - EncFS client application for Android
 * Copyright (C) 2012  Mark R. Pariente
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mrpdaemon.android.encdroidmc.fileProvider;

import java.util.List;

import org.mrpdaemon.android.encdroidmc.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.InputType;
import android.text.method.PasswordTransformationMethod;
import android.widget.EditText;
import android.widget.Toast;

public class FileProviderActivity  extends Activity{
	
	//input params
	private EncdroidFileProvider instance = null;
	private int requestCode;
	private String providerParams;
	
	
	//call contexts
	public static int CONTEXT_ACION_CREATE_NEW_PROVIDER_INSTANCE_FOR_VOLUME_IMPORT = 50;
	public static int CONTEXT_ACION_CREATE_NEW_PROVIDER_INSTANCE_FOR_VOLUME_CREATION = 51;
	public static int CONTEXT_ACION_LOAD_EXISTING_PROVIDER_INSTANCE_FOR_UNLOCK_VOLUME = 52;
	
	
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		int idProvider = this.getIntent().getExtras().getInt("idProvider");
		requestCode = this.getIntent().getIntExtra("requestCode", -1);
		providerParams=this.getIntent().getStringExtra("providerParams");
		
		try {
			instance = EncdroidFileProvider.getProviderById(idProvider);
			if (instance==null) {
				AlertDialog.Builder alertBuilder = new AlertDialog.Builder(this);
				alertBuilder.setMessage("File provider module is not installed (idProvider="+idProvider+")");
				alertBuilder.setCancelable(false);
				alertBuilder.setNeutralButton(getString(R.string.btn_ok_str),
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int whichButton) {
								dialog.dismiss();
								finish();
							}
						});
				try {
					AlertDialog alertDialog = alertDialog = alertBuilder.create();
					alertDialog.show();
				} catch (RuntimeException e){
					e.printStackTrace();
				}
			}
			else { //provider module exist
				if (instance instanceof Linkable){
					Linkable linker = (Linkable)instance;
					//linker.link(this);
					linkFileProvider asyncTask = new linkFileProvider (linker);
					asyncTask.execute();
				} else {//the provider is not linkable
					askParams();
					
				}
			}
			

			

		} catch (Exception e){
			throw new RuntimeException(e);
		}		
	}


	
	protected void onResume() {
		super.onResume();
		
		if ( instance instanceof Linkable){
			if (((Linkable)instance).onResume(this)){
				askParams();
			}
		}
		
	}
	
	public void askParams(){
		if (requestCode!=CONTEXT_ACION_LOAD_EXISTING_PROVIDER_INSTANCE_FOR_UNLOCK_VOLUME){
			showDialog(instance.getParamsToAsk());
		} else {
			instance.setParamValues(EncdroidFileProvider.unserializeParams( providerParams));
			finishFileProviderInit();
		}
	}
	
	
	
	private void finishFileProviderInit(){
		try {
			instance.init("/");
		} catch (Exception e){
			throw new RuntimeException(e);
		}
		 instance.setReady(true);
		 EncdroidFileProvider.lastCreatedProvider=instance;
		 Intent returnIntent = new Intent();
		 returnIntent.putExtra("providerInstance",instance);
		 setResult(RESULT_OK,returnIntent);     
		 finish();	
	}

	
	
	private void showDialog(List<EncdroidProviderParameter> questions){
		
		if (questions==null || questions.size()==0) {
        	//if (activity instanceof Fillable){
        		try {
        			//this.init("/");*
        			finishFileProviderInit();
        		} catch (Exception e){
        			e.printStackTrace();
        			String exceptionLabel = e.getMessage();
        			if (exceptionLabel==null) exceptionLabel=e.toString();
        			Toast.makeText(this.getApplicationContext(),exceptionLabel, Toast.LENGTH_LONG).show();
        			//ask again parameters question to the user
        			if (instance.getParamsToAsk()!=null && instance.getParamsToAsk().size()>0){
        				this.showDialog( instance.getParamsToAsk());
        			}
        			return;
        		}
        	//}			
    		//this.ready=true;
   			//((Fillable)activity).launchFillTask();			
			return;
		}
		
	    AlertDialog.Builder alert = new AlertDialog.Builder(this);                 
	    alert.setTitle(questions.get(0).getTitle());  
	    alert.setMessage(questions.get(0).getLabel());
	     

	     // Set an EditText view to get user input   
	     final EditText input = new EditText(this);
	     if (questions.get(0).isPassword()) {
	    	 input.setTransformationMethod(PasswordTransformationMethod.getInstance());
	    	 input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD); 
	     }
	     alert.setView(input);
         alert.setPositiveButton("Ok", new ClickListener(this,instance,input,questions));  
         alert.show();
	}
	
	
	private class ClickListener implements OnClickListener{
		EncdroidFileProvider instance;
		EditText input;
		Activity ctx;
		List<EncdroidProviderParameter> questions;
		
		public ClickListener(Activity ctx, EncdroidFileProvider instance,EditText input,List<EncdroidProviderParameter> questions){
			this.instance=instance;
			this.input=input;
			this.ctx=ctx;
			this.questions=questions;
		}
		
        public void onClick(DialogInterface dialog, int whichButton) {
            String value = input.getText().toString();
            instance.getParamValues().put(questions.get(0).getKey(), value);
            
            //remove the question just answered from the list of paramters to ask
            if (questions.size()>0){
            	questions.remove(0);
            	
            }

            //showDialog will ask for the next question, or init the fileprovider if all questions are answered
            showDialog(questions);
            

            
            return;                  
           }
	}	
	
	
	
	
	
	
    class linkFileProvider extends AsyncTask<Void, Void, Void> {
    	private Linkable linkable;
    	
    	public linkFileProvider(Linkable linkable){
    		this.linkable=linkable;
    	}
        

        protected void onPostExecute() {
        }

		@Override
		protected Void doInBackground(Void... arg0) {
			if (!linkable.isLinked(FileProviderActivity.this)){
				linkable.link(FileProviderActivity.this);
			} else {
				FileProviderActivity.this.askParams();
			}
			return null;
		}
     }	
	
	
}

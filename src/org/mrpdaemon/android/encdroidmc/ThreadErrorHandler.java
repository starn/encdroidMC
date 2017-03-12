package org.mrpdaemon.android.encdroidmc;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.widget.Toast;

public class ThreadErrorHandler extends Handler{
	Context ctx;
	
	public ThreadErrorHandler(Context ctx){
		this.ctx=ctx;
	}
	
    public void handleMessage(Message msg) {
        String text = (String)msg.obj;
        Toast.makeText(this.ctx,text, Toast.LENGTH_LONG).show();
    }
}

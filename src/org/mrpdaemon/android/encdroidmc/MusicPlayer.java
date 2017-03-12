package org.mrpdaemon.android.encdroidmc;

import java.util.ArrayList;
import java.util.List;

import org.mrpdaemon.sec.encfs.EncFSFile;

import fr.starn.webdavServer.HttpServer;
import android.app.Activity;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

/**
 * not used yet
 *
 */
public class MusicPlayer extends Activity implements OnCompletionListener, Runnable, OnSeekBarChangeListener{
	private MediaPlayer mediaPlayer;
	private int playedIndex;
	private List<EncFSFile> playableFiles;
	private SeekBar seekBar;
	
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.music_player);
		
		new Thread(this).start();
		mediaPlayer = new MediaPlayer();
		mediaPlayer.setOnCompletionListener(this);
		seekBar = (SeekBar) findViewById(R.id.SeekBar01);
		seekBar.setOnSeekBarChangeListener(this);
//		if (mediaPlayer==null){
//		mediaPlayer = new MediaPlayer();
//	} else {
//		mediaPlayer.reset();
//	}
	
		playableFiles = new ArrayList<EncFSFile>();
		
		int i = 0;
		for (EncFSFile file:EDVolumeBrowserActivity.childEncFSFiles){
			if (file.getName().endsWith(".mp3")){
				playableFiles.add(file);
			}
		}
		
		playedIndex=0;
		EncFSFile fileToPlay=playableFiles.get(playedIndex);
		play(fileToPlay);
		Button previous = (Button) findViewById(R.id.ButtonPrevious);
		Button next = (Button) findViewById(R.id.ButtonNext);
		Button playPause = (Button) findViewById(R.id.ButtonPlayStop);
		playPause.setOnClickListener(playPauseListener);
		previous.setOnClickListener(previousListener);
		next.setOnClickListener(nextListener);
	}
	
	
	
	  View.OnClickListener previousListener = new View.OnClickListener() {
		    public void onClick(View v) {
		    	if (playedIndex>0) playedIndex--;
		    	else playedIndex=playableFiles.size()-1;
		    	EncFSFile fileToPlay = playableFiles.get(playedIndex);
		    	play(fileToPlay);
		    }
		  };
		  
	  View.OnClickListener nextListener = new View.OnClickListener() {
	    public void onClick(View v) {
	    	if (playedIndex<playableFiles.size()-1) playedIndex++;
	    	else playedIndex=0;
	    	EncFSFile fileToPlay = playableFiles.get(playedIndex);
	    	play(fileToPlay);
	    }
	  };
	  
	  View.OnClickListener playPauseListener = new View.OnClickListener() {
		    public void onClick(View v) {
		    	Button playPause = (Button) findViewById(R.id.ButtonPlayStop);
		    	if (mediaPlayer.isPlaying()) {
		    		mediaPlayer.pause();
		    		playPause.setText("Play");
		    	}
		    	else {
		    		mediaPlayer.start();
		    		playPause.setText("Pause");
		    	}
		    }
		  };
	
	  protected void onStop() {
		  
		  mediaPlayer.stop();
		  super.onStop();
	  };
	  

	  
	  private void play(EncFSFile fileToPlay){
		  	this.setTitle((playedIndex+1)+". "+playableFiles.get(playedIndex).getName());
		  	new Play(fileToPlay).start();
			Button playPause = (Button) findViewById(R.id.ButtonPlayStop);
			playPause.setText("Pause");
	  }
	  
	  class Play extends Thread{
		  EncFSFile fileToPlay;
		  
		  Play(EncFSFile fileToPlay){
			  this.fileToPlay=fileToPlay;
		  }
		  
		  @Override
		public void run() {
			  mediaPlayer.reset();
			  HttpServer server = HttpServer.getInstance();
				String id = server.setFile(fileToPlay);
				try {

					
					
					mediaPlayer.setDataSource("http://127.0.0.1:8080/"+id);

					Thread.sleep(500);
					mediaPlayer.prepare();
					mediaPlayer.start();
					seekBar.setMax(mediaPlayer.getDuration());
				} catch (Exception e){
					e.printStackTrace();
				}	
		}
	  }



	@Override
	public void onCompletion(MediaPlayer arg0) {
    	if (playedIndex<playableFiles.size()-1) playedIndex++;
    	else playedIndex=0;
    	EncFSFile fileToPlay = playableFiles.get(playedIndex);
    	play(fileToPlay);
	}
	
	@Override
	public void run() {
        //int currentPosition= 0;
        
        while (true) {
            try {
                Thread.sleep(2000);
                int currentPosition= mediaPlayer.getCurrentPosition();
                seekBar.setProgress(currentPosition);
                
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (Exception e) {
            	e.printStackTrace();
            }            
            
        }
	}



	@Override
	public void onProgressChanged(SeekBar seekBar, int progress,   boolean fromUser) {
		//we cannot seek to a position with an encrypted stream...
		//this method will work only if the stream has already been loaded until this position
		//mediaPlayer.seekTo(progress);
	}



	@Override
	public void onStartTrackingTouch(SeekBar arg0) {
		// TODO Auto-generated method stub
		
	}



	@Override
	public void onStopTrackingTouch(SeekBar arg0) {
		// TODO Auto-generated method stub
		
	}
}

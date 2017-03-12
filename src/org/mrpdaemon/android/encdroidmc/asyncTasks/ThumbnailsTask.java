package org.mrpdaemon.android.encdroidmc.asyncTasks;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.util.HashMap;

import org.mrpdaemon.android.encdroidmc.EDFileChooserItem;
import org.mrpdaemon.android.encdroidmc.EDVolumeBrowserActivity;
import org.mrpdaemon.android.encdroidmc.MimeManagement;
import org.mrpdaemon.sec.encfs.EncFSFile;
import org.mrpdaemon.sec.encfs.EncFSVolume;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.media.ThumbnailUtils;
import android.os.AsyncTask;
import android.os.Environment;
import android.provider.MediaStore.Images;
import android.provider.MediaStore.Video.Thumbnails;
import android.widget.Toast;
import fr.starn.ThumbnailManager;
import fr.starn.webdavServer.HttpServer;

public class ThumbnailsTask extends AsyncTask<Void, Void, Void> {
	private EDFileChooserItem fileToRefresh;
	private EDVolumeBrowserActivity volumeBrowserActivity;
	private boolean displayExistingThumbnailOnly;//do not generate new thumbnail if not already existing
	NotificationHelper n;
	String thumbnailAbsolutePath = null;
	
	private boolean generateVideoThumbnails = false;
	private static final boolean CACHE_THUMBNAILS_IN_LOCAL = false;
	
	public ThumbnailsTask(EDVolumeBrowserActivity volumeBrowserActivity) {
		this(volumeBrowserActivity,null,false,false);
	}

	
	public ThumbnailsTask(EDVolumeBrowserActivity volumeBrowserActivity, EDFileChooserItem fileToRefresh,boolean displayExistingThumbnailOnly, boolean generateVideoThumbnails) {
		super();
		this.fileToRefresh=fileToRefresh;
		this.volumeBrowserActivity=volumeBrowserActivity;
		this.displayExistingThumbnailOnly=displayExistingThumbnailOnly;
		this.generateVideoThumbnails=generateVideoThumbnails;
	}

	
	@Override
	protected void onPreExecute() {
		try {//sometime it crash if the user refresh while the list is already being refreshed
			super.onPreExecute();
			
		} catch (Exception e){
			e.printStackTrace();
		}
	}

	
	@Override
	protected Void doInBackground(Void... arg0) {
		ThumbnailManager tm = null;
		
		String thumbnailFilename = volumeBrowserActivity.mCurEncFSDir.getName().replaceAll("/","")+".thumb";
		thumbnailAbsolutePath = EncFSVolume.combinePath(volumeBrowserActivity.mCurEncFSDir, thumbnailFilename);
		EncFSFile thumbnailEncFSFile= null;
		
		//search if thumbnails file exist, and load it
		for (EncFSFile file : volumeBrowserActivity.childEncFSFiles) {
			if (thumbnailFilename.equals(file.getName()) ){
				thumbnailEncFSFile=file;
				try {
					System.out.println("******** found thumbnails file");
					tm = new ThumbnailManager(CACHE_THUMBNAILS_IN_LOCAL?file.openSDCardCachedInputStream():file.openInputStream(0));
					updateallFileIcons(tm);
					break;
				} catch (Exception e){
					e.printStackTrace();
				}
			}
		}
		if (displayExistingThumbnailOnly) return null;
		if (tm==null) tm = new ThumbnailManager(new byte[0]);
		
		//estimate number of thumb to generate for status bar info
		int nbFileToGenerate = 0 ;
		for (EncFSFile file : EDVolumeBrowserActivity.childEncFSFiles) {
			if (fileToRefresh!=null && !fileToRefresh.getName().equals(file.getName())) continue;
			if (!tm.contains(file.getName())){
				if (    (MimeManagement.isVideo(file.getName()) &&   (fileToRefresh!=null || generateVideoThumbnails))    || MimeManagement.isImage(file.getName())){
					nbFileToGenerate++;
				}
			}
		}
		
		if (nbFileToGenerate>0){
			n = new NotificationHelper(volumeBrowserActivity);
			n.setMax(nbFileToGenerate);
			n.setTitle("Generate thumbnails");
		}
		
		//create thumbnails if it does not already exist on the tm file and (the user ask to generate this tm or we are on local provider)
		boolean hasChanged = false;
		for (EncFSFile file : EDVolumeBrowserActivity.childEncFSFiles) {
			if (fileToRefresh!=null && !fileToRefresh.getName().equals(file.getName())) continue;
			if (this.isCancelled()) break;
			
			if (!tm.contains(file.getName())){
				if (MimeManagement.isVideo(file.getName()) && (fileToRefresh!=null || generateVideoThumbnails) ) {//disable video thumbnail in automatic mode
					HttpServer server = HttpServer.getInstance();
					String id = server.setFile(file);
					Bitmap bmp= createVideoThumbnail("http://127.0.0.1:8080/"+id, Thumbnails.MICRO_KIND);
					if (bmp != null){
						ByteArrayOutputStream stream = new ByteArrayOutputStream();
						bmp.compress(Bitmap.CompressFormat.PNG, 100, stream);
						byte[] byteArray = stream.toByteArray();
						if (byteArray!=null && byteArray.length>0) {
							tm.addFile(file.getName(), byteArray);
							System.out.println("add new thumbnail "+file.getName());
							updateFileIcon(tm,file);
							hasChanged = true;
						}
					}
					if (nbFileToGenerate>0) n.incrementProgressBy(1);
				}//end of video mgt
				if (MimeManagement.isImage(file.getName())) {
					try {
						InputStream is = file.openInputStream(0);
						byte[] img = ThumbnailManager.inputStreamToByteArray(is);
						is.close();
						Bitmap bmp = createImageThumbnail(img);
						if (bmp != null){
							ByteArrayOutputStream stream = new ByteArrayOutputStream();
							bmp.compress(Bitmap.CompressFormat.PNG, 50, stream);
							byte[] byteArray = stream.toByteArray();
							if (byteArray!=null && byteArray.length>0) {
								tm.addFile(file.getName(), byteArray);
								System.out.println("add new thumbnail "+file.getName());
								updateFileIcon(tm,file);
								hasChanged = true;
							}
						}
					} catch (Exception e){
						e.printStackTrace();
					} finally{
						if (nbFileToGenerate>0) n.incrementProgressBy(1);
					}
				}//end of image mgt
				
			}// end of "if (!tm.contains(file.getName())){"
			
			if (isCancelled()) {
				save(thumbnailEncFSFile, thumbnailFilename,tm);
				if (nbFileToGenerate>0) n.dismiss();
				return null;
				
			}
		}//end of for loop

		//thumbnail generator update the view as soon as a bitmap is generated
		//so we need to update thee view only if thumbnails were stored on the file and not generated
		if (nbFileToGenerate==0){
			//update the view
			updateallFileIcons(tm);
		}
		
		//save the new thumbnail file
		if (hasChanged && !tm.isEmpty()){		
			save(thumbnailEncFSFile, thumbnailFilename,tm);
		}
		if (nbFileToGenerate>0) n.dismiss();
		return null;
	}
	
	private void save(EncFSFile thumbnailEncFSFile, String thumbnailFilename,ThumbnailManager tm){

			try {
				
				if (thumbnailEncFSFile==null){
					if (volumeBrowserActivity.getmVolume().pathExists(thumbnailAbsolutePath)) volumeBrowserActivity.getmVolume().deletePath(thumbnailAbsolutePath, false);
					thumbnailEncFSFile = volumeBrowserActivity.getmVolume().createFile(thumbnailAbsolutePath);
					
				} 
				tm.save(CACHE_THUMBNAILS_IN_LOCAL?thumbnailEncFSFile.openCachedOutputStream(0):thumbnailEncFSFile.openOutputStream(0));
			} catch (Exception e){
				e.printStackTrace();
			}
	}
	public EDFileChooserItem getGuiItemFromEncFSFile(EncFSFile f){
		int i=0;
		do {
			for (EDFileChooserItem item: volumeBrowserActivity.mCurFileList){
				if (item.getName().equals(f.getName())) return item;
			}
			try { Thread.sleep(1000);} catch (Exception e){e.printStackTrace();}
			i++;
		} while(i<5);
		return null;
	}
	
	private void updateallFileIcons(ThumbnailManager tm){
		if (!tm.isEmpty()){
			for (EncFSFile file : EDVolumeBrowserActivity.childEncFSFiles) {
				if (tm.contains(file.getName())){
					updateFileIcon(tm,file);
				}
			}
		}
	}
	private void updateFileIcon(ThumbnailManager tm, EncFSFile file){
		EDFileChooserItem item = getGuiItemFromEncFSFile(file);
		if (item==null) return;
		byte[] bitmapContent = tm.getFileContent(file.getName());
		Bitmap fileBitmap = BitmapFactory.decodeByteArray(bitmapContent,0,bitmapContent.length);
		item.setIcon(fileBitmap);
		volumeBrowserActivity.refreshListViewGUI();

	}
	
	@Override
	protected void onPostExecute(Void result) {
		try {//sometime it crash if the user refresh while the list is already being refreshed

		} catch (Exception e){
			e.printStackTrace();
		}
	}
	
	
	public static Bitmap createImageThumbnail(byte[] img){
		   try 
		    {
			    final int THUMBNAIL_SIZE = 100;
			    //InputStream is=getAssets().open("apple-android-battle.jpg");
			    Bitmap imageBitmap = BitmapFactory.decodeByteArray(img, 0, img.length);//Stream(is);
	
			    Float width = new Float(imageBitmap.getWidth());
			    Float height = new Float(imageBitmap.getHeight());
			    Float ratio = width/height;
			    imageBitmap = Bitmap.createScaledBitmap(imageBitmap, (THUMBNAIL_SIZE ), THUMBNAIL_SIZE, true);
	

			    return imageBitmap;
		    }
		    catch(Exception ex) {
		    	return null;
		    }
	}
	public void cancel(){
		this.cancel(false);
	}
	
 public synchronized static Bitmap createVideoThumbnail(String uri, int kind) {
     Bitmap bitmap = null;
     MediaMetadataRetriever retriever = new MediaMetadataRetriever();
     try {
         retriever.setDataSource(uri,new HashMap());
         bitmap = retriever.getFrameAtTime(-1,MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
     } catch (IllegalArgumentException ex) {
     	//Toast.makeText(volumeBrowserActivity.getApplicationContext(), "Cannot generate thumbnail: "+ex.getMessage(), Toast.LENGTH_SHORT).show();
         // Assume this is a corrupt video file
     	return null;
     } catch (RuntimeException ex) {
     	//Toast.makeText(volumeBrowserActivity.getApplicationContext(), "Cannot generate thumbnail: "+ex.getMessage(), Toast.LENGTH_SHORT).show();
     	return null;
         // Assume this is a corrupt video file.
     } finally {
         try {
             retriever.release();
         } catch (RuntimeException ex) {
             // Ignore failures while cleaning up.
         }
     }

     if (bitmap == null) {
     	return null;
     }

     if (kind == Images.Thumbnails.MINI_KIND) {
         // Scale down the bitmap if it's too large.
         int width = bitmap.getWidth();
         int height = bitmap.getHeight();
         int max = Math.max(width, height);
         if (max > 512) {
             float scale = 512f / max;
             int w = Math.round(scale * width);
             int h = Math.round(scale * height);
             bitmap = Bitmap.createScaledBitmap(bitmap, w, h, true);
         }
     } else if (kind == Images.Thumbnails.MICRO_KIND) {
         int OPTIONS_RECYCLE_INPUT = 0x2;
         int TARGET_SIZE_MINI_THUMBNAIL = 320;
         int TARGET_SIZE_MICRO_THUMBNAIL = 96;
         bitmap = ThumbnailUtils.extractThumbnail(bitmap,
                 TARGET_SIZE_MICRO_THUMBNAIL,
                 TARGET_SIZE_MICRO_THUMBNAIL,
                 OPTIONS_RECYCLE_INPUT);
     }
     return bitmap;



 }
}
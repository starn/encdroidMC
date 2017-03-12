package fr.starn;



import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ThumbnailManager {
    private byte[] dbFileContent;
   
    private static int filenameLengthInByte = 100;
    private static int fileSizeLengthInByte = 4;
    //private static int fileContentLengthInByte=3500;
    //private static int blockSize = filenameLengthInByte+fileSizeLengthInByte+fileContentLengthInByte;
    private Map<String, Integer> filenameToStartingIndexMap;
   
   
//    public static void main(String[] args) throws Exception{
//        File dbFile = new File("Y:/2d/thumbnail/encdroid.db");
//        if (!dbFile.exists()) dbFile.createNewFile();
//        FileInputStream fis = new FileInputStream(dbFile);
//        ThumbnailManager db = new ThumbnailManager(inputStreamToByteArray(fis));
//       
//        File f = new File ("Y:/2d/thumbnail/");
//        File[] images = f.listFiles();
//        for (File img: images){
//            if (img.getName().endsWith("db") || !img.isFile() || img.getName().endsWith("txt")) continue;
//            db.addFile(img.getName(), inputStreamToByteArray( new FileInputStream(img)) );
//        }
//        db.save(new FileOutputStream(dbFile));
//       
//       
//       
//        byte[] c = db.getFileContent("n4.flv.png");
//        OutputStream ps = new FileOutputStream(new File("C:/dev/testImg/test/n4.flv.png"));
//        ps.write(c);
//        ps.close();
//        c = db.getFileContent("aaa.txt");
//        ps = new FileOutputStream(new File("C:/dev/testImg/test/aaa.txt"));
//        ps.write(c);
//        ps.close();
//
//       
//    }
   
   
    public ThumbnailManager(InputStream is){
    	this(inputStreamToByteArray(is));
    }

   
   
    public ThumbnailManager(byte[] byteArray){
        dbFileContent= byteArray;
        if (filenameToStartingIndexMap==null) filenameToStartingIndexMap=new HashMap<String,Integer>();
        
        for (int i=0;i<dbFileContent.length;i++){
            int filenamePos = i;
            String filename = new String(Arrays.copyOfRange(dbFileContent,filenamePos,filenamePos+filenameLengthInByte)).trim();

            int sizePos = i+filenameLengthInByte;
            int fileSize = byteArrayToInt(Arrays.copyOfRange(dbFileContent,sizePos,sizePos+fileSizeLengthInByte));

//            System.out.println(filename+","+fileSize);
            
            filenameToStartingIndexMap.put(filename, i);
            i+=filenameLengthInByte+fileSizeLengthInByte+fileSize-1;
        	//System.out.println("load existing thumbnail "+filename);
        }
    }
    
    public boolean isEmpty(){
    	return dbFileContent==null || dbFileContent.length==0;
    }
   

    public Set<String> getFilenameList(){
    	if (filenameToStartingIndexMap==null) return new HashSet<String>();
    	return filenameToStartingIndexMap.keySet();
    	
//        if (dbFileContent==null) return new ArrayList<String>();
//        List<String> resultList = new ArrayList<String>();
//        for (int i = 0 ; i< dbFileContent.length;i+=blockSize){
//            byte [] filename = Arrays.copyOfRange(dbFileContent, i, i+filenameLengthInByte);
//            //System.out.println(new String(filename).trim());
//            resultList.add(new String(filename).trim());
//        }
//        return resultList;
    	
    }
   
    private int getFilenamePos(String filename){
    	if (filenameToStartingIndexMap==null || !filenameToStartingIndexMap.containsKey(filename)) return -1;
        return filenameToStartingIndexMap.get(filename);
    }
    
    public boolean contains(String filename){
    	return getFilenamePos( filename) != -1;
    }
   
    public boolean addFile(String filename, byte[] fileToAdd){
    	//System.out.println("add file:"+filename+" size:"+fileToAdd.length);
        try {
//            int startIndexToWrite = -1;//default: new file => write from the end
            if (getFilenameList().contains(filename)) {
                //System.out.println("replace existing file");
                //startIndexToWrite = getFilenamePos(filename);
            	//System.out.println("file already exist");
            	return false;
            }
            //System.out.println("add file "+filename);

           
            //copy 40 bytes of filename in filenameBytesInResult
            //write filename (40 first bytes)
            byte[] filenameBytesInResult = copyByteArray(filename.getBytes(),filenameLengthInByte);
               
            //copy file size block
            //System.out.println("file size:"+fileToAdd.length);
            byte[] fileSizeBytes=copyByteArray(intToByteArray( fileToAdd.length),fileSizeLengthInByte);
               
               
               
//            //System.out.println("write in final byte[]:"+(startIndexToWrite)+" to "+(4000+startIndexToWrite));
//            for (int i = 0; i<filecontentInByte;i++){
//                //System.out.println(""+(i+startIndexToWrite)+" => "+i);
//                dbFileContent[i+startIndexToWrite]=toAppend[i];
//            }
            
              filenameToStartingIndexMap.put(filename, dbFileContent.length);
              dbFileContent=concatByteArrays(dbFileContent,filenameBytesInResult,fileSizeBytes,fileToAdd);
              
//            else {//replace in the file
//                byte[] newBlock = concatByteArrays(filenameBytesInResult,fileSizeBytes,fileToAdd);
//                for (int i = 0 ; i < newBlock.length; i++ ){
//                    dbFileContent[startIndexToWrite+i]=newBlock[i];
//                }
//            }
           
            return true;
        } catch (Exception e){
            e.printStackTrace();
            return false;
        }
    }
   
    public boolean save(OutputStream os){
    	if (isEmpty()) return false;
        System.out.println("save thumbnails");
        try {
            os.write(dbFileContent);
            os.close();
            return true;
        } catch (IOException e){
        	try {
        		os.close();
        	} catch (Exception e2){
        		e2.printStackTrace();
        	}
            e.printStackTrace();
            return false;
        }
    }
   
    public byte[] getFileContent(String filename){
        int filenamePos = getFilenamePos(filename);
        if (filenamePos==-1) {
            System.out.println("file "+filename+" not found");
            return new byte[0];
        }
       
        int sizePos = filenamePos+filenameLengthInByte;
        int fileSize = byteArrayToInt(Arrays.copyOfRange(dbFileContent,sizePos,sizePos+fileSizeLengthInByte));
       
        int contentPos = filenamePos+filenameLengthInByte+fileSizeLengthInByte;// first bytes are the filename and file size
       
        byte [] result =  Arrays.copyOfRange(dbFileContent, contentPos,contentPos+fileSize);
        return result;
    }
   

   
    public void removeFile(){
       
    }
   
    public static  byte[] inputStreamToByteArray(InputStream is){
    	try {
	        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
	        int nRead;
	        byte[] data = new byte[16384];
	        while ((nRead = is.read(data, 0, data.length)) != -1) {
	          buffer.write(data, 0, nRead);
	        }
	        buffer.flush();
	        return buffer.toByteArray();
    	} catch (Exception e){
    		e.printStackTrace();
    		return new byte [0];
    	}
    }
   
    public static  byte[] intToByteArray(int myInteger){
        return ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(myInteger).array();
    }

    public static int byteArrayToInt(byte [] byteBarray){
        return ByteBuffer.wrap(byteBarray).order(ByteOrder.BIG_ENDIAN).getInt();
    }
   
    private byte[] copyByteArray(byte[] ba, int finalArraySize){
//    	if (finalArraySize<ba.length){
//    		System.err.print("data will be trunkated: ");
//    		for (int i = 0;i<50 && i<ba.length;i++){
//    			System.out.print((char)ba[i]);
//    		}
//    		//System.out.println("\n");
//    	}
        byte[] result = new byte[finalArraySize];
        for (int i = 0 ; i< ba.length && i<finalArraySize;i++){
            result[i]=ba[i];
        }
        return result;
    }
   
    public static byte[] concatByteArrays(byte[]... ba){
        int resultSize = 0;
        for (int i=0;i<ba.length;i++){
            resultSize+=ba[i].length;
        }
        byte[] result = new byte[resultSize];
        int index = 0;
        for (int i=0;i<ba.length;i++){
            for (int j =0; j<ba[i].length;j++){
                result[index]=ba[i][j];
                index++;
            }
        }
        return result;
       
    }

}

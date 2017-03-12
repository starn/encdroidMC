package fr.starn;



import java.util.HashMap;
import java.util.Map;

import org.mrpdaemon.sec.encfs.EncFSFile;

public class PersistantInstanceManager {
	private static Map<String, EncFSFile> encfsFiles;
	
	public static EncFSFile getEncfsFile(String filename){
		if (encfsFiles==null) encfsFiles = new HashMap<String, EncFSFile>();
		return encfsFiles.get(filename);
	}
	
	public static void addEncfsFile(EncFSFile file){
		if (encfsFiles==null) encfsFiles = new HashMap<String, EncFSFile>();
		encfsFiles.put(file.getName(), file);
	}
}

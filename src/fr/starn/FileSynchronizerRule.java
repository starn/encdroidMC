package fr.starn;

/**
 * @author starn
 *
 */
public class FileSynchronizerRule {
	private int id = 7;
	private String volumeNameToSync = "test";
	private String volumePassword = "m06m0o88";
	private String volumePathToSync = "/test";
	private String deleteSrcFileAfterSync = "true";
	private String syncOnlyOnWifi = "true";
	private String localPathToSync = "/sdcard/Pictures";

	
	
	
	
	public FileSynchronizerRule(int id, String volumeNameToSync,
			String volumePassword, String volumePathToSync,
			String deleteSrcFileAfterSync, String syncOnlyOnWifi,
			String localPathToSync) {
		super();
		this.id = id;
		this.volumeNameToSync = volumeNameToSync;
		this.volumePassword = volumePassword;
		this.volumePathToSync = volumePathToSync;
		this.deleteSrcFileAfterSync = deleteSrcFileAfterSync;
		this.syncOnlyOnWifi = syncOnlyOnWifi;
		this.localPathToSync = localPathToSync;
	}
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public String getVolumeNameToSync() {
		return volumeNameToSync;
	}
	public void setVolumeNameToSync(String volumeNameToSync) {
		this.volumeNameToSync = volumeNameToSync;
	}
	public String getLocalPathToSync() {
		return localPathToSync;
	}
	public void setLocalPathToSync(String localPathToSync) {
		this.localPathToSync = localPathToSync;
	}
	public String getVolumePathToSync() {
		return volumePathToSync;
	}
	public void setVolumePathToSync(String volumePathToSync) {
		this.volumePathToSync = volumePathToSync;
	}
	public String getVolumePassword() {
		return volumePassword;
	}
	public void setVolumePassword(String volumePassword) {
		this.volumePassword = volumePassword;
	}
	public String getDeleteSrcFileAfterSync() {
		return deleteSrcFileAfterSync;
	}
	public void setDeleteSrcFileAfterSync(String deleteSrcFileAfterSync) {
		this.deleteSrcFileAfterSync = deleteSrcFileAfterSync;
	}
	public String getSyncOnlyOnWifi() {
		return syncOnlyOnWifi;
	}
	public boolean getSyncOnlyOnWifiBool(){
		return new Boolean(syncOnlyOnWifi);
	}
	public void setSyncOnlyOnWifi(String syncOnlyOnWifi) {
		this.syncOnlyOnWifi = syncOnlyOnWifi;
	}


	
	
}

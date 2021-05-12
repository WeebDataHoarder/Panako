package be.panako.strategy.olaf;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import be.panako.util.Config;
import be.panako.util.FileUtils;
import be.panako.util.Key;

public class OlafFileStorage implements OlafStorage {
	
	/**
	 * The single instance of the storage.
	 */
	private static OlafFileStorage instance;

	/**
	 * A mutex for synchronization purposes
	 */
	private static Object mutex = new Object();

	/**
	 * @return Returns or creates a storage instance. This should be a thread
	 *         safe operation.
	 */
	public synchronized static OlafFileStorage getInstance() {
		if (instance == null) {
			synchronized (mutex) {
				if (instance == null) {
					instance = new OlafFileStorage();
				}
			}
		}
		return instance;
	}
	
	
	final List<long[]> storeQueue;
	final File storeDir;

	
	public OlafFileStorage() {
		String folder = Config.get(Key.OLAF_CACHE_FOLDER);
		if(!new File(folder).exists()) {
			FileUtils.mkdirs(folder);
		}
		if(!new File(folder).exists()) {
			throw new RuntimeException("Could not create CACHE folder: " + folder);
		}
		
		storeDir = new File(folder);
		
		storeQueue = new ArrayList<>();
	}
	
	
	
	public void storeMetadata(long resourceID,String resourcePath,float duration, int fingerprints) {
		String path = FileUtils.combine(storeDir.getAbsolutePath(),resourceID + "_meta_data.txt");		
		StringBuilder sb = new StringBuilder();		
		sb.append(resourceID).append("\n").append(duration).append("\n").append(fingerprints).append("\n").append(resourcePath).append("\n");	
		FileUtils.writeFile(sb.toString(), path);		
	}
	
	@Override
	public OlafResourceMetadata getMetadata(long identifier) {
		String path = FileUtils.combine(storeDir.getAbsolutePath(),identifier + "_meta_data.txt");
		if(!FileUtils.exists(path)) {
			return null;
		}
		String[] metaDataItems = FileUtils.readFile(path).split("\n");
		
		OlafResourceMetadata metaData = new OlafResourceMetadata();
		metaData.duration= Float.valueOf(metaDataItems[1]);
		metaData.numFingerprints= Integer.valueOf(metaDataItems[2]);
		metaData.path= metaDataItems[3];
		
		return metaData;
	}
	
	
	public void addToStoreQueue(long fingerprintHash, int resourceIdentifier, int t1) {
		long[] data = {fingerprintHash,resourceIdentifier,t1};
		storeQueue.add(data);
	}
	
	public String storeQueueToString() {		
		//sort by hash asc
		//storeQueue.sort((a, b) -> Long.valueOf(a[0]).compareTo(b[0]));
		StringBuilder sb = new StringBuilder();
		for(long[] data : storeQueue) {
			sb.append(data[0]).append(" ").append(data[1]).append(" ").append(data[2]).append("\n");
		}
		
		// Clears the store queue
		storeQueue.clear();
		
		return sb.toString();
	}
	
	public void processStoreQueue() {
		if(storeQueue.isEmpty()) return;
		
		int resourceIdentifier = (int) storeQueue.get(0)[1];
		
		// Clears the store queue
		String fingerprintsAsString = storeQueueToString();
		String path = FileUtils.combine(storeDir.getAbsolutePath(),resourceIdentifier + ".tdb");
		FileUtils.writeFile(fingerprintsAsString, path);
	}
	
	public long[] dataFromLine(String line) {
		String[] data = line.split(" ");
		long[] dataArray = {Long.valueOf(data[0]),Long.valueOf(data[1]),Long.valueOf(data[2])};
		return dataArray;
	}
}

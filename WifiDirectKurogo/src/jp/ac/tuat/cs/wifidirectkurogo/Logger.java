package jp.ac.tuat.cs.wifidirectkurogo;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

import jp.ac.tuat.cs.wifidirectkurogo.message.LogContent;
import android.util.Log;

public class Logger {
	public final int CAPACITY = 100;
	private Queue<Serializable> logs;

	public Logger() {
		logs = new ArrayBlockingQueue<Serializable>(CAPACITY);
	}
	
	public void d(String TAG, String message){
		Log.d(TAG, message);
		if(logs.size() >= CAPACITY){
			logs.remove();
		}
		
		logs.add(new LogContent(TAG, message));
	}
	
	public List<Serializable> popLogs(){
		List<Serializable> ret = new LinkedList<Serializable>(logs);
		logs.clear();
		Log.d("TEST", "Logger ret.size()="+ret.size());
		return ret;
	}
}

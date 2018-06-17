package com.cvt.io;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cvt.io.model.Hash;
import com.cvt.io.storage.PersistenceProvider;
import com.cvt.io.storage.rocksDB.RocksDBPersistenceProvider;

public class Account  {
	private static final Logger log = LoggerFactory.getLogger(Account.class);
	
	public final ReadWriteLock rwlock = new ReentrantReadWriteLock();
	public   Map<Hash, Long> state=new HashMap<Hash, Long>();
	private RocksDBPersistenceProvider rdb;
	
	
	public Account(){
		
	}
	public  void  initAccount(RocksDBPersistenceProvider rdb,String initailPath) throws Exception {
		this.rdb=rdb;
		Map<Hash, Long> startState=initInitialState(initailPath);
		
		state=startState;
		for (Map.Entry<Hash, Long> entry : startState.entrySet()) { 
			log.info("Key = " + entry.getKey() + ", Value = " + entry.getValue()); 
			String value=rdb.getValue(entry.getKey().toString());
			if(value==null||value.equals("")) {
				rdb.putValue(entry.getKey().toString(),entry.getValue().toString());
			}
			
		}
		
		
		
	}
	
	
    public Long getBalance(String hash) {
        Long l=0l;
        //rwlock.readLock().lock();
        try {
        	String value=rdb.getValue(hash.toString());
        	l=Long.valueOf(value);
        }catch(Exception e){
        	log.error(e.getMessage());
        	return 0l;
        }
        
        //rwlock.readLock().unlock();
        return l;
    }
	
    
    public int transfer(String sendAddress,String toAddress,long value) {
        
        rwlock.writeLock().lock();
        
        Long sendAccountValue=getBalance(sendAddress);
        Long toAccountValue=getBalance(toAddress);
        if(sendAccountValue<value) {
        	log.error("err transfer");
        	return -1;
        }
        
        try {
        	rdb.putValue(sendAddress.toString(),new Long(sendAccountValue-value).toString());
        	rdb.putValue(toAddress.toString(),new Long(toAccountValue+value).toString());
        }catch(Exception e){
        	log.error(e.getMessage());
        	return -2;
        }
        rwlock.writeLock().unlock();
        return 0;
    }
	
    private  InputStream getAccountStream(String snapshotPath) throws FileNotFoundException {
        InputStream inputStream = Account.class.getResourceAsStream(snapshotPath);
        //if resource doesn't exist, read from file system
        if (inputStream == null) {
            inputStream = new FileInputStream(snapshotPath);
        }

        return inputStream;
    }

	
	 private  Map<Hash, Long> initInitialState(String snapshotFile) {
	        String line;
	        Map<Hash, Long> state = new HashMap<>();
	        BufferedReader reader = null;
	        try {
	            InputStream snapshotStream = getAccountStream(snapshotFile);
	            BufferedInputStream bufferedInputStream = new BufferedInputStream(snapshotStream);
	            reader = new BufferedReader(new InputStreamReader(bufferedInputStream));
	            while ((line = reader.readLine()) != null) {
	                String[] parts = line.split(";", 2);
	                if (parts.length >= 2) {
	                    String key = parts[0];
	                    String value = parts[1];
	                    state.put(new Hash(key), Long.valueOf(value));
	                }
	            }
	        } catch (IOException e) {
	            //syso is left until logback is fixed
	            System.out.println("Failed to load snapshot.");
	            log.error("Failed to load snapshot.", e);
	            System.exit(-1);
	        }
	        finally {
	            IOUtils.closeQuietly(reader);
	        }
	        return state;
	    }
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
         String fuck="";
         Long i=0l;
         try {
        	 i=Long.valueOf(fuck);
         }catch(Exception e) {
        	 i=0l; 
         }
         System.out.println(i);
	}

}

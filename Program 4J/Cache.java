/* Author: Ruiqi Huang
Date: 2-26-2024
Class: CSS 430 
Assignment: Program 4J
----------------------------------------------------------------------------------------------
Description: This is the cache for our ThreadOS. This cache implements the enahcned second chance algorithm.
This means when the cache is full, it picks the victim from the cache by looking at if it is dirty and its 
reference count. The cache have vectors that will represent the pages, we keep track of the vicitm using our
victim variable, and keep track of the block size of each page using blockSize. Each entry or data has a bit
to represent if is it dirty or referenced. The main methods in our cache is read, write, sync, and flush. The
helper methods we have is nextVictim and writeback.
All the methods are described below. 
*/


import java.util.*;

public class Cache {
    private int blockSize;
    private Vector<byte[]> pages; // you may use: private byte[][] = null;
    private int victim;

    private class Entry {
		public static final int INVALID = -1;
		public boolean reference;
		public boolean dirty;
		public int frame;
		public Entry( ) {
	 		reference = false;
	    	dirty = false;
	    	frame = INVALID;
		}
    }

    private Entry[] pageTable = null;

	/*nextVctim()
	---------------------------------------------------------------------------------------------------
	Description: Helper function that helps find the next victim using enhanced second chance algorithm. This
	means that both the modify bit (dirty) and the reference bit (reference) is considered. 
	
	Pre-condition: there is victims to choose from. 
	Post-condition: returns the next victim that is chosen.
	*/
	//To be implemented
    private int nextVictim( ) {
		// implement by yourself
		//Did we find the victim?
		boolean findVictim = false;
	
		//Remember the victim we started at
		int firstVictim;
		if(victim == (pageTable.length - 1)){
			firstVictim = 0;
		} else {
			firstVictim = victim + 1;
		}

		//keeps track of number of cycles
		int cycle = 0;

		while(findVictim == false){
			//update the victim
			if(victim == (pageTable.length - 1)){
				victim = 0;
			} else {
				victim = victim + 1;
			}

			//update the cycle
			if(victim == firstVictim){
				cycle = cycle + 1;
			}
			//setting the value of the next possible victim
			//first check reference then modify bit
			if(pageTable[victim].reference == false){
				if(pageTable[victim].dirty == false){
					findVictim = true;
					//if we went through two cycles and the best victim is still ref = 0 and ref = 1 then the first pick is the victim
				} else if((pageTable[victim].dirty == true) && (cycle == 2)){
					findVictim = true;
				}
			} else {
				//if the reference bit is 1 then we set it to 0 then move on
				pageTable[victim].reference = false;
			}

		}
		return victim;
    }

	/*writeBack(int victimEntry)
	---------------------------------------------------------------
	Description: This methods helps check if a victim is dirty. If it is then it writes its content back to 
	the disk and write new data into this cache block. This helper function can be used in both read and 
	write function.

	Pre-condition: the victimEntry is dirty.
	Post-condition: the victimEntry is written back into disk and now the victim is not dirty. 
	*/
    private void writeBack( int victimEntry ) {
        if ( pageTable[victimEntry].frame != Entry.INVALID &&
            pageTable[victimEntry].dirty == true ) {
	    	SysLib.rawwrite(pageTable[victimEntry].frame, pages.elementAt(victimEntry)); //*
	    	pageTable[victimEntry].dirty = false;
		}
    }

	/*Cache(int blockSize, int cacheBlocks)
	---------------------------------------------
	Description: The constructor allocates a cacheBlocks number of cache blocks in memory. 
	Each cache Blocks should contain a blockSize-byte of data. Given the block size and the cache size, 
	we instantiate the our variables. 
	*/
    public Cache( int blockSize, int cacheBlocks ) {
		//for now the victim is set to 0
		victim = 0;
		this.blockSize = blockSize; //*
		// instantiate pages
		this.pages = new Vector<byte[]>();

		// instantiate and initialize pageTable
		//The number of entry in a page table is the number of cache blocks we have 
		//cache blocks = accessed pages
		pageTable = new Entry[cacheBlocks];
		for(int i = 0; i < cacheBlocks; i++){
			pageTable[i] = new Entry();
		}

		for(int i = 0; i < cacheBlocks; i++){
			//each cache block should contain a block size byte of data
			byte[] cacheBlock = new byte[blockSize];
			pages.add(cacheBlock);
		}
		
    } //*

	/*read(int blockId, byte buffer[])
	------------------------------------------------------------------------------------------------------
	Description: This function reads the page table and see if the corresponding data to the blockId is in memory. 
	Returns true if it is and false otherwise. 

	There are several different outcomes of this function:
	1. The corresponding entry is found in the cache and we can just read the content from the cache.
	2. The entry is not found in cache and there is a free block entry in the cache. We can read the data
	from the disk to the cache.
	3. The entry is not found in cache and there is no free block entry in the cache (cache is full). We
	will have to find a victim using enhanced second chance algorithm to write content back into disk and 
	read the new content into cache. 

	Pre-condition: The blockId is not negative and we can read disk. 
	Post-condition: returns true if read is sucessfully and false otherwise.

	Note to self: arraycopy(src_arr, sourcePos, dest_array, destPos, len) according to GeeksforGeeks:
	https://www.geeksforgeeks.org/system-arraycopy-in-java/
	*/
    public synchronized boolean read( int blockId, byte buffer[] ) {
		if ( blockId < 0 ) {
	    	SysLib.cerr( "threadOS: a wrong blockId for cread\n" );
	    	return false;
		}

		// locate a valid page
		for ( int i = 0; i < pageTable.length; i++ ) {
	    	if ( pageTable[i].frame == blockId ) {
				// cache hit!!
				//get the pages and put it into block
				byte[] block = pages.elementAt(i); 
				// copy pages[i] to buffer
				//copy it into buffer
				System.arraycopy(block, 0, buffer, 0, blockSize);
				pageTable[i].reference = true;
				return true;
	    	}
		}

		// page miss!!
        // find an invalid page
		boolean found = false;
		int victimEntry = -1;
		int count = 0;
		while((count >= pageTable.length) || (found == true)){
			if(pageTable[count].frame == -1){
				found = true;
				victimEntry = count;
			} else {
				count = count + 1;
			}
			
		}

		if(found == false){
			victimEntry = -1;
		}

		// if no invalid page is found, all pages are full
		if(victimEntry == -1){
			//seek for a victim
       		victimEntry = nextVictim( );
		}

		// write back a dirty copy
		writeBack( victimEntry );

		//read a requested block from disk
		SysLib.rawread(blockId, buffer); 

		//cache it
		byte[] block = new byte[blockSize];
		//copy pages[victimEntry] to buffer
		System.arraycopy(buffer, 0, block, 0, blockSize); 
		pages.set(victimEntry, block); 
		
		pageTable[victimEntry].frame = blockId;
    	pageTable[victimEntry].reference = true;
		return true;
	}

	/*write(int blockID, byte buffer[])
	-------------------------------------------------------------------------
	Description: Writes the contents of the buffer given to the cache block to the blockId given. 
	Returns true if this is sucessful and false otherwise. 

	There are several different outcomes of this function:
	1. The corresponding entry is found in cache. In this case we just have to write the new data to this 
	cache block corresponding to the blockId.
	2. If a corresponding entry is not found in cache but there is a free block entry in the cache, write 
	the new data to this cache block. 
	3. If a corresponding entry is not found and there is no free block in the cache then use enhanced second
	chance algorithm to find the victim to replace with the data. 

	Pre-condition: The blockId is not negative.
	Post-condition: returns true if write is sucessfully and false otherwise.
	*/
    public synchronized boolean write( int blockId, byte buffer[] ) {
		if ( blockId < 0 ) {
	    	SysLib.cerr( "threadOS: a wrong blockId for cwrite\n" );
	    	return false;
		}

		// locate a valid page
		for ( int i = 0; i < pageTable.length; i++ ) {
	    	if ( pageTable[i].frame == blockId ) {
				// cache hit
				//makes a nwe block to use to copy into pages
				byte[] block = new byte[blockSize]; //*
				// copy buffer to pages[i]
				System.arraycopy(buffer, 0, block, 0, blockSize); 
				//copy into pages
				pages.set(i, block); 
				pageTable[i].reference = true;
        		pageTable[i].dirty = true;
				return true;
	    	}
		}

		//page miss
    	//find an invalid page
		boolean found = false;
		int victimEntry = -1;
		int count = 0;
		while((count >= pageTable.length) || (found == true)){
			if(pageTable[count].frame == -1){
				found = true;
				victimEntry = count;
			} else {
				count = count + 1;
			}
		}

		if(found == false){
			victimEntry = -1;
		}

		//if no invalid page is found, all pages are full.
		if(victimEntry == -1){
			//seek for a victim
       		victimEntry = nextVictim( );
		}

		// write back a dirty copy
		writeBack(victimEntry);
		
		// cache it but not write through.
		byte[] block = new byte[blockSize];
		// copy buffer to pages[victimEntry]
		System.arraycopy(buffer, 0, block, 0, blockSize);
		pages.set(victimEntry, block);

		pageTable[victimEntry].frame = blockId;
    	pageTable[victimEntry].reference = true;
    	pageTable[victimEntry].dirty = true;
		return true;
    }


	/*sync()
	----------------------------------------------------------
	Description: All blocks that are dirty is written back intot he disk. Uses a simple for loop to go through
	all blocks and uses helper function writeBack(int victimEntry) find dirty blocks to write back to disk. 

	Pre-condition: there are blocks to be written back.
	Post-condition: All dirty block is written back to disk. 

	Note: this is pre-implemented so no implementation was done personally to this method.
	*/
    public synchronized void sync( ) {
		for ( int i = 0; i < pageTable.length; i++ )
	    	writeBack( i );
		SysLib.sync( );
    }

	/*flush()
	---------------------------------------------------
	Description: Writes back all dirty blocks to disk and wipes all cache blocks. Similar to sync() but 
	also wipes all cache blocks from the page table. 

	Pre-condition: there are blocks to be written back and there is block to be wiped.
	Post-condition: All dirty block is written back to disk, the page table is cleared. 
	Note: this is pre-implemented so no implementation was done personally to this method.
	*/
    public synchronized void flush( ) {
		for ( int i = 0; i < pageTable.length; i++ ) {
	    	writeBack( i );
	   		pageTable[i].reference = false;
	    	pageTable[i].frame = Entry.INVALID;
		}
		SysLib.sync( );
    }


}

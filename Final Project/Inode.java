/*Aurthor: Ruiqi Huang
Date started: 2/6/2024
Date finished: 3/14/2024
CSS 430
--------------------------------------------------------------------------------------------------------------
Description: The inode class is part of the file system. One inode represents one file. In an inode there are 
11 direct blocks and 1 single indirect block. To read into an direct block, you can read directly to get the data, 
but to read an indirect block you have to read the indirect that points to another index, and then read htat index 
to get the data. In an inode you also have length (the file size in bytes), count (the number of file table entries
pointing to this inode), flag (whether the inode is being used or not, if it is what is it used for), InodeSize of 32
(the fixed size of an inode), and directSizr of 11 (the number of directs). 
The main methods we have for inode.java is the toDisk, getIndexBlockNumber, setIndexBlock, findTargetBlock, and 
insertFreeBlock. All the descriptions of the functions can be found below.
*/
public class Inode {
	public final static int iNodeSize = 32;  // fixed to 32 bytes
    public final static int directSize = 11; // # direct pointers

    public final static int NoError              = 0;
    public final static int ErrorBlockRegistered = -1;
    public final static int ErrorPrecBlockUnused = -2;
    public final static int ErrorIndirectNull    = -3;

    public int length;                 // file size in bytes
    public short count;                // # file-table entries pointing to this
    public short flag;       // 0 = unused, 1 = used(r), 2 = used(!r), 
                             // 3=unused(wreg), 4=used(r,wreq), 5= used(!r,wreg)
    public short direct[] = new short[directSize]; // director pointers
    public short indirect;                         // an indirect pointer

	/* Inode ()
	------------------------------------------------------------------------------
	Description: Default constructor of inode. It makes an inode with default valeus such as length = 0, count = 0,
	and flag = 1. 
	Pre-condition: N/A
	Post-condition: A inode is made.
	Note: I did not implement this method as it is provided by assignment template.
	*/
    Inode ( ) {                
	length = 0;
	count = 0;
	flag = 1;
	for ( int i = 0; i < directSize; i++ )
	    direct[i] = -1;
	indirect = -1;
    }

	/* Inode (short iNumber)
	------------------------------------------------------------------------------
	Description: Constructor of inode that takes in a inode number given and makes the inode from the 
	disk using that number. 
	Pre-condition: The iNumber have to be valid. 
	Post-condition: A inode is made.
	Note: I did not implement this method as it is provided by assignment template.
	*/
	Inode ( short iNumber ) {                  
		int blkNumber = 1 + iNumber / 16;          // inodes start from block#1
		byte[] data = new byte[512];
		SysLib.rawread( blkNumber, data );         // get the inode block
		int offset = ( iNumber % 16 ) * iNodeSize; // locate the inode top

		length = SysLib.bytes2int( data, offset ); // retrieve all data members
		offset += 4;                               // from data
		count = SysLib.bytes2short( data, offset );
		offset += 2;
		flag = SysLib.bytes2short( data, offset );
		offset += 2;
		for ( int i = 0; i < directSize; i++ ) {
			direct[i] = SysLib.bytes2short( data, offset );
			offset += 2;
		}
		indirect = SysLib.bytes2short(data, offset);
		offset += 2;
    }

 	
	/* toDisk(short iNumber)
	--------------------------------------------------------------------------------------------------
	Need to save inode info to iNumber-th inode in disk, iNumber is given in the argument.
	Description: This functions saves the iNumber-th inode into the disk. This is to prevent inode inconsistency.
	This implementation is similar to the implementation of the constructor but writing to disk instead
	of just reading from disk. In short, this funtion saves the inode to the disk given the inode Number, iNumber.
	Pre-condition: The inode number is valid
	Post-condition: inode is written to disk
	 */
	void toDisk( short iNumber ) {     
		//---
		byte[] block = new byte[32];
		//---
		//top of inode
		//int offset = ( iNumber % 16 ) * 32;
		int offset = 0;

		//first 32 byte includes: length (4 byte), count (2 byte), flag (2 byte), 1 direct (22 byte), and indirect (2 byte)
		//(4 + 2 + 2 + 22 + 2) byte = 32 byte
		SysLib.int2bytes(length, block, offset);
		offset += 4;
		SysLib.short2bytes(count, block, offset);
		offset += 2;
		SysLib.short2bytes(flag, block, offset);
		offset += 2;

		//first 11 is direct index
		for(int i = 0; i < 11; i++){
			SysLib.short2bytes(direct[i], block, offset);
			offset += 2;
		}

		//last one is indirect index (12)
		SysLib.short2bytes(indirect, block, offset);
		offset += 2;
		//---
		//1 (SuperBlock) + iNumber (number of inodes) / 16 (number inodes in a block)
		//Need the block number to write into disk
		int blockNumber = 1 + (iNumber / 16);
		//iNumber is the inode #
		byte[] blockData = new byte[512];
		SysLib.rawread(blockNumber, blockData);
		offset = iNumber % 16 * 32;
		//copy everything from old data to new
		System.arraycopy(block, 0, blockData, offset, 32);
		//---
		//The block now contains all the values, we also have to write it into disk
		SysLib.rawwrite(blockNumber, blockData);
	}

	//Additional Block Functions mentioned Inode Slide

	/* getIndexBlockNumber()
	-------------------------------------------------------------------------------------------
	Description: This function gets the indirect block number and returns it. It is also called the index
	block number that points directly to the indirect index. 
	Pre-condition: N/A
	Post-condition: returns the index block number	
	 */
	short getIndexBlockNumber(){
		//the index block number is just indirect
		short indexBlockNumber = indirect;
		return indexBlockNumber;
	}

	/* setIndexBlock(short indexBlockNumber)
	--------------------------------------------------------------------------------------
	Description: If the index block have not been set yet, we use this helper function to set the index block. 
	This function first checks if there is any direct blocks that are still not filled because we need to
	fill all direct blocks first in order to set the index block for use. Once we confirmed that all the direct block
	is filled we can set our indirect block. 
	Pre-condition: direct blocks are all filled
	Post-condition: If the index block is succssfully set then it will return true and if not then it will
	return false. 
	 */
	boolean setIndexBlock(short indexBlockNumber){
		//if the direct blocks are filled then we can't set index block (indirect)
		//check all 11 direct block first
		for(int i = 0; i < 11; i++){
			if(direct[i] == -1){
				return false;
			}
		}
		//Now checking indirect block
		//this can be found in assignment FAQ
		int maxIndirectBlocks = 256;
		byte[] block = new byte[512];
		//if the indrect block is -1 then we can set the index
		if(indirect == -1){
			indirect = indexBlockNumber;
			//put the block into disk
			for(int i = 0; i < maxIndirectBlocks; i++){
				SysLib.short2bytes((short)-1, block, i*2);
			}

			SysLib.rawwrite(indexBlockNumber, block);
			return true;
		}
		//if all else fails return false
		return false;
	}

	/*findTargetBlock(int offset)
	-----------------------------------------------------------------------------------------------------
	Find the target block given by the offset. If we find the target block we return the blockID of the
	target block and if not then we return -1.
	Description: Given the offset (or the position of the seekPtr in the file system), it will find the block ID
	of the target that the seek pointer is pointing to and returns it. This method is widely used in both read and write
	system calls of the file system. To find the seek pointer we will look in three places:
	1. direct block
	2. first indirect block
	3. indirect index
	Pre-condition: the target must exist
	Post-contiion: if target is an uninitialized indirect then return -1 otherwise we return the target block ID
	 */
	int findTargetBlock(int offset){
		//this will hold our target
		int target;
		//given in the old final project slides that block = offset/blockSize, block = block ID
		int block = offset/512;

		//if the block is within 11 blocks then it should a direct block
		if(block < directSize){
			target = direct[block];
			//SysLib.cout("target block is in direct. Target = " + target + "\n");
			return target;
		} else {
			//SysLib.cout("target block is in indirect \n");
			//SysLib.cout("indirect = " + indirect + "\n");
			//Our target should be an indirect block
			//We have to make sure the indirect block itself is not empty
			if(indirect == -1){
				//SysLib.cout("target block is in indirect. Target = -1 because indirect = -1. \n");
				return -1;
			}
			//for indirect blocks we have to go into memory
			byte[] data = new byte[512];
			SysLib.rawread(indirect, data);
			offset = (block - directSize) * 2; 
			target = SysLib.bytes2short(data, offset);
			//SysLib.cout("target block is in indirect. Target = " + target + "\n");
			return target;
		}
	}

	/*insertFreeBlock(int offset, short freeBlock)
	--------------------------------------------------------------------------
	Descriptions: Inserts a free block to the place given by the offset. 
	There is three cases of where the free block can be inserted:
	1. We can insert free block as a direct block
	2. We can insert the free block as the beginning of the indirect block
	3. We have to insert block as a indirect block

	Pre-condition: offset must exist. freeBlock given mus tbe a free block that exists. 
	Post-condition (below are the detailed return options)
	Acts as a boolean but with 4 return options:
	1. 1 means true because we can insert free block in indirect
	2. 2 means true because we can insert the free block in direct
	2. 0 means false because the block is not free
	3. -1 means false because indirect block is not set to anything
	 */
	int insertFreeBlock(int offset, short freeBlock){
		int blockNumber = offset / 512;
		//We can insert free block as a direct
		if(blockNumber < 11){
			direct[blockNumber] = freeBlock;
			return 2;
		} else {
			if(indirect == -1){
				//if the inderect have not been set then we need to set it so return false
				return -1;
			} else {
				//find an indirect to insert
				//read the indirect data from disk
				byte[] data = new byte[512];
				SysLib.rawread(indirect, data);
				//new indirect block number
				int newBlockNumber = (blockNumber - 11) * 2;

				//if the block is not free then return false
				if(SysLib.bytes2short(data, newBlockNumber) > 0){
					return 0;
				} else {
					SysLib.short2bytes(freeBlock, data, newBlockNumber);
					SysLib.rawwrite(indirect, data);
					return 1;
				}
				
			}
		}
	}
}
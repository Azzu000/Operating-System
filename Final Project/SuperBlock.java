
/*Aurthor: Ruiqi Huang
Date started: 2/6/2024
Date finished: 3/14/2024
CSS 430
--------------------------------------------------------------------------------------------------------------
Description: The superblock class is part of the file system. The superblock is the first block in the inode table.
It contains totalBlocks (the total blocks int he disk), inodeBlocks (the number of inodes in table), and most importantly
the freeList (the number of head blocks of the free list). The free list points to all the free blocks that are not 
currently being used. These free blocks are located after the inodes that are not free. The main function of superblock is
sync, format, getFreeBlock, and returnBlock, all which will be described in detail below.  
*/
public class SuperBlock {
    private final int defaultInodeBlocks = 64;

	//The number of disk blocks (size of disk)
    public int totalBlocks;

	//The number of inodes
    public int inodeBlocks;

	//The block number of the head block of the freelist (head of free block)
    public int freeList;

	/* SuperBlock(int diskSize)
	----------------------------------------------------------------------------------------------------
	Description: The constructor of the superblock. It initiates all the content of the superblock given 
	the disk size.
	 */
	public SuperBlock( int diskSize ) {
		// read the SuperBlock from disk

		//the offset needs to be a multiple of 4
		int offset = 0;
		//The max SuperBlock size is 512 (maximum disk block size)
		//This creates a new superBlock
		byte[] superBlock = new byte[512];
		//direct access to the superBlock from disk (read SuperBlock from disk)
 		SysLib.rawread(0, superBlock);
		totalBlocks = SysLib.bytes2int(superBlock, offset);
		inodeBlocks = SysLib.bytes2int(superBlock, (offset + 4));
		freeList = SysLib.bytes2int(superBlock, (offset + 8));

		//if any of these are true then there is something wrong
		if(freeList < 2 || totalBlocks != diskSize || inodeBlocks <= 0){
			//Since we detected something wrong with the disk content, we will set default format
			format(64);
			totalBlocks = diskSize;
		}
	}
	
	/*sync()
	--------------------------------------------------------
	Description: This is a helper function that helps synchronize the superblock by writing the superblock
	into the disk.
	Pre-condition: N/A
	Post-condition: the superblock is synchronized into disk

	Note: I did not implement this function as it is proviced in assignment template 
	*/
	void sync( ) {
		byte[] superBlock = new byte[512];
		SysLib.int2bytes( totalBlocks, superBlock, 0 );
		SysLib.int2bytes( inodeBlocks, superBlock, 4 );
		SysLib.int2bytes( freeList, superBlock, 8 );
		SysLib.rawwrite( 0, superBlock );
		SysLib.cerr( "SuperBlock synchronized\n" );
    }

	/*format()
	----------------------------------------------------------
	Description: Default function of format. Uses the format
	function to initiate the default number of inode blocks. 
	Pre-condition: N/A
	Post-condition: the superblock is formatted

	Note: I did not implement this function as it is proviced in assignment template 
	*/
    void format( ) {
		// default format with 64 inodes
		format( defaultInodeBlocks );
    }

	/*format(int files)
	--------------------------------------------------------------------------------------------------
	Description: Decides how many files you would like to create in the file system. The file argument
	passed to format indicates the maximum number of inodes we want. 
	Pre-condition: the number of files must be valid.
	Post-condition: the super block is initialized. 
	 */
	 void format( int files ) {
		// initialize the superblock
		//the number of files is the same as the number of inodes since each file is represented by an inode
		inodeBlocks = files;

		//number of files (inodeBlock) * (byte size for inodes in one block / maximum bytes for a disk) + 2 (minimum number of freelist)
		//files (or number of inodes) / 16 + 2 (superblock plus first free block)
		freeList = (files/16) + 2;
        totalBlocks = 1000;

		 //write free block into disk (the freeblock starts at the freelist (head) until the end block (freeblock to totalblock)
		 for(int i = freeList; i < totalBlocks; i++){
            byte[] block = new byte[512];

			//set everything inside block to 0
			for(int j = 0; j < 512; j++){
				block[j] = 0;
			}
			//turns block into bytes so we can put in disk
			SysLib.int2bytes(i + 1, block, 0);
			SysLib.rawwrite(i, block);
		 }

		  //save all the inodes to the disk
		 for(int i = 0; i < inodeBlocks; i++){
			 //temporarily holds the inode
			 Inode tempInode = new Inode();
             //inode is unused
             tempInode.flag = 0;
			 tempInode.toDisk((short)i);
		 }
        
		sync();
	 }

	 /*getFreeBlock()
	 ---------------------------------------------------------------------------------------------
	 Description: Gets a new free block from the freelist. Dequeue the top block from the free list.
	 Pre-condition: the freeList does not equal -1 meaning that there is no free blocks and the free list 
	 does not exist. 
	 Post-condition: A free block is taken from the disk returned and dequeued tot he top of the free list
	  */
	public int getFreeBlock( ) {
		//temporarily holds current free block
		int freeBlock = freeList;
		
		//freeList can't be -1 and has to be smaller than the total number of blocks
		if(freeBlock != -1){ 
			//this will hold our first free block we find
			byte[] block = new byte[512];
			SysLib.rawread(freeBlock, block);
			//next free block
			freeList = SysLib.bytes2int(block, 0);

			//update the disk
			//convert back to bytes to write back into disk
			//set the block to 0 to say this block have been freed
			SysLib.int2bytes(0, block, 0);
			SysLib.rawwrite(freeBlock, block);
		}
		//return the first free block
		return freeBlock;
	}

	/*return Block(int oldBlockNumber)
	-----------------------------------------------------------------------------------------
	Description: returnBlock takes the old block number given and return it back into the free list and disk.
	The block should be enqueued to the end of the free list.
	Pre-condition: the block number given exists
	Post-condition: the free block is returned and enqueued to the end of the free list. 
	 */
	// you implement
	public boolean returnBlock( int oldBlockNumber ) {
		if(oldBlockNumber >= 0){
			byte[] block = new byte[512];
			//turns the block to bytes to return to disk
			SysLib.int2bytes(freeList, block, 0);
			//set the head of the free list to the old block number
			freeList = oldBlockNumber;
			SysLib.rawwrite(oldBlockNumber, block);
			return true;
		} else {
			return false;
		}
	}
	
}

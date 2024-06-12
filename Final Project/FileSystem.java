/*Aurthor: Ruiqi Huang
Date started: 2/6/2024
Date finished: 3/14/2024
CSS 430
--------------------------------------------------------------------------------------------------------------
Description: The file system contains a file table, a single level root directory, and the superblock which 
contains all the inode information. This file system uses index allocation method to allocate files. This means 
that we will have a inode or file table filled with an index of direct blocks and indirect blocks (in this case 1 
indirect block). Each inode represents one file. The details of all the different classes that make up the file system
is described in depth in the comment header of its respective class. The file system has all the system calls in which
we can access the files or file information. System calls like open, format, close, fsize, read, write, seek, 
deallocAllBlocks and delete. To use these system calls we must use the corresponding SysLib command for each system call.
The SysLib command will then tell the Kernel which call to initiate. The file system itself is in the Kernel and
therefore only the Kernel can initiate the system calls. 
*/

public class FileSystem {
    private SuperBlock superblock;
    private Directory directory;
    private FileTable filetable;

    /*FileSystem( int diskBlocks )
    ---------------------------------------------------------------------------
    Description: File System constructor used to initialize the file system given the number of disk blocks.  

    Note: I did not implement this function as it is proviced in assignment template 
    */
    public FileSystem( int diskBlocks ) {
        // create superblock, and format disk with 64 inodes in default
        superblock = new SuperBlock( diskBlocks );
    
        // create directory, and register "/" in directory entry 0
        directory = new Directory( superblock.inodeBlocks );
    
        // file table is created, and store directory in the file table
        filetable = new FileTable( directory );
    
        // directory reconstruction
        FileTableEntry dirEnt = open( "/", "r" );
        int dirSize = fsize( dirEnt );
        if ( dirSize > 0 ) {
            byte[] dirData = new byte[dirSize];
            read( dirEnt, dirData );
            directory.bytes2directory( dirData );
        }
        close( dirEnt );
    }

    /*sync( )
    ---------------------------------------------------------------------------
    Description: A helper function that helps synchronize both the directory and the superblock.  

    Pre-condition: N/A
    Post-condition: synchronize superblock and directory

    Note: I did not implement this function as it is proviced in assignment template
    */
    void sync( ) {
        // directory synchronizatioin
        FileTableEntry dirEnt = open( "/", "w" );
        byte[] dirData = directory.directory2bytes( );
        write( dirEnt, dirData );
        close( dirEnt );
    
        // superblock synchronization
        superblock.sync( );
    }

    /*format( int files )
    ---------------------------------------------------------------------------
    Description: The files argument is the number of files we are going to format.
    Format uses the super block format function to format the file. It will format the disk's content as well.
    It will also create the directory and file table which the directory will be stored in. 

    Pre-condition: the number of files is valid and the file table is empty
    Post-condition: return 0 of everything is successfully formatted and -1 otherwise

    Note: I did not implement this function as it is proviced in assignment template
    */
    boolean format( int files ) {
        // wait until all filetable entries are destructed
        while ( filetable.fempty( ) == false );
    
        // format superblock, initialize inodes, and create a free list
        superblock.format( files );
    
        // create directory, and register "/" in directory entry 0
        directory = new Directory( superblock.inodeBlocks );
    
        // file table is created, and store directory in the file table
        filetable = new FileTable( directory );
    
        return true;
    }

    /*open( String filename, String mode )
    -------------------------------------------------------------------------------------
    Description: Opens the file given the file name and mode. This uses falloc to allocated the file. This 
    call will allocate a new file descriptor to this file and if it does not exist in the mode w, w+, or a
    the file will eb created. 

    Pre-condition: The file inidicated by file name must exist and the mode must also exist
    Post-condition: Return entry if it is successful and return -1 if it is not

    Note: Implementation was referenced from filesystem.pdf given in assignment spec notes
     */
    FileTableEntry open( String filename, String mode ) {
        // filetable entry is allocated
        FileTableEntry ftEnt = filetable.falloc(filename, mode);
        if(mode.equals("w")){
            if(deallocAllBlocks(ftEnt) == false){
                return null;
            }
        }
        return ftEnt;
    }

    /*close( FileTableEntry ftEnt )
    -----------------------------------------------------------
    Description: closes the file by freeing the file entry from being used. This means decrementing
    the count of the entry. 
    Pre-condition: The file is open
    Post-condition: returns true of the file is sucessfully closed

    Note: I did not implement this function as it is proviced in assignment template
    */
    boolean close( FileTableEntry ftEnt ) {
        // filetable entry is freed
        synchronized ( ftEnt ) {
            // need to decrement count; also: changing > 1 to > 0 below
            ftEnt.count--;
            if ( ftEnt.count > 0 ) // my children or parent are(is) using it
                return true;
        }
        return filetable.ffree( ftEnt );
    }
	
	

    /*fsize( FileTableEntry ftEnt )
    -----------------------------------------------------------
    Description: if the file entry is not null then we will get the file size (inode length).
    Pre-condition: The file entry given is not null
    Post-condition: returns the file size is the entry is not null and returns -1 otherwise
    */
    int fsize( FileTableEntry ftEnt ) {
        //if the number of files is null then return -1
        if (ftEnt == null) {
            return -1;
        } else {
            return ftEnt.inode.length;
        }
    }


    /*read( FileTableEntry ftEnt, byte[] buffer )
    --------------------------------------------------------------------------------------------------
    Description: Reads the byte.length from the file given by the file descriptor. We know the starting position
    of where to read by the seekPtr.
    Below is what will happen if the bytes betweem the current seek pointer and the end of the fiels is less than th
    buffer length:
            a. SysLib.read reads as many bytes as possible and puts them into the beginning of buffer.
            b. It increments the seek pointer by the number of bytes to have been read.
            c. The return value is the number of bytes that have been read, or a negative value upon an error.
    Pre-condition: The file we want to read to exists and it is in mode read. 
    Post-condition: returns the number of bytes read
     */
    int read( FileTableEntry ftEnt, byte[] buffer ) {
        //This is a read not write or append
        if ( ftEnt.mode == "w" || ftEnt.mode == "a" ){
            SysLib.cout("It is not read.");
            return -1;
        }
        int offset = 0;              // buffer offset
        int left = buffer.length;  // the remaining data of this buffer
    
        synchronized ( ftEnt ) {
			// repeat reading until no more data  or reaching EOF
            //If the seek pointer reaches the end and there is nothing left we know we are done
            while(ftEnt.seekPtr < fsize(ftEnt) && left > 0){
                //initialize all needed variables to read
                int blockID = ftEnt.inode.findTargetBlock(ftEnt.seekPtr);
                //SysLib.cout("blockID for read = " + blockID + "\n");

                //if the block id is -1 then skip this loop
                if(blockID == -1){
                    break;
                } else {
                    byte[] data = new byte[512];
                    //read data from disk
                    SysLib.rawread(blockID, data);
                    //start read position
                    int start = ftEnt.seekPtr % 512; //*
                    //the amount of file data we still need to read
                    int fileData = fsize(ftEnt) - ftEnt.seekPtr;
                    //the amount of block data we still have to read
                    int blockData = 512 - start;
                    //the size of data we will read depends on # buffer data left, # file data left to read, and # of block data left to read
                    int sizeRead = 0;
                    if(fileData < blockData){
                        //if the #file data left we don't have to read the full block
                        sizeRead = fileData;
                    } else {
                        //otherwise we have to read the full block
                        sizeRead = blockData;
                    }

                    //We also have to check whether the buffer have enough space for the data
                    if(sizeRead > left){
                        sizeRead = left;
                    }

                    //copy everything to the buffer
                    System.arraycopy(data, start, buffer, offset, sizeRead);

                    //the buffer size is decreasing by read size
                    left = left - sizeRead;
                    //increments seek pointer to the number of bytes to have been read
                    ftEnt.seekPtr = ftEnt.seekPtr + sizeRead;
                    //increment the offset
                    offset = offset + sizeRead;
                }
            }
            //SysLib.cout ("offset for read = " + offset + "\n" );
            //return the number of bytes that have been read
            return offset;
        }
    }

    /*write( FileTableEntry ftEnt, byte[] buffer )
    -----------------------------------------------------------------------------------------------------
    Description: This function is the system call to write to the file indicated byt he file descriptor. It will
    write from the position of the seekPtr. This operation includes append operations which will start at the end
    of the file (seekPtr should be at end). As we write the seekPtr will be moved by the number of bytes we read.
    We will return the number of bytes written at the end. If there is a problem or an error, it will return -1. 
    
    If the blockID of the seekPtr is -1, that means that the poisiton the seek pointer is in has not been set yet. 
    This is normal as we want to write at that position. There are a couple of ways in which we will get our block ID
    depending on the position of the seek pointer. They are listed below:
    1. The seek pointer is in direct therefore we can directly insert free block in the direct
    2. The seek pointer is in indirect but indirect index have not been initialized. We will initialize the 
    indirect along with the indirect index, which means we will need 2 free blocks
    3. The seek pointer is in indirect index, we will have to use the indirect pointer to get to the index
    and then place the free block there

    After all this we can get the block ID of the free block that we are going to write to.
    Pre-condition: The file we want to write to exists and it is not in mode read. 
    Post-condition: returns the number of bytes written
     */
    int write( FileTableEntry ftEnt, byte[] buffer ) {
        //at this point, ftEnt is only the one to modify the inode
        if ( ftEnt.mode == "r" ){
            return -1;
        }
    
        synchronized ( ftEnt ) {
            int offset = 0;              // buffer offset
            int left = buffer.length;  // the remaining data of this buffer

            //while there is still buffer left
            while(left > 0){
                //get the block id of the seek pointer
                int blockID = ftEnt.inode.findTargetBlock(ftEnt.seekPtr);
                //SysLib.cout("blockID for write = " + blockID + "\n");
                
                //if the blockID is equal to -1 that means we need to write to a free block
                //We have to find the free block
                if(blockID == -1){
                    //get free block so we can add it to the inode
                    int freeBlock = superblock.getFreeBlock(); //short
                    //if we hit th index block then we need two blocks to make start the index block
                    //one for the indirect block itself and one for the new index block
                    int indexFreeBlock;
                    int insertFreeBlock = ftEnt.inode.insertFreeBlock(ftEnt.seekPtr, (short) freeBlock);
                    //Not able to insert free block in general
                    if(insertFreeBlock == 0){
                        //make sure to return the free block
                        superblock.returnBlock(freeBlock);
                        //return -1 to show that write failed
                        return -1;
                    } else if(insertFreeBlock == -1){
                        indexFreeBlock = superblock.getFreeBlock();
                        //The indirect block is not set so set it
                        boolean setIndexBlock = ftEnt.inode.setIndexBlock((short) indexFreeBlock);
                        //if we failed to set the index block then write has failed
                        if(setIndexBlock == false){
                            //make sure to return the free block to the super bloxk
                            superblock.returnBlock(freeBlock);
                            return -1;
                        }
                        //try inserting the free block again 
                        insertFreeBlock = ftEnt.inode.insertFreeBlock(ftEnt.seekPtr, (short) freeBlock);
                        //if the insert is not successful again then write failed
                        if(insertFreeBlock != 1){
                            superblock.returnBlock(freeBlock);
                            return -1;
                        }
                    }
                    //Once we inserted free block we find the blockID again
                    //Now the seek pointer should be pointed to the free block
                    //int oldblockID = ftEnt.inode.findTargetBlock(ftEnt.seekPtr);
                    //SysLib.cout("oldBlockID = " + oldblockID + "\n");
                    //only if we can insert the free block we do this
                    if(insertFreeBlock == 2 || insertFreeBlock == 1){
                        blockID = ftEnt.inode.findTargetBlock(ftEnt.seekPtr);
                        //SysLib.cout("New blockID for write = " + blockID + "\n");
                    } else {
                        blockID = freeBlock;
                    }
                }

                
                byte[] data = new byte[512];
                int start = ftEnt.seekPtr % 512; //*
                //the amount of block data we still have to read
                //int blockData = ftEnt.inode.length - start; //*
                int blockData = 512 - start;

                int sizeWrite = 0;
                if(blockData < left){
                    //if the amount of block data left is smaller than the amount of buffer left then we can write for the rest of the block
                    sizeWrite = blockData;
                } else {
                    //if the amount of buffer left is less than we can only write for the number of buffer left
                    sizeWrite = left;
                }
                //read data from disk
                SysLib.rawread(blockID, data);

                //copy the buffer from the offset to the data from the start
                //the total size should be the size we can write
                System.arraycopy(buffer, offset, data, start, sizeWrite);
                //write the data to disk
                SysLib.rawwrite(blockID, data);

                //the buffer size is decreasing by write size
                left = left - sizeWrite;
                //increments seek pointer to the number of bytes to have been written
                ftEnt.seekPtr = ftEnt.seekPtr + sizeWrite;
                //if the seekPtr has changed that means we have written tot he file so the file size have to change
                if(ftEnt.seekPtr > ftEnt.inode.length){
                    //Since we have written to the file, we need to adjust the file size or inode size
                    ftEnt.inode.length = ftEnt.seekPtr;
                }
                //increment the offset
                offset = offset + sizeWrite;
            }
            //update the inode to disk
            ftEnt.inode.toDisk(ftEnt.iNumber);
            //SysLib.cout("offset for write = " + offset + "\n");
            return offset;
        }
    }

    /*deallocAllBlocks( FileTableEntry ftEnt )
    -----------------------------------------------------------------------------------
    Description: Deallocate all the blocks of the inode. This includes all the direct and indirect blocks. 
    Pre-condition: The file is not already empty and the given fiel entry exists. 
    Post-condition: All inodes are returned to disk and is deallocated. 
    deallocate all the blocks given file/inode is pointing to
     */
    private boolean deallocAllBlocks( FileTableEntry ftEnt ) {
        if(ftEnt.inode.count != 1){
            return false;
        }

        //deallocate indirect blocks
        //need to read indirect blocks from disk
        //make sure indirect is not already empty
        if(ftEnt.inode.count == 1 && ftEnt != null && ftEnt.inode.indirect >= 0){
            //SysLib.cout("Deallocating indirect block...");
            byte[] data = new byte[512];

        //clear all direct and indirect blocks of inode
        //deallocate direct blocks
        for(int i = 0; i < Inode.directSize; i++){
            //SysLib.cout("Deallocating direct block " + i + " \n");
            //if the direct block is not already empty
            if(ftEnt.inode.direct[i] != -1){
                //return block to free list
                superblock.returnBlock(ftEnt.inode.direct[i]);
                //deallocate block
                ftEnt.inode.direct[i] = -1;
            }
        }

        //read from disk
            SysLib.rawread(ftEnt.inode.indirect, data);
            //set indirect to -1
            ftEnt.inode.indirect = -1;
            if(data != null){
                int offset = 0;
                //convert block from bytes to short because it was in bytes in disk
                int block = SysLib.bytes2short(data, offset);
                while(block != -1 && offset < 512){
                    superblock.returnBlock(block);
                    //next indirect block
                    offset = offset + 2;
                    //set block to new index
                    block = SysLib.bytes2short(data, offset);
                }
            }
        }
        //return actual indirect block
        superblock.returnBlock(ftEnt.inode.indirect);
        ftEnt.inode.toDisk(ftEnt.iNumber);
        return true;
    }

	
	/*delete( String filename )
    -----------------------------------------------------------------------
    Description: Deletes the file given by the file name. 
    Pre-condtion: The file is not currently open or will be openned
    Post-condition: The file is deleted
    Destroys the file specified by fileName.  
    */
    boolean delete( String filename ) {
        FileTableEntry ftEnt = open( filename, "w" );
        //SysLib.cout("ftEnt's inode number is " + ftEnt.iNumber + "\n");
        short iNumber = ftEnt.iNumber;
        //set the inode for mark for deletion
        ftEnt.inode.flag = 2;
        if(close( ftEnt ) == true && directory.ifree( iNumber ) == true){
            return true;
        } else {
            return false;
        }
    }

    private final int SEEK_SET = 0;
    private final int SEEK_CUR = 1;
    private final int SEEK_END = 2;

    /*seek( FileTableEntry ftEnt, int offset, int whence )
    --------------------------------------------------------------------------------------------
    Description:
    Updates the seek pointer corresponding to fd as follows:
    1. If whence is SEEK_SET (=0), the file's seek pointer is set to offset bytes from
    the beginning of the file.
    2. If whence SEEK_CUR (=1), the file's seek pointer is set to its current value plus the offset.
    The offset can be positive or negative.
    3. If whence is SEEK_END (=2), the file's seek pointer is set to the size of the file plus the
    offset. The offset can be positive or negative.

    Boundaries:
    1. If the user attempts to set the seek pointer to a negative number you must clamp it to zero
    and the return must be successful.
    2. If the user attempts to set the seek pointer to beyond the file size, you must set the seek
    pointer to the end of the file and the return must be successful.

    The offset location of the seek pointer in the file is returned from the call to seek.
    */
    int seek( FileTableEntry ftEnt, int offset, int whence ) {

        synchronized ( ftEnt ) {
            /*
            System.out.println( "seek: offset=" + offset +
                    " fsize=" + fsize( ftEnt ) +
                    " seekptr=" + ftEnt.seekPtr +
                    " whence=" + whence );
            */
            if(whence == SEEK_SET){
                //the file's seek pointer is set to offset bytes from the beginning of the file
                ftEnt.seekPtr = offset;
            } else if(whence == SEEK_CUR){
                //the file's seek pointer is set to its current value plus the offset.
                //offset can be positive or negative.
                ftEnt.seekPtr = ftEnt.seekPtr + offset;
            } else if(whence == SEEK_END){
                //the file's seek pointer is set to the size of the file plus the offset.
                //offset can be positive or negative.
                ftEnt.seekPtr = ftEnt.inode.length + offset;
            }
		}

        if(ftEnt.seekPtr < 0){
            //if negative number clamp it to zero
            ftEnt.seekPtr = 0;
        } else if(ftEnt.seekPtr > ftEnt.inode.length){
            //if beyond the file size set the seek pointer to the end of the file
            ftEnt.seekPtr = ftEnt.inode.length;
        }

        //return the seek pointer
        return ftEnt.seekPtr;
    }
}

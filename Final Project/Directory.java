/*Aurthor: Ruiqi Huang
Date started: 2/6/2024
Date finished: 3/14/2024
CSS 430
--------------------------------------------------------------------------------------------------------------
Description: The Directory class is part of the file system. The directory is a single level directory called the root
directory. One file is maintained in different directory entry and each directory entry contains a file name and 
a inode number that corresponds with it. The directory keeps track of what inodes or files are being in use and 
the mac number of inodes or file that is allowed. The variables maxChars represent the max number of characters
allowed in a file name, fsizes is the actual size of each file name, and fnames is the file names in characters.
The main functions of the directory is bytes2directory, directory2bytes, ialloc, ifree, and namei. All function
details are listed below. 
*/

import java.util.*;

public class Directory {
	private static int maxChars = 30; // the max characters of each file name

    private int fsizes[];             // the actual size of each file name
    private char fnames[][];          // file names in characters

	/*Directory(int maxInumber)
	--------------------------------------------------------------------------
	Description: This is the constructor of the directory. It is used to initialize the directory.
	*/
    public Directory ( int maxInumber ) {       // a default constructor
	fsizes = new int[maxInumber];           // maxInumber = max files
	for ( int i = 0; i < maxInumber; i++ )  // all file sizes set to 0
	    fsizes[i] = 0;
	fnames = new char[maxInumber][maxChars];

	String root = "/";                      // entry(inode) 0 is "/"
	fsizes[0] = root.length( );
	root.getChars( 0, fsizes[0], fnames[0], 0 ); 
    }

	/*bytes2directory(byte data[])
	----------------------------------------------------------------------------------
	Description: This method takes the bytes or information retrieved from the disk and
	initialize the directory with it.
	Pre-condition: N/A
	Post-condition: The bytes from byte array is initialized into directory

	Note: Implementation is referenced from FinalRoject.pdf given in assignment spec notes
	 */
	public void bytes2directory( byte data[] ) {
		// assumes data[] contains directory information retrieved from disk
        // initialize the directory fsizes[] and fnames[] with this data[]
		//same variable as the one used in directory2bytes()
		int offset = 0;

		//looking at bytes2int function, it takes the 4 offset from the current offset so offset += 4
		for(int i = 0; i < fsizes.length; i++, offset += 4){
			fsizes[i] = SysLib.bytes2int(data,offset);
		}

		//the maxChars is 30 but the max byte is 60 so offset = maxChars * 2
		for(int i = 0; i < fnames.length; i++, offset += maxChars * 2){
			String name = new String(data, offset, maxChars * 2);
			name.getChars(0, fsizes[i], this.fnames[i], 0);
		}

	}

	/*directory2bytes()
	-------------------------------------------------------------
	Description: Converts and return the directory information in byte array. The byte array will then be written 
	back to the disk.

	Pre-condition: N/A
	Post-condition: the byte array is returned. 

	Note: I did not implement this function as it is proviced in assignment template
	*/
	public byte[] directory2bytes( ) {
        // converts and return directory information into a plain byte array
		// this byte array will be written back to disk
		byte[] data = new byte[fsizes.length * 4 + fnames.length * maxChars * 2];
		int offset = 0;
		for ( int i = 0; i < fsizes.length; i++, offset += 4 ) {
			SysLib.int2bytes(fsizes[i], data, offset);
		}

		for ( int i = 0; i < fnames.length; i++, offset += maxChars * 2 ) {
			String tableEntry = new String( fnames[i], 0, fsizes[i] );
			byte[] bytes = tableEntry.getBytes( );
			System.arraycopy( bytes, 0, data, offset, bytes.length );
		}
		return data;
		}

	/*ialloc(String filename)
	-------------------------------------------------------------
	Description: Given the file name, we will allocate a new inode number for this file. 
	Pre-condition: file name exists
	Post-condition: a new innode number is allocated to this file. returns the inode number. 

	Note: I did not implement this function as it is proviced in assignment template
	*/
	public short ialloc ( String filename ) {
		// filename is the name of a file to be created.
		// allocates a new inode number for this filename.
		short i;
		// i = 0 is already used for "/"
		for ( i = 1; i < fsizes.length; i++ ) {
			if ( fsizes[i] == 0 ) {
			fsizes[i] = Math.min( filename.length( ), maxChars );
			filename.getChars( 0, fsizes[i], fnames[i], 0 );
			return i;
			}
		}
		return -1;			
    }

	/*ifree(short iNumber)
	-------------------------------------------------------------------------------------------------
	Description: Deallocates the inode number given (iNumber)
	pre-condition: The inode number have not already been deallocated.
	post-condition: Returns true if we successfully deallocate the inode number given in the argument
	and returns false otherwise.
	 */
    public boolean ifree ( short iNumber ) {
		// deallocates this inumber (inode number).
		// the corresponding file will be deleted.

		//successfully deallocated inode number
		if(fsizes[iNumber] > 0){
			//deallocates this iNumber (inode number)
			fsizes[iNumber] = 0;
			return true;
		} else {
			//otherwise we failed to deallocate the inode number
			return false;
		}
    }

	/*namei(String filename)
	------------------------------------------------------------------------
	Description: Uses the file naem to find the table entry with the corresponding file name.
	Pre-condition: The file name exists.
	Post-condition: returns the inode number of with the corresponding filename

	Note: I did not implement this function as it is proviced in assignment template
	*/
    public short namei( String filename ) {
		short i;
		for ( i = 0; i < fsizes.length; i++ ) {
			if ( fsizes[i] == filename.length( ) ) {
			String tableEntry = new String( fnames[i], 0, fsizes[i] );
			if ( filename.compareTo( tableEntry ) == 0 )
				return i;
			}
		}
		return -1;
    }
	
	
}


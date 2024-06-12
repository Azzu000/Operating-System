/*Aurthor: Ruiqi Huang
Date started: 2/6/2024
Date finished: 3/14/2024
CSS 430
--------------------------------------------------------------------------------------------------------------
Description: The FileTable class is part of the file system. It is what holds all the file table entries. The file
table is shared among all the threads (meaning they can all use it). Each file entry has a seekPtr that is set depending
on the mode of the file. The file table itself contain the table for the entries and the root directory. 
It keeps track of the next read and write position of the file. The main function of the FileTable is falloc, ffree, and fempty. 
*/

import java.util.Vector;

public class FileTable {
// File Structure Table

    private Vector<FileTableEntry> table;// the entity of File Structure Table
    private Directory dir;         // the root directory
    
    public FileTable ( Directory directory ) {// a default constructor
	table = new Vector<FileTableEntry>( );// instantiate a file table
	dir = directory;                      // instantiate the root directory
    }

	/* falloc(String fname, String mode)
	-------------------------------------------------------------------------------------------------------
	Descrition: Allocates a file to the file table given the filename and the mode the file is in. The modes of a 
	file can either be r (read), w (write), w+ (read and write), or a (apoend). Below are some inode flags information
	and what they stand for. 
	Inode Flag Notes (temporary comment):
			0 = unused
			1 = used - read
			2 = marked for deletion and no one can read or write to it.
			3 = write to a new/empty file
			//less important flags
			4 = a file that have been written to and still have it open or has closed it
			5 = relatively same as 2
	Note: Some code was referenced from old file system slides given in the assignments specs.
	Some comments is also given in the assignment specs, these comments include hints on how this function
	works.
	 */
	public synchronized FileTableEntry falloc( String fname, String mode ) {
		
		Inode inode = null;
		short inodeNumber = -1;

		while (true) {
			//allocate a new file (structure) table entry for this file name
			if (fname == "/") {
				inodeNumber = 0;
			} else {
				inodeNumber = dir.namei(fname);
				//SysLib.cout("inodeNumber for " + fname + " is " + inodeNumber + "\n");
			}

			//Only when the inode number is -1 and the mode is not read that we allocate a new inode for file
			if(inodeNumber == -1 && mode != "r"){
				//if iNumber does not exist
				//get the inode number from directory and set it to the inode
				inodeNumber = dir.ialloc(fname);
				inode = new Inode();
			} else if (inodeNumber >= 0) {
				//allocate/retrieve and register the corresponding inode using dir
				inode = new Inode(inodeNumber);

				if (inode.flag == 2) {
					SysLib.cerr("File is set to be deleted. Cannot access.");
					return null;
				}

				//Is the mode read
				if (mode == "r") {
					//if inode is not write or to be deleted then we can read it
					if (inode.flag != 3 || inode.flag != 2) {
						//set flag to used for reading (inode.flag == 0 || inode.flag == 1)
						inode.flag = 1;
						break; //no need to wait
					} else {
						//if the flag is not used to read or unused then we need to wait
						try {
							wait();
						} catch (InterruptedException e) {
						}
						//set the flag to show that is it being used and read by someone
						inode.flag = 1;
					}

				} else {
					if (inode.flag == 2) {
						SysLib.cerr("File is set to be deleted. Cannot access.");
						return null;
					} else {
						//if file is not in write and not to be deleted then we can write 
						if (inode.flag != 3 || inode.flag != 2) {
							//no need to wait and set flag to write
							inode.flag = 3;
							break;
						} else {
							//otherwise wait
							try {
								wait();
							} catch (InterruptedException e) {
							}
						}
					}

				}
			} else {
				return null;
			}
		}
		//increment this inode's count
		inode.count++;
		//immediately write back this inode to the disk
		inode.toDisk(inodeNumber);
		FileTableEntry entry = new FileTableEntry(inode, inodeNumber, mode);
		table.addElement(entry);
		//return a reference to this file (structure) table entry
		return entry;
	}

	/*ffree(FileTableEntry e)
	-----------------------------------------------------------------------------
	Description: Frees the file table entry given corresponding to the index. 
	Pre-condition: N/A
	Post-condtion: return true of the file table entry was freed successfully and false otherwise. 

	Note: I did not implement this function as it is proviced in assignment template
	*/
	public synchronized boolean ffree (FileTableEntry e ){
		// receive a file table entry
		// free the file table entry corresponding to this index
		if (table.removeElement(e) == true) { // find this file table entry
			e.inode.count--;       // this entry no longer points to this inode
			switch (e.inode.flag) {
				case 1:
					e.inode.flag = 0;
					break;
				case 2:
					e.inode.flag = 0;
					break;
				case 4:
					e.inode.flag = 3;
					break;
				case 5:
					e.inode.flag = 3;
					break;
			}
			e.inode.toDisk(e.iNumber);     // reflect this inode to disk
			e = null;                        // this file table entry is erased.
			notify();
			return true;
		} else
			return false;
	}

	/*fempty()
	-------------------------------------------------
	Description: empties the file table. 
	Pre-condtion: file table is not already empty.
	Post-condtion: return true of the file table have been emptied successfully and false otherwise. 

	Note: I did not implement this function as it is proviced in assignment template
	*/
	public synchronized boolean fempty() {
		return table.isEmpty();             // return if table is empty
	}                                        // called before a format
}

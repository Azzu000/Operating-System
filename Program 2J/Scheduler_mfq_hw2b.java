/*Author for run function: Ruiqi Huang
CSS 430 Operating System
2/3/2024
------------------------------------------------------------------------------------------
Description: This imitates a multilevel feedback queue scheduler for our ThreadOS system. In this multilevel
feedback queue scheduler, we have 3 queues in which the threads can be inserted: queue0, queue1, and queue2.
For queue0 the time quantum is timeslice/2 (1/2), queue1 the time quantum is timeslice (1), and queue2
the time quantum is 2*timeslice (2). Every timeslice/2 we will have a time interrupt where it tells the
scheduler to check other queues of higher priority to see if there are new threads coming into queue0.
This multilevel feedback queue uses the scheduling algorithm of first come first serve, which means any
thread that goes in the queue first gets served first.
 */

import java.util.*; // Scheduler_mfq.java

public class Scheduler extends Thread
{   @SuppressWarnings({"unchecked","rawtypes"})
    private Vector<TCB>[] queue = new Vector[3];
    private int timeSlice;
    private static final int DEFAULT_TIME_SLICE = 1000;

    // New data added to the original algorithm 
    private boolean[] tids; // Indicate which ids have been used
    private static final int DEFAULT_MAX_THREADS = 10000;

    // A new feature added to the original algorithm 
    // Allocate an ID array, each element indicating if that id has been used
    private int nextId = 0;
    private void initTid( int maxThreads ) {
	tids = new boolean[maxThreads];
	for ( int i = 0; i < maxThreads; i++ )
	    tids[i] = false;
    }

    // A new feature added to the original algorithm 
    // Search an available thread ID and provide a new thread with this ID
    private int getNewTid( ) {
	for ( int i = 0; i < tids.length; i++ ) {
	    int tentative = ( nextId + i ) % tids.length;
	    if ( tids[tentative] == false ) {
		tids[tentative] = true;
		nextId = ( tentative + 1 ) % tids.length;
		return tentative;
	    }
	}
	return -1;
    }

    // A new feature added to the original algorithm 
    // Return the thread ID and set the corresponding tids element to be unused
    private boolean returnTid( int tid ) {
	if ( tid >= 0 && tid < tids.length && tids[tid] == true ) {
	    tids[tid] = false;
	    return true;
	}
	return false;
    }

    // A new feature added to the original algorithm 
    // Retrieve the current thread's TCB from the queue
    public TCB getMyTcb( ) {
	Thread myThread = Thread.currentThread( ); // Get my thread object
	synchronized( queue ) {
	    for ( int level = 0; level < 3; level++ ) {
		for ( int i = 0; i < queue[level].size( ); i++ ) {
		    TCB tcb=queue[level].elementAt( i );
		    Thread thread = tcb.getThread( );
		    if ( thread == myThread ) // if this is my TCB, return it
			return tcb;
		}
	    }
	}
	return null;
    }

    // A new feature added to the original algorithm 
    // Return the maximal number of threads to be spawned in the system
    public int getMaxThreads( ) {
	return tids.length;
    }

    public Scheduler( ) {
	timeSlice = DEFAULT_TIME_SLICE;
	initTid( DEFAULT_MAX_THREADS );
	for ( int i = 0; i < 3; i++ ) queue[i] = new Vector<TCB>( );
    }

    public Scheduler( int quantum ) {
	timeSlice = quantum;
	initTid( DEFAULT_MAX_THREADS );
	for ( int i = 0; i < 3; i++ ) queue[i] = new Vector<TCB>( );
    }

    // A new feature added to the original algorithm 
    // A constructor to receive the max number of threads to be spawned
    public Scheduler( int quantum, int maxThreads ) {
	timeSlice = quantum;
	initTid( maxThreads );
	for ( int i = 0; i < 3; i++ ) queue[i] = new Vector<TCB>( );
    }

    private void schedulerSleep( ) {
	try {
	    Thread.sleep( timeSlice / 2 );
	} catch ( InterruptedException e ) {
	}
    }

    // A modified addThread of the original algorithm
    public TCB addThread( Thread t ) {
	TCB parentTcb = getMyTcb( ); // get my TCB and find my TID
	int pid = ( parentTcb != null ) ? parentTcb.getTid( ) : -1;
	int tid = getNewTid( ); // get a new TID
	if ( tid == -1)
	    return null;
	TCB tcb = new TCB( t, tid, pid ); // create a new TCB
	queue[0].add( tcb );
	return tcb;
    }

    // A new feature added to the original algorithm
    // Removing the TCB of a terminating thread
    public boolean deleteThread( ) {
	TCB tcb = getMyTcb( ); 
	if ( tcb!= null ) {
	    this.interrupt( );
	    return tcb.setTerminated( );
	} else
	    return false;
    }

    public void sleepThread( int milliseconds ) {
	try {
	    sleep( milliseconds );
	} catch ( InterruptedException e ) { }
    }

	/*public void run()
	----------------------------------------------------------------------------------------
	Description: This is the heart of our scheduler. The run method is what runs the scheduler. It processes
	all the thread via a multilevel feedback queue. It uses a while loop to go through all the threads in the queues
	and if there is no threads then there will be a null exception. slice[] is the work unit of the threads.
	It keep track of how much the processor have been working on the threads and helps move the threads to the
	queues accordingly. We have 3 differend queue with different quatum of time that can be allocated to each thread.
	queue 0 can allocate timeslice/2 (500ms), queue 1 can allocate timeslice (1s), and queue 2 can allocated 
	timeslice*2 (2s). 
	Pre-condition: Threads and their TCB are inputted
	Post-condition: Threads are executed in a multilevel feedback queue fashion
	 */
    // A modified run of the original algorithm
    public void run( ) {
	//initiating variables
	Thread current = null;
	TCB currentTCB = null;
	TCB prevTCB = null;
	int slice[] = new int[3];

	//all starting value of slice is 0
	for ( int i = 0; i < 3; i++ )
	    slice[i] = 0;

	while ( true ) {
	    try {
		// get the next TCB and its thread from the highest queue
		int level = 0;
		for ( ; level < 3; level++ ) {
		    if ( slice[level] == 0 ) {
			if ( queue[level].size( ) == 0 )
			    continue;
			currentTCB = queue[level].firstElement( );
			break;
		    }
		    else {
			currentTCB = prevTCB;
			break;
		    }
		}
		if ( level == 3 )
		    continue;

		//if the current thread is finished executing
		if ( currentTCB.getTerminated( ) == true ) {
		    // Remove this thread from queue[level]
			queue[level].remove(currentTCB); //*
		    // Return this thread id
		    returnTid(currentTCB.getTid()); //*
		    // slice[level] must be 0
			slice[level] = 0; //*
		    continue;
		}
		current = currentTCB.getThread( );

		if ( ( current != null ) ) {
		    // If current is alive, resume it otherwise start it.
		    // The same logic as Scheduler_rr.java
		    // Just copy the logic here. **
			if ( current.isAlive( ) ){
				current.resume( );
			}else{
				// Spawn must be controlled by Scheduler
				// Scheduler must start a new thread
				current.start( );
			}
		}

		// Scheduler should sleep here.
		schedulerSleep();

		// If current is alive, suspend it.
		// The same logic as Scheduler_rr.java
		// Just copy the logic here *
		synchronized( queue ) {
			if ( current != null && current.isAlive( ) )
				current.suspend( );
			queue[level].remove( currentTCB ); // rotate this TCB to the end
			queue[level].add( currentTCB );
		}

		prevTCB = currentTCB;
		// This is the heart of Prog2C!!!!
		// Update slice[level].
		if(level == 0){
			//returns to zero after
			slice[level] = 0;
		}else if(level == 1){
			if(slice[level] == (timeSlice/2)){
				slice[level] = timeSlice;
			} else if(slice[level] == timeSlice){
				slice[level] = 0;
			}
		} else if(level == 2){
			if(slice[level] < (2*timeSlice)){
				slice[level] = slice[level] + (timeSlice/2);
			} else if(slice[level] == (2*timeSlice)){
				slice[level] = 0;
			}
		}
		// if slice[level] returns to 0,
		//   currentThread must go to the next level or
		//   rotate back in queue[2]
		if(slice[level] == 0){
			//if we are not at queue 2 then we can go to the next level
			if(level == 2){
				queue[2].remove(currentTCB);
				queue[2].add(currentTCB);
			} else {
				queue[level].remove(currentTCB);
				level = level + 1;
				queue[level].add(currentTCB);
			}
		}
	    } catch ( NullPointerException e3 ) { };
	}
    }
	//Max slice of queue 2 is 2
}

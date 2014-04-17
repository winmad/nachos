//u`ofe`{i`nlhofgdhbi`nsthvtmhg`o
//PART OF THE NACHOS. DON'T CHANGE CODE OF THIS LINE
package nachos.threads;

import nachos.machine.*;

import java.util.Collection;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.TreeSet;
import java.util.HashSet;
import java.util.Iterator;

/**
 * A scheduler that chooses threads based on their priorities.
 *
 * <p>
 * A priority scheduler associates a priority with each thread. The next thread
 * to be dequeued is always a thread with priority no less than any other
 * waiting thread's priority. Like a round-robin scheduler, the thread that is
 * dequeued is, among all the threads of the same (highest) priority, the
 * thread that has been waiting longest.
 *
 * <p>
 * Essentially, a priority scheduler gives access in a round-robin fassion to
 * all the highest-priority threads, and ignores all other threads. This has
 * the potential to
 * starve a thread if there's always a thread waiting with higher priority.
 *
 * <p>
 * A priority scheduler must partially solve the priority inversion problem; in
 * particular, priority must be donated through locks, and through joins.
 */
public class PriorityScheduler extends Scheduler {
    /**
     * Allocate a new priority scheduler.
     */
    public PriorityScheduler() {
    }

    /**
     * Allocate a new priority thread queue.
     *
     * @param	transferPriority	<tt>true</tt> if this queue should
     *					transfer priority from waiting threads
     *					to the owning thread.
     * @return	a new priority thread queue.
     */
    public ThreadQueue newThreadQueue(boolean transferPriority) {
    	return new PriorityQueue(transferPriority);
    }

    public int getPriority(KThread thread) {
    	Lib.assertTrue(Machine.interrupt().disabled());

    	return getThreadState(thread).getPriority();
    }

    public int getEffectivePriority(KThread thread) {
    	Lib.assertTrue(Machine.interrupt().disabled());

    	return getThreadState(thread).getEffectivePriority();
    }

    public void setPriority(KThread thread, int priority) {
    	Lib.assertTrue(Machine.interrupt().disabled());

    	Lib.assertTrue(priority >= priorityMinimum &&
    			priority <= priorityMaximum);

    	getThreadState(thread).setPriority(priority);
    }

    public boolean increasePriority() {
    	boolean intStatus = Machine.interrupt().disable();

    	KThread thread = KThread.currentThread();

    	int priority = getPriority(thread);
    	if (priority == priorityMaximum)
    		return false;

    	setPriority(thread, priority+1);

    	Machine.interrupt().restore(intStatus);
    	return true;
    }

    public boolean decreasePriority() {
    	boolean intStatus = Machine.interrupt().disable();

    	KThread thread = KThread.currentThread();

    	int priority = getPriority(thread);
    	if (priority == priorityMinimum)
    		return false;

    	setPriority(thread, priority-1);

    	Machine.interrupt().restore(intStatus);
    	return true;
    }

    /**
     * The default priority for a new thread. Do not change this value.
     */
    public static final int priorityDefault = 1;
    /**
     * The minimum priority that a thread can have. Do not change this value.
     */
    public static final int priorityMinimum = 0;
    /**
     * The maximum priority that a thread can have. Do not change this value.
     */
    public static final int priorityMaximum = 7;

    /**
     * Return the scheduling state of the specified thread.
     *
     * @param	thread	the thread whose scheduling state to return.
     * @return	the scheduling state of the specified thread.
     */
    protected ThreadState getThreadState(KThread thread) {
    	if (thread.schedulingState == null)
    		thread.schedulingState = new ThreadState(thread);

    	return (ThreadState) thread.schedulingState;
    }

    /**
     * A <tt>ThreadQueue</tt> that sorts threads by priority.
     */
    protected class PriorityQueue extends ThreadQueue {
    	PriorityQueue(boolean transferPriority) {
    		this.transferPriority = transferPriority;
    		waitQueue = new LinkedList<ThreadState>();
    		resAccessing = null;
    	}

    	public void waitForAccess(KThread thread) {
    		Lib.assertTrue(Machine.interrupt().disabled());
    		getThreadState(thread).waitForAccess(this);
    	}

    	public void acquire(KThread thread) {
    		Lib.assertTrue(Machine.interrupt().disabled());
    		getThreadState(thread).acquire(this);
    	}	
    	
    	public KThread nextThread() {
    		Lib.assertTrue(Machine.interrupt().disabled());
    		
    		/** the thread gives up the resource */
    		if (resAccessing != null) {
    			this.resAccessing.resources.remove(waitQueue);
    			
    			if (!this.resAccessing.resources.isEmpty()) {
    				PriorityQueue resourceQueue = this.resAccessing.resources.keys().nextElement();
    				if (resourceQueue.transferPriority) {
    					this.resAccessing.updatePriority(resourceQueue);
    				}
    			}
    			else
    				this.resAccessing.priority = this.resAccessing.originalPriority;
    		}
    		
    		/** nextThread acquires resource */
    		ThreadState nextThreadState = pickNextThread();
    		if (nextThreadState != null) {
    			this.waitQueue.remove(nextThreadState);
    			this.acquire(nextThreadState.thread);
    			return nextThreadState.thread;
    		}
    		else
    			return null;
    	}

    	/**
    	 * Return the next thread that <tt>nextThread()</tt> would return,
    	 * without modifying the state of this queue.
    	 *
    	 * @return	the next thread that <tt>nextThread()</tt> would
    	 *		return.
    	 */
    	protected ThreadState pickNextThread() {
    		ThreadState nextThreadState = null;
    		int maxPriority = -1;
    		
    		for (int i = 0; i < waitQueue.size(); i++) {
    			ThreadState ts = waitQueue.get(i);
    			int tmpPriority = ts.getEffectivePriority();
    			if (tmpPriority > maxPriority) {
    				maxPriority = tmpPriority;
    				nextThreadState = ts;
    			}
    		}
    		return nextThreadState;
    	}

    	public void print() {
    		Lib.assertTrue(Machine.interrupt().disabled());
    		
    		Iterator it = waitQueue.iterator();
    		
    		if (it.hasNext())
    			System.out.println("===========");
    		while (it.hasNext()) {
    			ThreadState ts = (ThreadState)it.next();
    			ts.print();
    		}
    	}

    	protected int getMaxPriority() {
    		int maxPriority = -1;
    		if (transferPriority) {
    			for (int i = 0; i < waitQueue.size(); i++) {
    				ThreadState ts = waitQueue.get(i);
    				maxPriority = Math.max(maxPriority , ts.getEffectivePriority());
    			}
    		}
    		return maxPriority;
    	}
    	
    	
    	/**
    	 * <tt>true</tt> if this queue should transfer priority from waiting
    	 * threads to the owning thread.
    	 */
    	public boolean transferPriority;
    	
    	/** the thread accessing to resources */
    	protected ThreadState resAccessing;
    	
    	protected LinkedList<ThreadState> waitQueue;
    }

   	/**
   	 * The scheduling state of a thread. This should include the thread's
   	 * priority, its effective priority, any objects it owns, and the queue
   	 * it's waiting for, if any.
   	 *
   	 * @see	nachos.threads.KThread#schedulingState
   	 */
   	protected class ThreadState {
   		/**
   		 * Allocate a new <tt>ThreadState</tt> object and associate it with the
   		 * specified thread.
   		 *
   		 * @param	thread	the thread this state belongs to.
   		 */
   		public ThreadState(KThread thread) {
   			this.thread = thread;
   			resources = new Hashtable<PriorityQueue , Integer>();
   			resourceWaitQueue = null;
   			setPriority(priorityDefault);
   		}
   		
   		/**
    	 * Return the priority of the associated thread.
    	 *
    	 * @return	the priority of the associated thread.
    	 */
    	public int getPriority() {
    		return originalPriority;
    	}

    	/**
    	 * Return the effective priority of the associated thread.
    	 *
    	 * @return	the effective priority of the associated thread.
    	 */
    	public int getEffectivePriority() {
    		return priority;
    	}
    	
    	/**
    	 * Update the effective priority of the associated thread
    	 */
    	public void updatePriority(PriorityQueue resourceQueue) {
    		if (!resourceQueue.transferPriority)
    			return;
    		
    		int maxPriority = resourceQueue.getMaxPriority();
    		resources.put(resourceQueue , maxPriority);
    		
    		Collection<Integer> priorities = resources.values();
    		int res = originalPriority;
    		for (Integer p: priorities)
    			res = Math.max(res , p);
    		
    		priority = res;
    		
    		/** waiting threads would donate their priority */
    		if (this.resourceWaitQueue != null && 
    			this.resourceWaitQueue.transferPriority &&
    			this.resourceWaitQueue.resAccessing != null) {
    			this.resourceWaitQueue.resAccessing.updatePriority(this.resourceWaitQueue);
    		}
    	}

    	/**
    	 * Set the priority of the associated thread to the specified value.
    	 *
    	 * @param	priority	the new priority.
    	 */
    	public void setPriority(int priority) {
    		if (this.originalPriority == priority)
    			return;
   			this.originalPriority = priority;

   			if (resourceWaitQueue == null) {
   				this.priority = priority;
   			}
   				
   			/** receive donated priority by the waiting threads */
    		if (!resources.isEmpty()) {
    			PriorityQueue resourceQueue = resources.keys().nextElement();
    			if (resourceQueue.transferPriority)
    				updatePriority(resourceQueue);
    		}
    		
    		/** donate its own priority to the thread holding the resource */
    		if (resourceWaitQueue != null &&
    			resourceWaitQueue.transferPriority) {
    			resourceWaitQueue.resAccessing.updatePriority(resourceWaitQueue);
    		}
    	}

    	/**
    	 * Called when <tt>waitForAccess(thread)</tt> (where <tt>thread</tt> is
    	 * the associated thread) is invoked on the specified priority queue.
    	 * The associated thread is therefore waiting for access to the
    	 * resource guarded by <tt>waitQueue</tt>. This method is only called
    	 * if the associated thread cannot immediately obtain access.
    	 *
    	 * @param	waitQueue	the queue that the associated thread is
    	 *				now waiting on.
    	 *
    	 * @see	nachos.threads.ThreadQueue#waitForAccess
    	 */
    	public void waitForAccess(PriorityQueue waitQueue) {
    		/** add in waitQueue */
    		waitQueue.waitQueue.add(this);
    		this.resourceWaitQueue = waitQueue;
    		
    		/** waiting thread will donate its priority to the thread holding the resource */
    		if (waitQueue.transferPriority) {
    			this.resourceWaitQueue.resAccessing.updatePriority(waitQueue);
    		}
    	}
    	
   		/**
    	 * Called when the associated thread has acquired access to whatever is
    	 * guarded by <tt>waitQueue</tt>. This can occur either as a result of
    	 * <tt>acquire(thread)</tt> being invoked on <tt>waitQueue</tt> (where
    	 * <tt>thread</tt> is the associated thread), or as a result of
    	 * <tt>nextThread()</tt> being invoked on <tt>waitQueue</tt>.
    	 *
    	 * @see	nachos.threads.ThreadQueue#acquire
    	 * @see	nachos.threads.ThreadQueue#nextThread
    	 */
    	public void acquire(PriorityQueue waitQueue) {
    		/** acquired resources, so doesn't waiting anymore */
    		this.resourceWaitQueue = null;
    		waitQueue.resAccessing = this;
    		
    		/** would be donated priority by threads in waitQueue */
    		if (waitQueue.transferPriority) {
    			this.updatePriority(waitQueue);
    		}
    	}
    	
    	public void print() {
    		System.out.println(thread.getName() + " has priority " + priority);
    	}
    	
   		/** The thread with which this object is associated. */
    	protected KThread thread;
    	/** The priority of the associated thread. */
    	protected int priority;
    	protected int originalPriority;
    	/** All resources holding by the associated thread */
    	protected Hashtable<PriorityQueue , Integer> resources;
    	/** Waiting caching queue: all other threads waiting with the associated thread */
    	protected PriorityQueue resourceWaitQueue;
    }
}

//u`ofe`{i`nlhofgdhbi`nsthvtmhg`o
//PART OF THE NACHOS. DON'T CHANGE CODE OF THIS LINE
package nachos.threads;

import nachos.machine.*;

import java.util.Comparator;
import java.util.Hashtable;
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
    		
    		if (resAccessing != null) {
    			this.resAccessing.resources.remove(this);
    			
    			if (!this.resAccessing.resources.isEmpty()) {
    				PriorityQueue resourceQueue = this.resAccessing.resources.last();
    				if (resourceQueue.transferPriority) {
    					this.resAccessing.updatePriority(resourceQueue);
    				}
    			}
    		}
    		
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
    		if (!waitQueue.isEmpty()) {
    			nextThreadState = waitQueue.last();
    		}
    		return nextThreadState;
    	}

    	public void print() {
    		Lib.assertTrue(Machine.interrupt().disabled());
    		Iterator it = waitQueue.iterator();
    		while (it.hasNext()) {
    			ThreadState ts = (ThreadState)it.next();
    			ts.print();
    		}
    	}

    	protected int getMaxPriority() {
    		maxPriority = -1;
    		if (!this.waitQueue.isEmpty())
    			maxPriority = waitQueue.last().getEffectivePriority();
    		return maxPriority;
    	}
    	
    	/**
    	 * <tt>true</tt> if this queue should transfer priority from waiting
    	 * threads to the owning thread.
    	 */
    	public boolean transferPriority;
    	
    	/** the thread accessing to resources */
    	protected ThreadState resAccessing;
    	
    	int maxPriority;
    	
    	/** a sorted set according to a thread's priority */
    	protected TreeSet<ThreadState> waitQueue = new TreeSet<ThreadState>(new Comparator() {
    		@Override
    		public int compare(Object o1 , Object o2) {
    			ThreadState th1 = (ThreadState)o1;
    			ThreadState th2 = (ThreadState)o2;
    			int p1 = th1.getEffectivePriority();
    			int p2 = th2.getEffectivePriority();
    			long t1 = th1.addTime;
    			long t2 = th2.addTime;
    			if (p1 < p2)
    				return -1;
    			else if (p1 > p2)
    				return 1;
    			else if (t1 < t2)
    				return -1;
    			else if (t1 > t2)
    				return 1;
    			else
    				return 0;
    		}
    	});
    	
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
   			addTime = 0;
   			resources = new TreeSet<PriorityQueue>(new Comparator() {
   				@Override
   				public int compare(Object o1 , Object o2) {
   					PriorityQueue p1 = (PriorityQueue)o1;
   					PriorityQueue p2 = (PriorityQueue)o2;
   					if (p1.getMaxPriority() < p2.getMaxPriority())
   						return -1;
   					else if (p1.getMaxPriority() > p2.getMaxPriority())
   						return 1;
   					else 
   						return 0;
   				}
   			});
   			resourceWaitQueue = null;
   			setPriority(priorityDefault);
   		}
   		
   		/**
    	 * Return the priority of the associated thread.
    	 *
    	 * @return	the priority of the associated thread.
    	 */
    	public int getPriority() {
    		return priority;
    	}

    	/**
    	 * Return the effective priority of the associated thread.
    	 *
    	 * @return	the effective priority of the associated thread.
    	 */
    	public int getEffectivePriority() {
    		int res = priority;
    		if (resources != null && !resources.isEmpty())
    			res = Math.max(priority , resources.last().getMaxPriority());
    		return res;
    	}
    	
    	/**
    	 * Update the effective priority of the associated thread
    	 */
    	public void updatePriority(PriorityQueue resourceQueue) {
    		if (!resourceQueue.transferPriority)
    			return;
    		
    		if (!resources.contains(resourceQueue))
    			resources.add(resourceQueue);
    		
    		int res = priority;
    		res = Math.max(res , resources.last().getMaxPriority());
    		
    		priority = res;
    		
    		if (this.resourceWaitQueue != null &&
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
    		if (this.priority == priority)
    			return;
   			this.priority = priority;

   			if (resourceWaitQueue == null)
   				return;
   			
    		if (!resources.isEmpty()) {
    			PriorityQueue resourceQueue = resources.last();
    			updatePriority(resourceQueue);
    		}
    		
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
    		this.addTime = Machine.timer().getTime();
    		this.resourceWaitQueue = waitQueue;
    		this.resourceWaitQueue.waitQueue.add(this);
    		
    		if (waitQueue.transferPriority) {
    			this.resourceWaitQueue.resAccessing.updatePriority(
    					this.resourceWaitQueue);
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
    		this.resourceWaitQueue = null;
    		waitQueue.resAccessing = this;
    		
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
    	/** The time when the thread is added in queue */
    	protected long addTime;
    	/** All resources holding by the associated thread */
    	protected TreeSet<PriorityQueue> resources;
    	/** Waiting caching queue: all other threads waiting with the associated thread */
    	protected PriorityQueue resourceWaitQueue;
    }
}

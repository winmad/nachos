//u`ofe`{i`nlhofgdhbi`nsthvtmhg`o
//PART OF THE NACHOS. DON'T CHANGE CODE OF THIS LINE
package nachos.threads;

import nachos.machine.*;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.Random;
import java.util.Iterator;

/**
 * A scheduler that chooses threads using a lottery.
 *
 * <p>
 * A lottery scheduler associates a number of tickets with each thread. When a
 * thread needs to be dequeued, a random lottery is held, among all the tickets
 * of all the threads waiting to be dequeued. The thread that holds the winning
 * ticket is chosen.
 *
 * <p>
 * Note that a lottery scheduler must be able to handle a lot of tickets
 * (sometimes billions), so it is not acceptable to maintain state for every
 * ticket.
 *
 * <p>
 * A lottery scheduler must partially solve the priority inversion problem; in
 * particular, tickets must be transferred through locks, and through joins.
 * Unlike a priority scheduler, these tickets add (as opposed to just taking
 * the maximum).
 */
public class LotteryScheduler extends PriorityScheduler {
    /**
     * Allocate a new lottery scheduler.
     */
    public LotteryScheduler() {
    	super();
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
    	return new LotteryQueue(transferPriority);
    }
    
    public int getPriority(KThread thread) {
    	Lib.assertTrue(Machine.interrupt().disabled());

    	return getLotteryThreadState(thread).getPriority();
    }

    public int getEffectivePriority(KThread thread) {
    	Lib.assertTrue(Machine.interrupt().disabled());

    	return getLotteryThreadState(thread).getEffectivePriority();
    }

    public void setPriority(KThread thread, int priority) {
    	Lib.assertTrue(Machine.interrupt().disabled());

    	Lib.assertTrue(priority >= priorityMinimum &&
    			priority <= priorityMaximum);

    	getLotteryThreadState(thread).setPriority(priority);
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

    public static final int priorityDefault = 1;
    public static final int priorityMinimum = 1;
    public static final int priorityMaximum = Integer.MAX_VALUE;
	
	/**
     * Return the scheduling state of the specified thread.
     *
     * @param	thread	the thread whose scheduling state to return.
     * @return	the scheduling state of the specified thread.
     */
    protected LotteryThreadState getLotteryThreadState(KThread thread) {
    	if (thread.schedulingState == null)
    		thread.schedulingState = new LotteryThreadState(thread);

    	return (LotteryThreadState) thread.schedulingState;
    }
   
    protected class LotteryQueue extends ThreadQueue {
    	LotteryQueue(boolean transferPriority) {
    		super();
    		this.transferPriority = transferPriority;
    		waitQueue = new LinkedList<LotteryThreadState>();
    		resAccessing = null;
    		counter = 0;
    		donation = 0;
    	}
    	
		public void waitForAccess(KThread thread) {
    		Lib.assertTrue(Machine.interrupt().disabled());
    		getLotteryThreadState(thread).waitForAccess(this , counter++);
    	}

    	public void acquire(KThread thread) {
    		Lib.assertTrue(Machine.interrupt().disabled());
    		//if (!transferPriority)
    		//	return;
    		getLotteryThreadState(thread).acquire(this);
    		resAccessing = getLotteryThreadState(thread);
    	}	
    	
    	public KThread nextThread() {
    		Lib.assertTrue(Machine.interrupt().disabled());
    		
    		/** nextThread acquires resource */
    		LotteryThreadState nextThreadState = pickNextThread();
    		if (resAccessing != null) {
    			resAccessing.release(this);
    			resAccessing = null;
    		}
    		
    		if (nextThreadState == null) 
    			return null;
    		
    		waitQueue.remove(nextThreadState);
    		nextThreadState.ready();
    		
    		donatePriority();
    		acquire(nextThreadState.thread);
    		
    		return nextThreadState.thread;
    	}

    	protected int getMaxPriority() {
    		int totTickets = 0;
    		if (transferPriority) {
    			for (int i = 0; i < waitQueue.size(); i++) {
    				LotteryThreadState ts = waitQueue.get(i);
    				totTickets += ts.getEffectivePriority();
    			}
    		}
    		return totTickets;
    	}
    	
    	protected LotteryThreadState pickNextThread() {
    		if (waitQueue.isEmpty())
    			return null;
    		int[] tickets = new int[waitQueue.size()];
    		int totTickets = 0;
    		for (int i = 0; i < waitQueue.size(); i++) {
    			int tmp = waitQueue.get(i).getEffectivePriority();
    			totTickets += tmp;
    			tickets[i] = totTickets;
    		}
    		int winTicket = rand.nextInt(totTickets);
    		for (int i = 0; i < waitQueue.size(); i++) {
    			if (winTicket < tickets[i])
    				return waitQueue.get(i);
    		}
    		return null;
    	}
    	
    	protected void donatePriority() {
    		donation = 0;
    		if (transferPriority)
    			donation = getMaxPriority();
    		
    		if (this.resAccessing != null) {
    			this.resAccessing.resources.put(this , donation);
    			this.resAccessing.updatePriority();
    		}
    	}
    	
    	protected void removeFromWaitQueue(LotteryThreadState ts) {
    		Lib.assertTrue(waitQueue.remove(ts));
    	}
    	
    	protected void addToWaitQueue(LotteryThreadState ts) {
    		waitQueue.add(ts);
    		donatePriority();
    	}
    	
    	public void print() {
    		Lib.assertTrue(Machine.interrupt().disabled());
    		
    		Iterator it = waitQueue.iterator();
    		
    		if (it.hasNext())
    			System.out.println("===========");
    		while (it.hasNext()) {
    			LotteryThreadState ts = (LotteryThreadState)it.next();
    			ts.print();
    		}
    	}
		
		/**
    	 * <tt>true</tt> if this queue should transfer priority from waiting
    	 * threads to the owning thread.
    	 */
    	public boolean transferPriority;
    	
    	/** the thread accessing to resources */
    	protected LotteryThreadState resAccessing = null;
    	
    	protected LinkedList<LotteryThreadState> waitQueue = new LinkedList<LotteryThreadState>();
    	
    	protected int counter;
    	
    	protected int donation;
    }
    
    protected class LotteryThreadState {
    	public LotteryThreadState(KThread thread) {	
    		this.thread = thread;
    		resources = new Hashtable<LotteryQueue , Integer>();
    		resourceWaitQueue = null;
    		setPriority(priorityDefault);
    	}
		
		/**
    	 * Return the priority of the associated thread.
    	 *
    	 * @return	the priority of the associated thread.
    	 */
    	public int getPriority() {
    		Lib.assertTrue(originalPriority >= priorityMinimum && originalPriority <= priorityMaximum);
    		return originalPriority;
    	}

    	/**
    	 * Return the effective priority of the associated thread.
    	 *
    	 * @return	the effective priority of the associated thread.
    	 */
    	public int getEffectivePriority() {
    		Lib.assertTrue(priority >= originalPriority);
    		Lib.assertTrue(priority >= priorityMinimum && priority <= priorityMaximum);
    		return priority;
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
   				
   			updatePriority();
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
    	public void waitForAccess(LotteryQueue waitQueue , int time) {
    		/** add in waitQueue */
    		Lib.assertTrue(!waitQueue.waitQueue.contains(this));
    		Lib.assertTrue(this.resourceWaitQueue == null);

    		this.addTime = time;
    		this.resourceWaitQueue = waitQueue;
    		
    		waitQueue.addToWaitQueue(this);
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
    	public void acquire(LotteryQueue waitQueue) {	
    		resources.put(waitQueue , waitQueue.donation);
    		this.updatePriority();
    	}
    	
    	public void release(LotteryQueue waitQueue) {
    		resources.remove(waitQueue);
    		this.updatePriority();
    	}
    	
    	public void ready() {
    		Lib.assertTrue(this.resourceWaitQueue != null);
    		this.resourceWaitQueue = null;
    	}
    	
		protected void removeFromResources(LotteryQueue waitQueue) {
    		Lib.assertTrue(resources.remove(waitQueue) != null);
    	}
    	
    	protected void addToResources(LotteryQueue waitQueue) {
    		resources.put(waitQueue , waitQueue.donation);
    		updatePriority();
    	}
		
    	protected void updatePriority() {
    		int newEffectivePriority = originalPriority;
    		
    		if (!resources.isEmpty()) {
    			for (Enumeration<LotteryQueue> queues = resources.keys(); 
    					queues.hasMoreElements(); ) {
    				LotteryQueue q = queues.nextElement();
    				newEffectivePriority += q.donation;
    			}
    		}
    		
    		if (newEffectivePriority == priority)
    			return;
    		
    		priority = newEffectivePriority;
    		if (resourceWaitQueue != null)
    			resourceWaitQueue.donatePriority();
    	}
		
    	public void print() {
    		System.out.println(thread.getName() + " has priority " + priority);
    	}
    	
		/** The thread with which this object is associated. */
    	protected KThread thread;
    	/** The priority of the associated thread. */
    	protected int priority;
    	protected int originalPriority;
    	protected int addTime;
    	/** All resources holding by the associated thread */
    	protected Hashtable<LotteryQueue , Integer> resources = new Hashtable<LotteryQueue , Integer>();
    	/** Waiting caching queue: all other threads waiting with the associated thread */
    	protected LotteryQueue resourceWaitQueue = null;
    }
    
    protected static Random rand = new Random(19931004); 
}

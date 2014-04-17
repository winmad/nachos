//u`ofe`{i`nlhofgdhbi`nsthvtmhg`o
//PART OF THE NACHOS. DON'T CHANGE CODE OF THIS LINE
package nachos.threads;

import nachos.machine.*;

import java.util.Collection;
import java.util.Random;
import java.util.TreeSet;
import java.util.HashSet;
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

    public static final int priorityDefault = 1;
    public static final int priorityMinimum = 1;
    public static final int priorityMaximum = Integer.MAX_VALUE;
    
    /**
     * Allocate a new lottery thread queue.
     *
     * @param	transferPriority	<tt>true</tt> if this queue should
     *					transfer tickets from waiting threads
     *					to the owning thread.
     * @return	a new lottery thread queue.
     */
    public ThreadQueue newThreadQueue(boolean transferPriority) {
    	return new LotteryQueue(transferPriority);
    }
    
    protected class LotteryQueue extends PriorityScheduler.PriorityQueue {
    	LotteryQueue(boolean transferPriority) {
    		super(transferPriority);
    	}
    	
    	@Override
    	protected int getMaxPriority() {
    		int totTickets = 0;
    		if (transferPriority) {
    			for (int i = 0; i < waitQueue.size(); i++) {
    				ThreadState ts = waitQueue.get(i);
    				totTickets += ts.getEffectivePriority();
    			}
    		}
    		return totTickets;
    	}
    	
    	@Override
    	protected ThreadState pickNextThread() {
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
    }
    
    protected class LotteryThreadState extends PriorityScheduler.ThreadState {
    	public LotteryThreadState(KThread thread) {
    		super(thread);
    	}
    	
    	@Override
    	public void updatePriority(PriorityQueue resourceQueue) {
    		if (!resourceQueue.transferPriority)
    			return;
    		
    		int totTickets = resourceQueue.getMaxPriority();
    		resources.put(resourceQueue , totTickets);
    		
    		Collection<Integer> tickets = resources.values();
    		int res = originalPriority;
    		for (Integer p: tickets)
    			res += p;
    		
    		priority = res;
    		
    		/** waiting threads would donate their priority */
    		if (this.resourceWaitQueue != null && 
    			this.resourceWaitQueue.transferPriority &&
    			this.resourceWaitQueue.resAccessing != null) {
    			((LotteryThreadState)this.resourceWaitQueue.resAccessing).updatePriority(this.resourceWaitQueue);
    		}
    	}
    }
    
    protected static Random rand = new Random(19931004);
}

//u`ofe`{i`nlhofgdhbi`nsthvtmhg`o
//PART OF THE NACHOS. DON'T CHANGE CODE OF THIS LINE
package nachos.threads.threadsTest;
import java.util.LinkedList;

import nachos.threads.*;

// Test the condition variable
// We test the condition variable to implement the Producer-Consumer model. 
// We have 10 consumers and 10 producers. The flow of the middle bottleneck is 3.
// The way we implement this model by condition variables is the same as was taught in class.
// The outputs will imply whether the implementation is valid.


public class TestCondition2 {
	public void testCondition2() {
		System.out.println("=== test task 2: condition2 ===");
		
		//two linked lists, represent producers and consumers, respectively.
		LinkedList<KThread> producers = new LinkedList<KThread>();
		LinkedList<KThread> consumers = new LinkedList<KThread>();
				
		//fork producers
		for (int i = 0; i < producerNum; i++) {
			producers.add(new KThread(new Producer()).setName("Produer #" + i));
			producers.get(i).fork();
		}
		
		//fork consumers
		for (int i = 0; i < consumerNum; i++) {
			consumers.add(new KThread(new Consumer()).setName("Consumer #" + i));
			consumers.get(i).fork();
		}
			
		for (int i = 0; i < producerNum; i++) {
			producers.get(i).join();
		}
		for (int i = 0; i < consumerNum; i++) {
			consumers.get(i).join();
		}
	}
	    	
	private class Producer implements Runnable {
		@Override
		public void run() {
			String name = KThread.currentThread().getName();
			
			lock.acquire();
			
			while (counter >= maxCount) {
				System.out.println(name + " is waiting... counter = " + counter);
				cond.sleep();
			}
			counter++;
			System.out.println(name + " adds one! counter = " + counter);
			
			cond.wake();
			
			lock.release();
		}
	}
	
	private class Consumer implements Runnable {
		@Override
		public void run() {
			String name = KThread.currentThread().getName();
			
			lock.acquire();
			
			while (counter <= 0) {
				System.out.println(name + " is waiting... counter = " + counter);
				cond.sleep();
			}
			counter--;
			System.out.println(name + " uses one! counter = " + counter);
			
			cond.wakeAll();
			
			lock.release();
		}
	}
	
	private Lock lock = new Lock();
	private Condition2 cond = new Condition2(lock);
	private int counter = 0;
	
	private static final int maxCount = 3;
	private static final int producerNum = 10;
	private static final int consumerNum = 10;
}
package nachos.test.unittest;

import nachos.threads.*;


// Testing the join() method. 
// We design 3 threads A, B, C, which output 'a', 'b', 'c' for 10 times each respectively, to perform join() method.
// A, B, C will be forked sequentially, and A, C will call b.join(), such that they will wait B to finish first.
// Also notice that this test case also tests the performance if we call join() more than once.
// The expected outputs should be first 10 b's, then following with 10 a's and 10 c's with an arbitary order.



public class TestKThread {
	public void testJoin() {
		StringBuffer str = new StringBuffer("");
		
		//Create the threads
		printB = new KThread(new PrintChar(str , 'b', KThread.currentThread())).setName("forked thread");
			
		KThread printA = new KThread(new PrintChar(str , 'a', printB)).setName("waiting thread1");
		KThread printC = new KThread(new PrintChar(str , 'c', printB)).setName("waiting thread2");


		//fork the threads
		printA.fork();
		printB.fork();
		printC.fork();
		
		printA.join();
		printB.join();
		printC.join();
		
		System.out.println("=== test task 1: join ===");
		System.out.println(str);
	}
	
	KThread printB;
	
	private class PrintChar implements Runnable {
		PrintChar(StringBuffer str , char ch, KThread pengding_thread) {
			this.str = str;
			this.ch = ch;
			this.pending_thread = pending_thread;
		}
		
		@Override
		public void run() {
			if(ch == 'a' || ch == 'c') {
				//System.out.println(ch);
				//pending_thread.join();
				printB.join();	//A and C call B.join()
			}
			for (int i = 0; i < 10; i++) {
				str.append(ch);
				KThread.yield();
			}
			if(str.charAt(0) == 'a') {
				printB.join();
			}
		}
		
		private StringBuffer str;
		private char ch;
		private KThread pending_thread;
	}
	
	private StringBuffer str = new StringBuffer("");
}

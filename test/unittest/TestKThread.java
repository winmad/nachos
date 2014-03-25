package nachos.test.unittest;

import nachos.threads.*;


public class TestKThread {
	public void testJoin() {
		StringBuffer str = new StringBuffer("");
		printB = new KThread(new PrintChar(str , 'b', KThread.currentThread())).setName("forked thread");
			
		KThread printA = new KThread(new PrintChar(str , 'a', printB)).setName("waiting thread1");
		KThread printC = new KThread(new PrintChar(str , 'c', printB)).setName("waiting thread2");


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
				printB.join();
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

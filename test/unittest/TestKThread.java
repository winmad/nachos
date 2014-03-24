package nachos.test.unittest;

import nachos.threads.*;


public class TestKThread {
	public void testJoin() {
		KThread printA = new KThread(new PrintChar(str , 'A')).setName("forked thread");
		printA.fork();
		printA.join();
		new PrintChar(str , 'B').run();
		System.out.println("=== test task 1: join ===");
		System.out.println(str);
	}
	
	private class PrintChar implements Runnable {
		PrintChar(StringBuffer str , char ch) {
			this.str = str;
			this.ch = ch;
		}
		
		@Override
		public void run() {
			for (int i = 0; i < 5; i++) {
				str.append(ch);
				KThread.yield();
			}
		}
		
		private StringBuffer str;
		private char ch;
	}
	
	private StringBuffer str = new StringBuffer("");
}

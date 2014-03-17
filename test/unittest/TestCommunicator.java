package nachos.test.unittest;

import java.util.LinkedList;

import nachos.threads.*;


public class TestCommunicator {
	public void testCommunicator() {
		System.out.println("=== test task 4: communicator ===");
		LinkedList<KThread> listeners = new LinkedList<KThread>();
		LinkedList<KThread> speakers = new LinkedList<KThread>();
		for (int i = 0; i < 5; i++) {
			listeners.add(new KThread(new Listener("Listener #" + i)));
			speakers.add(new KThread(new Speaker("Speaker #" + i , 1000 + i)));
		}
		
		for (int i = 0; i < 5; i++) {
			speakers.get(i).fork();
		}
		for (int i = 0; i < 5; i++) {
			listeners.get(i).fork();
		}
		
		for (int i = 0; i < 5; i++) {
			listeners.get(i).join();
			speakers.get(i).join();
		}
	}
	
	private class Listener implements Runnable {
		Listener(String name) {
			this.name = name;
		}
		
		@Override
		public void run() {
			int word = commu.listen();
			System.out.println(name + " listened " + word);
		}
		
		private String name;
	}
	
	private class Speaker implements Runnable {
		Speaker(String name , int word) {
			this.name = name;
			this.word = word;
		}
		
		@Override
		public void run() {
			System.out.println(name + " spoke " + this.word);
			commu.speak(this.word);
		}
		
		private String name;
		private int word;
	}
	
	private Communicator commu = new Communicator();
}

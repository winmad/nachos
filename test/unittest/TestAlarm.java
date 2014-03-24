package nachos.test.unittest;
import nachos.machine.Machine;
import nachos.threads.*;


public class TestAlarm {
	public void testAlarm() {
		System.out.println("=== test task 3: alarm ===");
		KThread t1 = new KThread(new SetAlarm(5000)).setName("t1");
		KThread t2 = new KThread(new SetAlarm(1000)).setName("t2");
		KThread t3 = new KThread(new SetAlarm(10000)).setName("t3");
		KThread t4 = new KThread(new SetAlarm(2000)).setName("t4");
		t1.fork();
		t2.fork();
		t3.fork();
		t4.fork();
		
		t1.join();
		t2.join();
		t3.join();
		t4.join();
	}

	private class SetAlarm implements Runnable {
		SetAlarm(long x) {
			this.x = x;
		}
		@Override
		public void run() {
			String name = KThread.currentThread().getName();
			System.out.println(name + " will start in " + x + " after " + Machine.timer().getTime());
			alarm.waitUntil(x);
			System.out.println(name + " started at " + Machine.timer().getTime());
		}
		
		private long x;
		
	}
	
	Alarm alarm = new Alarm();
}

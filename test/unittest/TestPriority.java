package nachos.test.unittest;
import nachos.machine.Machine;


import nachos.threads.*;


public class TestPriority {

	public static long tic;
	
	public static void selfTestRun( KThread t1, int t1p, KThread t2, int t2p, KThread t3, int t3p ) {

		boolean int_state;

		int_state = Machine.interrupt().disable();
		ThreadedKernel.scheduler.setPriority( t1, t1p );
		ThreadedKernel.scheduler.setPriority( t2, t2p );
		ThreadedKernel.scheduler.setPriority( t3, t3p );
		Machine.interrupt().restore( int_state );

		t1.setName("a").fork();
		t2.setName("b").fork();
		t3.setName("c").fork();
		t1.join();
		t2.join();
		t3.join();

	}
	
	public static void selfTestRun( KThread t1, int t1p, KThread t2, int t2p) {

		boolean int_state;

		int_state = Machine.interrupt().disable();
		ThreadedKernel.scheduler.setPriority( t1, t1p );
		ThreadedKernel.scheduler.setPriority( t2, t2p );
		Machine.interrupt().restore( int_state );

		t1.setName("a").fork();
		t2.setName("b").fork();
		t1.join();
		t2.join();

	}
	
	public static void selfTestRun(KThread[] t, int N) {
		for(int i = 0; i < N; i++) {
		int k = (int)Math.floor(Math.random() * 8);
		if(k >= 8 || k < 0)
			k = (int)Math.floor(Math.random() * 8);
		boolean int_state;

		int_state = Machine.interrupt().disable();
		ThreadedKernel.scheduler.setPriority( t[i], k );
		Machine.interrupt().restore( int_state );
		
		}
		for (int i = 0; i < N; i++)
		t[i].setName(i + "").fork();
		tic = Machine.timer().getTime();
		for (int i = N - 1; i >= 0; i--)
		t[i].join();
	}

	/**
	 * Tests whether this module is working.
	 */
	public static class Start_Flag {
		
			public static int flag;
			public Start_Flag() {
				flag = 0;
			}
			
	}
	
	public static void selfTest() {

		KThread t1, t2, t3;
		final Lock lock, lock2;
		final Condition2 condition, condition2;

		/*
		 * Case 1: Tests priority scheduler without donation
		 *
		 * This runs t1 with priority 7, and t2 with priority 4.
		 *
		 */

		System.out.println( "Case 1:" );

		t1 = new KThread(new Runnable() {
				public void run() {
					System.out.println( KThread.currentThread().getName() + " started working" );
					for( int i = 0; i < 10; ++i ) {
						System.out.println( KThread.currentThread().getName() + " working " + i );	
						KThread.yield();
					}
					System.out.println( KThread.currentThread().getName() + " finished working" );
				}
			});

		t2 = new KThread(new Runnable() {
				public void run() {
					System.out.println( KThread.currentThread().getName() + " started working" );
					for( int i = 0; i < 10; ++i ) {
						System.out.println( KThread.currentThread().getName() + " working " + i );	
						KThread.yield();
					}
					System.out.println( KThread.currentThread().getName() + " finished working" );
				}

			});

		selfTestRun( t1, 7, t2, 4 );

		/*
		 * Case 2: Tests priority scheduler without donation, altering
		 * priorities of threads after they've started running
		 *
		 * This runs t1 with priority 7, and t2 with priority 4, but
		 * half-way through t1's process its priority is lowered to 2.
		 *
		 */

		System.out.println( "Case 2:" );

		t1 = new KThread(new Runnable() {
				public void run() {
					System.out.println( KThread.currentThread().getName() + " started working" );
					for( int i = 0; i < 10; ++i ) {
						System.out.println( KThread.currentThread().getName() + " working " + i );	
						KThread.yield();
						if( i == 4 ) {
							System.out.println( KThread.currentThread().getName() + " reached 1/2 way, changing priority" );
							boolean int_state = Machine.interrupt().disable();
							ThreadedKernel.scheduler.setPriority( 2 );
							Machine.interrupt().restore( int_state );
						}
					}
					System.out.println( KThread.currentThread().getName() + " finished working" );
				}
			});

		t2 = new KThread(new Runnable() {
				public void run() {
					System.out.println( KThread.currentThread().getName() + " started working" );
					for( int i = 0; i < 10; ++i ) {
						System.out.println( KThread.currentThread().getName() + " working " + i );	
						KThread.yield();
					}
					System.out.println( KThread.currentThread().getName() + " finished working" );
				}

			});

		selfTestRun( t1, 7, t2, 4 );

		/*
		 * Case 3: Tests priority donation
		 *
		 * This runs t1 with priority 7, t2 with priority 6 and t3 with
		 * priority 4. t1 will wait on a lock, and while t2 would normally
		 * then steal all available CPU, priority donation will ensure that
		 * t3 is given control in order to help unlock t1.
		 *
		 */

		System.out.println( "Case 3:" );

		lock = new Lock();
		condition = new Condition2( lock );

		t1 = new KThread(new Runnable() {
			public void run() {
				lock.acquire();
				System.out.println( KThread.currentThread().getName() + " active" );
				lock.release();
			}
		});

		t2 = new KThread(new Runnable() {
				public void run() {
					System.out.println( KThread.currentThread().getName() + " started working" );
					for( int i = 0; i < 3; ++i ) {
						System.out.println( KThread.currentThread().getName() + " working " + i );	
						KThread.yield();
					}
					System.out.println( KThread.currentThread().getName() + " finished working" );
				}

			});

		t3 = new KThread(new Runnable() {
			public void run() {
				lock.acquire();

				boolean int_state = Machine.interrupt().disable();
				ThreadedKernel.scheduler.setPriority( 2 );
				Machine.interrupt().restore( int_state );

				KThread.yield();
				Scheduler temp = new PriorityScheduler();
				boolean intStatus = Machine.interrupt().disable();
				System.out.println("Effective Priority of " + KThread.currentThread().getName() + ": " + temp.getEffectivePriority());

		    	Machine.interrupt().restore(intStatus);

				// t1.acquire() will now have to release that t3 owns the lock it wants to obtain
				// so program execution will continue here.

				System.out.println( KThread.currentThread().getName() + " active ('a' wants its lock back so we are here)" );
				lock.release();
				KThread.yield();
				lock.acquire();
				System.out.println( KThread.currentThread().getName() + " active-again (should be after 'a' and 'b' done)" );
				lock.release();

			}
		});

		selfTestRun( t1, 4, t2, 5, t3, 7 );


		System.out.println( "Case 4:" );
		
		

		lock2 = new Lock();
		condition2 = new Condition2( lock2 );

		t1 = new KThread(new Runnable() {
			public void run() {
				lock2.acquire();
				System.out.println( KThread.currentThread().getName() + " active" );
				lock2.release();
			}
		});

		t2 = new KThread(new Runnable() {
				public void run() {
					System.out.println( KThread.currentThread().getName() + " started working" );
					for( int i = 0; i < 3; ++i ) {
						System.out.println( KThread.currentThread().getName() + " working " + i );	
						KThread.yield();
					}
					System.out.println( KThread.currentThread().getName() + " finished working" );
				}

			});

		t3 = new KThread(new Runnable() {
			public void run() {
				lock2.acquire();

				boolean int_state = Machine.interrupt().disable();
				ThreadedKernel.scheduler.setPriority( 2 );
				Scheduler temp = new PriorityScheduler();
				Machine.interrupt().restore( int_state );

				KThread.yield();
				boolean intStatus = Machine.interrupt().disable();
				System.out.println("Effective Priority of " + KThread.currentThread().getName() + ": " + temp.getEffectivePriority());

		    	Machine.interrupt().restore(intStatus);

				// t1.acquire() will now have to release that t3 owns the lock it wants to obtain
				// so program execution will continue here.

				System.out.println( KThread.currentThread().getName() + " active ('a' wants its lock back so we are here)" );
				lock2.release();
				KThread.yield();
				lock2.acquire();
				System.out.println( KThread.currentThread().getName() + " active-again (should be after 'a' done, but before 'b' done)" );
				lock2.release();

			}
		});

		selfTestRun( t1, 6, t2, 4, t3, 7 );

		
		System.out.println("Case 5: ");
		
		final int N = 100;
		
		
		KThread [] thread_pool = new KThread[N];
		final Alarm alarm = new Alarm();
		for(int i = 0; i < N; i++) {
			thread_pool[i] = new KThread(new Runnable() {
				public void run() {
					while(true){
						//alarm.waitUntil(N / 2);
						//System.out.println( KThread.currentThread().getName() + " active" );
						break;
					}
						
				}
			});
		}
		
		selfTestRun(thread_pool, N);


		long toc = Machine.timer().getTime();
		System.out.println("Elapsed time: " + (toc - tic) + ".");
	}
}

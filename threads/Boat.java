//u`ofe`{i`nlhofgdhbi`nsthvtmhg`o
//PART OF THE NACHOS. DON'T CHANGE CODE OF THIS LINE
package nachos.threads;
import java.util.LinkedList;

import nachos.ag.BoatGrader;

public class Boat
{
    static BoatGrader bg;

    public static void selfTest()
    {
    	BoatGrader b = new BoatGrader();

    	System.out.println("\n ***Testing Boats with only 2 children***");
    	begin(0, 2, b);

    	System.out.println("\n ***Testing Boats with 2 children, 1 adult***");
    	begin(1, 2, b);

    	System.out.println("\n ***Testing Boats with 3 children, 3 adults***");
    	begin(3, 3, b);
    	
    	//System.out.println("\n ***Testing Boats with 10 children, 10 adults***");
    	//begin(10 , 10 , b);
    }

    public static void begin( int adults, int children, BoatGrader b )
    {
    	// Store the externally generated autograder in a class
    	// variable to be accessible by children.
    	bg = b;

    	// Instantiate global variables here
    	childrenSt = children;
        childrenEd = 0;
        adultsSt = adults;
        adultsEd = 0;
        childrenStWaitBoat = 0;
        childrenOnBoat = 0;
        boatAtSt = true;
    	
    	// Create threads here. See section 3.4 of the Nachos for Java
    	// Walkthrough linked from the projects page.
        LinkedList<KThread> thChildren = new LinkedList<KThread>();
        LinkedList<KThread> thAdults = new LinkedList<KThread>();
        
        for (int i = 0; i < children; i++) {
        	KThread t = new KThread(new Runnable() {
        		@Override
        		public void run() {
        			ChildItinerary();
        		}
        	});
        	t.setName("child #" + i).fork();
        	thChildren.add(t);
        }
        
        for (int i = 0; i < adults; i++) {
        	KThread t = new KThread(new Runnable() {
        		@Override
        		public void run() {
        			AdultItinerary();
        		}
        	});
        	t.setName("adult #" + i).fork();
        	thAdults.add(t);
        }
    	
        doneLock.acquire();
        done.sleep();
        doneLock.release();
    }

    static void AdultItinerary()
    {
    	bg.initializeAdult(); //Required for autograder interface. Must be the first thing called.
    	//DO NOT PUT ANYTHING ABOVE THIS LINE.

    	/* This is where you should put your solutions. Make calls
	   	to the BoatGrader to show that it is synchronized. For
	   	example:
	    	bg.AdultRowToMolokai();
	   	indicates that an adult has rowed the boat across to Molokai
    	 */
    	islandSt.acquire();
    	
    	while (childrenSt >= 2) {
    		adultWaitAtSt.sleep();
    	}
    	
    	adultsSt--;
    	bg.AdultRowToMolokai();
    	boatAtSt = false;
    	
    	islandSt.release();
    	
    	islandEd.acquire();
    	
    	adultsEd++;
    	
    	childRestAtEd.wake();
    	
    	islandEd.release();
    }

    static void ChildItinerary()
    {
    	bg.initializeChild(); //Required for autograder interface. Must be the first thing called.
    	//DO NOT PUT ANYTHING ABOVE THIS LINE.
    	
    	while (childrenSt + adultsSt >= 2) {
    		islandSt.acquire();
    		
    		if (childrenSt == 1)
    			adultWaitAtSt.wake();
    		
    		while (childrenStWaitBoat >= 2 || !boatAtSt) {
    			childWaitForBoat.sleep();
    		}
    		
    		if (childrenStWaitBoat == 0) {
    			childrenStWaitBoat++;
    			childWaitForBoat.wake();
    			childWaitForAnother.sleep(); // ?
    			
    			bg.ChildRowToMolokai();
    			
    			childWaitForAnother.wake();
    		}
    		else {
    			childrenStWaitBoat++;
    			childWaitForAnother.wake();
    			
    			bg.ChildRideToMolokai();
    			
    			childWaitForAnother.sleep();
    		}
    		
    		childrenSt--;
    		childrenStWaitBoat--;
    		boatAtSt = false;
    		
    		islandSt.release();
    		
    		islandEd.acquire();
    		
    		childrenEd++;
    		childrenOnBoat++;
    		
    		if (childrenOnBoat == 1) 
    			childRestAtEd.sleep();
    		
    		childrenEd--;
    		childrenOnBoat = 0;
    		
    		bg.ChildRowToOahu();
    		
    		islandEd.release();
    		
    		islandSt.acquire();
    		
    		childrenSt++;
    		boatAtSt = true;
    		
    		islandSt.release();
    	}
    	
    	islandSt.acquire();
    	childrenSt--;
    	bg.ChildRowToMolokai();
    	islandSt.release();
    	
    	islandEd.acquire();
    	childrenSt++;
    	islandEd.release();
    	
    	doneLock.acquire();
    	done.wake();
    	doneLock.release();
    }

    static void SampleItinerary()
    {
    	// Please note that this isn't a valid solution (you can't fit
    	// all of them on the boat). Please also note that you may not
    	// have a single thread calculate a solution and then just play
    	// it back at the autograder -- you will be caught.
    	System.out.println("\n ***Everyone piles on the boat and goes to Molokai***");
    	bg.ChildRideToMolokai();
    	bg.ChildRowToMolokai();
    }
    
    private static int childrenSt;
    private static int childrenEd;
    private static int adultsSt;
    private static int adultsEd;
    private static int childrenStWaitBoat;
    private static int childrenOnBoat;
    
    private static boolean boatAtSt;
    private static Lock islandSt = new Lock();
    private static Lock islandEd = new Lock();
    private static Lock doneLock = new Lock();
    private static Condition adultWaitAtSt = new Condition(islandSt);
    private static Condition childWaitForBoat = new Condition(islandSt);
    private static Condition childWaitForAnother = new Condition(islandSt);
    private static Condition childRestAtEd = new Condition(islandEd);
    private static Condition done = new Condition(doneLock);
}

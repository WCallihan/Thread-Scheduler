import java.util.concurrent.*;

public class Restaurant {

	//all Semaphores initialized as fair to enforce First Come First Serve
	static Semaphore restaurantDoors[] = {new Semaphore(1, true), new Semaphore(1, true)}; //starts available
	static Semaphore tablesAvailable[] = {new Semaphore(4, true), new Semaphore(4, true), new Semaphore(4, true)}; //starts with 4 available
	static Semaphore waiterAvailable[] = {new Semaphore(0, true), new Semaphore(0, true), new Semaphore(0, true)}; //starts unavailable
	static Semaphore orderRequests[] = {new Semaphore(0, true), new Semaphore(0, true), new Semaphore(0, true)};
	static Semaphore orderCompleted[] = {new Semaphore(0, true), new Semaphore(0, true), new Semaphore(0, true)};
	static Semaphore kitchenAvailable = new Semaphore(1, true);
	static Semaphore payingAvailable = new Semaphore(1, true);
	
	static int[] tableLineCounts = new int[] {0, 0, 0};
	static int customersInRestaurant = 0; //keeps track of how many customers there are in the restaurant
	static int[] currentOrders = new int[3]; //used to pass the "orders" between the customers and waiters
	
	static public class Customer extends Thread {
		int id;
		int firstChoice;
		int secondChoice;
		Customer(int i) {
			id = i;
			firstChoice = ThreadLocalRandom.current().nextInt(0, 3); //picks random table out of 1-3
			secondChoice = ThreadLocalRandom.current().nextInt(0, 3);
			while(firstChoice == secondChoice) { //makes sure they aren't the same table
				secondChoice = ThreadLocalRandom.current().nextInt(0, 3);
			}
		}
		
		public void run() {
			try {				
				//enter the restaurant through the first available door
				int door = 0;
				while(!restaurantDoors[0].tryAcquire()) {
					if(restaurantDoors[1].tryAcquire()) {
						door = 1;
						break;
					}
				}
				customersInRestaurant++;
				System.out.println("Customer " + id + " entered through door " + door + ".");
				
				//choose which table to sit at
				int table;
				String choiceType;
				if(tableLineCounts[firstChoice] >= 7) {
					if(tableLineCounts[secondChoice] >= 7) {
						table = firstChoice; //goes to first table if both are long
						choiceType = "first";
					} else {
						table = secondChoice; //goes to second choice if that line isn't long
						choiceType = "second";
					}
				} else {
					table = firstChoice; //goes to first choice if that line isn't long
					choiceType = "first";
				}
				restaurantDoors[door].release(); //steps away from door, allowing next person to enter
				
				//try to sit down at the table, or wait in line until you can
				if(!tablesAvailable[table].tryAcquire()) {
					tableLineCounts[table]++; //add one to the line if you can't sit down right away
					System.out.println("Customer " + id + " has entered line " + table + ".");
					tablesAvailable[table].acquire(); //wait until the table is available; the semaphore is set as fair
				}
				System.out.println("Customer " + id + " has sat down at table " + table + ". It is their " + choiceType + " choice.");
				
				//tell the waiter your order when they are available
				System.out.println("Customer " + id + " calls for a waiter.");
				waiterAvailable[table].acquire(); //wait for waiter to be available
				currentOrders[table] = id; //make order
				orderRequests[table].release(); //request order
				System.out.println("Customer " + id + " gives their order to Waiter " + table + ".");
				
				//wait for the order to be delivered and eat it
				orderCompleted[table].acquire();
				System.out.println("Customer " + id + " receives their order.");
				Thread.sleep((long)ThreadLocalRandom.current().nextInt(200, 1001)); //eat in random time
				System.out.println("Customer " + id + " eats their order.");
				
				//leave the table, pay, and then leave the restaurant
				tablesAvailable[table].release(); //leave the table
				System.out.println("Customer " + id + " leaves table " + table + ".");
				payingAvailable.acquire(); //wait until paying is available
				payingAvailable.release(); //pay
				System.out.println("Customer " + id + " pays and leaves the restaurant.");
				customersInRestaurant--; //leave the restaurant
			}
			catch(Exception e) {
				System.err.println("Error in Customer " + id + ": " + e);
			}
		}
	}
	
	static public class Waiter extends Thread {
		int id;
		Waiter(int i){
			id = i;
		}
		
		public void run() {
			try {				
				outerloop:
				while(customersInRestaurant > 0) {
					//become available for customers
					waiterAvailable[id].release();
					System.out.println("Waiter " + id + " is ready to be called.");
					
					//take a customer's order
					while(!orderRequests[id].tryAcquire()) { //wait for customer to make order
						if(customersInRestaurant <= 0) { //makes sure that the restaurant isn't empty
							break outerloop; //breaks all the way out if all customers are gone
						}
					}
					int order = currentOrders[id]; //take order
					System.out.println("Waiter " + id + " takes Customer " + order + "'s order.");
					
					//deliver order to kitchen
					kitchenAvailable.acquire(); //wait for the kitchen to be available
					Thread.sleep((long)ThreadLocalRandom.current().nextInt(100, 501)); //deliver order in random time
					kitchenAvailable.release(); //leaves kitchen
					System.out.println("Waiter " + id + " delivers their order to the kitchen.");
					
					//wait for the order to be ready
					Thread.sleep((long)ThreadLocalRandom.current().nextInt(300, 1001)); //order is cooked in random time
					System.out.println("Waiter " + id + " waits for their order to ready.");
					
					//pickup order from kitchen
					kitchenAvailable.acquire(); //wait for the kitchen to be available again
					Thread.sleep((long)ThreadLocalRandom.current().nextInt(100, 501)); //pickup order in random time
					kitchenAvailable.release();
					System.out.println("Waiter " + id + " gets their order from the kitchen.");
					
					//give order to customer
					System.out.println("Waiter " + id + " delivers order to Customer " + order + ".");
					orderCompleted[id].release();
				}
				System.out.println("Waiter " + id + " cleans their table and leaves the restaurant.");
			}
			catch(Exception e) {
				System.err.println("Error in Waiter " + id + ": " + e);
			}
		}
	}
	
	static public void main(String[] args) {
		Customer[] customers = new Customer[40];
		Waiter[] waiters = new Waiter[3];
		
		//make instances of all customers and run them
		for(int i=0; i<customers.length; i++) {
			customers[i] = new Customer(i);
			customers[i].start(); //customer i+1 is running concurrently
		}
		//make instances of all waiters and run them
		for(int i=0; i<waiters.length; i++) {
			waiters[i] = new Waiter(i);
			waiters[i].start(); //waiter i+1 is running concurrently
		}
	}
}

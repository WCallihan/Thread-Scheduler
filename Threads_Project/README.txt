included source files:
Restaurant.java

Execute the program by compiling and running the file like any other file. No command line arguments are necessary.
For example, on any Linux system, execute the following:
javac Restaurant.java
java Restaurant

or simply 'Run' Restaurant.java in any java IDE.

The threads are IDed 0-40 in the order they are created and started and the waiter are IDed 0-2 in the same fashion.
All semaphores are initialized as fair so that no queues are required to simulate the lines for tables and paying. Instead, threads naturally go in the order they arrived.
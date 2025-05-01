import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

// Process class representing the data from processes.txt
class Process {
    int pid;
    int arrivalTime;
    int burstTime;
    int priority;

    public Process(int pid, int arrivalTime, int burstTime, int priority) {
        this.pid = pid;
        this.arrivalTime = arrivalTime;
        this.burstTime = burstTime;
        this.priority = priority;
    }

    @Override
    public String toString() {
        return "Process " + pid + " (Arrival: " + arrivalTime + 
               ", Burst: " + burstTime + ", Priority: " + priority + ")";
    }
}

// Bounded buffer for the Producer-Consumer problem
class BoundedBuffer {
    private final int capacity;
    private final Semaphore mutex;
    private final Semaphore emptySlots;
    private final Semaphore filledSlots;
    private final Queue<Process> buffer;

    public BoundedBuffer(int capacity) {
        this.capacity = capacity;
        this.mutex = new Semaphore(1);
        this.emptySlots = new Semaphore(capacity);
        this.filledSlots = new Semaphore(0);
        this.buffer = new LinkedList<>();
    }

    public void produce(Process process) throws InterruptedException {
        emptySlots.acquire(); // Wait if buffer is full
        mutex.acquire();       // Ensure mutual exclusion

        try {
            buffer.add(process);
            System.out.println("[Producer] Added " + process + " to buffer. Buffer size: " + buffer.size());
        } finally {
            mutex.release();
        }

        filledSlots.release(); // Signal that a slot is filled
    }

    public Process consume() throws InterruptedException {
        filledSlots.acquire(); // Wait if buffer is empty
        mutex.acquire();       // Ensure mutual exclusion

        Process process = null;
        try {
            process = buffer.poll();
            System.out.println("[Consumer] Removed " + process + " from buffer. Buffer size: " + buffer.size());
        } finally {
            mutex.release();
        }

        emptySlots.release(); // Signal that a slot is empty
        return process;
    }
}

// Producer thread that reads processes and adds them to the buffer
class Producer extends Thread {
    private final BoundedBuffer buffer;
    private final List<Process> processes;

    public Producer(BoundedBuffer buffer, List<Process> processes) {
        this.buffer = buffer;
        this.processes = processes;
    }

    @Override
    public void run() {
        try {
            System.out.println("[Producer] Starting to add processes to buffer");
            
            // Sort processes by arrival time
            processes.sort(Comparator.comparingInt(p -> p.arrivalTime));
            
            int currentTime = 0;
            for (Process process : processes) {
                // Simulate arrival time by sleeping
                if (process.arrivalTime > currentTime) {
                    int sleepTime = process.arrivalTime - currentTime;
                    System.out.println("[Producer] Waiting " + sleepTime + " time units for next process to arrive");
                    Thread.sleep(sleepTime * 100); // Scale for simulation speed
                    currentTime = process.arrivalTime;
                }
                
                System.out.println("[Producer] Process " + process.pid + " has arrived at time " + currentTime);
                buffer.produce(process);
                currentTime++;  // Increment time after producing
            }
            
            System.out.println("[Producer] All processes added to buffer");
        } catch (InterruptedException e) {
            System.out.println("[Producer] Interrupted: " + e.getMessage());
            Thread.currentThread().interrupt();
        }
    }
}

// Consumer thread that processes items from the buffer
class Consumer extends Thread {
    private final BoundedBuffer buffer;
    private final int id;
    private boolean stop = false; // Flag to stop the consumer
    private final ReentrantLock cpuLock; // CPU resource lock

    public Consumer(int id, BoundedBuffer buffer, ReentrantLock cpuLock) {
        this.id = id;
        this.buffer = buffer;
        this.cpuLock = cpuLock;
        setName("Consumer-" + id);
    }

    public void stopConsuming() {
        this.stop = true;
    }

    @Override
    public void run() {
        try {
            System.out.println("[Consumer-" + id + "] Started");
            
            while (!stop) {
                Process process = buffer.consume();
                if (process == null) continue;
                
                System.out.println("[Consumer-" + id + "] Processing " + process);
                
                // Acquire CPU lock to simulate CPU execution
                System.out.println("[Consumer-" + id + "] Waiting for CPU lock");
                cpuLock.lock();
                try {
                    System.out.println("[Consumer-" + id + "] Acquired CPU lock, executing Process " + process.pid);
                    
                    // Simulate process execution with sleep
                    long startTime = System.currentTimeMillis();
                    Thread.sleep(process.burstTime * 100); // Scale for simulation speed
                    long endTime = System.currentTimeMillis();
                    
                    System.out.println("[Consumer-" + id + "] Completed Process " + process.pid + 
                                      " (Execution time: " + (endTime - startTime) / 100.0 + " time units)");
                } finally {
                    System.out.println("[Consumer-" + id + "] Released CPU lock");
                    cpuLock.unlock();
                }
            }
            
            System.out.println("[Consumer-" + id + "] Stopped");
        } catch (InterruptedException e) {
            System.out.println("[Consumer-" + id + "] Interrupted: " + e.getMessage());
            Thread.currentThread().interrupt();
        }
    }
}

public class ThreadProcessSimulation {
    public static void main(String[] args) {
        // Read processes from file
        List<Process> processes = readProcessesFromFile("processes.txt");
        
        if (processes.isEmpty()) {
            System.out.println("No processes found. Creating sample processes.");
            // Create sample processes if file not found
            processes.add(new Process(1, 0, 5, 2));
            processes.add(new Process(2, 2, 3, 1));
            processes.add(new Process(3, 4, 2, 3));
        }
        
        // Print process information
        System.out.println("Process Information:");
        for (Process p : processes) {
            System.out.println(p);
        }
        System.out.println();
        
        // Create bounded buffer for producer-consumer
        BoundedBuffer buffer = new BoundedBuffer(5); // Buffer capacity of 5
        
        // Create CPU lock for consumers
        ReentrantLock cpuLock = new ReentrantLock();
        
        // Create and start producer
        Producer producer = new Producer(buffer, processes);
        
        // Create and start consumers (CPUs)
        int numConsumers = 2; // Simulate 2 CPU cores
        List<Consumer> consumers = new ArrayList<>();
        
        for (int i = 1; i <= numConsumers; i++) {
            Consumer consumer = new Consumer(i, buffer, cpuLock);
            consumers.add(consumer);
            consumer.start();
        }
        
        // Start the producer
        producer.start();
        
        // Wait for producer to finish
        try {
            producer.join();
            System.out.println("Producer has finished adding all processes");
            
            // Give consumers time to process all items
            Thread.sleep(processes.size() * 200);
            
            // Stop consumers
            for (Consumer consumer : consumers) {
                consumer.stopConsuming();
            }
            
            // Wait for consumers to finish
            for (Consumer consumer : consumers) {
                consumer.join();
            }
            
            System.out.println("All processes have been executed!");
            
        } catch (InterruptedException e) {
            System.out.println("Main thread interrupted: " + e.getMessage());
            Thread.currentThread().interrupt();
        }
    }
    
    private static List<Process> readProcessesFromFile(String filename) {
        List<Process> processes = new ArrayList<>();
        
        try {
            Scanner scanner = new Scanner(new File(filename));
            // Skip header if present
            if (scanner.hasNextLine()) {
                String header = scanner.nextLine();
                if (!header.matches("\\d+.*")) {
                    // Header detected, continue to data
                }
            }

            while (scanner.hasNextLine()) {
                String line = scanner.nextLine().trim();
                if (line.isEmpty()) continue;

                String[] parts = line.split("\\s+");
                int pid = Integer.parseInt(parts[0]);
                int arrivalTime = Integer.parseInt(parts[1]);
                int burstTime = Integer.parseInt(parts[2]);
                int priority = parts.length > 3 ? Integer.parseInt(parts[3]) : 0;

                processes.add(new Process(pid, arrivalTime, burstTime, priority));
            }
            scanner.close();
            System.out.println("Successfully read " + processes.size() + " processes from file.");
        } catch (FileNotFoundException e) {
            System.out.println("File not found: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("Error reading file: " + e.getMessage());
        }
        
        return processes;
    }
}

import java.io.*;
import java.util.*;

class Process {
    int pid;
    int arrivalTime;
    int burstTime;
    int priority;
    int remainingTime;
    int waitingTime;
    int turnaroundTime;
    int completionTime;
    int startTime = -1;

    public Process(int pid, int arrivalTime, int burstTime, int priority) {
        this.pid = pid;
        this.arrivalTime = arrivalTime;
        this.burstTime = burstTime;
        this.priority = priority;
        this.remainingTime = burstTime;
    }

    public Process clone() {
        return new Process(pid, arrivalTime, burstTime, priority);
    }
}

public class ProcessSchedulingSimulation {
    private static List<Process> processes = new ArrayList<>();
    private static List<Process> originalProcesses = new ArrayList<>();
    private static List<int[]> ganttChart = new ArrayList<>();

    public static void main(String[] args) {
        // Read processes from file
        readProcessesFromFile("processes.txt");

        // Make a copy of the original processes for each algorithm
        saveOriginalProcesses();

        // Run FCFS Algorithm
        System.out.println("--------------------------------------------------");
        System.out.println("Executing First-Come, First-Served (FCFS)");
        System.out.println("--------------------------------------------------");
        resetProcesses();
        fcfsScheduling();
        displayResults("FCFS");

        // Run SJF Algorithm
        System.out.println("\n--------------------------------------------------");
        System.out.println("Executing Shortest Job First (SJF)");
        System.out.println("--------------------------------------------------");
        resetProcesses();
        sjfScheduling();
        displayResults("SJF");

        // Run Round Robin Algorithm
        System.out.println("\n--------------------------------------------------");
        System.out.println("Executing Round Robin (Quantum = 2)");
        System.out.println("--------------------------------------------------");
        resetProcesses();
        roundRobinScheduling(2);
        displayResults("Round Robin");
    }

    private static void readProcessesFromFile(String filename) {
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
            System.out.println("File not found. Creating sample processes instead.");
            // Create sample processes if file not found
            processes.add(new Process(1, 0, 5, 2));
            processes.add(new Process(2, 2, 3, 1));
            processes.add(new Process(3, 4, 2, 3));
        } catch (Exception e) {
            System.out.println("Error reading file: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void saveOriginalProcesses() {
        for (Process p : processes) {
            originalProcesses.add(p.clone());
        }
    }

    private static void resetProcesses() {
        processes.clear();
        ganttChart.clear();
        
        for (Process p : originalProcesses) {
            processes.add(p.clone());
        }
    }

    private static void fcfsScheduling() {
        // Sort processes by arrival time
        processes.sort(Comparator.comparingInt(p -> p.arrivalTime));
        
        int currentTime = 0;
        
        for (Process process : processes) {
            // If there's idle time
            if (currentTime < process.arrivalTime) {
                currentTime = process.arrivalTime;
            }
            
            process.startTime = currentTime;
            process.waitingTime = currentTime - process.arrivalTime;
            currentTime += process.burstTime;
            process.completionTime = currentTime;
            process.turnaroundTime = process.completionTime - process.arrivalTime;
            
            // Add to Gantt chart
            ganttChart.add(new int[]{process.pid, process.startTime, process.completionTime});
        }
    }

    private static void sjfScheduling() {
        // Sort processes by arrival time initially
        processes.sort(Comparator.comparingInt(p -> p.arrivalTime));
        
        int currentTime = 0;
        int completed = 0;
        boolean[] isCompleted = new boolean[processes.size()];
        
        while (completed != processes.size()) {
            int minBurstTimeIndex = -1;
            int minBurstTime = Integer.MAX_VALUE;
            
            for (int i = 0; i < processes.size(); i++) {
                Process p = processes.get(i);
                if (p.arrivalTime <= currentTime && !isCompleted[i]) {
                    if (p.burstTime < minBurstTime) {
                        minBurstTime = p.burstTime;
                        minBurstTimeIndex = i;
                    }
                }
            }
            
            if (minBurstTimeIndex == -1) {
                // No process available at current time
                currentTime++;
            } else {
                Process process = processes.get(minBurstTimeIndex);
                process.startTime = currentTime;
                process.waitingTime = currentTime - process.arrivalTime;
                
                currentTime += process.burstTime;
                process.completionTime = currentTime;
                process.turnaroundTime = process.completionTime - process.arrivalTime;
                
                isCompleted[minBurstTimeIndex] = true;
                completed++;
                
                // Add to Gantt chart
                ganttChart.add(new int[]{process.pid, process.startTime, process.completionTime});
            }
        }
    }

    private static void roundRobinScheduling(int timeQuantum) {
        // Sort processes by arrival time
        processes.sort(Comparator.comparingInt(p -> p.arrivalTime));
        
        Queue<Integer> readyQueue = new LinkedList<>();
        boolean[] inQueue = new boolean[processes.size()];
        int[] remainingBurstTime = new int[processes.size()];
        
        for (int i = 0; i < processes.size(); i++) {
            remainingBurstTime[i] = processes.get(i).burstTime;
        }
        
        int currentTime = 0;
        int completed = 0;
        
        // Add first process to ready queue
        if (!processes.isEmpty()) {
            readyQueue.add(0);
            inQueue[0] = true;
        }
        
        while (completed != processes.size()) {
            // If ready queue is empty, find next arriving process
            if (readyQueue.isEmpty()) {
                int nextArrival = Integer.MAX_VALUE;
                int nextProcess = -1;
                
                for (int i = 0; i < processes.size(); i++) {
                    if (remainingBurstTime[i] > 0 && processes.get(i).arrivalTime < nextArrival) {
                        nextArrival = processes.get(i).arrivalTime;
                        nextProcess = i;
                    }
                }
                
                if (nextProcess != -1) {
                    currentTime = nextArrival;
                    readyQueue.add(nextProcess);
                    inQueue[nextProcess] = true;
                }
            }
            
            if (!readyQueue.isEmpty()) {
                int index = readyQueue.poll();
                Process process = processes.get(index);
                
                // Record start time if first execution
                if (process.startTime == -1) {
                    process.startTime = currentTime;
                }
                
                // Calculate execution time in this time slice
                int executeTime = Math.min(remainingBurstTime[index], timeQuantum);
                
                // Add to Gantt chart
                ganttChart.add(new int[]{process.pid, currentTime, currentTime + executeTime});
                
                // Update current time
                currentTime += executeTime;
                remainingBurstTime[index] -= executeTime;
                
                // Check for newly arrived processes during this time slice
                for (int i = 0; i < processes.size(); i++) {
                    if (remainingBurstTime[i] > 0 && processes.get(i).arrivalTime <= currentTime && !inQueue[i]) {
                        readyQueue.add(i);
                        inQueue[i] = true;
                    }
                }
                
                // Process completed
                if (remainingBurstTime[index] == 0) {
                    completed++;
                    process.completionTime = currentTime;
                    process.turnaroundTime = process.completionTime - process.arrivalTime;
                    process.waitingTime = process.turnaroundTime - process.burstTime;
                } else {
                    // Process not completed, re-add to queue
                    readyQueue.add(index);
                }
            } else {
                // Should not happen with the new logic, but keep as safety
                currentTime++;
            }
        }
    }

    private static void displayResults(String algorithm) {
        // Print Gantt Chart
        System.out.println("\nGantt Chart:");
        printGanttChart();
        
        // Calculate and print averages
        double totalWT = 0, totalTAT = 0;
        
        System.out.println("\nProcess Execution Details:");
        System.out.println("PID\tArrival\tBurst\tCompletion\tWaiting\tTurnaround");
        
        for (Process p : processes) {
            totalWT += p.waitingTime;
            totalTAT += p.turnaroundTime;
            
            System.out.printf("%d\t%d\t%d\t%d\t\t%d\t%d\n", 
                    p.pid, p.arrivalTime, p.burstTime, 
                    p.completionTime, p.waitingTime, p.turnaroundTime);
        }
        
        double avgWT = totalWT / processes.size();
        double avgTAT = totalTAT / processes.size();
        
        System.out.println("\nPerformance Metrics:");
        System.out.printf("Average Waiting Time: %.5f\n", avgWT);
        System.out.printf("Average Turnaround Time: %.5f\n", avgTAT);
    }

    private static void printGanttChart() {
        // Sort Gantt chart entries by start time
        ganttChart.sort(Comparator.comparingInt(a -> a[1]));
        
        // Find the maximum completion time
        int maxCompletionTime = 0;
        for (int[] entry : ganttChart) {
            maxCompletionTime = Math.max(maxCompletionTime, entry[2]);
        }
        
        // Print process IDs
        System.out.print("|");
        for (int[] entry : ganttChart) {
            String processLabel = " P" + entry[0] + " ";
            int processTime = entry[2] - entry[1];
            
            // Center the process label
            int spaces = Math.max(processTime * 2 - processLabel.length(), 1);
            int leftSpaces = spaces / 2;
            int rightSpaces = spaces - leftSpaces;
            
            System.out.print(" ".repeat(leftSpaces) + processLabel + " ".repeat(rightSpaces) + "|");
        }
        System.out.println();
        
        // Print timeline
        System.out.print(ganttChart.get(0)[1]);
        for (int[] entry : ganttChart) {
            int processTime = entry[2] - entry[1];
            System.out.print(" ".repeat(processTime * 2) + entry[2]);
        }
        System.out.println();
    }
}

package org.example;
import MonteCarlo.*;
import com.zeroc.Ice.Communicator;
import com.zeroc.Ice.ObjectPrx;
import com.zeroc.Ice.Util;

import java.util.Scanner;

public class Client {
    public static void main(String[] args) {

        try (Communicator communicator = Util.initialize(args)) {
            // Connect to master
            ObjectPrx base = communicator.stringToProxy("master:default -p 10001");
            MasterPrx master = MasterPrx.checkedCast(base);

            if (master == null) {
                throw new Error("Invalid master proxy");
            }

            Scanner scanner = new Scanner(System.in);

            while (true) {
                try {
                    System.out.println("\n=== Monte Carlo PI Estimation ===");
                    System.out.println("1. Start new calculation");
                    System.out.println("2. Exit");
                    System.out.print("\nChoose an option (1-2): ");

                    if (!scanner.hasNextInt()) {
                        System.out.println("Please enter a valid number (1-2)");
                        scanner.nextLine(); // Clear invalid input
                        continue;
                    }

                    int choice = scanner.nextInt();
                    scanner.nextLine(); // Clear newline

                    if (choice == 2) {
                        System.out.println("Exiting...");
                        break;
                    }

                    if (choice != 1) {
                        System.out.println("Invalid option. Please choose 1 or 2.");
                        continue;
                    }

                    // Get calculation parameters
                    long totalPoints = 0;
                    int numWorkers = 0;

                    while (totalPoints <= 0) {
                        System.out.print("Enter total number of points (N > 0): ");
                        try {
                            totalPoints = Long.parseLong(scanner.nextLine());
                            if (totalPoints <= 0) {
                                System.out.println("Please enter a positive number.");
                            }
                        } catch (NumberFormatException e) {
                            System.out.println("Please enter a valid number.");
                        }
                    }

                    while (numWorkers <= 0) {
                        System.out.print("Enter number of workers (n > 0): ");
                        try {
                            numWorkers = Integer.parseInt(scanner.nextLine());
                            if (numWorkers <= 0) {
                                System.out.println("Please enter a positive number.");
                            }
                        } catch (NumberFormatException e) {
                            System.out.println("Please enter a valid number.");
                        }
                    }

                    // Start calculation
                    System.out.println("\nStarting PI calculation...");
                    System.out.println("Points per worker: " + (totalPoints / numWorkers));

                    long startTime = System.currentTimeMillis();
                    master.calculatePI(totalPoints, numWorkers);

                    // Monitor progress
                    String lastStatus = "";
                    while (true) {
                        String currentStatus = master.getCalculationStatus();
                        if (!currentStatus.equals(lastStatus)) {
                            System.out.println("Status: " + currentStatus);
                            lastStatus = currentStatus;
                        }

                        if (currentStatus.startsWith("Error") ||
                                currentStatus.startsWith("Calculation complete")) {
                            break;
                        }

                        Thread.sleep(500); // Poll every half second
                    }

                    // Get and display results
                    try {
                        double pi = master.getPIValue();
                        long endTime = System.currentTimeMillis();
                        double duration = (endTime - startTime) / 1000.0;

                        System.out.println("\n=== Results ===");
                        System.out.printf("Estimated π: %.10f\n", pi);
                        System.out.printf("Actual π:    %.10f\n", Math.PI);
                        System.out.printf("Absolute error: %.10f\n", Math.abs(pi - Math.PI));
                        System.out.printf("Relative error: %.10f%%\n",
                                Math.abs(pi - Math.PI) / Math.PI * 100);
                        System.out.printf("Total time: %.3f seconds\n", duration);
                        System.out.printf("Points processed per second: %.2f\n",
                                totalPoints / duration);

                    } catch (Exception e) {
                        System.out.println("\nError getting results: " + e.getMessage());
                    }

                } catch (Exception e) {
                    System.out.println("\nError: " + e.getMessage());
                    System.out.println("Press Enter to continue...");
                    scanner.nextLine();
                }
            }

        } catch (Exception e) {
            System.err.println("Fatal error:");
            e.printStackTrace();
        }
    }

}
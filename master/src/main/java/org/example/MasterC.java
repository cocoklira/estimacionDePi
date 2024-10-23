package org.example;

import com.zeroc.Ice.*;
import MonteCarlo.*;

import java.lang.Exception;
import java.lang.Object;
import java.util.concurrent.*;
import java.util.ArrayList;
import java.util.List;



public class MasterC implements Master {
    private volatile boolean calculationComplete = false;
    private volatile String status = "Ready";
    private double piEstimation = 0.0;
    private final List<CompletableFuture<double[]>> workerFutures = new ArrayList<>();
    private final Object lock = new Object();

    @Override

    public void calculatePI(long totalPoints, int numWorkers, Current current) {

        synchronized (lock) {
            calculationComplete = false;
            status = "Calculation in progress";
            workerFutures.clear();

            // Calculate points per worker, handling remainder
            long pointsPerWorker = totalPoints / numWorkers;
            long remainingPoints = totalPoints % numWorkers;

            try (Communicator communicator = Util.initialize()) {
                // Start calculations on all workers
                for (int i = 0; i < numWorkers; i++) {
                    final int workerId = i;
                    // Add remainder points to last worker
                    final long workerPoints = (i == numWorkers - 1) ?
                            pointsPerWorker + remainingPoints : pointsPerWorker;

                    CompletableFuture<double[]> future = CompletableFuture.supplyAsync(() -> {
                        try {
                            // Connect to worker
                            ObjectPrx base = communicator.stringToProxy(
                                    String.format("worker%d:default -p %d", workerId, 10000 + workerId));
                            WorkerPrx worker = WorkerPrx.checkedCast(base);

                            if (worker == null) {
                                throw new Error("Invalid worker proxy");
                            }

                            // Start async calculation
                            worker.generatePoints(workerPoints);

                            // Get results (this will wait until calculation is complete)
                            long pointsInside = worker.getPointsInside();
                            long totalProcessed = worker.getTotalPoints();

                            // Return results as array: [pointsInside, totalPoints]
                            return new double[]{pointsInside, totalProcessed};

                        } catch (Exception e) {
                            System.err.println("Error in worker " + workerId + ": " + e.getMessage());
                            throw new CompletionException(e);
                        }
                    });

                    workerFutures.add(future);
                }

                // Handle all results
                CompletableFuture.allOf(workerFutures.toArray(new CompletableFuture[0]))
                        .thenAccept(v -> aggregateResults())
                        .exceptionally(e -> {
                            status = "Error: " + e.getMessage();
                            calculationComplete = true;
                            synchronized (lock) {
                                lock.notifyAll();
                            }
                            return null;
                        });

                System.out.println("All worker tasks initiated");

            } catch (Exception e) {
                status = "Error initializing calculation: " + e.getMessage();
                calculationComplete = true;
                synchronized (lock) {
                    lock.notifyAll();
                }
            }
        }
    }

    private void aggregateResults() {
        double totalPointsInside = 0;
        double totalPointsProcessed = 0;

        try {
            for (CompletableFuture<double[]> future : workerFutures) {
                double[] result = future.get();
                totalPointsInside += result[0];
                totalPointsProcessed += result[1];
            }

            if (totalPointsProcessed > 0) {
                piEstimation = 4.0 * totalPointsInside / totalPointsProcessed;
                status = String.format("Calculation complete. PI ≈ %.10f", piEstimation);
            } else {
                status = "Error: No points were processed";
            }

        } catch (Exception e) {
            status = "Error during aggregation: " + e.getMessage();
        } finally {
            calculationComplete = true;
            synchronized (lock) {
                lock.notifyAll();
            }
        }
    }

    @Override
    public double getPIValue(Current current) {
        while (!calculationComplete) {
            try {
                wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted while waiting for PI value");
            }
        }
        if (status.startsWith("Error")) {
            throw new RuntimeException(status);
        }
        return piEstimation;
    }

    @Override
    public String getCalculationStatus(Current current) {
        return status;
    }
}
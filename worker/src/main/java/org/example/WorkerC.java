package org.example;
import com.zeroc.Ice.*;
import MonteCarlo.*;
import java.util.Random;
import java.util.concurrent.CompletableFuture;



public class WorkerC implements Worker {

    private final Random random = new Random();
    private volatile boolean calculationComplete = false;
    private long pointsInside = 0;
    private long totalPoints = 0;
    private CompletableFuture<Void> calculationFuture;

    @Override
    public void generatePoints(long numPoints, Current current) {

        calculationComplete = false;
        pointsInside = 0;
        totalPoints = 0;

        // Start async calculation
        calculationFuture = CompletableFuture.runAsync(() -> {
            for (long i = 0; i < numPoints; i++) {
                // Generate random points in square [-1,1] x [-1,1]
                double x = random.nextDouble() * 2 - 1;
                double y = random.nextDouble() * 2 - 1;

                // Check if point is inside unit circle
                if (x * x + y * y <= 1) {
                    pointsInside++;
                }
            }

            synchronized (this) {
                totalPoints = numPoints;
                calculationComplete = true;
                notifyAll();
            }
        });
    }

    @Override
    public long getPointsInside(Current current) {

        while (!calculationComplete) {
            try {
                wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted while waiting for results");
            }
        }
        return pointsInside;
    }


    @Override
    public long getTotalPoints(Current current) {
        while (!calculationComplete) {
            try {
                wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted while waiting for results");
            }
        }
        return totalPoints;
    }
}

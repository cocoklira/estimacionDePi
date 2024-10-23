package org.example;


import com.zeroc.Ice.Communicator;
import com.zeroc.Ice.ObjectAdapter;
import com.zeroc.Ice.Util;


public class WorkerMain {
    public static void main(String[] args) {
        try (Communicator communicator = Util.initialize(args)) {
            ObjectAdapter adapter = communicator.createObjectAdapterWithEndpoints(
                    "WorkerAdapter", "default -p 10000");

            WorkerC worker = new WorkerC();
            adapter.add( worker, Util.stringToIdentity("worker"));
            adapter.activate();

            System.out.println("Worker is ready.");
            communicator.waitForShutdown();
        }
    }
}
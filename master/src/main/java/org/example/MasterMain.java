package org.example;

import com.zeroc.Ice.*;

import java.lang.Exception;


public class MasterMain {
    public static void main(String[] args) {
        try (Communicator communicator = Util.initialize(args)) {
            // Create adapter
            ObjectAdapter adapter = communicator.createObjectAdapterWithEndpoints(
                    "MasterAdapter", "default -p 10001");

            // Create and register Master object
            MasterC master = new MasterC();
            adapter.add( master, Util.stringToIdentity("master"));
            adapter.activate();

            System.out.println("Master is ready and waiting for client requests...");
            System.out.println("To shutdown the master, press Ctrl+C");

            // Wait for shutdown
            communicator.waitForShutdown();

        } catch (Exception e) {
            System.err.println("Error in Master: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
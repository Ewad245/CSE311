package cse311;

import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;

public class InputThread {
    private final Scanner scanner;
    private final AtomicBoolean running;
    
    public InputThread() {
        scanner = new Scanner(System.in);
        running = new AtomicBoolean(true);
    }

    public void getInput(MemoryManager memory) {
        try {
            while (running.get()) {
                if (scanner.hasNextLine()) {
                    String input = scanner.nextLine();
                    if (input != null) {
                        memory.getInput(input);
                    }
                } else {
                    // Avoid busy waiting
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Input thread error: " + e.getMessage());
        }
    }
    
    /**
     * Stops the input thread gracefully
     */
    public void stop() {
        running.set(false);
    }
    
    /**
     * Properly closes resources when done using the object
     */
    public void close() {
        stop();
        if (scanner != null) {
            scanner.close();
        }
    }
}

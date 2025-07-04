package cse311;

import java.util.concurrent.locks.ReentrantLock;
import java.nio.ByteBuffer;

public class Uart {
    private static final int TX_READY = 0x20; // Bit 5 (0x20) for TX ready
    private static final int RX_READY = 0x01; // Bit 0 (0x01) for RX ready
    private static final int BUFFER_SIZE = 1024; // Reduced buffer size to avoid excessive memory usage

    private volatile int status;
    private volatile int control;
    private final ByteBuffer rxBuffer; // Using ByteBuffer for better memory management
    private final ReentrantLock lock = new ReentrantLock();

    public Uart() {
        status = TX_READY; // Always ready to transmit
        control = 0;
        rxBuffer = ByteBuffer.allocate(BUFFER_SIZE);
    }

    public int read(int address) {
        lock.lock();
        try {
            switch (address - MemoryManager.UART_BASE) {
                case 0x0: // TX Data
                    return 0;
                case 0x4: // RX Data
                    if (rxBuffer.position() > 0) {
                        // Get the last byte from the buffer
                        rxBuffer.position(rxBuffer.position() - 1);
                        byte data = rxBuffer.get();
                        rxBuffer.position(rxBuffer.position()); // Update position
                        
                        // If buffer is now empty, clear RX ready bit
                        if (rxBuffer.position() == 0) {
                            status &= ~RX_READY;
                        }
                        return data & 0xFF;
                    }
                    return 0;
                case 0x8: // Status
                    // Return current status with TX always ready
                    return status | TX_READY; // TX is always ready
                case 0xC: // Control
                    return control;
                default:
                    return 0;
            }
        } finally {
            lock.unlock();
        }
    }

    public void write(int address, int value) {
        lock.lock();
        try {
            switch (address - MemoryManager.UART_BASE) {
                case 0x0: // TX Data
                    char c = (char)(value & 0xFF);
                    System.out.print(c);
                    // Flush immediately for newlines to ensure output is visible
                    if (c == '\n') {
                        System.out.flush();
                    }
                    break;
                case 0xC: // Control
                    control = value;
                    break;
            }
        } finally {
            lock.unlock();
        }
    }

    public void receiveData(byte data) {
        lock.lock();
        try {
            // Check if buffer has space
            if (rxBuffer.position() < BUFFER_SIZE) {
                // Add data to buffer
                rxBuffer.put(data);
                status |= RX_READY;
            } else {
                // Buffer is full, compact it if possible
                if (rxBuffer.position() > 0) {
                    rxBuffer.flip();
                    // Move data to the beginning of the buffer
                    ByteBuffer newBuffer = ByteBuffer.allocate(BUFFER_SIZE);
                    newBuffer.put(rxBuffer);
                    rxBuffer.clear();
                    rxBuffer.put(newBuffer.array(), 0, newBuffer.position());
                    // Now add the new data
                    rxBuffer.put(data);
                    status |= RX_READY;
                }
            }
        } finally {
            lock.unlock();
        }
    }

    public void receiveDatas(byte[] data) {
        if (data == null || data.length == 0) {
            return;
        }
        
        lock.lock();
        try {
            // Check if we can add all data at once
            if (rxBuffer.remaining() >= data.length) {
                rxBuffer.put(data);
                status |= RX_READY;
            } else {
                // Add data one by one
                for (byte b : data) {
                    receiveData(b);
                }
            }
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Receive a string as input data
     * @param input The string to receive
     */
    public void receiveString(String input) {
        if (input == null || input.isEmpty()) {
            return;
        }
        
        receiveDatas(input.getBytes());
    }
}
package cse311;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.concurrent.locks.ReentrantLock;

import com.corundumstudio.socketio.SocketIOClient;

public class Uart {
    private static final int TX_READY = 0x20; // Bit 5 (0x20) for TX ready
    private static final int RX_READY = 0x01; // Bit 0 (0x01) for RX ready

    private int status;
    private int control;
    private byte[] rxBuffer;
    private int rxIndex;
    private ByteArrayOutputStream buffer;
    private PrintStream monitoredStream;
    private SocketIOClient client;
    private final ReentrantLock lock = new ReentrantLock();

    public Uart(SocketIOClient client) {
        status = TX_READY; // Always ready to transmit
        this.buffer = new ByteArrayOutputStream();
        this.monitoredStream = new PrintStream(buffer);
        this.client = client;
        control = 0;
        rxBuffer = new byte[2048];
        rxIndex = 0;
    }

    public Uart() {
        status = TX_READY; // Always ready to transmit
        this.buffer = new ByteArrayOutputStream();
        this.monitoredStream = new PrintStream(buffer);
        this.client = null;
        control = 0;
        rxBuffer = new byte[2048];
        rxIndex = 0;
    }

    public int read(int address) {
        lock.lock();
        try {
            switch (address - MemoryManager.UART_BASE) {
                case 0x0: // TX Data
                    return 0;
                case 0x4: // RX Data
                    if (rxIndex > 0) {
                        byte data = rxBuffer[--rxIndex];
                        if (rxIndex == 0) {
                            status &= ~RX_READY; // Clear RX ready bit
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

    public void write(int address, int value) throws IOException {
        lock.lock();
        try {
            switch (address - MemoryManager.UART_BASE) {
                case 0x0: // TX Data
                    monitoredStream.write(value & 0xFF);
                    // Send data to client
                    client.sendEvent("cpu_output", buffer.toString());
                    buffer.flush();
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
            if (rxIndex < rxBuffer.length) {
                rxBuffer[rxIndex++] = data;
                status |= RX_READY;
            }
        } finally {
            lock.unlock();
        }
    }

    public void receiveDatas(byte[] data) {
        for (int i = 0; i < data.length; i++) {
            receiveData(data[i]);
        }
    }
}
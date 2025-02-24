package cse311;

public class Uart {
    private static final int TX_READY = 1;
    private static final int RX_READY = 2;

    private int status;
    private int control;
    private byte[] rxBuffer;
    private int rxIndex;

    public Uart() {
        status = TX_READY; // Always ready to transmit
        control = 0;
        rxBuffer = new byte[1024];
        rxIndex = 0;
    }

    public synchronized int read(int address) {
        switch (address - MemoryManager.UART_BASE) {
            case 0x0: // TX Data
                return 0;
            case 0x4: // RX Data
                if (rxIndex > 0) {
                    byte data = rxBuffer[--rxIndex];
                    if (rxIndex == 0) {
                        status &= ~RX_READY;
                    }
                    return data & 0xFF;
                }
                return 0;
            case 0x8: // Status
                return status;
            case 0xC: // Control
                return control;
            default:
                return 0;
        }
    }

    public synchronized void write(int address, int value) {
        switch (address - MemoryManager.UART_BASE) {
            case 0x0: // TX Data
                System.out.write(value & 0xFF);
                System.out.flush();
                break;
            case 0xC: // Control
                control = value;
                break;
        }
    }

    public synchronized void receiveData(byte data) {
        if (rxIndex < rxBuffer.length) {
            rxBuffer[rxIndex++] = data;
            status |= RX_READY;
        }
    }
}
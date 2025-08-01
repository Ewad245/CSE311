package cse311;

public class MemoryManager {
    private SimpleMemory memory;
    private Uart uart;

    // Memory layout constants
    public static final int TEXT_START = 0x10000;
    public static final int RODATA_START = 0x1010000;
    public static final int DATA_START = 0x2010000;
    public static final int HEAP_START = 0x3010000;
    public static final int STACK_START = 0x7C00000;
    
    // User mode memory layout constants
    public static final int USER_TEXT_START = 0x00200000;
    public static final int USER_RODATA_START = 0x00210000;
    public static final int USER_DATA_START = 0x00220000;
    public static final int USER_HEAP_START = 0x00230000;
    public static final int USER_STACK_START = 0x00300000;
    
    // Machine mode reserved memory regions
    public static final int MACHINE_RESERVED_START = 0x00400000;
    public static final int MACHINE_RESERVED_END = 0x00500000;

    // UART Memory-Mapped Registers
    public static final int UART_BASE = 0x10000000;
    public static final int UART_TX_DATA = UART_BASE + 0x0; // Write data to transmit
    public static final int UART_RX_DATA = UART_BASE + 0x4; // Read received data
    public static final int UART_STATUS = UART_BASE + 0x8; // Status register
    public static final int UART_CONTROL = UART_BASE + 0xC;

    private int heapPtr;
    private int stackPtr;

    public MemoryManager() {
        this.memory = new SimpleMemory();
        this.heapPtr = HEAP_START;
        this.stackPtr = STACK_START;
        this.uart = new Uart();
    }
    
    public MemoryManager(SimpleMemory memory) {
        this.memory = memory;
        this.heapPtr = HEAP_START;
        this.stackPtr = STACK_START;
        this.uart = new Uart();
    }

    public void loadProgram(byte[] program) throws MemoryAccessException {
        // Load program into text segment
        for (int i = 0; i < program.length; i++) {
            memory.writeByte(TEXT_START + i, program[i]);
        }
    }

    public void loadData(byte[] data) throws MemoryAccessException {
        // Load data into data segment
        for (int i = 0; i < data.length; i++) {
            memory.writeByte(DATA_START + i, data[i]);
        }
    }

    public int allocateHeap(int size) throws MemoryAccessException {
        int allocated = heapPtr;
        heapPtr += size;
        if (heapPtr >= stackPtr) {
            throw new MemoryAccessException("Out of memory");
        }
        return allocated;
    }
    
    /**
     * Simple no-op free implementation.
     * This is a placeholder for a future implementation of memory deallocation.
     * Currently, memory is not actually freed and will only be reclaimed on reset().
     * 
     * @param address The address to free (ignored in this implementation)
     */
    public void free(int address) {
        // No-op implementation for now
        // In a real implementation, we would track freed blocks and reuse them
    }

    // Memory access methods
    public byte readByte(int address) throws MemoryAccessException {
        if (address >= UART_BASE && address < UART_BASE + 0x1000) {

            return (byte) uart.read(address);
        }
        validateAccess(address);
        return memory.readByte(address);
    }

    public short readHalfWord(int address) throws MemoryAccessException {
        if (address >= UART_BASE && address < UART_BASE + 0x1000) {

            return (short) uart.read(address);
        }
        validateAccess(address);
        validateAccess(address + 1);
        return memory.readHalfWord(address);
    }

    public int readWord(int address) throws MemoryAccessException {
        if (address >= UART_BASE && address < UART_BASE + 0x1000) {
            return (int) uart.read(address);
        }
        validateAccess(address);
        validateAccess(address + 3);
        return memory.readWord(address);
    }

    public void writeByte(int address, byte value) throws MemoryAccessException {
        if (address >= UART_BASE && address < UART_BASE + 0x1000) {
            uart.write(address, value);
            return;
        }
        validateAccess(address);
        validateWriteAccess(address);
        memory.writeByte(address, value);
    }

    public void writeHalfWord(int address, short value) throws MemoryAccessException {
        if (address >= UART_BASE && address < UART_BASE + 0x1000) {

            uart.write(address, value);
            return;
        }
        validateAccess(address);
        validateAccess(address + 1);
        validateWriteAccess(address);
        memory.writeHalfWord(address, value);
    }

    public void writeWord(int address, int value) throws MemoryAccessException {
        if (address >= UART_BASE && address < UART_BASE + 0x1000) {

            uart.write(address, value);
            return;
        }
        validateAccess(address);
        validateAccess(address + 3);
        // validateWriteAccess(address);
        memory.writeWord(address, value);
    }

    // Special method for ELF loading
    public void writeByteToText(int address, byte value) throws MemoryAccessException {
        validateAccess(address); // Only check address range, not write protection
        memory.writeByte(address, value);
    }

    // Stack operations
    public void pushWord(int value) throws MemoryAccessException {
        stackPtr -= 4;
        if (stackPtr <= heapPtr) {
            throw new MemoryAccessException("Stack overflow");
        }
        writeWord(stackPtr, value);
    }

    public int popWord() throws MemoryAccessException {
        int value = readWord(stackPtr);
        stackPtr += 4;
        if (stackPtr > STACK_START) {
            throw new MemoryAccessException("Stack underflow");
        }
        return value;
    }

    // Memory management utilities
    public int getStackPointer() {
        return stackPtr;
    }

    public int getHeapPointer() {
        return heapPtr;
    }

    public void reset() {
        heapPtr = HEAP_START;
        stackPtr = STACK_START;
    }

    // Validation methods
    /**
     * Validate memory access based on address and current CPU privilege mode
     * 
     * @param address The memory address to validate
     * @param privilegeMode The current CPU privilege mode
     * @throws MemoryAccessException If the access is invalid
     */
    public void validateAccess(int address, int privilegeMode) throws MemoryAccessException {
        // Check if address is within valid memory range
        if (address < TEXT_START || address > STACK_START) {
            throw new MemoryAccessException("Invalid memory access: " +
                    String.format("0x%08X", address));
        }
        
        // Check PMP (Physical Memory Protection) if not in Machine mode
        if (privilegeMode != RV32iCpu.PRIVILEGE_MACHINE) {
            // In a real implementation, we would check PMP registers here
            // For now, we'll implement a simple protection scheme:
            // User mode can only access user memory regions
            if (privilegeMode == RV32iCpu.PRIVILEGE_USER) {
                // Example: Restrict user mode from accessing certain memory regions
                if (address < USER_TEXT_START || address >= USER_STACK_START) {
                    throw new MemoryAccessException("User mode cannot access privileged memory at address: 0x" 
                            + Integer.toHexString(address));
                }
            }
        }
    }

    /**
     * Validate memory write access based on address and current CPU privilege mode
     * 
     * @param address The memory address to validate
     * @param privilegeMode The current CPU privilege mode
     * @throws MemoryAccessException If the write access is invalid
     */
    public void validateWriteAccess(int address, int privilegeMode) throws MemoryAccessException {
        if (address >= TEXT_START && address < DATA_START) {
            throw new MemoryAccessException("Cannot write to text segment at address: 0x" 
                    + Integer.toHexString(address));
        }
        
        if (address >= (DATA_START + 0x1000000)) {
            throw new MemoryAccessException("Cannot write to data segment: " +
                    String.format("0x%08X", address));
        }
        
        // Check PMP (Physical Memory Protection) if not in Machine mode
        if (privilegeMode != RV32iCpu.PRIVILEGE_MACHINE) {
            // In a real implementation, we would check PMP registers here
            // For now, we'll implement a simple protection scheme:
            
            // User mode can only write to user memory regions
            if (privilegeMode == RV32iCpu.PRIVILEGE_USER) {
                // Example: Restrict user mode from writing to certain memory regions
                if (address < USER_DATA_START || address >= USER_STACK_START) {
                    throw new MemoryAccessException("User mode cannot write to privileged memory at address: 0x" 
                            + Integer.toHexString(address));
                }
            }
            
            // Supervisor mode has more access but still restricted from some regions
            if (privilegeMode == RV32iCpu.PRIVILEGE_SUPERVISOR) {
                // Example: Restrict supervisor mode from writing to machine-only regions
                if (address >= MACHINE_RESERVED_START && address < MACHINE_RESERVED_END) {
                    throw new MemoryAccessException("Supervisor mode cannot write to machine-only memory at address: 0x" 
                            + Integer.toHexString(address));
                }
            }
        }
    }
    
    /**
     * Legacy method for backward compatibility
     */
    private void validateAccess(int address) throws MemoryAccessException {
        validateAccess(address, RV32iCpu.PRIVILEGE_MACHINE);
    }

    /**
     * Legacy method for backward compatibility
     */
    private void validateWriteAccess(int address) throws MemoryAccessException {
        validateWriteAccess(address, RV32iCpu.PRIVILEGE_MACHINE);
    }

    // Debug utilities
    public String getMemoryMap() {
        StringBuilder sb = new StringBuilder();
        sb.append("Memory Map:\n");
        sb.append(String.format("Text:  0x%08X - 0x%08X\n", TEXT_START, DATA_START - 1));
        sb.append(String.format("Data:  0x%08X - 0x%08X\n", DATA_START, HEAP_START - 1));
        sb.append(String.format("Heap:  0x%08X - 0x%08X\n", HEAP_START, heapPtr - 1));
        sb.append(String.format("Stack: 0x%08X - 0x%08X\n", stackPtr, STACK_START));
        return sb.toString();
    }

    public void dumpMemory(int start, int length) throws MemoryAccessException {
        System.out.println(memory.dumpMemory(start, length));
    }

    public String dumpMemory() throws MemoryAccessException {
        return memory.dumpMemory(TEXT_START, STACK_START - TEXT_START);
    }

    public byte[] getByteMemory() {
        return memory.getMemory();
    }

    public void getInput(String data) {
        uart.receiveDatas(data.getBytes());
    }
}

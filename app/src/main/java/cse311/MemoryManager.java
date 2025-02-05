package cse311;

public class MemoryManager {
    private SimpleMemory memory;

    // Memory layout constants
    public static final int TEXT_START = 0x10000;
    public static final int DATA_START = 0x20000;
    public static final int HEAP_START = 0x30000;
    public static final int STACK_START = 0x7FFFFFF0;

    private int heapPtr;
    private int stackPtr;

    public MemoryManager(SimpleMemory memory) {
        this.memory = memory;
        this.heapPtr = HEAP_START;
        this.stackPtr = STACK_START;
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

    // Memory access methods
    public byte readByte(int address) throws MemoryAccessException {
        validateAccess(address);
        return memory.readByte(address);
    }

    public short readHalfWord(int address) throws MemoryAccessException {
        validateAccess(address);
        validateAccess(address + 1);
        return memory.readHalfWord(address);
    }

    public int readWord(int address) throws MemoryAccessException {
        validateAccess(address);
        validateAccess(address + 3);
        return memory.readWord(address);
    }

    public void writeByte(int address, byte value) throws MemoryAccessException {
        validateAccess(address);
        validateWriteAccess(address);
        memory.writeByte(address, value);
    }

    public void writeHalfWord(int address, short value) throws MemoryAccessException {
        validateAccess(address);
        validateAccess(address + 1);
        validateWriteAccess(address);
        memory.writeHalfWord(address, value);
    }

    public void writeWord(int address, int value) throws MemoryAccessException {
        validateAccess(address);
        validateAccess(address + 3);
        validateWriteAccess(address);
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
    private void validateAccess(int address) throws MemoryAccessException {
        if (address < TEXT_START || address > STACK_START) {
            throw new MemoryAccessException("Invalid memory access: " +
                    String.format("0x%08X", address));
        }
    }

    private void validateWriteAccess(int address) throws MemoryAccessException {
        if (address >= TEXT_START && address < DATA_START) {
            throw new MemoryAccessException("Cannot write to text segment: " +
                    String.format("0x%08X", address));
        }
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
}
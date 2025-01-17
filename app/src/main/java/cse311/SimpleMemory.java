package cse311;

class SimpleMemory {
    private byte[] memory;
    private int MEMORY_SIZE = 1024; // 1KB of memory

    public SimpleMemory() {
        memory = new byte[MEMORY_SIZE];
    }

    public SimpleMemory(int memSize) {
        MEMORY_SIZE = memSize;
        memory = new byte[MEMORY_SIZE];
    }

    public byte readByte(int address) throws MemoryAccessException {
        if (address < 0 || address >= MEMORY_SIZE) {
            throw new MemoryAccessException("Memory access out of bounds: " + address);
        }
        return memory[address];
    }

    public void writeByte(int address, byte value) throws MemoryAccessException {
        if (address < 0 || address >= MEMORY_SIZE) {
            throw new MemoryAccessException("Memory access out of bounds: " + address);
        }
        memory[address] = value;
    }

    /**
     * Initialize a region of memory with given bytes
     */
    public void initializeMemory(int startAddress, byte[] data) {
        System.arraycopy(data, 0, memory, startAddress, data.length);
    }
}

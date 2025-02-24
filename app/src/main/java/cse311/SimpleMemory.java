package cse311;

import java.util.Arrays;

class SimpleMemory {
    private byte[] memory;
    private int MEMORY_SIZE = 1024; // 1KB of memory

    // Memory alignment constants
    private static final int HALF_WORD_ALIGN = 2;
    private static final int WORD_ALIGN = 4;

    public SimpleMemory() {
        memory = new byte[MEMORY_SIZE];
    }

    public SimpleMemory(int memSize) {
        MEMORY_SIZE = memSize;
        memory = new byte[MEMORY_SIZE];
    }

    // Add MMIO ranges
    private static final int MMIO_START = 0x10000000;
    private static final int MMIO_END = 0x10001000;

    public byte readByte(int address) throws MemoryAccessException {
        // Check if address is in MMIO range
        if (address >= MMIO_START && address < MMIO_END) {
            // Let MemoryManager handle MMIO
            throw new MemoryAccessException("MMIO_ACCESS:" + address);
        }
        
        if (address < 0 || address >= MEMORY_SIZE) {
            throw new MemoryAccessException("Memory access out of bounds: " + address);
        }
        return memory[address];
    }

    public void writeByte(int address, byte value) throws MemoryAccessException {
        // Check if address is in MMIO range
        if (address >= MMIO_START && address < MMIO_END) {
            // Let MemoryManager handle MMIO
            throw new MemoryAccessException("MMIO_ACCESS:" + address);
        }

        if (address < 0 || address >= MEMORY_SIZE) {
            throw new MemoryAccessException("Memory access out of bounds: " + address);
        }
        memory[address] = value;
    }

    public short readHalfWord(int address) throws MemoryAccessException {
        checkAddress(address, HALF_WORD_ALIGN);
        checkAlignment(address, HALF_WORD_ALIGN);

        return (short) ((memory[address + 1] & 0xFF) << 8 |
                (memory[address] & 0xFF));
    }

    public int readWord(int address) throws MemoryAccessException {
        checkAddress(address, WORD_ALIGN);
        checkAlignment(address, WORD_ALIGN);

        return (memory[address + 3] & 0xFF) << 24 |
                (memory[address + 2] & 0xFF) << 16 |
                (memory[address + 1] & 0xFF) << 8 |
                (memory[address] & 0xFF);
    }

    public void writeHalfWord(int address, short value) throws MemoryAccessException {
        checkAddress(address, HALF_WORD_ALIGN);
        checkAlignment(address, HALF_WORD_ALIGN);

        memory[address] = (byte) (value & 0xFF);
        memory[address + 1] = (byte) ((value >> 8) & 0xFF);
    }

    public void writeWord(int address, int value) throws MemoryAccessException {
        checkAddress(address, WORD_ALIGN);
        checkAlignment(address, WORD_ALIGN);

        memory[address] = (byte) (value & 0xFF);
        memory[address + 1] = (byte) ((value >> 8) & 0xFF);
        memory[address + 2] = (byte) ((value >> 16) & 0xFF);
        memory[address + 3] = (byte) ((value >> 24) & 0xFF);
    }

    // Utility methods
    private void checkAddress(int address, int accessSize) throws MemoryAccessException {
        if (address < 0 || address + accessSize > MEMORY_SIZE) {
            throw new MemoryAccessException(
                    String.format("Memory access out of bounds: address=%d, size=%d", address, accessSize));
        }
    }

    private void checkAlignment(int address, int alignment) throws MemoryAccessException {
        if ((address % alignment) != 0) {
            throw new MemoryAccessException(
                    String.format("Misaligned memory access: address=%d, required alignment=%d",
                            address, alignment));
        }
    }

    public void clear() {
        Arrays.fill(memory, (byte) 0);
    }

    // Debug helper methods
    public String dumpMemory(int startAddress, int length) throws MemoryAccessException {
        checkAddress(startAddress, length);
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < length; i += 16) {
            // Print address
            sb.append(String.format("%08x: ", startAddress + i));

            // Print hex values
            for (int j = 0; j < 16 && (i + j) < length; j++) {
                if (j % 4 == 0)
                    sb.append(" ");
                sb.append(String.format("%02x ", memory[startAddress + i + j]));
            }

            sb.append("\n");
        }

        return sb.toString();
    }

    /**
     * Initialize a region of memory with given bytes
     */
    public void initializeMemory(int startAddress, byte[] data) {
        System.arraycopy(data, 0, memory, startAddress, data.length);
    }

    public byte[] getMemory() {
        return memory;
    }
}

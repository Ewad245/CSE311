package cse311;

import java.util.Arrays;
import java.nio.ByteBuffer;

class SimpleMemory {
    // Memory size constants
    private static final int DEFAULT_MEMORY_SIZE = 128 * 1024 * 1024; // 128MB of memory
    
    // Memory alignment constants
    private static final int HALF_WORD_ALIGN = 2;
    private static final int WORD_ALIGN = 4;
    
    // Cache configuration
    private static final int CACHE_SIZE = 1024; // 1KB cache
    private static final int CACHE_LINE_SIZE = 64; // 64 bytes per cache line
    private static final int CACHE_LINES = CACHE_SIZE / CACHE_LINE_SIZE;
    
    private final int memorySize;
    private final ByteBuffer memory; // Using ByteBuffer for better performance
    
    // Simple direct-mapped cache
    private final int[] cacheTagArray;
    private final ByteBuffer[] cacheDataArray;
    private final boolean[] cacheValidArray;
    
    public SimpleMemory() {
        this(DEFAULT_MEMORY_SIZE);
    }

    public SimpleMemory(int memSize) {
        this.memorySize = memSize;
        this.memory = ByteBuffer.allocateDirect(memSize); // Using direct buffer for better performance
        
        // Initialize cache
        this.cacheTagArray = new int[CACHE_LINES];
        this.cacheDataArray = new ByteBuffer[CACHE_LINES];
        this.cacheValidArray = new boolean[CACHE_LINES];
        
        for (int i = 0; i < CACHE_LINES; i++) {
            cacheDataArray[i] = ByteBuffer.allocate(CACHE_LINE_SIZE);
            cacheValidArray[i] = false;
        }
    }

    // Add MMIO ranges
    private static final int MMIO_START = 0x10000000;
    private static final int MMIO_END = 0x10001000;

    /**
     * Get cache line index for an address
     */
    private int getCacheIndex(int address) {
        return (address / CACHE_LINE_SIZE) % CACHE_LINES;
    }
    
    /**
     * Get cache line tag for an address
     */
    private int getCacheTag(int address) {
        return address / CACHE_LINE_SIZE;
    }
    
    /**
     * Get offset within a cache line
     */
    private int getCacheOffset(int address) {
        return address % CACHE_LINE_SIZE;
    }
    
    /**
     * Check if address is in cache and return the cached data
     */
    private ByteBuffer checkCache(int address) {
        int index = getCacheIndex(address);
        int tag = getCacheTag(address);
        
        if (cacheValidArray[index] && cacheTagArray[index] == tag) {
            return cacheDataArray[index];
        }
        return null;
    }
    
    /**
     * Update cache with data from memory
     */
    private void updateCache(int address) {
        int index = getCacheIndex(address);
        int tag = getCacheTag(address);
        int baseAddress = tag * CACHE_LINE_SIZE;
        
        // Load data from memory into cache
        ByteBuffer cacheData = cacheDataArray[index];
        cacheData.clear();
        
        // Copy data from memory to cache
        for (int i = 0; i < CACHE_LINE_SIZE; i++) {
            int addr = baseAddress + i;
            if (addr < memorySize) {
                memory.position(addr);
                cacheData.put(i, memory.get());
            }
        }
        
        // Update cache metadata
        cacheTagArray[index] = tag;
        cacheValidArray[index] = true;
    }
    
    /**
     * Invalidate cache entry for an address
     */
    private void invalidateCache(int address) {
        int index = getCacheIndex(address);
        cacheValidArray[index] = false;
    }

    public byte readByte(int address) throws MemoryAccessException {
        // Check if address is in MMIO range
        if (address >= MMIO_START && address < MMIO_END) {
            // Let MemoryManager handle MMIO
            throw new MemoryAccessException("MMIO_ACCESS:" + address);
        }

        if (address < 0 || address >= memorySize) {
            throw new MemoryAccessException("Memory access out of bounds: " + address);
        }
        
        // Check cache first
        ByteBuffer cachedData = checkCache(address);
        if (cachedData != null) {
            return cachedData.get(getCacheOffset(address));
        }
        
        // Cache miss - read from memory and update cache
        memory.position(address);
        byte value = memory.get();
        
        // Update cache for future reads
        updateCache(address);
        
        return value;
    }

    public void writeByte(int address, byte value) throws MemoryAccessException {
        // Check if address is in MMIO range
        if (address >= MMIO_START && address < MMIO_END) {
            // Let MemoryManager handle MMIO
            throw new MemoryAccessException("MMIO_ACCESS:" + address);
        }

        if (address < 0 || address >= memorySize) {
            throw new MemoryAccessException("Memory access out of bounds: " + address);
        }
        
        // Write to memory
        memory.position(address);
        memory.put(value);
        
        // Invalidate cache for this address
        invalidateCache(address);
    }

    public short readHalfWord(int address) throws MemoryAccessException {
        checkAddress(address, HALF_WORD_ALIGN);
        checkAlignment(address, HALF_WORD_ALIGN);

        // Check cache first
        ByteBuffer cachedData = checkCache(address);
        if (cachedData != null) {
            int offset = getCacheOffset(address);
            return (short) ((cachedData.get(offset + 1) & 0xFF) << 8 | 
                           (cachedData.get(offset) & 0xFF));
        }
        
        // Cache miss - read from memory
        memory.position(address);
        short value = memory.getShort();
        
        // Update cache
        updateCache(address);
        
        return value;
    }

    public int readWord(int address) throws MemoryAccessException {
        checkAddress(address, WORD_ALIGN);
        checkAlignment(address, WORD_ALIGN);

        // Check cache first
        ByteBuffer cachedData = checkCache(address);
        if (cachedData != null) {
            int offset = getCacheOffset(address);
            return (cachedData.get(offset + 3) & 0xFF) << 24 |
                   (cachedData.get(offset + 2) & 0xFF) << 16 |
                   (cachedData.get(offset + 1) & 0xFF) << 8 |
                   (cachedData.get(offset) & 0xFF);
        }
        
        // Cache miss - read from memory
        memory.position(address);
        int value = memory.getInt();
        
        // Update cache
        updateCache(address);
        
        return value;
    }

    public void writeHalfWord(int address, short value) throws MemoryAccessException {
        checkAddress(address, HALF_WORD_ALIGN);
        checkAlignment(address, HALF_WORD_ALIGN);

        // Write to memory
        memory.position(address);
        memory.putShort(value);
        
        // Invalidate cache
        invalidateCache(address);
    }

    public void writeWord(int address, int value) throws MemoryAccessException {
        checkAddress(address, WORD_ALIGN);
        checkAlignment(address, WORD_ALIGN);

        // Write to memory
        memory.position(address);
        memory.putInt(value);
        
        // Invalidate cache
        invalidateCache(address);
    }

    // Utility methods
    private void checkAddress(int address, int accessSize) throws MemoryAccessException {
        if (address < 0 || address + accessSize > memorySize) {
            throw new MemoryAccessException(
                    String.format("Memory access out of bounds: address=%d, size=%d", address, accessSize));
        }

        // Check if address is in MMIO range
        if (address >= MMIO_START && address < MMIO_END) {
            // Let MemoryManager handle MMIO
            throw new MemoryAccessException("MMIO_ACCESS:" + address);
        }
    }

    private void checkAlignment(int address, int alignment) throws MemoryAccessException {
        if ((address % alignment) != 0) {
            throw new MemoryAccessException(
                    String.format("Misaligned memory access: address=%d, required alignment=%d",
                            address, alignment));
        }
    }
    
    /**
     * Clear the cache by invalidating all entries
     */
    public void clearCache() {
        for (int i = 0; i < CACHE_LINES; i++) {
            cacheValidArray[i] = false;
        }
    }

    public void clear() {
        memory.clear();
        for (int i = 0; i < memorySize; i++) {
            memory.put(i, (byte) 0);
        }
        clearCache();
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
                sb.append(String.format("%02x ", memory.get(startAddress + i + j) & 0xFF));
            }

            sb.append("\n");
        }

        return sb.toString();
    }

    /**
     * Initialize a region of memory with given bytes
     */
    public void initializeMemory(int startAddress, byte[] data) {
        memory.position(startAddress);
        memory.put(data);
        
        // Invalidate affected cache lines
        int startLine = getCacheTag(startAddress);
        int endLine = getCacheTag(startAddress + data.length - 1);
        for (int line = startLine; line <= endLine; line++) {
            int index = line % CACHE_LINES;
            cacheValidArray[index] = false;
        }
    }

    public ByteBuffer getMemory() {
        return memory.duplicate();
    }
}

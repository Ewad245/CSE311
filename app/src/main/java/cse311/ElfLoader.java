package cse311;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Paths;

public class ElfLoader {
    private byte[] elfData;
    private MemoryManager memory;

    // ELF Header Constants
    private static final byte[] ELF_MAGIC = { 0x7f, 0x45, 0x4c, 0x46 }; // "\177ELF"
    private static final int EI_CLASS_64 = 2;
    private static final int EI_DATA_LE = 1;
    private static final int EM_RISCV = 243;

    // Program Header Types
    private static final int PT_LOAD = 1;

    // Section Header Types
    private static final int SHT_PROGBITS = 1;
    private static final int SHT_NOBITS = 8;

    // Program Header Flags
    private static final int PF_X = 1;
    private static final int PF_W = 2;
    private static final int PF_R = 4;

    public ElfLoader(MemoryManager memory) {
        this.memory = memory;
    }

    public void loadElf(String filename) throws IOException, ElfException {
        elfData = Files.readAllBytes(Paths.get(filename));

        if (!validateElfHeader()) {
            throw new ElfException("Invalid ELF file");
        }

        loadProgramSegments();
    }

    private boolean validateElfHeader() {
        if (elfData.length < 52) { // Minimum size for 32-bit ELF header
            return false;
        }

        // Check magic number
        for (int i = 0; i < ELF_MAGIC.length; i++) {
            if (elfData[i] != ELF_MAGIC[i]) {
                return false;
            }
        }

        // Check ELF class (32-bit)
        if (elfData[4] != 1) {
            return false;
        }

        // Check endianness (little-endian)
        if (elfData[5] != EI_DATA_LE) {
            return false;
        }

        // Check machine type (RISC-V)
        ByteBuffer buffer = ByteBuffer.wrap(elfData).order(ByteOrder.LITTLE_ENDIAN);
        buffer.position(18);
        if (buffer.getShort() != EM_RISCV) {
            return false;
        }

        return true;
    }

    private void loadProgramSegments() throws ElfException {
        ByteBuffer buffer = ByteBuffer.wrap(elfData).order(ByteOrder.LITTLE_ENDIAN);

        // Get program header offset and number of entries
        buffer.position(28);
        int programHeaderOffset = buffer.getInt();
        buffer.position(42);
        int programHeaderEntrySize = buffer.getShort();
        int programHeaderEntryCount = buffer.getShort();

        // Process each program header
        for (int i = 0; i < programHeaderEntryCount; i++) {
            int offset = programHeaderOffset + (i * programHeaderEntrySize);
            buffer.position(offset);

            int type = buffer.getInt();
            if (type != PT_LOAD) {
                continue;
            }

            int offset_in_file = buffer.getInt();
            int virtual_addr = buffer.getInt();
            buffer.getInt(); // physical addr (unused)
            int size_in_file = buffer.getInt();
            int size_in_mem = buffer.getInt();
            int flags = buffer.getInt();
            int alignment = buffer.getInt();

            try {
                loadSegment(offset_in_file, virtual_addr, size_in_file, size_in_mem, flags);
            } catch (MemoryAccessException e) {
                throw new ElfException("Failed to load segment: " + e.getMessage());
            }
        }
    }

    private void loadSegment(int fileOffset, int virtualAddr, int sizeInFile,
            int sizeInMem, int flags) throws MemoryAccessException {
        int mappedAddr = mapAddress(virtualAddr);

        if (sizeInFile > 0) {
            byte[] segmentData = new byte[sizeInFile];
            System.arraycopy(elfData, fileOffset, segmentData, 0, sizeInFile);

            // Use writeByteToText for initial program loading
            for (int i = 0; i < sizeInFile; i++) {
                memory.writeByteToText(mappedAddr + i, segmentData[i]);
            }
        }

        // Zero-initialize remaining memory
        for (int i = sizeInFile; i < sizeInMem; i++) {
            memory.writeByteToText(mappedAddr + i, (byte) 0);
        }
    }

    private int mapAddress(int virtualAddr) {
        // Special handling for UART addresses
        if (virtualAddr >= MemoryManager.UART_BASE &&
                virtualAddr < MemoryManager.UART_BASE + 0x1000) {
            return virtualAddr; // Don't remap UART addresses
        }

        // Convert negative addresses (like 0x80000000) to our memory layout
        if (virtualAddr < 0) {
            // Calculate offset from 0x80000000
            long unsignedAddr = virtualAddr & 0xFFFFFFFFL; // Convert to unsigned long
            int offset = (int) (unsignedAddr - 0x80000000L); // Calculate offset
            return MemoryManager.TEXT_START + offset;
        }
        return virtualAddr;
    }

    public int getEntryPoint() {
        ByteBuffer buffer = ByteBuffer.wrap(elfData).order(ByteOrder.LITTLE_ENDIAN);
        buffer.position(24);
        int entryPoint = buffer.getInt();
        // Map the entry point address as well
        return mapAddress(entryPoint);
    }
}

class ElfException extends Exception {
    public ElfException(String message) {
        super(message);
    }
}
package cse311;

import java.nio.ByteBuffer;
import java.util.Scanner;

public class RV32iCpu {

    private final int[] x = new int[32];
    private int lastPC = -1;
    private int loopCount = 0;
    private int pc = 0;
    private static final int INSTRUCTION_SIZE = 4; // 32-bit instructions

    private final MemoryManager memory;
    private Thread cpuThread;
    private volatile boolean running = false;
    private static final int LOOP_THRESHOLD = 1000; // Maximum times to execute same instruction
    private final InputThread input;
    
    // Cache for frequently accessed memory
    private static final int CACHE_SIZE = 1024;
    private final int[] instructionCache = new int[CACHE_SIZE];
    private final int[] cacheAddresses = new int[CACHE_SIZE];
    private final boolean[] cacheValid = new boolean[CACHE_SIZE];


    public RV32iCpu(MemoryManager memory) {
        this.memory = memory;
        input = new InputThread();
    }

    public void setProgramCounterEntryPoint(int entryPoint) {
        this.pc = entryPoint;
    }

    public void turnOn() {
        if (running) {
            System.out.println("CPU is already running");
            return;
        }
        
        // Reset cache before starting
        clearCache();
        
        Runnable task1 = () -> input.getInput(memory);
        this.cpuThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (RV32iCpu.this.running) {
                    try {
                        // find13And12(memory.getByteMemory());
                        fetchExecuteCycle();
                    } catch (Exception e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }
        });
        
        // Set thread as daemon to prevent it from blocking JVM shutdown
        this.cpuThread.setDaemon(true);
        new Thread(task1).start();
        this.running = true;
        this.cpuThread.start();
    }
    
    private void clearCache() {
        // Clear instruction cache
        for (int i = 0; i < CACHE_SIZE; i++) {
            cacheValid[i] = false;
        }
        
        // Clear memory cache
        memory.clearCache();
    }
    
    public void stop() {
        running = false;
        
        // Stop and close the input thread
        if (input != null) {
            input.close();
        }
        
        // Wait for CPU thread to finish
        try {
            if (cpuThread != null && cpuThread.isAlive()) {
                cpuThread.join(1000); // Wait up to 1 second
                
                // If thread is still running, interrupt it
                if (cpuThread.isAlive()) {
                    cpuThread.interrupt();
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Interrupted while stopping CPU: " + e.getMessage());
        }
        
        // Clear caches and reset state
        clearCache();
    }

    private void fetchExecuteCycle() throws Exception {
        // Check for infinite loop
        if (pc == lastPC) {
            loopCount++;
            if (loopCount > LOOP_THRESHOLD) {
                System.out.println("Infinite loop detected at PC: 0x" + Integer.toHexString(pc));
                System.out.println("Program halted after " + LOOP_THRESHOLD + " iterations");
                this.running = false;
                return;
            }
        } else {
            lastPC = pc;
            loopCount = 0;
        }
        // Fetch the instruction from memory at the address in the pc register
        int instructionFetched = fetch();
        InstructionDecoded instructionDecoded = decode(instructionFetched);
        execute(instructionDecoded); // Viet them update cho pc, cpu sau nay
        // System.out.println(instructionDecoded.toString());
        // displayRegisters();
    }

    private int fetch() throws MemoryAccessException {
        // Check if instruction is in cache
        int cacheIndex = (pc / INSTRUCTION_SIZE) % CACHE_SIZE;
        if (cacheValid[cacheIndex] && cacheAddresses[cacheIndex] == pc) {
            // Cache hit
            int instruction = instructionCache[cacheIndex];
            pc += INSTRUCTION_SIZE;
            return instruction;
        }
        
        // Cache miss - read from memory
        int instruction = 0;
        try {
                // Fallback to byte-by-byte reading for unaligned access
                byte byte0 = memory.readByte(pc);
                byte byte1 = memory.readByte(pc + 1);
                byte byte2 = memory.readByte(pc + 2);
                byte byte3 = memory.readByte(pc + 3);
                
                instruction = (byte3 & 0xFF) << 24
                        | (byte2 & 0xFF) << 16
                        | (byte1 & 0xFF) << 8
                        | (byte0 & 0xFF);

            
            // Update cache
            instructionCache[cacheIndex] = instruction;
            cacheAddresses[cacheIndex] = pc;
            cacheValid[cacheIndex] = true;
            
            // Increment PC by instruction size
            pc += INSTRUCTION_SIZE;
            
        } catch (Exception e) {
            throw new MemoryAccessException("Failed to fetch instruction at PC: " + pc);
        }
        
        return instruction;
    }

    // Reusable instruction decoder instance to avoid object creation
    private final InstructionDecoded decodedInstruction = new InstructionDecoded();
    
    private InstructionDecoded decode(int instructionInt) {
        // Extract instruction fields based on RISC-V RV32I format
        int opcode = instructionInt & 0x7F; // bits 0-6
        decodedInstruction.setOpcode(opcode);
        int rd = (instructionInt >> 7) & 0x1F; // bits 7-11
        decodedInstruction.setRd(rd);
        int func3 = (instructionInt >> 12) & 0x7; // bits 12-14
        decodedInstruction.setFunc3(func3);
        int rs1 = (instructionInt >> 15) & 0x1F; // bits 15-19
        decodedInstruction.setRs1(rs1);
        int rs2 = (instructionInt >> 20) & 0x1F; // bits 20-24
        decodedInstruction.setRs2(rs2);
        int func7 = (instructionInt >> 25) & 0x7F; // bits 25-31
        decodedInstruction.setFunc7(func7);

        // Immediate values for different instruction formats
        // I-type: Sign extended 12-bit immediate
        int imm_i = ((instructionInt >> 20) << 20) >> 20;
        decodedInstruction.setImm_i(imm_i);

        // S-type: Sign extended 12-bit immediate
        int imm_s = (((instructionInt >> 25) << 5) | ((instructionInt >> 7) & 0x1F));
        imm_s = (imm_s << 20) >> 20; // Sign extend
        decodedInstruction.setImm_s(imm_s);

        // B-type: Sign extended 13-bit immediate
        int imm_b = (((instructionInt >> 31) << 12) // imm[12]
                |
                ((instructionInt >> 7) & 0x1) << 11) // imm[11]
                |
                ((instructionInt >> 25) & 0x3F) << 5 // imm[10:5]
                |
                ((instructionInt >> 8) & 0xF) << 1; // imm[4:1]
        imm_b = (imm_b << 19) >> 19; // Sign extend
        decodedInstruction.setImm_b(imm_b);

        // U-type: 20-bit immediate, shifted left by 12
        int imm_u = instructionInt & 0xFFFFF000;
        decodedInstruction.setImm_u(imm_u);

        // J-type: Sign extended 21-bit immediate
        int imm_j = (((instructionInt >> 31) << 20) // imm[20]
                |
                ((instructionInt >> 12) & 0xFF) << 12 // imm[19:12]
                |
                ((instructionInt >> 20) & 0x1) << 11 // imm[11]
                |
                ((instructionInt >> 21) & 0x3FF) << 1); // imm[10:1]
        imm_j = (imm_j << 11) >> 11; // Sign extend
        decodedInstruction.setImm_j(imm_j);
        return decodedInstruction;
    }

    private void execute(InstructionDecoded instruction) {
        final int opcode = instruction.getOpcode();
        final int rd = instruction.getRd();
        final int rs1 = instruction.getRs1();
        final int rs2 = instruction.getRs2();
        final int func3 = instruction.getFunc3();
        final int func7 = instruction.getFunc7();
        final int imm_i = instruction.getImm_i();
        final int imm_s = instruction.getImm_s();
        final int imm_b = instruction.getImm_b();
        final int imm_u = instruction.getImm_u();
        final int imm_j = instruction.getImm_j();
        
        // Ensure x0 is always 0
        x[0] = 0;
        switch (opcode) {
            // R-type instructions
            case 0b0110011: // R-type
                switch (func3) {
                    case 0b000: // ADD/SUB
                        if (func7 == 0) {
                            x[rd] = x[rs1] + x[rs2]; // ADD
                        } else if (func7 == 0b0100000) {
                            x[rd] = x[rs1] - x[rs2]; // SUB
                        }
                        break;
                    case 0b001: // SLL
                        x[rd] = x[rs1] << (x[rs2] & 0x1F);
                        break;
                    case 0b010: // SLT
                        x[rd] = (x[rs1] < x[rs2]) ? 1 : 0;
                        break;
                    case 0b011: // SLTU
                        x[rd] = (Integer.compareUnsigned(x[rs1], x[rs2]) < 0) ? 1 : 0;
                        break;
                    case 0b100: // XOR
                        x[rd] = x[rs1] ^ x[rs2];
                        break;
                    case 0b101: // SRL/SRA
                        if (func7 == 0) {
                            x[rd] = x[rs1] >>> (x[rs2] & 0x1F); // SRL
                        } else if (func7 == 0b0100000) {
                            x[rd] = x[rs1] >> (x[rs2] & 0x1F); // SRA
                        }
                        break;
                    case 0b110: // OR
                        x[rd] = x[rs1] | x[rs2];
                        break;
                    case 0b111: // AND
                        x[rd] = x[rs1] & x[rs2];
                        break;
                }
                break;

            // I-type instructions
            case 0b0010011: // I-type ALU instructions
                switch (func3) {
                    case 0b000: // ADDI
                        x[rd] = x[rs1] + imm_i;
                        break;
                    case 0b001: // SLLI
                        x[rd] = x[rs1] << (imm_i & 0x1F);
                        break;
                    case 0b010: // SLTI
                        x[rd] = (x[rs1] < imm_i) ? 1 : 0;
                        break;
                    case 0b011: // SLTIU
                        // Use direct comparison for better performance
                        int xrs1 = x[rs1];
                        x[rd] = ((xrs1 < 0) != (imm_i < 0)) ? 
                                ((xrs1 < 0) ? 0 : 1) : 
                                ((xrs1 < imm_i) ? 1 : 0);
                        break;
                    case 0b100: // XORI
                        x[rd] = x[rs1] ^ imm_i;
                        break;
                    case 0b101: // SRLI, SRAI
                        int shamt = imm_i & 0x1F;
                        if ((imm_i & 0xFE0) == 0) { // SRLI
                            x[rd] = x[rs1] >>> shamt;
                        } else if ((imm_i & 0xFE0) == 0x400) { // SRAI
                            x[rd] = x[rs1] >> shamt;
                        }
                        break;
                    case 0b110: // ORI
                        x[rd] = x[rs1] | imm_i;
                        break;
                    case 0b111: // ANDI
                        x[rd] = x[rs1] & imm_i;
                        break;
                }
                break;

            // Load instructions
            case 0b0000011: // LOAD
                int address = mapAddress(x[rs1] + imm_i);
                if (!checkUARTAddress(address)) {
                    ;// If address is in data segment
                    if (address < MemoryManager.DATA_START) {
                        int temp = address + MemoryManager.RODATA_START;
                        if (temp < MemoryManager.DATA_START) {
                            address = MemoryManager.DATA_START + address;
                        } else {
                            address += MemoryManager.TEXT_START;
                        }
                    }
                }
                try {
                    switch (func3) {
                        case 0b000: // LB - load byte with sign extension
                            byte b = memory.readByte(address);
                            x[rd] = b; // Java automatically sign-extends byte to int
                            break;
                        case 0b001: // LH - load half-word with sign extension
                            short h = memory.readHalfWord(address);
                            x[rd] = h; // Java automatically sign-extends short to int
                            break;
                        case 0b010: // LW - load word
                            x[rd] = memory.readWord(address);
                            break;
                        case 0b100: // LBU - load byte unsigned
                            x[rd] = memory.readByte(address) & 0xFF;
                            break;
                        case 0b101: // LHU - load half-word unsigned
                            x[rd] = memory.readHalfWord(address) & 0xFFFF;
                            break;
                    }
                } catch (MemoryAccessException e) {
                    // Handle memory access exception
                    throw new RuntimeException("Memory access error during load", e);
                }
                break;

            // Store instructions
            case 0b0100011: // STORE
                address = mapAddress(x[rs1] + imm_s);
                if (!checkUARTAddress(address)) {
                    if (address + MemoryManager.DATA_START < MemoryManager.HEAP_START) {
                        address = MemoryManager.DATA_START + address;
                    }
                }
                try {
                    switch (func3) {
                        case 0b000: // SB - store byte
                            memory.writeByte(address, (byte) (x[rs2] & 0xFF));
                            break;
                        case 0b001: // SH - store half-word
                            memory.writeHalfWord(address, (short) (x[rs2] & 0xFFFF));
                            break;
                        case 0b010: // SW - store word
                            memory.writeWord(address, x[rs2]);
                            break;
                    }
                } catch (MemoryAccessException e) {
                    throw new RuntimeException("Memory access error during store", e);
                }
                break;

            // Branch instructions
            case 0b1100011: // BRANCH
                boolean takeBranch = false;
                switch (func3) {
                    case 0b000: // BEQ
                        takeBranch = (x[rs1] == x[rs2]);
                        break;
                    case 0b001: // BNE
                        takeBranch = (x[rs1] != x[rs2]);
                        break;
                    case 0b100: // BLT
                        takeBranch = (x[rs1] < x[rs2]);
                        break;
                    case 0b101: // BGE
                        takeBranch = (x[rs1] >= x[rs2]);
                        break;
                    case 0b110: // BLTU
                        // Optimized unsigned comparison
                        int xrs1 = x[rs1];
                        int xrs2 = x[rs2];
                        takeBranch = ((xrs1 < 0) != (xrs2 < 0)) ? 
                                    (xrs1 < 0) : (xrs1 < xrs2);
                        break;
                    case 0b111: // BGEU
                        // Optimized unsigned comparison
                        xrs1 = x[rs1];
                        xrs2 = x[rs2];
                        takeBranch = ((xrs1 < 0) != (xrs2 < 0)) ? 
                                    (xrs2 < 0) : (xrs1 >= xrs2);
                        break;
                }
                if (takeBranch) {
                    pc += imm_b - INSTRUCTION_SIZE; // Subtract INSTRUCTION_SIZE because pc was already incremented in fetch
                    
                    // Check for infinite loops
                    if (pc == lastPC) {
                        loopCount++;
                        if (loopCount > LOOP_THRESHOLD) {
                            System.out.println("Infinite loop detected at PC: 0x" + Integer.toHexString(pc));
                            System.out.println("Program halted after " + LOOP_THRESHOLD + " iterations");
                            this.running = false;
                            return;
                        }
                    }
                }
                break;

            // Jump instructions
            case 0b1101111: // JAL
                if (rd != 0) {
                    x[rd] = pc;
                }
                pc += imm_j - INSTRUCTION_SIZE;
                break;

            case 0b1100111: // JALR
                int temp = pc;
                pc = (x[rs1] + imm_i) & ~1;
                if (rd != 0) {
                    x[rd] = temp;
                }
                break;

            // LUI and AUIPC
            case 0b0110111: // LUI
                x[rd] = imm_u;
                break;

            case 0b0010111: // AUIPC
                x[rd] = pc - INSTRUCTION_SIZE + imm_u;
                break;

            case 0b1110011: // SYSTEM
                if (func3 == 0) {
                    if (imm_i == 0) { // ECALL
                        if (x[17] == 93) { // Exit syscall
                            this.running = false;
                            System.out.println("Program exited with code: " + x[10]);
                        }
                    } else if (imm_i == 1) { // EBREAK
                        handleQemuSemihosting();
                    }
                }
                break;
        }

    }

    private void handleQemuSemihosting() {
        if (x[17] == 93) { // Exit operation
            this.running = false;
            System.out.println("Program exited with code: " + x[10]);
        }
    }

    private void displayRegisters() {
        String[] regNames = {
                "zero", "ra", "sp", "gp", "tp", "t0", "t1", "t2",
                "s0/fp", "s1", "a0", "a1", "a2", "a3", "a4", "a5",
                "a6", "a7", "s2", "s3", "s4", "s5", "s6", "s7",
                "s8", "s9", "s10", "s11", "t3", "t4", "t5", "t6"
        };

        System.out.println("\nRegister Values:");
        System.out.println("PC: 0x" + String.format("%08X", pc));

        for (int i = 0; i < x.length; i++) {
            if (x[i] != 0) { // Only show non-zero registers to reduce clutter
                System.out.printf("x%d (%s): 0x%08X\n", i, regNames[i], x[i]);
            }
        }
        System.out.println("------------------------");
    }

    private int mapAddress(int virtualAddr) {
        // Handle UART addresses
        if (checkUARTAddress(virtualAddr)) {
            return virtualAddr;
        }

        // Handle addresses in 0x80000000+ range
        if (virtualAddr < 0 || virtualAddr >= 0x80000000L) {
            // Convert to unsigned using long to handle overflow
            long unsignedAddr = virtualAddr & 0xFFFFFFFFL;

            // Calculate offset from base (0x80000000)
            int offset = (int) (unsignedAddr - 0x80000000L);

            // Map data accesses to data segment instead of text segment

            /*
             * if (offset >= 0x100000) { // If offset is beyond first 4KB
             * return MemoryManager.DATA_START + (offset - 0x100000);
             * }
             */

            // Map first 4KB to text segment (for instruction fetch)
            // return MemoryManager.TEXT_START + offset;
            return offset;
        }
        return virtualAddr;
    }

    // Test-purpose only methods
    public void setRegister(int index, int value) {
        if (index >= 0 && index < x.length) {
            x[index] = value;
        }
    }

    public int getRegister(int index) {
        if (index >= 0 && index < x.length) {
            return x[index];
        }
        return 0;
    }

    public int getPc() {
        return pc;
    }

    public void executeTest(InstructionDecoded inst) {
        execute(inst);
    }

    public int mapAddressTest(int i) {
        return mapAddress(i);
    }

    /**
     * Find bytes with value 13 or 12 in a ByteBuffer
     * @param buffer The ByteBuffer to search
     */
    public void find13And12(ByteBuffer buffer) {
        ByteBuffer duplicate = buffer.duplicate();
        duplicate.clear(); // Reset position to start
        int limit = duplicate.limit();
        
        for (int i = 0; i < limit; i++) {
            byte value = duplicate.get(i);
            if (value == 13 || value == 12) {
                if (value == 13) {
                    System.out.println("Found 13 at index " + i);
                } else {
                    System.out.println("Found 12 at index " + i);
                }
            }
        }
    }

    public boolean checkUARTAddress(int virtualAddr) {
        if (virtualAddr >= MemoryManager.UART_BASE &&
                virtualAddr < MemoryManager.UART_BASE + 0x1000) {
            return true;
        }
        return false;
    }
}

package cse311;

import java.util.Scanner;
import java.util.logging.MemoryHandler;

import com.corundumstudio.socketio.SocketIOClient;

public class RV32iCpu {

    private int[] x = new int[32];
    private int lastPC = -1;
    private int lastPCBranch = -1;
    private int loopCountBranch = 0;
    private int loopCount = 0;
    private int pc = 0;
    // private int[] instruction;
    private static final int INSTRUCTION_SIZE = 4; // 32-bit instructions

    private MemoryManager memory;
    private Scanner reader;
    private Thread cpuThread;
    private boolean running = false;
    private static final int LOOP_THRESHOLD = 1000; // Maximum times to execute same instruction
    private InputThread input;
    private SocketIOClient client;

    public RV32iCpu(MemoryManager memory) {
        this.memory = memory;
        input = new InputThread();
    }

    public RV32iCpu(MemoryManager memory, SocketIOClient client) {
        this.memory = memory;
        this.client = client;
        input = new InputThread();
    }

    public void setProgramCounterEntryPoint(int entryPoint) {
        this.pc = entryPoint;
    }

    public void turnOn() {
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
        this.running = true;
        this.cpuThread.start();

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
        // Read 32-bit instruction from memory at PC
        int instruction = 0;

        // Read 4 bytes and combine them
        try {
            byte byte0 = memory.readByte(pc);
            byte byte1 = memory.readByte(pc + 1);
            byte byte2 = memory.readByte(pc + 2);
            byte byte3 = memory.readByte(pc + 3);

            // Combine bytes into 32-bit instruction
            instruction = (byte3 & 0xFF) << 24
                    | (byte2 & 0xFF) << 16
                    | (byte1 & 0xFF) << 8
                    | (byte0 & 0xFF);

            // Increment PC by instruction size (4 bytes)
            pc += INSTRUCTION_SIZE;

        } catch (Exception e) {
            throw new MemoryAccessException("Failed to fetch instruction at PC: " + pc);
        }

        return instruction;
    }

    private InstructionDecoded decode(int instructionInt) {
        // Combine the bytes into a 32-bit instruction
        /*
         * int instructionInt = (instruction[3] & 0xFF) << 24
         * | (instruction[2] & 0xFF) << 16
         * | (instruction[1] & 0xFF) << 8
         * | (instruction[0] & 0xFF);
         */
        InstructionDecoded instruction = new InstructionDecoded();

        // Extract instruction fields based on RISC-V RV32I format
        int opcode = instructionInt & 0x7F; // bits 0-6
        instruction.setOpcode(opcode);
        int rd = (instructionInt >> 7) & 0x1F; // bits 7-11
        instruction.setRd(rd);
        int func3 = (instructionInt >> 12) & 0x7; // bits 12-14
        instruction.setFunc3(func3);
        int rs1 = (instructionInt >> 15) & 0x1F; // bits 15-19
        instruction.setRs1(rs1);
        int rs2 = (instructionInt >> 20) & 0x1F; // bits 20-24
        instruction.setRs2(rs2);
        int func7 = (instructionInt >> 25) & 0x7F; // bits 25-31
        instruction.setFunc7(func7);

        // Immediate values for different instruction formats
        // I-type: Sign extended 12-bit immediate
        int imm_i = ((instructionInt >> 20) << 20) >> 20;
        instruction.setImm_i(imm_i);

        // S-type: Sign extended 12-bit immediate
        int imm_s = (((instructionInt >> 25) << 5) | ((instructionInt >> 7) & 0x1F));
        imm_s = (imm_s << 20) >> 20; // Sign extend
        instruction.setImm_s(imm_s);

        // B-type: Sign extended 13-bit immediate
        int imm_b = (((instructionInt >> 31) << 12) // imm[12]
                |
                ((instructionInt >> 7) & 0x1) << 11) // imm[11]
                |
                ((instructionInt >> 25) & 0x3F) << 5 // imm[10:5]
                |
                ((instructionInt >> 8) & 0xF) << 1; // imm[4:1]
        imm_b = (imm_b << 19) >> 19; // Sign extend
        instruction.setImm_b(imm_b);

        // U-type: 20-bit immediate, shifted left by 12
        int imm_u = instructionInt & 0xFFFFF000;
        instruction.setImm_u(imm_u);

        // J-type: Sign extended 21-bit immediate
        int imm_j = (((instructionInt >> 31) << 20) // imm[20]
                |
                ((instructionInt >> 12) & 0xFF) << 12 // imm[19:12]
                |
                ((instructionInt >> 20) & 0x1) << 11 // imm[11]
                |
                ((instructionInt >> 21) & 0x3FF) << 1); // imm[10:1]
        imm_j = (imm_j << 11) >> 11; // Sign extend
        instruction.setImm_j(imm_j);
        return instruction;

    }

    private void execute(InstructionDecoded instruction) {
        int opcode = instruction.getOpcode();
        int rd = instruction.getRd();
        int rs1 = instruction.getRs1();
        int rs2 = instruction.getRs2();
        int func3 = instruction.getFunc3();
        int func7 = instruction.getFunc7();
        int imm_i = instruction.getImm_i();
        int imm_s = instruction.getImm_s();
        int imm_b = instruction.getImm_b();
        int imm_u = instruction.getImm_u();
        int imm_j = instruction.getImm_j();
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
            case 0b0010011: // I-type ALU
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
                        x[rd] = (Integer.compareUnsigned(x[rs1], imm_i) < 0) ? 1 : 0;
                        break;
                    case 0b100: // XORI
                        x[rd] = x[rs1] ^ imm_i;
                        break;
                    case 0b101: // SRLI/SRAI
                        if ((imm_i & 0xFE0) == 0) {
                            x[rd] = x[rs1] >>> (imm_i & 0x1F); // SRLI
                        } else if ((imm_i & 0xFE0) == 0x400) {
                            x[rd] = x[rs1] >> (imm_i & 0x1F); // SRAI
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
                        case 0b000: // LB
                            x[rd] = memory.readByte(address);
                            break;
                        case 0b001: // LH
                            x[rd] = memory.readHalfWord(address);
                            break;
                        case 0b010: // LW
                            x[rd] = memory.readWord(address);
                            break;
                        case 0b100: // LBU
                            x[rd] = memory.readByte(address) & 0xFF;
                            break;
                        case 0b101: // LHU
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
                        case 0b000: // SB
                            memory.writeByte(address, (byte) x[rs2]);
                            break;
                        case 0b001: // SH
                            memory.writeHalfWord(address, (short) x[rs2]);
                            break;
                        case 0b010: // SW
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
                        takeBranch = (Integer.compareUnsigned(x[rs1], x[rs2]) < 0);
                        break;
                    case 0b111: // BGEU
                        takeBranch = (Integer.compareUnsigned(x[rs1], x[rs2]) >= 0);
                        break;
                }
                if (takeBranch) {
                    pc += imm_b - INSTRUCTION_SIZE; // Subtract INSTRUCTION_SIZE because pc was already incremented in
                                                    // fetch
                    /*
                     * if (lastPCBranch == -1) {
                     * lastPCBranch = pc;
                     * } else if (pc == lastPCBranch) {
                     * loopCountBranch++;
                     * } else {
                     * loopCountBranch = 0;
                     * lastPCBranch = -1;
                     * }
                     * if (loopCountBranch > 20) {
                     * loopCountBranch = 0;
                     * lastPCBranch = -1;
                     * System.out.println("Getting input");
                     * memory.getInput(reader.nextLine());
                     * }
                     */
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

    public boolean checkUARTAddress(int virtualAddr) {
        if (virtualAddr >= MemoryManager.UART_BASE &&
                virtualAddr < MemoryManager.UART_BASE + 0x1000) {
            return true;
        }
        return false;
    }

    public MemoryManager getMemoryManager() {
        return memory;
    }

}

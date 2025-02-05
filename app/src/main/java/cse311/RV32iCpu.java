package cse311;

public class RV32iCpu {

    private int[] x = new int[32];
    private int pc = 0;
    private int[] instruction;
    private static final int INSTRUCTION_SIZE = 4; // 32-bit instructions

    private MemoryManager memory;
    private Thread cpuThread;
    private boolean running = false;

    public RV32iCpu(MemoryManager memory) {
        this.memory = memory;
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
        // Fetch the instruction from memory at the address in the pc register
        int instructionFetched = fetch();
        decode(instructionFetched);
        // execute(); // Viet them update cho pc, cpu sau nay
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

    private void decode(int instructionInt) {
        // Combine the bytes into a 32-bit instruction
        /*
         * int instructionInt = (instruction[3] & 0xFF) << 24
         * | (instruction[2] & 0xFF) << 16
         * | (instruction[1] & 0xFF) << 8
         * | (instruction[0] & 0xFF);
         */

        // Extract instruction fields based on RISC-V RV32I format
        int opcode = instructionInt & 0x7F; // bits 0-6
        int rd = (instructionInt >> 7) & 0x1F; // bits 7-11
        int func3 = (instructionInt >> 12) & 0x7; // bits 12-14
        int rs1 = (instructionInt >> 15) & 0x1F; // bits 15-19
        int rs2 = (instructionInt >> 20) & 0x1F; // bits 20-24
        int func7 = (instructionInt >> 25) & 0x7F; // bits 25-31

        // Immediate values for different instruction formats
        // I-type: Sign extended 12-bit immediate
        int imm_i = ((instructionInt >> 20) << 20) >> 20;

        // S-type: Sign extended 12-bit immediate
        int imm_s = (((instructionInt >> 25) << 5) | ((instructionInt >> 7) & 0x1F));
        imm_s = (imm_s << 20) >> 20; // Sign extend

        // B-type: Sign extended 13-bit immediate
        int imm_b = (((instructionInt >> 31) << 12) // imm[12]
                |
                ((instructionInt >> 7) & 0x1) << 11) // imm[11]
                |
                ((instructionInt >> 25) & 0x3F) << 5 // imm[10:5]
                |
                ((instructionInt >> 8) & 0xF) << 1; // imm[4:1]
        imm_b = (imm_b << 19) >> 19; // Sign extend

        // U-type: 20-bit immediate, shifted left by 12
        int imm_u = instructionInt & 0xFFFFF000;

        // J-type: Sign extended 21-bit immediate
        int imm_j = (((instructionInt >> 31) << 20) // imm[20]
                |
                ((instructionInt >> 12) & 0xFF) << 12 // imm[19:12]
                |
                ((instructionInt >> 20) & 0x1) << 11 // imm[11]
                |
                ((instructionInt >> 21) & 0x3FF) << 1); // imm[10:1]
        imm_j = (imm_j << 11) >> 11; // Sign extend

    }

    private void execute(int opcode, int rd, int rs1, int rs2, int func3, int func7,
            int imm_i, int imm_s, int imm_b, int imm_u, int imm_j) {
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
                    case 0b010: // SLTI
                        x[rd] = (x[rs1] < imm_i) ? 1 : 0;
                        break;
                    case 0b011: // SLTIU
                        x[rd] = (Integer.compareUnsigned(x[rs1], imm_i) < 0) ? 1 : 0;
                        break;
                    case 0b100: // XORI
                        x[rd] = x[rs1] ^ imm_i;
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
                int address = x[rs1] + imm_i;
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
                address = x[rs1] + imm_s;
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
                }
                break;

            // Jump instructions
            case 0b1101111: // JAL
                x[rd] = pc;
                pc += imm_j - INSTRUCTION_SIZE;
                break;

            case 0b1100111: // JALR
                int temp = pc;
                pc = (x[rs1] + imm_i) & ~1;
                x[rd] = temp;
                break;

            // LUI and AUIPC
            case 0b0110111: // LUI
                x[rd] = imm_u;
                break;

            case 0b0010111: // AUIPC
                x[rd] = pc - INSTRUCTION_SIZE + imm_u;
                break;
        }
    }
}

package cse311;

public class RV32iCpu {

    private int[] x = new int[32];
    private int pc = 0;
    private byte[] instruction;
    private static final int INSTRUCTION_SIZE = 4; // 32-bit instructions

    private SimpleMemory memory;
    private Thread cpuThread;
    private boolean running = false;

    public RV32iCpu(SimpleMemory memory) {
        this.memory = memory;
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

    public void turnOff() {
        this.running = false;
        try {
            if (cpuThread != null) {
                cpuThread.join();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void fetchExecuteCycle() throws Exception {
        // Fetch the instruction from memory at the address in the pc register
        fetch();
        decode();
        execute(); // Viet them update cho pc, cpu sau nay
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

    private void decode() {
        // Combine the bytes into a 32-bit instruction
        int instructionInt = (instruction[3] & 0xFF) << 24
                | (instruction[2] & 0xFF) << 16
                | (instruction[1] & 0xFF) << 8
                | (instruction[0] & 0xFF);

        // Extract instruction fields based on RISC-V RV32I format
        int opcode = instructionInt & 0x7F;              // bits 0-6
        int rd = (instructionInt >> 7) & 0x1F;           // bits 7-11
        int func3 = (instructionInt >> 12) & 0x7;       // bits 12-14
        int rs1 = (instructionInt >> 15) & 0x1F;         // bits 15-19
        int rs2 = (instructionInt >> 20) & 0x1F;         // bits 20-24
        int func7 = (instructionInt >> 25) & 0x7F;      // bits 25-31

        // Immediate values for different instruction formats
        // I-type: Sign extended 12-bit immediate
        int imm_i = ((instructionInt >> 20) << 20) >> 20;

        // S-type: Sign extended 12-bit immediate
        int imm_s = (((instructionInt >> 25) << 5) | ((instructionInt >> 7) & 0x1F));
        imm_s = (imm_s << 20) >> 20; // Sign extend

        // B-type: Sign extended 13-bit immediate
        int imm_b = (((instructionInt >> 31) << 12)    // imm[12]
                | 
                ((instructionInt >> 7) & 0x1) << 11)   // imm[11]
                | 
                ((instructionInt >> 25) & 0x3F) << 5   // imm[10:5]
                | 
                ((instructionInt >> 8) & 0xF) << 1;    // imm[4:1]
        imm_b = (imm_b << 19) >> 19; // Sign extend

        // U-type: 20-bit immediate, shifted left by 12
        int imm_u = instructionInt & 0xFFFFF000;

        // J-type: Sign extended 21-bit immediate
        int imm_j = (((instructionInt >> 31) << 20)     // imm[20]
                | 
                ((instructionInt >> 12) & 0xFF) << 12   // imm[19:12]
                | 
                ((instructionInt >> 20) & 0x1) << 11    // imm[11]
                | 
                ((instructionInt >> 21) & 0x3FF) << 1); // imm[10:1]
        imm_j = (imm_j << 11) >> 11; // Sign extend

    }

    private void execute() {
    }
}

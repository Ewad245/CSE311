package cse311;

public class RV32iCpu {
    private byte[] x = new byte[32];
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
            instruction = (byte3 & 0xFF) << 24 |
                    (byte2 & 0xFF) << 16 |
                    (byte1 & 0xFF) << 8 |
                    (byte0 & 0xFF);

            // Increment PC by instruction size (4 bytes)
            pc += INSTRUCTION_SIZE;

        } catch (Exception e) {
            throw new MemoryAccessException("Failed to fetch instruction at PC: " + pc);
        }

        return instruction;
    }

    private void decode() {
    }

    private void execute() {
    }
}

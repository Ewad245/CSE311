package cse311;

import cse311.RV32iCpu;

public class RV32iComputer {
    private RV32iCpu cpu;
    private SimpleMemory memory;

    public RV32iComputer(int memSize) {
        memory = new SimpleMemory(memSize);
        this.cpu = new RV32iCpu(memory);

    }


    public void initProgram(byte[] program) throws MemoryAccessException {
        memory.initializeMemory(0, program);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("RV32i Computer State:\n");
        sb.append("Memory Size: ").append(memory.getSize()).append(" bytes\n");
        // Add CPU state when available
        return sb.toString();
    }
}

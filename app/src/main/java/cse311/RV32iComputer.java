package cse311;

import cse311.RV32iCpu;

public class RV32iComputer {
    private RV32iCpu cpu;
    private SimpleMemory memory;

    public RV32iComputer(int memSize) {
        memory = new SimpleMemory(memSize);
        this.cpu = new RV32iCpu(memory);

    }

    public void initProgram(byte[] program) {
        memory.initializeMemory(0, program);
    }

    @Override
    public String toString() {
        // TODO Auto-generated method stub
        return super.toString();
    }
}

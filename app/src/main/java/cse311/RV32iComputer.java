package cse311;

import cse311.RV32iCpu;

public class RV32iComputer {
    private RV32iCpu cpu;
    private byte[] memory;

    public RV32iComputer(int memSize) {
        this.cpu = new RV32iCpu(memSize);
        this.mem = new byte[memSize];

    }

    public void initProgram(byte[] program) {
        for (int i = 0; i < program.length; i++) {
            this.memory[i] = program[i];
        }
    }

    @Override
    public String toString() {
        // TODO Auto-generated method stub
        return super.toString();
    }
}

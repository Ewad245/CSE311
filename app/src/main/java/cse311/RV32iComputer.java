package cse311;

import cse311.RV32iCpu;

public class RV32iComputer {
    private RV32iCpu cpu;
    private byte[] memory;

    public RV32iComputer(byte[] memSize) {
        this.cpu = new RV32iCpu(memSize);
        this.memory = new byte[memSize.length];

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

package cse311;

import cse311.RV32iCpu;

public class RV32iComputer {
    private RV32iCpu cpu;
    private MemoryManager memory;

    public RV32iComputer(int memSize) {
        memory = new MemoryManager(new SimpleMemory(memSize));
        this.cpu = new RV32iCpu(memory);
    }

    /**
     * Creates a new task with the specified entry point.
     * 
     * @param entryPoint The entry point (initial PC value)
     * @return The ID of the created task, or -1 if task creation failed
     */
    public int createTask(int entryPoint) {
        return cpu.createTask(entryPoint);
    }

    /**
     * Gets the CPU.
     * 
     * @return The CPU
     */
    public RV32iCpu getCpu() {
        return cpu;
    }

    /**
     * Gets the memory manager.
     * 
     * @return The memory manager
     */
    public MemoryManager getMemoryManager() {
        return memory;
    }

    @Override
    public String toString() {
        return "RV32iComputer [cpu=" + cpu + ", memory=" + memory + "]";
    }
}

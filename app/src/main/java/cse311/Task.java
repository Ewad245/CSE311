package cse311;

/**
 * Represents a task (process) in the simulated operating system.
 * Each task has its own program counter, register state, and stack.
 */
public class Task {
    private int id;
    private int pc;          // Program counter
    private int[] registers; // Register state
    private int stackBase;   // Base address of the task's stack
    private int stackSize;   // Size of the stack in bytes
    private boolean active;  // Whether the task is currently active

    /**
     * Creates a new task with the specified ID and stack size.
     * 
     * @param id The task ID
     * @param entryPoint The entry point (initial PC value)
     * @param stackSize The size of the task's stack in bytes
     * @param stackBase The base address of the task's stack
     */
    public Task(int id, int entryPoint, int stackSize, int stackBase) {
        this.id = id;
        this.pc = entryPoint;
        this.registers = new int[32]; // RV32I has 32 registers
        this.stackSize = stackSize;
        this.stackBase = stackBase;
        this.active = false;
        
        // Initialize stack pointer (x2) to the top of the stack
        this.registers[2] = stackBase + stackSize;
    }

    /**
     * Saves the current CPU state to this task.
     * 
     * @param cpu The CPU whose state should be saved
     */
    public void saveState(RV32iCpu cpu) {
        this.pc = cpu.getProgramCounter();
        System.arraycopy(cpu.getRegisters(), 0, this.registers, 0, 32);
    }

    /**
     * Restores this task's state to the CPU.
     * 
     * @param cpu The CPU to restore the state to
     */
    public void restoreState(RV32iCpu cpu) {
        cpu.setProgramCounter(this.pc);
        cpu.setRegisters(this.registers);
    }

    // Getters and setters
    public int getId() {
        return id;
    }

    public int getProgramCounter() {
        return pc;
    }

    public void setProgramCounter(int pc) {
        this.pc = pc;
    }

    public int[] getRegisters() {
        return registers;
    }

    public int getStackBase() {
        return stackBase;
    }

    public int getStackSize() {
        return stackSize;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
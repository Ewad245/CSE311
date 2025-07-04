package cse311;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class RV32iCpuTest {
    private RV32iCpu cpu;
    private MemoryManager memory;

    @BeforeEach
    void setUp() {
        SimpleMemory simpleMemory = new SimpleMemory(128 * 1024 * 1024);
        memory = new MemoryManager(simpleMemory);
        cpu = new RV32iCpu(memory);
    }

    // R-type instruction tests
    @Test
    void testAdd() {
        // ADD x1, x2, x3
        InstructionDecoded inst = new InstructionDecoded();
        inst.setOpcode(0b0110011);
        inst.setRd(1);
        inst.setFunc3(0b000);
        inst.setRs1(2);
        inst.setRs2(3);
        inst.setFunc7(0b0000000);

        // Set initial register values
        cpu.setRegister(2, 5);
        cpu.setRegister(3, 3);

        cpu.executeTest(inst);
        assertEquals(8, cpu.getRegister(1), "ADD failed");
    }

    @Test
    void testSub() {
        // SUB x1, x2, x3
        InstructionDecoded inst = new InstructionDecoded();
        inst.setOpcode(0b0110011);
        inst.setRd(1);
        inst.setFunc3(0b000);
        inst.setRs1(2);
        inst.setRs2(3);
        inst.setFunc7(0b0100000);

        cpu.setRegister(2, 10);
        cpu.setRegister(3, 3);

        cpu.executeTest(inst);
        assertEquals(7, cpu.getRegister(1), "SUB failed");
    }

    // I-type instruction tests
    @Test
    void testAddi() {
        // ADDI x1, x2, 5
        InstructionDecoded inst = new InstructionDecoded();
        inst.setOpcode(0b0010011);
        inst.setRd(1);
        inst.setFunc3(0b000);
        inst.setRs1(2);
        inst.setImm_i(5);

        cpu.setRegister(2, 10);

        cpu.executeTest(inst);
        assertEquals(15, cpu.getRegister(1), "ADDI failed");
    }

    // Load instruction tests
    @Test
    void testLw() throws MemoryAccessException {
        // LW x1, 0(x2)
        InstructionDecoded inst = new InstructionDecoded();
        inst.setOpcode(0b0000011);
        inst.setRd(1);
        inst.setFunc3(0b010);
        inst.setRs1(2);
        inst.setImm_i(0);

        // Use a virtual address that will be correctly mapped
        int virtualAddr = 0;
        
        // Set the virtual address in register
        cpu.setRegister(2, virtualAddr);

        // Calculate the physical address that will be used after mapping
        // The CPU's execute method for LW will map the virtual address to a physical address
        // by adding MemoryManager.DATA_START
        int physicalAddr = MemoryManager.DATA_START + virtualAddr;
        
        // Write test value to the physical address
        memory.writeWord(physicalAddr, 42);

        // Execute the instruction
        cpu.executeTest(inst);
        
        // Verify the value was loaded into register x1
        assertEquals(42, cpu.getRegister(1), "LW failed");
    }

    // Store instruction tests
    @Test
    void testSw() throws MemoryAccessException {
        // SW x1, 0(x2)
        InstructionDecoded inst = new InstructionDecoded();
        inst.setOpcode(0b0100011);
        inst.setRs2(1);
        inst.setFunc3(0b010);
        inst.setRs1(2);
        inst.setImm_s(0);

        // Use a virtual address that will be correctly mapped
        int virtualAddr = 100;
        
        // Calculate the physical address that will be used after mapping
        // The CPU's execute method for SW will map the virtual address to a physical address
        // by adding MemoryManager.DATA_START
        int physicalAddr = MemoryManager.DATA_START + virtualAddr;
        
        // Set registers
        cpu.setRegister(1, 42); // Value to store
        cpu.setRegister(2, virtualAddr); // Virtual base address

        // Execute the instruction
        cpu.executeTest(inst);

        // Verify stored value at physical address
        int storedValue = memory.readWord(physicalAddr);
        assertEquals(42, storedValue, "SW failed");
    }

    // Branch instruction tests
    @Test
    void testBeq() {
        // BEQ x1, x2, 8
        InstructionDecoded inst = new InstructionDecoded();
        inst.setOpcode(0b1100011);
        inst.setFunc3(0b000);
        inst.setRs1(1);
        inst.setRs2(2);
        inst.setImm_b(8);

        cpu.setRegister(1, 5);
        cpu.setRegister(2, 5);
        int initialPc = cpu.getPc();

        cpu.executeTest(inst);
        assertEquals(initialPc + 8, cpu.getPc() + 4, "BEQ failed");
    }

    // Jump instruction tests
    @Test
    void testJal() {
        // JAL x1, 16
        InstructionDecoded inst = new InstructionDecoded();
        inst.setOpcode(0b1101111);
        inst.setRd(1);
        inst.setImm_j(16);

        int initialPc = cpu.getPc();
        cpu.executeTest(inst);

        assertEquals(initialPc, cpu.getRegister(1), "JAL return address failed");
        assertEquals(initialPc + 16, cpu.getPc() + 4, "JAL target address failed");
    }

    // Upper immediate instruction tests
    @Test
    void testLui() {
        // LUI x1, 0x12345
        InstructionDecoded inst = new InstructionDecoded();
        inst.setOpcode(0b0110111);
        inst.setRd(1);
        inst.setImm_u(0x12345000);

        cpu.executeTest(inst);
        assertEquals(0x12345000, cpu.getRegister(1), "LUI failed");
    }
}
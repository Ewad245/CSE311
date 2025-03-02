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
        // LW x1, 4(x2)
        InstructionDecoded inst = new InstructionDecoded();
        inst.setOpcode(0b0000011);
        inst.setRd(1);
        inst.setFunc3(0b010);
        inst.setRs1(2);
        inst.setImm_i(0);

        // Use virtual address that maps directly to DATA_START
        int virtualBaseAddr = 0x82010000;

        // Calculate mapped physical address using CPU's mapping
        int mappedAddr = cpu.mapAddressTest(virtualBaseAddr);

        // Set base address in register
        cpu.setRegister(2, mappedAddr);

        // Write test value to mapped physical address
        memory.writeWord(mappedAddr, 42);

        // Debug output
        System.out.printf("Virtual base: 0x%08X\n", virtualBaseAddr);
        System.out.printf("Virtual address (base+4): 0x%08X\n", virtualBaseAddr + 4);
        System.out.printf("Mapped physical address: 0x%08X\n", mappedAddr);
        System.out.printf("Value at physical address: %d\n", memory.readWord(mappedAddr));

        cpu.executeTest(inst);
        assertEquals(42, cpu.getRegister(1), "LW failed");
    }

    // Store instruction tests
    @Test
    void testSw() throws MemoryAccessException {
        // SW x1, 4(x2)
        InstructionDecoded inst = new InstructionDecoded();
        inst.setOpcode(0b0100011);
        inst.setRs2(1);
        inst.setFunc3(0b010);
        inst.setRs1(2);
        inst.setImm_s(0);

        // Use virtual address in data segment range
        int virtualBaseAddr = 0x82010000;

        // Calculate mapped physical address using CPU's mapping
        int mappedAddr = cpu.mapAddressTest(virtualBaseAddr);

        // Set registers
        cpu.setRegister(1, 42); // Value to store
        cpu.setRegister(2, mappedAddr); // Base address

        // Debug output
        System.out.printf("Virtual base: 0x%08X\n", virtualBaseAddr);
        System.out.printf("Virtual address (base+4): 0x%08X\n", virtualBaseAddr + 4);
        System.out.printf("Mapped physical address: 0x%08X\n", mappedAddr);

        cpu.executeTest(inst);

        // Verify stored value at mapped address
        int storedValue = memory.readWord(mappedAddr);
        System.out.printf("Value at physical address: %d\n", storedValue);
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
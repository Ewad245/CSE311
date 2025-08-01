# RISC-V CSR and Privilege Mode Support

This document describes the implementation of Control and Status Registers (CSRs) and privilege modes in the RV32i CPU simulator.

## Overview

The implementation adds support for:

1. RISC-V privilege levels (Machine, Supervisor, User)
2. Control and Status Registers (CSRs)
3. CSR instructions (CSRRW, CSRRS, CSRRC, CSRRWI, CSRRSI, CSRRCI)
4. Exception handling and privilege mode transitions
5. Memory protection based on privilege level

## Privilege Levels

The RISC-V architecture defines three privilege levels:

- **Machine Mode (M-mode)**: The highest privilege level with full access to all hardware resources.
- **Supervisor Mode (S-mode)**: An intermediate privilege level for operating systems.
- **User Mode (U-mode)**: The lowest privilege level for user applications.

The CPU starts in Machine Mode and can switch to lower privilege levels through the MRET and SRET instructions.

## Control and Status Registers (CSRs)

The implementation includes the following key CSRs:

### Machine-level CSRs

- `mstatus` (0x300): Machine status register
- `misa` (0x301): Machine ISA register
- `medeleg` (0x302): Machine exception delegation register
- `mideleg` (0x303): Machine interrupt delegation register
- `mie` (0x304): Machine interrupt-enable register
- `mtvec` (0x305): Machine trap-handler base address
- `mepc` (0x341): Machine exception program counter
- `mcause` (0x342): Machine trap cause
- `mtval` (0x343): Machine trap value

### Supervisor-level CSRs

- `sstatus` (0x100): Supervisor status register
- `sie` (0x104): Supervisor interrupt-enable register
- `stvec` (0x105): Supervisor trap handler base address
- `sepc` (0x141): Supervisor exception program counter
- `scause` (0x142): Supervisor trap cause
- `stval` (0x143): Supervisor trap value
- `satp` (0x180): Supervisor address translation and protection

## CSR Instructions

The implementation supports the following CSR instructions:

- **CSRRW**: CSR Read and Write
- **CSRRS**: CSR Read and Set
- **CSRRC**: CSR Read and Clear
- **CSRRWI**: CSR Read and Write Immediate
- **CSRRSI**: CSR Read and Set Immediate
- **CSRRCI**: CSR Read and Clear Immediate

## Exception Handling

When an exception occurs (e.g., illegal instruction, memory access fault), the CPU:

1. Saves the current PC to `mepc` or `sepc`
2. Sets the cause in `mcause` or `scause`
3. Sets the trap value in `mtval` or `stval`
4. Updates the status register (`mstatus` or `sstatus`)
5. Switches to the appropriate privilege mode
6. Jumps to the trap handler address in `mtvec` or `stvec`

Exceptions can be delegated from M-mode to S-mode using the `medeleg` register.

## Memory Protection

The implementation includes a simple memory protection scheme based on privilege levels:

- User mode can only access user memory regions
- Supervisor mode has more access but is still restricted from machine-only regions
- Machine mode has full access to all memory

Additionally, when virtual memory translation is enabled (via the `satp` register), memory accesses go through address translation and permission checking.

## Usage Example

To use the CSR and privilege mode features:

1. Initialize the CPU in Machine mode
2. Set up trap handlers in `mtvec` and `stvec`
3. Configure exception delegation in `medeleg`
4. Use MRET/SRET instructions to switch privilege modes
5. Use CSR instructions to read and write CSRs

## Testing

The implementation includes unit tests in `RV32iCpuCSRTest.java` that verify:

- CSR read and write operations
- CSR immediate instructions
- Privilege mode switching
- Exception handling

## Future Enhancements

Possible future enhancements include:

- Full virtual memory support with page table walks
- Physical Memory Protection (PMP)
- Interrupt handling
- More comprehensive memory protection
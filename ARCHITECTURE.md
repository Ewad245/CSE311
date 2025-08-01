# RISC-V 32-bit Emulator Architecture

## Overview

This project is a RISC-V 32-bit (RV32i) computer emulator/VM written in Java. It simulates a complete computer system with memory management, CPU, and I/O capabilities.

## System Architecture

### Memory Layout

- **Text Segment**: `0x10000` and up (executable code)
- **RO Data**: `0x1010000` (read-only data)
- **Data Segment**: `0x2010000` (initialized data)
- **Heap**: `0x3010000` (grows upward)
- **Stack**: `0x7C00000` (grows downward)

### Core Components

#### 1. CPU (RV32iCpu.java)

- 32 general-purpose registers (x0-x31)
- Program counter (PC) management
- Instruction fetch-decode-execute cycle
- Support for base integer instruction set (RV32I)

#### 2. Memory Management (MemoryManager.java)

- Handles memory allocation and access
- Manages memory-mapped I/O
- Implements stack operations
- Enforces memory access permissions

#### 3. UART (Uart.java)

- Implements UART (Universal Asynchronous Receiver/Transmitter) functionality
- Handles console input/output
- Memory-mapped registers for communication

#### 4. Task Management (TaskManager.java, Task.java)

- Cooperative multitasking support
- Task creation and management
- Context switching between tasks

## Input/Output System

### UART Implementation

- **Base Address**: `0x10000000`
- **Registers**:
  - `UART_TX_DATA` (0x10000000): Write data to transmit
  - `UART_RX_DATA` (0x10000004): Read received data
  - `UART_STATUS` (0x10000008): Status register
  - `UART_CONTROL` (0x1000000C): Control register

### Input Handling

1. **InputThread** runs as a separate thread
2. Reads lines from `System.in`
3. Forwards input to MemoryManager via `getInput()`
4. Data is buffered in the UART's receive buffer
5. Programs can read input through memory-mapped UART registers

### System Calls

Supported syscalls for I/O and task management:

- `63 (read)`: Read from console input
- `64 (write)`: Write to console output
- `24 (yield)`: Cooperative multitasking
- `93 (exit)`: Terminate program

## ELF Loading

- Supports loading and executing RISC-V 32-bit ELF files
- Maps program sections to appropriate memory regions
- Handles program entry point

## Execution Flow

1. System initializes with specified memory size
2. ELF file is loaded into memory
3. Tasks are created (at least one main task)
4. CPU begins execution at the entry point
5. Programs can use syscalls for I/O and task management

## Limitations

1. Input is line-buffered (requires Enter key)
2. Basic UART implementation without hardware flow control
3. Limited error handling for malformed input
4. No support for interrupts or exceptions
5. Memory protection is basic

## Usage Example

```java
// Create a computer with 128MB of memory
RV32iComputer computer = new RV32iComputer(128 * 1024 * 1024);
RV32iCpu cpu = computer.getCpu();
MemoryManager memoryManager = computer.getMemoryManager();
ElfLoader elfLoader = new ElfLoader(memoryManager);

// Load and execute an ELF file
elfLoader.loadElf("program.elf");
int entryPoint = elfLoader.getEntryPoint();
int mainTaskId = computer.createTask(entryPoint);
cpu.setProgramCounterEntryPoint(entryPoint);
cpu.turnOn();
```

## Dependencies

- Java 8 or higher
- No external dependencies required

## Building and Running

1. Compile the project:
   ```bash
   javac -d bin src/main/java/cse311/*.java
   ```
2. Run the emulator:
   ```bash
   java -cp bin cse311.App program.elf
   ```

## License

[Specify your license here]

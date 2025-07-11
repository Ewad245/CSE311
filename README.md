# RV32I Simulator in Java with OS Features

This project is an **RV32I CPU simulator** written in **Java**, designed to execute **bare-metal ELF files** targeting the **RISC-V RV32I** architecture. It features memory-mapped I/O (MMIO) for UART communication, memory protection.

## Features
- **RV32I Instruction Set**: Supports base integer instructions.
- **Memory Layout**: Custom linker script with text, data, heap, and stack sections.
- **UART Emulation**: Memory-mapped I/O for serial communication.
- **ELF Execution**: Loads and executes ELF binaries.
- **OS Feature (In Progress)s**:
  - **Syscall Handling**: Support for exit, read, write, and yield syscalls
  - **Memory Management**: Basic heap allocation and deallocation
  - **Multitasking**: Cooperative task switching
- **Java + Gradle**: Built using Java with the Gradle Groovy DSL.
- **Networking Support** *(Planned)*: Connect to a Socket.io client via Netty.
- **GUI Frontend** *(Planned)*: Graphical interface for debugging and execution monitoring.

## Installation & Setup

### Prerequisites
- **Java 17+** installed.
- **Gradle** (comes with the project).
- **Git** (for cloning the repository).
- **RISC-V Toolchain** (for compiling example programs).

### Steps to Build & Run

1. **Clone the repository**:
   ```sh
   git clone https://github.com/your-repo/rv32i-emulator.git
   cd rv32i-emulator
   ```

2. **Build the project using Gradle**:
   ```sh
   ./gradlew build
   ```

3. **Run the emulator**:
   ```sh
   java -cp app/build/libs/app.jar cse311.App "path/to/your/program.elf"
   ```

### Running Tests
To ensure everything works correctly, run:
```sh
./gradlew test
```

## Usage
- Place your **RV32I ELF binaries** in the project directory.
- Run the emulator with the ELF file as an argument.
- View UART output from memory-mapped I/O.
- Use syscalls in your programs to interact with the OS features.

## OS Implementation

The OS implementation follows the roadmap outlined in the `OperatingSystemDevelopGuide.md` document and includes:

1. **Syscall Handling**
   - `exit` (93): Terminate the program
   - `write` (64): Write to stdout/stderr
   - `read` (63): Read from stdin
   - `yield` (24): Cooperative multitasking

2. **Memory Management**
   - Basic heap allocation with `allocateHeap`
   - Placeholder for memory deallocation with `free`

3. **Multitasking**
   - Task structure for maintaining process state
   - Task manager for creating and scheduling tasks
   - Cooperative multitasking via the `yield` syscall

### Example Program

An example program `examples/hello_os.c` demonstrates the use of syscalls:

```c
// Define syscall numbers
#define SYS_read  63
#define SYS_write 64
#define SYS_yield 24
#define SYS_exit  93

// Main function
int main() {
    print("Hello from our minimal OS!\n");
    yield();
    // Read input, process, and exit
    exit(0);
    return 0;
}
```

### Compiling Example Programs

To compile the example programs, use the RISC-V toolchain:

```bash
riscv32-unknown-elf-gcc -march=rv32i -mabi=ilp32 -nostdlib -static -T linker.ld examples/hello_os.c -o hello_os.elf
```

## Roadmap
- [X] Implement web version. (currently working on it using SocketIO and NextJS client) (In "Support SocketIO" Branch)
- [ ] Implement basic OS features (syscalls, memory management, multitasking). (Pending)
- [ ] Implement RV32M (Multiplication & Division).
- [ ] Add CSR (Control and Status Registers).
- [ ] Improve ELF loading and debugging support.
- [ ] Implement file system support
- [ ] Add user mode execution
- [ ] Implement a simple shell

## Contributing
Pull requests are welcome! Open an issue for discussion before making major changes.

## License
This project is licensed under the **MIT License**.
```
Let me know if you want any modifications! 🚀

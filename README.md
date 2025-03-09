# RV32I Emulator in Java

This project is an **RV32I CPU emulator** written in **Java**, designed to execute **bare-metal ELF files** targeting the **RISC-V RV32I** architecture. It features memory-mapped I/O (MMIO) for UART communication, memory protection, and plans for extended support such as exception handling, peripheral simulation, and debugging capabilities.

## Features
- **RV32I Instruction Set**: Supports base integer instructions.
- **Memory Layout**: Custom linker script with text, data, heap, and stack sections.
- **UART Emulation**: Memory-mapped I/O for serial communication.
- **ELF Execution**: Loads and executes ELF binaries.
- **Java + Gradle**: Built using Java with the Gradle Groovy DSL.
- **Networking Support** *(Planned)*: Connect to a Socket.io client via Netty.
- **GUI Frontend** *(Planned)*: Graphical interface for debugging and execution monitoring.

## Installation & Setup

### Prerequisites
- **Java 17+** installed.
- **Gradle** (comes with the project).
- **Git** (for cloning the repository).

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
   ./gradlew run --args="path/to/your/program.elf"
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

## Roadmap
- [ ] Implement web version. (currently working on it using SocketIO and NextJS client)
- [ ] Implement RV32M (Multiplication & Division).
- [ ] Add CSR (Control and Status Registers).
- [ ] Improve ELF loading and debugging support.

## Contributing
Pull requests are welcome! Open an issue for discussion before making major changes.

## License
This project is licensed under the **MIT License**.
```
Let me know if you want any modifications! 🚀

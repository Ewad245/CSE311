Here’s a **step-by-step roadmap**, broken down by stages and milestones.

---

## 🧱 Stage 1: Minimal OS Kernel (Bootstrapping)

### ✅ Step 1. Set Up a Bare-Metal Kernel ELF

* Write a C or Assembly file as your OS kernel entry point (e.g., `kernel.c` or `start.s`).
* Ensure `_start` is your entry symbol, matching your linker script.
* Compile with `riscv32-unknown-elf-gcc` using `-nostdlib -ffreestanding`.
* Link with your current `linker.ld`.

```c
void _start() {
    // Initialize BSS (optional)
    // Setup stack pointer if needed
    // Call main()
    while (1) {} // hang
}
```

---

### ✅ Step 2. Print Something (via UART)

* Write to the MMIO UART data register at `0x10000000` from kernel code.
* Test UART output from your kernel ELF (e.g., `puts("Hello from OS")`).

---

### ✅ Step 3. Setup Stack and Basic ABI Support

* Manually set `sp = STACK_START` in assembly before calling `main()`.
* Optionally implement simple `memcpy`, `memset`, `strlen` (if using C).
* Handle alignment and ABI rules if calling from C.

---

## 🧠 Stage 2: OS Runtime Features

### ✅ Step 4. Implement a Simple Syscall Dispatcher

* Trap on ECALL from user programs.
* Check `x[17]` for syscall number.
* Implement basic syscalls like:

  * `write(fd, buf, size)` to UART.
  * `exit(code)` to halt simulation.
  * `read(fd, buf, size)` to read from input.

Update your `ECALL` instruction handler in emulator:

```java
case 93: // exit
case 64: // write
case 63: // read
```

---

### ✅ Step 5. Add a Simple Memory Allocator (Kernel `malloc`)

* Bump-pointer heap allocator: `heap_ptr += size; return old_heap_ptr;`.
* Optional: implement `free()` as no-op for now.

---

## 🧵 Stage 3: Multitasking and Scheduling

### ⏳ Step 6. Create a Simple Task Structure

* Each task has:

  * Stack
  * PC
  * Register state
* Store them in a `Task[]` array.

---

### 🔁 Step 7. Cooperative Multitasking

* Implement a `yield()` syscall.
* Kernel saves current task context, switches to next.
* Round-robin scheduler.

---

### 🧭 Step 8. Timer Interrupt (Preemptive Multitasking)

* Add timer peripheral simulation in your CPU (e.g., memory-mapped MTIME).
* Implement interrupt vector table (trap entry point).
* Use CLINT (Core Local Interruptor) or software timer simulation.

---

## 📦 Stage 4: File System and Drivers

### 🗃️ Step 9. Simulate a File System

* In emulator: map a virtual block device (e.g., SD card) to host file.
* Kernel: implement simple filesystem like FAT12, ext2, or a toy FS.
* Implement file-related syscalls: `open()`, `read()`, `write()`.

---

### ⌨️ Step 10. Device Drivers

* UART: already done.
* Timer: needed for preemptive scheduling.
* Keyboard: simulate via UART input.
* Display: create memory-mapped frame buffer or serial-based UI.

---

## 🖥️ Stage 5: Userland & Shell

### 💬 Step 11. Build a Shell

* Add a small command-line shell (parses user input from UART).
* Commands:

  * `ls`, `cat`, `exec`, `reboot`, etc.
* Load ELF binaries from memory or file system.

---

### 👤 Step 12. Support ELF Program Loading (From Kernel)

* Reuse your `ElfLoader` in kernel space to load user programs.
* Setup memory, stack, and jump to entry point.

---

## 🚀 Final Goals: Full OS Support

### 🎯 Step 13. Add More System Calls

* `fork`, `exec`, `wait`, `sbrk` (for dynamic memory).
* `kill`, `getpid`, `time`, `stat`, etc.

---

### 🎯 Step 14. User/Kernel Mode Separation

* Not possible on RV32I (no privilege levels), but you can simulate:

  * Tag instructions as user or kernel.
  * Restrict memory access in emulator for user programs.

---

### 🎯 Step 15. Add Networking (Optional)

* Simulate network interface in emulator.
* Implement basic protocols (echo server, HTTP, etc.).

---

## Tools to Use Along the Way

* **Compiler:** `riscv32-unknown-elf-gcc`
* **Linker Script:** Place `.text`, `.data`, `.bss`, `.stack` manually.
* **Emulator Debugging:** Add trace logs, instruction counters, memory dumps.
* **Test Programs:** Use simple C programs compiled for RV32I.

---

Would you like a template project structure or help with a specific step (like writing the first kernel C code or ELF loader inside the kernel)?

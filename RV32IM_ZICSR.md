Below is a **practical, implementer‑oriented spec** for **RV32IM\_Zicsr** aimed at extending your current RV32I emulator. I’ve organized it into: (1) what the ISA includes, (2) the exact instruction semantics you need to implement, (3) the **CSR address space & access rules**, (4) the **minimum Machine‑mode CSRs** you should provide for a bare‑metal RV32 system, (5) **traps/exceptions** you must raise, (6) an **implementation plan** with common pitfalls, and (7) **conformance criteria & tests**. Citations point to the current RISC‑V ISA manuals (unprivileged + privileged) and the official M‑extension/Zicsr chapters.

---

## 1) What “rv32im\_zicsr” means (scope)

* **RV32I**: Base 32‑bit integer ISA (you already have this). See the Unprivileged ISA manual. ([Grainger Course Websites][1])
* **M** (Multiply/Divide): Adds `MUL`, `MULH`, `MULHSU`, `MULHU`, `DIV`, `DIVU`, `REM`, `REMU`. Precise behavior for divide‑by‑zero and signed overflow is defined below. ([Grainger Course Websites][1], [GitHub][2])
* **Zicsr** (CSR access): Adds CSR read/modify/write instructions: `CSRRW`, `CSRRS`, `CSRRC` and immediate forms `CSRRWI`, `CSRRSI`, `CSRRCI`. It also defines CSR address space & access rules used by the privileged arch (but Zicsr itself does not mandate which CSRs exist). ([Five EmbedDev][3])

> Note: Counters (`cycle/time/instret`) are **Zicntr**, which *depends on Zicsr* but is a separate extension. If you want those CSRs, add **Zicntr** as well; otherwise reads of those CSRs should trap or be absent. ([Five EmbedDev][4])

---

## 2) Instruction semantics you must support

### 2.1 M extension (RV32M)

**Multiply family**

* `MUL rd, rs1, rs2`: low 32 bits of signed×signed product.
* `MULH rd, rs1, rs2`: high 32 bits of signed×signed product.
* `MULHSU rd, rs1, rs2`: high 32 bits of signed×unsigned product.
* `MULHU rd, rs1, rs2`: high 32 bits of unsigned×unsigned product.
  (All defined on 32×32→64 intermediate product; return specified half.) ([Grainger Course Websites][1])

**Divide / Remainder family**

* `DIV rd, rs1, rs2`: signed division.
* `DIVU rd, rs1, rs2`: unsigned division.
* `REM rd, rs1, rs2`: signed remainder.
* `REMU rd, rs1, rs2`: unsigned remainder.
  **Corner cases (must match spec exactly):**
* **Divisor = 0:** `DIV*` result = **−1** (all 1s in XLEN); `REM*` result = **rs1** (the dividend).
* **Signed overflow (INT\_MIN / −1):** `DIV` result = **INT\_MIN**, `REM` result = **0**.
* Unsigned overflow does not occur. (Implementations may use any algorithm, but results must match.) ([GitHub][2], [docs.openhwgroup.org][5])

### 2.2 Zicsr (CSR instructions)

**Encodings & behavior (atomic RMW at the CSR):**

* `CSRRW rd, csr, rs1`: swap CSR with `rs1`. `rd` gets old CSR.
* `CSRRS rd, csr, rs1`: **Set bits** in CSR with mask `rs1`; `rd` gets old CSR. If `rs1==x0`, it’s a **pure read** (no write side‑effects).
* `CSRRC rd, csr, rs1`: **Clear bits** in CSR with mask `rs1`; `rd` gets old CSR. If `rs1==x0`, it’s a **pure read**.
* `CSRRWI`, `CSRRSI`, `CSRRCI`: same operations but with **zimm\[4:0]** as the mask/value (zero‑extended to XLEN). If `zimm=0`, `CSRRSI/CSRRCI` become pure reads.
* Accesses must perform privilege checks and respect read‑only/write‑any‑legal (WARL) field rules; illegal accesses raise **illegal instruction**. ([Five EmbedDev][3])

---

## 3) CSR address space & access rules (Zicsr background you must enforce)

* **CSR space:** 4,096 CSRs per hart. The CSR **12‑bit address** encodes access constraints; privilege checking must be enforced. (e.g., Machine CSRs are only accessible in M‑mode by default.) ([Five EmbedDev][3])
* **Access checks:**

  * Attempting to write a **read‑only** CSR (or read an absent CSR) ⇒ **illegal instruction trap**.
  * Attempting to access a CSR from insufficient privilege ⇒ **illegal instruction trap**.
* **Read/modify/write atomicity:** Each CSR instruction reads the current value and writes the updated value **as one architectural operation** (no intervening updates visible). ([Five EmbedDev][3])
* **WARL fields:** Many CSR bits are **Write‑Any, Read‑Legal**—writes outside the legal set must coerce to the nearest legal value. This is particularly relevant in `mstatus`, `mtvec`, etc. (see next section). ([Grainger Course Websites][6])

---

## 4) Minimal Machine‑mode CSRs you should implement for a bare‑metal RV32 system

For an M‑mode‑only RV32 target (no S/U modes), implement at least the Machine CSRs commonly required by toolchains/RTOSes and by the privileged spec’s M‑mode flow:

* **Machine Status:** `mstatus`

  * Holds global interrupt enable `MIE`, prior interrupt enable `MPIE`, and other status bits. Many fields are WARL/readonly depending on the implementation. ([Grainger Course Websites][6])
* **Trap setup:** `mtvec` (trap vector base + mode), often only **direct** mode is needed; **vectored** is optional. `mtvec` is WARL: alignment constraints apply. ([Grainger Course Websites][6])
* **Trap handling registers:** `mepc` (faulting PC), `mcause` (trap cause), `mtval` (bad address/inst), optional `mtinst/mtval2` (if you implement them; otherwise not present). ([Grainger Course Websites][6])
* **Machine interrupt enable/pending:** `mie`, `mip` (if you model interrupts/timers); for a minimal emulator you can implement them and leave sources inactive until you add a timer/PLIC. ([Grainger Course Websites][6])
* **`mscratch`** (scratch register for trap handler). ([Grainger Course Websites][6])
* **`misa`** (ISA string bits) — optional but strongly recommended; report `I`, `M`, and `Zicsr` as present, set XLEN=32. If unimplemented, it can legally read as zero; many runtimes probe it. ([Grainger Course Websites][6])
* **Base counters (optional unless you claim Zicntr):** `mcycle`/`minstret` and unprivileged read‑only aliases `cycle`/`instret`. If you claim **Zicntr**, implement `cycle`, `time`, `instret` and `mcountinhibit`. If you don’t, omit them and make reads trap. ([Five EmbedDev][4], [Stanford Center for Space Science][7])

**Privileged instructions you’ll likely need with these CSRs** (though not part of Zicsr):

* `MRET` to return from traps; `ECALL`/`EBREAK` to enter. `WFI` optional. These are in the **Privileged ISA**, not Zicsr, but practically necessary for trap flow. ([Grainger Course Websites][6])

---

## 5) Traps/exceptions you must generate

* **Illegal instruction**:

  * Unknown opcode or unsupported extension (e.g., an M‑op when M is disabled).
  * CSR access violation (insufficient privilege, non‑existent CSR, or write to read‑only CSR). ([Grainger Course Websites][1], [Grainger Course Websites][6])
* **Breakpoint**: `EBREAK`. ([Grainger Course Websites][1])
* **Environment call**: `ECALL` from M (or U/S if you later add them). ([Grainger Course Websites][6])
* **Misaligned/Access faults**: as per your RV32I implementation (unchanged by M/Zicsr). ([Grainger Course Websites][1])

On a trap, update `mstatus` (save/clear MIE to MPIE, set MPP, etc.), write `mepc`, set `mcause`, and optionally `mtval` with the faulting address/inst. Then transfer control to `mtvec`. `MRET` restores state and returns to `mepc`. ([Grainger Course Websites][6])

---

## 6) Implementation plan (step‑by‑step)

### 6.1 Decoder

* Add M‑opcodes/funct3/funct7 patterns for `MUL*`, `DIV*`, `REM*`.
* Add Zicsr patterns (`SYSTEM` major opcode with funct3=001/010/011 for `CSRRW/CSRRS/CSRRC`, funct3=101/110/111 for the immediate forms). Ensure you parse the **12‑bit CSR address** and **zimm\[4:0]**. ([Grainger Course Websites][1], [Five EmbedDev][3])

### 6.2 Execute (ALU/MUL/DIV)

* **Multipliers:** Allowed to be multi‑cycle; architecturally precise result only at writeback. For `MULH*`, do a 64‑bit product and select the high word with correct sign treatment.
* **Dividers:** Implement restoring/non‑restoring or library routine; must produce the corner‑case results above (−1 / dividend rules, INT\_MIN/−1). Consider a cycle budget and expose no intermediate architectural state. ([GitHub][2])

### 6.3 CSR File

* Maintain a **map from 12‑bit CSR address → (value, properties)** where properties include: implemented?, min privilege, read‑only/WARL mask, side effects.
* On each CSR instr:

  1. **Privilege check** vs. current mode and CSR’s required privilege.
  2. **Implementability check** (exists, writable bits).
  3. Compute new value:

     * `CSRRW`: `new = rs1`;
     * `CSRRS`: `new = old | mask`;
     * `CSRRC`: `new = old & ~mask`;
     * Immediate forms use `zimm` (zero‑extended).
  4. Apply WARL masks (coerce illegal bit combos).
  5. Commit atomically and write `rd=old`.
* For **`mstatus`**: implement at least `MIE`, `MPIE`, `MPP` fields and legal encodings (e.g., only M‑mode present). For others (like floating‑point state bits), tie off as 0 and mark read‑only if you don’t implement F/D. ([Grainger Course Websites][6])

### 6.4 Trap/Return flow

* On **illegal instruction** (including CSR violations), set `mcause` appropriately, `mepc=PC of faulting instr`, optionally `mtval` (e.g., the CSR address or instruction), update `mstatus`, and jump to `mtvec`.
* Implement **`MRET`** to restore `mstatus` (MIE from MPIE, MPP→current mode, etc.) and `PC=mepc`. ([Grainger Course Websites][6])

### 6.5 Optional counters (only if you claim Zicntr)

* Implement **`cycle`/`instret`** (read‑only in U‑space) and **`mcycle`/`minstret`** (M‑space). Add **`mcountinhibit`** so software can stop incrementing to save energy or to take atomic snapshots. `time` usually comes from a platform timebase; for an emulator you can map it to your host clock or a simulated tick. If you **don’t** implement Zicntr, reads of these CSRs should trap as illegal. ([Five EmbedDev][4], [Stanford Center for Space Science][7])

### 6.6 Common pitfalls

* **CSR atomicity:** Don’t split read and write across events that could change the CSR between them. Treat it as one micro‑op. ([Five EmbedDev][3])
* **`CSRRS/CSRRC` with `rs1=x0` or `zimm=0`:** must be a **pure read** (no side effects). ([EECS at UC Berkeley][8])
* **M‑division corner cases:** Must match spec (−1, dividend return, INT\_MIN case). Tests check these. ([GitHub][2])
* **WARL coercion:** e.g., `mtvec` alignment—if illegal, coerce to legal (e.g., clear low bits) rather than trap. ([Grainger Course Websites][6])
* **`misa`:** If present, make sure you report exactly the extensions you implement (`I`, `M`, and `Zicsr`; optionally `C`, etc.). If absent, many OSes still boot; some libraries probe it. ([Grainger Course Websites][6])

---

## 7) Conformance criteria & tests (“what it should meet”)

1. **Architectural correctness (mandatory):**

   * All RV32I base rules continue to hold.
   * All **M‑extension** instructions produce spec‑accurate results, including divide corner‑cases. ([GitHub][2])
   * All **Zicsr** instructions obey privilege checks, read‑only/WARL behavior, and atomic RMW semantics. ([Five EmbedDev][3])

2. **Privileged behavior (M‑mode):**

   * Trap entry/return (`mstatus` updates, `mepc/mcause/mtval`, vectoring via `mtvec`) follow the privileged spec for M‑mode. ([Grainger Course Websites][6])

3. **If you claim Zicntr (optional):**

   * `cycle/time/instret` are readable at unprivileged level (XLEN‑wide aliases); `mcycle/minstret` exist in M‑mode; `mcountinhibit` behavior matches spec. ([Five EmbedDev][4], [Stanford Center for Space Science][7])

4. **ISA string & discovery:**

   * `misa` (if implemented) accurately reports `rv32im_zicsr` (and any others). ([Grainger Course Websites][6])

5. **Pass the official architecture tests:**

   * Use **riscv-arch-test** (via RISCOF or your harness) for **RV32I**, **M**, and **Zicsr**. Configure the ISA string as `rv32im_zicsr`. Tests cover CSR legality, privilege checks, and M corner cases. ([GitHub][9], [riscof.readthedocs.io][10], [GitHub][11])

---

## 8) Minimal checklist (quick reference)

* [ ] Decode + execute `MUL*`, `DIV*`, `REM*` with correct corner cases. ([GitHub][2])
* [ ] Decode + execute `CSRRW/CSRRS/CSRRC` and `CSRRWI/CSRRSI/CSRRCI`; implement atomic RMW; `rs1=x0` / `zimm=0` ⇒ pure read. ([Five EmbedDev][3], [EECS at UC Berkeley][8])
* [ ] CSR file with privilege, RO/WARL masks, existence table; enforce illegal instruction on violations. ([Five EmbedDev][3])
* [ ] M‑mode CSRs: `mstatus`, `mtvec`, `mepc`, `mcause`, `mtval`, `mie`, `mip`, `mscratch`, and (optionally) `misa`. Trap flow per spec; implement `MRET`, `ECALL`, `EBREAK`. ([Grainger Course Websites][6])
* [ ] (Optional) Zicntr counters (`cycle/time/instret`, `mcycle/minstret`, `mcountinhibit`) or cleanly absent with traps. ([Five EmbedDev][4], [Stanford Center for Space Science][7])
* [ ] riscv‑arch‑test passes for I, M, Zicsr. ([GitHub][9])

---

## 9) Example: CSR access outcomes (summary table)

* `CSRRS rd, csr, x0` → read‑only; `rd=CSR`, CSR unchanged.
* `CSRRS x0, csr, rs1` → set CSR bits by mask; throw away old value.
* `CSRRCI rd, csr, 0` → read‑only; CSR unchanged.
* Attempt to write to RO bits → **illegal instruction** trap.
* Access a Machine CSR from non‑M privilege (if you later add U/S) → **illegal instruction** trap. ([Five EmbedDev][3])

---

### Want help wiring this into your emulator?

If you share your current decode/execute skeleton (Java), I can sketch exact match tables for `M` and `Zicsr`, a minimal CSR map (addresses and masks for the Machine CSRs above), and a tiny trap handler demo that exercises `CSRRW/CSRRSI`, `ECALL/MRET`, and a divide corner case.

**Sources**

* Unprivileged ISA manual (latest public builds, includes `I`, `M`, and Zicsr chapters). ([Grainger Course Websites][1])
* Zicsr chapter (CSR space and instruction semantics). ([Five EmbedDev][3])
* M‑extension semantics, including division corner cases. ([GitHub][2])
* Privileged ISA manual (M‑mode CSRs, traps, `MRET`, `ECALL/EBREAK`, WARL). ([Grainger Course Websites][6])
* Zicntr (counters/timers) if you choose to add them. ([Five EmbedDev][4], [Stanford Center for Space Science][7])
* Architecture compliance tests. ([GitHub][9], [GitHub][11])

If you tell me whether you want **Zicntr** now or later, I’ll tailor the CSR map and the arch‑test configuration to match.

[1]: https://courses.grainger.illinois.edu/ece391/su2025/docs/unpriv-isa-20240411.pdf?utm_source=chatgpt.com "The RISC-V Instruction Set Manual Volume I"
[2]: https://github.com/riscv/riscv-isa-manual/blob/main/src/m-st-ext.adoc?utm_source=chatgpt.com "riscv-isa-manual/src/m-st-ext.adoc at main"
[3]: https://www.five-embeddev.com/riscv-user-isa-manual/latest-adoc/zicsr.html?utm_source=chatgpt.com "1. \"Zicsr\", Control and Status Register (CSR) Instructions, ..."
[4]: https://www.five-embeddev.com/riscv-user-isa-manual/latest-adoc/counters.html?utm_source=chatgpt.com "1. \"Zicntr\" and \"Zihpm\" Counters, Version 2.0"
[5]: https://docs.openhwgroup.org/projects/cva6-user-manual/07_cv32a60x/riscv/unpriv.html?utm_source=chatgpt.com "Unprivileged RISC-V ISA — CVA6 documentation"
[6]: https://courses.grainger.illinois.edu/ece391/sp2025/docs/priv-isa-20240411.pdf?utm_source=chatgpt.com "The RISC-V Instruction Set Manual: Volume II"
[7]: https://www.scs.stanford.edu/~zyedidia/docs/riscv/riscv-privileged.pdf?utm_source=chatgpt.com "The RISC-V Instruction Set Manual"
[8]: https://www2.eecs.berkeley.edu/Pubs/TechRpts/2015/EECS-2015-49.pdf?utm_source=chatgpt.com "The RISC-V Instruction Set Manual Volume II"
[9]: https://github.com/riscv-non-isa/riscv-arch-test?utm_source=chatgpt.com "riscv-non-isa/riscv-arch-test"
[10]: https://riscof.readthedocs.io/en/stable/testformat.html?utm_source=chatgpt.com "Test Format Spec — RISCOF 1.24.0 documentation"
[11]: https://github.com/riscv-non-isa/riscv-arch-test/blob/dev/CHANGELOG.md?utm_source=chatgpt.com "CHANGELOG.md - riscv-non-isa/riscv-arch-test"

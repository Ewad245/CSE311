package cse311;

public class InstructionDecoded {
    private int opcode;
    private int rd;
    private int rs1;
    private int rs2;
    private int func3;
    private int func7;
    private int imm_i;
    private int imm_s;
    private int imm_b;
    private int imm_u;
    private int imm_j;

    public int getOpcode() {
        return opcode;
    }

    public void setOpcode(int opcode) {
        this.opcode = opcode;
    }

    public int getRd() {
        return rd;
    }

    public void setRd(int rd) {
        this.rd = rd;
    }

    public int getRs1() {
        return rs1;
    }

    public void setRs1(int rs1) {
        this.rs1 = rs1;
    }

    public int getRs2() {
        return rs2;
    }

    public void setRs2(int rs2) {
        this.rs2 = rs2;
    }

    public int getFunc3() {
        return func3;
    }

    public void setFunc3(int func3) {
        this.func3 = func3;
    }

    public int getFunc7() {
        return func7;
    }

    public void setFunc7(int func7) {
        this.func7 = func7;
    }

    public int getImm_i() {
        return imm_i;
    }

    public void setImm_i(int imm_i) {
        this.imm_i = imm_i;
    }

    public int getImm_s() {
        return imm_s;
    }

    public void setImm_s(int imm_s) {
        this.imm_s = imm_s;
    }

    public int getImm_b() {
        return imm_b;
    }

    public void setImm_b(int imm_b) {
        this.imm_b = imm_b;
    }

    public int getImm_u() {
        return imm_u;
    }

    public void setImm_u(int imm_u) {
        this.imm_u = imm_u;
    }

    public int getImm_j() {
        return imm_j;
    }

    public void setImm_j(int imm_j) {
        this.imm_j = imm_j;
    }

    @Override
    public String toString() {
        return "InstructionDecoded [opcode=" + opcode + ", rd=" + rd + ", rs1=" + rs1 + ", rs2=" + rs2 + ", func3="
                + func3 + ", func7=" + func7 + ", imm_i=" + imm_i + ", imm_s=" + imm_s + ", imm_b=" + imm_b + ", imm_u="
                + imm_u + ", imm_j=" + imm_j + "]";
    }

}

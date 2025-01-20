package cse311;

public class RV32iCpu {
    private byte[] x = new byte[32];
    private int pc = 0;
    private byte[] instruction;

    private byte[] memory;
    private Thread cpuThread;
    private boolean running = false;

    public RV32iCpu(byte[] memory) {
        this.memory = memory;
    }

    public void turnOn() {
        this.cpuThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (RV32iCpu.this.running) {
                    fetchExecuteCycle();
                }
            }
        });
        this.running = true;
        this.cpuThread.start();
    }

    private void fetchExecuteCycle() {
        // Fetch the instruction from memory at the address in the pc register
        fetch();
        decode();
        execute(); // Viet them update cho pc, cpu sau nay
    }

    private void fetch() {
        // Read from memory at pc
    }

}

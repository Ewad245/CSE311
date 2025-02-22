package cse311;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import com.google.gson.Gson;

import java.io.File;
import java.io.FileOutputStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

public class EmulatorWebSocket extends WebSocketServer {
    private final RV32iCpu cpu;
    private final Gson gson = new Gson();

    public EmulatorWebSocket(int port, RV32iCpu cpu) {
        super(new InetSocketAddress(port));
        this.cpu = cpu;
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        System.out.println("New connection established");
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        System.out.println("Connection closed");
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        // Handle incoming commands from web client
        Map<String, Object> command = gson.fromJson(message, Map.class);
        String action = (String) command.get("action");

        switch (action) {
            case "start":
                cpu.turnOn();
                break;
            case "stop":
                cpu.stopEmulation();
                break;
            // Add more commands as needed
        }
    }

    @Override
    public void onMessage(WebSocket conn, ByteBuffer message) {
        try {
            // Save the uploaded file temporarily
            File tempFile = File.createTempFile("uploaded", ".elf");
            try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                fos.getChannel().write(message);
            }

            // Load and run the ELF file
            ElfLoader elfLoader = new ElfLoader(cpu.getMemoryManager());
            elfLoader.loadElf(tempFile.getPath());

            // Reset CPU state
            cpu.stopEmulation();
            cpu.setProgramCounterEntryPoint(elfLoader.getEntryPoint());
            cpu.turnOn();

            // Clean up temp file
            tempFile.delete();

        } catch (Exception e) {
            e.printStackTrace();
            // Send error message back to client using the connection object
            conn.send("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    public void broadcastRegisterState(Map<String, Object> state) {
        String jsonState = gson.toJson(state);
        broadcast(jsonState);
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        System.err.println("WebSocket error: " + ex.getMessage());
    }

    @Override
    public void onStart() {
        System.out.println("WebSocket server started on port: " + getPort());
    }
}
package cse311;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;

public class WebSocketServer {
    private static final int PORT = 9092;
    private static final String ELF_STORAGE_DIR = "./elf_files/";
    private static final int MAX_FILES = 10;
    private static final Map<String, SocketIOClient> userSessions = new ConcurrentHashMap<>();
    private static final Map<String, RV32iCpu> userCPUs = new ConcurrentHashMap<>();
    private SimpleMemory memory;
    private MemoryManager memoryManager;
    private ElfLoader elfLoader;

    private SocketIOServer server;

    public WebSocketServer() {
        Configuration config = new Configuration();
        config.setHostname("0.0.0.0"); // Using localhost instead of hardcoded IP
        config.setPort(PORT);
        config.setEnableCors(true);
        // Set the origin to allow requests from any host
        config.setOrigin("*");

        server = new SocketIOServer(config);

        // Ensure storage directory exists
        File directory = new File(ELF_STORAGE_DIR);
        if (!directory.exists()) {
            directory.mkdirs();
        }

        server.addConnectListener(client -> {
            String sessionId = client.getSessionId().toString();
            userSessions.put(sessionId, client);
            memory = new SimpleMemory(128 * 1024 * 1024);
            memoryManager = new MemoryManager(memory, client);
            elfLoader = new ElfLoader(memoryManager);
            userCPUs.put(sessionId, new RV32iCpu(memoryManager, client)); // Assign a unique CPU instance
            client.sendEvent("connected");
            System.out.println("User connected: " + sessionId);
        });

        server.addEventListener("send_elf", String.class, (client, data, ackSender) -> {
            System.out.println("Received ELF file: ");

            // Decode ELF binary data
            byte[] elfBinary = Base64.getDecoder().decode(data);

            String fileName = "ELF.elf";

            // Save ELF file
            String filePath = saveElfFile(fileName, elfBinary);

            // Load ELF file
            try {
                elfLoader.loadElf(filePath);
                int entryPoint = elfLoader.getEntryPoint();
                // Start the CPU
                String sessionId = client.getSessionId().toString();
                RV32iCpu cpu = userCPUs.get(sessionId);
                cpu.setProgramCounterEntryPoint(entryPoint);
                cpu.turnOn();
            } catch (IOException e) {
                System.err.println("Error running program: " + e.getMessage());
                e.printStackTrace();
            }

            // Clean up old files
            cleanupOldFiles();

            // Send back confirmation
            client.sendEvent("elf_received");
        });

        // Test
        server.addEventListener("message", String.class, (client, data, ackSender) -> {
            System.out.println("Received: " + data);
            client.sendEvent("response", "Hello from Java Netty!");
        });

        server.addEventListener("user_input", String.class, (client, data, ackSender) -> {
            System.out.println("Received user input: " + data);
            String sessionId = client.getSessionId().toString();
            RV32iCpu cpu = userCPUs.get(sessionId);
            MemoryManager memManager = cpu.getMemoryManager();
            memManager.getInput(data);
        });

        server.addDisconnectListener(client -> {
            String sessionId = client.getSessionId().toString();
            userSessions.remove(sessionId);
            userCPUs.remove(sessionId);
            System.out.println("Client disconnected: " + sessionId);
        });
    }

    private String saveElfFile(String fileName, byte[] data) {
        String timestampedName = System.currentTimeMillis() + "_" + fileName;
        try (BufferedOutputStream bos = new BufferedOutputStream(
                new FileOutputStream(ELF_STORAGE_DIR + timestampedName))) {
            bos.write(data);
            bos.flush();
            System.out.println("File written successfully!");
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println(ELF_STORAGE_DIR + timestampedName);
        return ELF_STORAGE_DIR + timestampedName;
    }

    private void cleanupOldFiles() {
        try {
            List<Path> files = Files.list(Paths.get(ELF_STORAGE_DIR))
                    .sorted(Comparator.comparingLong(p -> p.toFile().lastModified()))
                    .collect(Collectors.toList());

            while (files.size() > MAX_FILES) {
                Files.delete(files.get(0)); // Delete oldest file
                files.remove(0);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void start() {
        server.start();
        System.out.println("WebSocket server started on port " + PORT);
    }

    public void stop() {
        server.stop();
        System.out.println("WebSocket server stopped.");
    }

    public static class ElfData {
        public String folderName;
        public String fileName;
        public String elfData;
    }
}

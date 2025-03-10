package cse311;

public class ElfData {
    private String folderName;
    private String fileName;
    private byte[] elfData; // Binary ELF data

    // Getters and setters (needed for deserialization)
    public String getFolderName() {
        return folderName;
    }

    public void setFolderName(String folderName) {
        this.folderName = folderName;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public byte[] getElfData() {
        return elfData;
    }

    public void setElfData(byte[] elfData) {
        this.elfData = elfData;
    }
}

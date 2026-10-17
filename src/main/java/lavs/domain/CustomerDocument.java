package lavs.domain;

public record CustomerDocument(
    Long id, String customerNumber, String fileName, String fileType, long size, String type) {}

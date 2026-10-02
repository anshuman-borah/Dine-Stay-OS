package project.EnterpriseSaas.demo.modules.storage.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import project.EnterpriseSaas.demo.modules.storage.dto.UploadResultDto;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class StorageService {

    @Value("${app.url:http://localhost:4000}")
    private String apiBaseUrl;

    private static final long MAX_SIZE_BYTES = 5 * 1024 * 1024; // 5 MB
    private static final List<String> ALLOWED_MIMETYPES = List.of(
            "image/jpeg", "image/png", "image/webp", "image/gif", "application/pdf"
    );

    private final Path localUploadsDir = Paths.get(System.getProperty("user.dir"), "uploads").toAbsolutePath().normalize();

    public StorageService() {
        try {
            if (!Files.exists(localUploadsDir)) {
                Files.createDirectories(localUploadsDir);
            }
        } catch (IOException e) {
            log.error("Failed to create local uploads directory: {}", e.getMessage());
        }
    }

    // ── Main Upload Method ───────────────────────────────────────────────────

    public UploadResultDto upload(MultipartFile file, String folder, UUID tenantId) {
        validateFile(file);

        String folderPath = buildFolderPath(folder, tenantId);

        // Generate unique 24-character hex filename + extension
        String originalFilename = file.getOriginalFilename();
        String ext = (originalFilename != null && originalFilename.contains("."))
                ? originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase()
                : mimetypeToExt(file.getContentType());

        byte[] randomBytes = new byte[12];
        new SecureRandom().nextBytes(randomBytes);
        String filename = HexFormat.of().formatHex(randomBytes) + ext;

        String key = folderPath + "/" + filename;

        try {
            Path destinationDir = localUploadsDir.resolve(folderPath).normalize();

            // Security: Path Traversal Check for directory
            if (!destinationDir.startsWith(localUploadsDir)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid upload directory");
            }

            if (!Files.exists(destinationDir)) {
                Files.createDirectories(destinationDir);
            }

            Path destinationPath = localUploadsDir.resolve(key).normalize();

            // Security: Path Traversal Check for file
            if (!destinationPath.startsWith(localUploadsDir)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid file path");
            }

            Files.write(destinationPath, file.getBytes());

            String publicUrl = apiBaseUrl + "/static/" + key;
            log.info("File uploaded locally: {} ({} bytes)", key, file.getSize());

            return UploadResultDto.builder()
                    .url(publicUrl)
                    .key(key)
                    .driver("local")
                    .size(file.getSize())
                    .mimetype(file.getContentType())
                    .build();

        } catch (IOException e) {
            log.error("File upload failed: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to store uploaded file");
        }
    }

    // ── Delete File ──────────────────────────────────────────────────────────

    public void delete(String key) {
        try {
            Path filePath = localUploadsDir.resolve(key).normalize();

            // Security: Prevent Directory Traversal Attacks (e.g. "../../../etc/passwd")
            if (!filePath.startsWith(localUploadsDir)) {
                log.warn("Attempted path traversal attack detected: {}", key);
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access Denied");
            }

            if (Files.exists(filePath)) {
                Files.delete(filePath);
                log.info("Deleted local file: {}", key);
            } else {
                log.warn("File not found for deletion: {}", key);
            }
        } catch (IOException e) {
            log.error("Failed to delete file {}: {}", key, e.getMessage());
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private String buildFolderPath(String folder, UUID tenantId) {
        String effectiveFolder = "general";
        if (folder != null && !folder.isBlank()) {
            effectiveFolder = folder.replaceAll("[^a-zA-Z0-9-_]", "");
            if (effectiveFolder.isBlank()) {
                effectiveFolder = "general";
            }
        }
        return tenantId != null ? tenantId + "/" + effectiveFolder : effectiveFolder;
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No file provided");
        }
        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File too large — max 5MB");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_MIMETYPES.contains(contentType.toLowerCase())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File type not allowed: " + contentType);
        }
    }

    private String mimetypeToExt(String mimetype) {
        if (mimetype == null) return ".jpg";
        return switch (mimetype.toLowerCase()) {
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            case "image/gif" -> ".gif";
            case "application/pdf" -> ".pdf";
            default -> ".jpg";
        };
    }
}
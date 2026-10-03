package project.EnterpriseSaas.demo.modules.storage.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import project.EnterpriseSaas.demo.modules.storage.dto.UploadResultDto;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class StorageService {

    private final Cloudinary cloudinary;

    private static final long MAX_SIZE_BYTES = 5 * 1024 * 1024; // 5 MB
    private static final List<String> ALLOWED_MIMETYPES = List.of(
            "image/jpeg", "image/png", "image/webp", "image/gif", "application/pdf"
    );

    public UploadResultDto upload(MultipartFile file, String folder, UUID tenantId) {
        validateFile(file);
        String folderPath = buildFolderPath(folder, tenantId);

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> uploadResult = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "folder", "dinestay/" + folderPath,
                            "resource_type", "auto"
                    )
            );

            String secureUrl = (String) uploadResult.get("secure_url");
            String publicId = (String) uploadResult.get("public_id");

            log.info("File uploaded to Cloudinary: {}", secureUrl);

            return UploadResultDto.builder()
                    .url(secureUrl)
                    .key(publicId) // Storing public_id so we can delete it later
                    .driver("cloudinary")
                    .size(file.getSize())
                    .mimetype(file.getContentType())
                    .build();

        } catch (IOException e) {
            log.error("Cloudinary upload failed: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to upload to Cloudinary");
        }
    }

    public void delete(String publicId) {
        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
            log.info("Deleted Cloudinary file: {}", publicId);
        } catch (IOException e) {
            log.error("Failed to delete from Cloudinary {}: {}", publicId, e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to delete from Cloudinary");
        }
    }

    private String buildFolderPath(String folder, UUID tenantId) {
        String effectiveFolder = (folder != null && !folder.isBlank()) ? folder.replaceAll("[^a-zA-Z0-9-_]", "") : "general";
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
}
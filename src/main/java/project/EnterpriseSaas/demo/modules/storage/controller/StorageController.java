package project.EnterpriseSaas.demo.modules.storage.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import project.EnterpriseSaas.demo.common.dto.ApiResponse;
import project.EnterpriseSaas.demo.modules.storage.dto.UploadResultDto;
import project.EnterpriseSaas.demo.modules.storage.service.StorageService;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/storage")
@RequiredArgsConstructor
public class StorageController {

    private final StorageService storageService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<UploadResultDto>> upload(
            @RequestParam("file") MultipartFile file,
            @RequestHeader(value = "x-tenant-id", required = false) UUID tenantId,
            @RequestParam(value = "folder", defaultValue = "general") String folder
    ) {
        UploadResultDto result = storageService.upload(file, folder, tenantId);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @DeleteMapping("/**")
    public ResponseEntity<Void> delete(
            @RequestHeader("x-tenant-id") UUID tenantId,
            jakarta.servlet.http.HttpServletRequest request
    ) {
        // Safely extract and decode the Cloudinary public_id
        String requestUri = request.getRequestURI();
        String basePath = "/api/v1/storage/";
        int index = requestUri.indexOf(basePath);
        if (index == -1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid path");
        }

        String publicId = requestUri.substring(index + basePath.length());
        publicId = URLDecoder.decode(publicId, StandardCharsets.UTF_8);

        // Multi-tenant security check adapted for Cloudinary paths
        if (!publicId.contains(tenantId.toString())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only delete files belonging to your account");
        }

        storageService.delete(publicId);
        return ResponseEntity.noContent().build();
    }
}
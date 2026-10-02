package project.EnterpriseSaas.demo.modules.storage.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UploadResultDto {
    private String url;
    private String key;
    private String driver;
    private long size;
    private String mimetype;
}
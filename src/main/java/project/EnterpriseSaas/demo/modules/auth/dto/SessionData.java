package project.EnterpriseSaas.demo.modules.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SessionData implements Serializable {
    private String tokenHash;
    private String ip;
    private String userAgent;
    private String createdAt;
    private String lastSeenAt;
}
package project.EnterpriseSaas.demo.modules.user.entity;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.persistence.*;
import jakarta.persistence.Table;
import lombok.*;
import org.hibernate.annotations.*;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;
import org.hibernate.type.SqlTypes;
import project.EnterpriseSaas.demo.common.enums.UserRole;
import project.EnterpriseSaas.demo.common.util.LenientMapDeserializer;
import project.EnterpriseSaas.demo.modules.branch.entity.Branch;
import project.EnterpriseSaas.demo.modules.tenant.entity.Tenant;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id")
    private Branch branch;

    private String email;

    private String phone;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "role", columnDefinition = "user_role")
    @Builder.Default
    private UserRole role = UserRole.cashier;

    @Column(length = 72)
    private String pin;

    @Column(name = "employee_code")
    private String employeeCode;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "last_login_at")
    private OffsetDateTime lastLoginAt;

    @Column(name = "refresh_token", columnDefinition = "text")
    private String refreshToken;

    @Builder.Default
    @JdbcTypeCode(SqlTypes.JSON)
    @JsonDeserialize(using = LenientMapDeserializer.class)
    @Column(columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> settings = new HashMap<>();

    @Builder.Default
    @JdbcTypeCode(SqlTypes.JSON)
    @JsonDeserialize(using = LenientMapDeserializer.class)
    @Column(columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> permissions = new HashMap<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public String getFullName() {
        if (lastName == null || lastName.isBlank()) {
            return firstName;
        }
        return firstName + " " + lastName;
    }
}
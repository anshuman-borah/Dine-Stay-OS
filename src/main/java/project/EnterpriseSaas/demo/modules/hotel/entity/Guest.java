package project.EnterpriseSaas.demo.modules.hotel.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import project.EnterpriseSaas.demo.common.enums.Gender;
import project.EnterpriseSaas.demo.common.enums.IdType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "hotel_guests",
        indexes = {
                @Index(name = "idx_hotel_guests_tenant_id", columnList = "tenant_id")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Guest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "name", length = 150, nullable = false)
    private String name;

    @Column(name = "phone", length = 20, nullable = false)
    private String phone;

    @Column(name = "email", length = 200)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "id_type")
    private IdType idType;

    @Column(name = "id_number", length = 50)
    private String idNumber;

    @Column(name = "nationality", length = 60)
    @Builder.Default
    private String nationality = "India";

    @Column(name = "address", columnDefinition = "text")
    private String address;

    @Column(name = "city", length = 80)
    private String city;

    @Column(name = "state", length = 60)
    private String state;

    @Column(name = "pincode", length = 10)
    private String pincode;

    @Column(name = "dob")
    private LocalDate dob;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender")
    private Gender gender;

    @Column(name = "total_stays", nullable = false)
    @Builder.Default
    private Integer totalStays = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
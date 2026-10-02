package project.EnterpriseSaas.demo.modules.shift.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "shift_denominations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShiftDenomination {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shift_id", nullable = false)
    private Shift shift;

    @Builder.Default
    @Column(name = "is_opening", nullable = false)
    private Boolean isOpening = false;

    @Builder.Default
    @Column(name = "note2000", nullable = false)
    private Integer note2000 = 0;

    @Builder.Default
    @Column(name = "note500", nullable = false)
    private Integer note500 = 0;

    @Builder.Default
    @Column(name = "note200", nullable = false)
    private Integer note200 = 0;

    @Builder.Default
    @Column(name = "note100", nullable = false)
    private Integer note100 = 0;

    @Builder.Default
    @Column(name = "note50", nullable = false)
    private Integer note50 = 0;

    @Builder.Default
    @Column(name = "note20", nullable = false)
    private Integer note20 = 0;

    @Builder.Default
    @Column(name = "note10", nullable = false)
    private Integer note10 = 0;

    @Builder.Default
    @Column(name = "coin5", nullable = false)
    private Integer coin5 = 0;

    @Builder.Default
    @Column(name = "coin2", nullable = false)
    private Integer coin2 = 0;

    @Builder.Default
    @Column(name = "coin1", nullable = false)
    private Integer coin1 = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    // Helper method: calculates the total monetary value from notes & coins
    public BigDecimal getTotalAmount() {
        long total = (long) (note2000 != null ? note2000 : 0) * 2000
                + (long) (note500 != null ? note500 : 0) * 500
                + (long) (note200 != null ? note200 : 0) * 200
                + (long) (note100 != null ? note100 : 0) * 100
                + (long) (note50 != null ? note50 : 0) * 50
                + (long) (note20 != null ? note20 : 0) * 20
                + (long) (note10 != null ? note10 : 0) * 10
                + (long) (coin5 != null ? coin5 : 0) * 5
                + (long) (coin2 != null ? coin2 : 0) * 2
                + (long) (coin1 != null ? coin1 : 0);
        return BigDecimal.valueOf(total);
    }
}
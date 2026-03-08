package ar.gob.rdam.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "certificados")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Certificado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "solicitud_id", nullable = false, unique = true)
    private Solicitud solicitud;

    @Column(name = "archivo_url", nullable = false, columnDefinition = "TEXT")
    private String archivoUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "emisor_id")
    private Usuario emisor;

    @Column(name = "firma_digital", columnDefinition = "TEXT")
    private String firmaDigital;

    @Column(name = "fecha_vencimiento")
    private LocalDate fechaVencimiento;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}

package com.sedif.sistema_tickets.core.correo;

import com.sedif.sistema_tickets.util.audit.Auditable;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "correo_institucional")
@Getter
@Setter
@NoArgsConstructor
public class CorreoInstitucional extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pn_id")
    private Long id;

    // --- DATOS DEL TITULAR DE LA CUENTA ---
    @Column(name = "s_nombre", nullable = false, length = 100)
    private String nombre;

    @Column(name = "s_apellido_paterno", nullable = false, length = 100)
    private String apellidoPaterno;

    @Column(name = "s_apellido_materno", length = 100)
    private String apellidoMaterno;

    @Column(name = "s_area", nullable = false, length = 150)
    private String area; 

    @Column(name = "s_cargo", length = 150)
    private String cargo; 

    @Column(name = "s_extension", length = 20)
    private String extension; 

    // --- DATOS TÉCNICOS DEL CORREO ---
    @Column(name = "s_correo", nullable = false, unique = true, length = 150)
    private String correo;

    @Column(name = "s_estado", nullable = false, length = 50)
    private String estado; // ACTIVO, SUSPENDIDO, BAJA, EN_PROCESO

    @Column(name = "s_cuota_almacenamiento", length = 30)
    private String cuotaAlmacenamiento; 
}
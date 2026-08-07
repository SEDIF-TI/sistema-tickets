package com.sedif.sistema_tickets.util.audit;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.time.LocalDateTime;

/**
 * Base de las entidades que llevan rastro de quien las creo y modifico, y
 * cuando.
 *
 * <p>Al ser {@code @MappedSuperclass}, no tiene tabla propia: sus cuatro
 * columnas se anaden a la de cada entidad que la hereda. El
 * {@code AuditingEntityListener} las rellena solo, sin que los servicios tengan
 * que asignarlas: las anotadas con {@code @CreatedDate} y {@code @CreatedBy} en
 * el alta, y las de {@code @LastModifiedDate} y {@code @LastModifiedBy} en cada
 * actualizacion. El autor lo aporta {@link AuditorAwareImpl}.</p>
 *
 * <p>Las columnas de creacion se declaran {@code updatable = false}, de modo
 * que quedan fijadas en el alta y ninguna modificacion posterior puede
 * alterarlas: es lo que mantiene util el rastro.</p>
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class Auditable {

    @CreatedDate
    @Column(name = "d_fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @LastModifiedDate
    @Column(name = "d_fecha_modificacion")
    private LocalDateTime fechaModificacion;

    @CreatedBy
    @Column(name = "s_creado_por", updatable = false, length = 100)
    private String creadoPor;

    @LastModifiedBy
    @Column(name = "s_modificado_por", length = 100)
    private String modificadoPor;

    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }

    public LocalDateTime getFechaModificacion() { return fechaModificacion; }
    public void setFechaModificacion(LocalDateTime fechaModificacion) { this.fechaModificacion = fechaModificacion; }

    public String getCreadoPor() { return creadoPor; }
    public void setCreadoPor(String creadoPor) { this.creadoPor = creadoPor; }

    public String getModificadoPor() { return modificadoPor; }
    public void setModificadoPor(String modificadoPor) { this.modificadoPor = modificadoPor; }
}
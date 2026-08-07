package com.sedif.sistema_tickets.util.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Conversion de texto a enum en los tipos que gobiernan el dominio.
 *
 * <p>Estos enums traducen lo que llega del cliente o de la base a una constante
 * del codigo. Su contrato es el mismo en todos: normalizan mayusculas y
 * espacios, y devuelven vacio ante un valor desconocido en lugar de elegir uno
 * por su cuenta. Esa ultima parte importa en los que deciden permisos, donde
 * adivinar equivaldria a conceder acceso.</p>
 */
@DisplayName("Enums de dominio: conversion desde texto")
class EnumsDeDominioTest {

    @Nested
    @DisplayName("EstadoTicket")
    class Estado {

        @ParameterizedTest(name = "\"{0}\" resuelve a EN_PROCESO")
        @ValueSource(strings = {"EN_PROCESO", "en_proceso", "  EN_PROCESO  "})
        void normalizaMayusculasYEspacios(String valor) {
            assertThat(EstadoTicket.desde(valor)).contains(EstadoTicket.EN_PROCESO);
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"RESUELTO", "ASIGNADO", "cualquier-cosa"})
        @DisplayName("un valor fuera del enum no resuelve a ninguno")
        void valorDesconocido(String valor) {
            assertThat(EstadoTicket.desde(valor)).isEmpty();
        }

        @Test
        @DisplayName("solo CERRADO es estado final")
        void estadoFinal() {
            assertThat(EstadoTicket.CERRADO.esFinal()).isTrue();
            assertThat(EstadoTicket.ABIERTO.esFinal()).isFalse();
            assertThat(EstadoTicket.EN_PROCESO.esFinal()).isFalse();
        }

        @Test
        @DisplayName("la etiqueta es legible y distinta del nombre de la constante")
        void etiquetaLegible() {
            assertThat(EstadoTicket.EN_PROCESO.getEtiqueta()).isEqualTo("En proceso");
        }
    }

    @Nested
    @DisplayName("Prioridad")
    class PrioridadTest {

        @ParameterizedTest(name = "\"{0}\" resuelve a URGENTE")
        @ValueSource(strings = {"URGENTE", "urgente", " Urgente "})
        void normaliza(String valor) {
            assertThat(Prioridad.desde(valor)).contains(Prioridad.URGENTE);
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"CRITICA", "MEDIA"})
        @DisplayName("un valor fuera del enum no resuelve a ninguno")
        void valorDesconocido(String valor) {
            assertThat(Prioridad.desde(valor)).isEmpty();
        }

        @Test
        @DisplayName("la prioridad por defecto es NORMAL")
        void porDefecto() {
            assertThat(Prioridad.porDefecto()).isEqualTo(Prioridad.NORMAL);
        }
    }

    @Nested
    @DisplayName("RolUsuario")
    class Rol {

        @ParameterizedTest(name = "\"{0}\" resuelve a ADMINISTRADOR")
        @ValueSource(strings = {"ADMINISTRADOR", "administrador", " Administrador ", "ROLE_ADMINISTRADOR"})
        @DisplayName("normaliza mayusculas, espacios y el prefijo de Spring Security")
        void normaliza(String valor) {
            assertThat(RolUsuario.desde(valor)).contains(RolUsuario.ADMINISTRADOR);
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"ADMIN", "SUPERVISOR", "root"})
        @DisplayName("un rol inventado no resuelve a ninguno")
        void rolDesconocido(String valor) {
            assertThat(RolUsuario.desde(valor)).isEmpty();
        }

        @Test
        @DisplayName("es() compara contra el rol concreto")
        void comparacionDirecta() {
            assertThat(RolUsuario.SOPORTE.es("SOPORTE")).isTrue();
            assertThat(RolUsuario.SOPORTE.es("ROLE_SOPORTE")).isTrue();
            assertThat(RolUsuario.SOPORTE.es("ADMINISTRADOR")).isFalse();
            assertThat(RolUsuario.SOPORTE.es(null)).isFalse();
        }

        @Test
        @DisplayName("el nombre en base de datos coincide con la constante")
        void nombreEnBd() {
            assertThat(RolUsuario.SOPORTE.nombreEnBd()).isEqualTo("SOPORTE");
        }
    }

    @Nested
    @DisplayName("NivelVision")
    class Vision {

        @ParameterizedTest(name = "\"{0}\" resuelve a GLOBAL")
        @ValueSource(strings = {"GLOBAL", "global", " Global "})
        void normaliza(String valor) {
            assertThat(NivelVision.desde(valor)).contains(NivelVision.GLOBAL);
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"TODO", "TOTAL", "ADMIN"})
        @DisplayName("un nivel desconocido no resuelve a GLOBAL")
        void nivelDesconocidoNoConcedeAccesoTotal(String valor) {
            // Es la garantia que sostiene el filtrado de la bandeja: un nivel
            // mal configurado no puede acabar mostrando todos los tickets.
            assertThat(NivelVision.desde(valor)).isEmpty();
        }
    }

    @Nested
    @DisplayName("PlanTrabajo")
    class Plan {

        @Test
        @DisplayName("una clave del catalogo se acepta")
        void claveValida() {
            Integer clave = PlanTrabajo.values()[0].getClave();
            assertThat(PlanTrabajo.esClaveValida(clave)).isTrue();
            assertThat(PlanTrabajo.porClave(clave)).isPresent();
        }

        @Test
        @DisplayName("una clave fuera del catalogo se rechaza")
        void claveInvalida() {
            assertThat(PlanTrabajo.esClaveValida(9999)).isFalse();
            assertThat(PlanTrabajo.esClaveValida(null)).isFalse();
            assertThat(PlanTrabajo.porClave(9999)).isEmpty();
        }
    }
}

import { useState, useEffect, useMemo } from 'react';
import {
    Box, Typography, Button, Paper, Stack, Divider, Alert, CircularProgress
} from '@mui/material';
import SendIcon from '@mui/icons-material/Send';
import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import ConfirmationNumberIcon from '@mui/icons-material/ConfirmationNumber';
import { useNavigate } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';

import { ticketService } from '../../services/ticketService';
import { userService } from '../../services/userService';
import { useNotification } from '../../context/NotificationContext.jsx';
import { useNetworkStatus } from '../../hooks/useNetworkStatus.jsx';
import { useRol } from '../../hooks/useRol.jsx';
import { esquemaTicket } from '../../util/esquemas';

import CampoFormulario from '../../components/CampoFormulario';

/**
 * Alta de un ticket de soporte.
 *
 * Cambios respecto a la versión anterior:
 *  - Cada campo tenía su `useState` y la validación era una cadena de `if` en
 *    el manejador de envío: el usuario veía un solo error genérico arriba, sin
 *    saber qué campo corregir. Ahora valida React Hook Form contra un esquema
 *    que espeja las anotaciones del DTO de Java, y el error sale bajo su campo.
 *  - No había protección contra el doble envío: pulsar dos veces con la red
 *    lenta creaba dos tickets.
 *  - El campo `prioridad` no se enviaba nunca, así que todos los tickets
 *    nacían en NORMAL y la gráfica de prioridades del panel era una sola barra.
 *  - El color guinda estaba escrito a mano y no coincidía con el del tema.
 *  - El rol se deducía probando `user.rol`, `user.role` y `'ADMIN'`; el backend
 *    solo envía `user.rol` (ver `JwtResponse`).
 */
export default function FormularioTicket() {
    const navigate = useNavigate();
    // El aviso de éxito lo muestra la pantalla de destino, que recibe el
    // mensaje por `state` al navegar.
    const { notificarError } = useNotification();
    const { esAdministrador, esSoporte, esTecnico } = useRol();

    const estadoRed = useNetworkStatus();
    const sinConexion = Boolean(estadoRed.sinConexion ?? estadoRed);

    const [prioridades, setPrioridades] = useState([]);
    const [tecnicos, setTecnicos] = useState([]);
    const [cargandoCatalogos, setCargandoCatalogos] = useState(true);

    // La sede solo se pide a quien levanta tickets de sedes ajenas.
    const esquema = useMemo(() => esquemaTicket(esTecnico), [esTecnico]);

    const {
        control,
        handleSubmit,
        formState: { isSubmitting },
    } = useForm({
        resolver: zodResolver(esquema),
        // Se valida al salir del campo, no en cada tecla: marcar en rojo
        // mientras se escribe la primera letra es hostil.
        mode: 'onBlur',
        defaultValues: {
            solicitante: '',
            titulo: '',
            descripcion: '',
            sede: '',
            prioridad: 'NORMAL',
            usuarioSoporteId: '',
        },
    });

    // --- Catálogos --------------------------------------------------------
    useEffect(() => {
        let cancelado = false;

        const cargar = async () => {
            try {
                // Las dos peticiones van en paralelo: son independientes.
                const [respPrioridades, respTecnicos] = await Promise.all([
                    ticketService.getPrioridades(),
                    esAdministrador ? userService.getSoporte() : Promise.resolve(null),
                ]);

                if (cancelado) return;

                setPrioridades(respPrioridades?.data ?? []);
                setTecnicos(respTecnicos?.data ?? []);
            } catch (error) {
                if (!cancelado) notificarError(error);
            } finally {
                if (!cancelado) setCargandoCatalogos(false);
            }
        };

        cargar();
        return () => { cancelado = true; };
    }, [esAdministrador, notificarError]);

    // --- Envío ------------------------------------------------------------
    const enviar = async (datos) => {
        if (sinConexion) {
            notificarError('No hay conexión con el servidor. Inténtalo de nuevo cuando se restablezca.');
            return;
        }

        try {
            const respuesta = await ticketService.create({
                titulo: datos.titulo.trim(),
                descripcion: datos.descripcion.trim(),
                solicitante: datos.solicitante.trim(),
                sede: esTecnico ? datos.sede?.trim() || null : null,
                prioridad: datos.prioridad,
                usuarioSoporteId: datos.usuarioSoporteId
                    ? Number(datos.usuarioSoporteId)
                    : null,
            });

            const mensaje = respuesta?.mensaje || 'Ticket creado correctamente.';

            // Cada rol vuelve a la pantalla donde verá el ticket recién creado.
            navigate(esSoporte ? '/soporte/bandeja' : '/empleado/historial', {
                state: { mensajeExito: mensaje },
            });
        } catch (error) {
            notificarError(error);
        }
    };

    const opcionesPrioridad = prioridades.map((p) => ({
        valor: p.valor,
        etiqueta: p.etiqueta,
    }));

    const opcionesTecnico = [
        { valor: '', etiqueta: 'Asignación automática (por carga de trabajo)' },
        ...tecnicos.map((t) => ({
            valor: String(t.id),
            etiqueta: `${t.nombre}${t.disponibleSoporte ? '' : ' — ocupado'}`,
        })),
    ];

    if (cargandoCatalogos) {
        return (
            <Box sx={{ display: 'flex', justifyContent: 'center', py: 8 }}>
                <CircularProgress aria-label="Cargando el formulario" />
            </Box>
        );
    }

    return (
        <Box sx={{ maxWidth: 780, mx: 'auto' }}>
            <Box sx={{ mb: 3 }}>
                <Typography
                    variant="h4"
                    component="h2"
                    color="primary.main"
                    sx={{ display: 'flex', alignItems: 'center', gap: 1 }}
                >
                    <ConfirmationNumberIcon fontSize="large" aria-hidden="true" />
                    Levantar un ticket
                </Typography>
                <Typography variant="body2" color="text.secondary">
                    Describe la falla con el mayor detalle posible: ayuda al técnico
                    a llegar preparado.
                </Typography>
            </Box>

            {sinConexion && (
                <Alert severity="warning" sx={{ mb: 3 }}>
                    Sin conexión con el servidor. Puedes escribir el reporte, pero no
                    podrás enviarlo hasta que se restablezca.
                </Alert>
            )}

            <Paper variant="outlined" sx={{ p: { xs: 2.5, sm: 4 } }}>
                {/* noValidate: la validación la hace el esquema, no el navegador,
                    que mostraría mensajes en el idioma del sistema operativo. */}
                <form onSubmit={handleSubmit(enviar)} noValidate>
                    <Stack spacing={3}>
                        <CampoFormulario
                            control={control}
                            nombre="solicitante"
                            etiqueta="Persona afectada"
                            obligatorio
                            maximo={100}
                            mayusculas
                            ayuda="Quién tiene el problema, aunque no seas tú."
                            placeholder="NOMBRE COMPLETO"
                        />

                        <CampoFormulario
                            control={control}
                            nombre="titulo"
                            etiqueta="Falla principal"
                            obligatorio
                            maximo={255}
                            mayusculas
                            ayuda="Resume el problema en una línea."
                            placeholder="EJ. LA IMPRESORA NO SE CONECTA A LA RED"
                        />

                        {/* La sede solo la piden soporte y administración: el
                            empleado siempre reporta desde la suya. */}
                        {esTecnico && (
                            <CampoFormulario
                                control={control}
                                nombre="sede"
                                etiqueta="Sede"
                                obligatorio
                                maximo={255}
                                mayusculas
                                ayuda="Dónde se encuentra el equipo con la falla."
                            />
                        )}

                        <CampoFormulario
                            control={control}
                            nombre="descripcion"
                            etiqueta="Descripción de la falla"
                            obligatorio
                            maximo={5000}
                            mayusculas
                            multiline
                            rows={5}
                            ayuda="Qué acciones provocan el problema y qué mensajes aparecen."
                            placeholder="AL ENCENDER EL EQUIPO APARECE UNA PANTALLA AZUL Y SE REINICIA…"
                        />

                        <CampoFormulario
                            control={control}
                            nombre="prioridad"
                            etiqueta="Prioridad"
                            obligatorio
                            opciones={opcionesPrioridad}
                            ayuda="Urgente solo si impide trabajar por completo."
                        />

                        {esAdministrador && (
                            <>
                                <Divider>
                                    <Typography variant="caption" color="text.secondary">
                                        Asignación
                                    </Typography>
                                </Divider>

                                <CampoFormulario
                                    control={control}
                                    nombre="usuarioSoporteId"
                                    etiqueta="Técnico asignado"
                                    opciones={opcionesTecnico}
                                    ayuda="Si no eliges a nadie, se asigna al técnico con menos carga."
                                />
                            </>
                        )}
                    </Stack>

                    {/* En móvil los botones se apilan a lo ancho. */}
                    <Stack
                        direction={{ xs: 'column-reverse', sm: 'row' }}
                        spacing={2}
                        justifyContent="flex-end"
                        sx={{ mt: 4 }}
                    >
                        <Button
                            variant="text"
                            color="inherit"
                            startIcon={<ArrowBackIcon />}
                            onClick={() => navigate(-1)}
                            disabled={isSubmitting}
                            fullWidth={false}
                            sx={{ width: { xs: '100%', sm: 'auto' } }}
                        >
                            Cancelar
                        </Button>

                        <Button
                            type="submit"
                            variant="contained"
                            size="large"
                            startIcon={!isSubmitting && <SendIcon />}
                            // isSubmitting bloquea el doble envío: antes, dos
                            // pulsaciones seguidas creaban dos tickets.
                            disabled={isSubmitting || sinConexion}
                            sx={{ width: { xs: '100%', sm: 'auto' } }}
                        >
                            {isSubmitting ? 'Enviando…' : 'Enviar ticket'}
                        </Button>
                    </Stack>
                </form>
            </Paper>
        </Box>
    );
}

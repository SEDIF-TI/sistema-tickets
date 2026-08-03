import { Controller } from 'react-hook-form';
import { TextField, MenuItem } from '@mui/material';

/**
 * Campo de formulario: une React Hook Form con el `TextField` de MUI.
 *
 * MUI aporta el componente visual pero no gestiona el estado ni la validación
 * del formulario; es su límite declarado. Sin esta unión, cada pantalla
 * declaraba un `useState` por campo y comprobaba las reglas a mano dentro del
 * manejador de envío.
 *
 * Este componente resuelve de forma uniforme lo que antes se repetía:
 *
 *  - Muestra el error del campo debajo, y el texto de ayuda cuando no lo hay,
 *    en lugar de un `Alert` genérico arriba que no señalaba qué corregir.
 *  - Marca `aria-invalid` para que un lector de pantalla anuncie el fallo.
 *  - Cuenta los caracteres cuando el campo tiene límite: el usuario ve cuánto
 *    le queda antes de que el servidor lo rechace.
 *  - Con `opciones`, se convierte en desplegable.
 *  - Con `mayusculas`, transforma la entrada al escribir, criterio que el
 *    sistema aplica a los textos que acaban en documentos oficiales.
 *
 * @param {object}   control     El `control` de `useForm`.
 * @param {string}   nombre      Nombre del campo dentro del formulario.
 * @param {string}   etiqueta    Texto del `label`.
 * @param {Array}    opciones    `[{ valor, etiqueta }]`; lo convierte en select.
 * @param {number}   maximo      Límite de caracteres, para el contador.
 * @param {boolean}  mayusculas  Transforma a mayúsculas mientras se escribe.
 * @param {string}   ayuda       Texto de ayuda cuando no hay error.
 */
export default function CampoFormulario({
    control,
    nombre,
    etiqueta,
    opciones,
    maximo,
    mayusculas = false,
    ayuda,
    obligatorio = false,
    ...props
}) {
    return (
        <Controller
            name={nombre}
            control={control}
            render={({ field, fieldState }) => {
                const valor = field.value ?? '';
                const hayError = Boolean(fieldState.error);

                // El contador solo aparece si el campo tiene límite. Se cuenta
                // el texto tal cual se ve, no el recortado.
                const contador = maximo ? `${valor.length}/${maximo}` : '';

                let textoAyuda = fieldState.error?.message || ayuda || '';
                if (contador) {
                    textoAyuda = textoAyuda ? `${textoAyuda} · ${contador}` : contador;
                }

                return (
                    <TextField
                        {...field}
                        {...props}
                        value={valor}
                        onChange={(e) => {
                            const texto = e.target.value;
                            field.onChange(mayusculas ? texto.toUpperCase() : texto);
                        }}
                        select={Boolean(opciones)}
                        label={etiqueta}
                        required={obligatorio}
                        error={hayError}
                        helperText={textoAyuda}
                        fullWidth
                        // El límite se aplica al escribir, además de validarse:
                        // es preferible impedir el exceso a avisar después.
                        slotProps={{
                            htmlInput: {
                                maxLength: maximo,
                                'aria-invalid': hayError,
                            },
                        }}
                    >
                        {opciones?.map((op) => (
                            <MenuItem key={op.valor} value={op.valor}>
                                {op.etiqueta}
                            </MenuItem>
                        ))}
                    </TextField>
                );
            }}
        />
    );
}

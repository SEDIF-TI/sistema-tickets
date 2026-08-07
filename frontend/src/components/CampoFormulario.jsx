import { Controller } from 'react-hook-form';
import { TextField, MenuItem } from '@mui/material';

/**
 * Campo de formulario: une React Hook Form con el `TextField` de MUI.
 *
 * MUI aporta el componente visual pero no gestiona el estado ni la validación;
 * el `Controller` es quien conecta ambas partes, de modo que la pantalla no
 * declara un `useState` por campo ni comprueba reglas dentro del manejador de
 * envío. Las reglas viven en los esquemas de `util/esquemas.js`.
 *
 * Lo que el componente resuelve en cada campo:
 *
 *  - Muestra el error justo debajo, en el sitio donde hay que corregir, y el
 *    texto de ayuda mientras no lo hay.
 *  - Marca `aria-invalid` para que un lector de pantalla anuncie el fallo.
 *  - Cuenta los caracteres cuando el campo tiene límite, de manera que se vea
 *    cuánto queda antes de que el servidor lo rechace.
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

                // El contador solo aparece si el campo declara límite, y cuenta
                // el texto tal como se ve, sin recortar los espacios que el
                // esquema sí descarta al validar.
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
                        // El límite se impone también en el propio input, no
                        // solo en la validación: impedir el exceso ahorra
                        // escribir un texto que luego habría que recortar.
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

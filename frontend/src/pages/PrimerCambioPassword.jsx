// 1. Asegúrate de extraer la nueva función del contexto al inicio de tu componente:
const { marcarPasswordCambiada } = useContext(AuthContext);
const navigate = useNavigate();

// 2. En tu función de envío:
const handleSubmit = async (e) => {
    e.preventDefault();
    try {
        // Tu petición actual (ejemplo):
        await api.put('/usuarios/password', { nuevaPassword: password });
        
        // ¡AQUÍ ESTÁ LA SOLUCIÓN!
        marcarPasswordCambiada(); // Rompemos el bloqueo de seguridad actualizando el estado
        
        // Redirigimos a la raíz. 
        // Como el estado cambió, App.jsx evaluará tu rol y te mandará a tu vista correspondiente.
        navigate('/'); 
        
    } catch (error) {
        console.error("Error al cambiar la contraseña:", error);
    }
};
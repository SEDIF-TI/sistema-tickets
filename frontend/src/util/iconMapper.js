import AddCircleIcon from '@mui/icons-material/AddCircle';
import AssignmentIcon from '@mui/icons-material/Assignment';
import DashboardIcon from '@mui/icons-material/Dashboard';

// Este objeto asocia el texto del backend con el componente real de MUI
const iconMap = {
    'AddCircleIcon': AddCircleIcon,
    'AssignmentIcon': AssignmentIcon,
    'DashboardIcon': DashboardIcon,
};

export const getIcon = (iconName) => {
    // Si el icono existe en nuestro mapa, lo devuelve; si no, pone uno por defecto
    const IconComponent = iconMap[iconName];
    return IconComponent ? IconComponent : DashboardIcon;
};
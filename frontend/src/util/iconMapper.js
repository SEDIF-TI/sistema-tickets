import DashboardIcon from '@mui/icons-material/Dashboard';
import PeopleIcon from '@mui/icons-material/People';
import BusinessIcon from '@mui/icons-material/Business';
import ConfirmationNumberIcon from '@mui/icons-material/ConfirmationNumber';
import HistoryIcon from '@mui/icons-material/History'; // <-- Ícono de reloj para historial
import LabelIcon from '@mui/icons-material/Label';

export const getIcon = (iconName) => {
    switch (iconName) {
        case 'dashboard':
            return DashboardIcon;
        case 'usuarios':
            return PeopleIcon;
        case 'areas':
            return BusinessIcon;
        case 'tickets':
            return ConfirmationNumberIcon;
        case 'historial': // <-- Agregamos el caso exacto
            return HistoryIcon; 
        default:
            return LabelIcon; // Cambiamos el por defecto
    }
};
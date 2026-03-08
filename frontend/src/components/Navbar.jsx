import '../App.css';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';

export default function Navbar() {
    const { user, isAuthenticated, logout } = useAuth();
    const navigate = useNavigate();

    const handleLogout = async () => {
        await logout();
        navigate('/login');
    };

    if (!isAuthenticated) return null;

    const isCiudadano = user?.rol === 'CIUDADANO';
    const isGestor = user?.rol === 'GESTOR' || user?.rol === 'ADMIN';
    const isAdmin = user?.rol === 'ADMIN';

    const homeRoute = isCiudadano ? '/solicitudes/mis' : '/bandeja';

    return (
        <nav className="navbar">
            <div className="navbar-inner">
                <Link to={homeRoute} className="navbar-brand">
                    <span className="navbar-logo">RDAM</span>
                </Link>

                <div className="navbar-links">
                    {isCiudadano && (
                        <>
                            <Link to="/solicitudes/mis" className="nav-link">Mis Solicitudes</Link>
                            <Link to="/solicitudes/nueva" className="nav-link">Nueva Solicitud</Link>
                        </>
                    )}
                    {isGestor && (
                        <>
                            <Link to="/bandeja" className="nav-link">Bandeja</Link>
                        </>
                    )}
                    {isAdmin && (
                        <>
                            <Link to="/usuarios" className="nav-link">Usuarios</Link>
                            <Link to="/reportes" className="nav-link">Reportes</Link>
                        </>
                    )}
                </div>

                <div className="navbar-user">
                    <span className="navbar-user-name">
                        {user?.nombre} {user?.apellido}
                    </span>
                    <span className={`badge badge-${user?.rol?.toLowerCase()}`}>
                        {user?.rol}
                    </span>
                    <button onClick={handleLogout} className="btn btn-sm btn-secondary">
                        Salir
                    </button>
                </div>
            </div>
        </nav>
    );
}

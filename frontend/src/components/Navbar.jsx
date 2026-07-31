import '../App.css';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';
import logoImg from '../assets/Logo.png';

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
                <Link to={homeRoute} className="navbar-brand" style={{ display: 'flex', alignItems: 'center', gap: '20px' }}>
                    <img src={logoImg} alt="RDAM Logo" style={{ height: '64px', width: 'auto' }} />
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
                            <Link to="/reportes" className="nav-link">Reportes & Métricas</Link>
                        </>
                    )}
                </div>

                <div className="navbar-user">
                    {isCiudadano ? (
                        <span className="navbar-user-email" style={{
                            marginRight: '12px',
                            fontWeight: '500',
                            color: 'var(--text-color)',
                            padding: '4px 10px',
                            border: '1px solid rgba(198, 172, 119, 0.25)',
                            borderRadius: '8px',
                            backgroundColor: 'transparent',
                            display: 'flex',
                            flexDirection: 'column',
                            alignItems: 'flex-start',
                            lineHeight: '1.2'
                        }}>
                            <span style={{ fontSize: '0.75em', color: 'var(--text-muted, gray)', fontWeight: 'normal' }}>Email ingresado:</span>
                            <span style={{ fontSize: '0.9em' }}>{user?.email}</span>
                        </span>
                    ) : (
                        <span className="navbar-user-name" style={{
                            marginRight: '12px',
                            fontWeight: '500',
                            color: 'var(--text-color)',
                            padding: '4px 10px',
                            border: '1px solid rgba(198, 172, 119, 0.25)',
                            borderRadius: '8px',
                            backgroundColor: 'transparent'
                        }}>
                            {user?.nombre} {user?.apellido}
                        </span>
                    )}
                    <span className={`badge badge-${user?.rol?.toLowerCase()}`} style={{ marginRight: '10px' }}>
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

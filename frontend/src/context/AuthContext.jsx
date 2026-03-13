import { createContext, useState, useCallback } from 'react';
import api from '../api/axios';

export const AuthContext = createContext(null);

export function AuthProvider({ children }) {
    const [user, setUser] = useState(() => {
        const stored = localStorage.getItem('user');
        return stored ? JSON.parse(stored) : null;
    });
    const [loading, setLoading] = useState(false);

    const isAuthenticated = !!user;
    const portal = user?.portal || null;
    const rol = user?.rol || null;

    // ─── Ciudadano: Paso 1 — Solicitar código por email ───────────────────
    const solicitarCodigo = useCallback(async (email) => {
        setLoading(true);
        try {
            const { data } = await api.post('/auth/solicitar-codigo', { email });
            return data;
        } finally {
            setLoading(false);
        }
    }, []);

    // ─── Ciudadano: Paso 2 — Validar código ──────────────────────────────
    const validarCodigo = useCallback(async (email, codigo) => {
        setLoading(true);
        try {
            const { data } = await api.post('/auth/validar-codigo', { email, codigo });
            localStorage.setItem('access_token', data.accessToken);
            localStorage.setItem('refresh_token', data.refreshToken);
            const userData = { ...data.usuario, portal: data.portal };
            localStorage.setItem('user', JSON.stringify(userData));
            setUser(userData);
            return userData;
        } finally {
            setLoading(false);
        }
    }, []);

    // ─── Ciudadano: Paso 3 — Completar perfil (primer ingreso) ───────────
    const completarPerfil = useCallback(async (datos) => {
        setLoading(true);
        try {
            const { data } = await api.post('/auth/completar-perfil', datos);
            // Actualizar datos del usuario en localStorage y estado
            const userData = { ...data.usuario, portal: data.portal };
            localStorage.setItem('user', JSON.stringify(userData));
            setUser(userData);
            return userData;
        } finally {
            setLoading(false);
        }
    }, []);

    // ─── Login Admin/Gestor (con contraseña) ─────────────────────────────
    const loginAdmin = useCallback(async (identificador, password) => {
        setLoading(true);
        try {
            const { data } = await api.post('/auth/admin/login', { identificador, password });
            localStorage.setItem('access_token', data.accessToken);
            localStorage.setItem('refresh_token', data.refreshToken);
            const userData = { ...data.usuario, portal: data.portal };
            localStorage.setItem('user', JSON.stringify(userData));
            setUser(userData);
            return userData;
        } finally {
            setLoading(false);
        }
    }, []);

    // ─── Logout ─────────────────────────────────────────────────────────
    const logout = useCallback(async () => {
        try {
            const refreshToken = localStorage.getItem('refresh_token');
            if (refreshToken) {
                await api.post('/auth/logout', { refreshToken });
            }
        } catch {
            // silently fail — we're logging out anyway
        } finally {
            localStorage.removeItem('access_token');
            localStorage.removeItem('refresh_token');
            localStorage.removeItem('user');
            setUser(null);
        }
    }, []);

    const value = {
        user,
        loading,
        isAuthenticated,
        portal,
        rol,
        solicitarCodigo,
        validarCodigo,
        completarPerfil,
        loginAdmin,
        logout,
    };

    return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

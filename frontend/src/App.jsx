import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import Layout from './components/Layout';
import ProtectedRoute from './components/ProtectedRoute';

// ─── Pages ──────────────────────────────────────────────────────────────────
import LoginCiudadano from './pages/LoginCiudadano';
import LoginAdmin from './pages/LoginAdmin';
import MisSolicitudes from './pages/ciudadano/MisSolicitudes';
import NuevaSolicitud from './pages/ciudadano/NuevaSolicitud';
import DetalleSolicitud from './pages/ciudadano/DetalleSolicitud';
import ResultadoPago from './pages/ciudadano/ResultadoPago';
import Bandeja from './pages/interno/Bandeja';
import Usuarios from './pages/admin/Usuarios';
import Reportes from './pages/admin/Reportes';

export default function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          {/* ─── Rutas públicas (auth) ─────────────────────────────── */}
          <Route path="/login" element={<LoginCiudadano />} />
          <Route path="/admin/login" element={<LoginAdmin />} />
          {/* Resultado de pago — accesible sin login (PlusPagos redirige acá) */}
          <Route path="/pago/resultado" element={<ResultadoPago />} />

          {/* ─── Rutas protegidas con Layout ────────────────────────── */}
          <Route element={
            <ProtectedRoute>
              <Layout />
            </ProtectedRoute>
          }>
            {/* Ciudadano */}
            <Route path="/solicitudes/mis" element={
              <ProtectedRoute roles={['CIUDADANO']}>
                <MisSolicitudes />
              </ProtectedRoute>
            } />
            <Route path="/solicitudes/nueva" element={
              <ProtectedRoute roles={['CIUDADANO']}>
                <NuevaSolicitud />
              </ProtectedRoute>
            } />

            {/* Shared: detalle de solicitud (ciudadano + gestor + admin) */}
            <Route path="/solicitudes/:id" element={<DetalleSolicitud />} />

            {/* Gestor / Admin */}
            <Route path="/bandeja" element={
              <ProtectedRoute roles={['GESTOR', 'ADMIN']}>
                <Bandeja />
              </ProtectedRoute>
            } />

            {/* Solo Admin */}
            <Route path="/usuarios" element={
              <ProtectedRoute roles={['ADMIN']}>
                <Usuarios />
              </ProtectedRoute>
            } />
            <Route path="/reportes" element={
              <ProtectedRoute roles={['ADMIN']}>
                <Reportes />
              </ProtectedRoute>
            } />
          </Route>

          {/* ─── Catch-all ──────────────────────────────────────────── */}
          <Route path="*" element={<Navigate to="/login" replace />} />
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  );
}

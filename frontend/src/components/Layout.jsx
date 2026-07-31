import { Outlet } from 'react-router-dom';
import Navbar from './Navbar';
import HelpWidget from './HelpWidget';

export default function Layout() {
    return (
        <div className="app-container">
            <Navbar />
            <main className="main-content">
                <Outlet />
            </main>
            <HelpWidget />
        </div>
    );
}

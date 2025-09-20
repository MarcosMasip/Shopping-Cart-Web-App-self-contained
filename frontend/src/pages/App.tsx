import React from 'react';
import { Outlet, Link, useLocation } from 'react-router-dom';

export default function App() {
  const location = useLocation();
  return (
    <div className="layout">
      <header>
        <h1>Shopping Cart Demo</h1>
        <nav>
          <Link to="/" className={location.pathname === '/' ? 'active' : ''}>Products</Link>
          <Link to="/cart" className={location.pathname === '/cart' ? 'active' : ''}>Cart</Link>
        </nav>
      </header>
      <main>
        <Outlet />
      </main>
      <footer>
        <small>Self-contained demo &copy; 2025</small>
      </footer>
    </div>
  );
}

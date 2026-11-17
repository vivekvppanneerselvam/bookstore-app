import { Link, NavLink, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import { useCart } from '../api/queries';

export function Header() {
  const { user, status, logout } = useAuth();
  const { data: cart } = useCart(status === 'authenticated');
  const navigate = useNavigate();

  const handleLogout = async () => {
    await logout();
    navigate('/');
  };

  return (
    <header className="site-header">
      <div className="container header-inner">
        <Link to="/" className="brand">
          Bookstore
        </Link>
        <nav className="nav">
          <NavLink to="/" end>
            Books
          </NavLink>
          <NavLink to="/cart" className="cart-link">
            Cart
            {cart && cart.itemCount > 0 && <span className="badge">{cart.itemCount}</span>}
          </NavLink>
        </nav>
        <div className="auth-area">
          {status === 'authenticated' && user ? (
            <>
              <span className="muted">Hi, {user.displayName}</span>
              <button type="button" className="btn ghost" onClick={handleLogout}>
                Sign out
              </button>
            </>
          ) : status === 'anonymous' ? (
            <>
              <Link className="btn ghost" to="/login">
                Sign in
              </Link>
              <Link className="btn primary" to="/register">
                Create account
              </Link>
            </>
          ) : null}
        </div>
      </div>
    </header>
  );
}

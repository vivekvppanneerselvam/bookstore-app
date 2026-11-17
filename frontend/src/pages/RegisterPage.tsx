import { useState, type FormEvent } from 'react';
import { Link, Navigate, useNavigate } from 'react-router-dom';
import { getErrorMessage } from '../api/client';
import { useAuth } from '../auth/AuthContext';

export function RegisterPage() {
  const { register, status } = useAuth();
  const navigate = useNavigate();

  const [displayName, setDisplayName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  if (status === 'loading') {
    return <div className="page-loading">Loading…</div>;
  }
  if (status === 'authenticated') {
    return <Navigate to="/" replace />;
  }

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      await register(email.trim(), password, displayName.trim());
      navigate('/', { replace: true });
    } catch (err) {
      setError(getErrorMessage(err, 'Unable to create the account.'));
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <section className="auth-card card">
      <h1>Create account</h1>
      <p className="muted">It takes a few seconds.</p>
      {error && <div className="alert error">{error}</div>}
      <form onSubmit={handleSubmit} className="form">
        <label>
          Display name
          <input
            type="text"
            value={displayName}
            onChange={(event) => setDisplayName(event.target.value)}
            autoComplete="name"
            maxLength={80}
            required
          />
        </label>
        <label>
          Email
          <input
            type="email"
            value={email}
            onChange={(event) => setEmail(event.target.value)}
            autoComplete="email"
            required
          />
        </label>
        <label>
          Password
          <input
            type="password"
            value={password}
            onChange={(event) => setPassword(event.target.value)}
            autoComplete="new-password"
            minLength={8}
            maxLength={72}
            required
          />
          <span className="muted small">At least 8 characters.</span>
        </label>
        <button type="submit" className="btn primary full" disabled={submitting}>
          {submitting ? 'Creating account…' : 'Create account'}
        </button>
      </form>
      <p className="muted small">
        Already registered? <Link to="/login">Sign in</Link>
      </p>
    </section>
  );
}

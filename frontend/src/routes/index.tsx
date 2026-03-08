import { Routes, Route, Navigate } from 'react-router-dom';
import { useAuth } from '../providers/auth';
import { LoginPage } from '../pages/login';
import { RegisterPage } from '../pages/register';
import { HomePage } from '../pages/home';

export const AppRouter = () => {
  const { user, loading } = useAuth();

  if (loading) {
    return <div>Loading...</div>;
  }

  return (
    <Routes>
      <Route path="/login" element={user ? <Navigate to="/" /> : <LoginPage />} />
      <Route path="/register" element={user ? <Navigate to="/" /> : <RegisterPage />} />
      <Route path="/" element={user ? <HomePage /> : <Navigate to="/login" />} />
    </Routes>
  );
};

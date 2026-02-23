import { useState } from 'react';
import { createUserWithEmailAndPassword } from 'firebase/auth';
import { auth } from '../../lib/firebase';
import { Button, TextField, Box, Typography, Link } from '@mui/material';
import { Link as RouterLink } from 'react-router-dom';

const NEON_GREEN = '#39FF14';
const NAVY = '#0A1628';

export const RegisterPage = () => {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [error, setError] = useState<string | null>(null);

  const handleRegister = async () => {
    if (password !== confirmPassword) {
      setError('Passwords do not match');
      return;
    }
    try {
      await createUserWithEmailAndPassword(auth, email, password);
    } catch (err: any) {
      setError(err.message);
    }
  };

  return (
    <Box
      sx={{
        height: '100vh',
        width: '100vw',
        maxWidth: '100vw',
        background: `linear-gradient(160deg, ${NEON_GREEN} 0%, #00BB44 25%, #0A4A5A 60%, ${NAVY} 100%)`,
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        overflow: 'hidden',
        position: 'relative',
      }}
    >
      {/* Decorative circles - top right */}
      <Box
        sx={{
          position: 'absolute',
          top: -100,
          right: -100,
          width: 340,
          height: 340,
          borderRadius: '50%',
          background: 'rgba(57, 255, 20, 0.3)',
          pointerEvents: 'none',
        }}
      />
      <Box
        sx={{
          position: 'absolute',
          top: 60,
          right: 60,
          width: 200,
          height: 200,
          borderRadius: '50%',
          background: 'rgba(57, 255, 20, 0.15)',
          pointerEvents: 'none',
        }}
      />
      {/* Decorative circles - bottom left */}
      <Box
        sx={{
          position: 'absolute',
          bottom: -100,
          left: -100,
          width: 300,
          height: 300,
          borderRadius: '50%',
          background: 'rgba(10, 22, 40, 0.6)',
          pointerEvents: 'none',
        }}
      />
      <Box
        sx={{
          position: 'absolute',
          bottom: 60,
          left: 60,
          width: 170,
          height: 170,
          borderRadius: '50%',
          background: 'rgba(10, 22, 40, 0.4)',
          pointerEvents: 'none',
        }}
      />

      {/* Main content */}
      <Box sx={{ width: '100%', maxWidth: 400, px: 3, zIndex: 1 }}>
        {/* Heading */}
        <Box sx={{ mb: 3 }}>
          <Typography
            variant="h4"
            sx={{
              color: 'white',
              fontWeight: 800,
              whiteSpace: 'nowrap',
              textShadow: '0 2px 8px rgba(0,0,0,0.3)',
            }}
          >
            Create Your Account!
          </Typography>
        </Box>

        {/* Form card */}
        <Box
          sx={{
            background: 'rgba(255, 255, 255, 0.97)',
            borderRadius: 4,
            p: 4,
            boxShadow: '0 20px 60px rgba(0,0,0,0.35)',
          }}
        >
          <TextField
            label="Email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            margin="normal"
            fullWidth
            variant="outlined"
          />
          <TextField
            label="Password"
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            margin="normal"
            fullWidth
            variant="outlined"
          />
          <TextField
            label="Confirm Password"
            type="password"
            value={confirmPassword}
            onChange={(e) => setConfirmPassword(e.target.value)}
            margin="normal"
            fullWidth
            variant="outlined"
          />

          {error && (
            <Typography color="error" sx={{ fontSize: '0.85rem', mt: 1 }}>
              {error}
            </Typography>
          )}

          <Button
            onClick={handleRegister}
            variant="contained"
            fullWidth
            sx={{
              mt: 2,
              py: 1.5,
              borderRadius: 8,
              backgroundColor: NEON_GREEN,
              color: NAVY,
              fontWeight: 700,
              fontSize: '1rem',
              letterSpacing: 1,
              boxShadow: `0 4px 20px rgba(57, 255, 20, 0.4)`,
              '&:hover': {
                backgroundColor: '#2DE010',
                boxShadow: `0 6px 24px rgba(57, 255, 20, 0.6)`,
              },
            }}
          >
            SIGN UP
          </Button>
        </Box>

        {/* Footer link */}
        <Box sx={{ textAlign: 'center', mt: 3 }}>
          <Typography sx={{ color: 'rgba(255,255,255,0.85)', fontSize: '0.9rem' }}>
            Already have an account?{' '}
            <Link
              component={RouterLink}
              to="/login"
              sx={{ color: NEON_GREEN, fontWeight: 700, textDecoration: 'none' }}
            >
              Sign in
            </Link>
          </Typography>
        </Box>
      </Box>
    </Box>
  );
};

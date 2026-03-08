import { Button, Box, Typography } from '@mui/material';
import { signOut } from 'firebase/auth';
import { auth } from '../../lib/firebase';

const NEON_GREEN = '#39FF14';
const NAVY = '#0A1628';

export const HomePage = () => {
  const handleLogout = async () => {
    await signOut(auth);
  };

  return (
    <Box sx={{ height: '100vh', width: '100vw', display: 'flex', flexDirection: 'column', overflow: 'hidden' }}>
      {/* Gradient header with rounded bottom corners */}
      <Box
        sx={{
          width: '100%',
          background: `linear-gradient(160deg, ${NEON_GREEN} 0%, #00BB44 25%, #0A4A5A 60%, ${NAVY} 100%)`,
          position: 'relative',
          overflow: 'hidden',
          px: 3,
          pt: 3,
          pb: 5,
          flexShrink: 0,
          borderBottomLeftRadius: 16,
          borderBottomRightRadius: 16,
        }}
      >
        {/* Decorative circles */}
        <Box
          sx={{
            position: 'absolute',
            top: -80,
            right: -80,
            width: 220,
            height: 220,
            borderRadius: '50%',
            background: 'rgba(57, 255, 20, 0.3)',
            pointerEvents: 'none',
          }}
        />
        <Box
          sx={{
            position: 'absolute',
            top: 30,
            right: 30,
            width: 120,
            height: 120,
            borderRadius: '50%',
            background: 'rgba(57, 255, 20, 0.15)',
            pointerEvents: 'none',
          }}
        />
        <Box
          sx={{
            position: 'absolute',
            bottom: -50,
            left: -50,
            width: 160,
            height: 160,
            borderRadius: '50%',
            background: 'rgba(10, 22, 40, 0.5)',
            pointerEvents: 'none',
          }}
        />

        {/* Logo in white circle + title */}
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, mb: 1.5, zIndex: 1, position: 'relative' }}>
          <Box
            sx={{
              width: 48,
              height: 48,
              borderRadius: '50%',
              backgroundColor: 'white',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              boxShadow: '0 2px 12px rgba(0,0,0,0.2)',
              flexShrink: 0,
            }}
          >
            <Box
              component="img"
              src="/sharkfinlogo.png"
              alt="SharkFin Logo"
              sx={{ width: 32, height: 32, objectFit: 'contain' }}
            />
          </Box>
          <Typography sx={{ color: 'white', fontWeight: 700, fontSize: '1rem', letterSpacing: 1 }}>
            SHARKFIN
          </Typography>
        </Box>

        <Typography
          variant="h4"
          sx={{
            color: 'white',
            fontWeight: 800,
            whiteSpace: 'nowrap',
            textShadow: '0 2px 8px rgba(0,0,0,0.3)',
            zIndex: 1,
            position: 'relative',
          }}
        >
          Welcome Back!
        </Typography>
      </Box>

      {/* White dashboard body */}
      <Box
        sx={{
          flex: 1,
          backgroundColor: 'white',
          px: 3,
          pt: 4,
          pb: 3,
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
          justifyContent: 'space-between',
          overflow: 'hidden',
        }}
      >
        {/* Work in Progress watermark */}
        <Box
          sx={{
            flex: 1,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
          }}
        >
          <Typography
            sx={{
              fontSize: '1.6rem',
              fontWeight: 800,
              color: 'rgba(0,0,0,0.08)',
              letterSpacing: 3,
              textTransform: 'uppercase',
              textAlign: 'center',
              userSelect: 'none',
            }}
          >
            Work in Progress
          </Typography>
        </Box>

        {/* Logout button pinned to bottom center */}
        <Button
          onClick={handleLogout}
          variant="contained"
          sx={{
            px: 6,
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
          LOGOUT
        </Button>
      </Box>
    </Box>
  );
};

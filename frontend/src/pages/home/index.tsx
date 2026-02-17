import { Button } from '@mui/material';
import { signOut } from 'firebase/auth';
import { auth } from '../../lib/firebase';

export const HomePage = () => {
  const handleLogout = async () => {
    await signOut(auth);
  };

  return (
    <div>
      <h1>Welcome to your Dashboard</h1>
      <p>Your financial information will be displayed here.</p>
      <Button onClick={handleLogout} variant="contained">
        Logout
      </Button>
    </div>
  );
};

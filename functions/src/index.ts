import * as functions from 'firebase-functions';
import * as express from 'express';
import * as cors from 'cors';
import { createLinkToken, exchangePublicToken } from './plaid';
import * as admin from 'firebase-admin';

admin.initializeApp();

const app = express();
app.use(cors({ origin: true }));

const authenticate = async (req: express.Request, res: express.Response, next: express.NextFunction) => {
  if (!req.headers.authorization || !req.headers.authorization.startsWith('Bearer ')) {
    res.status(403).send('Unauthorized');
    return;
  }

  const idToken = req.headers.authorization.split('Bearer ')[1];
  try {
    const decodedIdToken = await admin.auth().verifyIdToken(idToken);
    req.user = decodedIdToken;
    next();
    return;
  } catch (e) {
    res.status(403).send('Unauthorized');
    return;
  }
};

app.post('/create_link_token', authenticate, async (req, res) => {
  try {
    const linkToken = await createLinkToken(req.user.uid);
    res.json({ link_token: linkToken });
  } catch (error) {
    console.error(error);
    res.status(500).send('Error creating link token');
  }
});

app.post('/exchange_public_token', authenticate, async (req, res) => {
  try {
    const { publicToken } = req.body;
    if (!publicToken) {
      res.status(400).send('Missing public token');
      return;
    }
    const { accessToken, itemId } = await exchangePublicToken(publicToken, req.user.uid);
    res.json({ accessToken, itemId });
  } catch (error) {
    console.error(error);
    res.status(500).send('Error exchanging public token');
  }
});

export const api = functions.https.onRequest(app);

declare global {
  namespace Express {
    export interface Request {
      user: admin.auth.DecodedIdToken;
    }
  }
}

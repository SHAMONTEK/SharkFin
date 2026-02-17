import { Configuration, PlaidApi, PlaidEnvironments } from 'plaid';
import * as functions from 'firebase-functions';
import * as admin from 'firebase-admin';

const configuration = new Configuration({
  basePath: PlaidEnvironments[functions.config().plaid.env],
  baseOptions: {
    headers: {
      'PLAID-CLIENT-ID': functions.config().plaid.client_id,
      'PLAID-SECRET': functions.config().plaid.secret,
    },
  },
});

const client = new PlaidApi(configuration);

export const createLinkToken = async (userId: string) => {
  const response = await client.linkTokenCreate({
    user: {
      client_user_id: userId,
    },
    client_name: 'My App',
    products: ['auth', 'transactions'],
    country_codes: ['US'],
    language: 'en',
  });

  return response.data.link_token;
};

export const exchangePublicToken = async (publicToken: string, userId: string) => {
  const response = await client.itemPublicTokenExchange({ public_token: publicToken });

  const accessToken = response.data.access_token;
  const itemId = response.data.item_id;

  await admin.firestore().collection('items').doc(itemId).set({
    userId,
    accessToken,
  });

  return {
    accessToken,
    itemId,
  };
};

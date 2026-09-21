import { GOOGLE_AUTH_URI } from './env';

export const OAuthConfig = {
    clientId: import.meta.env.VITE_GOOGLE_CLIENT_ID || "your-google-client-id",
    redirectUri: import.meta.env.VITE_GOOGLE_REDIRECT_URI || "http://localhost:5173/authenticate",
    authUri: GOOGLE_AUTH_URI,
};

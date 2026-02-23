import { initializeApp } from "firebase/app";
import { getAuth } from "firebase/auth";
import { getFirestore } from "firebase/firestore";
import { getFunctions } from "firebase/functions";

const firebaseConfig = {
  apiKey: "AIzaSyDfqW3OtI4gVtJGYmQcP9SZp1QjqrGv0cg",
  authDomain: "sharkfin-eba6f.firebaseapp.com",
  projectId: "sharkfin-eba6f",
  storageBucket: "sharkfin-eba6f.firebasestorage.app",
  messagingSenderId: "855823693206",
  appId: "1:855823693206:web:fdfa23d2cba4529c50d9b7",
  measurementId: "G-QTVKYWDHCL",
};

// Initialize Firebase
const app = initializeApp(firebaseConfig);

// Export Firebase services
export const auth = getAuth(app);
export const db = getFirestore(app);
export const functions = getFunctions(app);

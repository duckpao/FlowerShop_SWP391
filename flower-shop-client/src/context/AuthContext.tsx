import React, { createContext, useContext } from 'react';

type AuthContextType = {
  user: any;
  busy: boolean;
  error: string | null;
  logout: () => void;
};

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ value: AuthContextType; children: React.ReactNode }> = ({ value, children }) => {
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};

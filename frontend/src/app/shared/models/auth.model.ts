export interface LoginRequest {
  email: string;
  password: string;
}

export interface LoginResponse {
  token: string;
  tokenType: string;
  subject: string;
  userId: number;
  userName: string;
  role: 'USUARIO' | 'TECNICO' | 'ADMIN';
  expiresAt: string;
}

export type UserRole = 'USUARIO' | 'TECNICO' | 'ADMIN';

export interface Usuario {
  id: number;
  nombre: string;
  email: string;
  rol: UserRole;
}

import { Injectable, signal } from '@angular/core';

export interface UsuarioAtual {
  id: string;
  nome: string;
  email: string;
  perfil: 'COMPRADOR' | 'FORNECEDOR' | 'ADMIN';
  ativo: boolean;
  criadoEm: string;
}
@Injectable({ providedIn: 'root' })
export class SessaoStore {
  readonly usuario = signal<UsuarioAtual | null>(null);
  readonly estado = signal<'carregando' | 'pronta' | 'erro'>('carregando');
  readonly aviso = signal('');
  versao = 0;
  resolver(usuario: UsuarioAtual | null): void {
    this.versao++;
    this.usuario.set(usuario);
    this.estado.set('pronta');
  }
  expirar(): void {
    if (this.usuario())
      this.aviso.set('Sua sessão expirou. Entre novamente para acessar sua conta.');
    this.resolver(null);
  }
}

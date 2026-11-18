import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { catchError, finalize, map, Observable, of, shareReplay, tap, throwError } from 'rxjs';
import { SessaoStore, UsuarioAtual } from './sessao.store';

@Injectable({ providedIn: 'root' })
export class AutenticacaoService {
  private readonly http = inject(HttpClient);
  readonly sessao = inject(SessaoStore);
  private consulta?: Observable<UsuarioAtual | null>;

  carregar(): Observable<UsuarioAtual | null> {
    if (this.sessao.estado() === 'pronta') return of(this.sessao.usuario());
    if (this.consulta) return this.consulta;
    const versao = this.sessao.versao;
    this.sessao.estado.set('carregando');
    this.consulta = this.http.get<UsuarioAtual>('/api/auth/eu').pipe(
      map((usuario) => {
        if (this.sessao.versao === versao) this.sessao.resolver(usuario);
        return this.sessao.usuario();
      }),
      catchError((erro: HttpErrorResponse) => {
        if (erro.status === 401) {
          if (this.sessao.versao === versao) this.sessao.resolver(null);
          return of(this.sessao.usuario());
        }
        if (this.sessao.versao === versao) this.sessao.estado.set('erro');
        return throwError(() => erro);
      }),
      finalize(() => (this.consulta = undefined)),
      shareReplay({ bufferSize: 1, refCount: false }),
    );
    return this.consulta;
  }
  entrar(email: string, senha: string): Observable<UsuarioAtual> {
    return this.http.post<UsuarioAtual>('/api/auth/login', { email: email.trim(), senha }).pipe(
      tap((usuario) => {
        this.sessao.resolver(usuario);
        this.sessao.aviso.set('');
      }),
    );
  }
  cadastrar(nome: string, email: string, senha: string): Observable<UsuarioAtual> {
    return this.http.post<UsuarioAtual>('/api/auth/cadastro', {
      nome: nome.trim(),
      email: email.trim(),
      senha,
    });
  }
  sair(): Observable<void> {
    return this.http.post<void>('/api/auth/logout', {}).pipe(
      tap(() => {
        this.sessao.resolver(null);
        this.sessao.aviso.set('');
      }),
    );
  }
}

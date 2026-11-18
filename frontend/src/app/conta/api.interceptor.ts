import { inject } from '@angular/core';
import {
  HttpClient,
  HttpErrorResponse,
  HttpInterceptorFn,
  HttpRequest,
} from '@angular/common/http';
import { catchError, switchMap, throwError } from 'rxjs';
import { SessaoStore } from './sessao.store';

export const apiInterceptor: HttpInterceptorFn = (request, next) => {
  if (!request.url.startsWith('/api/')) return next(request);
  const sessao = inject(SessaoStore);
  const http = inject(HttpClient);
  const versao = sessao.versao;
  const enviar = (req: HttpRequest<unknown>) =>
    next(req).pipe(
      catchError((erro: HttpErrorResponse) => {
        if (erro.status === 401 && request.url !== '/api/auth/login' && sessao.versao === versao)
          sessao.expirar();
        return throwError(() => erro);
      }),
    );
  if (['GET', 'HEAD', 'OPTIONS'].includes(request.method)) return enviar(request);
  // Um token novo antes de cada alteração acompanha a rotação de sessão no login/logout.
  return http
    .get<{ cabecalho: string; token: string }>('/api/auth/csrf')
    .pipe(
      switchMap((csrf) => enviar(request.clone({ setHeaders: { [csrf.cabecalho]: csrf.token } }))),
    );
};

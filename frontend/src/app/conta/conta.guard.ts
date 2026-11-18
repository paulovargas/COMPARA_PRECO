import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { catchError, map, of } from 'rxjs';
import { AutenticacaoService } from './autenticacao.service';

export function destinoInterno(destino: string | null): string {
  return destino &&
    destino.startsWith('/') &&
    !destino.startsWith('//') &&
    !destino.includes('\\') &&
    !/^\/(entrar|cadastro)([/?#]|$)/.test(destino)
    ? destino
    : '/';
}
export const contaGuard: CanActivateFn = (_route, state) => {
  const auth = inject(AutenticacaoService);
  const router = inject(Router);
  return auth.carregar().pipe(
    map((usuario) =>
      usuario ? true : router.createUrlTree(['/entrar'], { queryParams: { retorno: state.url } }),
    ),
    catchError(() =>
      of(router.createUrlTree(['/entrar'], { queryParams: { retorno: state.url } })),
    ),
  );
};

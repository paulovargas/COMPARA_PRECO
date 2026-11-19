import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { catchError, map, of } from 'rxjs';
import { AutenticacaoService } from '../conta/autenticacao.service';
export const adminGuard: CanActivateFn = (_route, state) => {
  const auth = inject(AutenticacaoService);
  const router = inject(Router);
  return auth.carregar().pipe(
    map((usuario) =>
      usuario?.perfil === 'ADMIN'
        ? true
        : usuario
          ? router.createUrlTree(['/'])
          : router.createUrlTree(['/entrar'], { queryParams: { retorno: state.url } }),
    ),
    catchError(() =>
      of(router.createUrlTree(['/entrar'], { queryParams: { retorno: state.url } })),
    ),
  );
};

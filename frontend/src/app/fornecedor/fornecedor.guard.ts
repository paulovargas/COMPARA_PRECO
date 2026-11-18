import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { catchError, map, of } from 'rxjs';
import { AutenticacaoService } from '../conta/autenticacao.service';

export const fornecedorGuard: CanActivateFn = (route, state) => {
  const auth = inject(AutenticacaoService);
  const router = inject(Router);
  return auth.carregar().pipe(
    map((usuario) => {
      if (!usuario)
        return router.createUrlTree(['/entrar'], { queryParams: { retorno: state.url } });
      if (usuario.perfil === 'ADMIN') return router.createUrlTree(['/']);
      if (route.data['exigeFornecedor'] && usuario.perfil !== 'FORNECEDOR')
        return router.createUrlTree(['/fornecedor']);
      return true;
    }),
    catchError(() =>
      of(router.createUrlTree(['/entrar'], { queryParams: { retorno: state.url } })),
    ),
  );
};

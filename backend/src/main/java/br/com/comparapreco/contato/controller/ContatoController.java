package br.com.comparapreco.contato.controller;

import java.util.UUID;
import br.com.comparapreco.contato.dto.ContatoResponse;
import br.com.comparapreco.contato.service.ContatoService;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ContatoController {
    private final ContatoService service;
    public ContatoController(ContatoService service) { this.service = service; }
    @GetMapping("/api/catalogo/ofertas/{id}/contato")
    public ResponseEntity<ContatoResponse> contato(@PathVariable UUID id) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.consultar(id));
    }
}

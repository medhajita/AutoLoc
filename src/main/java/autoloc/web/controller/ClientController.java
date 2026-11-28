package autoloc.web.controller;

import autoloc.domain.Client;
import autoloc.repository.ClientRepository;
import autoloc.web.dto.ClientDto;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/clients")
@Transactional
public class ClientController {

    private final ClientRepository clients;

    public ClientController(ClientRepository clients) {
        this.clients = clients;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public List<ClientDto> list() {
        return clients.findAll().stream().map(ClientController::toDto).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ClientDto create(@Valid @RequestBody ClientDto dto) {
        Client c = apply(new Client(), dto);
        c.setDateInscription(dto.dateInscription() != null ? dto.dateInscription() : LocalDate.now());
        return toDto(clients.save(c));
    }

    @PutMapping("/{id}")
    public ClientDto update(@PathVariable Long id, @Valid @RequestBody ClientDto dto) {
        Client c = clients.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Client introuvable"));
        return toDto(clients.save(apply(c, dto)));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        if (!clients.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Client introuvable");
        }
        clients.deleteById(id);
        clients.flush();
    }

    private Client apply(Client c, ClientDto dto) {
        c.setNom(dto.nom());
        c.setPrenom(dto.prenom());
        c.setEmail(dto.email());
        c.setTelephone(dto.telephone());
        c.setNumPermis(dto.numPermis());
        return c;
    }

    static ClientDto toDto(Client c) {
        return new ClientDto(c.getIdClient(), c.getNom(), c.getPrenom(), c.getEmail(),
                c.getTelephone(), c.getNumPermis(), c.getDateInscription());
    }
}

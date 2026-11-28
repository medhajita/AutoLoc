package autoloc.web.controller;

import autoloc.domain.Agence;
import autoloc.repository.AgenceRepository;
import autoloc.web.dto.AgenceDto;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/agences")
@Transactional
public class AgenceController {

    private final AgenceRepository agences;

    public AgenceController(AgenceRepository agences) {
        this.agences = agences;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public List<AgenceDto> list() {
        return agences.findAll().stream().map(AgenceController::toDto).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AgenceDto create(@Valid @RequestBody AgenceDto dto) {
        return toDto(agences.save(apply(new Agence(), dto)));
    }

    @PutMapping("/{id}")
    public AgenceDto update(@PathVariable Long id, @Valid @RequestBody AgenceDto dto) {
        Agence a = agences.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Agence introuvable"));
        return toDto(agences.save(apply(a, dto)));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        if (!agences.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Agence introuvable");
        }
        agences.deleteById(id);
        agences.flush();
    }

    private Agence apply(Agence a, AgenceDto dto) {
        a.setNom(dto.nom());
        a.setVille(dto.ville());
        a.setAdresse(dto.adresse());
        a.setTelephone(dto.telephone());
        return a;
    }

    private static AgenceDto toDto(Agence a) {
        return new AgenceDto(a.getIdAgence(), a.getNom(), a.getVille(), a.getAdresse(), a.getTelephone());
    }
}

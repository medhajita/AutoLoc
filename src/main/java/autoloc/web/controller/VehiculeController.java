package autoloc.web.controller;

import autoloc.domain.Agence;
import autoloc.domain.Vehicule;
import autoloc.repository.AgenceRepository;
import autoloc.repository.VehiculeRepository;
import autoloc.web.dto.VehiculeDto;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/vehicules")
@Transactional
public class VehiculeController {

    private final VehiculeRepository vehicules;
    private final AgenceRepository agences;

    public VehiculeController(VehiculeRepository vehicules, AgenceRepository agences) {
        this.vehicules = vehicules;
        this.agences = agences;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public List<VehiculeDto> list() {
        return vehicules.findAll().stream().map(VehiculeController::toDto).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public VehiculeDto create(@Valid @RequestBody VehiculeDto dto) {
        return toDto(vehicules.save(apply(new Vehicule(), dto)));
    }

    @PutMapping("/{id}")
    public VehiculeDto update(@PathVariable Long id, @Valid @RequestBody VehiculeDto dto) {
        Vehicule v = vehicules.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Véhicule introuvable"));
        return toDto(vehicules.save(apply(v, dto)));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        if (!vehicules.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Véhicule introuvable");
        }
        vehicules.deleteById(id);
        vehicules.flush();
    }

    private Vehicule apply(Vehicule v, VehiculeDto dto) {
        v.setImmatriculation(dto.immatriculation());
        v.setMarque(dto.marque());
        v.setModele(dto.modele());
        v.setCategorie(dto.categorie());
        v.setTarifJournalier(dto.tarifJournalier());
        v.setStatut(dto.statut());
        Agence agence = dto.agenceId() == null ? null : agences.findById(dto.agenceId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Agence introuvable"));
        v.setAgence(agence);
        return v;
    }

    static VehiculeDto toDto(Vehicule v) {
        Agence a = v.getAgence();
        return new VehiculeDto(v.getIdVehicule(), v.getImmatriculation(), v.getMarque(), v.getModele(),
                v.getCategorie(), v.getTarifJournalier(), v.getStatut(),
                a == null ? null : a.getIdAgence(), a == null ? null : a.getNom());
    }
}

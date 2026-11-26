package autoloc;

import autoloc.domain.CategorieVehicule;
import autoloc.domain.StatutVehicule;
import autoloc.domain.Vehicule;
import autoloc.repository.VehiculeRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    private final VehiculeRepository vehiculeRepository;

    public DataInitializer(VehiculeRepository vehiculeRepository) {
        this.vehiculeRepository = vehiculeRepository;
    }

    @Override
    public void run(String... args) {
        if (vehiculeRepository.count() > 0) {
            return;
        }

        vehiculeRepository.saveAll(List.of(
            new Vehicule(null, "AB-123-CD", "Renault", "Clio", CategorieVehicule.CITADINE, new BigDecimal("45.00"), StatutVehicule.DISPONIBLE, null, new ArrayList<>(), new ArrayList<>(), new ArrayList<>()),
            new Vehicule(null, "EF-456-GH", "Peugeot", "508",  CategorieVehicule.BERLINE,  new BigDecimal("75.00"), StatutVehicule.DISPONIBLE, null, new ArrayList<>(), new ArrayList<>(), new ArrayList<>()),
            new Vehicule(null, "IJ-789-KL", "Toyota",  "RAV4", CategorieVehicule.SUV,      new BigDecimal("95.00"), StatutVehicule.MAINTENANCE, null, new ArrayList<>(), new ArrayList<>(), new ArrayList<>())
        ));
    }
}

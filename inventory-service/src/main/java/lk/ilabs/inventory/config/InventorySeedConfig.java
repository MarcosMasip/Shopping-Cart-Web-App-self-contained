package lk.ilabs.inventory.config;

import lk.ilabs.inventory.entity.Item;
import lk.ilabs.inventory.repository.ItemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Configuration
public class InventorySeedConfig implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(InventorySeedConfig.class);
    private final ItemRepository repo;

    public InventorySeedConfig(ItemRepository repo) {
        this.repo = repo;
    }

    record Seed(int code, String description, int qty, double price) {}

    @Override
    @Transactional
    public void run(String... args) {
        log.info("[InventorySeed] Starting inventory seeding check");
        List<Seed> seeds = List.of(
                new Seed(1,"Wireless Mouse",50,1999.00),
                new Seed(2,"Mechanical Keyboard",35,8999.00),
                new Seed(3,"USB-C Hub",80,4599.00),
                new Seed(4,"Noise Cancelling Headphones",25,12999.00),
                new Seed(5,"4K Monitor",15,25999.00),
                new Seed(6,"Webcam HD",60,4999.00),
                new Seed(7,"Portable SSD 1TB",40,10999.00),
                new Seed(8,"Laptop Stand",70,2999.00)
        );
        for (Seed s : seeds) {
            repo.findById(s.code()).ifPresentOrElse(
                    existing -> log.info("[InventorySeed] EXISTS code={} desc={}", existing.getCode(), existing.getDescription()),
                    () -> {
                        Item item = new Item();
                        item.setCode(s.code());
                        item.setDescription(s.description());
                        item.setQty(s.qty());
                        item.setPrice(s.price());
                        item.setCreatedAt(Instant.now());
                        repo.save(item);
                        log.info("[InventorySeed] CREATED code={} desc={}", s.code(), s.description());
                    }
            );
        }
        log.info("[InventorySeed] Completed inventory seeding check");
    }
}

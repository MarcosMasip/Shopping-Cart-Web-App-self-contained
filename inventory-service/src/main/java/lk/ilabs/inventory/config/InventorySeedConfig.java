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
import java.math.BigDecimal;

@Configuration
public class InventorySeedConfig implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(InventorySeedConfig.class);
    private final ItemRepository repo;

    public InventorySeedConfig(ItemRepository repo) {
        this.repo = repo;
    }

    record Seed(int code, String description, int qty, BigDecimal price) {}

    @Override
    @Transactional
    public void run(String... args) {
        log.info("[InventorySeed] Starting inventory seeding check");
    List<Seed> seeds = List.of(
        new Seed(1,"Wireless Mouse",50,new BigDecimal("1999.00")),
        new Seed(2,"Mechanical Keyboard",35,new BigDecimal("8999.00")),
        new Seed(3,"USB-C Hub",80,new BigDecimal("4599.00")),
        new Seed(4,"Noise Cancelling Headphones",25,new BigDecimal("12999.00")),
        new Seed(5,"4K Monitor",15,new BigDecimal("25999.00")),
        new Seed(6,"Webcam HD",60,new BigDecimal("4999.00")),
        new Seed(7,"Portable SSD 1TB",40,new BigDecimal("10999.00")),
        new Seed(8,"Laptop Stand",70,new BigDecimal("2999.00"))
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

package lk.ilabs.inventory.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class Item implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer code;
    private String description;
    private int qty;
    @Column(precision = 15, scale = 2)
    private BigDecimal price;
    private Instant createdAt;

    public Item(String description, int qty, BigDecimal price) {
        this.description = description;
        this.qty = qty;
        this.price = price;
        this.createdAt = Instant.now();
    }
}

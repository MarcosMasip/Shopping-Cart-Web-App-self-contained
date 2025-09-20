package lk.ilabs.assignment.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CartLineDTO implements Serializable {
    private UUID id;
    private Integer itemCode;
    private String description;
    private int qty;
    private BigDecimal unitPrice;
    private BigDecimal lineTotal;
}

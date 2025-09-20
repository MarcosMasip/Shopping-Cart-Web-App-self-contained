package lk.ilabs.assignment.service;

import lk.ilabs.assignment.dto.CartItemDTO;
import lk.ilabs.assignment.dto.CartLineDTO;
import lk.ilabs.assignment.dto.CartSummaryDTO;
import java.util.List;

public interface CartService {
    void addItemToCard(CartItemDTO cartItem);
    void removeItemFromCard(Integer itemCode, String username);
    List<CartLineDTO> listCart(String username);
    CartSummaryDTO summary(String username);
}

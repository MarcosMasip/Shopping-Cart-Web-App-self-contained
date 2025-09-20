package lk.ilabs.assignment.service;

import lk.ilabs.assignment.dto.CartItemDTO;
import lk.ilabs.assignment.dto.CartSummaryDTO;
import lk.ilabs.assignment.entity.CartItem;
import lk.ilabs.assignment.repository.CartItemRepository;
import lk.ilabs.assignment.service.impl.CartServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class CartServiceImplTest {

    private CartItemRepository cartItemRepository;
    private RestTemplate restTemplate;
    private CartService cartService;

    @BeforeEach
    void setup(){
        cartItemRepository = mock(CartItemRepository.class);
        restTemplate = mock(RestTemplate.class);
        cartService = new CartServiceImpl(cartItemRepository, restTemplate);
    }

    @Test
    void addItemThenSummary() {
        lk.ilabs.assignment.dto.ItemDTO itemDTO = new lk.ilabs.assignment.dto.ItemDTO();
        itemDTO.setCode(1); itemDTO.setDescription("Widget"); itemDTO.setQty(100); itemDTO.setPrice(BigDecimal.valueOf(5.50));
        when(restTemplate.getForObject(anyString(), eq(lk.ilabs.assignment.dto.ItemDTO.class), ArgumentMatchers.any())).thenReturn(itemDTO);
        when(cartItemRepository.findCartItemByItemCodeAndUsername(1, "alice")).thenReturn(Optional.empty());
        when(cartItemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        cartService.addItemToCard(new CartItemDTO("alice", 1, 3));

        CartItem ci = new CartItem();
        ci.setItemCode(1); ci.setQty(3); ci.setPriceSnapshot(BigDecimal.valueOf(5.50)); ci.setDescriptionSnapshot("Widget"); ci.setUsername("alice");
        when(cartItemRepository.findAll()).thenReturn(java.util.List.of(ci));

        CartSummaryDTO summary = cartService.summary("alice");
        assertThat(summary.getLines()).hasSize(1);
        assertThat(summary.getTotal()).isEqualByComparingTo("16.50");
    }
}
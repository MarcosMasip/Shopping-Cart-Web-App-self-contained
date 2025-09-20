package lk.ilabs.assignment.service.impl;

import lk.ilabs.assignment.dto.CartItemDTO;
import lk.ilabs.assignment.dto.CartLineDTO;
import lk.ilabs.assignment.dto.CartSummaryDTO;
import lk.ilabs.assignment.dto.ItemDTO;
import lk.ilabs.assignment.entity.CartItem;
import lk.ilabs.assignment.repository.CartItemRepository;
import lk.ilabs.assignment.service.CartService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.StreamSupport;

@Service
public class CartServiceImpl implements CartService {

    private CartItemRepository cartItemRepository;
    private RestTemplate restTemplate;

    public CartServiceImpl(CartItemRepository cartItemRepository, RestTemplate restTemplate) {
        this.cartItemRepository = cartItemRepository;
        this.restTemplate = restTemplate;
    }

    @Override
    public void addItemToCard(CartItemDTO cartItem) {
    // Direct call (service discovery removed): inventory-service default port 8081
    ItemDTO itemInStock = restTemplate.getForObject("http://localhost:8081/api/v1/items/{code}", ItemDTO.class, cartItem.getItemCode());
        if (itemInStock == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Item not found");
        Optional<CartItem> optCartItem = cartItemRepository.findCartItemByItemCodeAndUsername(cartItem.getItemCode(), cartItem.getUsername());

        if (optCartItem.isEmpty()){
            if (itemInStock.getQty() < cartItem.getQty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Insufficient stock!");
            cartItemRepository.save(new CartItem(cartItem.getUsername(), cartItem.getItemCode(), cartItem.getQty(), itemInStock.getPrice(), itemInStock.getDescription()));
        }else{
            CartItem cartItemEntity = optCartItem.get();
            if (itemInStock.getQty() < (cartItem.getQty() + cartItemEntity.getQty()))throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Insufficient stock!");
            cartItemEntity.setQty(cartItemEntity.getQty() + cartItem.getQty());
            cartItemRepository.save(cartItemEntity);
        }

    }

    @Override
    public void removeItemFromCard(Integer itemCode, String username) {
        CartItem cartItem = cartItemRepository.findCartItemByItemCodeAndUsername(itemCode, username).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cart item doesn't exist"));
        cartItemRepository.delete(cartItem);
    }

    @Override
    public List<CartLineDTO> listCart(String username) {
        Iterable<CartItem> all = cartItemRepository.findAll();
        return StreamSupport.stream(all.spliterator(), false)
                .filter(ci -> ci.getUsername().equals(username))
                .map(this::toLine)
                .toList();
    }

    @Override
    public CartSummaryDTO summary(String username) {
        List<CartLineDTO> lines = listCart(username);
        BigDecimal total = lines.stream().map(CartLineDTO::getLineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new CartSummaryDTO(lines, total);
    }

    private CartLineDTO toLine(CartItem entity){
        BigDecimal unitPrice = entity.getPriceSnapshot();
        BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(entity.getQty()));
        return new CartLineDTO(entity.getId(), entity.getItemCode(), entity.getDescriptionSnapshot(), entity.getQty(), unitPrice, lineTotal);
    }
}

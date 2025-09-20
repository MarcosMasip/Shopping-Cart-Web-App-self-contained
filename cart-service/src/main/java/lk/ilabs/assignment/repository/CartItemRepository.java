package lk.ilabs.assignment.repository;

import lk.ilabs.assignment.entity.CartItem;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for CartItem entities.
 * Primary key is a UUID, so use CrudRepository<CartItem, UUID>.
 */
public interface CartItemRepository extends CrudRepository<CartItem, UUID> {
    Optional<CartItem> findCartItemByItemCodeAndUsername(Integer code, String username);
}

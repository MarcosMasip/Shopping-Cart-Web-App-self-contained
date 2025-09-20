package lk.ilabs.inventory.repository;

import lk.ilabs.inventory.entity.Item;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class ItemRepositoryTest {

    @Autowired
    private ItemRepository itemRepository;

    @Test
    void saveAndFindItem() {
        Item item = new Item("Test Widget", 5, BigDecimal.valueOf(9.99));
        Item saved = itemRepository.save(item);
        assertThat(saved.getCode()).isNotNull();
        assertThat(itemRepository.findById(saved.getCode())).isPresent();
    }
}
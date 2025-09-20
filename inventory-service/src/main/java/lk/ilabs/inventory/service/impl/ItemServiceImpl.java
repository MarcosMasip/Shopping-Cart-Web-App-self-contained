package lk.ilabs.inventory.service.impl;

import lk.ilabs.inventory.dto.ItemDTO;
import lk.ilabs.inventory.entity.Item;
import lk.ilabs.inventory.repository.ItemRepository;
import lk.ilabs.inventory.service.ItemService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ItemServiceImpl implements ItemService {

    private ItemRepository itemRepository;

    public ItemServiceImpl(ItemRepository itemRepository) {
        this.itemRepository = itemRepository;
    }

    @Override
    public ItemDTO addNewItem(ItemDTO item) {
        if (item.getPrice() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Price is required");
        }
        Item entity = this.itemRepository.save(new Item(item.getDescription(), item.getQty(), item.getPrice()));
        return new ItemDTO(entity.getCode(), entity.getDescription(), entity.getQty(), entity.getPrice(), entity.getCreatedAt());
    }

    @Override
    public ItemDTO getItem(Integer code) {
        return this.itemRepository.findById(code).map(entity -> new ItemDTO(entity.getCode(), entity.getDescription(), entity.getQty(), entity.getPrice(), entity.getCreatedAt()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @Override
    public Iterable<ItemDTO> listAll() {
        Iterable<Item> all = itemRepository.findAll();
        java.util.List<ItemDTO> list = new java.util.ArrayList<>();
        for (Item entity : all) {
            list.add(new ItemDTO(entity.getCode(), entity.getDescription(), entity.getQty(), entity.getPrice(), entity.getCreatedAt()));
        }
        return list;
    }
}

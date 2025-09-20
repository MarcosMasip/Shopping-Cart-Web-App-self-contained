package lk.ilabs.inventory.service;

import lk.ilabs.inventory.dto.ItemDTO;

public interface ItemService {
    ItemDTO addNewItem(ItemDTO item);
    ItemDTO getItem(Integer code);
    Iterable<ItemDTO> listAll();
}

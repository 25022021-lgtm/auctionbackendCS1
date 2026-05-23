package com.auction.itemstatus;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.auction.items.Item;

import jakarta.persistence.LockModeType;

public interface ItemStatusRepository extends JpaRepository<ItemStatus, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(value = "SELECT s FROM ItemStatus s WHERE s.item = :item")
    ItemStatus findByItemWithLock(@Param("item") Item item);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(value = "SELECT s FROM ItemStatus s WHERE s.item.itemId = :itemId")
    ItemStatus findByItemWithLockByItemId(@Param("itemId") Long itemId);

}

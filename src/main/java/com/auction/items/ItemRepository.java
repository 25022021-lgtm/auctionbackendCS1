package com.auction.items;

import com.auction.users.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** Repository for managing {@link Item} entities and custom item queries. */
@Repository
public interface ItemRepository extends JpaRepository<Item, Long> {

  // Get all items that are active
  @Query(
      value =
          "SELECT itemstat.item FROM ItemStatus itemstat"
              + " WHERE itemstat.endTime > :currentTime")
  Page<Item> findActiveItemPage(Pageable pageable, @Param("currentTime") Long endtime);

  // Getting the listing of a user
  Page<Item> findItemByUser(Pageable pageable, User user);
}

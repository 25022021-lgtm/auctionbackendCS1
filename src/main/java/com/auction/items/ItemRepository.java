package com.auction.items;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.auction.users.User;

@Repository
public interface ItemRepository extends JpaRepository<Item, Long> {

    // get all bids that is active
    @Query(value = "SELECT itemstat.item FROM ItemStatus itemstat WHERE itemstat.endTime > :currentTime")
    public Page<Item> findActiveItemPage(Pageable pageable, @Param("currentTime") Long endtime);

    // getting the listing of a user
    public Page<Item> findItemByUser(Pageable pageable, User user);

}

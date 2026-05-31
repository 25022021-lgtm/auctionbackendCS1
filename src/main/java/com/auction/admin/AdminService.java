package com.auction.admin;

import org.springframework.stereotype.Service;

import com.auction.common.BaseResponse;
import com.auction.items.ItemService;
import com.auction.users.User;
import com.auction.users.UserService;

@Service
public class AdminService {
    private ItemService itemService;
    private UserService userService;
    public AdminService(ItemService itemService, UserService userService) {
        this.itemService = itemService;
        this.userService = userService;
    }

    public BaseResponse cancelAuction(Long itemId) {
        String sellername = itemService.getItem(itemId).getUser().getUsername();
        return itemService.cancelItem(itemId, sellername);
    }

    public BaseResponse banUser(String username) {
        User user = userService.getUserByUsername(username);
        user.setDisplayName("Banned User");
        user.setHashedPassword("1");
        userService.saveUser(user);
        return new BaseResponse(true, "successfully banned user");
    }
}


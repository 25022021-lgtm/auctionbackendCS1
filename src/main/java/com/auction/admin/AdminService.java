package com.auction.admin;

import com.auction.admin.dto.*;
import com.auction.auth.AuthService;
import com.auction.common.BaseResponse;
import com.auction.items.ItemService;
import com.auction.users.User;
import com.auction.users.UserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AdminService {

    private final ItemService itemService;
    private final UserService userService;
    private final AuthService authService;
    private final PasswordEncoder passwordEncoder;

    public AdminService(
        ItemService itemService,
        UserService userService,
        AuthService authService,
        PasswordEncoder passwordEncoder
    ) {
        this.itemService = itemService;
        this.userService = userService;
        this.authService = authService;
        this.passwordEncoder = passwordEncoder;
    }

    public BaseResponse cancelAuction(Long itemId) {
        String sellername = itemService.getItem(itemId).getUser().getUsername();
        return itemService.cancelItem(itemId, sellername);
    }

    public BaseResponse banUser(String username) {
        User user = userService.getUserByUsername(username);
        user.setHashedPassword("1");
        authService.revokeToken(username);
        userService.saveUser(user);
        return new BaseResponse(true, "successfully banned user");
    }

    public BaseResponse unbanUser(UnbanRequest request) {
        User user = userService.getUserByUsername(request.username());
        String hashedPassword = passwordEncoder.encode(request.password());
        user.setHashedPassword(hashedPassword);
        userService.saveUser(user);
        return new BaseResponse(true, "Succesfully unbanned user.");
    }
}

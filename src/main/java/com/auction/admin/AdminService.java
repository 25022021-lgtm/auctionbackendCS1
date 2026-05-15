    package com.auction.admin;

    import java.time.Instant;

    import com.auction.admin.dto.*;
    import com.auction.auth.AuthService;
    import com.auction.auth.RevokedToken;
    import com.auction.auth.RevokedTokenRepository;
    import com.auction.common.*;
    import com.auction.items.ItemService;
    import com.auction.users.User;
    import com.auction.users.UserService;

    import org.springframework.beans.factory.annotation.Value;
    import org.springframework.security.crypto.password.PasswordEncoder;
    import org.springframework.stereotype.Service;

    @Service
    public class AdminService {

        private final ItemService itemService;
        private final UserService userService;
        private final AuthService authService;
        private final PasswordEncoder passwordEncoder;
        private final RevokedTokenRepository revokedTokenRepository;

        @Value("${ban_hash}")
        private String banHash;

        public AdminService(
                ItemService itemService,
                UserService userService,
                AuthService authService,
                PasswordEncoder passwordEncoder,
                RevokedTokenRepository revokedTokenRepository
        ) {
            this.itemService = itemService;
            this.userService = userService;
            this.authService = authService;
            this.passwordEncoder = passwordEncoder;
            this.revokedTokenRepository = revokedTokenRepository;
        }

        public BaseResponse cancelAuction(Long itemId) {
            String sellername = itemService.getItem(itemId).getUser().getUsername();
            return itemService.cancelItem(itemId, sellername);
        }

        public BaseResponse banUser(String username) {
            if (username.equals("admin")) {
                throw new BaseException("You can't ban admin");
            }
            User user = userService.getUserByUsername(username);
            authService.revokeToken(username);
            user.setHashedPassword(banHash);
            authService.revokeToken(username);
            userService.saveUser(user);

            revokedTokenRepository.save(
                    new RevokedToken(username, Instant.now().toEpochMilli())
            );

            return new BaseResponse(true, "successfully banned user");
        }

        public BaseResponse unbanUser(UnbanRequest request) {
            User user = userService.getUserByUsername(request.username());
            String hashedPassword = passwordEncoder.encode(request.password());
            user.setHashedPassword(hashedPassword);
            userService.saveUser(user);

            revokedTokenRepository.deleteById(request.username());

            return new BaseResponse(true, "Succesfully unbanned user.");
        }
    }

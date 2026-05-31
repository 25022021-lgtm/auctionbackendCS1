package com.auction.admin;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.auction.admin.dto.BanUserDto;
import com.auction.auth.jwtools.UserDetailsImpl;
import com.auction.common.BaseResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/admin")
public class AdminController {
    private final AdminService adminService;
    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @PostMapping("/ban")
    public ResponseEntity<BaseResponse> ban(
            @Valid @RequestBody BanUserDto banDetails,
            @AuthenticationPrincipal UserDetailsImpl userDetailsImpl) {
        BaseResponse response = adminService.banUser(banDetails.username());
        return ResponseEntity.ok(response);
    }
    @PostMapping("/cancel/{itemId}")
    public ResponseEntity<BaseResponse> cancelItem(@PathVariable Long itemId) {
        BaseResponse response = adminService.cancelAuction(itemId);
        return ResponseEntity.ok(response);
    }
}

package com.auction.admin;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.auction.admin.dto.BanUserDto;
import com.auction.auth.jwtools.UserDetailsImpl;
import com.auction.common.BaseResponse;



@RestController
@RequestMapping("/admin")
public class AdminController {
    private AdminService adminService;
    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }
    
    @PostMapping("/ban")
    public ResponseEntity<BaseResponse> ban(@RequestBody BanUserDto banDetails, UserDetailsImpl userDetailsImpl) {
        adminService.banUser()
    }
}

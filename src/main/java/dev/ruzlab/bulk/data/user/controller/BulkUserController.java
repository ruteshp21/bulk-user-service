package dev.ruzlab.bulk.data.user.controller;

import dev.ruzlab.bulk.data.user.dao.UserDTO;
import dev.ruzlab.bulk.data.user.service.BulkUserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class BulkUserController {

    private final BulkUserService bulkUserService;

    public BulkUserController(BulkUserService bulkUserService) {
        this.bulkUserService = bulkUserService;
    }

    @PostMapping("/users")
    public ResponseEntity<?> createUsersParallel(@RequestBody List<UserDTO> userList) {
        bulkUserService.processUsersInBulk(userList);
        return ResponseEntity.ok().body(null);
    }

    @DeleteMapping("/users")
    public ResponseEntity<?> deleteAllUsers() {
        bulkUserService.deleteAll();
        return ResponseEntity.ok().build();
    }
}

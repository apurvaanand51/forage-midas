package com.jpmc.midascore.controller;

import com.jpmc.midascore.foundation.Balance;
import com.jpmc.midascore.entity.UserRecord;
import com.jpmc.midascore.repository.UserRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BalanceController {

    private final UserRepository userRepository;

    public BalanceController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping(path = "/balance")
    public Balance getBalance(@RequestParam(name = "userId") long userId) {
        UserRecord user = userRepository.findById(userId);
        float amount = 0.0f;
        if (user != null) {
            amount = user.getBalance();
        }
        return new Balance(amount);
    }
}

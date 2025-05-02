package com.example.investmentplatform.service;

import org.springframework.stereotype.Service;

import com.example.investmentplatform.entity.User;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NotificationService {

    public void sendAlert(User user, String title, String body) {
        System.out.println("To       : " + user.getEmail());
        System.out.println("Title    : " + title);
        System.out.println("Message  : " + body);
    }

}

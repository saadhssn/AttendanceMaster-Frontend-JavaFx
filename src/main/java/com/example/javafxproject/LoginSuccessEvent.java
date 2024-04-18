package com.example.javafxproject;// LoginSuccessEvent.java
import javafx.event.Event;
import javafx.event.EventType;

public class LoginSuccessEvent extends Event {

    public static final EventType<LoginSuccessEvent> LOGIN_SUCCESS =
            new EventType<>(Event.ANY, "LOGIN_SUCCESS");

    public LoginSuccessEvent() {
        super(LOGIN_SUCCESS);
    }
}

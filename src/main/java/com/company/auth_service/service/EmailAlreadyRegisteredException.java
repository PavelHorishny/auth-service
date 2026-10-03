package com.company.auth_service.service;

public class EmailAlreadyRegisteredException extends RuntimeException {
    public EmailAlreadyRegisteredException(String email) {
        super("Email already registered: " + email);
    }
}
//Separate folder for exceptions classes?? or to domain folder as it doesn't depend on any other class?
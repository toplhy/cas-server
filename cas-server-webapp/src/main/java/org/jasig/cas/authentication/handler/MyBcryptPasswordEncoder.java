package org.jasig.cas.authentication.handler;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;


public class MyBcryptPasswordEncoder implements PasswordEncoder {

    @Override
    public String encode(String password) {
        if (password == null) {
            return null;
        }

        BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();
        return bCryptPasswordEncoder.encode(password);
    }
}

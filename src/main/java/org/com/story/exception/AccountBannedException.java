package org.com.story.exception;

public class AccountBannedException extends AppException {

    public AccountBannedException(String message) {
        super("ACCOUNT_BANNED", message);
    }
}


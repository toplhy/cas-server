package org.jasig.cas.authentication;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public class CustomUsernamePasswordCredential extends UsernamePasswordCredential{

    @NotNull
    @Size(min=1, message = "required.captcha")
    private String captcha;

    public CustomUsernamePasswordCredential() {
    }

    public CustomUsernamePasswordCredential(String userName, String password, String captcha) {
        super(userName, password);
        this.captcha = captcha;
    }

    public String getCaptcha() {
        return captcha;
    }

    public void setCaptcha(String captcha) {
        this.captcha = captcha;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final CustomUsernamePasswordCredential that = (CustomUsernamePasswordCredential) o;

        if (getPassword() != null ? !getPassword().equals(that.getPassword()) : that.getPassword() != null) {
            return false;
        }

        if (getUsername() != null ? !getUsername().equals(that.getUsername()) : that.getUsername() != null) {
            return false;
        }

        if (getCaptcha() != null ? !getCaptcha().equals(that.getCaptcha()) : that.getCaptcha() != null) {
            return false;
        }

        return true;
    }

}

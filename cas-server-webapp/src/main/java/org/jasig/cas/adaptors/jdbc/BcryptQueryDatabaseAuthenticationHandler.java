package org.jasig.cas.adaptors.jdbc;

import org.apache.commons.lang3.StringUtils;
import org.jasig.cas.authentication.HandlerResult;
import org.jasig.cas.authentication.PreventedException;
import org.jasig.cas.authentication.UsernamePasswordCredential;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import javax.security.auth.login.AccountNotFoundException;
import javax.security.auth.login.FailedLoginException;
import javax.sql.DataSource;
import javax.validation.constraints.NotNull;
import java.security.GeneralSecurityException;

@Component("bcryptQueryDatabaseAuthenticationHandler")
public class BcryptQueryDatabaseAuthenticationHandler extends AbstractJdbcUsernamePasswordAuthenticationHandler {

    @NotNull
    private String sql;

    @Override
    protected HandlerResult authenticateUsernamePasswordInternal(UsernamePasswordCredential credential)
            throws GeneralSecurityException, PreventedException {

        if (StringUtils.isBlank(this.sql) || getJdbcTemplate() == null) {
            throw new GeneralSecurityException("Authentication handler is not configured correctly");
        }

        final String username = credential.getUsername();
//        final String encryptedPassword = this.getPasswordEncoder().encode(credential.getPassword());
        try {
            final String dbPassword = getJdbcTemplate().queryForObject(this.sql, String.class, username);
//            if (!dbPassword.equals(encryptedPassword)) {
//                throw new FailedLoginException("Password does not match value on record.");
//            }
            BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();
            if(!bCryptPasswordEncoder.matches(credential.getPassword(),dbPassword)){
                throw new FailedLoginException("Password does not match value on record.");
            }
        } catch (final IncorrectResultSizeDataAccessException e) {
            if (e.getActualSize() == 0) {
                throw new AccountNotFoundException(username + " not found with SQL query");
            } else {
                throw new FailedLoginException("Multiple records found for " + username);
            }
        } catch (final DataAccessException e) {
            throw new PreventedException("SQL exception while executing query for " + username, e);
        }
        return createHandlerResult(credential, this.principalFactory.createPrincipal(username), null);
    }

    /**
     * @param sql The sql to set.
     */
    @Autowired
    public void setSql(@Value("${cas.jdbc.authn.query.sql:}") final String sql) {
        this.sql = sql;
    }

    @Override
    @Autowired(required = false)
    public void setDataSource(@Qualifier("queryDatabaseDataSource") final DataSource dataSource) {
        super.setDataSource(dataSource);
    }

}

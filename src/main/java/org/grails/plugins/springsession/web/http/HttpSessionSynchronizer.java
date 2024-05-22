package org.grails.plugins.springsession.web.http;

import org.springframework.core.annotation.Order;
import org.springframework.session.web.http.SessionRepositoryFilter;
import org.springframework.util.Assert;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Enumeration;

/**
 * A filter to synchronize HTTP sessions based on Spring Session configuration properties.
 */
@Order(SessionRepositoryFilter.DEFAULT_ORDER + 1)
public class HttpSessionSynchronizer extends OncePerRequestFilter {

    private final SpringSessionConfigProperties springSessionConfigProperties;
    private Boolean persistMutable;

    /**
     * Constructor for HttpSessionSynchronizer.
     *
     * @param springSessionConfigProperties the properties used to configure the session.
     */
    public HttpSessionSynchronizer(SpringSessionConfigProperties springSessionConfigProperties) {
        this.springSessionConfigProperties = springSessionConfigProperties;
        this.persistMutable = springSessionConfigProperties.getAllowPersistMutable();
    }

    @Override
    public void afterPropertiesSet() throws ServletException {
        super.afterPropertiesSet();
        Assert.notNull(persistMutable, "persistMutable property must not be null");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        filterChain.doFilter(request, response);
        if (Boolean.TRUE.equals(persistMutable) && request != null && request.getSession() != null) {
            HttpSession session = request.getSession();
            Enumeration<String> attributeNames = session.getAttributeNames();
            while (attributeNames.hasMoreElements()) {
                String key = attributeNames.nextElement();
                try {
                    Object object = session.getAttribute(key);
                    session.setAttribute(key, object);
                } catch (Exception ignored) {
                    // Log ignored exception if necessary
                }
            }
        }
    }

    /**
     * Sets the persistMutable property.
     *
     * @param persistMutable whether to allow persisting mutable sessions
     */
    public void setPersistMutable(Boolean persistMutable) {
        this.persistMutable = persistMutable;
    }
}

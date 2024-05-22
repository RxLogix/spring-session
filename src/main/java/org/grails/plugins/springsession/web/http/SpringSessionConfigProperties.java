package org.grails.plugins.springsession.web.http;

import org.springframework.stereotype.Component;

@Component
public class SpringSessionConfigProperties {
    private int maxInactiveInterval;
    private String mapName;
    private Boolean allowPersistMutable;

    /**
     * Sets the configuration values.
     *
     * @param maxInactiveInterval the maximum inactive interval in seconds
     * @param mapName             the name of the session map
     * @param allowPersistMutable whether to allow persisting mutable sessions
     */
    public void setValues(int maxInactiveInterval, String mapName, Boolean allowPersistMutable) {
        this.maxInactiveInterval = maxInactiveInterval;
        this.mapName = mapName;
        this.allowPersistMutable = allowPersistMutable;
    }

    // Getters and setters

    public int getMaxInactiveInterval() {
        return maxInactiveInterval;
    }

    public void setMaxInactiveInterval(int maxInactiveInterval) {
        this.maxInactiveInterval = maxInactiveInterval;
    }

    public String getMapName() {
        return mapName;
    }

    public void setMapName(String mapName) {
        this.mapName = mapName;
    }

    public Boolean getAllowPersistMutable() {
        return allowPersistMutable;
    }

    public void setAllowPersistMutable(Boolean allowPersistMutable) {
        this.allowPersistMutable = allowPersistMutable;
    }
}

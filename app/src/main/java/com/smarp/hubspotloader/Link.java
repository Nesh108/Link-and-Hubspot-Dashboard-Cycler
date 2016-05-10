package com.smarp.hubspotloader;

public class Link {

    private String URL;
    private boolean isHubspot;

    public Link(String url, boolean isHub) {
        URL = url;
        isHubspot = isHub;
    }

    public String getURL() {
        return URL;
    }

    public boolean isHubspot() {
        return isHubspot;
    }
}

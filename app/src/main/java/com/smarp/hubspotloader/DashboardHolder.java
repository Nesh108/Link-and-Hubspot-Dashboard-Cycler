package com.smarp.hubspotloader;

import android.content.Context;

import java.util.ArrayList;

public class DashboardHolder {

    ArrayList<Link> dashes;
    int index;
    Context parent;

    public DashboardHolder(Context parent, ArrayList<Link> dashes) {
        this.dashes = dashes;
        this.parent = parent;
        index = 0;
    }

    public String next() {
        Link l = dashes.get(index);
        index++;

        // restart
        if (index >= dashes.size())
            index = 0;

        if (l.isHubspot())
            return parent.getResources().getString(R.string.base_url) + l.getURL();
        else
            return l.getURL();
    }

    public String first() {

        if (dashes.get(0).isHubspot())
            return parent.getResources().getString(R.string.base_url) + dashes.get(0).getURL();
        else
            return dashes.get(0).getURL();
    }

    public String current() {
        Link l = dashes.get(index);

        if (l.isHubspot())
            return parent.getResources().getString(R.string.base_url) + l.getURL();
        else
            return l.getURL();
    }
}

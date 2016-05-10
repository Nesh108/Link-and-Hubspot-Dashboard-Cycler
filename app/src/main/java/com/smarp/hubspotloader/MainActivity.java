/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.smarp.hubspotloader;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.http.SslError;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.SslErrorHandler;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;

import java.util.ArrayList;

/*
 * MainActivity class that loads MainFragment
 */
public class MainActivity extends Activity {
    /**
     * Called when the activity is first created.
     */

    WebView webView;
    DashboardHolder dh;
    ArrayList<Link> dashboards;

    String hubUsername;
    String hubPassword;
    boolean anyHubLinks = false;
    boolean loadingPage = false;

    int timeout = 60;   // in sec

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (PrefUtils.getFromPrefs(this, PrefUtils.PREFS_HUBSPOT_USERNAME_KEY, "__UNKNOWN__").equals("__UNKNOWN__")) {
            showLoginDialog(getBaseContext());
        } else {
            startLoader();
        }

    }

    private void startLoader() {
        hubUsername = PrefUtils.getFromPrefs(this, PrefUtils.PREFS_HUBSPOT_USERNAME_KEY, "__NOT_SET__");
        hubPassword = PrefUtils.getFromPrefs(this, PrefUtils.PREFS_HUBSPOT_PASSWORD_KEY, "__NOT_SET__");

        try {
            timeout = Integer.parseInt(PrefUtils.getFromPrefs(this, PrefUtils.PREFS_TIMEOUT, "" + timeout));
        } catch (Exception e) {
            // Keep default
        }

        processLinks();

        String newUA = "Mozilla/5.0 (X11; U; Linux i686; en-US; rv:1.9.0.4) Gecko/20100101 Firefox/4.0";

        webView = (WebView) findViewById(R.id.main_wv);
        webView.getSettings().setUserAgentString(newUA);
        webView.getSettings().setUseWideViewPort(true);
        webView.getSettings().setLoadWithOverviewMode(true);
        webView.setScrollBarStyle(WebView.SCROLLBARS_OUTSIDE_OVERLAY);
        webView.setScrollbarFadingEnabled(false);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.setPadding(0, 0, 0, 0);
        webView.getSettings().setDomStorageEnabled(true);
        webView.setInitialScale(1);
        webView.zoomBy(0.02f);

        dh = new DashboardHolder(getBaseContext(), dashboards);

        loadPages();

        final Handler h = new Handler();

        h.postDelayed(new Runnable() {
            public void run() {

                loadPages();

                h.postDelayed(this, timeout * 1000);
            }
        }, timeout * 1000);

    }

    private void loadPages() {
        loadingPage = true;
        webView.loadUrl(dh.next()); // Loads the next URL

        // Load the first dashboard
        Log.d("Load-URL", dh.current());

        webView.setWebViewClient(new WebViewClient() {

            public void onPageFinished(WebView view, String url) {

                if (loadingPage) {
                    Log.d("URL-Title", webView.getTitle());

                    // Check if login went through
                    if (webView.getTitle().equals("Login to HubSpot") && anyHubLinks) {
                        login();
                    }

                    loadingPage = false;
                }

            }

            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                super.onReceivedSslError(view, handler, error);
                handler.proceed(); // Ignore SSL certificate errors
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                webView.loadUrl(url);
                return true;
            }
        });
    }

    private void login() {
        // Get Email Textbox
        webView.loadUrl("javascript:void(document.getElementById('username').value =\"" + hubUsername + "\")");

        // Get Password Textbox
        webView.loadUrl("javascript:void(document.getElementById('password').value=\"" + hubPassword + "\")");

        // Click login button
        webView.loadUrl("javascript:void(document.getElementById('loginBtn').click())");

    }


    private void showLoginDialog(Context parent) {
        // get prompts.xml view
        LayoutInflater li = LayoutInflater.from(MainActivity.this);
        View promptsView = li.inflate(R.layout.info_prompt, null);

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                MainActivity.this);

        // set xml to alertdialog builder
        alertDialogBuilder.setView(promptsView);

        final EditText usernameEdit = (EditText) promptsView
                .findViewById(R.id.usernameEdit);
        final EditText passwordEdit = (EditText) promptsView
                .findViewById(R.id.passwordEdit);
        final EditText dashboardsEdit = (EditText) promptsView
                .findViewById(R.id.dashboardsEdit);
        final EditText linksEdit = (EditText) promptsView
                .findViewById(R.id.linksEdit);
        final EditText timeoutEdit = (EditText) promptsView
                .findViewById(R.id.timeoutEdit);

        final Context p = parent;

        // set dialog message
        alertDialogBuilder
                .setCancelable(false)
                .setPositiveButton("Save",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // Save information
                                PrefUtils.saveToPrefs(p, PrefUtils.PREFS_HUBSPOT_USERNAME_KEY, usernameEdit.getText().toString());
                                PrefUtils.saveToPrefs(p, PrefUtils.PREFS_HUBSPOT_PASSWORD_KEY, passwordEdit.getText().toString());
                                PrefUtils.saveToPrefs(p, PrefUtils.PREFS_HUBSPOTS_DASHBOARDS, dashboardsEdit.getText().toString());
                                PrefUtils.saveToPrefs(p, PrefUtils.PREFS_OTHER_LINKS, linksEdit.getText().toString());
                                PrefUtils.saveToPrefs(p, PrefUtils.PREFS_TIMEOUT, timeoutEdit.getText().toString());

                                startLoader();
                            }
                        })
                .setNegativeButton("Quit",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                System.exit(0);
                            }
                        });

        // create alert dialog
        AlertDialog alertDialog = alertDialogBuilder.create();

        // show it
        alertDialog.show();

    }

    private void processLinks() {

        String[] hubspotDashboards = null;
        String[] otherLinks = null;
        String savedDashboards = PrefUtils.getFromPrefs(this, PrefUtils.PREFS_HUBSPOTS_DASHBOARDS, "__UNKNOWN__");
        String savedLinks = PrefUtils.getFromPrefs(this, PrefUtils.PREFS_OTHER_LINKS, "__UNKNOWN__");

        if (!savedDashboards.equals("__UNKNOWN__")) {
            hubspotDashboards = savedDashboards.split(",");
        }

        if (!savedLinks.equals("__UNKNOWN__")) {
            otherLinks = savedLinks.split(",");
        }

        dashboards = new ArrayList<>();

        if (hubspotDashboards != null) {
            anyHubLinks = true;
            for (String s : hubspotDashboards) {
                dashboards.add(new Link(s.replace(" ", ""), true));
            }
        }

        if (otherLinks != null) {
            for (String s : otherLinks) {
                dashboards.add(new Link(s.replace(" ", ""), false));
            }
        }
//
//        // Debugging
//        for (Link l : dashboards)
//            Log.d("URL", l.getURL());

    }
}

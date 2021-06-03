/*
 * Ven's Aliucord Plugins
 * Copyright (C) 2021 Vendicated
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at 
 * http://www.apache.org/licenses/LICENSE-2.0
*/

package com.aliucord.plugins.plugindownloader;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.widget.NestedScrollView;

import com.aliucord.Http;
import com.aliucord.Utils;
import com.aliucord.fragments.SettingsPage;
import com.aliucord.views.Button;
import com.google.gson.reflect.TypeToken;
import com.lytefast.flexinput.R$h;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;

public final class Modal extends SettingsPage {
    private static final Type resType = TypeToken.getParameterized(Map.class, String.class, Plugin.Info.class).getType();

    private final String author;
    private final String repo;
    private Map<String, Plugin.Info> plugins;
    private IOException ex;

    public Modal(String author, String repo) {
        super();
        this.author = author;
        this.repo = repo;
    }

    @Override
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        setActionBarTitle("Plugin downloader");
        setActionBarSubtitle(String.format("https://github.com/%s/%s", author, repo));
    }

    @Override
    @SuppressLint("SetTextI18n")
    public void onViewBound(View view) {
        super.onViewBound(view);
        var ctx = view.getContext();
        var layout = (LinearLayout) ((NestedScrollView) ((CoordinatorLayout) view).getChildAt(1)).getChildAt(0);
        int p = Utils.getDefaultPadding();
        layout.setPadding(p, p, p, p);

        if (ex != null) {
            var exView = new TextView(ctx, null, 0, R$h.UiKit_Settings_Item_SubText);
            var sw = new StringWriter();
            var pw = new PrintWriter(sw);
            ex.printStackTrace(pw);
            exView.setText("An error:\n\n" + sw.toString());
            exView.setTextIsSelectable(true);
            layout.addView(exView);
        } else if (plugins == null) {
            new Thread(() -> {
                try {
                    plugins = Http.simpleJsonGet(getUpdaterUrl(), resType);
                } catch (IOException e) {
                    ex = e;
                }
                new Handler(Looper.getMainLooper()).post(() -> {
                    onViewBound(view);
                });
            }).start();
        } else {
            var list = new ArrayList<Plugin.CardInfo>();
            for (var plugin : plugins.entrySet()) {
                String name = plugin.getKey();
                if (name.equals("default")) continue;;
                boolean exists = PDUtil.isPluginInstalled(name);
                String title = String.format("%s %s v%s", exists ? "Uninstall" : "Install", name, plugin.getValue().version);
                list.add(new Plugin.CardInfo(name, title, exists));
            }
            Collections.sort(list, (a,b) -> a.title.compareTo(b.title));
            for (var plugin: list) {
                var button = new Button(ctx, plugin.exists);
                button.setText(plugin.title);
                Runnable callback = () -> {
                    layout.removeAllViews();
                    onViewBound(view);
                };
                if (!plugin.exists) button.setOnClickListener(e -> {
                    PDUtil.downloadPlugin(ctx, author, repo, plugin.name, callback);
                });
                else button.setOnClickListener(e -> PDUtil.deletePlugin(ctx, plugin.name, callback));
                layout.addView(button);
            }
        }
    }

    private String getUpdaterUrl() {
        return String.format("https://raw.githubusercontent.com/%s/%s/builds/updater.json", author, repo);
    }
}
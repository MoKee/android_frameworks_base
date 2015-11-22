/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.commands.settings;

import android.app.ActivityManagerNative;
import android.app.IActivityManager;
import android.app.IActivityManager.ContentProviderHolder;
import android.content.IContentProvider;
import android.database.Cursor;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Process;
import android.os.RemoteException;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;
import mokee.providers.MKSettings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class SettingsCmd {

    private static final String SETTINGS_AUTHORITY = Settings.AUTHORITY;
    private static final String MKSETTINGS_AUTHORITY = MKSettings.AUTHORITY;

    enum CommandVerb {
        UNSPECIFIED,
        GET,
        PUT,
        DELETE,
        LIST,
    }

    static String[] mArgs;
    int mNextArg;
    int mUser = -1;     // unspecified
    CommandVerb mVerb = CommandVerb.UNSPECIFIED;
    String mTable = null;
    String mKey = null;
    String mValue = null;
    boolean mUseMKSettingsProvider = false;

    public static void main(String[] args) {
        if (args == null || args.length < 2) {
            printUsage();
            return;
        }

        mArgs = args;
        try {
            new SettingsCmd().run();
        } catch (Exception e) {
            System.err.println("Unable to run settings command");
        }
    }

    public void run() {
        boolean valid = false;
        String arg;
        try {
            while ((arg = nextArg()) != null) {
                if ("--user".equals(arg)) {
                    if (mUser != -1) {
                        // --user specified more than once; invalid
                        break;
                    }
                    mUser = Integer.parseInt(nextArg());
                } else if ("--mk".equals(arg)) {
                    mUseMKSettingsProvider = true;
                } else if (mVerb == CommandVerb.UNSPECIFIED) {
                    if ("get".equalsIgnoreCase(arg)) {
                        mVerb = CommandVerb.GET;
                    } else if ("put".equalsIgnoreCase(arg)) {
                        mVerb = CommandVerb.PUT;
                    } else if ("delete".equalsIgnoreCase(arg)) {
                        mVerb = CommandVerb.DELETE;
                    } else if ("list".equalsIgnoreCase(arg)) {
                        mVerb = CommandVerb.LIST;
                    } else {
                        // invalid
                        System.err.println("Invalid command: " + arg);
                        break;
                    }
                } else if (mTable == null) {
                    if (!"system".equalsIgnoreCase(arg)
                            && !"secure".equalsIgnoreCase(arg)
                            && !"global".equalsIgnoreCase(arg)) {
                        System.err.println("Invalid namespace '" + arg + "'");
                        break;  // invalid
                    }
                    mTable = arg.toLowerCase();
                    if (mVerb == CommandVerb.LIST) {
                        valid = true;
                        break;
                    }
                } else if (mVerb == CommandVerb.GET || mVerb == CommandVerb.DELETE) {
                    mKey = arg;
                    if (mNextArg >= mArgs.length) {
                        valid = true;
                    } else {
                        System.err.println("Too many arguments");
                    }
                    break;
                } else if (mKey == null) {
                    mKey = arg;
                    // keep going; there's another PUT arg
                } else {    // PUT, final arg
                    mValue = arg;
                    if (mNextArg >= mArgs.length) {
                        valid = true;
                    } else {
                        System.err.println("Too many arguments");
                    }
                    break;
                }
            }
        } catch (Exception e) {
            valid = false;
        }

        if (valid) {
            if (mUser < 0) {
                mUser = UserHandle.USER_OWNER;
            }

            // Implicitly use MKSettings provider if the setting is a legacy setting
            if (!mUseMKSettingsProvider && isLegacySetting(mTable, mKey)) {
                System.err.println("'" + mKey + "' has moved to MKSettings.  Use --mk to avoid " +
                        "this warning in the future.");
                mUseMKSettingsProvider = true;
            }

            try {
                IActivityManager activityManager = ActivityManagerNative.getDefault();
                IContentProvider provider = null;
                IBinder token = new Binder();
                try {
                    ContentProviderHolder holder = activityManager.getContentProviderExternal(
                            mUseMKSettingsProvider ? MKSETTINGS_AUTHORITY : SETTINGS_AUTHORITY,
                            UserHandle.USER_OWNER, token);
                    if (holder == null) {
                        throw new IllegalStateException("Could not find settings provider");
                    }
                    provider = holder.provider;

                    switch (mVerb) {
                        case GET:
                            System.out.println(getForUser(provider, mUser, mTable, mKey));
                            break;
                        case PUT:
                            putForUser(provider, mUser, mTable, mKey, mValue);
                            break;
                        case DELETE:
                            System.out.println("Deleted "
                                    + deleteForUser(provider, mUser, mTable, mKey) + " rows");
                            break;
                        case LIST:
                            for (String line : listForUser(provider, mUser, mTable)) {
                                System.out.println(line);
                            }
                            break;
                        default:
                            System.err.println("Unspecified command");
                            break;
                    }

                } finally {
                    if (provider != null) {
                        activityManager.removeContentProviderExternal("settings", token);
                    }
                }
            } catch (Exception e) {
                System.err.println("Error while accessing settings provider");
                e.printStackTrace();
            }

        } else {
            printUsage();
        }
    }

    private List<String> listForUser(IContentProvider provider, int userHandle, String table) {
        final Uri systemUri = mUseMKSettingsProvider ? MKSettings.System.CONTENT_URI
                : Settings.System.CONTENT_URI;
        final Uri secureUri = mUseMKSettingsProvider ? MKSettings.Secure.CONTENT_URI
                : Settings.Secure.CONTENT_URI;
        final Uri globalUri = mUseMKSettingsProvider ? MKSettings.Global.CONTENT_URI
                : Settings.Global.CONTENT_URI;
        final Uri uri = "system".equals(table) ? systemUri
                : "secure".equals(table) ? secureUri
                : "global".equals(table) ? globalUri
                : null;
        final ArrayList<String> lines = new ArrayList<String>();
        if (uri == null) {
            return lines;
        }
        try {
            final Cursor cursor = provider.query(resolveCallingPackage(), uri, null, null, null,
                    null, null);
            try {
                while (cursor != null && cursor.moveToNext()) {
                    lines.add(cursor.getString(1) + "=" + cursor.getString(2));
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
            Collections.sort(lines);
        } catch (RemoteException e) {
            System.err.println("List failed in " + table + " for user " + userHandle);
        }
        return lines;
    }

    private String nextArg() {
        if (mNextArg >= mArgs.length) {
            return null;
        }
        String arg = mArgs[mNextArg];
        mNextArg++;
        return arg;
    }

    String getForUser(IContentProvider provider, int userHandle,
            final String table, final String key) {
        final String systemGetCommand = mUseMKSettingsProvider ? MKSettings.CALL_METHOD_GET_SYSTEM
                : Settings.CALL_METHOD_GET_SYSTEM;
        final String secureGetCommand = mUseMKSettingsProvider ? MKSettings.CALL_METHOD_GET_SECURE
                : Settings.CALL_METHOD_GET_SECURE;
        final String globalGetCommand = mUseMKSettingsProvider ? MKSettings.CALL_METHOD_GET_GLOBAL
                : Settings.CALL_METHOD_GET_GLOBAL;
        final String callGetCommand;
        if ("system".equals(table)) callGetCommand = systemGetCommand;
        else if ("secure".equals(table)) callGetCommand = secureGetCommand;
        else if ("global".equals(table)) callGetCommand = globalGetCommand;
        else {
            System.err.println("Invalid table; no put performed");
            throw new IllegalArgumentException("Invalid table " + table);
        }

        String result = null;
        try {
            Bundle arg = new Bundle();
            arg.putInt(mUseMKSettingsProvider ? MKSettings.CALL_METHOD_USER_KEY
                    : Settings.CALL_METHOD_USER_KEY, userHandle);
            Bundle b = provider.call(resolveCallingPackage(), callGetCommand, key, arg);
            if (b != null) {
                result = b.getPairValue();
            }
        } catch (RemoteException e) {
            System.err.println("Can't read key " + key + " in " + table + " for user " + userHandle);
        }
        return result;
    }

    void putForUser(IContentProvider provider, int userHandle,
            final String table, final String key, final String value) {
        final String systemPutCommand = mUseMKSettingsProvider ? MKSettings.CALL_METHOD_PUT_SYSTEM
                : Settings.CALL_METHOD_PUT_SYSTEM;
        final String securePutCommand = mUseMKSettingsProvider ? MKSettings.CALL_METHOD_PUT_SECURE
                : Settings.CALL_METHOD_PUT_SECURE;
        final String globalPutCommand = mUseMKSettingsProvider ? MKSettings.CALL_METHOD_PUT_GLOBAL
                : Settings.CALL_METHOD_PUT_GLOBAL;
        final String callPutCommand;
        if ("system".equals(table)) callPutCommand = systemPutCommand;
        else if ("secure".equals(table)) callPutCommand = securePutCommand;
        else if ("global".equals(table)) callPutCommand = globalPutCommand;
        else {
            System.err.println("Invalid table; no put performed");
            return;
        }

        try {
            Bundle arg = new Bundle();
            arg.putString(Settings.NameValueTable.VALUE, value);
            arg.putInt(mUseMKSettingsProvider ? MKSettings.CALL_METHOD_USER_KEY
                    : Settings.CALL_METHOD_USER_KEY, userHandle);
            provider.call(resolveCallingPackage(), callPutCommand, key, arg);
        } catch (RemoteException e) {
            System.err.println("Can't set key " + key + " in " + table + " for user " + userHandle);
        }
    }

    int deleteForUser(IContentProvider provider, int userHandle,
            final String table, final String key) {
        final Uri systemUri = mUseMKSettingsProvider ? MKSettings.System.getUriFor(key)
                : Settings.System.getUriFor(key);
        final Uri secureUri = mUseMKSettingsProvider ? MKSettings.Secure.getUriFor(key)
                : Settings.Secure.getUriFor(key);
        final Uri globalUri = mUseMKSettingsProvider ? MKSettings.Global.getUriFor(key)
                : Settings.Global.getUriFor(key);
        Uri targetUri;
        if ("system".equals(table)) targetUri = systemUri;
        else if ("secure".equals(table)) targetUri = secureUri;
        else if ("global".equals(table)) targetUri = globalUri;
        else {
            System.err.println("Invalid table; no delete performed");
            throw new IllegalArgumentException("Invalid table " + table);
        }

        int num = 0;
        try {
            num = provider.delete(resolveCallingPackage(), targetUri, null, null);
        } catch (RemoteException e) {
            System.err.println("Can't clear key " + key + " in " + table + " for user "
                    + userHandle);
        }
        return num;
    }

    private static void printUsage() {
        System.err.println("usage:  settings [--user NUM] [--mk] get namespace key");
        System.err.println("        settings [--user NUM] [--mk] put namespace key value");
        System.err.println("        settings [--user NUM] [--mk] delete namespace key");
        System.err.println("        settings [--user NUM] [--mk] list namespace");
        System.err.println("\n'namespace' is one of {system, secure, global}, case-insensitive");
        System.err.println("If '--user NUM' is not given, the operations are performed on the owner user.");
        System.err.println("If '--mk' is given, the operations are performed on the MKSettings provider.");
    }

    private static boolean isLegacySetting(String table, String key) {
        if (!TextUtils.isEmpty(key)) {
            if ("system".equals(table)) {
                return MKSettings.System.isLegacySetting(key);
            } else if ("secure".equals(table)) {
                return MKSettings.Secure.isLegacySetting(key);
            } else if ("global".equals(table)) {
                return MKSettings.Global.isLegacySetting(key);
            }
        }
        return false;
    }

    public static String resolveCallingPackage() {
        switch (android.os.Process.myUid()) {
            case Process.ROOT_UID: {
                return "root";
            }

            case Process.SHELL_UID: {
                return "com.android.shell";
            }

            default: {
                return null;
            }
        }
    }
}

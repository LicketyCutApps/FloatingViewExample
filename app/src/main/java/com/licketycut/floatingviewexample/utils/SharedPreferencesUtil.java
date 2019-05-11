/*
 * Copyright 2019 Adam Claflin [adam.r.claflin@gmail.com].
 *
 * Licensed under the Attribution-NonCommercial 4.0 International (CC BY-NC 4.0);
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://creativecommons.org/licenses/by-nc/4.0/
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.licketycut.floatingviewexample.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/** Simple singleton class to set and get default shared preferences. */
public class SharedPreferencesUtil {
    private static final String KEY_APP_SHOWING = "KEY_APP_SHOWING";

    // One and only instance of our singleton class.
    private static final SharedPreferencesUtil ourInstance = new SharedPreferencesUtil();

    public static SharedPreferencesUtil getInstance() {
        return ourInstance;
    }

    private SharedPreferencesUtil() { }

    // Get a handle to the apps default shared preferences.
    private SharedPreferences getSharedPreferences(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }

    // Get flag set by the main activity when it moves to back or moves to front.
    public boolean isAppShowing(Context context) {
        return getSharedPreferences(context).getBoolean(KEY_APP_SHOWING, true);
    }

    // Set flag to indicate weather main activity is showing or not.
    public void setIsAppShowing(Context context, boolean isAppShowing) {
        getSharedPreferences(context).edit().putBoolean(KEY_APP_SHOWING, isAppShowing).apply();
    }

}

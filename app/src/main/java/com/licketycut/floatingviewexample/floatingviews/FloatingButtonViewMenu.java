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

package com.licketycut.floatingviewexample.floatingviews;

import android.content.Context;
import android.view.ContextThemeWrapper;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;

import com.licketycut.floatingviewexample.R;

/** Popup menu manager class for our {@link FloatingButtonView}. */
class FloatingButtonViewMenu {
    private final android.widget.PopupMenu popupMenu;

    /** Inflate the popup menu, attach it to the anchor view and set item click listener. */
    FloatingButtonViewMenu(Context context, View anchor, int menu, 
                                    final OnFloatingButtonMenuCallback callback) {
        // Inflate the popup menu utilizing our style resource.
        Context wrapper = new ContextThemeWrapper(context, R.style.PopUpMenu);
        popupMenu = new PopupMenu(wrapper, anchor);
        popupMenu.inflate(menu);

        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                // Make callbacks according to the menu item clicked.
                switch (item.getItemId()) {
                    case R.id.menu_close:
                        if (callback != null) {
                            callback.onCloseItemClick();
                            break;
                        }
                        break;
                    case R.id.menu_show_main:
                        callback.onShowMainItemClick();
                        break;
                }
                // Dismiss popup menu when any item is clicked.
                popupMenu.dismiss();
                return true;
            }
        });
    }

    void show() {
        popupMenu.show();
    }

    /** Default callback methods can be overriden when the menu is created. */
    public interface OnFloatingButtonMenuCallback {

        void onShowMainItemClick();

        void onCloseItemClick();
    }
}

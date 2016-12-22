/**
 * Copyright (c) 2016, The Smartisan Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
 
package android.view.onestep;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import android.text.TextUtils;

import com.smartisanos.internal.R;

/**
 * Utilities for dealing with MIME types.
 */
final class MimeUtils {
    private static final Map<String, Integer> mimeTypeToIconMap = new HashMap<String, Integer>();
    private static final Map<String, Integer> extensionToIconMap = new HashMap<String, Integer>();

    static {
        add("", "fodp", R.drawable.file_icon_fodp);
        add("", "fods", R.drawable.file_icon_fods);
        add("", "fodt", R.drawable.file_icon_fodt);
        add("", "pages", R.drawable.file_icon_pages);
        add("", "numbers", R.drawable.file_icon_numbers);
        add("application/vnd.ms-word.document.macroenabled.12", "docm", R.drawable.file_icon_docm);
        add("application/x-7z-compressed", "7z", R.drawable.file_icon_7z);
        add("application/vnd.oasis.opendocument.presentation", "odp", R.drawable.file_icon_odp);
        add("application/vnd.oasis.opendocument.text", "odt", R.drawable.file_icon_odt);
        add("application/pdf", "pdf", R.drawable.file_icon_pdf);
        add("application/pgp-keys", "key", R.drawable.file_icon_key);
        add("application/rar", "rar", R.drawable.file_icon_rar);
        add("application/zip", "zip", R.drawable.file_icon_zip);
        add("application/vnd.android.package-archive", "apk", R.drawable.file_icon_apk);
        add("application/vnd.oasis.opendocument.spreadsheet", "ods", R.drawable.file_icon_ods);
        add("application/msword", "doc", R.drawable.file_icon_doc);
        add("application/msword", "dot", R.drawable.file_icon_dot);
        add("application/vnd.ms-word.template.macroenabled.12", "dotm", R.drawable.file_icon_dotm);
        add("application/vnd.openxmlformats-officedocument.wordprocessingml.document", "docx", R.drawable.file_icon_docx);
        add("application/vnd.openxmlformats-officedocument.wordprocessingml.template", "dotx", R.drawable.file_icon_dotx);
        add("application/vnd.ms-excel", "xls", R.drawable.file_icon_xls);
        add("application/vnd.ms-excel", "xlt", R.drawable.file_icon_xlt);
        add("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet","xlsx", R.drawable.file_icon_xlsx);
        add("application/vnd.openxmlformats-officedocument.spreadsheetml.template", "xltx", R.drawable.file_icon_xltx);
        add("application/vnd.ms-powerpoint", "ppt", R.drawable.file_icon_ppt);
        add("application/vnd.ms-powerpoint", "pot", R.drawable.file_icon_pot);
        add("application/vnd.openxmlformats-officedocument.presentationml.presentation", "pptx", R.drawable.file_icon_pptx);
        add("application/vnd.ms-powerpoint.presentation.macroenabled.12", "pptm", R.drawable.file_icon_pptm);
        add("application/vnd.openxmlformats-officedocument.presentationml.template", "potx", R.drawable.file_icon_potx);
        add("text/plain", "txt", R.drawable.file_icon_txt);
        add("message/rfc822", "eml", R.drawable.file_icon_eml);
    }

    private static void add(String mimeType, String extension, int resId) {
        if (!TextUtils.isEmpty(mimeType)) {
            mimeTypeToIconMap.put(mimeType, resId);
        }
        if (!TextUtils.isEmpty(extension)) {
            extensionToIconMap.put(extension, resId);
        }
    }

    private MimeUtils() {
    }

    public static int getFileIconResId(String mimeType, String extension) {
        if (!TextUtils.isEmpty(extension)) {
            if (extensionToIconMap.containsKey(extension)) {
                return extensionToIconMap.get(extension);
            }
        }
        if (!TextUtils.isEmpty(mimeType)) {
            if (mimeTypeToIconMap.containsKey(mimeType)) {
                return mimeTypeToIconMap.get(mimeType);
            }
            // use default for current type
            if (mimeType.startsWith("images/")) {
                return R.drawable.file_icon_type_image;
            } else if (mimeType.startsWith("audio/")) {
                return R.drawable.file_icon_type_audio;
            } else if (mimeType.startsWith("video/")) {
                return R.drawable.file_icon_type_video;
            }
        }
        return R.drawable.file_icon_default;
    }

    public static int getFileIconResId(String mimeType, File file) {
        String suffix = null;
        String name = file.getName();
        if (!TextUtils.isEmpty(name)) {
            String[] sp = name.split("\\.");
            if (sp.length > 1) {
                suffix = sp[sp.length - 1];
            }
        }
        return MimeUtils.getFileIconResId(mimeType, suffix);
    }
}

/*
 * Copyright (c) 2010-2011, The MiCode Open Source Community (www.micode.net)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.rdjksp.mi.note.data;

import android.content.Context;
import android.database.Cursor;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.Data;
import android.telephony.PhoneNumberUtils;
import android.util.Log;

import java.util.HashMap;

public class Contact {
    private static HashMap<String, String> sContactCache;
    private static final String TAG = "Contact";

    private static final String CALLER_ID_SELECTION = "PHONE_NUMBERS_EQUAL(" + Phone.NUMBER
    + ",?) AND " + Data.MIMETYPE + "='" + Phone.CONTENT_ITEM_TYPE + "'"
    + " AND " + Data.RAW_CONTACT_ID + " IN "
            + "(SELECT raw_contact_id "
            + " FROM phone_lookup"
            + " WHERE min_match = '+')";
/*
1. 检查静态缓存 `sContactCache` 是否为空，如果是，则初始化一个 `HashMap`。
2. 检查缓存中是否已经包含传入的 `phoneNumber`，如果包含，则直接返回缓存中的联系人名称。
3. 如果缓存中没有找到对应的联系人，则准备一个SQL查询条件 `selection`，这个条件使用了 `PhoneNumberUtils.toCallerIDMinMatch` 方法来处理国际格式电话号码的匹配问题。
4. 使用 `context.getContentResolver().query` 方法查询 `Data.CONTENT_URI`，它代表了联系人数据的内容URI。查询的列只包括 `Phone.DISPLAY_NAME`，即联系人的显示名称。
5. 如果有查询结果且移动到第一条记录，则尝试获取联系人的名称，并将其存储在 `sContactCache` 中，然后返回这个名称。
6. 如果查询结果为空或者在尝试获取联系人名称时出现 `IndexOutOfBoundsException` 异常，则记录错误日志并返回 `null`。
7. 最后，无论查询成功与否，都会关闭游标以释放资源。
 */
    public static String getContact(Context context, String phoneNumber) {
        if(sContactCache == null) {
            sContactCache = new HashMap<>();
        }

        if(sContactCache.containsKey(phoneNumber)) {
            return sContactCache.get(phoneNumber);
        }

        String selection = CALLER_ID_SELECTION.replace("+",
                PhoneNumberUtils.toCallerIDMinMatch(phoneNumber));
        Cursor cursor = context.getContentResolver().query(
                Data.CONTENT_URI,
                new String [] { Phone.DISPLAY_NAME },
                selection,
                new String[] { phoneNumber },
                null);

        if (cursor != null && cursor.moveToFirst()) {
            try {
                String name = cursor.getString(0);
                sContactCache.put(phoneNumber, name);
                return name;
            } catch (IndexOutOfBoundsException e) {
                Log.e(TAG, " Cursor get string error " + e);
                return null;
            } finally {
                cursor.close();
            }
        } else {
            Log.d(TAG, "No contact matched with number:" + phoneNumber);
            return null;
        }
    }
}

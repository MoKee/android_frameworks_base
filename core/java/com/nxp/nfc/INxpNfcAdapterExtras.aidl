/*
 *
 *  Copyright (C) 2014 NXP Semiconductors
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package com.nxp.nfc;

import android.os.Bundle;
import com.nxp.intf.IJcopService;
import android.content.Intent;

/**
 * {@hide}
 */
interface INxpNfcAdapterExtras {

    int getSecureElementTechList(in String pkg);
    byte[] getSecureElementUid(in String pkg);
    boolean reset(in String pkg);
    Bundle getAtr(in String pkg);
    byte[] doGetRouting();
    void notifyCheckCertResult(in String pkg, in boolean success);
    void deliverSeIntent(String pkg, in Intent seIntent);
    int selectUicc(int uiccSlot);
    int getSelectedUicc();
    Bundle openuicc(in String pkg, IBinder b);
    Bundle closeuicc(in String pkg, IBinder b);
    Bundle transceiveuicc(in String pkg, in byte[] data_in);
    boolean eSEChipReset(in String pkg);
}
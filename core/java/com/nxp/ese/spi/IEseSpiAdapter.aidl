/*
 * Copyright (C) 2014 NXP Semiconductors
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

package com.nxp.ese.spi;

import android.os.Bundle;
import com.nxp.intf.IeSEClientServicesAdapter;
/**
 *{@hide}
 */
interface IEseSpiAdapter {
    int getState();
    boolean enable(int timeout,IBinder b);
    boolean disable(boolean saveState);
    boolean reset();
    boolean eseChipReset();
    byte[] transceive(in String pkg, in byte[] data_in);
    int getSeInterface(int type);
    IeSEClientServicesAdapter getSpieSEClientServicesAdapterInterface();
}

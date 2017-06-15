 /*
  * Copyright (C) 2015 NXP Semiconductors
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

package com.nxp.eseclient;

import android.util.Log;
import android.os.IBinder;
import android.os.RemoteException;
import com.nxp.intf.IeSEClientServicesAdapter;
import android.os.ServiceManager;
import java.io.IOException;
import java.lang.reflect.Method;

/** {@hide} */
public final class EseClientManager{
    private static final String TAG = "EseClientManager";
    private static Object sNfcService, sSpiService;
    private static Object sNxpNfcService, sNxpNfcExtrasService;
    private static EseClientManager mEseManager;
    public static final int NFC = 1;
    public static final int SPI = 2;
    public static final int LDRSERVICE = 1;
    public static final int JCPSERVICE = 2;
    public static final int LTSMSERVICE = 3;
    Class nfc, spi, nxpNfc;
    Class nfcStub, spiStub;
    Method getNxpAdapterMthd;

    static{
        mEseManager = new EseClientManager();
    }
    public static EseClientManager getInstance() {
        return mEseManager;
    }

    /** get handle to NFC service interface */
    private Object getNfcServiceInterface() {
        /* get a handle to NFC service */
        Object obj = null;
        IBinder b = ServiceManager.getService("nfc");
        if (b == null) {
            return null;
        }
        try{
            nfc = System.class.forName("android.nfc.INfcAdapter");
            Class cls[] = nfc.getClasses();
            for(int i=0; i<cls.length; i++) {
                Log.e(TAG, "cls["+i+"] = "+ cls[i].getSimpleName());
                if(cls[i].getSimpleName().compareTo("Stub") == 0) {
                    nfcStub = cls[i];
                }
            }
            Log.e(TAG, "Total number of classes = "+ cls.length);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            throw new UnsupportedOperationException();
        }
        if(nfcStub != null) {
            try{
                Method asInterfaceMethod = nfcStub.getDeclaredMethod("asInterface", IBinder.class);
                obj = asInterfaceMethod.invoke(null,b);
            } catch (Exception e) {
                Log.e(TAG, "nfc-asInterface method not found", e);
                throw new UnsupportedOperationException();
            }
        }
        return obj;
    }

    private Object getNxpNfcServiceInterface() {
        Object obj = null;
        if(sNfcService == null)
        {
            Log.e(TAG, "could not retrieve NFC service");
            throw new UnsupportedOperationException();
        }
        try {
            Method[] mthd = sNfcService.getClass().getMethods();
            for(int i=0; i<mthd.length; i++) {
                //Log.e(TAG, "mthd["+i+"] = "+ mthd[i].getName());
                if(mthd[i].getName().compareTo("getNxpNfcAdapterInterface") == 0) {
                    getNxpAdapterMthd = mthd[i];
                }
            }
            obj = getNxpAdapterMthd.invoke(sNfcService);
        } catch (Exception e) {
            Log.e(TAG, "getNxpNfcAdapterInterface method not found", e);
            throw new UnsupportedOperationException();
        }
        return obj;
    }

    private Object getNxpNfcExtrasAdapterInterface() {
        Object obj = null;
        if(sNxpNfcService != null)
        {
            try {
                Method getNxpExtrasInterface = sNxpNfcService.getClass().getDeclaredMethod("getNxpNfcAdapterExtrasInterface");
                obj = getNxpExtrasInterface.invoke(sNxpNfcService);
            } catch (Exception e) {
                Log.e(TAG, "getNxpNfcAdapterExtrasInterface method not found", e);
                throw new UnsupportedOperationException();
            }
        }
        return obj;
    }

    /** get handle to SPI service interface */
    private Object getSpiServiceInterface() {
        /* get a handle to SPI service */
        Object obj = null;
        IBinder b = ServiceManager.getService("spi");
        if (b == null) {
            return null;
        }
        try{
            spi = System.class.forName("com.nxp.ese.spi.IEseSpiAdapter");
            Class cls[] = spi.getClasses();
            for(int i=0; i<cls.length; i++) {
                Log.e(TAG, "cls["+i+"] = "+ cls[i].getSimpleName());
                if(cls[i].getSimpleName().compareTo("Stub") == 0) {
                    spiStub = cls[i];
                }
            }
        } catch (ClassNotFoundException e) {
            Log.e(TAG, "spi adapter class not found", e);
            e.printStackTrace();
            throw new UnsupportedOperationException();
        }
        if(spiStub != null) {
            try{
                Method asInterfaceMethod = spiStub.getDeclaredMethod("asInterface", IBinder.class);
                obj = asInterfaceMethod.invoke(null,b);
            } catch (Exception e) {
                Log.e(TAG, "spi-asInterface method not found", e);
                throw new UnsupportedOperationException();
            }
        }
        return obj;
    }

    public synchronized void initialize() {
        sNfcService = getNfcServiceInterface();
        if(sNfcService != null)
        {
            sNxpNfcService = getNxpNfcServiceInterface();
            if(sNxpNfcService != null)
            {
                sNxpNfcExtrasService = getNxpNfcExtrasAdapterInterface();
            }
        }
        sSpiService = getSpiServiceInterface();
    }

    public int getSeInterface(int type){
        try {
            Integer seMedium = 0;
            if(sNxpNfcService != null)
            {
                try{
                    Method getSeInterface = sNxpNfcService.getClass().getDeclaredMethod("getSeInterface", int.class);
                    seMedium = (Integer)getSeInterface.invoke(sNxpNfcService, type);
                } catch (Exception e) {
                    Log.e(TAG, "Nfc-getSeInterface method not found", e);
                    throw new UnsupportedOperationException();
                }
            }
            else if(sSpiService != null)
            {
                try{
                    Method getSeInterface = sSpiService.getClass().getDeclaredMethod("getSeInterface", int.class);
                    seMedium = (Integer)getSeInterface.invoke(sSpiService, type);
                } catch (Exception e) {
                    Log.e(TAG, "SPI-getSeInterface method not found", e);
                    throw new UnsupportedOperationException();
                }
            }
            else
            {
                seMedium = 0;
            }
            return seMedium;
        } catch (Exception e) {
            Log.e(TAG, "getSeInterface failed", e);
            throw new UnsupportedOperationException();
        }
    }
    /**
    * @hide
    */
    public IeSEClientServicesAdapter getNfcEseClientServicesAdapterInterface(){
        IeSEClientServicesAdapter ClientLdr = null;
        if(sNxpNfcService == null)
        {
            Log.e(TAG, "could not retrieve NxpNfc service");
            throw new UnsupportedOperationException();
        }
        try {
            Method getNxpClientInterfaceMthd = sNxpNfcService.getClass().getDeclaredMethod("getNfcEseClientServicesAdapterInterface");
            ClientLdr = (IeSEClientServicesAdapter)getNxpClientInterfaceMthd.invoke(sNxpNfcService);
        } catch (Exception e) {
            Log.e(TAG, "getNfcClientServicesInterface method not found", e);
            throw new UnsupportedOperationException();
        }
        return ClientLdr;
    }
    /**
    * @hide
    */
    public IeSEClientServicesAdapter getSpiEseClientServicesAdapterInterface(){
        IeSEClientServicesAdapter ClientLdr = null;
        if(sSpiService != null)
        {
            try {
                Method getNxpClientInterfaceMthd = sSpiService.getClass().getDeclaredMethod("getSpieSEClientServicesAdapterInterface");
                ClientLdr = (IeSEClientServicesAdapter)getNxpClientInterfaceMthd.invoke(sSpiService);
            } catch (Exception e) {
                Log.e(TAG, "getSpiClientServicesInterface failed", e);
                throw new UnsupportedOperationException();
            }
        }
        Log.e(TAG, "could not retrieve SPI service");
        return ClientLdr;
    }
}

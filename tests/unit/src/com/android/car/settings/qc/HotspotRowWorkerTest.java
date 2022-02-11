/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.car.settings.qc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.net.TetheringManager;
import android.net.wifi.SoftApInfo;
import android.net.wifi.WifiClient;
import android.net.wifi.WifiManager;
import android.os.HandlerExecutor;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;

@RunWith(AndroidJUnit4.class)
public class HotspotRowWorkerTest {
    private final Context mContext = spy(ApplicationProvider.getApplicationContext());
    private HotspotRowWorker mWorker;

    @Mock
    private WifiManager mWifiManager;
    @Mock
    private TetheringManager mTetheringManager;
    @Mock
    private HotspotRow mQCItem;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(mContext.getSystemService(WifiManager.class)).thenReturn(mWifiManager);
        when(mContext.getSystemService(TetheringManager.class)).thenReturn(mTetheringManager);
        mWorker = new HotspotRowWorker(mContext, SettingsQCRegistry.HOTSPOT_ROW_URI);
        mWorker.setQCItem(mQCItem);
    }

    @Test
    public void onSubscribe_registersCallbacks() {
        mWorker.onQCItemSubscribe();
        verify(mTetheringManager).registerTetheringEventCallback(any(HandlerExecutor.class),
                any(TetheringManager.TetheringEventCallback.class));
        verify(mWifiManager).registerSoftApCallback(any(Executor.class),
                any(WifiManager.SoftApCallback.class));
    }

    @Test
    public void onUnsubscribe_unregistersCallbacks() {
        ArgumentCaptor<TetheringManager.TetheringEventCallback> tetheringEventCaptor =
                ArgumentCaptor.forClass(TetheringManager.TetheringEventCallback.class);
        ArgumentCaptor<WifiManager.SoftApCallback> softApCaptor = ArgumentCaptor.forClass(
                WifiManager.SoftApCallback.class);
        mWorker.onQCItemSubscribe();
        verify(mTetheringManager).registerTetheringEventCallback(any(HandlerExecutor.class),
                tetheringEventCaptor.capture());
        verify(mWifiManager).registerSoftApCallback(any(Executor.class),
                softApCaptor.capture());

        mWorker.onQCItemUnsubscribe();
        verify(mTetheringManager).unregisterTetheringEventCallback(tetheringEventCaptor.getValue());
        verify(mWifiManager).unregisterSoftApCallback(softApCaptor.getValue());
    }

    @Test
    public void onTetheringSupported_updatesQCItem() {
        when(mQCItem.getHotspotSupported()).thenReturn(true);
        ArgumentCaptor<TetheringManager.TetheringEventCallback> tetheringEventCaptor =
                ArgumentCaptor.forClass(TetheringManager.TetheringEventCallback.class);
        mWorker.onQCItemSubscribe();
        verify(mTetheringManager).registerTetheringEventCallback(any(HandlerExecutor.class),
                tetheringEventCaptor.capture());

        TetheringManager.TetheringEventCallback callback = tetheringEventCaptor.getValue();
        callback.onTetheringSupported(false);
        verify(mQCItem).setHotspotSupported(false);
    }

    @Test
    public void onConnectedClientsChanged_updatesQCItem() {
        ArgumentCaptor<WifiManager.SoftApCallback> softApCaptor = ArgumentCaptor.forClass(
                WifiManager.SoftApCallback.class);
        mWorker.onQCItemSubscribe();
        verify(mWifiManager).registerSoftApCallback(any(Executor.class),
                softApCaptor.capture());

        WifiManager.SoftApCallback callback = softApCaptor.getValue();
        SoftApInfo info = mock(SoftApInfo.class);
        List<WifiClient> clients = Collections.singletonList(mock(WifiClient.class));
        callback.onConnectedClientsChanged(info, clients);
        verify(mQCItem).setConnectedDevicesCount(1);
    }
}

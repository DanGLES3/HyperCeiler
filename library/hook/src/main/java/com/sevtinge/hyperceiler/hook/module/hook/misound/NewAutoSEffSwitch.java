/*
 * This file is part of HyperCeiler.

 * HyperCeiler is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.

 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.

 * Copyright (C) 2023-2025 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.hook.module.hook.misound;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.provider.Settings;

import com.hchen.hooktool.HCBase;
import com.hchen.hooktool.utils.SystemPropTool;
import com.sevtinge.hyperceiler.hook.IEffectInfo;

import org.luckypray.dexkit.DexKitBridge;

/**
 * 新版连接耳机自动切换原声
 * 
 * @author 焕晨HChen
 */
public class NewAutoSEffSwitch extends HCBase {
    public static final String TAG = "NewAutoSEffSwitch";
    private Context mContext;
    public static DexKitBridge mDexKit;
    public static AudioManager mAudioManager;
    private static IEffectInfo mIEffectInfo;
    private NewFWAudioEffectControl mNewFWAudioEffectControl = null;
    private NewAudioEffectControl mNewAudioEffectControl = null;

    @Override
    public void init() {
        if (isSupportFW()) {
            mNewFWAudioEffectControl = new NewFWAudioEffectControl();
            mNewFWAudioEffectControl.init();
        } else {
            mNewAudioEffectControl = new NewAudioEffectControl();
            mNewAudioEffectControl.init();
        }
    }

    public static boolean isSupportFW() {
        return SystemPropTool.getProp("ro.vendor.audio.fweffect", false);
    }

    @Override
    protected void onApplication(Context context) {
        mContext = context;

        Intent intent = mContext.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (intent == null) return;

        try {
            Bundle bundle = intent.getBundleExtra("effect_info");
            if (bundle == null) {
                logW(TAG, "onApplication: 'effect_info' bundle is null.");
                return;
            }

            IBinder binder = bundle.getBinder("effect_info");
            if (binder != null) {
                mIEffectInfo = IEffectInfo.Stub.asInterface(binder);
                logI(TAG, "onApplication: EffectInfoService: " + mIEffectInfo);
            } else {
                logW(TAG, "onApplication: Binder 'effect_info' is null.");
            }

        } catch (Exception e) {
            logE(TAG, "Exception retrieving effect_info binder from intent", e);
            return;
        }

        if (mIEffectInfo == null) return;

        if (mNewFWAudioEffectControl != null) {
            mNewFWAudioEffectControl.mIEffectInfo = mIEffectInfo;
        } else if (mNewAudioEffectControl != null) {
            mNewAudioEffectControl.mIEffectInfo = mIEffectInfo;
        }

        mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);

        mContext.getContentResolver().registerContentObserver(
            Settings.Global.getUriFor("auto_effect_switch_earphone_state"),
            false,
            new ContentObserver(new Handler(mContext.getMainLooper())) {
                @Override
                public void onChange(boolean selfChange) {
                    if (selfChange) return;
                    try {
                        int result = Settings.Global.getInt(mContext.getContentResolver(), "auto_effect_switch_earphone_state", 0);
                        logI(TAG, "settings observer earphone state change to: " + result);

                        if (mNewFWAudioEffectControl != null) {
                            mNewFWAudioEffectControl.updateEffectSelectionState();
                        } else if (mNewAudioEffectControl != null) {
                            mNewAudioEffectControl.updateEffectSelectionState();
                        }
                    } catch (Exception e) {
                        logE(TAG, "Exception in ContentObserver onChange", e);
                    }
                }
            }
        );

        // If this method uses reflection internally, better to wrap it in try/catch
        try {
            callStaticMethod("android.media.audiofx.AudioEffectCenter", "getInstance", mContext);
        } catch (Exception e) {
            logE(TAG, "Failed to call AudioEffectCenter.getInstance", e);
        }
    }

    public static boolean getEarPhoneStateFinal() {
        if (mIEffectInfo != null) {
            try {
                return mIEffectInfo.isEarphoneConnection();
            } catch (RemoteException e) {
                logE(TAG, e);
                return false;
            }
        }
        logW(TAG, "getEarPhoneStateFinal: mIEffectInfo is null!!");
        return false;
    }
}

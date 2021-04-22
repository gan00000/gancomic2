/*
 * Copyright 2016 Hippo Seven
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

package com.ccsky.sfish.ui.scene;

import android.content.Context;
import android.widget.Toast;
import androidx.annotation.StringRes;

import com.ccsky.sfish.client.SkyClient;
import com.ccsky.sfish.SkyApplication;
import com.ccsky.sfish.ui.SkyMainActivity;
import com.ccsky.scene.SceneFragment;
import com.ccsky.scene.StageActivity;

public abstract class EhCallback<E extends SceneFragment, T> implements SkyClient.Callback<T> {

    private final SkyApplication mApplication;
    private final int mStageId;
    private final String mSceneTag;

    public EhCallback(Context context, int stageId, String sceneTag) {
        mApplication = (SkyApplication) context.getApplicationContext();
        mStageId = stageId;
        mSceneTag = sceneTag;
    }

    public abstract boolean isInstance(SceneFragment scene);

    public Context getContent() {
        Context context = getStageActivity();
        if (context == null) {
            context = getApplication();
        }
        return context;
    }

    public SkyApplication getApplication() {
        return mApplication;
    }

    public StageActivity getStageActivity() {
        return mApplication.findStageActivityById(mStageId);
    }

    @SuppressWarnings("unchecked")
    public E getScene() {
        StageActivity stage = mApplication.findStageActivityById(mStageId);
        if (stage == null) {
            return null;
        }
        SceneFragment scene = stage.findSceneByTag(mSceneTag);
        if (isInstance(scene)) {
            return (E) scene;
        } else {
            return null;
        }
    }

    public void showTip(@StringRes int id, int length) {
        StageActivity activity = getStageActivity();
        if (activity instanceof SkyMainActivity) {
            ((SkyMainActivity) activity).showTip(id, length);
        } else {
            Toast.makeText(getApplication(), id,
                    length == SkyBaseScene.LENGTH_LONG ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT).show();
        }
    }

    public void showTip(String tip, int length) {
        StageActivity activity = getStageActivity();
        if (activity instanceof SkyMainActivity) {
            ((SkyMainActivity) activity).showTip(tip, length);
        } else {
            Toast.makeText(getApplication(), tip,
                    length == SkyBaseScene.LENGTH_LONG ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT).show();
        }
    }
}

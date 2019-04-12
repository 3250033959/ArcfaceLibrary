package com.chanzhu.arcfacelibrary.util;

import android.content.Context;
import android.widget.Toast;

import com.arcsoft.face.ErrorInfo;
import com.arcsoft.face.FaceEngine;
import com.chanzhu.arcfacelibrary.R;
import com.chanzhu.arcfacelibrary.common.Constants;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

/**
 * @ProjectName: bkd-android-ui
 * @Package: com.chanzhu.arcfacelibrary.util
 * @ClassName: EngineUtil
 * @Description: java类作用描述
 * @Author: zc
 * @CreateDate: 2019/4/10 10:02
 * @UpdateRemark: 更新说明
 * @Version: ${VERSION_NAME}
 */
public class EngineUtil {

    /**
     * 激活引擎
     *
     * @param context
     */
    public static void activeEngine(final Context context) {

        Observable.create(new ObservableOnSubscribe<Integer>() {
            @Override
            public void subscribe(ObservableEmitter<Integer> emitter) throws Exception {
                FaceEngine faceEngine = new FaceEngine();
                int activeCode = faceEngine.active(context, Constants.APP_ID, Constants.SDK_KEY);
                emitter.onNext(activeCode);
            }
        })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<Integer>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onNext(Integer activeCode) {
                        if (activeCode == ErrorInfo.MOK) {
                            Toast.makeText(context, context.getString(R.string.active_success), Toast.LENGTH_SHORT).show();
                        } else if (activeCode == ErrorInfo.MERR_ASF_ALREADY_ACTIVATED) {
                            Toast.makeText(context, context.getString(R.string.already_activated), Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(context, context.getString(R.string.active_failed, activeCode), Toast.LENGTH_SHORT).show();
                        }

                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onComplete() {

                    }
                });

    }

}

package com.hiroshi.cimoc.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.SparseArray;

import com.facebook.drawee.backends.pipeline.PipelineDraweeControllerBuilder;
import com.facebook.drawee.backends.pipeline.PipelineDraweeControllerBuilderSupplier;
import com.facebook.imagepipeline.backends.okhttp3.OkHttpImagePipelineConfigFactory;
import com.facebook.imagepipeline.core.ImagePipelineConfig;
import com.facebook.imagepipeline.core.ImagePipelineFactory;
import com.hiroshi.cimoc.core.Kami;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by Hiroshi on 2016/7/8.
 */
public class ControllerBuilderFactory {

    private static SparseArray<PipelineDraweeControllerBuilder> builderArray = new SparseArray<>();

    public static PipelineDraweeControllerBuilder getControllerBuilder(int source, Context context) {
        if (builderArray.get(source) == null) {
            ImagePipelineFactory factory;
            if (source == Kami.SOURCE_EHENTAI) {
                factory = buildFactory(context.getApplicationContext(), source, "igneous=583e748d60dc007822213a471d8e71dcba801b6a55cd0ffe04953e8adb63f294d4b60f303d9182b4276281ac883cec4c48a669db0b6c4914da78073945f49b12583e748d60dc007822213a471d8e71dcba801b6a55cd0ffe04953e8adb63f294d4b60f303d9182b4276281ac883cec4c48a669db0b6c4914da78073945f49b12");
            } else {
                factory = buildFactory(context.getApplicationContext(), source, null);
            }
            builderArray.put(source, new PipelineDraweeControllerBuilderSupplier(context.getApplicationContext(), factory).get());
        }
        return builderArray.get(source);
    }

    private static ImagePipelineFactory buildFactory(Context context, final int source, final String cookie) {
        OkHttpClient client = new OkHttpClient.Builder().addInterceptor(new Interceptor() {
            @Override
            public Response intercept(Chain chain) throws IOException {
                String referer = Kami.getReferer(source);
                Request.Builder request = chain.request().newBuilder();
                request.addHeader("Referer", referer);
                if (cookie != null) {
                    request.header("Cookie", cookie);
                }
                return chain.proceed(request.build());
            }
        }).build();
        ImagePipelineConfig config = OkHttpImagePipelineConfigFactory.newBuilder(context, client)
                .setBitmapsConfig(Bitmap.Config.RGB_565)
                .build();
        return new ImagePipelineFactory(config);
    }

}
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

package com.ccsky.sfish.client;

import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;

import androidx.annotation.Nullable;

import com.ccsky.network.StatusCodeException;
import com.ccsky.sfish.AppConfig;
import com.ccsky.sfish.GetText;
import com.ccsky.sfish.R;
import com.ccsky.sfish.Settings;
import com.ccsky.sfish.client.data.GalleryComment;
import com.ccsky.sfish.client.data.GalleryDetail;
import com.ccsky.sfish.client.data.GalleryInfo;
import com.ccsky.sfish.client.data.PreviewSet;
import com.ccsky.sfish.client.exception.CancelledException;
import com.ccsky.sfish.client.exception.NoHAtHClientException;
import com.ccsky.sfish.client.exception.ParseException;
import com.ccsky.sfish.client.exception.SkyException;
import com.ccsky.sfish.client.parser.ArchiveParser;
import com.ccsky.sfish.client.parser.FavoritesParser;
import com.ccsky.sfish.client.parser.ForumsParser;
import com.ccsky.sfish.client.parser.GalleryApiParser;
import com.ccsky.sfish.client.parser.GalleryDetailParser;
import com.ccsky.sfish.client.parser.GalleryListParser;
import com.ccsky.sfish.client.parser.GalleryPageApiParser;
import com.ccsky.sfish.client.parser.GalleryPageParser;
import com.ccsky.sfish.client.parser.GalleryTokenApiParser;
import com.ccsky.sfish.client.parser.ProfileParser;
import com.ccsky.sfish.client.parser.RateGalleryParser;
import com.ccsky.sfish.client.parser.SignInParser;
import com.ccsky.sfish.client.parser.TorrentParser;
import com.ccsky.sfish.client.parser.VoteCommentParser;
import com.ccsky.util.ExceptionUtils;
import com.hippo.yorozuya.AssertUtils;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.Call;
import okhttp3.FormBody;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class SkyEngine {

    private static final String TAG = SkyEngine.class.getSimpleName();

    private static final String SAD_PANDA_DISPOSITION = "inline; filename=\"sadpanda.jpg\"";
    private static final String SAD_PANDA_TYPE = "image/gif";
    private static final String SAD_PANDA_LENGTH = "9615";

    public static final MediaType MEDIA_TYPE_JSON = MediaType.parse("application/json; charset=utf-8");
    private static final MediaType MEDIA_TYPE_JPEG = MediaType.parse("image/jpeg");

    private static final Pattern PATTERN_NEED_HATH_CLIENT = Pattern.compile("(You must have a H@H client assigned to your account to use this feature\\.)");

    public static EhFilter sEhFilter;

    public static void initialize() {
        sEhFilter = EhFilter.getInstance();
    }

    private static void doThrowException(Call call, int code, @Nullable Headers headers,
            @Nullable String body, Throwable e) throws Throwable {
        if (call.isCanceled()) {
            throw new CancelledException();
        }

        // Check sad panda
        if (headers != null && SAD_PANDA_DISPOSITION.equals(headers.get("Content-Disposition")) &&
                SAD_PANDA_TYPE.equals(headers.get("Content-Type")) &&
                SAD_PANDA_LENGTH.equals(headers.get("Content-Length"))) {
            throw new SkyException("Sad Panda");
        }

        if (e instanceof ParseException) {
            if (body != null && !body.contains("<")){
                throw new SkyException(body);
            } else {
                if (Settings.getSaveParseErrorBody()) {
                    AppConfig.saveParseErrorBody((ParseException) e);
                }
                throw new SkyException(GetText.getString(R.string.error_parse_error));
            }
        }

        if (code >= 400) {
            throw new StatusCodeException(code);
        }

        if (e != null) {
            throw e;
        }
    }

    private static void throwException(Call call, int code, @Nullable Headers headers,
        @Nullable String body, Throwable e) throws Throwable {
        try {
            doThrowException(call, code, headers, body, e);
        } catch (Throwable error) {
            error.printStackTrace();
            throw error;
        }
    }

    public static String signIn(@Nullable SkyClient.Task task, OkHttpClient okHttpClient,
                                String username, String password, String recaptchaChallenge, String recaptchaResponse) throws Throwable {
        FormBody.Builder builder = new FormBody.Builder()
                .add("UserName", username)
                .add("PassWord", password)
                .add("submit", "Log me in")
                .add("CookieDate", "1")
                .add("temporary_https", "off");
        if (!TextUtils.isEmpty(recaptchaChallenge) && !TextUtils.isEmpty(recaptchaResponse)) {
            builder.add("recaptcha_challenge_field", recaptchaChallenge);
            builder.add("recaptcha_response_field", recaptchaResponse);
        }
        String url = SkyUrl.API_SIGN_IN;
        String referer = SkyUrl.API_DOMMAIN + "/index.php?act=Login&CODE=00";
        String origin = SkyUrl.API_DOMMAIN;
        Log.d(TAG, url);
        Request request = new SkyRequestBuilder(url, referer, origin)
                .post(builder.build())
                .build();
        Call call = okHttpClient.newCall(request);

        // Put call
        if (null != task) {
            task.setCall(call);
        }

        String body = null;
        Headers headers = null;
        int code = -1;
        try {
            Response response = call.execute();
            code = response.code();
            headers = response.headers();
            body = response.body().string();
            return SignInParser.parse(body);
        } catch (Throwable e) {
            ExceptionUtils.throwIfFatal(e);
            throwException(call, code, headers, body, e);
            throw e;
        }
    }

    private static void fillGalleryList(@Nullable SkyClient.Task task, OkHttpClient okHttpClient, List<GalleryInfo> list, String url, boolean filter) throws Throwable {
        // Filter title and uploader
        if (filter) {
            for (int i = 0, n = list.size(); i < n; i++) {
                GalleryInfo info = list.get(i);
                if (!sEhFilter.filterTitle(info) || !sEhFilter.filterUploader(info)) {
                    list.remove(i);
                    i--;
                    n--;
                }
            }
        }

        boolean hasTags = false;
        boolean hasPages = false;
        boolean hasRated = false;
        for (GalleryInfo gi : list) {
            if (gi.simpleTags != null) {
                hasTags = true;
            }
            if (gi.pages != 0) {
                hasPages = true;
            }
            if (gi.rated) {
                hasRated = true;
            }
        }

        boolean needApi = (filter && sEhFilter.needTags() && !hasTags) ||
                (Settings.getShowGalleryPages() && !hasPages) ||
                hasRated;
        if (needApi) {
            fillGalleryListByApi(task, okHttpClient, list, url);
        }

        // Filter tag
        if (filter) {
            for (int i = 0, n = list.size(); i < n; i++) {
                GalleryInfo info = list.get(i);
                if (!sEhFilter.filterTag(info) || !sEhFilter.filterTagNamespace(info)) {
                    list.remove(i);
                    i--;
                    n--;
                }
            }
        }

        for (GalleryInfo info : list) {
            info.thumb = SkyUrl.getFixedPreviewThumbUrl(info.thumb);
        }
    }

    public static GalleryListParser.Result getGalleryList(@Nullable SkyClient.Task task, OkHttpClient okHttpClient,
                                                          String url) throws Throwable {
        String referer = SkyUrl.getReferer();
        Log.d(TAG, url);
        Request request = new SkyRequestBuilder(url, referer).build();
        Call call = okHttpClient.newCall(request);

        // Put call
        if (null != task) {
            task.setCall(call);
        }

        String body = null;
        Headers headers = null;
        GalleryListParser.Result result;
        int code = -1;
        try {
            Response response = call.execute();
            code = response.code();
            headers = response.headers();
            body = response.body().string();
            result = GalleryListParser.parse(body);
        } catch (Throwable e) {
            ExceptionUtils.throwIfFatal(e);
            throwException(call, code, headers, body, e);
            throw e;
        }

        fillGalleryList(task, okHttpClient, result.galleryInfoList, url, true);

        return result;
    }

    // At least, GalleryInfo contain valid gid and token
    public static List<GalleryInfo> fillGalleryListByApi(@Nullable SkyClient.Task task, OkHttpClient okHttpClient,
                                                         List<GalleryInfo> galleryInfoList, String referer) throws Throwable {
        // We can only request 25 items com.ssract.one time at most
        final int MAX_REQUEST_SIZE = 25;
        List<GalleryInfo> requestItems = new ArrayList<>(MAX_REQUEST_SIZE);
        for (int i = 0, size = galleryInfoList.size(); i < size; i++) {
            requestItems.add(galleryInfoList.get(i));
            if (requestItems.size() == MAX_REQUEST_SIZE || i == size - 1) {
                doFillGalleryListByApi(task, okHttpClient, requestItems, referer);
                requestItems.clear();
            }
        }
        return galleryInfoList;
    }

    private static void doFillGalleryListByApi(@Nullable SkyClient.Task task, OkHttpClient okHttpClient,
                                               List<GalleryInfo> galleryInfoList, String referer) throws Throwable {
        JSONObject json = new JSONObject();
        json.put("method", "gdata");
        JSONArray ja = new JSONArray();
        for (int i = 0, size = galleryInfoList.size(); i < size; i++) {
            GalleryInfo gi = galleryInfoList.get(i);
            JSONArray g = new JSONArray();
            g.put(gi.gid);
            g.put(gi.token);
            ja.put(g);
        }
        json.put("gidlist", ja);
        json.put("namespace", 1);
        String url = SkyUrl.getApiUrl();
        String origin = SkyUrl.getOrigin();
        Log.d(TAG, url);
        Request request = new SkyRequestBuilder(url, referer, origin)
                .post(RequestBody.create(MEDIA_TYPE_JSON, json.toString()))
                .build();
        Call call = okHttpClient.newCall(request);

        // Put call
        if (null != task) {
            task.setCall(call);
        }

        String body = null;
        Headers headers = null;
        int code = -1;
        try {
            Response response = call.execute();
            code = response.code();
            headers = response.headers();
            body = response.body().string();
            GalleryApiParser.parse(body, galleryInfoList);
        } catch (Throwable e) {
            ExceptionUtils.throwIfFatal(e);
            throwException(call, code, headers, body, e);
            throw e;
        }
    }

    public static GalleryDetail getGalleryDetail(@Nullable SkyClient.Task task, OkHttpClient okHttpClient,
                                                 String url) throws Throwable {
        String referer = SkyUrl.getReferer();
        Log.d(TAG, url);
        Request request = new SkyRequestBuilder(url, referer).build();
        Call call = okHttpClient.newCall(request);

        // Put call
        if (null != task) {
            task.setCall(call);
        }

        String body = null;
        Headers headers = null;
        int code = -1;
        try {
            Response response = call.execute();
            code = response.code();
            headers = response.headers();
            body = response.body().string();
            return GalleryDetailParser.parse(body);
        } catch (Throwable e) {
            ExceptionUtils.throwIfFatal(e);
            throwException(call, code, headers, body, e);
            throw e;
        }
    }


    public static Pair<PreviewSet, Integer> getPreviewSet(
            @Nullable SkyClient.Task task, OkHttpClient okHttpClient, String url) throws Throwable {
        String referer = SkyUrl.getReferer();
        Log.d(TAG, url);
        Request request = new SkyRequestBuilder(url, referer).build();
        Call call = okHttpClient.newCall(request);

        // Put call
        if (null != task) {
            task.setCall(call);
        }

        String body = null;
        Headers headers = null;
        int code = -1;
        try {
            Response response = call.execute();
            code = response.code();
            headers = response.headers();
            body = response.body().string();
            return Pair.create(GalleryDetailParser.parsePreviewSet(body),
                    GalleryDetailParser.parsePreviewPages(body));
        } catch (Throwable e) {
            ExceptionUtils.throwIfFatal(e);
            throwException(call, code, headers, body, e);
            throw e;
        }
    }

    public static RateGalleryParser.Result rateGallery(@Nullable SkyClient.Task task,
                                                       OkHttpClient okHttpClient, long apiUid, String apiKey, long gid,
                                                       String token, float rating) throws Throwable {
        final JSONObject json = new JSONObject();
        json.put("method", "rategallery");
        json.put("apiuid", apiUid);
        json.put("apikey", apiKey);
        json.put("gid", gid);
        json.put("token", token);
        json.put("rating", (int) Math.ceil(rating * 2));
        final RequestBody requestBody = RequestBody.create(MEDIA_TYPE_JSON, json.toString());
        String url = SkyUrl.getApiUrl();
        String referer = SkyUrl.getGalleryDetailUrl(gid, token);
        String origin = SkyUrl.getOrigin();
        Log.d(TAG, url);
        Request request = new SkyRequestBuilder(url, referer, origin)
                .post(requestBody)
                .build();
        Call call = okHttpClient.newCall(request);

        // Put call
        if (null != task) {
            task.setCall(call);
        }

        String body = null;
        Headers headers = null;
        int code = -1;
        try {
            Response response = call.execute();
            code = response.code();
            headers = response.headers();
            body = response.body().string();
            return RateGalleryParser.parse(body);
        } catch (Throwable e) {
            ExceptionUtils.throwIfFatal(e);
            throwException(call, code, headers, body, e);
            throw e;
        }
    }

    public static GalleryComment[] commentGallery(@Nullable SkyClient.Task task,
                                                  OkHttpClient okHttpClient, String url, String comment) throws Throwable {
        FormBody.Builder builder = new FormBody.Builder()
                .add("commenttext_new", comment);
        String origin = SkyUrl.getOrigin();
        Log.d(TAG, url);
        Request request = new SkyRequestBuilder(url, url, origin)
                .post(builder.build())
                .build();
        Call call = okHttpClient.newCall(request);

        // Put call
        if (null != task) {
            task.setCall(call);
        }

        String body = null;
        Headers headers = null;
        int code = -1;
        try {
            Response response = call.execute();
            code = response.code();
            headers = response.headers();
            body = response.body().string();
            Document document = Jsoup.parse(body);

            Elements elements = document.select("#chd + p");
            if (elements.size() > 0) {
                throw new SkyException(elements.get(0).text());
            }

            return GalleryDetailParser.parseComments(document);
        } catch (Throwable e) {
            ExceptionUtils.throwIfFatal(e);
            throwException(call, code, headers, body, e);
            throw e;
        }
    }

    public static String getGalleryToken(@Nullable SkyClient.Task task, OkHttpClient okHttpClient,
                                         long gid, String gtoken, int page) throws Throwable {
        JSONObject json = new JSONObject()
                .put("method", "gtoken")
                .put("pagelist", new JSONArray().put(
                        new JSONArray().put(gid).put(gtoken).put(page + 1)));
        final RequestBody requestBody = RequestBody.create(MEDIA_TYPE_JSON, json.toString());
        String url = SkyUrl.getApiUrl();
        String referer = SkyUrl.getReferer();
        String origin = SkyUrl.getOrigin();
        Log.d(TAG, url);
        Request request = new SkyRequestBuilder(url, referer, origin)
                .post(requestBody)
                .build();
        Call call = okHttpClient.newCall(request);

        // Put call
        if (null != task) {
            task.setCall(call);
        }

        String body = null;
        Headers headers = null;
        int code = -1;
        try {
            Response response = call.execute();
            code = response.code();
            headers = response.headers();
            body = response.body().string();
            return GalleryTokenApiParser.parse(body);
        } catch (Throwable e) {
            ExceptionUtils.throwIfFatal(e);
            throwException(call, code, headers, body, e);
            throw e;
        }
    }

    public static FavoritesParser.Result getFavorites(@Nullable SkyClient.Task task, OkHttpClient okHttpClient,
                                                      String url, boolean callApi) throws Throwable {
        String referer = SkyUrl.getReferer();
        Log.d(TAG, url);
        Request request = new SkyRequestBuilder(url, referer).build();
        Call call = okHttpClient.newCall(request);

        // Put call
        if (null != task) {
            task.setCall(call);
        }

        String body = null;
        Headers headers = null;
        FavoritesParser.Result result;
        int code = -1;
        try {
            Response response = call.execute();
            code = response.code();
            headers = response.headers();
            body = response.body().string();
            result = FavoritesParser.parse(body);
        } catch (Throwable e) {
            ExceptionUtils.throwIfFatal(e);
            throwException(call, code, headers, body, e);
            throw e;
        }

        fillGalleryList(task, okHttpClient, result.galleryInfoList, url, false);

        return result;
    }

    /**
     * @param dstCat -1 for delete, 0 - 9 for cloud favorite, others throw Exception
     * @param note max 250 characters
     */
    public static Void addFavorites(@Nullable SkyClient.Task task, OkHttpClient okHttpClient,
                                    long gid, String token, int dstCat, String note) throws Throwable {
        String catStr;
        if (dstCat == -1) {
            catStr = "favdel";
        } else if (dstCat >= 0 && dstCat <= 9) {
            catStr = String.valueOf(dstCat);
        } else {
            throw new SkyException("Invalid dstCat: " + dstCat);
        }
        FormBody.Builder builder = new FormBody.Builder();
        builder.add("favcat", catStr);
        builder.add("favnote", note != null ? note : "");
        // submit=Add+to+Favorites is not necessary, just use submit=Apply+Changes all the time
        builder.add("submit", "Apply Changes");
        builder.add("update", "1");
        String url = SkyUrl.getAddFavorites(gid, token);
        String origin = SkyUrl.getOrigin();
        Log.d(TAG, url);
        Request request = new SkyRequestBuilder(url, url, origin)
                .post(builder.build())
                .build();
        Call call = okHttpClient.newCall(request);

        // Put call
        if (null != task) {
            task.setCall(call);
        }

        String body = null;
        Headers headers = null;
        int code = -1;
        try {
            Response response = call.execute();
            code = response.code();
            headers = response.headers();
            body = response.body().string();
            throwException(call, code, headers, body, null);
        } catch (Throwable e) {
            ExceptionUtils.throwIfFatal(e);
            throwException(call, code, headers, body, e);
            throw e;
        }

        return null;
    }

    public static Void addFavoritesRange(@Nullable SkyClient.Task task, OkHttpClient okHttpClient,
                                         long[] gidArray, String[] tokenArray, int dstCat) throws Throwable {
        AssertUtils.assertEquals(gidArray.length, tokenArray.length);
        for (int i = 0, n = gidArray.length; i < n; i++) {
            addFavorites(task, okHttpClient, gidArray[i], tokenArray[i], dstCat, null);
        }
        return null;
    }

    public static FavoritesParser.Result modifyFavorites(@Nullable SkyClient.Task task, OkHttpClient okHttpClient,
                                                         String url, long[] gidArray, int dstCat, boolean callApi) throws Throwable {
        String catStr;
        if (dstCat == -1) {
            catStr = "delete";
        } else if (dstCat >= 0 && dstCat <= 9) {
            catStr = "fav" + dstCat;
        } else {
            throw new SkyException("Invalid dstCat: " + dstCat);
        }
        FormBody.Builder builder = new FormBody.Builder();
        builder.add("ddact", catStr);
        for (long gid : gidArray) {
            builder.add("modifygids[]", Long.toString(gid));
        }
        builder.add("apply", "Apply");
        String origin = SkyUrl.getOrigin();
        Log.d(TAG, url);
        Request request = new SkyRequestBuilder(url, url, origin)
                .post(builder.build())
                .build();
        Call call = okHttpClient.newCall(request);

        // Put call
        if (null != task) {
            task.setCall(call);
        }

        String body = null;
        Headers headers = null;
        FavoritesParser.Result result;
        int code = -1;
        try {
            Response response = call.execute();
            code = response.code();
            headers = response.headers();
            body = response.body().string();
            result = FavoritesParser.parse(body);
        } catch (Throwable e) {
            ExceptionUtils.throwIfFatal(e);
            throwException(call, code, headers, body, e);
            throw e;
        }

        fillGalleryList(task, okHttpClient, result.galleryInfoList, url, false);

        return result;
    }

    public static Pair<String, String>[] getTorrentList(@Nullable SkyClient.Task task, OkHttpClient okHttpClient,
                                                        String url, long gid, String token) throws Throwable {
        String referer = SkyUrl.getGalleryDetailUrl(gid, token);
        Log.d(TAG, url);
        Request request = new SkyRequestBuilder(url, referer).build();
        Call call = okHttpClient.newCall(request);

        // Put call
        if (null != task) {
            task.setCall(call);
        }

        String body = null;
        Headers headers = null;
        Pair<String, String>[] result;
        int code = -1;
        try {
            Response response = call.execute();
            code = response.code();
            headers = response.headers();
            body = response.body().string();
            result = TorrentParser.parse(body);
        } catch (Throwable e) {
            ExceptionUtils.throwIfFatal(e);
            throwException(call, code, headers, body, e);
            throw e;
        }

        return result;
    }

    public static Pair<String, Pair<String, String>[]> getArchiveList(@Nullable SkyClient.Task task, OkHttpClient okHttpClient,
                                                                      String url, long gid, String token) throws Throwable {
        String referer = SkyUrl.getGalleryDetailUrl(gid, token);
        Log.d(TAG, url);
        Request request = new SkyRequestBuilder(url, referer).build();
        Call call = okHttpClient.newCall(request);

        // Put call
        if (null != task) {
            task.setCall(call);
        }

        String body = null;
        Headers headers = null;
        Pair<String, Pair<String, String>[]> result;
        int code = -1;
        try {
            Response response = call.execute();
            code = response.code();
            headers = response.headers();
            body = response.body().string();
            result = ArchiveParser.parse(body);
        } catch (Throwable e) {
            ExceptionUtils.throwIfFatal(e);
            throwException(call, code, headers, body, e);
            throw e;
        }

        return result;
    }

    public static Void downloadArchive(@Nullable SkyClient.Task task, OkHttpClient okHttpClient,
                                       long gid, String token, String or, String res) throws Throwable {
        if (or == null || or.length() == 0) {
            throw new SkyException("Invalid form param or: " + or);
        }
        if (res == null || res.length() == 0) {
            throw new SkyException("Invalid res: " + res);
        }
        FormBody.Builder builder = new FormBody.Builder();
        builder.add("hathdl_xres", res);
        String url = SkyUrl.getDownloadArchive(gid, token, or);
        String referer = SkyUrl.getGalleryDetailUrl(gid, token);
        String origin = SkyUrl.getOrigin();
        Log.d(TAG, url);
        Request request = new SkyRequestBuilder(url, referer, origin)
                .post(builder.build())
                .build();
        Call call = okHttpClient.newCall(request);

        // Put call
        if (null != task) {
            task.setCall(call);
        }

        String body = null;
        Headers headers = null;
        int code = -1;
        try {
            Response response = call.execute();
            code = response.code();
            headers = response.headers();
            body = response.body().string();
            throwException(call, code, headers, body, null);
        } catch (Throwable e) {
            ExceptionUtils.throwIfFatal(e);
            throwException(call, code, headers, body, e);
            throw e;
        }

        Matcher m = PATTERN_NEED_HATH_CLIENT.matcher(body);
        if (m.find()) {
            throw new NoHAtHClientException("No H@H client");
        }

        return null;
    }

    private static ProfileParser.Result getProfileInternal(@Nullable SkyClient.Task task,
                                                           OkHttpClient okHttpClient, String url, String referer) throws Throwable {
        Log.d(TAG, url);
        Request request = new SkyRequestBuilder(url, referer).build();
        Call call = okHttpClient.newCall(request);

        // Put call
        if (null != task) {
            task.setCall(call);
        }

        String body = null;
        Headers headers = null;
        int code = -1;
        try {
            Response response = call.execute();
            code = response.code();
            headers = response.headers();
            body = response.body().string();
            return ProfileParser.parse(body);
        } catch (Throwable e) {
            ExceptionUtils.throwIfFatal(e);
            throwException(call, code, headers, body, e);
            throw e;
        }
    }

    public static ProfileParser.Result getProfile(@Nullable SkyClient.Task task,
            OkHttpClient okHttpClient) throws Throwable {
        String url = SkyUrl.URL_FORUMS;
        Log.d(TAG, url);
        Request request = new SkyRequestBuilder(url, null).build();
        Call call = okHttpClient.newCall(request);

        // Put call
        if (null != task) {
            task.setCall(call);
        }

        String body = null;
        Headers headers = null;
        int code = -1;
        try {
            Response response = call.execute();
            code = response.code();
            headers = response.headers();
            body = response.body().string();
            return getProfileInternal(task, okHttpClient, ForumsParser.parse(body), url);
        } catch (Throwable e) {
            ExceptionUtils.throwIfFatal(e);
            throwException(call, code, headers, body, e);
            throw e;
        }
    }

    public static VoteCommentParser.Result voteComment(@Nullable SkyClient.Task task, OkHttpClient okHttpClient,
                                                       long apiUid, String apiKey, long gid, String token, long commentId, int commentVote) throws Throwable {
        final JSONObject json = new JSONObject();
        json.put("method", "votecomment");
        json.put("apiuid", apiUid);
        json.put("apikey", apiKey);
        json.put("gid", gid);
        json.put("token", token);
        json.put("comment_id", commentId);
        json.put("comment_vote", commentVote);
        final RequestBody requestBody = RequestBody.create(MEDIA_TYPE_JSON, json.toString());
        String url = SkyUrl.getApiUrl();
        String referer = SkyUrl.getReferer();
        String origin = SkyUrl.getOrigin();
        Log.d(TAG, url);
        Request request = new SkyRequestBuilder(url, referer, origin)
                .post(requestBody)
                .build();
        Call call = okHttpClient.newCall(request);

        // Put call
        if (null != task) {
            task.setCall(call);
        }

        String body = null;
        Headers headers = null;
        int code = -1;
        try {
            Response response = call.execute();
            code = response.code();
            headers = response.headers();
            body = response.body().string();
            return VoteCommentParser.parse(body, commentVote);
        } catch (Throwable e) {
            ExceptionUtils.throwIfFatal(e);
            throwException(call, code, headers, body, e);
            throw e;
        }
    }

    /**
     * @param image Must be jpeg
     */
    public static GalleryListParser.Result imageSearch(@Nullable SkyClient.Task task, OkHttpClient okHttpClient,
                                                       File image, boolean uss, boolean osc, boolean se) throws Throwable {
        MultipartBody.Builder builder = new MultipartBody.Builder();
        builder.setType(MultipartBody.FORM);
        builder.addPart(
                Headers.of("Content-Disposition", "form-data; name=\"sfile\"; filename=\"a.jpg\""),
                RequestBody.create(MEDIA_TYPE_JPEG, image)
        );
        if (uss) {
            builder.addPart(
                    Headers.of("Content-Disposition", "form-data; name=\"fs_similar\""),
                    RequestBody.create(null, "on")
            );
        }
        if (osc) {
            builder.addPart(
                    Headers.of("Content-Disposition", "form-data; name=\"fs_covers\""),
                    RequestBody.create(null, "on")
            );
        }
        if (se) {
            builder.addPart(
                    Headers.of("Content-Disposition", "form-data; name=\"fs_exp\""),
                    RequestBody.create(null, "on")
            );
        }
        builder.addPart(
                Headers.of("Content-Disposition", "form-data; name=\"f_sfile\""),
                RequestBody.create(null, "File Search")
        );
        String url = SkyUrl.getImageSearchUrl();
        String referer = SkyUrl.getReferer();
        String origin = SkyUrl.getOrigin();
        Log.d(TAG, url);
        Request request = new SkyRequestBuilder(url, referer, origin)
                .post(builder.build())
                .build();
        Call call = okHttpClient.newCall(request);

        // Put call
        if (null != task) {
            task.setCall(call);
        }

        String body = null;
        Headers headers = null;
        GalleryListParser.Result result;
        int code = -1;
        try {
            Response response = call.execute();

            Log.d(TAG, "" + response.request().url().toString());

            code = response.code();
            headers = response.headers();
            body = response.body().string();
            result = GalleryListParser.parse(body);
        } catch (Throwable e) {
            ExceptionUtils.throwIfFatal(e);
            throwException(call, code, headers, body, e);
            throw e;
        }

        fillGalleryList(task, okHttpClient, result.galleryInfoList, url, true);

        return result;
    }

    public static GalleryPageParser.Result getGalleryPage(@Nullable SkyClient.Task task,
                                                          OkHttpClient okHttpClient, String url, long gid, String token) throws Throwable {
        String referer = SkyUrl.getGalleryDetailUrl(gid, token);
        Log.d(TAG, url);
        Request request = new SkyRequestBuilder(url, referer).build();
        Call call = okHttpClient.newCall(request);

        // Put call
        if (null != task) {
            task.setCall(call);
        }

        String body = null;
        Headers headers = null;
        int code = -1;
        try {
            Response response = call.execute();
            code = response.code();
            headers = response.headers();
            body = response.body().string();
            return GalleryPageParser.parse(body);
        } catch (Throwable e) {
            ExceptionUtils.throwIfFatal(e);
            throwException(call, code, headers, body, e);
            throw e;
        }
    }

    public static GalleryPageApiParser.Result getGalleryPageApi(@Nullable SkyClient.Task task,
            OkHttpClient okHttpClient, long gid, int index, String pToken, String showKey, String previousPToken) throws Throwable {
        final JSONObject json = new JSONObject();
        json.put("method", "showpage");
        json.put("gid", gid);
        json.put("page", index + 1);
        json.put("imgkey", pToken);
        json.put("showkey", showKey);
        final RequestBody requestBody = RequestBody.create(MEDIA_TYPE_JSON, json.toString());
        String url = SkyUrl.getApiUrl();
        String referer = null;
        if (index > 0 && previousPToken != null) {
            referer = SkyUrl.getPageUrl(gid, index - 1, previousPToken);
        }
        String origin = SkyUrl.getOrigin();
        Log.d(TAG, url);
        Request request = new SkyRequestBuilder(url, referer, origin)
            .post(requestBody)
            .build();
        Call call = okHttpClient.newCall(request);

        // Put call
        if (null != task) {
            task.setCall(call);
        }

        String body = null;
        Headers headers = null;
        int code = -1;
        try {
            Response response = call.execute();
            code = response.code();
            headers = response.headers();
            body = response.body().string();
            return GalleryPageApiParser.parse(body);
        } catch (Throwable e) {
            ExceptionUtils.throwIfFatal(e);
            throwException(call, code, headers, body, e);
            throw e;
        }
    }
}

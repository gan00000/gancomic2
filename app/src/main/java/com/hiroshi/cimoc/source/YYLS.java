package com.hiroshi.cimoc.source;

import android.util.Pair;

import com.hiroshi.cimoc.model.Chapter;
import com.hiroshi.cimoc.model.Comic;
import com.hiroshi.cimoc.model.ImageUrl;
import com.hiroshi.cimoc.model.Source;
import com.hiroshi.cimoc.parser.MangaCategory;
import com.hiroshi.cimoc.parser.MangaParser;
import com.hiroshi.cimoc.parser.NodeIterator;
import com.hiroshi.cimoc.parser.SearchIterator;
import com.hiroshi.cimoc.soup.Node;
import com.hiroshi.cimoc.utils.StringUtils;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.Headers;
import okhttp3.Request;

public class YYLS extends MangaParser {

    public static final int TYPE = 9;
    public static final String DEFAULT_TITLE = "YYLS";

    private String _cid = "";
    private String Baseurl = "http://8comic.se/";

    public YYLS(Source source) {
        init(source, new Category());
    }

    public static Source getDefaultSource() {
        return new Source(null, DEFAULT_TITLE, TYPE, true);
    }

    @Override
    public Request getSearchRequest(String keyword, int page) throws UnsupportedEncodingException {
        String url = "";
        if (page == 1)
            url = "http://8comic.se/%e6%90%9c%e5%b0%8b%e7%b5%90%e6%9e%9c/?w=" + keyword;
        return new Request.Builder()
//                .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 7.0;) Chrome/58.0.3029.110 Mobile")
                .url(url)
                .build();
    }

    @Override
    public SearchIterator getSearchIterator(String html, int page) {
        Node body = new Node(html);
        return new NodeIterator(body.list("#content #dp-widget-posts-2 > ul > li")) {
            @Override
            protected Comic parse(Node node) {
                String cid = node.href("a");
                String title = node.text("a");
                String cover = null;
                String update = "";
                String author = null;
                return new Comic(TYPE, cid, title, cover, update, author);
            }
        };
    }

    @Override
    public Request getInfoRequest(String cid) {
        String url = "http://8comic.se".concat(cid);
        _cid = cid;
        return new Request.Builder().url(url).build();
    }

    @Override
    public void parseInfo(String html, Comic comic) throws UnsupportedEncodingException {
        Node body = new Node(html);
        String title = body.text("#main > div > div.entry-header.cf > div > h1");
        String cover = body.src("#details > div.entry-content.rich-content > table > tbody > tr:nth-child(1) > td:nth-child(1) > img");
        String update = body.getLastChild("div.entry-content.rich-content a").text().trim();
        String author = "";
        String intro = body.text("#details > div.entry-content.rich-content > table > tbody > tr:nth-child(1) > td:nth-child(2) > p");
        Matcher matcher = Pattern.compile("????????????").matcher(html);
        boolean status = matcher.find();
        comic.setInfo(title, cover, update, intro, author, status);
    }

    @Override
    public List<Chapter> parseChapter(String html) {
        List<Chapter> list = new LinkedList<>();
        for (Node node : new Node(html).list("div.entry-content.rich-content a")) {
            String title = node.text();
//            title = Pattern.compile("[^0-9.]").matcher(title).replaceAll("");
            String path = node.href();
            path = path.substring(17, path.length() - 1);
            list.add(new Chapter(title, path));
        }
        Collections.reverse(list);
        return list;
    }

    @Override
    public Request getImagesRequest(String cid, String path) {
        path = Baseurl.concat(path);
        return new Request.Builder().addHeader("Referer", StringUtils.format("http://8comic.se%s", _cid)).url(path).build();
    }


    @Override
    public List<ImageUrl> parseImages(String html) {
        List<ImageUrl> list = new LinkedList<>();
        Matcher pageMatcher = Pattern.compile("id=.*?caonima.*?src=\"(.*?)\\d{3}\\.jpg").matcher(html);
        if (!pageMatcher.find()) return null;
        Matcher NumMatcher = Pattern.compile("???([\\d]*?)???").matcher(html);
        if (!NumMatcher.find()) return null;
        int page = Integer.parseInt(NumMatcher.group(1));
        for (int i = 1; i <= page; ++i) {
            list.add(new ImageUrl(i, StringUtils.format("%s//%03d.jpg", pageMatcher.group(1), i), false));
        }
        return list;
    }

    @Override
    public Request getCheckRequest(String cid) {
        return getInfoRequest(cid);
    }

    @Override
    public String parseCheck(String html) {
        return new Node(html).getLastChild("div.entry-content.rich-content a").text().trim();
    }

    @Override
    public List<Comic> parseCategory(String html, int page) {
        List<Comic> list = new ArrayList<>();
        Node body = new Node(html);
        for (Node node : body.list(".nag.cf > div")) {
            String cid = node.href(".thumb > a");
            cid = StringUtils.match("http://8comic.se(/\\d+)/", cid, 1);
            String title = node.attr(".thumb > a", "title");
            String cover = node.src(".thumb > a img");
            String update = "";
            list.add(new Comic(TYPE, cid, title, cover, update, null));
        }
        return list;
    }

    private static class Category extends MangaCategory {


        @Override
        public String getFormat(String... args) {
            return StringUtils.format("http://8comic.se/category/????????????/%s/page/%%d/",
                    args[CATEGORY_SUBJECT]);
        }

        @Override
        protected List<Pair<String, String>> getSubject() {
            List<Pair<String, String>> list = new ArrayList<>();
            list.add(Pair.create("????????????", "????????????"));
            list.add(Pair.create("????????????", "????????????"));
            list.add(Pair.create("??????", "??????"));
            list.add(Pair.create("??????", "??????"));
            list.add(Pair.create("??????", "??????"));
            list.add(Pair.create("??????", "??????"));
            list.add(Pair.create("??????", "??????"));
            list.add(Pair.create("??????", "??????"));
            list.add(Pair.create("??????", "??????"));
            list.add(Pair.create("??????", "??????"));
            list.add(Pair.create("??????", "??????"));
            list.add(Pair.create("??????", "??????"));
            list.add(Pair.create("??????", "??????"));
            list.add(Pair.create("??????", "??????"));
            list.add(Pair.create("??????", "??????"));
            return list;
        }

    }

    @Override
    public Headers getHeader() {
        return Headers.of("Referer", "http://8comic.se".concat(_cid));
    }


}



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

package com.ccsky.sfish.client.parser;

import com.ccsky.sfish.client.exception.ParseException;
import com.ccsky.sfish.client.SkyUrl;
import com.ccsky.util.ExceptionUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public class ForumsParser {

    public static String parse(String body) throws ParseException {
        try {
            Document d = Jsoup.parse(body, SkyUrl.URL_FORUMS);
            Element userlinks = d.getElementById("userlinks");
            Element child = userlinks.child(0).child(0).child(0);
            return child.attr("href");
        } catch (Throwable e) {
            ExceptionUtils.throwIfFatal(e);
            throw new ParseException("Parse forums error", body);
        }
    }
}

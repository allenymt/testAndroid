package com.vdian.android.lib.testforgradle.jsonparser;


import com.vdian.android.lib.testforgradle.jsonparser.jsonstyle.JsonObject;
import com.vdian.android.lib.testforgradle.jsonparser.parser.Parser;
import com.vdian.android.lib.testforgradle.jsonparser.tokenizer.ReaderChar;
import com.vdian.android.lib.testforgradle.jsonparser.tokenizer.TokenList;
import com.vdian.android.lib.testforgradle.jsonparser.tokenizer.Tokenizer;

import java.io.IOException;
import java.io.StringReader;

/**
 * @author yebin
 * @version 1.0
 * @className Client
 * @description 测试类
 * @date 2019/4/1 16:45
 **/
public class JsonParser {

    public static void testJsonParser() throws IOException {
        Tokenizer tokenizer = new Tokenizer();

        Parser parser = new Parser();
        String json = "{" +
                "\"hello\":\"112344\"," +
                "\"world\":\"21231231\"" +
                "}";
        ReaderChar charReader = new ReaderChar(new StringReader(json));
        TokenList tokens = tokenizer.getTokenStream(charReader);
        JsonObject o = (JsonObject)parser.parse(tokens);
        System.out.println(o.get("hello"));
        System.out.println(o.toString());
    }
}

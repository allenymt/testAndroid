package com.vdian.android.lib.testforgradle.jsonparser.tokenizer;

import java.util.ArrayList;
import java.util.List;

/**
 * @author yebin
 * @version 1.0
 * @className TokenList
 * @description 存储解析流
 * @date 2019/4/1 16:30
 **/
public class TokenList {
    private List<Token> tokens = new ArrayList<Token>();
    private int index = 0;

    public void add(Token token) {
        tokens.add(token);
    }

    public Token peek() {
        return index < tokens.size() ? tokens.get(index) : null;
    }

    public Token peekPrevious() {
        return index - 1 < 0 ? null : tokens.get(index - 2);
    }

    public Token next() {
        return tokens.get(index++);
    }

    public boolean hasMore() {
        return index < tokens.size();
    }

    @Override
    public String toString() {
        return "TokenList{" +
                "tokens=" + tokens +
                '}';
    }
}
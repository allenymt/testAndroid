package com.vdian.android.lib.testforgradle.jsonparser.tokenizer;

/**
 * @author yebin
 * @version 1.0
 * @className Token
 * @description 存储对应字符串
 * @date 2019/4/1 16:25
 **/
public class Token {
    private TokenTypeEnum tokenType;
    private String value;

    public Token(TokenTypeEnum tokenType, String value) {
        this.tokenType = tokenType;
        this.value = value;
    }

    public TokenTypeEnum getTokenType() {
        return tokenType;
    }

    public void setTokenType(TokenTypeEnum tokenType) {
        this.tokenType = tokenType;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "Token{" +
                "tokenType=" + tokenType +
                ", value='" + value + '\'' +
                '}';
    }
}

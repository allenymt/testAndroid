package com.vdian.android.lib.testforgradle.jsonparser.tokenizer;

/**
 * @author yebin
 */

public enum TokenTypeEnum {
    /**object开始的编号**/
    BEGIN_OBJECT(1),
    /**object结束的编号**/
    END_OBJECT(2),
    /**数组开始的编号**/
    BEGIN_ARRAY(4),
    /**数组结束的编号**/
    END_ARRAY(8),
    /**null的编号**/
    NULL(16),
    /**数字的编号**/
    NUMBER(32),
    /**string的编号**/
    STRING(64),
    /**Boolean的编号**/
    BOOLEAN(128),
    /**每个类型的编号**/
    SEP_COLON(256),
    /**编号**/
    SEP_COMMA(512),
    /**文档编号**/
    END_DOCUMENT(1024);
    /**每个类型的编号**/
    private int code;

    TokenTypeEnum(int code) {
        this.code = code;
    }

    public int getTokenCode() {
        return code;
    }
}

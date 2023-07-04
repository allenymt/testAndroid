package com.vdian.android.lib.testforgradle.jsonparser.fastjson;

import java.io.Serializable;

/**
 * @author yulun
 * @sinice 2020-03-27 17:58
 */
public class FastJsonInnerTest implements  Serializable{

    /**
     * 是否官方
     */
    public boolean editor;

    /**
     * 店铺id，一般通uerid
     */
    public String shopId;

    public String authorId;

    public String authorLogo;

    public String authorName;

    public String nameLogoBucket;//头像昵称分桶  0-店铺， 1-用户

    public String shopAddress;

    public String shopLocation;
    public String shopLocationIcon;
    public String shopFlagUrl;

    public String authorDesc;// 2019年10月28日新增，用户描述，仅部分场景出
    public long totalFeedCount;// 2019年10月28日新增，用户总的动态数，仅部分场景出

    public int authorType; //0 卖家，1 微店用户(这个用户有可能也是卖家，但在这里，做为普通的微店用户来处理)

    public long fansCount;//粉丝数

    /**
     * 开通分成推广状态，true-开通，false-关闭状态
     */
    public boolean openIncomeStatus;

    /**
     * 是否在直播
     */
    public boolean live;

    /**
     * 直播间url
     */
    public String liveUrl;

    /**
     * 是否置顶 1-未置顶，2-置顶
     */
    public int userTop;

    /**
     * 是否是创作者达人
     */
    public TalentInfo talentInfo;

    public boolean isSellerCard() {
        return authorType == 0;
    }

    public boolean isBuyerCard() {
        return authorType == 1;
    }

    /**
     * 认证
     */
    public  class TalentInfo implements Serializable {
        public boolean ifTalent;//是否是创作者达人
        public String info;
    }

}



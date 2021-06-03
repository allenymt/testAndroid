package com.vdian.android.lib.testforgradle.proxy;

/**
 * @author yulun
 * @sinice 2020-03-12 20:51
 */
public interface UserManager {
    public void addUser(ResultCallBack<?> callBack);

    public void delUser(String userId);

    public String findUser(String userId);

    public void modifyUser(String userId, String userName);
}

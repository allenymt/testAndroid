package com.vdian.android.lib.testforgradle.proxy;

/**
 * @author yulun
 * @sinice 2020-03-12 20:53
 */
public class UserManagerImpl implements UserManager {

//    @Override
//    public void addUser(String userId, String userName) {
//        System.out.println("UserManagerImpl.addUser");
//    }

    @Override
    public void addUser(ResultCallBack<?> callBack) {

    }

    @Override
    public void delUser(String userId) {
        System.out.println("UserManagerImpl.delUser");
    }

    @Override
    public String findUser(String userId) {
        System.out.println("UserManagerImpl.findUser");
        return "张三";
    }

    @Override
    public void modifyUser(String userId, String userName) {
        System.out.println("UserManagerImpl.modifyUser");

    }
}

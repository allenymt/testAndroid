package com.vdian.android.lib.testforgradle.generic;

import java.util.ArrayList;
import java.util.List;

/**
 * @author yulun
 * @sinice 2021-09-14 12:19
 */
class TestPesc {
    public static void testPesc() {
        //通配符 具体泛型 object
        List<Object> list1 = new ArrayList<>();
        list1.add("hello");
        list1.add(0);

        List<String> list2 = new ArrayList<>();
        list2.add("world");

        List<? super Number> list3 = new ArrayList();
        Integer a = Integer.valueOf(1);
        list3.add(a);

        Double b = Double.valueOf(1.0);
        list3.add(b);

        Object number = list3.get(0);
        list3.add(null);

        GenericTest<Boolean> c = new GenericTest();
        try {
            c.setTestParam(true);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    // List<? extends Apple>，意味着元素都是Apple的子类，但子类可以有多个
    // 关键是编译器并不知道具体是哪个子类，所以代码里add都编译报错
    // 但在取数据的时候，编译器能确定的是原生肯定是Apple或者Fruit
    public void testCoVariance(List<? extends Apple> apples) {
        Apple b = new Apple();
        AppleLite c = new AppleLite();
//        apples.add(b); // 编译出错，这里感觉不合理，本身应该可以插入
//        apples.add(c); // 编译出错
        Apple a = apples.get(0);
        Fruit d = apples.get(1);
    }

    // List<? super Apple>，意味着元素都是Apple的父类，而前面有说过
    // 子类是可以赋值给父类的，所以add Apple 和 AppleLite不会报错
    // 取数据时报错是因为，父类不能赋值给子类，虽然Fruit看起来是根，但在java里
    // 根是Object，取值Object是没错的
    public void testContraVariance(List<? super Apple> apples) {
        Apple b = new Apple();
        AppleLite c = new AppleLite();
        apples.add(b);
        apples.add(c);
//        Fruit a = apples.get(0); // 编译报错
        Object d = apples.get(0); // 编译通过
    }

    public static class Fruit {
    }

    public static class Apple extends Fruit {
    }

    public class AppleLite extends Apple {
    }

    /**参数使用List<? extends Fruit>**/
//    public static void getOutFruits(List<Fruit> basket){ //如果用这一行，会有编译错误
    public static void getOutFruits(List<? extends Fruit> basket){
        for (Fruit fruit : basket) {
            System.out.println(fruit);
            //...do something other
        }
    }
    public static void main(String[] args) {
        List<Fruit> fruitBasket = new ArrayList<>();
        fruitBasket.add(new Fruit());
        getOutFruits(fruitBasket);

        List<Apple> appleBasket = new ArrayList<>();
        appleBasket.add(new Apple());
        getOutFruits(appleBasket);//编译正确
    }

}

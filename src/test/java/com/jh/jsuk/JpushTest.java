package com.jh.jsuk;

import com.jh.jsuk.envm.UserType;
import com.jh.jsuk.utils.JPushUtils;
import org.junit.Test;

public class JpushTest {




    @Test
    public void test() throws Exception {

//        JPushUtils.push(UserType.SHOP,248,"sdfasd少时诵诗呜呜呜呜书fasdfasdf","asdfasdfasdf");
//        JPushUtils.push(UserType.USER,281,"sdfasdfasss收拾收拾是是是s收拾收拾搜索sdfasdf","asdfasdfasdf");

        JPushUtils.push(UserType.DISTRIBUTION,250,"----------------你撒22222222啊啊啊","asdfasdfasdf");


    }

}

package com.luban.springmvc;

import java.math.BigDecimal;
import java.util.Date;

/**
 * @date 2020/10/21 20:12
 * @author chengjiaqing
 * @version : 0.1
 */


public class Book {


    private String bookname;
    private Date birthday;

    public String getBookname() {
        return bookname;
    }

    public void setBookname(String bookname) {
        this.bookname = bookname;
    }

    public Date getBirthday() {
        return birthday;
    }

    public void setBirthday(Date birthday) {
        this.birthday = birthday;
    }

}
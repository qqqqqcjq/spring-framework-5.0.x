package com.luban.springioc;
import javax.servlet.ServletException;

public class Test {

    public static void main(String[] args) {
        try {
            SpringApplicationLuban.run();
        } catch (ServletException e) {
            e.printStackTrace();
        }
    }
}

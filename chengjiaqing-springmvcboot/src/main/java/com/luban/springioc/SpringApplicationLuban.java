package com.luban.springioc;

import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.WebResourceRoot;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.webresources.DirResourceSet;
import org.apache.catalina.webresources.StandardRoot;

import javax.servlet.ServletException;
import java.io.File;

public class SpringApplicationLuban {
    public static  void run() throws ServletException {
        Tomcat tomcat = new Tomcat();
        tomcat.setPort(8080);

        String sourcePath = SpringApplicationLuban.class.getResource("/").getPath();
        System.out.println(sourcePath);
        System.out.println(new File("chengjiaqing-springmvcboot/src/main/webapp").getAbsolutePath());
        //告訴tomcat你的源碼在哪裏
        Context ctx = tomcat.addWebapp("/chengjiaqing-springmvcboot",new File("chengjiaqing-springmvcboot/src/main/webapp").getAbsolutePath());
        WebResourceRoot resources = new StandardRoot(ctx);
        resources.addPreResources(new DirResourceSet(resources, "/WEB-INF",sourcePath, "/"));
        ctx.setResources(resources);
        try {
            tomcat.start();
            tomcat.getServer().await();
        } catch (LifecycleException e) {
            e.printStackTrace();
        }

    }
}

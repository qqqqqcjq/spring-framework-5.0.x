package testSpringEL;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.FileNotFoundException;

/**
 * @date 2020/10/12 23:35
 * @author chengjiaqing
 * @version : 0.1
 */


@Component
public class LoginController {

    @Value("#{1}")
    private int number; //获取数字 1

    @Value("#{'Spring Expression Language'}") //获取字符串常量
    private String str;

    @Value("#{dataSource.url}") //获取bean的属性
    private String jdbcUrl;

    @Value("#{dataSource}")
    private DataSource dataSource;

    public int getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    public String getStr() {
        return str;
    }

    public void setStr(String str) {
        this.str = str;
    }

    public String getJdbcUrl() {
        return jdbcUrl;
    }

    public void setJdbcUrl(String jdbcUrl) {
        this.jdbcUrl = jdbcUrl;
    }
}
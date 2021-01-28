package testSpringPlaceholder;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

@Component
@PropertySource(value = {"classpath:db.properties"})
public class DataSource {

    /**
     * 驱动类
     */
    @Value("${driveClass}")
    private String driveClass;

    /**
     * jdbc地址
     */
    @Value("${url}")
    private String url;

    /**
     * 用户名
     */
    @Value("${userName}")
    private String userName;

    /**
     * 密码
     */
    @Value("${password}")
    private String password;

    public String getDriveClass() {
        return driveClass;
    }

    public void setDriveClass(String driveClass) {
        this.driveClass = driveClass;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public String toString() {
        return "DataSource [driveClass=" + driveClass + ", url=" + url + ", userName=" + userName + ", password=" + password + "]";
    }

}
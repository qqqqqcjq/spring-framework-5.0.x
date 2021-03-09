package testPropertyEditor;

import java.util.Date;

/**
 * @date 2020/3/14 19:58
 * @author chengjiaqing
 * @version : 0.1
 */ 
 
  
public class UserManager {
    private Date dateValue;
    public void setDateValue(Date dateValue) {
        this.dateValue = dateValue;
    }
    public Date getDateValue() {
        return dateValue;
    }
}
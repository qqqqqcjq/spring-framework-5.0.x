package dto;

import org.springframework.stereotype.Component;

@Component
public class UserEntity {


    private Integer id;
    private String name;

    public UserEntity() {
    }

    public UserEntity(String name) {
        this.name = name;
    }

    public UserEntity(int  id) {
        this.id = id;
    }

    public UserEntity(int id, String name) {
        this.name = name;
        this.id = id;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}

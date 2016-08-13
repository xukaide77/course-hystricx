package com.raycloud.test.hystricx;

import java.io.Serializable;

/**
 * Created by liumingjian on 16/8/12.
 */
public class User implements Serializable{

    private Long id;

    private String name;

    private Integer age;

    private Integer sex;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public Integer getSex() {
        return sex;
    }

    public void setSex(Integer sex) {
        this.sex = sex;
    }

    @Override
    public String toString() {
        return String.format("user{id=%s,name=%s,age=%s,sex=%s}", id, name, age, sex);
    }
}

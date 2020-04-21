package ink.akira.mybatis.domain;

import java.util.Date;

public class Student {
    private long id;
    private int age;
    private String userName;
    private Date birth;

    public Student(long id, int age, String userName, Date birth) {
        this.id = id;
        this.age = age;
        this.userName = userName;
        this.birth = birth;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public Date getBirth() {
        return birth;
    }

    public void setBirth(Date birth) {
        this.birth = birth;
    }
}

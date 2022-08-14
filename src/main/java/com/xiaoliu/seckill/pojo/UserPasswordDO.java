package com.xiaoliu.seckill.pojo;

import javax.persistence.*;

@Table(name = "user_password")
public class UserPasswordDO {
    @Id
    private Integer id;

    @Column(name = "encrpt_password")
    private String encrptPassword;

    @Column(name = "user_id")
    private Integer userId;

    /**
     * @return id
     */
    public Integer getId() {
        return id;
    }

    /**
     * @param id
     */
    public void setId(Integer id) {
        this.id = id;
    }

    /**
     * @return encrpt_password
     */
    public String getEncrptPassword() {
        return encrptPassword;
    }

    /**
     * @param encrptPassword
     */
    public void setEncrptPassword(String encrptPassword) {
        this.encrptPassword = encrptPassword;
    }

    /**
     * @return user_id
     */
    public Integer getUserId() {
        return userId;
    }

    /**
     * @param userId
     */
    public void setUserId(Integer userId) {
        this.userId = userId;
    }
}
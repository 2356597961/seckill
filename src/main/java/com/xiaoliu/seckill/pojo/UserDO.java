package com.xiaoliu.seckill.pojo;

import javax.persistence.*;

@Table(name = "user_info")
public class UserDO {
    @Id
    private Integer id;

    private String name;

    /**
     * //1代表男性，2代表女性
     */
    private Byte gender;

    private Integer age;

    private String telphone;

    /**
     * //byphone,bywechat,byalipay
     */
    @Column(name = "register_mode")
    private String registerMode;

    @Column(name = "third_party_id")
    private String thirdPartyId;

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
     * @return name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * 获取//1代表男性，2代表女性
     *
     * @return gender - //1代表男性，2代表女性
     */
    public Byte getGender() {
        return gender;
    }

    /**
     * 设置//1代表男性，2代表女性
     *
     * @param gender //1代表男性，2代表女性
     */
    public void setGender(Byte gender) {
        this.gender = gender;
    }

    /**
     * @return age
     */
    public Integer getAge() {
        return age;
    }

    /**
     * @param age
     */
    public void setAge(Integer age) {
        this.age = age;
    }

    /**
     * @return telphone
     */
    public String getTelphone() {
        return telphone;
    }

    /**
     * @param telphone
     */
    public void setTelphone(String telphone) {
        this.telphone = telphone;
    }

    /**
     * 获取//byphone,bywechat,byalipay
     *
     * @return register_mode - //byphone,bywechat,byalipay
     */
    public String getRegisterMode() {
        return registerMode;
    }

    /**
     * 设置//byphone,bywechat,byalipay
     *
     * @param registerMode //byphone,bywechat,byalipay
     */
    public void setRegisterMode(String registerMode) {
        this.registerMode = registerMode;
    }

    /**
     * @return third_party_id
     */
    public String getThirdPartyId() {
        return thirdPartyId;
    }

    /**
     * @param thirdPartyId
     */
    public void setThirdPartyId(String thirdPartyId) {
        this.thirdPartyId = thirdPartyId;
    }
}
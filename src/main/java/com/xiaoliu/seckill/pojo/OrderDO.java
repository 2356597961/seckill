package com.xiaoliu.seckill.pojo;

import javax.persistence.*;

@Table(name = "order_info")
public class OrderDO {
    @Id
    private String id;

    @Column(name = "user_id")
    private Integer userId;

    @Column(name = "item_id")
    private Integer itemId;

    @Column(name = "item_price")
    private Double itemPrice;

    private Integer amount;

    @Column(name = "order_price")
    private Double orderPrice;

    @Column(name = "promo_id")
    private Integer promoId;

    /**
     * @return id
     */
    public String getId() {
        return id;
    }

    /**
     * @param id
     */
    public void setId(String id) {
        this.id = id;
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

    /**
     * @return item_id
     */
    public Integer getItemId() {
        return itemId;
    }

    /**
     * @param itemId
     */
    public void setItemId(Integer itemId) {
        this.itemId = itemId;
    }

    /**
     * @return item_price
     */
    public Double getItemPrice() {
        return itemPrice;
    }

    /**
     * @param itemPrice
     */
    public void setItemPrice(Double itemPrice) {
        this.itemPrice = itemPrice;
    }

    /**
     * @return amount
     */
    public Integer getAmount() {
        return amount;
    }

    /**
     * @param amount
     */
    public void setAmount(Integer amount) {
        this.amount = amount;
    }

    /**
     * @return order_price
     */
    public Double getOrderPrice() {
        return orderPrice;
    }

    /**
     * @param orderPrice
     */
    public void setOrderPrice(Double orderPrice) {
        this.orderPrice = orderPrice;
    }

    /**
     * @return promo_id
     */
    public Integer getPromoId() {
        return promoId;
    }

    /**
     * @param promoId
     */
    public void setPromoId(Integer promoId) {
        this.promoId = promoId;
    }
}
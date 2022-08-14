package com.xiaoliu.seckill.pojo;

import java.util.Date;
import javax.persistence.*;

@Table(name = "promo")
public class PromoDO {
    @Id
    private Integer id;

    @Column(name = "promo_name")
    private String promoName;

    @Column(name = "start_date")
    private Date startDate;

    @Column(name = "item_id")
    private Integer itemId;

    @Column(name = "promo_item_price")
    private Double promoItemPrice;

    @Column(name = "end_date")
    private Date endDate;

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
     * @return promo_name
     */
    public String getPromoName() {
        return promoName;
    }

    /**
     * @param promoName
     */
    public void setPromoName(String promoName) {
        this.promoName = promoName;
    }

    /**
     * @return start_date
     */
    public Date getStartDate() {
        return startDate;
    }

    /**
     * @param startDate
     */
    public void setStartDate(Date startDate) {
        this.startDate = startDate;
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
     * @return promo_item_price
     */
    public Double getPromoItemPrice() {
        return promoItemPrice;
    }

    /**
     * @param promoItemPrice
     */
    public void setPromoItemPrice(Double promoItemPrice) {
        this.promoItemPrice = promoItemPrice;
    }

    /**
     * @return end_date
     */
    public Date getEndDate() {
        return endDate;
    }

    /**
     * @param endDate
     */
    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }
}
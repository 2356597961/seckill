package com.xiaoliu.seckill.viewobject;

import java.util.Date;

public class PromoVO {


    private String promoName;


    private Date startDate;


    private Integer itemId;


    private Double promoItemPrice;


    private Date endDate;

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

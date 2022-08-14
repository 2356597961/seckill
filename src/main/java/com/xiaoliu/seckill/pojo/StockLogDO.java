package com.xiaoliu.seckill.pojo;

import javax.persistence.*;

@Table(name = "stock_log")
public class StockLogDO {
    @Id
    @Column(name = "stock_log_id")
    private String stockLogId;

    @Column(name = "item_id")
    private Integer itemId;

    private Integer amount;

    /**
     * //1表示初始状态，2表示下单扣减库存成功，3表示下单回滚
     */
    private Integer status;

    /**
     * @return stock_log_id
     */
    public String getStockLogId() {
        return stockLogId;
    }

    /**
     * @param stockLogId
     */
    public void setStockLogId(String stockLogId) {
        this.stockLogId = stockLogId;
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
     * 获取//1表示初始状态，2表示下单扣减库存成功，3表示下单回滚
     *
     * @return status - //1表示初始状态，2表示下单扣减库存成功，3表示下单回滚
     */
    public Integer getStatus() {
        return status;
    }

    /**
     * 设置//1表示初始状态，2表示下单扣减库存成功，3表示下单回滚
     *
     * @param status //1表示初始状态，2表示下单扣减库存成功，3表示下单回滚
     */
    public void setStatus(Integer status) {
        this.status = status;
    }
}
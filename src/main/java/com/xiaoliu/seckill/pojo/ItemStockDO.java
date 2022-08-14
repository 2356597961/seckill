package com.xiaoliu.seckill.pojo;

import javax.persistence.*;

@Table(name = "item_stock")
public class ItemStockDO {
    @Id
    private Integer id;

    private Integer stock;

    @Column(name = "item_id")
    private Integer itemId;

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
     * @return stock
     */
    public Integer getStock() {
        return stock;
    }

    /**
     * @param stock
     */
    public void setStock(Integer stock) {
        this.stock = stock;
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
}
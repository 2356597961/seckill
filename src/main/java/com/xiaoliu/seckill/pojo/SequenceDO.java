package com.xiaoliu.seckill.pojo;

import javax.persistence.*;

@Table(name = "sequence_info")
public class SequenceDO {
    @Id
    private String name;

    @Column(name = "current_value")
    private Integer currentValue;

    private Integer step;

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
     * @return current_value
     */
    public Integer getCurrentValue() {
        return currentValue;
    }

    /**
     * @param currentValue
     */
    public void setCurrentValue(Integer currentValue) {
        this.currentValue = currentValue;
    }

    /**
     * @return step
     */
    public Integer getStep() {
        return step;
    }

    /**
     * @param step
     */
    public void setStep(Integer step) {
        this.step = step;
    }
}
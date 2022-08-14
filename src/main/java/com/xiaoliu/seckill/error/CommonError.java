package com.xiaoliu.seckill.error;

/**
 * Created by xiaoliu on 2022/4/18.
 */
public interface CommonError {
    public int getErrCode();
    public String getErrMsg();
    public CommonError setErrMsg(String errMsg);


}

package com.xiaoliu.seckill.page;

import com.xiaoliu.seckill.service.ItemService;
import com.xiaoliu.seckill.service.model.ItemModel;
import com.xiaoliu.seckill.viewobject.PromoVO;
import freemarker.template.Configuration;
import freemarker.template.Template;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;

import javax.annotation.Resource;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Created with IDEA
 *
 * @Auther:XIAOLIU
 * @Date: 2022/08/11/17:26
 * @Description:
 */
@Component
public class HtmlService {

    @Value("${htmlPath}")
    private String htmlPath;

    @Autowired
    private Configuration configuration;

    @Resource
    private ItemService itemService;


    /****
     * 生成静态页
     * 1.模板名称   templateName
     * 2.数据模型-填充模板-------->Map<String,Object>
     * 3.生成的静态页存储的路径 path
     * 4.生成的文件名字    name
     */
    public Boolean writerPage(Integer itemId, PromoVO promoVO) throws Exception {

        //模板名称
        String templateName = "items_1.ftl";
        //生成的静态页存储的路径
        String path = htmlPath;
        File file = new File(path);
        if(!file.exists()){
            file.mkdirs();
        }

        //生成的文件名字
        String name = itemId+".html";

        ItemModel itemModel = itemService.getItemById(itemId);
        //数据模型
        Map<String,Object> dataMap = new HashMap<String,Object>();

        dataMap.put("sku",itemModel);
        if (promoVO!=null) {
            dataMap.put("startDate", promoVO.getStartDate());
            dataMap.put("endDate", promoVO.getEndDate());
            dataMap.put("deprecate", promoVO.getPromoItemPrice());
        }else {
            throw new Exception();
        }
        //获取spec
        //根据模板名字获取模板对象
        Template template = configuration.getTemplate(templateName);
        //生成文件，并转成字符串
        String content = FreeMarkerTemplateUtils.processTemplateIntoString(template, dataMap);
        //输出到磁盘
        FileUtils.writeStringToFile(new File(path,name),content);
        return true;
    }

    public void deletePage(Integer itemId) throws Exception{
        File file = new File(itemId + ".html", htmlPath);
        if (file.exists()){
            file.delete();
        }
    }
}

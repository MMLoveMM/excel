package cn.psimm.excel;

import cn.psimm.excel.utils.DateUtils;
import cn.psimm.excel.utils.ExcelUtil;
import cn.psimm.excel.utils.HttpUtils;
import cn.psimm.excel.utils.JsonMapper;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class excel {

    public static void main(String[] args) throws Exception {
        String filePath = "/Users/pangpengjun/Downloads/20180606高层次人才完整名单.xlsx";

        String[] headerAttrs = {"name", "company"};

        List<Map<String, String>> datas = ExcelUtil.getListMapByHeader(filePath, headerAttrs, 0, 1, 0, 0);

        List<Map<String, Object>> isHaidians = Lists.newArrayList();
        List<Map<String, Object>> notHaidians = Lists.newArrayList();

        if(!datas.isEmpty()) {
            String url = "https://editor.olmap.cn/spacialUtil/poi";
            Map<String, Object> params = Maps.newHashMap();
            params.put("eId", "siptea");
            params.put("cUserId", "15297ff0-59a4-11e8-a337-7f5921d6cae6");
            params.put("offset", "1");
            params.put("city", "beijing");
            params.put("toCoordType", "bd09");
            datas.forEach(data -> {
                List<String> companys = replaceStr(data.get("company"));
                if(!companys.isEmpty()) {
                    companys.forEach(company -> {
                        params.put("keywords", company.replaceAll("[^0-9\\u4e00-\\u9fa5a-zA-Z()（）]", ""));
                        Map poiData = JsonMapper.getInstance().fromJson(
                                HttpUtils.doGet(url, params, Maps.newHashMap()), Map.class
                        );
                        if(null != poiData && (boolean) poiData.get("success")) {
                            Map newData = (Map) poiData.get("data");
                            if(null != newData.get("pois")) {
                                List<Map> pois = (List<Map>) newData.get("pois");
                                if (null != pois && !pois.isEmpty()) {
                                    pois.forEach(poi -> {
                                        if("海淀区".equals(poi.get("adname"))) {
                                            isHaidians.add(getMap(data, company, poi));
                                        }else {
                                            notHaidians.add(getMap(data, company, poi));
                                        }
                                    });
                                }
                            }
                        }
                    });
                }
            });
        }

        String path = "/Users/pangpengjun/Downloads";
        String[] header = {"姓名", "公司", "地区"};
        String[] keys = {"name", "company", "adname"};
        ExcelUtil.setListMapToExcel(path, "海淀区数据", "海淀区", DateUtils.getDate(), header, keys, isHaidians);
        ExcelUtil.setListMapToExcel(path, "非海淀区数据", "非海淀区", DateUtils.getDate(), header, keys, notHaidians);

        System.out.println(isHaidians);
        System.out.println(notHaidians);

    }

    private static List<String> replaceStr(String str) {
        if(null == str && StringUtils.isEmpty(str))
            return new ArrayList<>();

        str = str.replaceAll(" ",",");
        str = str.replaceAll("\t",",");
        str = str.replaceAll(",+", ",");

        return Arrays.asList(str.split(","));
    }

    private static Map<String, Object> getMap(Map<String, String> data, String company, Map<String, Object> poi) {
        Map<String, Object> map = Maps.newHashMap();
        map.put("name", data.get("name"));
        map.put("company", company);
        map.put("adname", poi.get("adname"));

        return map;
    }
}

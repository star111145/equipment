package com.swpu.equipment.dashboard.export;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

@Data
public class DashboardExcelData {
    
    @ExcelProperty("统计类型")
    private String statType;
    
    @ExcelProperty("设备名称")
    private String equipmentName;
    
    @ExcelProperty("设备编号")
    private String equipmentNumber;
    
    @ExcelProperty("设备类型")
    private String equipmentType;
    
    @ExcelProperty("数量")
    private Integer count;
    
    @ExcelProperty("时间")
    private String date;
    
    @ExcelProperty("备注")
    private String remark;
}

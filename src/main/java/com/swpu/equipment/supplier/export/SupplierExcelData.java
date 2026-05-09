package com.swpu.equipment.supplier.export;

import lombok.Data;

@Data
public class SupplierExcelData {
    private String supplierName;
    private String supplierContact;
    private String phone;
    private String email;
    private String address;
    private String description;
    private String createTime;
}

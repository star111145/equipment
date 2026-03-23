package com.swpu.equipment.supplier.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.swpu.equipment.supplier.entity.Supplier;

public interface SupplierService extends IService<Supplier> {
    IPage<Supplier> getSupplierPage(Page<Supplier> page, String keyword);
}

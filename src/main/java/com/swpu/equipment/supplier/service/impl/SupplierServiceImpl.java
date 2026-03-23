package com.swpu.equipment.supplier.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.swpu.equipment.supplier.entity.Supplier;
import com.swpu.equipment.supplier.mapper.SupplierMapper;
import com.swpu.equipment.supplier.service.SupplierService;
import org.springframework.stereotype.Service;

@Service
public class SupplierServiceImpl extends ServiceImpl<SupplierMapper, Supplier> implements SupplierService {

    @Override
    public IPage<Supplier> getSupplierPage(Page<Supplier> page, String keyword) {
        LambdaQueryWrapper<Supplier> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(Supplier::getSupplierName, keyword)
                    .or()
                    .like(Supplier::getSupplierContact, keyword)
                    .or()
                    .like(Supplier::getPhone, keyword);
        }
        wrapper.orderByDesc(Supplier::getCreateTime);
        return this.page(page, wrapper);
    }
}

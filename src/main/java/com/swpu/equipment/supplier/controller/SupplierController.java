package com.swpu.equipment.supplier.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.swpu.equipment.common.util.Result;
import com.swpu.equipment.supplier.entity.Supplier;
import com.swpu.equipment.supplier.service.SupplierService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/supplier")
public class SupplierController {

    @Autowired
    private SupplierService supplierService;

    /**
     * 获取供应商列表（分页）
     */
    @GetMapping("/list")
    @PreAuthorize("hasAuthority('admin')")
    public Result<IPage<Supplier>> getSupplierList(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String keyword) {
        Page<Supplier> page = new Page<>(current, size);
        IPage<Supplier> result = supplierService.getSupplierPage(page, keyword);
        return Result.success(result);
    }

    /**
     * 获取所有供应商列表
     */
    @GetMapping("/all")
    public Result<List<Supplier>> getAllSuppliers() {
        List<Supplier> list = supplierService.list();
        return Result.success(list);
    }

    /**
     * 获取供应商详情
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('admin')")
    public Result<Supplier> getSupplierDetail(@PathVariable Long id) {
        Supplier supplier = supplierService.getById(id);
        if (supplier == null) {
            return Result.error("供应商不存在");
        }
        return Result.success(supplier);
    }

    /**
     * 添加供应商
     */
    @PostMapping
    @PreAuthorize("hasAuthority('admin')")
    public Result<Void> addSupplier(@RequestBody Supplier supplier) {
        boolean success = supplierService.save(supplier);
        return success ? Result.success() : Result.error("添加失败");
    }

    /**
     * 更新供应商
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('admin')")
    public Result<Void> updateSupplier(@PathVariable Long id, @RequestBody Supplier supplier) {
        supplier.setId(id);
        boolean success = supplierService.updateById(supplier);
        return success ? Result.success() : Result.error("更新失败");
    }

    /**
     * 删除供应商
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('admin')")
    public Result<Void> deleteSupplier(@PathVariable Long id) {
        boolean success = supplierService.removeById(id);
        return success ? Result.success() : Result.error("删除失败");
    }

    /**
     * 批量删除供应商
     */
    @DeleteMapping("/batch")
    @PreAuthorize("hasAuthority('admin')")
    public Result<Void> batchDeleteSupplier(@RequestParam List<Long> ids) {
        boolean success = supplierService.removeByIds(ids);
        return success ? Result.success() : Result.error("删除失败");
    }
}

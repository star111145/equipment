package com.swpu.equipment.supplier.controller;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.swpu.equipment.common.util.Result;
import com.swpu.equipment.supplier.entity.Supplier;
import com.swpu.equipment.supplier.export.SupplierExcelData;
import com.swpu.equipment.supplier.service.SupplierService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

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

    @GetMapping("/export")
    @PreAuthorize("hasAuthority('admin')")
    public void exportSupplier(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "false") Boolean exportAll,
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            HttpServletResponse response) throws IOException {
        List<Supplier> supplierList = supplierService.list();
        
        if (keyword != null && !keyword.isEmpty()) {
            supplierList = supplierList.stream()
                .filter(s -> s.getSupplierName().contains(keyword) || 
                           (s.getSupplierContact() != null && s.getSupplierContact().contains(keyword)))
                .collect(Collectors.toList());
        }
        
        if (exportAll == null) {
            exportAll = false;
        }
        
        if (!exportAll) {
            int total = supplierList.size();
            int fromIndex = (current - 1) * size;
            int toIndex = Math.min(fromIndex + size, total);
            if (fromIndex < total) {
                supplierList = supplierList.subList(fromIndex, toIndex);
            } else {
                supplierList = List.of();
            }
        }
        
        List<SupplierExcelData> dataList = supplierList.stream().map(supplier -> {
            SupplierExcelData data = new SupplierExcelData();
            data.setSupplierName(supplier.getSupplierName());
            data.setSupplierContact(supplier.getSupplierContact());
            data.setPhone(supplier.getPhone());
            data.setEmail(supplier.getEmail());
            data.setAddress(supplier.getAddress());
            data.setDescription(supplier.getDescription());
            data.setCreateTime(supplier.getCreateTime() != null ? supplier.getCreateTime().toString() : "");
            return data;
        }).collect(Collectors.toList());
        
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String fileName = "供应商信息_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        response.setHeader("Content-Disposition", "attachment; filename=" + fileName + ".xlsx");
        EasyExcel.write(response.getOutputStream(), SupplierExcelData.class).sheet("供应商信息").doWrite(dataList);
    }
}

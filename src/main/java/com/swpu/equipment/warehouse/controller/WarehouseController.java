package com.swpu.equipment.warehouse.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.swpu.equipment.common.result.Result;
import com.swpu.equipment.equipment.entity.Equipment;
import com.swpu.equipment.equipment.service.EquipmentService;
import com.swpu.equipment.warehouse.entity.Warehouse;
import com.swpu.equipment.warehouse.entity.WarehouseRecord;
import com.swpu.equipment.warehouse.entity.WarehouseRecordVO;
import com.swpu.equipment.warehouse.entity.WarehouseVO;
import com.swpu.equipment.warehouse.service.WarehouseRecordService;
import com.swpu.equipment.warehouse.service.WarehouseService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/warehouse")
public class WarehouseController {

    @Autowired
    private WarehouseService warehouseService;

    @Autowired
    private WarehouseRecordService warehouseRecordService;

    @Autowired
    private EquipmentService equipmentService;

    /**
     * 获取仓库列表（分页）
     */
    @GetMapping("/list")
    @PreAuthorize("hasAuthority('admin')")
    public Result<IPage<WarehouseVO>> getWarehouseList(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String keyword) {
        Page<WarehouseVO> page = new Page<>(current, size);
        IPage<WarehouseVO> result = warehouseService.getWarehousePage(page, keyword);
        return Result.success(result);
    }

    /**
     * 获取所有仓库列表
     */
    @GetMapping("/all")
    public Result<List<Warehouse>> getAllWarehouses() {
        List<Warehouse> list = warehouseService.list();
        return Result.success(list);
    }


    /**
     * 获取管理员列表
     */
    @GetMapping("/managers")
    @PreAuthorize("hasAuthority('admin')")
    public Result<List<Map<String, Object>>> getManagers() {
        List<Map<String, Object>> list = warehouseService.getManagerOptions();
        return Result.success(list);
    }

    /**
     * 获取仓库详情
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('admin')")
    public Result<WarehouseVO> getWarehouseDetail(@PathVariable Long id) {
        WarehouseVO warehouse = warehouseService.getWarehousePage(new Page<>(1, 1), null).getRecords().stream()
                .filter(w -> w.getId().equals(id))
                .findFirst()
                .orElse(null);
        if (warehouse == null) {
            return Result.error("仓库不存在");
        }
        return Result.success(warehouse);
    }

    /**
     * 添加仓库
     */
    @PostMapping
    @PreAuthorize("hasAuthority('admin')")
    public Result<Void> addWarehouse(@RequestBody Warehouse warehouse) {
        boolean success = warehouseService.save(warehouse);
        return success ? Result.success() : Result.error("添加失败");
    }

    /**
     * 更新仓库
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('admin')")
    public Result<Void> updateWarehouse(@PathVariable Long id, @RequestBody Warehouse warehouse) {
        warehouse.setId(id);
        boolean success = warehouseService.updateById(warehouse);
        return success ? Result.success() : Result.error("更新失败");
    }

    /**
     * 删除仓库
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('admin')")
    public Result<Void> deleteWarehouse(@PathVariable Long id) {
        boolean success = warehouseService.removeById(id);
        return success ? Result.success() : Result.error("删除失败");
    }

    /**
     * 批量删除仓库
     */
    @DeleteMapping("/batch")
    @PreAuthorize("hasAuthority('admin')")
    public Result<Void> batchDeleteWarehouse(@RequestParam List<Long> ids) {
        boolean success = warehouseService.removeByIds(ids);
        return success ? Result.success() : Result.error("删除失败");
    }

    /**
     * 入库操作
     */
    @PostMapping("/in")
    @PreAuthorize("hasAuthority('admin')")
    public Result<Void> warehouseIn(@RequestBody WarehouseRecord record, HttpServletRequest request) {
        if (record.getEquipmentId() == null || record.getQuantity() == null || record.getQuantity() <= 0) {
            return Result.error("参数错误");
        }

        // 更新设备库存
        Equipment equipment = equipmentService.getById(record.getEquipmentId());
        if (equipment == null) {
            return Result.error("设备不存在");
        }
        equipment.setStockQuantity(equipment.getStockQuantity() + record.getQuantity());
        equipmentService.updateById(equipment);

        // 设置记录类型为入库
        record.setRecordType(1);
        warehouseRecordService.save(record);

        return Result.success();
    }

    /**
     * 出库操作
     */
    @PostMapping("/out")
    @PreAuthorize("hasAuthority('admin')")
    public Result<Void> warehouseOut(@RequestBody WarehouseRecord record) {
        if (record.getEquipmentId() == null || record.getQuantity() == null || record.getQuantity() <= 0) {
            return Result.error("参数错误");
        }

        // 更新设备库存
        Equipment equipment = equipmentService.getById(record.getEquipmentId());
        if (equipment == null) {
            return Result.error("设备不存在");
        }
        if (equipment.getStockQuantity() < record.getQuantity()) {
            return Result.error("库存不足");
        }
        equipment.setStockQuantity(equipment.getStockQuantity() - record.getQuantity());
        equipmentService.updateById(equipment);

        // 设置记录类型为出库
        record.setRecordType(2);
        warehouseRecordService.save(record);

        return Result.success();
    }

    /**
     * 获取出入库记录列表（分页）
     */
    @GetMapping("/record/list")
    @PreAuthorize("hasAuthority('admin')")
    public Result<IPage<WarehouseRecordVO>> getRecordList(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Long equipmentId,
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) Long supplierId,
            @RequestParam(required = false) Integer recordType,
            @RequestParam(required = false) String keyword) {
        Page<WarehouseRecordVO> page = new Page<>(current, size);
        IPage<WarehouseRecordVO> result = warehouseRecordService.getRecordPage(page, equipmentId, warehouseId, supplierId, recordType, keyword);
        return Result.success(result);
    }

    /**
     * 获取设备的出入库记录
     */
    @GetMapping("/record/equipment/{equipmentId}")
    @PreAuthorize("hasAuthority('admin')")
    public Result<List<WarehouseRecord>> getRecordsByEquipment(@PathVariable Long equipmentId) {
        LambdaQueryWrapper<WarehouseRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WarehouseRecord::getEquipmentId, equipmentId)
                .orderByDesc(WarehouseRecord::getCreateTime);
        List<WarehouseRecord> list = warehouseRecordService.list(wrapper);
        return Result.success(list);
    }
}

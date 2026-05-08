package com.swpu.equipment.equipment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.swpu.equipment.equipment.entity.Equipment;
import com.swpu.equipment.equipment.entity.EquipmentVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface EquipmentMapper extends BaseMapper<Equipment> {
    IPage<EquipmentVO> getEquipmentPage(Page<EquipmentVO> page, @Param("keyword") String keyword, @Param("equipmentType") String equipmentType);
    
    EquipmentVO getEquipmentById(@Param("id") Long id);
    
    List<String> getEquipmentTypes();
    
    /**
     * 获取设备的借用数量（已借出状态）
     */
    Long getBorrowQuantity(@Param("equipmentId") Long equipmentId);
    
    /**
     * 获取设备的报修数量（报修中状态）
     */
    Long getRepairQuantity(@Param("equipmentId") Long equipmentId);
    
    /**
     * 获取设备的维修数量（已维修状态）
     */
    Long getMaintenanceQuantity(@Param("equipmentId") Long equipmentId);
    
    /**
     * 查询设备的已借出数量（已同意的借用）
     */
    @Select("SELECT COALESCE(SUM(borrow_quantity), 0) " +
            "FROM equipment_borrow " +
            "WHERE equipment_id = #{equipmentId} " +
            "AND borrow_status = 1")
    Integer selectSumBorrowed(@Param("equipmentId") Long equipmentId);
    
    /**
     * 查询设备的已归还数量
     */
    @Select("SELECT COALESCE(SUM(er.return_quantity), 0) " +
            "FROM equipment_return er " +
            "WHERE er.equipment_id = #{equipmentId} " +
            "AND er.return_status = 1")
    Integer selectSumReturned(@Param("equipmentId") Long equipmentId);
    
    /**
     * 查询设备的已故障数量（已同意的报修）
     */
    @Select("SELECT COALESCE(SUM(er.repair_quantity), 0) " +
            "FROM equipment_repair er " +
            "WHERE er.equipment_id = #{equipmentId} " +
            "AND er.repair_status = 1 " +
            "AND er.audit_status = 1")
    Integer selectSumFaulty(@Param("equipmentId") Long equipmentId);
    
    /**
     * 查询设备是否有待处理的报修
     */
    @Select("SELECT COUNT(*) > 0 " +
            "FROM equipment_repair er " +
            "WHERE er.equipment_id = #{equipmentId} " +
            "AND er.repair_status = 0")
    Boolean selectHasPendingRepair(@Param("equipmentId") Long equipmentId);
    
    /**
     * 查询设备是否有有效的预约（在预约时间段内且已审核通过）
     */
    @Select("SELECT COUNT(*) > 0 " +
            "FROM equipment_reservation er " +
            "WHERE er.equipment_id = #{equipmentId} " +
            "AND er.reserve_status = 1 " +
            "AND DATE_ADD(er.reserve_time, INTERVAL er.reserve_duration HOUR) > NOW()")
    Boolean hasActiveReservation(@Param("equipmentId") Long equipmentId);
}

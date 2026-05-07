package com.swpu.equipment.lifecycle.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.swpu.equipment.lifecycle.entity.EquipmentReservation;
import com.swpu.equipment.lifecycle.entity.EquipmentReservationVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface EquipmentReservationMapper extends BaseMapper<EquipmentReservation> {
    
    IPage<EquipmentReservationVO> getPageList(Page<EquipmentReservationVO> page, 
                                              @Param("keyword") String keyword, 
                                              @Param("status") Integer status);
    
    IPage<EquipmentReservationVO> getPageListByUserId(Page<EquipmentReservationVO> page, 
                                                      @Param("userId") Long userId,
                                                      @Param("keyword") String keyword, 
                                                      @Param("status") Integer status);
    
    EquipmentReservationVO getReservationById(Long id);
    
    List<EquipmentReservationVO> getCalendarReservations(@Param("equipmentId") Long equipmentId,
                                                          @Param("start") LocalDateTime start,
                                                          @Param("end") LocalDateTime end);
}

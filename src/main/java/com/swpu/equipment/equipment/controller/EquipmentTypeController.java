package com.swpu.equipment.equipment.controller;

import com.swpu.equipment.common.util.Result;
import com.swpu.equipment.equipment.entity.EquipmentType;
import com.swpu.equipment.equipment.service.EquipmentTypeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/equipment-type")
public class EquipmentTypeController {

    @Autowired
    private EquipmentTypeService equipmentTypeService;

    @GetMapping("/all")
    public Result<List<EquipmentType>> getAllTypes() {
        List<EquipmentType> list = equipmentTypeService.list();
        return Result.success(list);
    }
}

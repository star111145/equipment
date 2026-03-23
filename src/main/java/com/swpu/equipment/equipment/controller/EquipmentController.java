package com.swpu.equipment.equipment.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.swpu.equipment.common.result.Result;
import com.swpu.equipment.equipment.entity.Equipment;
import com.swpu.equipment.equipment.entity.EquipmentVO;
import com.swpu.equipment.equipment.service.EquipmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;

@RestController
@RequestMapping("/equipment")
public class EquipmentController {

    @Autowired
    private EquipmentService equipmentService;
    @Value("${upload.base-dir:src/main/resources/static/uploads}")
    private String uploadBaseDir;

    @GetMapping("/list")
    public Result<Page<EquipmentVO>> getEquipmentList(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "") String equipmentType) {
        Page<EquipmentVO> page = new Page<>(current, size);
        Page<EquipmentVO> result = equipmentService.getEquipmentPage(page, keyword, equipmentType);
        return Result.success(result);
    }

    @GetMapping("/{id}/qrcode")
    public void getQrCode(@PathVariable Long id, HttpServletResponse response) {
        Equipment equipment = equipmentService.getById(id);
        if (equipment == null || equipment.getQrcodeUrl() == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        File file = new File(equipment.getQrcodeUrl());
        if (!file.exists()) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        response.setContentType("image/png");
        response.setHeader("Content-Disposition", "attachment; filename=" + equipment.getEquipmentNumber() + ".png");

        try (FileInputStream fis = new FileInputStream(file);
             OutputStream os = response.getOutputStream()) {
            byte[] buffer = new byte[1024];
            int len;
            while ((len = fis.read(buffer)) != -1) {
                os.write(buffer, 0, len);
            }
            os.flush();
        } catch (IOException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
    // 获取设备类型列表
    @GetMapping("/types")
    public Result<List<String>> getEquipmentTypes() {
        List<String> types = equipmentService.getEquipmentTypes();
        return Result.success(types);
    }

    @GetMapping("/{id}")
    public Result<EquipmentVO> getEquipmentById(@PathVariable Long id) {
        EquipmentVO equipment = equipmentService.getEquipmentById(id);
        if (equipment == null) {
            return Result.error("设备不存在");
        }
        return Result.success(equipment);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('admin')")
    public Result<String> updateEquipment(@PathVariable Long id, @RequestBody Equipment equipment) {
        equipment.setId(id);
        boolean updated = equipmentService.updateById(equipment);
        if (updated) {
            return Result.success("更新设备成功");
        } else {
            return Result.error("更新设备失败");
        }
    }

    @DeleteMapping("/batch")
    @PreAuthorize("hasAuthority('admin')")
    public Result<String> batchDelete(@RequestBody java.util.List<Long> equipmentIds) {
        if (equipmentIds == null || equipmentIds.isEmpty()) {
            return Result.error("请选择要删除的设备");
        }
        equipmentService.removeByIds(equipmentIds);
        return Result.success("批量删除成功");
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('admin')")
    public Result<String> deleteEquipment(@PathVariable Long id) {
        Equipment equipment = equipmentService.getById(id);
        boolean deleted = equipmentService.removeById(id);
        if (deleted) {
            if (equipment != null && equipment.getQrcodeUrl() != null) {
                File file = new File(equipment.getQrcodeUrl());
                if (file.exists()) {
                    file.delete();
                }
            }
            return Result.success("删除成功");
        } else {
            return Result.error("删除失败");
        }
    }

    @PostMapping("/upload-image")
    @PreAuthorize("hasAuthority('admin')")
    public Result<String> uploadImage(@RequestParam("file") MultipartFile file) {
        try {
            // 使用配置文件中的上传目录
            String uploadDir = Paths.get(uploadBaseDir, "equipment").toString();
            File dir = new File(uploadDir);
            System.out.println("上传目录: " + dir.getAbsolutePath());
            System.out.println("目录是否存在: " + dir.exists());
            if (!dir.exists()) {
                System.out.println("创建目录: " + dir.mkdirs());
            }

            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null) {
                return Result.error("文件名不能为空");
            }

            String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            String newFilename = System.currentTimeMillis() + extension;

            File uploadedFile = new File(dir, newFilename);
            System.out.println("上传文件: " + uploadedFile.getAbsolutePath());
            System.out.println("文件是否存在: " + uploadedFile.exists());
            // 复制文件到目标目录
            Files.copy(file.getInputStream(), uploadedFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            System.out.println("文件复制后是否存在: " + uploadedFile.exists());

            String imageUrl = "/uploads/equipment/" + newFilename;
            System.out.println("返回的 imageUrl: " + imageUrl);
            return Result.success(imageUrl);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("上传失败: " + e.getMessage());
        }
    }

    @GetMapping("/qrcode/{equipmentNumber}")
    public void getQrCodeByNumber(@PathVariable String equipmentNumber, HttpServletResponse response) {
        Equipment equipment = equipmentService.getOne(new LambdaQueryWrapper<Equipment>()
                .eq(Equipment::getEquipmentNumber, equipmentNumber));
        if (equipment == null || equipment.getQrcodeUrl() == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        File file = new File(equipment.getQrcodeUrl());
        if (!file.exists()) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        response.setContentType("image/png");
        response.setHeader("Content-Disposition", "attachment; filename=" + equipmentNumber + ".png");

        try (FileInputStream fis = new FileInputStream(file);
             OutputStream os = response.getOutputStream()) {
            byte[] buffer = new byte[1024];
            int len;
            while ((len = fis.read(buffer)) != -1) {
                os.write(buffer, 0, len);
            }
            os.flush();
        } catch (IOException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}

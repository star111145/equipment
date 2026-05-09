package com.swpu.equipment.equipment.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.swpu.equipment.common.util.Result;
import com.swpu.equipment.common.util.TokenUtil;
import com.swpu.equipment.common.util.QrCodeUtil;
import com.swpu.equipment.equipment.entity.Equipment;
import com.swpu.equipment.equipment.entity.EquipmentVO;
import com.swpu.equipment.equipment.export.EquipmentExcelData;
import com.swpu.equipment.equipment.service.EquipmentService;
import com.swpu.equipment.lifecycle.entity.EquipmentBorrow;
import com.swpu.equipment.lifecycle.service.EquipmentBorrowService;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.write.style.column.LongestMatchColumnWidthStyleStrategy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.File;

import java.io.IOException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;


import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Value;


@RestController
@RequestMapping("/equipment")
public class EquipmentController {

    @Autowired
    private EquipmentService equipmentService;
    @Autowired
    private EquipmentBorrowService equipmentBorrowService;
    @Autowired
    private TokenUtil tokenUtil;
    @Value("${upload.base-dir:src/main/resources/static/uploads}")
    private String uploadBaseDir;
    @Value("${mobile.base-url:http://localhost:5173}")
    private String mobileBaseDir;

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

        String fileName = equipment.getQrcodeUrl().substring(equipment.getQrcodeUrl().lastIndexOf("/") + 1);
        String qrCodeDir = QrCodeUtil.getQrCodeDir();
        File file = new File(qrCodeDir, fileName);
        if (!file.exists()) {
            System.out.println("二维码文件不存在: " + file.getAbsolutePath());
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        response.setContentType("image/png");
        response.setHeader("Content-Transfer-Encoding", "binary");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + equipment.getEquipmentNumber() + ".png\"");

        try {
            Files.copy(file.toPath(), response.getOutputStream());
            response.getOutputStream().flush();
        } catch (IOException e) {
            System.out.println("读取二维码文件失败: " + e.getMessage());
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

    @GetMapping("/number/{equipmentNumber}")
    public Result<EquipmentVO> getEquipmentByNumber(@PathVariable String equipmentNumber) {
        EquipmentVO equipment = equipmentService.getEquipmentByNumber(equipmentNumber);
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
            String uploadDir = Paths.get(uploadBaseDir, "equipment/info/").toString();
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

            String imageUrl = "/uploads/equipment/info/" + newFilename;
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

        String fileName = equipment.getQrcodeUrl().substring(equipment.getQrcodeUrl().lastIndexOf("/") + 1);
        String qrCodeDir = QrCodeUtil.getQrCodeDir();
        File file = new File(qrCodeDir, fileName);
        if (!file.exists()) {
            System.out.println("二维码文件不存在: " + file.getAbsolutePath());
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        response.setContentType("image/png");
        response.setHeader("Content-Transfer-Encoding", "binary");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + equipmentNumber + ".png\"");

        try {
            Files.copy(file.toPath(), response.getOutputStream());
            response.getOutputStream().flush();
        } catch (IOException e) {
            System.out.println("读取二维码文件失败: " + e.getMessage());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/{id}/regenerate-qrcode")
    public Result<String> regenerateQrCode(@PathVariable Long id) {
        Equipment equipment = equipmentService.getById(id);
        if (equipment == null) {
            return Result.error("设备不存在");
        }
        if (equipment.getEquipmentNumber() == null) {
            return Result.error("设备编号不能为空");
        }
        
        try {
            String qrcodeContent = mobileBaseDir + "/mobile/device?equipmentNumber=" + equipment.getEquipmentNumber();
            String qrcodeUrl = QrCodeUtil.generateQrCode(qrcodeContent, equipment.getEquipmentNumber());
            
            Equipment updateEquipment = new Equipment();
            updateEquipment.setId(equipment.getId());
            updateEquipment.setQrcodeUrl(qrcodeUrl);
            updateEquipment.setQrcodeContent(qrcodeContent);
            equipmentService.updateById(updateEquipment);
            
            return Result.success(qrcodeUrl);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("生成二维码失败: " + e.getMessage());
        }
    }

    @PostMapping("/upload-fault-image")
    public Result<String> uploadFaultImage(@RequestParam("file") MultipartFile file) {
        try {
            // 使用配置文件中的上传目录
            String uploadDir = Paths.get(uploadBaseDir, "equipment/fault/").toString();
            File dir = new File(uploadDir);
            System.out.println("故障图片上传目录: " + dir.getAbsolutePath());
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
            System.out.println("上传故障图片: " + uploadedFile.getAbsolutePath());
            System.out.println("文件是否存在: " + uploadedFile.exists());
            // 复制文件到目标目录
            Files.copy(file.getInputStream(), uploadedFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            System.out.println("文件复制后是否存在: " + uploadedFile.exists());

            String imageUrl = "/uploads/equipment/fault/" + newFilename;
            System.out.println("返回的 imageUrl: " + imageUrl);
            return Result.success(imageUrl);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("上传失败: " + e.getMessage());
        }
    }

    @GetMapping("/export")
    @PreAuthorize("hasAuthority('admin')")
    public ResponseEntity<byte[]> exportEquipment(
            @RequestParam(required = false) List<Long> equipmentIds,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String equipmentType,
            @RequestParam(defaultValue = "false") Boolean exportAll,
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size) {
        try {
            List<EquipmentExcelData> dataList;
            if (equipmentIds != null && !equipmentIds.isEmpty()) {
                dataList = equipmentService.exportSelectedEquipment(equipmentIds);
            } else {
                dataList = equipmentService.exportEquipmentList(keyword, equipmentType, exportAll, current, size);
            }
            
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            EasyExcel.write(baos, EquipmentExcelData.class)
                    .sheet("设备信息")
                    .registerWriteHandler(new LongestMatchColumnWidthStyleStrategy())
                    .doWrite(dataList);
            
            baos.flush();
            byte[] excelBytes = baos.toByteArray();
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", "设备信息_" + 
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + ".xlsx");
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(excelBytes);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }

    @PostMapping("/{id}/borrow")
    @PreAuthorize("hasAuthority('user')")
    public Result<Void> borrowEquipment(@PathVariable Long id, @RequestBody EquipmentBorrow borrow, HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        Long userId = tokenUtil.getUserIdFromToken(token);
        if (userId == null) {
            return Result.error("未登录");
        }

        Equipment equipment = equipmentService.getById(id);
        if (equipment == null) {
            return Result.error("设备不存在");
        }

        if (equipment.getAvailableQuantity() < borrow.getBorrowQuantity()) {
            return Result.error("可用数量不足");
        }

        // 创建借用记录
        EquipmentBorrow equipmentBorrow = new EquipmentBorrow();
        equipmentBorrow.setEquipmentId(id);
        equipmentBorrow.setEquipmentNumber(equipment.getEquipmentNumber());
        equipmentBorrow.setEquipmentName(equipment.getEquipmentName());
        equipmentBorrow.setEquipmentModel(equipment.getEquipmentModel());
        equipmentBorrow.setEquipmentTypeId(equipment.getEquipmentTypeId());
        equipmentBorrow.setEquipmentImage(equipment.getEquipmentImage());
        equipmentBorrow.setUserId(userId);
        equipmentBorrow.setRealName(borrow.getRealName());
        equipmentBorrow.setPhone(borrow.getPhone());
        equipmentBorrow.setBorrowQuantity(borrow.getBorrowQuantity());
        equipmentBorrow.setPurpose(borrow.getPurpose());
        equipmentBorrow.setBorrowTime(LocalDateTime.now());
        equipmentBorrow.setBorrowStatus(0); // 待审核
        equipmentBorrow.setAuditStatus(0); // 待审核

        equipmentBorrowService.save(equipmentBorrow);

        // 不立即扣减可用数量，等待审核通过后再扣减
        // equipment.setAvailableQuantity(equipment.getAvailableQuantity() - borrow.getQuantity());
        // equipmentService.updateById(equipment);

        return Result.success();
    }
}

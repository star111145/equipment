package com.swpu.equipment;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
//
//@MapperScan({
//        "com.swpu.equipment.equipment.repository",
//        "com.swpu.equipment.user.repository",
//      // 添加精细 repository 包路径
//} )
public class EquipmentApplication {

    public static void main(String[] args) {
        SpringApplication.run(EquipmentApplication.class, args);
    }

}

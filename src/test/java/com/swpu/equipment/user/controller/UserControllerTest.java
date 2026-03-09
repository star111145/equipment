package com.swpu.equipment.user.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 密码加密测试类：生成BCrypt加密后的密码，用于插入测试数据
 */
@SpringBootTest
class UserControllerTest {

    // 密码加密器（和项目中配置的一致）
   @Autowired
    private  PasswordEncoder passwordEncoder ;

    /**
     * 测试1：生成指定明文密码的BCrypt加密串（核心方法）
     * 用于替换INSERT语句中的密码字段
     */
    @Test
    void generateBcryptPassword() {
        // 1. 待加密的明文密码（测试用：123456）
        String rawPassword = "123456";

        // 2. 生成加密串（每次生成结果不同，但都能验证123456）
        String encodedPassword = passwordEncoder.encode(rawPassword);

        // 3. 打印加密结果（复制此结果到INSERT SQL中）
        System.out.println("明文密码：" + rawPassword);
        System.out.println("BCrypt加密后：" + encodedPassword);

        // 4. 验证加密串是否能匹配明文（可选，确保加密正确）
        boolean isMatch = passwordEncoder.matches(rawPassword, encodedPassword);
        System.out.println("密码匹配验证：" + (isMatch ? "成功" : "失败"));
    }

    /**
     * 测试2：批量生成多个用户的加密密码（适配你的3个测试用户）
     */
    @Test
    void batchGeneratePassword() {
        // 定义测试用户：用户名 + 明文密码
        String[][] testUsers = {
                {"admin", "123456"},       // 管理员
                {"teacher01", "123456"},   // 教师
                {"student01", "123456"}    // 学生
        };

        // 批量生成加密密码
        System.out.println("===== 批量生成加密密码 =====");
        for (String[] user : testUsers) {
            String username = user[0];
            String rawPwd = user[1];
            String encodedPwd = passwordEncoder.encode(rawPwd);

            // 打印格式：直接复制到INSERT语句中
            System.out.println("用户名：" + username);
            System.out.println("明文密码：" + rawPwd);
            System.out.println("加密密码：" + encodedPwd);
            System.out.println("------------------------");
        }
    }

    /**
     * 测试3：验证加密密码是否匹配（可选，用于校验SQL中的加密串是否有效）
     * 例如：验证SQL中已有的加密串是否能匹配123456
     */
    @Test
    void verifyPassword() {
        // 1. SQL中已有的加密串（示例）
        String sqlEncodedPwd = "$2a$10$7I4Gk9Lw9y8y8y8y8y8y8y8y8y8y8y8y8y8y8y8y8y8y8y8y";
        // 2. 明文密码
        String rawPwd = "123456";

        // 3. 验证匹配
        boolean isMatch = passwordEncoder.matches(rawPwd, sqlEncodedPwd);
        System.out.println("验证SQL中的加密串是否匹配123456：" + (isMatch ? "是" : "否"));
    }

    @Test
    void verifyDifferentEncodedPasswords() {
        // 复制测试类生成的3个不同密文
        String adminPwd = "$2a$10$9jp8zv9fdbZkz9QFhW1uiuUUm9SCTgyDnXx2czSvc5zBg3hyxUfRK";
        String teacherPwd = "$2a$10$isUgDTTQbr/EeZAAKRCiregHDtClEy7EqM4JlRgS6jhqee4/OurMa";
        String studentPwd = "$2a$10$5uyfa0xBELMcoaEW9l.zTOaN2kHV8ovrp5WCxBDGcMuz.igwPMHQS";

        // 验证所有密文都匹配123456
        System.out.println("admin密文匹配：" + passwordEncoder.matches("123456", adminPwd)); // true
        System.out.println("teacher密文匹配：" + passwordEncoder.matches("123456", teacherPwd)); // true
        System.out.println("student密文匹配：" + passwordEncoder.matches("123456", studentPwd)); // true
    }
}
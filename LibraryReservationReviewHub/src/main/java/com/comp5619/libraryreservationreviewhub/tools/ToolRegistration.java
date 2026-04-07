package com.comp5619.libraryreservationreviewhub.tools;

import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 集中的工具注册类
 */
@Configuration
public class ToolRegistration {


    @Bean
    public UserInfoTool userInfoTool() {
        return new UserInfoTool(); // 单独交给spring管理，里面的 @Autowired 会生效
    }

    @Bean
    public ToolCallback[] allTools(UserInfoTool userInfoTool, BookSearchTool bookSearchTool) {
        FileOperationTool fileOperationTool = new FileOperationTool();



        // 使用 Spring 管理的 bookSearchTool，避免 @Autowired 为空
        return ToolCallbacks.from(
                fileOperationTool,

                userInfoTool,
                bookSearchTool
        );
    }
}

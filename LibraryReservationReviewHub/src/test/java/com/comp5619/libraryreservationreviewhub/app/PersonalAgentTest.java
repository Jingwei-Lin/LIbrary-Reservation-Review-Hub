package com.comp5619.libraryreservationreviewhub.app;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 集成测试：PersonalAgent 流式对话（SSE + 用户上下文）
 * 不使用 StepVerifier，直接收集 Flux 输出。
 */
@SpringBootTest
class PersonalAgentTest {

    @Resource
    private PersonalAgent personalAgent;

    @Test
    void testChatByStream_WithUserContext() {
        String chatId = UUID.randomUUID().toString();
        String message1 = "Please help me find the book \"The Hobbit\".";
        String message2 = "Do you remember my name?.";
        String userContext = "{\"status\":\"logged_in\",\"userId\":1,\"userName\":\"Jingwei\"}";


        // 调用流式聊天方法
        Flux<String> flux = personalAgent.doChatByStream(message1, chatId, userContext);

        // 同步等待所有响应完成，并把流收集成 List
        List<String> chunks = flux
                .timeout(Duration.ofSeconds(10))  // 防止卡死
                .collectList()
                .block();

        // 拼接为完整文本
        String fullResponse = String.join(" ", chunks);
        System.out.println(" Full Merged Response: " + fullResponse);

        // 验证结果（假设模型一定会响应）
        assertThat(fullResponse).isNotBlank();
        assertThat(fullResponse).containsIgnoringCase("The Hobbit");

        // 调用流式聊天方法
        Flux<String> flux2 = personalAgent.doChatByStream(message2, chatId, userContext);

        // 同步等待所有响应完成，并把流收集成 List
        List<String> chunks2 = flux2
                .timeout(Duration.ofSeconds(10))  // 防止卡死
                .collectList()
                .block();

        // 拼接为完整文本
        String fullResponse2 = String.join(" ", chunks2);
        System.out.println(" Full Merged Response: " + fullResponse2);


        assertThat(fullResponse2)
                .isNotBlank()
                .containsIgnoringCase("jing")
                .containsIgnoringCase("wei");
    }


}

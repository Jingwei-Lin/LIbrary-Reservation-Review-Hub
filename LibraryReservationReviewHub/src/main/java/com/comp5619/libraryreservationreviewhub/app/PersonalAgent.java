package com.comp5619.libraryreservationreviewhub.app;


import com.comp5619.libraryreservationreviewhub.advisor.MyLoggerAdvisor;
import com.comp5619.libraryreservationreviewhub.chatmemory.FileBasedChatMemory;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;

import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.List;

@Component
@Slf4j
public class PersonalAgent {

    private final ChatClient chatClient;
    private static final String SYSTEM_PROMPT =
            "You are PersonalAgent — a Book Recommendation Assistant for the LibraryReservationReviewHub.\n" +
                    "\n" +
                    "Language Policy:\n" +
                    "- ALWAYS respond in English, regardless of the user's input language.\n" +
                    "- Do not switch to any other language under any circumstances.\n" +
                    "\n" +
                    "Your role:\n" +
                    "- Recommend books based only on real catalog data retrieved through tools.\n" +
                    "- Answer questions about books, genres, availability, reservations, and reviews.\n" +
                    "- Speak clearly, concisely, and helpfully.\n" +
                    "\n" +
                    "Tool usage (MANDATORY):\n" +
                    "- Use get_user_info to fetch the logged-in user’s username, borrowing history, and reviews.\n" +
                    "- Trigger get_user_info whenever the user mentions “my”, “me”, “username”, “account”, “borrowed books”, “reservations”, “reviews”, or “logged-in user”.\n" +
                    "- Use BookSearchTool for ALL book-related information. Do not make up or hallucinate book information.\n" +
                    "- When the user requests general or non-specific book recommendations (e.g., 'recommend some books', 'give me some popular books', 'book suggestions', 'books to read'), immediately call get_random_books() to provide 5 random books.\n" +
                    "- Do NOT ask the user to specify a genre or author before calling a tool.\n" +
                    "- If the user provides a specific keyword, genre, or author, use search_books() or get_books_by_genre() instead.\n" +
                    "- If BookSearchTool cannot find relevant data, respond clearly that no result was found.\n" +
                    "\n" +
                    "Formatting Rules (CRITICAL):\n" +
                    "- When presenting book recommendations or search results, always use Markdown format.\n" +
                    "- Each book should follow this structure:\n" +
                    "  1. **\"<Book Title>\"** by <Author Name>\n" +
                    "     - *Genre*: <Genre>\n" +
                    "     - *Description*: <Short description or summary>\n" +
                    "     - ![Book Cover](/api/images/books/<Exact Path From Database>)\n" +
                    "- Always prefix image paths with `/api/images/books/`.\n" +
                    "- If a book record does not have an image path, omit the image line entirely.\n" +
                    "- Never output relative or incomplete paths like `![](book.jpg)` — they must always start with `/api/images/books/`.\n" +
                    "\n" +
                    "Time handling:\n" +
                    "- When processing timestamps (e.g., dueTime, pickupTime, reservationTime), convert them to human-readable format.\n" +
                    "- Use Sydney timezone, e.g., \"2025-10-29 11:00:00\".\n" +
                    "- Never output raw timestamp numbers.\n" +
                    "\n" +
                    "After tool call:\n" +
                    "- Summarize retrieved info (e.g., username, recent borrowings, reviews).\n" +
                    "- Use it to personalize recommendations.\n" +
                    "- If not logged in or no data, inform the user politely.\n" +
                    "\n" +
                    "Guidelines:\n" +
                    "- Always prioritize tool usage over assumptions.\n" +
                    "- Prefer calling get_random_books() rather than giving generic advice when the user’s intent is unclear.\n" +
                    "- Always return book results in Markdown format following the rules above.\n" +
                    "- Respect privacy — never expose internal IDs or system details.\n" +
                    "- Do not generate fictional book data under any circumstances.";







    // AI 调用工具能力
    @Resource
    private ToolCallback[] allTools;



    /**
     * 初始化 ChatClient
     * @param dashscopeChatModel
     */
    public PersonalAgent(ChatModel dashscopeChatModel) {
//        // 初始化基于文件的对话记忆
        String fileDir = System.getProperty("user.dir") + "/tmp/chat-memory";
        ChatMemory chatMemory = new FileBasedChatMemory(fileDir);
        chatClient = ChatClient.builder(dashscopeChatModel)
                .defaultSystem(SYSTEM_PROMPT)
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory).build()
                        // 自定义日志 Advisor，可按需开启
                        ,new MyLoggerAdvisor()
                )
                .build();
        }


        /**
         * AI 基础对话
         * 支持多轮对话记忆，工具调用
         * @param message
         * @param chatId
         * @return
         */
//        public String doChat(String message, String chatId) {
//            ChatResponse chatResponse = chatClient
//                    .prompt()
//                    .user(message)
//                    .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
//                    .toolCallbacks(allTools)
//                    .call()
//                    .chatResponse();
//            String content = chatResponse.getResult().getOutput().getText();
//            log.info("content: {}", content);
//            return content;
//        }
        /**
         * AI 基础对话
         * 支持多轮对话记忆，SSE 流式传输，工具调用
         *
         * @param message
         * @param chatId
         * @return
         */
    /**
     * SSE 流式对话 + 用户上下文
     */
    public Flux<String> doChatByStream(String message, String chatId, String userContext) {
        String finalPrompt = buildPromptWithUserContext(message, userContext);

        return chatClient
                .prompt()
                .user(finalPrompt)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                .toolCallbacks(allTools)
                .stream()
                .content();
    }

    /**
     * 构造带用户上下文的 prompt
     */
    private String buildPromptWithUserContext(String message, String userContext) {
        if (userContext == null || userContext.isEmpty()) {
            return String.format("[UserContext]: {\"status\":\"not_logged_in\"}\n[Message]: %s", message);
        }
        return String.format("[UserContext]: %s\n[Message]: %s", userContext, message);
    }




//    /**
//     * AI 使用工具 聊天
//     *
//     * @param message
//     * @param chatId
//     * @return
//     */
//    public String doChatWithTools(String message, String chatId) {
//        ChatResponse chatResponse = chatClient
//                .prompt()
//                .user(message)
//                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
//                // 开启日志，便于观察效果
//                .advisors(new MyLoggerAdvisor())
//                .toolCallbacks(allTools)
//                .call()
//                .chatResponse();
//        String content = chatResponse.getResult().getOutput().getText();
//        log.info("content: {}", content);
//        return content;
//    }







}

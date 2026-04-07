import { useState, useEffect, useRef, useCallback } from 'react';
import { v4 as uuidv4 } from 'uuid';
import { EventSourcePolyfill } from 'event-source-polyfill';

interface Message {
    id: string;
    sender: 'user' | 'ai';
    content: string;
    timestamp: Date;
}

const useChat = () => {
    const [messages, setMessages] = useState<Message[]>([]);
    const [isTyping, setIsTyping] = useState(false);
    const [chatId] = useState<string>(uuidv4());
    const eventSourceRef = useRef<EventSource | null>(null);
    const messagesEndRef = useRef<HTMLDivElement | null>(null);

    // 滚动到最新消息
    const scrollToBottom = useCallback(() => {
        messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
    }, []);

    useEffect(() => {
        scrollToBottom();
    }, [messages, scrollToBottom]);

    // 发送消息并接收流式响应
    const sendMessage = useCallback((content: string) => {
        if (!content.trim()) return;

        // 添加用户消息
        const userMessage: Message = {
            id: uuidv4(),
            sender: 'user',
            content,
            timestamp: new Date(),
        };
        setMessages(prev => [...prev, userMessage]);
        setIsTyping(true);

        // 创建AI消息占位符
        const aiMessageId = uuidv4();
        const aiMessage: Message = {
            id: aiMessageId,
            sender: 'ai',
            content: '',
            timestamp: new Date(),
        };
        setMessages(prev => [...prev, aiMessage]);

        // 关闭之前的连接
        if (eventSourceRef.current) {
            eventSourceRef.current.close();
        }

        // 使用 EventSourcePolyfill 代替 EventSource
        const url = `http://localhost:8432/api/ai/personal_agent/chat/server_sent_event?message=${encodeURIComponent(content)}&chatId=${chatId}`;
        const eventSource = new EventSourcePolyfill(url, {
            withCredentials: true  // ✅ 关键：让浏览器带上 cookie
        });
        eventSourceRef.current = eventSource as unknown as EventSource;

        eventSource.onmessage = (event) => {
            const chunk = event.data;
            setMessages(prev =>
                prev.map(msg =>
                    msg.id === aiMessageId
                        ? { ...msg, content: msg.content + chunk }
                        : msg
                )
            );
        };

        eventSource.onerror = () => {
            setIsTyping(false);
            eventSource.close();
            eventSourceRef.current = null;
        };

        eventSource.addEventListener('complete', () => {
            setIsTyping(false);
            eventSource.close();
            eventSourceRef.current = null;
        });

        return aiMessageId;
    }, [chatId]);

    // 组件卸载时关闭连接
    useEffect(() => {
        return () => {
            if (eventSourceRef.current) {
                eventSourceRef.current.close();
            }
        };
    }, []);

    return {
        messages,
        isTyping,
        sendMessage,
        messagesEndRef,
        chatId
    };
};

export default useChat;

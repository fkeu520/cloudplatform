package com.cloudhub.platform.message.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Service
public class SseService {

    /** SSE 超时: 5 分钟，客户端自动重连，防止失效连接泄漏 (M1) */
    private static final long SSE_TIMEOUT = 300_000L;

    /** 多标签页支持: 每个用户可持有多个 SseEmitter (M2) */
    private final Map<Long, CopyOnWriteArrayList<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public SseEmitter subscribe(Long userId) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);
        emitters.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>()).add(emitter);
        Runnable cleanup = () -> {
            List<SseEmitter> list = emitters.get(userId);
            if (list != null) {
                list.remove(emitter);
                if (list.isEmpty()) {
                    emitters.remove(userId);
                }
            }
        };
        emitter.onCompletion(cleanup);
        emitter.onTimeout(cleanup);
        emitter.onError(e -> cleanup.run());
        try {
            emitter.send(SseEmitter.event().name("connected").data("ok"));
        } catch (Exception ignored) {}
        return emitter;
    }

    public void sendToUser(Long userId, String eventName, Object data) {
        List<SseEmitter> list = emitters.get(userId);
        if (list == null || list.isEmpty()) return;
        list.removeIf(emitter -> {
            try {
                emitter.send(SseEmitter.event().name(eventName).data(data));
                return false;
            } catch (Exception e) {
                return true;
            }
        });
        if (list.isEmpty()) {
            emitters.remove(userId);
        }
    }

    public void broadcast(String eventName, Object data) {
        emitters.forEach((userId, list) ->
            list.removeIf(emitter -> {
                try {
                    emitter.send(SseEmitter.event().name(eventName).data(data));
                    return false;
                } catch (Exception e) {
                    return true;
                }
            })
        );
        emitters.values().removeIf(List::isEmpty);
    }
}

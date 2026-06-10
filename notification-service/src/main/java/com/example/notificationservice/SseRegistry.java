package com.example.notificationservice;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Component
public class SseRegistry {

  private static final Logger log = LoggerFactory.getLogger(SseRegistry.class);

  private final Map<String, List<SseEmitter>> emitters = new ConcurrentHashMap<>();
  private final ObjectMapper objectMapper;

  public SseRegistry() {
    this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
  }

  public void register(String auctionId, SseEmitter emitter) {
    log.info("SSE emitter registered for auction {}", auctionId);
    emitters.computeIfAbsent(auctionId, k -> new CopyOnWriteArrayList<>()).add(emitter);
    emitter.onCompletion(() -> remove(auctionId, emitter));
    emitter.onTimeout(() -> remove(auctionId, emitter));
    emitter.onError(e -> remove(auctionId, emitter));
  }

  @EventListener
  public void onSseNotificationEvent(SseNotificationEvent event) {
    List<SseEmitter> auctionEmitters = emitters.get(event.auctionId());
    log.info("SSE event received: type={} auctionId={} emitters={}",
        event.eventType(), event.auctionId(),
        auctionEmitters == null ? 0 : auctionEmitters.size());
    if (auctionEmitters == null || auctionEmitters.isEmpty()) {
      return;
    }

    Map<String, Object> envelope = Map.of("type", event.eventType(), "data", event.data());
    String payload;
    try {
      payload = objectMapper.writeValueAsString(envelope);
    } catch (JsonProcessingException e) {
      log.error("Failed to serialize SSE event for auction {}", event.auctionId(), e);
      return;
    }

    for (SseEmitter emitter : auctionEmitters) {
      try {
        emitter.send(payload);
      } catch (IOException e) {
        emitter.completeWithError(e);
      }
    }
  }

  private void remove(String auctionId, SseEmitter emitter) {
    List<SseEmitter> auctionEmitters = emitters.get(auctionId);
    if (auctionEmitters != null) {
      auctionEmitters.remove(emitter);
    }
  }
}

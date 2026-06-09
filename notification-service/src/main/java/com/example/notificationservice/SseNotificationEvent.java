package com.example.notificationservice;

import java.util.Map;

public record SseNotificationEvent(String auctionId, String eventType, Map<String, Object> data) {}

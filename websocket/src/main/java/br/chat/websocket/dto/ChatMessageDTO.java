package br.chat.websocket.dto;

public record ChatMessageDTO(String type, String username, String content) {
}

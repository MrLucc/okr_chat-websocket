package br.chat.websocket.chat;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
public class ChatHandler extends TextWebSocketHandler {
    private final Map<String, WebSocketSession> sessionsActives = new ConcurrentHashMap<>();
    private final Map<String, String> partnersActives = new ConcurrentHashMap<>();
    private final Map<String, String> usernames = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String sessionId = session.getId();
        sessionsActives.put(sessionId, session);

        System.out.println("Nova conexão: " + sessionId);

        if (sessionsActives.size() % 2 == 0) {
            String partnerId = findLonelySession(sessionId);
            if (partnerId != null) {
                partnersActives.put(sessionId, partnerId);
                partnersActives.put(partnerId, sessionId);

                sendMessage(session, "Conectado! Alguém está na sala com você!");
                sendMessage(sessionsActives.get(partnerId), "Conectado! Alguém está na sala com você!");
            }
        } else {
            sendMessage(session, "Conectado! Você está sozinho na sala.");
        }

        sendMessage(session, "Digite seu nome para começar:");
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String myId = session.getId();
        String text = message.getPayload().trim();

        if (text.isEmpty()) {
            return;
        }

        String partnerId = partnersActives.get(myId);
        if (partnerId == null) {
            sendMessage(session, "Você continua sozinho na sala.");
            return;
        }

        if (!usernames.containsKey(myId)) {
            String username = text.trim();
            usernames.put(myId, username);

            sendMessage(session, "Nome definido como: " + username);
            System.out.println("Nome definido: " + username + " (sessão " + myId + ")");
            return;
        }


        String username = usernames.get(myId);
        WebSocketSession partner = sessionsActives.get(partnerId);

        if (partner != null && partner.isOpen()) {
            String formattedMessage = username + ": " + text;
            partner.sendMessage(new TextMessage(formattedMessage));
            System.out.println("Enviado " + username + ": " + text);
        } else {
            sendMessage(session, "O amigo desconectou.");
        }
    }

    @Override
    @NullMarked
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception{
        String myId = session.getId();
        String partnerId = partnersActives.get(myId);
        sessionsActives.remove(myId);

        System.out.println("Saiu do chat: " + myId);

        if(partnerId != null){
            WebSocketSession partner = sessionsActives.get(partnerId);
            if(partner != null && partner.isOpen()){
                sendMessage(partner, "O outro usuario desconectou!");
                partnersActives.remove(partnerId);
            }
        }
    }


   private String findLonelySession(String excludeId) {
        for (Map.Entry<String, WebSocketSession> entry : sessionsActives.entrySet()) {
            String id = entry.getKey();
            if (!id.equals(excludeId) && !partnersActives.containsKey(id)) {
                WebSocketSession s = entry.getValue();
                if (s.isOpen()) {
                    return id;
                }
            }
        }
        return null;
    }

   private void sendMessage(WebSocketSession session, String text) throws Exception{
        if(session != null && session.isOpen()){
            session.sendMessage(new TextMessage(text));
        }
   }
    
    
}

package br.chat.websocket.chat;

import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
public class ChatHandler extends TextWebSocketHandler {
    private final Map<String, WebSocketSession> sessionsActives = new ConcurrentHashMap<>();
    private final Map<String, String> partnersActives = new ConcurrentHashMap<>();

    private final Queue<String> waitingQueue = new ConcurrentLinkedDeque<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception{
        String sessionId = session.getId();
        sessionsActives.put(sessionId, session);

        System.out.println("Nova Conexão: " + sessionId + " | total de sessões: " + sessionsActives.size());

        String partnerId = findLonelySession(sessionId);

        if(partnerId != null){
            if(Objects.nonNull(partnerId)){
                partnersActives.put(sessionId, partnerId);
                partnersActives.put(partnerId, sessionId);

                System.out.println("✅ Pairing feito: " + sessionId + " <-> " + partnerId);

                sendMessage(session, "Conectado! Você está sozinho na sala.");
                sendMessage(sessionsActives.get(partnerId), "Conectado! Alguém está na sala com você!");
            }
        }else{
            sendMessage(session, "Aguardado outra pessoa conectar...");
        }

    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception{
        String myId = session.getId();
        String partnerId = partnersActives.get(myId);

        System.out.println("📨 Mensagem recebida de " + myId + " | Partner: " + partnerId + " | Texto: " + message.getPayload());

        if(partnerId != null && sessionsActives.containsKey(partnerId)){
            WebSocketSession partnerSession = sessionsActives.get(partnerId);

            if(partnerSession.isOpen()){
                partnerSession.sendMessage(new TextMessage(message.getPayload()));
                System.out.println("✅ Mensagem ENCAMINHADA para " + partnerId);
            }else{
                System.out.println("❌ Partner fechado: " + partnerId);
            }

        }else{
            System.out.println("❌ Nenhum partner encontrado para " + myId);
            sendMessage(session, "Ainda não tem parceiro conectado.");
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception{
        String myId = session.getId();
        String partnerId = partnersActives.get(myId);
        sessionsActives.remove(myId);

        System.out.println("🔴 Desconexão: " + myId);

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

package br.chat.websocket.chat;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
public class ChatHandler extends TextWebSocketHandler {
    private final Map<String, WebSocketSession> sessionsActives = new ConcurrentHashMap<>();
    private final Map<String, String> partnersActives = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception{
        String sessionId = session.getId();
        sessionsActives.put(sessionId, session);

        System.out.println("Nova Conexão: " + sessionId + " | total de sessões: " + sessionsActives.size());

        if(sessionsActives.size() % 2 == 0){
            String partenerId = findWaitingPartner(sessionId);
            if(Objects.nonNull(partenerId)){
                partnersActives.put(sessionId, partenerId);
                partnersActives.put(partenerId, sessionId);

                System.out.println("✅ Pairing feito: " + sessionId + " <-> " + partenerId);

                sendMessage(session, "Conectado! Você está sozinho na sala.");
                sendMessage(sessionsActives.get(partenerId), "Conectado! Alguém está na sala com você!");
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


    private String findWaitingPartner(String myId){
        for(String id : sessionsActives.keySet()){
            if(id.equals(myId) && !partnersActives.containsKey(id)){
                return id;
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

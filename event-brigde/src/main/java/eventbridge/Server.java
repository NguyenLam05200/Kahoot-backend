package eventbridge;

import com.corundumstudio.socketio.*;
import com.corundumstudio.socketio.listener.DataListener;
import model.*;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class Server   {
	public static String randomString(int n) {
		return "TST";
	}

	public static void main(String[] args) {
		Configuration config = new Configuration();
//		config.setHostname("localhost");
		config.setPort(9092);

		final SocketIOServer server = new SocketIOServer(config);
		SocketIONamespace kahootNamespace =   server.addNamespace("/ws/kahoot");

		Map<UUID, User> users = new HashMap<>();
		Map<String, Room> rooms = new HashMap<>();

		kahootNamespace.addEventListener("chatevent", ChatObject.class, new DataListener<ChatObject>() {
			@Override
			public void onData(SocketIOClient client, ChatObject data, AckRequest ackRequest) {
				if (client.getAllRooms().isEmpty()) {
					client.sendEvent("error", "User has not join any rooms yet");
					return;
				}
				Set<String> roomIds = client.getAllRooms();
				String roomId = roomIds.iterator().next();
				User user = users.get(client);

//				server.getRoomOperations(roomId).sendEvent("chatevent", new ChatObject(user.getUsername(), data, Instant.now()));
			}
		});

		kahootNamespace.addEventListener(
        "join",
        JoinRequest.class,
        new DataListener<JoinRequest>() {
          @Override
          public void onData(SocketIOClient client, JoinRequest req, AckRequest ackRequest) {
            if (StringUtils.isEmpty(req.getRoomId()) || !rooms.containsKey(req.getRoomId())) {
              client.sendEvent("error", "Room not exist");
              return;
            }
            if (StringUtils.isEmpty(req.getUserName())) {
              client.sendEvent("error", "Username not set");
              return;
            }

            User user =
                User.builder()
                    .connectionId(client.getSessionId())
                    .username(req.getUserName())
                    .roomId(req.getRoomId())
                    .build();

            users.put(client.getSessionId(), user);

            // join room
            client.joinRoom(user.getRoomId());
          }
        });

		// get ranking
		kahootNamespace.addEventListener("ranking", RankingRequest.class, new DataListener<RankingRequest>() {
			@Override
			public void onData(SocketIOClient client, RankingRequest req, AckRequest ackRequest) {
				server.getRoomOperations(req.getRoomId()).sendEvent("ranking-response", "LIST OF RANKING");
			}
		});
		// get ranking
		kahootNamespace.addEventListener("ranking", RankingRequest.class, new DataListener<RankingRequest>() {
			@Override
			public void onData(SocketIOClient client, RankingRequest req, AckRequest ackRequest) {
				server.getRoomOperations(req.getRoomId()).sendEvent("ranking-response", "LIST OF RANKING");
			}
		});

		// start room()
		// do some checks
		// emit event to all room's users
		// change status

		kahootNamespace.addEventListener("answer", AnswerRequest.class, new DataListener<AnswerRequest>() {
			@Override
			public void onData(SocketIOClient client, AnswerRequest answer, AckRequest ackRequest) {
				// verify client's room for answer.question
				// check if correct answer
				// get streak
				// get time diff
				// calculate points
				// send point
			}
		});

		server.start();

		 try {
			 Thread.sleep(Integer.MAX_VALUE);
		 } catch (InterruptedException e){

		 }

		server.stop();
	}
}

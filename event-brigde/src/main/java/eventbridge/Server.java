package eventbridge;

import com.corundumstudio.socketio.*;
import model.*;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class Server {
  Map<String, Room> rooms;
  Map<UUID, User> users;
  private SocketIOServer socket;

  public static void main(String[] args) {
    Configuration config = new Configuration();
    //		config.setHostname("localhost");
    config.setPort(9092);

    final SocketIOServer server = new SocketIOServer(config);
    SocketIONamespace kahootNamespace = server.addNamespace("/ws/kahoot");

    Server sev = new Server();

    kahootNamespace.addEventListener("join", JoinRequest.class, sev::handleJoin);
    kahootNamespace.addEventListener(
        "broadcast_question", BroadcastQuestionRequest.class, sev::handleBroadcastQuestion);

    // start room()
    // do some checks
    // emit event to all room's users
    // change status

    kahootNamespace.addEventListener(
        "answer",
        AnswerRequest.class,
        (client, answer, ackRequest) -> {
          // verify client's room for answer.question
          // check if correct answer
          // get streak
          // get time diff
          // calculate points
          // send point
        });

    server.start();

    try {
      Thread.sleep(Integer.MAX_VALUE);
    } catch (InterruptedException e) {

    }

    server.stop();
  }

  String getRoomByUser(String user) {
    return "";
  }

  void chatEnv(SocketIOClient client, ChatObject data, AckRequest ackSender) throws Exception {}

  public void handleJoin(SocketIOClient client, JoinRequest req, AckRequest ackRequest) {
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

  public void handleBroadcastQuestion(
      SocketIOClient client, BroadcastQuestionRequest req, AckRequest ackRequest) {
    String room = getRoomByUser(client.getSessionId().toString());
    if (StringUtils.isEmpty(room)) {
      socket.getClient(client.getSessionId()).sendEvent("error", "room empty");
      return;
    }
    List<Answer> answers = req.getQuestion().getAns();
	  List<Answer> newAnswers = answers.stream().map(answer -> Answer.builder().text(answer.getText()).build()).collect(Collectors.toList());
    Question broadcastQuestion = Question.builder()
		    .text(req.getQuestion().getText())
		    .ans(newAnswers)
		    .img(req.getQuestion().getImg())
		    .build();
    socket.getRoomOperations(room).sendEvent("broadcast_question", broadcastQuestion);
  }
}

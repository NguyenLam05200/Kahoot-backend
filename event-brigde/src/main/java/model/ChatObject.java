package model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChatObject {
	private String userName;
	private String message;
//	private Instant timestamp;
//	private String roomId;
public ChatObject(String userName, String message) {
	super();
	this.userName = userName;
	this.message = message;
}
	public ChatObject() {

	}
	public String getUserName() {
		return userName;
	}
	public void setUserName(String userName) {
		this.userName = userName;
	}

	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}
}

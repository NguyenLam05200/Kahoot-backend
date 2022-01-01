package entity;

import common.JsonElement;
import common.JsonSerializable;

@JsonSerializable
public class User {

    @JsonElement(key = "usrname")
    public String userName;

    @JsonElement
    public String hashedPass;

    @JsonElement
    public int UserId;


    public User(String userName, String hashedPass) {
        this.userName = userName;
        this.hashedPass = hashedPass;
    }
}

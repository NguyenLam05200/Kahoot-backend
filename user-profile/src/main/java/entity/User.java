package entity;

import common.JsonElement;
import common.JsonSerializable;

@JsonSerializable
public class User {

    @JsonElement(key = "username_m")
    public String userName;

    @JsonElement
    public String hashedPass;

    @JsonElement
    public Integer UserId;

    private String email;


    public User(String userName, String hashedPass) {
        this.userName = userName;
        this.hashedPass = hashedPass;
        this.email = "email.com";
    }
}

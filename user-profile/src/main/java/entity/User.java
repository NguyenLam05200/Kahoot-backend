package entity;


public class User {


    public String userName;


    public String hashedPass;


    public Integer UserId;

    private String email;


    public User(String userName, String hashedPass) {
        this.userName = userName;
        this.hashedPass = hashedPass;
        this.email = "email.com";
    }
}

package entity;

public class User {
    public String userName;
    public String hashedPass;
    public int UserId;


    public User(String userName, String hashedPass) {
        this.userName = userName;
        this.hashedPass = hashedPass;
    }
}

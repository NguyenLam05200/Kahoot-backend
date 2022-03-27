package entity;


public class User {
    public String userName;
    public String hashedPass;
    public String salt;
    public Integer userId;
    private String email;

    public User(String userName, String hashedPass) {
        this.userName = userName;
        this.hashedPass = hashedPass;
        this.email = "email.com";
    }
}

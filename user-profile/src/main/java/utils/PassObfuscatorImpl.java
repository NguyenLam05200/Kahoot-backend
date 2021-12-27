package utils;

public class PassObfuscatorImpl implements PasswordObfuscator{

    @Override
    public String Obfuscate(String password) {
        return "MASKED" + password;
    }
}

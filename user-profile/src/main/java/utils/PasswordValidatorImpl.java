package utils;

public class PasswordValidatorImpl implements PasswordValidator {
    String[] validators;
    static PasswordValidatorImpl create(String[] validators) {
        return new PasswordValidatorImpl(validators);
    }

    public PasswordValidatorImpl(String[] validators) {
        this.validators = validators;
    }

    @Override
    public Boolean Validate(String password) {
        for (String validator : validators) {
            if (validator.isEmpty())
                return false;
        }
        return true;
    }
}

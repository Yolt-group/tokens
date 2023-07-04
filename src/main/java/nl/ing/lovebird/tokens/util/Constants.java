package nl.ing.lovebird.tokens.util;

import lombok.experimental.UtilityClass;

@UtilityClass
public class Constants {
    public static final String JWT_REGEX = "^[a-zA-Z0-9\\-_]*\\.[a-zA-Z0-9\\-_]*\\.[a-zA-Z0-9\\-_]*$";
    public static final int MAX_SIZE_REQUEST_TOKEN = 1500;
}

package cz.scholz.demo.livescore;

/**
 * Created by schojak on 12.1.17.
 */
public class InvalidGameException extends Exception {
    public InvalidGameException(String message) {
        super(message);
    }
}

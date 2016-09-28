package kr.ac.kaist.resl.cmsp.iotapp.engine;

/**
 * Created by shheo on 15. 4. 13.
 */

/**
 * Exception constructor is adopted from
 * http://stackoverflow.com/questions/1754315/how-to-create-custom-exceptions-in-java
 */

public class PlatformException extends Exception {
    public PlatformException() {
        super();
    }

    public PlatformException(String message) {
        super(message);
    }

    public PlatformException(String message, Throwable cause) {
        super(message, cause);
    }

    public PlatformException(Throwable cause) {
        super(cause);
    }
}

package jarvis.event;

/**
 * @author <a href="http://twitter.com/aloyer">@aloyer</a>
 */
public class MidAirCollision extends RuntimeException {
    public MidAirCollision(Object message) {
        super(String.valueOf(message));
    }
}

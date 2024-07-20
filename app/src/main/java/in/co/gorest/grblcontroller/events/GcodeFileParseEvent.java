package in.co.gorest.grblcontroller.events;

public class GcodeFileParseEvent {
    public enum Type {ParsingComplete}

    private final Type type;

    public GcodeFileParseEvent(Type type) {
        this.type = type;
    }

    public Type getType() {
        return this.type;
    }
}

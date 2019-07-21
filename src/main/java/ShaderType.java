public enum ShaderType {
    VERTEX(35633),
    FRAGMENT(35632);

    private int value;

    ShaderType(int value) {

        this.value = value;
    }

    public int getValue() {
        return value;
    }
}

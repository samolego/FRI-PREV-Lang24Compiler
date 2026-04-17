package lang24.data.type;

public interface WatType {
    Type watType();

    enum Type {
        VOID(""),
        I32("i32"),
        I64("i64"),
        F32("f32"),
        F64("f64");

        private final String watType;

        Type(String watType) {
            this.watType = watType;
        }

        @Override
        public String toString() {
            return watType;
        }
    }
}

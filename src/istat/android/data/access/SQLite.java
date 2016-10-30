package istat.android.data.access;

public class SQLite {
    public static DbSelection select(Class<?> clazz) {
        return new DbSelection(clazz);
    }

    public static DbUpdate update(Class<?> clazz) {
        return new DbUpdate(clazz);
    }

    public static DbDelete delete(Class<?> clazz) {
        return new DbDelete(clazz);
    }

    public static DbInsert insert(Object entity) {
        return new DbInsert().insert(entity);
    }

    public static class SELECT {
        public static DbSelection from(Class<?> clazz) {
            return new DbSelection(clazz);
        }
    }

    public static class UPDATE {
        public static DbUpdate table(Class<?> clazz) {
            return new DbUpdate(clazz);
        }
    }

    public static class DELETE {
        public static DbDelete from(Class<?> clazz) {
            return new DbDelete(clazz);
        }
    }

    public static class INSERT {
        public static DbInsert entity(QueryAble entity) {
            return new DbInsert().insert(entity);
        }
    }
}

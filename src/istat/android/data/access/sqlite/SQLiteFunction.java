package istat.android.data.access.sqlite;

/**
 * Created by istat on 28/03/17.
 */

class SQLiteFunction {
    String expression;
    String column;

//    public SQLiteFunction(String column) {
//        this.column = column;
//    }

    public void of(String column) {
        this.column = column;
    }

    String getExpression() {
        return expression;
    }


}

package istat.android.data.access.sqlite;

/**
 * Created by istat on 28/03/17.
 */

public class SQLiteFunction {
    public final static String TYPE_ROUND = "ROUND";
    public final static String TYPE_LAST = "LAST";
    public final static String TYPE_FIRST = "FIRST";
    public final static String TYPE_RANDOM = "RANDOM";
    public final static String TYPE_SUM = "SUM";
    public final static String TYPE_COUNT = "COUNT";
    public final static String TYPE_AVG = "AVG";
    public final static String TYPE_MIN = "MIN";
    public final static String TYPE_MAX = "MAX";
    public final static String TYPE_UPPER = "UPPER";
    public final static String TYPE_LOWER = "LOWER";

    public final static String TYPE_CHARINDEX = "CHARINDEX";
    public final static String TYPE_CONCAT = "CONCAT";
    public final static String TYPE_LEFT = "LEFT";
    public final static String TYPE_LENGTH = "LENGTH";
    public final static String TYPE_LTRIM = "LTRIM";
    public final static String TYPE_SUBSTRING = "SUBSTRING";
    public final static String TYPE_PATINDEX = "PATINDEX";
    public final static String TYPE_REPLACE = "REPLACE";
    public final static String TYPE_RIGHT = "RIGHT";



    public final static SQLiteFunction SUM = new SQLiteFunction(TYPE_SUM);
    public final static SQLiteFunction COUNT = new SQLiteFunction(TYPE_COUNT);
    public final static SQLiteFunction AVG = new SQLiteFunction(TYPE_AVG);
    public final static SQLiteFunction MIN = new SQLiteFunction(TYPE_MIN);
    public final static SQLiteFunction MAX = new SQLiteFunction(TYPE_MAX);
    String name;
    String column;

    SQLiteFunction(String functionName) {
        this.column = functionName;
    }

    public SQLiteFunction of(String column) {
        this.column = column;
        return this;
    }

    public SQLiteFunction of(SQLiteFunction fun) {

        return this;
    }


    public String getExpression() {
        return name + "(" + this.column + ")";
    }

    @Override
    public String toString() {
        return getExpression();
    }
}

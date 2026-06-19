package istat.android.data.access.sqlite;

/**
 * Created by istat on 28/03/17.
 */

public class SQLiteFunction {
    public final static String TYPE_ROUND = "ROUND";
    public final static String TYPE_LAST = "LAST";
    public final static String TYPE_FIRST = "FIRST";
    public final static String TYPE_RANDOM = "RANDOM";

    //opetarors

    public final static String TYPE_SUM = "SUM";
    public final static String TYPE_COUNT = "COUNT";
    public final static String TYPE_AVG = "AVG";
    public final static String TYPE_MIN = "MIN";
    public final static String TYPE_MAX = "MAX";
    public final static String TYPE_UPPER = "UPPER";
    public final static String TYPE_LOWER = "LOWER";

    //Charsequence

    public final static String TYPE_CHARINDEX = "CHARINDEX";
    public final static String TYPE_CONCAT = "CONCAT";
    public final static String TYPE_LEFT = "LEFT";
    public final static String TYPE_LENGTH = "LENGTH";
    public final static String TYPE_LTRIM = "LTRIM";
    public final static String TYPE_SUBSTRING = "SUBSTRING";
    public final static String TYPE_PATINDEX = "PATINDEX";
    public final static String TYPE_REPLACE = "REPLACE";
    public final static String TYPE_RIGHT = "RIGHT";

    //Dates
    /*
    NOW()	Returns the current date and time
    CURDATE()	Returns the current date
    CURTIME()	Returns the current time
    DATE()	Extracts the date part of a date or date/time expression
    EXTRACT()	Returns a single part of a date/time
    DATE_ADD()	Adds a specified time interval to a date
    DATE_SUB()	Subtracts a specified time interval from a date
    DATEDIFF()	Returns the number of days between two dates
    DATE_FORMAT()	Displays date/time data in different formats

    DATE - format YYYY-MM-DD
    DATETIME - format: YYYY-MM-DD HH:MI:SS
    TIMESTAMP - format: YYYY-MM-DD HH:MI:SS
    YEAR - format YYYY or YY

    GETDATE()	Returns the current date and time
    DATEPART()	Returns a single part of a date/time
    DATEADD()	Adds or subtracts a specified time interval from a date
    DATEDIFF()	Returns the time between two dates
    CONVERT()	Displays date/time data in different formats

    FORMAT()	Formats how a field is to be displayed
    NOW()	Returns the current system date and time


     */


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

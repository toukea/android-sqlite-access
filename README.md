# android-sqlite-access
android Library to help SQLite db Query and Management using a easy and sweet Query builder.

# Create Some class to persist.
```java
@SQLiteModel.Table(name = "User") //if not set, default is class.getSimpleName()
public class User {

    /*
    specify this field as Column and give it a name.
    If not set, default is the property label
     */
    @Column(name="userName")
    public String userName;
    
    public String firstName; //schould be persisted with label 'firstName'
    
    public int year;        //schould be persisted with label 'year'
    
    /**
    make this field as table primary key.
    ome thing to know is that:
    If your class doesn't has explicit primary key declaration but contain a
    property named 'id' (case not sensitive) it will be implicitelly considered as your primaryKey
    */
    @SQLiteModel.PrimaryKey(policy = SQLiteModel.PrimaryKey.POLICY_AUTO_INCREMENT) //usable for integer Id only
    int id; 
    /* if primaryKey policy is not set, SQLiteModel.PrimaryKey.POLICY_DEFAULT is used
    so "id" is persisted as it has been set event if not defined or NULL */
    
    @Ignore     //ignore this field when persisting and querying on Db.
    boolean readOnly=false;
 }
```

# Add SQLite Connexion 
you can add one or many 'SQLiteConnexion' to your SQLite context. 
in this part, we will add connection to the Database defined by:
DbName="testDB",
DBVersion=1;
**`NB: it is strongly recommended  to make it on the onCreate of your <extends> android.app.Application class.`**
```java
    String DbName = "TestDB";
    SQLite.addConnection(new SQLite.SQLiteConnection(appContext, DbName, 1) {
        @Override
        public void onCreateDb(SQLiteDatabase db) {
        /*
        here you can execute script to create your database from
         de SQLiteDataBase param instance
        */
            try {
            /* here, i am creating User Table from User.class using TableUtils.class */
                TableUtils.create(db, User.class);
            /*
             you can also init your table by executing an SQL script File resource.
             [in this case R.raw.db_script contain my db script.]
             SQLite.executeSQLScript(db, appContext.getResources().openRawResource(R.raw.db_script));
            */       
            
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        @Override
        public void onUpgradeDb(SQLiteDatabase db, int oldVersion, int newVersion) {
        /*
        here you can execute SQL script to update your database
         from de SQLiteDataBase param instance
        */
            try {
            /*
             here, i am executing a script from my R.raw resource
            */
                SQLite.executeSQLScript(db, appContext.getResources().openRawResource(R.raw.test));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }       
    });
 ```

# How to perform query from your Db?
In order perfom sql-query on your database, you have to get an SQL instance.
SQL instance can be get using tow way:
- Using SQL.prepareSQL.
- Using existing connection without prepare.

# SQL instance using prepare
From de DbName given above (when adding connection) you can prepare SQL instance.
NB: SQL instance will be useful for perform SQL Query.
 ```java
    SQLite.prepareSQL(DbName, new SQLite.PrepareHandler() {
        @Override
        public void onSQLReady(SQLite.SQL sql)  throws Exception {
           //when the prepare succeed, use the SQL instance to httpAsyncQuery for result.
        }

        @Override
        public void onSQLPrepareFail(Exception e) {
           //called when the prepare fail. it give you an Exception which describe the error.
        }
    });
```
You can also make a  transactional Prepare.
```java
    SQLite.prepareTransactionalSQL(DbName, new SQLite.PrepareHandler() { //Some code
```

# SQL instance using existing connection without prepare.
After least one data base connection successfully, it is also possible to execute SQLite clause
without call explicitly a prepare (prepareSQL or prepareTransactionalSQL).
Of course, it is possible to get an SQLite.SQL instance directly from connection Name.

Get SQLite.SQL instance from connection Name
```java
    /*
    obtain an SQLite.SQL instance.
     */
    SQLite.SQL sql= SQLite.fromConnection(DbName);

    /*
    Code to execute one or more SQL clause...
    .....
    .....
     */

    /*
    Clause SQL when not needed anymore
    */
    sql.close();
```

It is also possible to specify if you want to use an auto closable connection. (Auto closable SQL instance, is an SQL which would be auto close after any execution)
```java
    /*
     here i want an auto clauseAble SQL from my connection.
     */
    boolean autoClause=true;
    /*
    obtain an SQLite.SQL instance.
     */
    SQLite.SQL sql= SQLite.fromConnection(DbName, autoClause);

    /*
    Code to execute one or more SQL clause...
    .....
    .....
     */

    //SQL instance has been auto closed after execution.
```

# Make SQL Insert 
After SQL instance has been prepared successfully, you can use them to perform SQL Insert.
```java
       User user = new User();
       user.userName = "Toukea";
       user.firstName = "Jephte";
       user.year = 25;
                    
       long insertIds[] = sql.insert(user).execute();// Array List of last insert 'Id'
 ```   
It is also possible to perform multiple insertions in one step
 ```java
       /*
       here, some multiple[3] user definitions
       */
                                   
        long insertIds[] = sql.insert(user0,user1,user2).execute();// Array List of last insert 'Id'
        
        System.out.println("user0 extra= "+insertIds[0]);
        System.out.println("user1 extra= "+insertIds[1]);
        System.out.println("user2 extra= "+insertIds[2]);
  ``` 
  
# Make SQL Delete 
 After SQL instance has been prepared successfully, you can use them to perform SQL delete.
 ```java
         int deletedCount = sql.delete(User.class)
                                .where("firstname")
                                .like("%Jephte%")
                                .execute();
                                
         System.out.println("deleted line="+deletedCount);
  ```    
 
# Make SQL Update 
After SQL instance has been prepared successfully, you can use them to perform SQL update.
```java
          int updatedCount = sql.update(User.class)
                                   .set("userName", "newName")
                                   .where("firstName")
                                   .like("%Jephte%")
                                   .execute();

          System.out.println("updated line="+updatedCount);
           
``` 
It is also possible to update from another model.
```java
           User userModel=new User();
                userModel.firstName="Julie";
                userModel.year=21;
                
           int updatedCount = sql.update(User.class)
                                   .setAs(userModel)
                                   .where("firstname")
                                   .like("%jephte%")
                                   .execute();
                                   
           System.out.println("updated line="+updatedCount);
           
``` 

# Make SQL Selection 
After SQL instance has been prepared successfully, you can use them to perform SQL selection.
```java
           List<User> users = sql.select(User.class)
                       .where("firstname")
                       .like("%Jephte%")
                       .execute();
                       
           for (User u : users) {
              System.out.println(u.firstName);
           }
```   
It is also possible to make multiple nested selections:
```java
           /*
            this is my first SQLite Selection
           */
               SQLiteSelection selection1 = 
                        sql.select(User.class)
                       .where("firstname")
                       .like("%Jephte%");
                 
           /*
             this is my second SQLite Selection
           */
               SQLiteSelection selection2 = 
                       sql.select(User.class)
                      .where("firstname")
                      .like("%Julie%");
           
           /*
            this is my third SQLite Selection
           */
              SQLiteSelection selection3 = 
                      sql.select(User.class)
                     .where("firstname")
                     .like("%Julie%");
           
               List<User> users =
                          sql.select(User.class)
                         .WHERE(selection1)
                         .AND(selection2)
                         .OR(selection3)
                         .execute();

               for (User u : users) {
                   System.out.println(u.firstName);
               }
```
# Using SQL function inside where clause args.
```java
      List<User> users = sql.select(User.class)
                           .where("UPPER(firstname)")//you can put all SQL functions combination.
                           .equalTo("JEPHTE")
                           .execute();
```
# Using HAVING and GROUP BY.
Let consider **Purchase.class** defined by:
```java
     public static class Purchase {
            @SQLiteModel.PrimaryKey(policy = SQLiteModel.PrimaryKey.POLICY_AUTO_INCREMENT)
            int extra;
            int amount = 0;
            String clientName;
}
```
So it is possible to make selection using Having and Group By as SQL clause.
```java
    List<Purchase> purchases = sql.select(Purchase.class)
                            .groupBy("clientName")
                            .having("SUM(amount)")// or use having("SUM","amouclientNament")
                            .greatThan(8)
                            .orHaving("COUNT", "clientName")// or use orHaving("COUNT","clientName")
                            .greatThan(10)
                            .limit(5)
                            .execute();
```
# Make a selection with specified columns.
```java
    /*
     create a string array which represent
     columns you want to 'Moisturize/ hydrate' from User table above.
    */
      String[] columns={"userName", "firstName"};

    /*
     I am selecting only 2 columns from User table: userName and firstName
     as defined by the String array.
    */
      List<User> users = sql.select(columns, User.class)
                           .where("firstname")
                           .like("%Jephte%")
                           .execute();
```
# Make a selection ResultSet class conversion
```java
    public static class PurchaseStatistic {
            int amountSum=0; //the purchase total sum
            int amountAvg = 0;// the purchase amount average
            int purchaseCount=0;//The purchase total count
            String clientName;//The client name.
    }
```

```java
    String[] columns={
                     "SUM(amount) as amountSum",
                     "AVG(amount) as amountAvg",
                     "COUNT(client) as purchaseCount",
                     "clientName"
                     };

    List<PurchaseStatistic> statistic = sql.select(columns, Purchase.class)
                              .groupBy("clientName")
                              .execute(PurchaseStatistic.class);
```
# Using JOIN with SQL Selection 
Make and SQL join using Library is "easily" possible.
Let consider three classes defined by: 
```java
     class House {
            int extra;
            String name;
            String type_id;
            String location_id;
            @SQLiteModel.OneToOne(mappedBy = "type_id")
            Type type;
            @SQLiteModel.OneToOne(mappedBy = "location_id")
            Location location;
        }
    
        class Location {
            int extra;
            String description;
            String name;
        }
    
        class Type {
            int extra;
            String libelle;
        }
```
You can perform join Query  like:
```java
   List<House> houses = sql.select(House.class)
                      .innerJoin(Type.class)
                      .leftJoin(Location.class)
                      .where(House.class, "extra")
                      .greatThan(2)
                      .and(Location.class, "name")
                      .equalTo("Abidjan")
                      .and(House.class, "extra")
                      .in(1, 2, 3, 4)
                      .execute();
 ```
 It is also possible to make custom Join definition.
 ```java
    List<House> houses = sql.select(House.class)
                 .innerJoin(Type.class)
                 .on(Type.class, "extra").equalTo(House.class, "type_id")
                 .leftJoin(Location.class)
                 .on(Location.class, "extra").equalTo(House.class, "location_id")
                 .where(House.class, "extra")
                 .in(1, 2, 3, 4)
                 .execute();
 ```
# Make an asynchronous SQL clause execution.
To perform async SQL clause execution, you just need to use executeAsync instead of execute.
```java
//in progress...
```

Usage
-----
All available public versions are:
* 1.0.0
* 1.1.0
    - 1.1.1
    - 1.1.2
    - 1.1.3
* 1.2.0
    - 1.2.1
    - 1.2.2
    - 1.2.3
    - 1.2.5
    - 1.2.6
    - 1.2.7
    - 1.2.8
    - 1.2.9
    - 1.3.0
    - 1.3.1
    
minSdkVersion = 10
------------------
Library is compatible with Android 2.3 and newer.

Download
--------
Just add the dependency to your `build.gradle`:

```groovy
dependencies {
   compile 'istat.android.data.access.sqlite:istat-access-sqlite:1.3.1'
}
```

or add the dependency to your pom.xml:

```xml
<dependency>
  <groupId>istat.android.data.access.sqlite</groupId>
  <artifactId>istat-access-sqlite</artifactId>
  <version>1.3.1</version>
  <type>pom</type>
</dependency>
```


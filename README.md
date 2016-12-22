# android-sqlite-access
android Library to help SQLite db query and Management using a easy and sweet query builder.

#Create Some class to persist.
```java
@SQLiteModel.Table(name = "User") //if not set, default is class.getSimpleName()
public class User {

    @Column(name="userName")//specify this field as Column and give it a name. if not set, default is the property label
    public String userName;
    
    public String firstName; //schould be persisted with label 'firstName'
    
    public int year;        //schould be persisted with label 'year'
    
    /**
    make this field as table primary key.
    ome thing to know is that, if your class doesn't has explicit primary key declaration but contain a
    property named 'id' (case not sensitive) it will be implicitelly considered as your primaryKey
    */
    @PrimaryKey 
    String id;
    
    @Ignore     //ignore this field when persisting and querying on Db.
    boolean readOnly=false
 }
```

#Add SQLite Connexion 
you can add one or many 'SQLiteConnexion' to your SQLite context. 
in this part, we will add connection to the Database defined by:
DbName="testDB"
DBVersion=1;
NB: it is strongly recommended  to make it on the onCreate of your <extends> android.app.Application class.
```java
    String DbName = "TestDB";
    SQLite.addConnection(new SQLite.SQLiteConnection(appContext, DbName, 1) {
        @Override
        public void onCreateDb(SQLiteDatabase db) {
        //here you can execute script to create your database from de SQLiteDataBase param instance
            try {
                SQLite.executeSQLScript(db, appContext.getResources().openRawResource(R.raw.test));//here, i am executing a script from my R.raw resource
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        @Override
        public void onUpgradeDb(SQLiteDatabase db, int oldVersion, int newVersion) {
        //here you can execute SQL script to update your database from de SQLiteDataBase param instance
            try {
                SQLite.executeSQLScript(db, appContext.getResources().openRawResource(R.raw.test));//here, i am executing a script from my R.raw resource
            } catch (IOException e) {
                e.printStackTrace();
            }
        }       
    });
 ```
 
#Prepare SQL instance
From de DbName given above (when adding connection) you can prepare SQL instance.
NB: SQL instance will be useful for perform SQL query.
 ```java
    SQLite.prepareSQL(DbName, new SQLite.PrepareHandler() {
        @Override
        public void onSQLReady(SQLite.SQL sql) {
           //when the prepare succeed, use the SQL instance to query for result.
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

# Make SQL Insert 
After SQL instance has been prepared successfully, you can use them to perform SQL Insert.
```java
    @Override
    public void onSQLReady(SQLite.SQL sql) {
       User user = new User();
       user.userName = "Toukea";
       user.firstName = "Jephte";
       user.year = 25;
                    
       long insertIds[] = sql.insert(user).execute();// Array List of last insert 'Id'
    }
 ```   
It is also possible to perform multiple insertions in one step
 ```java
     @Override
     public void onSQLReady(SQLite.SQL sql) {
       /*
       here, some multiple[3] user definitions
       */
                                   
        long insertIds[] = sql.insert(user0,user1,user2).execute();// Array List of last insert 'Id'
        
        System.out.println("user0 id= "+insertIds[0]);
        System.out.println("user1 id= "+insertIds[1]);
        System.out.println("user2 id= "+insertIds[2]);
        
     }
  ``` 
  
# Make SQL Delete 
 After SQL instance has been prepared successfully, you can use them to perform SQL delete.
 ```java
         @Override
         public void onSQLReady(SQLite.SQL sql) {
         
             int deletedCount = sql.delete(User.class)
                                .where("firstname")
                                .like("%Jephte%")
                                .execute();
                                
             System.out.println("deleted line="+deletedCount);
             
         }
  ```    
 
# Make SQL Update 
After SQL instance has been prepared successfully, you can use them to perform SQL update.
```java
           @Override
           public void onSQLReady(SQLite.SQL sql) {
                int updatedCount = sql.update(User.class)
                                   .set("userName", "newName")
                                   .where("firstName")
                                   .like("%Jephte%")
                                   .execute();
                System.out.println("updated line="+updatedCount);
           }
           
``` 
It is also possible to update from another model.
```java
           @Override
           public void onSQLReady(SQLite.SQL sql) {
                User userModel=new User();
                userModel.firstName="Julie";
                userModel.year=21;
                
                int updatedCount = sql.update(User.class)
                                   .setAs(userModel)
                                   .where("firstname")
                                   .like("%jephte%")
                                   .execute();
                                   
                System.out.println("updated line="+updatedCount);
           }
           
``` 

# Make SQL Selection 
After SQL instance has been prepared successfully, you can use them to perform SQL selection.
```java
           @Override
           public void onSQLReady(SQLite.SQL sql) {
           
               List<User> users = sql.select(User.class)
                       .where("firstname")
                       .like("%Jephte%")
                       .execute();
                       
               for (User u : users) {
                   System.out.println(u.firstName);
               }
           }
```   
It is also possible to make multiple nested selections:
```java
           @Override
           public void onSQLReady(SQLite.SQL sql) {
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
           }
```   
# Using JOIN with SQL Selection 
Make and SQL join using Library is "easily" possible.
Let consider three classes defined by: 
```java
     class House {
            int id;
            String name;
            String type_id;
            String location_id;
            @SQLiteModel.OneToOne(mappedBy = "type_id")
            Type type;
            @SQLiteModel.OneToOne(mappedBy = "location_id")
            Location location;
        }
    
        class Location {
            int id;
            String description;
            String name;
        }
    
        class Type {
            int id;
            String libelle;
        }
```
You can perform join Query  like:
```java
           List<House> houses = sql.select(House.class)
                              .innerJoin(Type.class)
                              .leftJoin(Location.class)
                              .where(House.class, "id")
                              .greatThan(2)
                              .and(Location.class, "name")
                              .equalTo("Abidjan")
                              .and(Maison.class, "id")
                              .in(1, 2, 3,4)
                              .execute();
```
Usage
-----
Just add the dependency to your `build.gradle`:

```groovy
dependencies {
   compile 'istat.android.data.access.sqlite:istat-access-sqlite:1.1.0'
}
```

minSdkVersion = 10
------------------
Library is compatible with Android 2.3 and newer.

Download
--------
add the dependency to your pom.xml:

```xml
<dependency>
  <groupId>istat.android.data.access.sqlite</groupId>
  <artifactId>istat-access-sqlite</artifactId>
  <version>1.1.0</version>
  <type>pom</type>
</dependency>
```


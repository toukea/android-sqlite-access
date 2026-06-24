# android-sqlite-access
android Library to help SQLite db Query and Management using a easy and sweet Query builder.

**istat-access-sqlite** is a lightweight ORM and fluent query builder over Android's native SQLite.
You map plain Java objects to tables, then read and write them with a chainable DSL — no raw SQL
strings, no code generation, no annotation processor at build time.

## Features at a glance
- **POJO mapping, minimal annotations** — any class becomes a table; `@Table` / `@Column` /
  `@PrimaryKey` are all optional (defaults derived from class and field names; an `id` field is the
  implicit primary key).
- **Fluent CRUD** — `insert`, `persist` (insert-or-update), `update`, `delete`, `select`, all chainable.
- **Rich SELECT** — `where`/`and`/`or`, nested sub-selections, `LIKE`/`IN`/comparators, SQL functions
  inside clauses, `GROUP BY` / `HAVING`, `DISTINCT`, `LIMIT`, custom projections and result-set class
  conversion.
- **Joins** — `innerJoin` / `leftJoin` with implicit `mappedBy` keys or explicit `on(...)`, plus table
  aliases for readable column qualifiers.
- **Relations** — `@OneToOne` / `@OneToMany` / `@ManyToOne` / `@ManyToMany` with embedded persistence.
- **Two hydration models** — classic field injection **and** constructor injection for **immutable**
  entities (`final` fields, no setters, no no-arg constructor).
- **Pluggable (de)serialization** — custom `Serializer` / `CursorReader`, with a Gson fallback for
  complex types.
- **Asynchronous execution** — `executeAsync(...)` variants to run queries off the main thread.
- **Schema helpers** — auto `CREATE TABLE` from a class via `TableUtils.create` / `drop`, plus a
  connection manager (`SQLite.addConnection` / `prepareSQL`).
- **R8-ready** — ships `consumer-rules.pro`; immutable entities keep working under minification.

## Table of contents
- [Define an entity](#create-some-class-to-persist)
- [Annotations reference](#annotations-reference)
- [Hydration strategies (immutable & legacy)](#hydration-strategies-reading-rows-back-into-objects)
- [Add a SQLite connection](#add-sqlite-connexion)
- [Get an SQL instance](#how-to-perform-query-from-your-db)
- [Insert](#make-sql-insert) · [Delete](#make-sql-delete) · [Update](#make-sql-update) · [Select](#make-sql-selection)
- [Joins](#using-join-with-sql-selection) · [Async execution](#make-an-asynchronous-sql-clause-execution)

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

# Annotations reference
All annotations are nested types of `SQLiteModel` (e.g. `@SQLiteModel.Column`). They are **optional**:
a plain POJO already maps by class and field names, with an implicit `id` primary key. `transient`
fields are never persisted.

| Annotation | Applies to | Purpose |
|---|---|---|
| `@Table(name)` | type | Custom table name (default: the class simple name). |
| `@Column(name, nullable)` | field, **parameter** | Custom column name. On a **constructor parameter** it maps that argument to a column (constructor-based hydration). |
| `@PrimaryKey(policy)` | field | Marks the primary key. `policy` ∈ `POLICY_DEFAULT` / `POLICY_AUTO_INCREMENT` / `POLICY_AUTO_GENERATE` / `POLICY_NONE`. If absent, a field named `id` (case-insensitive) is used. |
| `@Ignore(when)` | field | Excludes the field from persistence and queries. |
| `@NotNull` | field | Intent marker for non-null columns. |
| `@OneToOne` / `@OneToMany` / `@ManyToOne` / `@ManyToMany(mappedBy)` | field | Relationships with embedded persistence. `mappedBy` defaults to `<Type>_id`. |
| `@Link(type, mappedBy)` | field | Generic relationship link. |
| `@CreatorConstructor` | constructor | *(new)* Forces/disambiguates the constructor used to hydrate immutable entities. Optional — auto-detection works without it. |
| `@Persistable` | type | *(new)* Marker so the shipped `consumer-rules.pro` keeps the entity's constructors and fields under R8. |

# Hydration strategies (reading rows back into objects)
When a row is read from the database, the ORM turns it into an instance of your entity class using
one of two strategies. They are tried in this order and are fully interoperable — pick per entity.

## Strategy A — Constructor-based (immutable entities)
Lets you use **`final` fields, no setters and no no-arg constructor**. The ORM instantiates the row
by calling a constructor and passing each column value as an argument. A constructor parameter is
matched to a column in one of two ways:

1. **By annotation (recommended, obfuscation-proof, API-independent):** annotate the parameter with
   `@SQLiteModel.Column(name = "column_name")`.
2. **By parameter name:** if the parameter is not annotated, its name is used as the column name.
   This requires the entity to be compiled with `-parameters` (so `Parameter.isNamePresent()` is
   `true`; available on Android API ≥ 26).

```java
@SQLiteModel.Persistable                     // marker kept by R8 (see ProGuard below)
public class User {
    final String id;
    final String userName;
    final int year;

    @SQLiteModel.CreatorConstructor          // optional: forces/disambiguates the creator constructor
    User(@SQLiteModel.Column(name = "id") String id,
         @SQLiteModel.Column(name = "userName") String userName,
         @SQLiteModel.Column(name = "year") int year) {
        this.id = id;
        this.userName = userName;
        this.year = year;
    }
}
```

**Constructor selection.** Among constructors whose every parameter resolves to a known column, the
one covering the **most** columns is chosen. Ties prefer a `@SQLiteModel.CreatorConstructor`-annotated
constructor, then higher arity — annotate the intended one to remove ambiguity. A constructor may be
`private` (it is made accessible reflectively). If a `@CreatorConstructor` is present but not fully
resolvable, hydration fails fast with a clear message.

**Hybrid entities.** Columns not covered by the chosen constructor are completed by field injection
afterwards (only on non-`final` fields), so you can mix constructor args and mutable fields.

## Strategy B — Field injection (legacy, default fallback)
The historical behaviour, unchanged: the entity needs a **public no-arg constructor** and
**non-`final` fields**; columns are assigned by reflection on fields. Used automatically whenever no
exploitable creator constructor is found. Existing entities keep working without any change.

## Requirements
Constructor-based hydration uses `java.lang.reflect.Parameter`, available on **Android API ≥ 26**.
The `@Column` strategy works on any supported API; the parameter-name strategy additionally needs
`Parameter.isNamePresent() == true`, which requires the `-parameters` compiler flag (see below).
Strategy B (field injection) is unchanged and has no extra requirement.

## ProGuard / R8 (consumers)
This library ships `consumer-rules.pro` automatically (it is merged into the consuming app's R8
configuration), so in most cases you write **no** rule yourself. It keeps:

- `RuntimeVisibleAnnotations` + `RuntimeVisibleParameterAnnotations` — so `@Column` and the other ORM
  annotations stay readable by reflection at runtime;
- the three hydration annotation interfaces;
- the **constructors and fields** of every `@SQLiteModel.Persistable` class.

It deliberately does **not** ship `-keepattributes MethodParameters`: that attribute would retain
original parameter *names* app-wide and is only useful for the parameter-name strategy — which
already forces you to opt into `-parameters`. None of the shipped rules disable name obfuscation.

### Choosing a strategy under minification

| Strategy | Annotate parameters | App needs `-parameters` | Extra ProGuard rule in your app | Trade-off |
|---|---|---|---|---|
| **`@Column` (recommended)** | `@SQLiteModel.Column(name = …)` on every param | No | None (covered by the shipped rules) | Obfuscation-proof; no parameter names retained |
| **Parameter name** | none | **Yes** — `options.compilerArgs << '-parameters'` | **Yes** — `-keepattributes MethodParameters` | Retains parameter names of any `-parameters`-compiled module |

If you prefer not to annotate entities with `@SQLiteModel.Persistable`, keep their members with your
own rule instead (apps frequently already do this):

```proguard
-keep class com.your.app.entities.** { *; }
```

> **Security note.** Keeping `MethodParameters` only makes decompiled code slightly easier to read
> for modules compiled with `-parameters`; it does **not** weaken signature-based anti-tamper or
> license checks (those rely on the APK signature, not on bytecode attributes). In security-sensitive
> apps, prefer the `@Column` strategy so no parameter names are ever retained.

> Out of scope: relational fields (`@OneToOne`/`@OneToMany`/`@ManyToOne`/`@ManyToMany`) are not
> resolved as constructor parameters — keep them as fields (they are completed by the hybrid path).

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
After SQL instance has been prepared successfully, you can use it to perform SQL Insert.
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
        
        System.out.println("user0 id= "+insertIds[0]);
        System.out.println("user1 id= "+insertIds[1]);
        System.out.println("user2 id= "+insertIds[2]);
  ```

## Insert From A Selection
You can also duplicate rows using a `SELECT` statement while overriding columns on the fly. The next example copies the user name
while generating a random hexadecimal identifier thanks to SQLite's `hex(randomblob(16))` helper. `ArchivedUser` is a mapped
table that mirrors `User` and contains an extra `uuid` column.

```java
       SQLiteSelect copy = sql.select(new String[]{"userName"}, User.class)
                              .where("userName")
                              .equalTo("Toukea");

       int inserted = sql.insertSelection(ArchivedUser.class, copy)
                        .setExpression("uuid", "hex(randomblob(16))")
                        .execute();
```

# Make SQL Delete
 After SQL instance has been prepared successfully, you can use it to perform SQL delete.
 ```java
         int deletedCount = sql.delete(User.class)
                                .where("firstname")
                                .like("%Jephte%")
                                .execute();
                                
         System.out.println("deleted line="+deletedCount);
  ```    
 
# Make SQL Update 
After SQL instance has been prepared successfully, you can use it to perform SQL update.
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
After SQL instance has been prepared successfully, you can use it to perform SQL selection.
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
            int id;
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
                      .and(House.class, "id")
                      .in(1, 2, 3, 4)
                      .execute();
 ```
 It is also possible to make custom Join definition.
 ```java
    List<House> houses = sql.select(House.class)
                 .innerJoin(Type.class)
                 .on(Type.class, "id").equalTo(House.class, "type_id")
                 .leftJoin(Location.class)
                 .on(Location.class, "id").equalTo(House.class, "location_id")
                 .where(House.class, "id")
                 .in(1, 2, 3, 4)
                 .execute();
 ```
You can also alias your selection and joined tables to work with readable column qualifiers.

```java
List<Bookmark> bookmarks = sql.select(Bookmark.class)
        .as("my_bookmark")
        .leftJoin(BookmarkEntry.class)
        .as("my_bookmark_entry")
        .on(BookmarkEntry.class, "bookmark_id").equalTo(Bookmark.class, "id")
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


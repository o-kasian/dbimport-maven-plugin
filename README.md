#DataBase Import Maven Plugin


Flexible and powerfull maven plugin to insert data into newly created database. Could be usefull when using automatic schema generator (such as h2ddl), or for test purposes.

###Key Features:


* **Export** existing data from database **to json**
* **Import data** to existing schema
* **Conditional** import based on json datafile, database and business rules
* **JavaScript** scripting support with predicates support
* It is possible to **recalculate identifiers** when using sequences
* Data generation **on the fly** (without any preexisting datafiles) with your business rules

###Example configuration

First, we have to configure the plugin in your **pom.xml** (in ```build/plugins``` section).
Following example plugin configuration show's how to configure a plugin to use Oracle Database to export two tables - ```USER_PREFERENCE``` and ```USER```. ```USER_PREFERENCE``` will keep a foreign key to ```USER```

```xml
<plugin>
  <groupId>org.projasource.dbimport</groupId>
  <artifactId>dbimport-maven-plugin</artifactId>
  <configuration>
  
    <!-- DataBase Connection configuration -->
    <driverClass>oracle.jdbc.driver.OracleDriver</driverClass>
    <url>jdbc:oracle:thin:@127.0.0.1:1521/ORALOCAL</url>
    <user>USERNAME</user>
    <password>PASSWORD</password>
    <schema>SCHEMANAME</schema>

    <!-- Tables that would be exported as is -->
    <tableNames>
      <tableName>USER_PREFERENCE</tableName>
      <tableName>USER</tableName>
    </tableNames>

    <!-- Another way - is to use extended configuration, to export partial data -->
    <!--extraConditions>
      <USER_PREFERENCE>select * from USER_PREFERENCE where USER_ID in (select ID from USER where NAME = 'John')</USER_PREFERENCE>
      <USER>select * from USER where NAME = 'John'</USER>
    </extraConditions-->

    <!-- A place where to keep exported data -->
    <storage>${basedir}/src/main/resources/import-data.json</storage>
    
    <!-- JSON configuration file that keep a configuration for [data-import:import] goal (listed below) -->
    <importDescriptor>${basedir}/src/main/resources/import-configuration.json</importDescriptor>
  </configuration>
  <dependencies>

    <!-- jdbc client library -->
    <dependency>
      <groupId>com.oracle.jdbc</groupId>
      <artifactId>ojdbc6</artifactId>
      <version>11.2.0.3.0</version>
    </dependency>
  </dependencies>
</plugin>
```

With a following configuration you are now ready to run ```mvn data-import:export``` in your project home directory. Data will be exported to ```src/main/resources/import-data.json``` relative to your project's home directory (importDescriptor property). You can export any data with any structure (f.ex. making custom select from multiple tables), this will not be imported 'as is', but as you configure to use it (look into **extraConditions** property).

When you are done with export, it's time to write **import configuration**. Location of import configuration is controlled with `importDescriptor` property. ImportDescriptor - is a json file with a predefined structure.

```json
{
    "preconditions": [
        "console.log('Starting DB import...')",
        "sql.execute('delete from USER_PREFERENCE')",
        "sql.execute('delete from USER')"
    ],
    "postconditions": [
        "console.log('Finished DB import!')"
    ],
    "tables": {
        "USER_PREFERENCE": {
            "iterator": "tableSize()",
            "fields": {
                "USER_ID": "extByName('USER', 'ID')",
                "PREF_NAME": "field()",
                "PREF_DATA": "field()"
            }
        },
        "USER": {
            "iterator": "tableSize()",
            "fields": {
                "ID": "extId(oracle.nextval('USER_SEQUENCE'))",
                "NAME": "field()",
                "FAMILY_NAME": "field()",
                "FULL_NAME": "db.USER[iterator].NAME + ' ' + db.USER[iterator].FAMILY_NAME"
            }
        }
    }
}
```

In this example two tables are imported. **USER.ID** - is a value recieved from sequence `USER_SEQUENCE` and **USER.FULL_NAME** - is a concatanation of **USER.NAME** and **USER.FAMILY_NAME**, which are recieved from exported files.

All possible properties are listed in the table

|property key                      |description                                                          |required |
|----------------------------------|---------------------------------------------------------------------|---------|
|/preconditions                    |An array of expressions to be executed before import.                |false    |
|/postconditions                   |An array of expressions to be executed after import.                 |false    |
|/tables                           |A map of 'table_name' => 'table' to be used during import            |true     |
|/tables/{NAME}                    |A table with name == {NAME}.                                         |true     |
|/tables/{NAME}/iterator           |How many rows will be inserted.                                      |true     |
|/tables/{NAME}/fields             |Fields to be used in insert statements.                              |true     |
|/tables/{NAME}/fields/{FIELD}     |An expression to be evaluated as a field value.                      |true     |

For property values standard **javascript expressions** are used (with several predefined functions such as field, tableSize etc.), full reference of the objects and functions available is listed below.

* **stdout** - System.out
* **stderr** - System.err
* **db** - imported file, represented as javascript object.
* **env** - System.getProperties()
* **sql** - SqlQueryExecutor object. Can be used to execute sql, and fetch resultset.
* **project** - MavenProject object. Can be used to obtain project properties.
* **tableName** - A name of a table for current iteration.
* **fieldName** - A name of a field for current iteration.
* **iterator** - Current iteration (a numeral of row to be inserted)
* **oQuery(obj, expr)** - Simple predicates library. Author - http://danml.com/js/oquery.js
* **require(url)** - Used to load and eval JS file.
* **tableSize()** - An alias for ```db[tableName].length```
* **field()** - An alias for ```db[tableName][iterator][fieldName]```
* **extId(value)** - writes an extra column for relation, when **value** is assigned to column used in foreign key 
* **extByName(table, field)** - retrieves a new id (previously written with ```extId()```), related to current field with ```db[table][field] == field()``` 

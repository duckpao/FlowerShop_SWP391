import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.Properties;

/** Local-only migration runner; never prints database credentials. */
class ApplyShopFollowSql {
    public static void main(String[] args) throws Exception {
        var config = new Properties();
        try(var reader=Files.newBufferedReader(Path.of("src/main/resources/application.properties"),StandardCharsets.UTF_8)){config.load(reader);}
        var url=config.getProperty("spring.datasource.url");
        if(url == null || !url.startsWith("jdbc:mysql://localhost:3306/flower_shop_db"))
            throw new IllegalArgumentException("This runner is restricted to the local flower_shop_db database.");
        try(var connection=DriverManager.getConnection(url,config.getProperty("spring.datasource.username"),config.getProperty("spring.datasource.password"))){
            for(var file:args){
                var path=Path.of(file).toAbsolutePath().normalize();
                var allowed=Path.of("sql").toAbsolutePath().normalize();
                if(!path.startsWith(allowed))throw new IllegalArgumentException("Only SQL files in flowershop/sql are accepted.");
                var sql=Files.readString(path,StandardCharsets.UTF_8).replaceAll("(?m)^\\s*--.*$","");
                try(var statement=connection.createStatement()){
                    for(var command:sql.split(";")){if(!command.isBlank())statement.execute(command);}
                }
                System.out.println("Applied "+path.getFileName());
            }
            try(var statement=connection.createStatement();var result=statement.executeQuery("SELECT count(*) FROM Shops WHERE id LIKE 'follow-demo-shop-%'")){
                result.next();System.out.println("Demo shops: "+result.getInt(1));
            }
        }
    }
}

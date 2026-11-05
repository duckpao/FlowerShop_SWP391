import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;

/** Read schema and exercise test-only rows in a transaction that always rolls back. */
class CheckShopFollowDatabase {
    public static void main(String[] args) throws Exception {
        var config=new Properties();
        try(var reader=Files.newBufferedReader(Path.of("src/main/resources/application.properties"),StandardCharsets.UTF_8)){config.load(reader);}
        var url=config.getProperty("spring.datasource.url");
        if(url==null || !url.startsWith("jdbc:mysql://localhost:3306/flower_shop_db"))throw new IllegalArgumentException("Local database only");
        try(var c=DriverManager.getConnection(url,config.getProperty("spring.datasource.username"),config.getProperty("spring.datasource.password"))){
            var tables=Map.of(
                "Favorite_Shops",List.of("user_id","shop_id","created_date"),
                "Shop_Product_Publications",List.of("product_id","published_at"),
                "Shop_Product_Mails",List.of("id","product_id","shop_id","user_id","status","attempts","next_attempt_at","created_at","sent_at"));
            boolean valid=true;
            for(var table:tables.entrySet()){
                var present=new HashSet<String>();
                try(var rs=c.getMetaData().getColumns(c.getCatalog(),null,table.getKey(),null)){while(rs.next())present.add(rs.getString("COLUMN_NAME"));}
                var missing=new ArrayList<>(table.getValue());missing.removeAll(present);
                System.out.println(table.getKey()+": "+(missing.isEmpty()?"PASS":"MISSING "+missing));
                if(!missing.isEmpty())valid=false;
            }
            try(var s=c.createStatement();var r=s.executeQuery("SELECT status, count(*) FROM Shops GROUP BY status")){while(r.next())System.out.println("Shops "+r.getString(1)+": "+r.getInt(2));}
            String shop;
            try(var s=c.createStatement();var r=s.executeQuery("SELECT id FROM Shops WHERE status='ACTIVE' ORDER BY id LIMIT 1")){
                if(!r.next())throw new IllegalStateException("No ACTIVE shop available for follow testing");shop=r.getString(1);
            }
            String user=UUID.randomUUID().toString(),publication=UUID.randomUUID().toString(),mail=UUID.randomUUID().toString();
            c.setAutoCommit(false);
            try {
                try(var s=c.prepareStatement("INSERT INTO Users(id,email,full_name,role,is_email_verified,status) VALUES(?,?,'Database test','CUSTOMER',TRUE,'ACTIVE')")){s.setString(1,user);s.setString(2,user+"@example.test");s.executeUpdate();}
                try(var s=c.prepareStatement("INSERT INTO Favorite_Shops(user_id,shop_id) VALUES(?,?)")){s.setString(1,user);s.setString(2,shop);s.executeUpdate();}
                try(var s=c.prepareStatement("SELECT count(*) FROM Favorite_Shops f JOIN Shops s ON s.id=f.shop_id WHERE f.user_id=?")){s.setString(1,user);try(var r=s.executeQuery()){r.next();if(r.getInt(1)!=1)throw new AssertionError("Follow list mismatch");}}
                var point=c.setSavepoint();boolean duplicateRejected=false;
                try(var s=c.prepareStatement("INSERT INTO Favorite_Shops(user_id,shop_id) VALUES(?,?)")){s.setString(1,user);s.setString(2,shop);s.executeUpdate();}
                catch(SQLIntegrityConstraintViolationException ex){duplicateRejected=true;c.rollback(point);}
                if(!duplicateRejected)throw new AssertionError("Duplicate follow accepted");
                System.out.println("Follow insertion, personal list and duplicate key: PASS");
                try(var s=c.prepareStatement("DELETE FROM Favorite_Shops WHERE user_id=? AND shop_id=?")){s.setString(1,user);s.setString(2,shop);if(s.executeUpdate()!=1)throw new AssertionError("Unfollow failed");}
                System.out.println("Unfollow: PASS");
                if(valid){
                    try(var s=c.prepareStatement("INSERT INTO Shop_Product_Publications(product_id,published_at) VALUES(?,NOW())")){s.setString(1,publication);s.executeUpdate();}
                    try(var s=c.prepareStatement("INSERT INTO Shop_Product_Mails(id,product_id,shop_id,user_id,status,attempts,next_attempt_at,created_at) VALUES(?,?,?,?,'PENDING',0,NOW(),NOW())")){s.setString(1,mail);s.setString(2,publication);s.setString(3,shop);s.setString(4,user);s.executeUpdate();}
                    System.out.println("Publication and mail queue insertion: PASS");
                }else System.out.println("Publication and mail queue: BLOCKED by missing schema");
            } finally {c.rollback();c.setAutoCommit(true);}
            try(var s=c.prepareStatement("SELECT count(*) FROM Users WHERE id=?")){s.setString(1,user);try(var r=s.executeQuery()){r.next();if(r.getInt(1)!=0)throw new AssertionError("Test data remained");}}
            System.out.println("Rollback: PASS; no test users, follows, publications or emails retained; SMTP not called.");
        }
    }
}

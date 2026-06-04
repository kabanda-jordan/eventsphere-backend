import java.sql.*;

public class DbQuery {
    public static void main(String[] args) throws Exception {
        String url = "jdbc:postgresql://localhost:5432/eventsphere";
        String user = "postgres";
        String pass = "lock123";
        Class.forName("org.postgresql.Driver");
        try (Connection conn = DriverManager.getConnection(url, user, pass)) {
            System.out.println("Connected");
            DatabaseMetaData meta = conn.getMetaData();
            System.out.println("DB: " + meta.getDatabaseProductName() + " " + meta.getDatabaseProductVersion());
            try (Statement st = conn.createStatement()) {
                try (ResultSet rs = st.executeQuery("SELECT username, email, role, enabled FROM users")) {
                    while (rs.next()) {
                        System.out.printf("USER:%s|%s|%s|%s\n", rs.getString(1), rs.getString(2), rs.getString(3), rs.getBoolean(4));
                    }
                }
            }
        }
    }
}

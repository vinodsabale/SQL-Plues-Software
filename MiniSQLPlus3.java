package com.nit.AdvanceJava;


import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Scanner;

public class MiniSQLPlus3 {
    // ANSI Colors
    private static final String RESET = "\u001B[0m", BOLD = "\u001B[1m";
    private static final String RED = "\u001B[31m", GREEN = "\u001B[32m";
    private static final String YELLOW = "\u001B[33m", CYAN = "\u001B[36m";

    public static void main(String[] args) {
        try (Scanner sc = new Scanner(System.in)) {
            // Get credentials
            System.out.print(BOLD + "Username: " + RESET);
            String user = sc.nextLine().trim();
            
            String pass = readPassword(sc);
            
            // Connect and run
            System.out.println(YELLOW + "\nConnecting..." + RESET);
            try (Connection con = DriverManager.getConnection(
                    "jdbc:oracle:thin:@localhost:1521/orcl", user, pass);
                 Statement st = con.createStatement()) {
                
                System.out.println(GREEN + "Connected!\n" + RESET);
                con.setAutoCommit(false);
                
                runSQLLoop(sc, con, st);
                
            } catch (SQLException e) {
                System.out.println(RED + "\nConnection failed: " + e.getMessage() + RESET);
            }
        }
    }
    
    private static String readPassword(Scanner sc) {
        var console = System.console();
        if (console != null) {
            return new String(console.readPassword(BOLD + "Password: " + RESET));
        }
        System.out.print(BOLD + "Password: " + RESET);
        return sc.nextLine().trim();
    }
    
    private static void runSQLLoop(Scanner sc, Connection con, Statement st) {
        while (true) {
            System.out.print(RED + "SQL> " + RESET);
            String query = sc.nextLine().trim();
            
            if (query.isEmpty()) continue;
            
            String cmd = query.toUpperCase();
            
            // Handle exit
            if (cmd.equals("EXIT") || cmd.equals("QUIT")) {
                System.out.println(CYAN + "\nDisconnected." + RESET);
                break;
            }
            
            // Handle transaction commands
            if (handleTransaction(con, cmd)) continue;
            
            // Execute SQL
            try {
                if (cmd.startsWith("SELECT")) {
                    executeQuery(st, query);
                } else if (cmd.equals("SHOW USER")) {
                    showUser(st);
                } else if (cmd.startsWith("DESC")) {
                    describeTable(st, query.substring(cmd.startsWith("DESCRIBE") ? 9 : 5).trim());
                } else {
                    executeDML(st, query, cmd);
                }
            } catch (SQLException e) {
                System.out.println(RED + "Error: " + e.getMessage() + RESET);
            }
        }
    }
    
    private static boolean handleTransaction(Connection con, String cmd) {
        try {
            if (cmd.equals("COMMIT")) {
                con.commit();
                System.out.println(GREEN + "Commit complete." + RESET);
                return true;
            }
            if (cmd.equals("ROLLBACK")) {
                con.rollback();
                System.out.println(GREEN + "Rollback complete." + RESET);
                return true;
            }
        } catch (SQLException e) {
            System.out.println(RED + "Error: " + e.getMessage() + RESET);
        }
        return false;
    }
    
    private static void executeQuery(Statement st, String query) throws SQLException {
        try (ResultSet rs = st.executeQuery(query)) {
            ResultSetMetaData md = rs.getMetaData();
            int cols = md.getColumnCount();
            
            // Headers
            System.out.println();
            for (int i = 1; i <= cols; i++) {
                System.out.printf(BOLD + YELLOW + "%-20s" + RESET, md.getColumnName(i));
            }
            System.out.println("\n" + "-".repeat(20 * cols));
            
            // Rows
            int count = 0;
            while (rs.next()) {
                for (int i = 1; i <= cols; i++) {
                    Object val = rs.getObject(i);
                    System.out.printf("%-20s", val != null ? val : "NULL");
                }
                System.out.println();
                count++;
            }
            
            System.out.println("\n" + CYAN + count + " row(s) selected." + RESET);
        }
    }
    
    private static void showUser(Statement st) throws SQLException {
        try (ResultSet rs = st.executeQuery("SELECT USER FROM dual")) {
            if (rs.next()) {
                System.out.println(CYAN + "USER is \"" + rs.getString(1) + "\"" + RESET);
            }
        }
    }
    
    private static void describeTable(Statement st, String table) throws SQLException {
        String query = "SELECT column_name, data_type, data_length, nullable " +
                      "FROM user_tab_columns WHERE table_name = ? ORDER BY column_id";
        
        try (PreparedStatement ps = st.getConnection().prepareStatement(query)) {
            ps.setString(1, table.toUpperCase());
            
            try (ResultSet rs = ps.executeQuery()) {
                System.out.println("\n" + BOLD + "Name                 Null?    Type" + RESET);
                System.out.println("-".repeat(50));
                
                boolean found = false;
                while (rs.next()) {
                    found = true;
                    String col = rs.getString(1);
                    String type = rs.getString(2);
                    int len = rs.getInt(3);
                    String nullable = rs.getString(4).equals("N") ? "NOT NULL" : "";
                    
                    if (type.contains("CHAR") || type.contains("VARCHAR")) {
                        type += "(" + len + ")";
                    }
                    
                    System.out.printf("%-20s %-8s %s%n", col, nullable, type);
                }
                
                if (!found) {
                    System.out.println(RED + "Table not found." + RESET);
                }
                System.out.println();
            }
        }
    }
    
    private static void executeDML(Statement st, String query, String cmd) throws SQLException {
        int rows = st.executeUpdate(query);
        
        if (cmd.startsWith("INSERT")) {
            System.out.println(GREEN + rows + " row(s) inserted." + RESET);
        } else if (cmd.startsWith("UPDATE")) {
            System.out.println(GREEN + rows + " row(s) updated." + RESET);
        } else if (cmd.startsWith("DELETE")) {
            System.out.println(GREEN + rows + " row(s) deleted." + RESET);
        } else if (cmd.contains("TABLE") || cmd.contains("USER") || 
                   cmd.startsWith("GRANT") || cmd.startsWith("REVOKE")) {
            System.out.println(GREEN + "Statement executed." + RESET);
        } else {
            System.out.println(GREEN + "Done." + RESET);
        }
    }
}
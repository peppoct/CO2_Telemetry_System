package iot.unipi.it;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CO2DBService {
	private static final Logger logger = LogManager.getLogger(CO2DBService.class);
		
	private final static String DB_DEFAULT_IP = "localhost";
    private final static String DB_DFAULT_PORT = "3306";
    private final static String DB_USER = "root";
    private final static String DB_PASSWORD = "root";
    private final static String DB_NAME = "CO2_Telemetry_System";

    private static Connection conn = null;
	private static CO2DBService instance = null;
	
	private CO2DBService() {}
	
    public static CO2DBService getInstance() {
        if (instance == null) {
            instance = new CO2DBService();
        }
        return instance;
    }
    
    public boolean cleanDB() {
    	String query =  "DELETE FROM sensors";
    	boolean success = true;
    	getConnection();
    	try (PreparedStatement ps = conn.prepareStatement(query);) 
    	{
    		int insertedRow = ps.executeUpdate();
    		
        } catch (SQLException se) {
        	System.out.println("Error in the delete sensor query! " + se);
        	success = false;
        }
    	
		return success;
    }
    
    private static void getConnection() {
    	String connStr = "jdbc:mysql://" + DB_DEFAULT_IP + ":" + DB_DFAULT_PORT + "/" + DB_NAME +
				"?zeroDateTimeBehavior=CONVERT_TO_NULL&serverTimezone=CET";
    	
    	if (conn == null) {
    		try {
    			conn = DriverManager.getConnection(connStr, DB_USER, DB_PASSWORD);
    			Statement s = conn.createStatement();
    			int Result = s.executeUpdate("CREATE DATABASE IF NOT EXISTS CO2_TELEMETRY_SYSTEM");
    			if (conn == null) {
    				System.out.println("Connection to DB failed");
    			}
    		} catch (SQLException s) {
    			System.out.println("MySQL Connection Failed! " + s);
    			conn = null;
    		}
    	}
    }
    
    
    public boolean addObservation(String sensor, long timestamp, int value) {
    	String query = "INSERT INTO observations (sensor, timestamp, value) VALUES (?, ?, ?);";
    	boolean success = true;
    	getConnection();
    	try (PreparedStatement ps = conn.prepareStatement(query);)
    	{
    		ps.setString(1, sensor);
    		ps.setLong(2, timestamp);
    		ps.setInt(3, value);
    		
    		int insertedRow = ps.executeUpdate();
    		if (insertedRow < 1) {
    			System.out.println("Add observation failed");
    			success = false;
    		}
    	} catch (SQLException se) {
    		System.out.println("Error during adding observation: " + se);
    	}
    	return success;
    }
    
    
    public boolean addSensor(String id) {
    	String query = "INSERT INTO sensors (id) VALUES (?);";
    	boolean success = true;
    	getConnection();
    	try (PreparedStatement ps = conn.prepareStatement(query);) {
    		ps.setString(1, id);
    		int insertedRow = ps.executeUpdate();
    		if (insertedRow < 1) {
    			System.out.println("Error during adding sendsor");
    			success = false;
    		}
    	} catch (SQLException se) {
    		System.out.println("Query error");
    		success = false;
    	}
    	return success;
    }
    
    
    public boolean updateSensorState(String sensor, short status) {
    	String query = "UPDATE sensors SET status=? WHERE id=?;";
    	boolean success = true;
    	getConnection();
    	try (PreparedStatement ps = conn.prepareStatement(query);)
    	{
    		ps.setShort(1, status);
    		ps.setString(2, sensor);
    		int insertedRow = ps.executeUpdate();
    		if(insertedRow < 1) {
    			System.out.println("Something wrong during add observation!");
    			success = false;
    		}
    	} catch (SQLException se) {
    		System.out.println("Error in the add observation query! " + se);
        	success = false;
    	}
    	return success;
    }
    
    public boolean checkSensorExistence(String sensor) {
    	String query = "SELECT id FROM sensors WHERE id=?;";
    	boolean success = false;
    	getConnection();
    	try (PreparedStatement ps = conn.prepareStatement(query);)
    	{
    		ps.setString(1, sensor);
    		ResultSet rs = ps.executeQuery();
    		while (rs.next()) {
    			success = true;
    		}
    	} catch (SQLException se) {
    		System.out.println("Error in the check sensor existence query");
    		success = true;
    	}
    	return success;
    }
	
}

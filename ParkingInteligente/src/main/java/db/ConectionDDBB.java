package db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Calendar;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import logic.Log;


public class ConectionDDBB
{
	public Connection obtainConnection(boolean autoCommit) throws NullPointerException
    {
        Connection con=null;
        int intentos = 5;
        for (int i = 0; i < intentos; i++) 
        {
        	Log.logdb.info("Attempt {} to connect to the database", i);
        	try
	          {
	            Context ctx = new InitialContext();
	            // Get the connection factory configured in Tomcat
	            DataSource ds = (DataSource) ctx.lookup("java:/comp/env/jdbc/parking");

	            // Obtiene una conexion
	            con = ds.getConnection();
				Calendar calendar = Calendar.getInstance();
				java.sql.Date date = new java.sql.Date(calendar.getTime().getTime());
	            Log.logdb.debug("Connection creation. Bd connection identifier: {} obtained in {}", con.toString(), date.toString());
	            con.setAutoCommit(autoCommit);
	        	Log.logdb.info("Conection obtained in the attempt: " + i);
	            i = intentos;
	          } catch (NamingException ex)
	          {
	            Log.logdb.error("Error getting connection while trying: {} = {}", i, ex); 
	          } catch (SQLException ex)
	          {
	            Log.logdb.error("ERROR sql getting connection while trying:{ }= {}", i, ex);
	            throw (new NullPointerException("SQL connection is null"));
	          }
		}        
        return con;
    }
    
    public void closeTransaction(Connection con)
    {
        try
          {
            con.commit();
            Log.logdb.debug("Transaction closed");
          } catch (SQLException ex)
          {
            Log.logdb.error("Error closing the transaction: {}", ex);
          }
    }
    
    public void cancelTransaction(Connection con)
    {
        try
          {
            con.rollback();
            Log.logdb.debug("Transaction canceled");
          } catch (SQLException ex)
          {
            Log.logdb.error("ERROR sql when canceling the transation: {}", ex);
          }
    }

    public void closeConnection(Connection con)
    {
        try
          {
        	Log.logdb.info("Closing the connection");
            if (null != con)
              {
				Calendar calendar = Calendar.getInstance();
				java.sql.Date date = new java.sql.Date(calendar.getTime().getTime());
	            Log.logdb.debug("Connection closed. Bd connection identifier: {} obtained in {}", con.toString(), date.toString());
                con.close();
              }

        	Log.logdb.info("The connection has been closed");
          } catch (SQLException e)
          {
        	  Log.logdb.error("ERROR sql closing the connection: {}", e);
        	  e.printStackTrace();
          }
    }
    
    public static PreparedStatement getStatement(Connection con,String sql)
    {
        PreparedStatement ps = null;
        try
          {
            if (con != null)
              {
                ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);

              }
          } catch (SQLException ex)
          {
    	        Log.logdb.warn("ERROR sql creating PreparedStatement:{} ", ex);
          }

        return ps;
    }   
    
  //************** CALLS TO THE DATABASE ***************************//
    public static PreparedStatement GetParkings(Connection con)
    {
    	return getStatement(con,"SELECT * FROM parking");
    }
    
    public static PreparedStatement GetParkingsFromCity(Connection con)
    {
    	return getStatement(con,"SELECT * FROM parking WHERE id_ciudad=?");
    }
    
    public static PreparedStatement GetParkingSensors(Connection con)
    {
    	return getStatement(con,"SELECT * FROM sensor WHERE id_parking=?;");
    }
    
    //falta
    public static PreparedStatement GetParkingHistoricoMedicionesLastDays(Connection con)
    {
    	return getStatement(con,"SELECT date(DATE) as date, min(VALUE) as min, max(VALUE) as max, avg(VALUE) as avg, dayofweek(DATE) as dayofweek FROM MEASUREMENT WHERE STATION_ID=? AND SENSORTYPE_ID=? and date(DATE)>=date(now()) - INTERVAL ? DAY and DATE<=now() group by date(DATE) ORDER BY DATE ASC;");  	
    }
    //falta
    public static PreparedStatement GetParkingHistoricoMedicionesLastMonths(Connection con)
    {
    	return getStatement(con,"SELECT month(DATE) as month,min(VALUE) as min, max(VALUE) as max, avg(VALUE) as avg FROM MEASUREMENT WHERE STATION_ID=? AND SENSORTYPE_ID=? and date(DATE)>=date(now()) - INTERVAL ? DAY group by month(DATE) ORDER BY DATE ASC;");  	
    }
    
    public static PreparedStatement GetCities(Connection con)
    {
    	return getStatement(con,"SELECT * FROM ciudad;");
    }
    //falta
    public static PreparedStatement GetParkingHistoricoMedicionesMonth(Connection con)
    {
    	return getStatement(con,"SELECT month(DATE) as date,  min(VALUE) as min, max(VALUE) as max, avg(VALUE) as avg FROM MEASUREMENT WHERE STATION_ID=? AND SENSORTYPE_ID=? group by month(DATE) ORDER BY DATE ASC;");  	
    }
    
    public static PreparedStatement GetUlimasMediciones(Connection con)
    {
    	return getStatement(con,"select * from historico_mediciones where id_sensor=? ORDER BY fecha LIMIT 1;");
    }
    
    public static PreparedStatement GetUltimosCoches(Connection con)
    {
    	return getStatement(con,"select * from historico_coches where id_parking=? ORDER BY fecha LIMIT 1;");
    }
    
    public static PreparedStatement GetInfoFromParking(Connection con)
    {
    	return getStatement(con,"SELECT * FROM parking WHERE id_parking=?;");
    }
    
    public static PreparedStatement InsertMedicion(Connection con)
    {
    	return getStatement(con,"INSERT INTO historico_mediciones (id_sensor, fecha, valor, alerta) VALUES (?,?,?,?) ON duplicate key update id_sensor=?, fecha=?, valor=?, alerta=?;");  	
    }
    
    public static PreparedStatement InsertMatriculas(Connection con)
    {
    	return getStatement(con,"INSERT INTO historico_coches (fecha, matricula, Entrada, id_parking) VALUES (?,?,?,?) ON duplicate key update fecha=?, matricula=?, Entrada=?, id_parking=?;");  	
    }
    
    

    public static PreparedStatement GetDataBD(Connection con)
    {
    	return getStatement(con,"SELECT * FROM parkig.historico_mediciones");
    }
    
    public static PreparedStatement SetDataBD(Connection con)
    {
    	return getStatement(con,"INSERT INTO parking.historico_mediciones VALUES (?,?)");
    }
    
    //************** CALLS TO THE DATABASE ***************************//
    public static PreparedStatement GetMonthTempFromParking(Connection con)
    {
    	PreparedStatement ps = getStatement(con,"SELECT DATE_FORMAT(historico_mediciones.fecha, '%Y-%m-%d') as fecha, AVG(historico_mediciones.valor) as media_valor\n"
    			+ "FROM historico_mediciones\n"
    			+ "JOIN sensor ON historico_mediciones.id_sensor = sensor.id_sensor\n"
    			+ "JOIN parking ON sensor.id_parking = parking.id_parking\n"
    			+ "JOIN ciudad ON parking.id_ciudad = ciudad.id_ciudad\n"
    			+ "WHERE sensor.id_tipo = ?\n"
    			+ "AND ciudad.id_ciudad = ?\n"
    			+ "AND parking.id_parking = ?\n"
    			+ "AND historico_mediciones.fecha >= NOW() - INTERVAL 30 DAY\n"
    			+ "GROUP BY DATE_FORMAT(historico_mediciones.fecha, '%Y-%m-%d');");
    	Log.log.debug(ps);
    return ps;
    }  
    public static PreparedStatement GetMonthGasesFromParking(Connection con)
    {
    	return getStatement(con,"SELECT * FROM Measurement JOIN Sensor ON Measurement.sensor = Sensor.id JOIN Parking ON Sensor.parking = Parking.id JOIN City ON Parking.ciudad = City.id WHERE Sensor.tipo = 'gas' AND City.id = ? AND Parking.id = ? AND Measurement.timestamp >= NOW() - INTERVAL 30 DAY;");  	
    }  
    public static PreparedStatement GetMonthAlarmsFromParking(Connection con)
    {
    	return getStatement(con,"SELECT * FROM Measurement JOIN Sensor ON Measurement.sensor = Sensor.id JOIN Parking ON Sensor.parking = Parking.id JOIN City ON Parking.ciudad = City.id WHERE Measurement.alerta = true AND City.id = ? AND Parking.id = ? AND Measurement.timestamp >= NOW() - INTERVAL 30 DAY;");  	
    }  
    public static PreparedStatement GetMonthCarHistoryFromParking(Connection con)
    {
    	return getStatement(con,"SELECT * FROM CarHistory JOIN Parking ON CarHistory.parking = Parking.id JOIN City ON Parking.ciudad = City.id WHERE City.id = ? AND Parking.id = ? AND CarHistory.timestamp >= NOW() - INTERVAL 30 DAY;");  	
    }  
    public static PreparedStatement GetParkingTimeDayFromParking(Connection con)
    {
    	return getStatement(con,"SELECT * FROM CarHistory JOIN Parking ON CarHistory.parking = Parking.id JOIN City ON Parking.ciudad = City.id WHERE City.id = ? AND Parking.id = ? AND DATE(CarHistory.timestamp) = CURRENT_DATE;");  	
    }  
    
    public static PreparedStatement InsertMeasurement(Connection con) {
    return getStatement(con, 
        "INSERT INTO historico_mediciones (id_sensor, fecha, valor, alerta) VALUES (?,?,?,?) ON DUPLICATE KEY UPDATE id_sensor=?, fecha=?, valor=?, alerta=?;");
    }

    public static PreparedStatement GetLastValueStationSensor(Connection con)
    {
    	return getStatement(con,"select * from MEASUREMENT where id_parking=? AND id_sensor= ? ORDER BY fecha LIMIT 1;");  	
    }
    
    public static PreparedStatement GetInfoFromStation(Connection con)
    {
    	return getStatement(con,"SELECT * FROM PARKING.SENSOR WHERE id_parking=?;");  	
    }
    
    
    public static PreparedStatement GetActualCarHistoryFromParking(Connection con)
    {
    	return getStatement(con,"SELECT * FROM historico_coches JOIN parking ON historico_coches.id_parking = parking.id_parking JOIN ciudad ON parking.id_ciudad = ciudad.id_ciudad WHERE ciudad.id_ciudad = ? AND parking.id_parking = ? AND date(historico_coches.fecha) = current_date();");  	
    }
    
    public static PreparedStatement GetActualGasesFromParking(Connection con)
    {
    	return getStatement(con,"SELECT historico_mediciones.id_sensor,historico_mediciones.fecha,historico_mediciones.valor,historico_mediciones.alerta\n"
    			+ "FROM historico_mediciones\n"
    			+ "JOIN sensor ON historico_mediciones.id_sensor = sensor.id_sensor\n"
    			+ "JOIN parking ON sensor.id_parking = parking.id_parking\n"
    			+ "JOIN ciudad ON parking.id_ciudad = ciudad.id_ciudad\n"
    			+ "WHERE sensor.id_tipo = ?\n"
    			+ "  AND ciudad.id_ciudad = ?\n"
    			+ "  AND parking.id_parking = ?\n"
    			+ "ORDER BY historico_mediciones.fecha DESC\n"
    			+ "LIMIT 1;");  	
    }
    
    public static PreparedStatement GetActualHumidityFromParking(Connection con)
    {
    	return getStatement(con,"SELECT historico_mediciones.id_sensor,historico_mediciones.fecha,historico_mediciones.valor,historico_mediciones.alerta\n"
    			+ "FROM historico_mediciones\n"
    			+ "JOIN sensor ON historico_mediciones.id_sensor = sensor.id_sensor\n"
    			+ "JOIN parking ON sensor.id_parking = parking.id_parking\n"
    			+ "JOIN ciudad ON parking.id_ciudad = ciudad.id_ciudad\n"
    			+ "WHERE sensor.id_tipo = ?\n"
    			+ "  AND ciudad.id_ciudad = ?\n"
    			+ "  AND parking.id_parking = ?\n"
    			+ "ORDER BY historico_mediciones.fecha DESC\n"
    			+ "LIMIT 1;");  	
    }
    
    public static PreparedStatement GetActualTempFromParking(Connection con)
    {
    	return getStatement(con,"SELECT historico_mediciones.id_sensor,historico_mediciones.fecha,historico_mediciones.valor,historico_mediciones.alerta\n"
    			+ "FROM historico_mediciones\n"
    			+ "JOIN sensor ON historico_mediciones.id_sensor = sensor.id_sensor\n"
    			+ "JOIN parking ON sensor.id_parking = parking.id_parking\n"
    			+ "JOIN ciudad ON parking.id_ciudad = ciudad.id_ciudad\n"
    			+ "WHERE sensor.id_tipo = ?\n"
    			+ "  AND ciudad.id_ciudad = ?\n"
    			+ "  AND parking.id_parking = ?\n"
    			+ "ORDER BY historico_mediciones.fecha DESC\n"
    			+ "LIMIT 1;");  	
    }
    
}

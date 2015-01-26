package i5.las2peer.services.annotations.database;

import java.sql.Connection;
import java.sql.SQLException;

import com.arangodb.ArangoConfigure;
import com.arangodb.ArangoDriver;

import org.apache.commons.dbcp2.BasicDataSource;

/**
 * This class manages database credentials and provides connection from a connection pooling system
 *
 */
public class DatabaseManager {

	private static ArangoDriver arangoDriver;

	public DatabaseManager(String username, String password, String host, String port,
			String database) {
		
		/*
		// prepare and configure data source
		dataSource = new BasicDataSource();
		dataSource.setDefaultAutoCommit(true);
		dataSource.setDriverClassName(jdbcDriverClassName);
		dataSource.setUsername(jdbcLogin);
		dataSource.setPassword(jdbcPass);
		dataSource.setUrl(jdbcUrl + jdbcSchema);
		dataSource.setValidationQuery("SELECT 1");
		dataSource.setDefaultQueryTimeout(1000);
		dataSource.setMaxConnLifetimeMillis(100000);*/
		
		// Initialize configure
		int db_port = Integer.parseInt(port);
		  ArangoConfigure configure = new ArangoConfigure();
		  configure.setHost(host);
		  configure.setPort(db_port);
		  configure.setUser(username);
		  configure.setPassword(password);
		  configure.init();

		  // Create Driver (this instance is thread-safe)
		 arangoDriver = new ArangoDriver(configure, database);	  
		  
		
	}

	public ArangoDriver getConnection() throws SQLException {
		return arangoDriver;
	}

}

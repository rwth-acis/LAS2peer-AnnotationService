package i5.las2peer.services.annotations;

import i5.las2peer.api.Service;
import i5.las2peer.restMapper.HttpResponse;
import i5.las2peer.restMapper.MediaType;
import i5.las2peer.restMapper.RESTMapper;
import i5.las2peer.restMapper.annotations.ContentParam;
import i5.las2peer.restMapper.annotations.DELETE;
import i5.las2peer.restMapper.annotations.GET;
import i5.las2peer.restMapper.annotations.POST;
import i5.las2peer.restMapper.annotations.PUT;
import i5.las2peer.restMapper.annotations.Path;
import i5.las2peer.restMapper.annotations.PathParam;
import i5.las2peer.restMapper.annotations.Produces;
import i5.las2peer.restMapper.annotations.QueryParam;
import i5.las2peer.restMapper.annotations.Version;
import i5.las2peer.restMapper.annotations.swagger.ApiInfo;
import i5.las2peer.restMapper.annotations.swagger.ApiResponses;
import i5.las2peer.restMapper.annotations.swagger.ApiResponse;
import i5.las2peer.restMapper.annotations.swagger.Notes;
import i5.las2peer.restMapper.annotations.swagger.ResourceListApi;
import i5.las2peer.restMapper.annotations.swagger.Summary;
import i5.las2peer.restMapper.tools.ValidationResult;
import i5.las2peer.restMapper.tools.XMLCheck;
import i5.las2peer.security.Context;
import i5.las2peer.security.UserAgent;
import i5.las2peer.services.annotations.database.DatabaseManager;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import com.arangodb.ArangoDriver;
import com.arangodb.entity.EdgeDefinitionEntity;
import com.arangodb.entity.GraphEntity;
import com.mysql.jdbc.ResultSetMetaData;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;
import net.minidev.json.parser.ParseException;

/**
 * LAS2peer Service
 * 
 * This is a LAS2peer service used to save details of videos uploaded in sevianno3
 * that uses the LAS2peer Web-Connector for RESTful access to it.
 * 
 */
@Path("annotations")
@Version("0.1")
@ApiInfo(
		title="Video details",
		description="<p>A RESTful service for saving details of the uploaded videos.</p>", 
				termsOfServiceUrl="",
				contact="bakiu@dbis.rwth-aachen.de",
				license="",
				licenseUrl=""
		)
public class AnnotationsClass extends Service {
	
	private String port;
	private String host;
	private String username;
	private String password;
	private String database;
	private String enableCURLLogger;
	private DatabaseManager dbm;
	
	private String epUrl;

	public AnnotationsClass() {
		// read and set properties values
		setFieldValues();
		
		if(!epUrl.endsWith("/")){
			epUrl += "/";
		}
		// instantiate a database manager to handle database connection pooling and credentials
		dbm = new DatabaseManager(username, password, host, port, database);
	}
	

	/**
	 * Function to validate a user login.
	 * @return HttpRespons
	 * 
	 */
	@GET
	@Produces(MediaType.TEXT_HTML)
	@Path("validation")
	@ResourceListApi(description = "Check the user")
	@Summary("Return a greeting for the logged in user")
	@Notes("This is an example method")
	@ApiResponses(value={
			@ApiResponse(code = 200, message = "The user is logged in"),
	})
	public HttpResponse validateLogin() {
		String returnString = "";
		returnString += "You are " + ((UserAgent) getActiveAgent()).getLoginName() + " !";
		
		HttpResponse res = new HttpResponse(returnString);
		res.setStatus(200);
		return res;
	}
	
		/**
	 * Method that retrieves the video details from the database 
	 * and return an HTTP response including a JSON object.
	 * 
	 * @return HttpResponse
	 * 
	 */
	@GET
	@Path("annotation")
	@ResourceListApi(description = "Return details for a selected video")
	@Summary("return a JSON with video details stored for the given VideoID")
	@Notes("query parameter selects the columns that need to be returned in the JSON.")
	@ApiResponses(value={
			@ApiResponse(code = 200, message = "Video details"),
			@ApiResponse(code = 404, message = "Video id does not exist"),
			@ApiResponse(code = 500, message = "Internal error"),
	})
	public HttpResponse getDatabaseDetails() {
		String result = "";
		String columnName="";
		String selectquery ="";
		int columnCount = 0;
		ArangoDriver conn = null;
		PreparedStatement stmnt = null;
		ResultSet rs = null;
		ResultSetMetaData rsmd = null;
		try {
			// get connection from connection pool
			conn = dbm.getConnection();
			conn.getDatabases(username, password);
			
			
			// prepare statement			
				
				// return HTTP Response on success
				HttpResponse r = new HttpResponse(conn.toString());
				r.setStatus(200);
				return r;
				
			
		} catch (Exception e) {
			// return HTTP Response on error
			HttpResponse er = new HttpResponse("Internal error: " + e.getMessage());
			er.setStatus(500);
			return er;
		} finally {
			// free resources
			if (rs != null) {
				try {
					rs.close();
				} catch (Exception e) {
					Context.logError(this, e.getMessage());
					
					// return HTTP Response on error
					HttpResponse er = new HttpResponse("Internal error: " + e.getMessage());
					er.setStatus(500);
					return er;
				}
			}
			if (stmnt != null) {
				try {
					stmnt.close();
				} catch (Exception e) {
					Context.logError(this, e.getMessage());
					
					// return HTTP Response on error
					HttpResponse er = new HttpResponse("Internal error: " + e.getMessage());
					er.setStatus(500);
					return er;
				}
			}
			
		}
	}
	
	/**
	 * 
	 * @return HttpResponse 
	 */
	
	@PUT
	@Path("graphs")
	@Summary("Insert new graph")
	@Notes("Requires authentication.")
	@ApiResponses(value={
			@ApiResponse(code = 200, message = "Graph saved successfully."),
			@ApiResponse(code = 401, message = "User is not authenticated."),
			@ApiResponse(code = 409, message = "Graph already exists."),
			@ApiResponse(code = 500, message = "Internal error.")	
	})
	public HttpResponse addNewGraph(){
		
		String result = "";
		ArangoDriver conn = null;
		PreparedStatement stmnt = null;
		ResultSet rs = null;
		java.util.Date date= new java.util.Date();
		try {
			JSONObject o;
			conn= dbm.getConnection();
			if(getActiveAgent().getId() == getActiveNode().getAnonymous().getId()){
				// Edge definitions of the graph
				List<EdgeDefinitionEntity> edgeDefinitions = new ArrayList<EdgeDefinitionEntity>();
				
				// We start with one edge definition:
				EdgeDefinitionEntity edgeDefHasWritten = new EdgeDefinitionEntity();

				// Define the edge collection...
				edgeDefHasWritten.setCollection("HasWritten");

				// ... and the vertex collection(s) where an edge starts...
				List<String> from = new ArrayList<String>();
				from.add("Person");
				edgeDefHasWritten.setFrom(from);

				// ... and ends.
				List<String> to = new ArrayList<String>();
				to.add("Publication");
				edgeDefHasWritten.setTo(to);

				// add the edge definition to the list
				edgeDefinitions.add(edgeDefHasWritten);

				// We do not need any orphan collections, so this is just an empty list
				List<String> orphanCollections = new ArrayList<String>();
				
				// Create the graph:
				GraphEntity graphNew = conn.createGraph("Academical", edgeDefinitions, orphanCollections, true);
				
				result = "Name:2";
				
				// return 
				HttpResponse r = new HttpResponse(result);
				r.setStatus(200);
				return r;
				
			}else{
				result = "User in not authenticated";
				
				// return 
				HttpResponse r = new HttpResponse(result);
				r.setStatus(401);
				return r;		
			}
			
		} catch (Exception e) {
			// return HTTP Response on error
			HttpResponse er = new HttpResponse("Internal error: " + e.getMessage());
			er.setStatus(500);
			return er;
		} finally {
			// free resources if exception or not
			if (rs != null) {
				try {
					rs.close();
				} catch (Exception e) {
					Context.logError(this, e.getMessage());
					
					// return HTTP Response on error
					HttpResponse er = new HttpResponse("Internal error: " + e.getMessage());
					er.setStatus(500);
					return er;
				}
			}
			if (stmnt != null) {
				try {
					stmnt.close();
				} catch (Exception e) {
					Context.logError(this, e.getMessage());
					
					// return HTTP Response on error
					HttpResponse er = new HttpResponse("Internal error: " + e.getMessage());
					er.setStatus(500);
					return er;
				}
			}
			if (conn != null) {
				try {
					conn = null;
				} catch (Exception e) {
					Context.logError(this, e.getMessage());
					
					// return HTTP Response on error
					HttpResponse er = new HttpResponse("Internal error: " + e.getMessage());
					er.setStatus(500);
					return er;
				}
			}
		}
	}
	

	
	
	
	// ================= Swagger Resource Listing & API Declarations =====================

		@GET
		@Path("api-docs")
		@Summary("retrieve Swagger 1.2 resource listing.")
		@ApiResponses(value={
				@ApiResponse(code = 200, message = "Swagger 1.2 compliant resource listing"),
				@ApiResponse(code = 404, message = "Swagger resource listing not available due to missing annotations."),
		})
		@Produces(MediaType.APPLICATION_JSON)
		public HttpResponse getSwaggerResourceListing(){
			return RESTMapper.getSwaggerResourceListing(this.getClass());
		}

		@GET
		@Path("api-docs/{tlr}")
		@Produces(MediaType.APPLICATION_JSON)
		@Summary("retrieve Swagger 1.2 API declaration for given top-level resource.")
		@ApiResponses(value={
				@ApiResponse(code = 200, message = "Swagger 1.2 compliant API declaration"),
				@ApiResponse(code = 404, message = "Swagger API declaration not available due to missing annotations."),
		})
		public HttpResponse getSwaggerApiDeclaration(@PathParam("tlr") String tlr){
			return RESTMapper.getSwaggerApiDeclaration(this.getClass(),tlr, epUrl);
		}

	/**
	 * Method for debugging purposes.
	 * Here the concept of restMapping validation is shown.
	 * It is important to check, if all annotations are correct and consistent.
	 * Otherwise the service will not be accessible by the WebConnector.
	 * Best to do it in the unit tests.
	 * To avoid being overlooked/ignored the method is implemented here and not in the test section.
	 * @return  true, if mapping correct
	 */
	public boolean debugMapping() {
		String XML_LOCATION = "./restMapping.xml";
		String xml = getRESTMapping();

		try {
			RESTMapper.writeFile(XML_LOCATION, xml);
		} catch (IOException e) {
			e.printStackTrace();
		}

		XMLCheck validator = new XMLCheck();
		ValidationResult result = validator.validate(xml);

		if (result.isValid())
			return true;
		return false;
	}

	/**
	 * This method is needed for every RESTful application in LAS2peer. There is no need to change!
	 * 
	 * @return the mapping
	 */
	public String getRESTMapping() {
		String result = "";
		try {
			result = RESTMapper.getMethodsAsXML(this.getClass());
		} catch (Exception e) {

			e.printStackTrace();
		}
		return result;
	}

}

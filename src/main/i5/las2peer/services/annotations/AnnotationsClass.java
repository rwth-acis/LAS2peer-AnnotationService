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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.arangodb.ArangoDriver;
import com.arangodb.ArangoException;
import com.arangodb.entity.CursorEntity;
import com.arangodb.entity.DeletedEntity;
import com.arangodb.entity.DocumentEntity;
import com.arangodb.entity.EdgeDefinitionEntity;
import com.arangodb.entity.EdgeEntity;
import com.arangodb.entity.GraphEntity;
import com.arangodb.util.MapBuilder;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;
import net.minidev.json.parser.ParseException;

/**
 * LAS2peer Service
 * 
 * This is a LAS2peer service used to save details of videos uploaded in
 * sevianno3 that uses the LAS2peer Web-Connector for RESTful access to it.
 * 
 */
@Path("annotations")
@Version("0.1")
@ApiInfo(title = "Video details", description = "<p>A RESTful service for saving details of the uploaded videos.</p>", termsOfServiceUrl = "", contact = "bakiu@dbis.rwth-aachen.de", license = "", licenseUrl = "")
public class AnnotationsClass extends Service {

	private String port;
	private String host;
	private String username;
	private String password;
	private String database;
	private String enableCURLLogger;
	private DatabaseManager dbm;
	
	private final static int SUCCESSFUL = 200;
	private final static int SUCCESSFUL_INSERT = 201;
	private final static int SUCCESSFUL_INSERT_EDGE = 202;
	private final static String HANDLE = "_id";
	
	private String epUrl;
	
	GraphEntity graphNew;

	public AnnotationsClass() {
		// read and set properties values
		setFieldValues();

		if (!epUrl.endsWith("/")) {
			epUrl += "/";
		}
		// instantiate a database manager to handle database connection pooling
		// and credentials
		dbm = new DatabaseManager(username, password, host, port, database);
	}

	/**
	 * Function to validate a user login.
	 * 
	 * @return HttpRespons
	 * 
	 */
	@GET
	@Produces(MediaType.TEXT_HTML)
	@Path("validation")
	@ResourceListApi(description = "Check the user")
	@Summary("Return a greeting for the logged in user")
	@Notes("This is an example method")
	@ApiResponses(value = { @ApiResponse(code = 200, message = "The user is logged in"), })
	public HttpResponse validateLogin() {
		String returnString = "";
		returnString += "You are "
				+ ((UserAgent) getActiveAgent()).getLoginName() + " !";

		HttpResponse res = new HttpResponse(returnString);
		res.setStatus(200);
		return res;
	}

	/**
	 * Method that retrieves the video details from the database and return an
	 * HTTP response including a JSON object.
	 * 
	 * @return HttpResponse
	 * 
	 */
	@GET
	@Path("annotation")
	@ResourceListApi(description = "Return details for a selected video")
	@Summary("return a JSON with video details stored for the given VideoID")
	@Notes("query parameter selects the columns that need to be returned in the JSON.")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Video details"),
			@ApiResponse(code = 404, message = "Video id does not exist"),
			@ApiResponse(code = 500, message = "Internal error"), })
	public HttpResponse getDatabaseDetails() {
		ArangoDriver conn = null;
		PreparedStatement stmnt = null;
		ResultSet rs = null;
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
			HttpResponse er = new HttpResponse("Internal error: "
					+ e.getMessage());
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
					HttpResponse er = new HttpResponse("Internal error: "
							+ e.getMessage());
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
					HttpResponse er = new HttpResponse("Internal error: "
							+ e.getMessage());
					er.setStatus(500);
					return er;
				}
			}

		}
	}

	/**
	 * Create new Graph
	 * 
	 * @param graphData Graph details that need to be saved. The data come in a JSON format
	 *            
	 * @return HttpResponse
	 */

	@PUT
	@Path("graph")
	@Summary("Insert new graph")
	@Notes("Requires authentication. JSON format {\"graphName\": \"Video\", \"collection\": \"Videos\", \"collection\": \"Annotations\", \"from\":\"Videos\", \"to\": \"Annotations\", \"edgeCollection\": \"newAnnotated\"}")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Graph saved successfully."),
			@ApiResponse(code = 401, message = "User is not authenticated."),
			@ApiResponse(code = 409, message = "Graph already exists."),
			@ApiResponse(code = 500, message = "Internal error.") })
	public HttpResponse addNewGraph(@ContentParam String graphData) {

		String result = "";
		ArangoDriver conn = null;
		PreparedStatement stmnt = null;
		ResultSet rs = null;
		try {
			JSONObject o;
			try {
				o = (JSONObject) JSONValue.parseWithException(graphData);
			} catch (ParseException e1) {
				throw new IllegalArgumentException("Data is not valid JSON!");
			}
			conn = dbm.getConnection();
			if (getActiveAgent().getId() == getActiveNode().getAnonymous()
					.getId()) {
				// Edge definitions of the graph
				List<EdgeDefinitionEntity> edgeDefinitions = new ArrayList<EdgeDefinitionEntity>();

				// We start with one edge definition:
				EdgeDefinitionEntity edgeDefHasAnnotated = new EdgeDefinitionEntity();

				// Define the edge collection...
				Object edgeCollectionName = new String("edgeCollection");
				try {

					// if(o.containsKey(edgeCollectionName)){
					String edgeCollection = (String) o.get(edgeCollectionName);
					edgeDefHasAnnotated.setCollection(edgeCollection);
					// }
				} catch (Exception e) {
					// return HTTP Response on error
					HttpResponse er = new HttpResponse("Internal error: "
							+ "Missing JSON object member with key \""
							+ edgeCollectionName.toString() + "\"");
					er.setStatus(500);
					return er;
				}
				// ... and the vertex collection(s) where an edge starts...
				Object edgeFrom = new String("from");
				List<String> from = new ArrayList<String>();
				try {
					for (Object key: o.keySet()){
						if(key.toString().equals(edgeFrom.toString())){
							from.add((String) o.get(key));
						}
					}
				} catch (Exception e) {
					// return HTTP Response on error
					HttpResponse er = new HttpResponse("Internal error: "
							+ "Missing JSON object member with key \""
							+ edgeFrom.toString() + "\"");
					er.setStatus(500);
					return er;
				}
				if(from.isEmpty()){
					// return HTTP Response on error
					HttpResponse er = new HttpResponse("Internal error: "
							+ "Missing JSON object member with key \""
							+ edgeFrom.toString() + "\"");
					er.setStatus(500);
					return er;
				}
				edgeDefHasAnnotated.setFrom(from);

				// ... and ends.
				Object edgeTo = new String("to");
				List<String> to = new ArrayList<String>();
				try {
					for (Object key: o.keySet()){
						if(key.toString().equals(edgeTo.toString())){
							to.add((String) o.get(key));
						}
					}
				} catch (Exception e) {
					// return HTTP Response on error
					HttpResponse er = new HttpResponse("Internal error: "
							+ "Missing JSON object member with key \""
							+ edgeTo.toString() + "\"");
					er.setStatus(500);
					return er;
				}
				if(to.isEmpty()){
					// return HTTP Response on error
					HttpResponse er = new HttpResponse("Internal error: "
							+ "Missing JSON object member with key \""
							+ edgeTo.toString() + "\"");
					er.setStatus(500);
					return er;
				}
				edgeDefHasAnnotated.setTo(to);

				// add the edge definition to the list
				edgeDefinitions.add(edgeDefHasAnnotated);

				
				// Collections for the vertices
				Object vertexCollection = new String("collection");
				List<String> orphanCollections = new ArrayList<String>();
				try {
					for (Object key: o.keySet()){
						if(key.toString().equals(vertexCollection.toString())){
							orphanCollections.add((String) o.get(key));
						}
					}
				} catch (Exception e) {
					// return HTTP Response on error
					HttpResponse er = new HttpResponse("Internal error: "
							+ "Missing JSON object member with key \""
							+ vertexCollection.toString() + "\"");
					er.setStatus(500);
					return er;
				}				
				//orphanCollections.add("Videos");
				//orphanCollections.add("Annotations");

				// Create the graph:
				Object graphName = new String("graphName");
				try {

					// if(o.containsKey(edgeCollectionName)){
					String name = (String) o.get(graphName);
					graphNew = conn.createGraph(name, edgeDefinitions,
							orphanCollections, true);
					// }
				} catch (Exception e) {
					// return HTTP Response on error
					HttpResponse er = new HttpResponse("Internal error: "
							+ "Missing JSON object member with key \""
							+ graphName.toString() + "\"");
					er.setStatus(500);
					return er;
				}
				

				result = "Name: " + graphNew.getName() + "Key: "
						+ graphNew.getDocumentHandle();

				// return
				HttpResponse r = new HttpResponse(result);
				r.setStatus(200);
				return r;

			} else {
				result = "User in not authenticated";

				// return
				HttpResponse r = new HttpResponse(result);
				r.setStatus(401);
				return r;
			}

		} catch (Exception e) {
			// return HTTP Response on error
			HttpResponse er = new HttpResponse("Internal error: "
					+ e.getMessage());
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
					HttpResponse er = new HttpResponse("Internal error: "
							+ e.getMessage());
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
					HttpResponse er = new HttpResponse("Internal error: "
							+ e.getMessage());
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
					HttpResponse er = new HttpResponse("Internal error: "
							+ e.getMessage());
					er.setStatus(500);
					return er;
				}
			}
		}
	}

	/**
	 * Add new vertexes, for an (nonAnnotation) item.
	 * @param vertexData Vertex details that need to be saved. The data come in a JSON format
	 * @return HttpResponse
	 */

	/*@PUT
	@Path("vertex")
	@Summary("Insert new vertex")
	@Notes("Requires authentication.")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Vertex saved successfully."),
			@ApiResponse(code = 401, message = "User is not authenticated."),
			@ApiResponse(code = 409, message = "Vertex already exists."),
			@ApiResponse(code = 500, message = "Internal error.") })
	public HttpResponse addNewVertex() {

		String result = "";
		ArangoDriver conn = null;
		PreparedStatement stmnt = null;
		ResultSet rs = null;
		java.util.Date date = new java.util.Date();
		try {
			JSONObject o;
			conn = dbm.getConnection();
			if (getActiveAgent().getId() == getActiveNode().getAnonymous()
					.getId()) {

				
				 * id1 = conn.graphCreateVertex("Video", "Videos", new
				 * Video("1", "TestVideo1"), true); id2 =
				 * conn.graphCreateVertex("Video", "Videos", new Video("2",
				 * "TestVideo2"), true); ann1 = conn.graphCreateVertex("Video",
				 * "Annotations", new Annotation("1", "Annotation1"), true);
				 * ann2 = conn.graphCreateVertex("Video", "Annotations", new
				 * Annotation("2", "Annotation2"), true); ann3 =
				 * conn.graphCreateVertex("Video", "Annotations", new
				 * Annotation("3", "Annotation3"), true);
				 

				for (int i = 500; i < 1000; i++) {
					conn.graphCreateVertex("Video", "Videos",
							new Video(Integer.toString(i), "TestVideo"
									+ Integer.toString(i)), true);
				}
				conn = null;
				conn = dbm.getConnection();
				for (int i = 500; i < 1000; i++) {
					ann3 = conn.graphCreateVertex("Video", "Annotations",
							new Annotation(Integer.toString(i), "Annotation"
									+ Integer.toString(i)), true);
				}
				result = "Comleted Succesfully";

				// return
				HttpResponse r = new HttpResponse(result);
				r.setStatus(200);
				return r;

			} else {
				result = "User in not authenticated";

				// return
				HttpResponse r = new HttpResponse(result);
				r.setStatus(401);
				return r;
			}

		} catch (Exception e) {
			// return HTTP Response on error
			HttpResponse er = new HttpResponse("Internal error: "
					+ e.getMessage());
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
					HttpResponse er = new HttpResponse("Internal error: "
							+ e.getMessage());
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
					HttpResponse er = new HttpResponse("Internal error: "
							+ e.getMessage());
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
					HttpResponse er = new HttpResponse("Internal error: "
							+ e.getMessage());
					er.setStatus(500);
					return er;
				}
			}
		}
	}*/
	@PUT
	@Path("vertex")
	@Summary("Insert new vertex. This vertex stores only the id of an item. The item can be video, image"
			+ "or any think stored by another microservice. ")
	@Notes("Requires authentication. JSON format {\"graphName\": \"Video\", \"collection\": \"Videos\", \"id\": \"1\"}")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Vertex saved successfully."),
			@ApiResponse(code = 401, message = "User is not authenticated."),
			@ApiResponse(code = 409, message = "Vertex already exists."),
			@ApiResponse(code = 500, message = "Internal error.") })
	public HttpResponse addNewVertex(@ContentParam String vertexData) {

		String result = "";
		ArangoDriver conn = null;
		PreparedStatement stmnt = null;
		ResultSet rs = null;
		try {
			JSONObject o;
			try{	
				o = (JSONObject) JSONValue.parseWithException(vertexData);
			} catch (ParseException e1) {
				throw new IllegalArgumentException("data is not valid JSON!");
			}
			
			if (getActiveAgent().getId() == getActiveNode().getAnonymous()
					.getId()) {
				
				conn = dbm.getConnection();
				String title = "";
				String id = "";
				String graphName = "";
				String graphCollection = "";

				Object idObj = new String("id");
				try {

					// if(o.containsKey(edgeCollectionName)){
					id = (String) o.get(idObj);
					
					// }
				} catch (Exception e) {
					// return HTTP Response on error
					HttpResponse er = new HttpResponse("Internal error: "
							+ "Missing JSON object member with key \""
							+ idObj.toString() + "\"");
					er.setStatus(500);
					return er;
				}
				Object graphNameObj = new String("graphName");
				try {

					// if(o.containsKey(edgeCollectionName)){
					graphName = (String) o.get(graphNameObj);
					
					// }
				} catch (Exception e) {
					// return HTTP Response on error
					HttpResponse er = new HttpResponse("Internal error: "
							+ "Missing JSON object member with key \""
							+ graphNameObj.toString() + "\"");
					er.setStatus(500);
					return er;
				}
				Object graphCollectionObj = new String("collection");
				try {

					// if(o.containsKey(edgeCollectionName)){
					graphCollection = (String) o.get(graphCollectionObj);
					
					// }
				} catch (Exception e) {
					// return HTTP Response on error
					HttpResponse er = new HttpResponse("Internal error: "
							+ "Missing JSON object member with key \""
							+ graphCollectionObj.toString() + "\"");
					er.setStatus(500);
					return er;
				}
				
				DocumentEntity<Video> newVideo = conn.graphCreateVertex(graphName, graphCollection, new Video(id, title), true);
				
				if(newVideo.getCode() == SUCCESSFUL){
					result = "Comleted Succesfully";
					// return
					HttpResponse r = new HttpResponse(result);
					r.setStatus(200);
					return r;
					
				}else{
					// return HTTP Response on error
					HttpResponse er = new HttpResponse("Internal error: Cannot add vertex");
					er.setStatus(500);
					return er;
				}
				
			} else {
				result = "User in not authenticated";

				// return
				HttpResponse r = new HttpResponse(result);
				r.setStatus(401);
				return r;
			}

		} catch (Exception e) {
			// return HTTP Response on error
			HttpResponse er = new HttpResponse("Internal error: "
					+ e.getMessage());
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
					HttpResponse er = new HttpResponse("Internal error: "
							+ e.getMessage());
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
					HttpResponse er = new HttpResponse("Internal error: "
							+ e.getMessage());
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
					HttpResponse er = new HttpResponse("Internal error: "
							+ e.getMessage());
					er.setStatus(500);
					return er;
				}
			}
		}
	}
	
	
	/**
	 * Add new vertexes, for an Annotation item. The collection where this vertex is added is specified
	 * in the received JSON object. 
	 * @param annotationData Annotation details that need to be saved. The data come in a JSON format
	 * @return HttpResponse
	 */
	@PUT
	@Path("annotation")
	@Summary("Insert new annotation. This vertex stores data for a new Annotation.")
	@Notes("Requires authentication. JSON format \"graphName\": \"Video\", \"collection\": \"Annotations\", \"id\": \"1\", ...Additional data ")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Annotation saved successfully."),
			@ApiResponse(code = 401, message = "User is not authenticated."),
			@ApiResponse(code = 409, message = "Annotation already exists."),
			@ApiResponse(code = 500, message = "Internal error.") })
	public HttpResponse addNewAnnotation(@ContentParam String annotationData) {

		String result = "";
		ArangoDriver conn = null;
		PreparedStatement stmnt = null;
		ResultSet rs = null;
		try {
			JSONObject o;
			try{	
				o = (JSONObject) JSONValue.parseWithException(annotationData);
			} catch (ParseException e1) {
				throw new IllegalArgumentException("Data is not valid JSON!");
			}
			
			if (getActiveAgent().getId() == getActiveNode().getAnonymous()
					.getId()) {
				
				conn = dbm.getConnection();
				String graphName = "";
				String graphCollection = "";
				String id = "";
				/*Object titleObj = new String("title");
				try {

					// if(o.containsKey(edgeCollectionName)){
					title = (String) o.get(titleObj);
					
					// }
				} catch (Exception e) {
					// return HTTP Response on error
					HttpResponse er = new HttpResponse("Internal error: "
							+ "Missing JSON object member with key \""
							+ titleObj.toString() + "\"");
					er.setStatus(500);
					return er;
				}*/
				Object idObj = new String("id");
				try {

					// if(o.containsKey(edgeCollectionName)){
					id = (String) o.get(idObj);
					o.remove(idObj);
					
					// }
				} catch (Exception e) {
					// return HTTP Response on error
					HttpResponse er = new HttpResponse("Internal error: "
							+ "Missing JSON object member with key \""
							+ idObj.toString() + "\"");
					er.setStatus(500);
					return er;
				}
				Object graphNameObj = new String("graphName");
				try {

					// if(o.containsKey(edgeCollectionName)){
					graphName = (String) o.get(graphNameObj);
					o.remove(graphNameObj);
					
					// }
				} catch (Exception e) {
					// return HTTP Response on error
					HttpResponse er = new HttpResponse("Internal error: "
							+ "Missing JSON object member with key \""
							+ graphNameObj.toString() + "\"");
					er.setStatus(500);
					return er;
				}
				Object graphCollectionObj = new String("collection");
				try {

					// if(o.containsKey(edgeCollectionName)){
					graphCollection = (String) o.get(graphCollectionObj);
					o.remove(graphCollectionObj);
					
					// }
				} catch (Exception e) {
					// return HTTP Response on error
					HttpResponse er = new HttpResponse("Internal error: "
							+ "Missing JSON object member with key \""
							+ graphCollectionObj.toString() + "\"");
					er.setStatus(500);
					return er;
				}
				
				DocumentEntity<Annotation> newAnnotation = conn.graphCreateVertex(graphName, graphCollection, new Annotation(id, o), true);
				
				if(newAnnotation.getCode() == SUCCESSFUL_INSERT){
					result = "Comleted Succesfully";
					// return
					HttpResponse r = new HttpResponse(result);
					r.setStatus(200);
					return r;
					
				}else{
					// return HTTP Response on error
					HttpResponse er = new HttpResponse("Internal error: Cannot add annotation. Error Code " + newAnnotation.getCode() + ".");
					er.setStatus(500);
					return er;
				}
				
			} else {
				result = "User in not authenticated";

				// return
				HttpResponse r = new HttpResponse(result);
				r.setStatus(401);
				return r;
			}

		} catch (Exception e) {
			// return HTTP Response on error
			HttpResponse er = new HttpResponse("Internal error: "
					+ e.getMessage());
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
					HttpResponse er = new HttpResponse("Internal error: "
							+ e.getMessage());
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
					HttpResponse er = new HttpResponse("Internal error: "
							+ e.getMessage());
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
					HttpResponse er = new HttpResponse("Internal error: "
							+ e.getMessage());
					er.setStatus(500);
					return er;
				}
			}
		}
	}

	/**
	 * Add new edge
	 * @param edgeData Data for the edge we want to store.
	 * @return HttpResponse
	 */
	@PUT
	@Path("edge")
	@Summary("Insert new edge")
	@Notes("Requires authentication. {\"graphName\": \"Video\", \"collection\": \"newAnnotated\", \"id\": \"1\", \"source\": \"124\","
			+ " \"dest\": \"1\", \"destCollection\": \"Annotations\","
			+ " \"pos\": { \"x\": \"10\", \"y\": \"10\", \"z\": \"10\"}, "
			+ "\"startTime\": \"1.324\", \"duration\": \"0.40\" }")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Edge saved successfully."),
			@ApiResponse(code = 401, message = "User is not authenticated."),
			@ApiResponse(code = 409, message = "Edge already exists."),
			@ApiResponse(code = 500, message = "Internal error.") })
	public HttpResponse addNewEdge(@ContentParam String edgeData) {

		String result = "";
		ArangoDriver conn = null;
		try {
			JSONObject o;
			try{	
				o = (JSONObject) JSONValue.parseWithException(edgeData);
			} catch (ParseException e1) {
				throw new IllegalArgumentException("Data is not valid JSON!");
			}
			if (getActiveAgent().getId() == getActiveNode().getAnonymous()
					.getId()) {
				
				conn = dbm.getConnection();
				String graphName = "";
				String edgeCollection = "";
				String sourceId = "";
				String destId = "";
				String destCollection = "";
				String sourceHandle = "";
				String destHandle = "";
				
				Object graphNameObj = new String("graphName");
				Object edgeCollectionObj = new String("collection");
				Object edgeSourceObj = new String("source");
				Object edgeDestObj = new String("dest");
				Object destCollectionObj = new String("destCollection");
				
				//get the graph name from the Json 
				graphName = getKeyFromJSON(graphNameObj, o, true);
				//get the edge collection name from the Json 
				edgeCollection = getKeyFromJSON(edgeCollectionObj, o,true);
				sourceId = getKeyFromJSON(edgeSourceObj, o, true);
				destId = getKeyFromJSON(edgeDestObj, o, true);
				destCollection = getKeyFromJSON(destCollectionObj, o, true);
				
				if ( !graphName.equals("") && !edgeCollection.equals("") && !sourceId.equals("") && !destId.equals("") && !destCollection.equals("") ){
					//get source handle
					Map<String, Object> soruceVertexMap = new MapBuilder().put("id",sourceId).get();
					String getSourceVertexByID = "for i in GRAPH_VERTICES('"+ graphName +"', @id,{}) return i._id";
					Map<String, Object> bindVarsSource = new MapBuilder().put("id",soruceVertexMap).get();
					CursorEntity<String> resSourceById = conn.executeQuery(	getSourceVertexByID, bindVarsSource, String.class, true, 1);
					Iterator<String> iteratorSource = resSourceById.iterator();
					if (iteratorSource.hasNext()){
						sourceHandle=iteratorSource.next();
					}
					//get destination handle
					destHandle = getVertexHandle(destId, destCollection, graphName);
					
				}else{ //not correct json
					
				}
				
				//insert the new edge
				EdgeEntity<?> edge = conn.graphCreateEdge(graphName, edgeCollection, null, sourceHandle, destHandle, o, null);
				
				if (edge.getCode() == SUCCESSFUL_INSERT_EDGE){
					result = "Comleted Succesfully";
					// return
					HttpResponse r = new HttpResponse(result);
					r.setStatus(200);
					return r;
					
				}else{
					// return HTTP Response on error
					HttpResponse er = new HttpResponse("Internal error: Cannot add edge. Error Code " + edge.getCode() + ".");
					er.setStatus(500);
					return er;
				}
				
			} else {
				result = "User in not authenticated";

				// return
				HttpResponse r = new HttpResponse(result);
				r.setStatus(401);
				return r;
			}

		} catch (Exception e) {
			// return HTTP Response on error
			HttpResponse er = new HttpResponse("Internal error: "
					+ e.getMessage());
			er.setStatus(500);
			return er;
		} finally {
			if (conn != null) {
				try {
					conn = null;
				} catch (Exception e) {
					Context.logError(this, e.getMessage());

					// return HTTP Response on error
					HttpResponse er = new HttpResponse("Internal error: "
							+ e.getMessage());
					er.setStatus(500);
					return er;
				}
			}
		}
	}

	/*@PUT
	@Path("edge")
	@Summary("Insert new edge")
	@Notes("Requires authentication.")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Edge saved successfully."),
			@ApiResponse(code = 401, message = "User is not authenticated."),
			@ApiResponse(code = 409, message = "Edge already exists."),
			@ApiResponse(code = 500, message = "Internal error.") })
	public HttpResponse addNewEdge() {

		String result = "";
		ArangoDriver conn = null;
		PreparedStatement stmnt = null;
		ResultSet rs = null;
		java.util.Date date = new java.util.Date();
		ArrayList<String> videoID = new ArrayList<String>();
		ArrayList<String> annotationID;
		Random rand = new Random();
		try {
			JSONObject o;
			conn = dbm.getConnection();

			if (getActiveAgent().getId() == getActiveNode().getAnonymous()
					.getId()) {

				conn = null;
				conn = dbm.getConnection();

				String geteAnnotationID = "for i in GRAPH_VERTICES('Video',  null,{vertexCollectionRestriction : 'Annotations' }) return i._id";
				// Map<String, Object> bindVarseAnnotation = new
				// MapBuilder().put("id", exampleAnnotationMap).get();
				CursorEntity<String> reseAnnotationById = conn.executeQuery(
						geteAnnotationID, null, String.class, true, 500);
				Iterator<String> iteratorAnnotationById = reseAnnotationById
						.iterator();
				String annKey = "";
				annotationID = new ArrayList<String>();
				while (iteratorAnnotationById.hasNext()) {
					annotationID.add(iteratorAnnotationById.next());
				}

				conn = null;
				conn = dbm.getConnection();

				String getVertexByID = "for i in GRAPH_VERTICES('Video',  {},{vertexCollecitonRestriction : 'Videos' }) return i._id";
				CursorEntity<String> resVertexById = conn.executeQuery(
						getVertexByID, null, String.class, true, 500);
				Iterator<String> iteratorVertexById = resVertexById.iterator();
				String videoKey = "";

				while (iteratorVertexById.hasNext()) {
					videoID.add(iteratorVertexById.next());
				}

				conn = null;
				conn = dbm.getConnection();

				for (int i = 0; i < 400; i++) {
					int rand_indexVideo = Math.abs(rand.nextInt() % 500);
					int rand_indexAnnotation = Math.abs(rand.nextInt() % 500);
					double rand_startTime = rand.nextDouble();
					double rand_duration = rand.nextDouble();
					EdgeEntity<?> edge1 = conn.graphCreateEdge("Video",
							"newAnnotated", null, videoID.get(rand_indexVideo),
							annotationID.get(rand_indexAnnotation),
							new VideoEdge(rand_startTime, rand_duration), null);
					EdgeEntity<?> edge2 = conn.graphCreateEdge("Video",
							"newAnnotated", null, videoID.get(rand_indexVideo),
							annotationID.get(rand_indexAnnotation),
							new VideoEdge(rand_startTime, rand_duration), null);
				}

				// id1 = conn.graphCreateVertex("Video", "Videos", new
				// Video("503", "TestVideo503"), true);

				// Map<String, Object> bindVars = new MapBuilder().put("id",
				// exampleVertexMap).get();
				
				 * conn = null; conn= dbm.getConnection(); String
				 * geteAnnotationID =
				 * "for i in GRAPH_VERTICES('Video', {},{vertexCollecitonRestriction:'Annotations'}) return i._id"
				 * ; //Map<String, Object> bindVarseAnnotation = new
				 * MapBuilder().put("id", exampleAnnotationMap).get();
				 * CursorEntity<String> reseAnnotationById =
				 * conn.executeQuery(geteAnnotationID, null, String.class, true,
				 * 500); Iterator<String> iteratorAnnotationById =
				 * reseAnnotationById.iterator(); String annKey = "";
				 * while(iteratorAnnotationById.hasNext()) {
				 * annotationID.add(iteratorAnnotationById.next()); } conn =
				 * null; conn = dbm.getConnection(); for(int i = 0;i<1000;i++){
				 * Random r = new Random(); int indexVideo =
				 * Math.abs(r.nextInt() % 500); int indexAnnotation =
				 * Math.abs(r.nextInt() % 500);
				 * 
				 * VideoEdge ve = new VideoEdge(
				 * 10*r.nextDouble(),r.nextDouble()); EdgeEntity<?> edge1 =
				 * conn.graphCreateEdge("Video", "newAnnotated", null,
				 * videoID.get(indexVideo), annotationID.get(indexAnnotation),
				 * ve, true); EdgeEntity<?> edge2 = conn.graphCreateEdge(
				 * "Video", "newAnnotated", null, videoID.get(indexVideo),
				 * annotationID.get(indexAnnotation), new VideoEdge(
				 * 10*r.nextDouble(),r.nextDouble()), true);
				 * Logger.getLogger("Test"); }
				 

				
				 * EdgeEntity<String> edg1 = conn.graphCreateEdge("Video",
				 * "annotated", null, id1.getDocumentHandle(),
				 * ann1.getDocumentHandle()); EdgeEntity<String> edg2 =
				 * conn.graphCreateEdge("Video", "annotated", null,
				 * id1.getDocumentHandle(), ann3.getDocumentHandle());
				 * EdgeEntity<String> edg3 = conn.graphCreateEdge("Video",
				 * "annotated", null, id2.getDocumentHandle(),
				 * ann2.getDocumentHandle()); EdgeEntity<String> edg4 =
				 * conn.graphCreateEdge("Video", "annotated", null,
				 * id2.getDocumentHandle(), ann3.getDocumentHandle());
				 

				result = "Comleted Succesfully";

				// return
				HttpResponse r = new HttpResponse(result);
				r.setStatus(200);
				return r;

			} else {
				result = "User in not authenticated";

				// return
				HttpResponse r = new HttpResponse(result);
				r.setStatus(401);
				return r;
			}

		} catch (Exception e) {
			// return HTTP Response on error
			HttpResponse er = new HttpResponse("Internal error: "
					+ e.getMessage());
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
					HttpResponse er = new HttpResponse("Internal error: "
							+ e.getMessage());
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
					HttpResponse er = new HttpResponse("Internal error: "
							+ e.getMessage());
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
					HttpResponse er = new HttpResponse("Internal error: "
							+ e.getMessage());
					er.setStatus(500);
					return er;
				}
			}
		}
	}*/

	/**
	 * Method to update a vertex with a given id
	 * 
	 * @param vertexKey  key of the vertex we need to update
	 * @return HttpResponse with the result of the method
	 */

	@POST
	@Path("vertex/{vertexKey}")
	@Summary("update details for an existing vertex.")
	@Notes("Requires authentication. JSON: { \"graphName\": \"Video\", \"collection\": \"Videos\", \"title\": \"Updated Title :)\" }")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Vertex details updated successfully."),
			@ApiResponse(code = 401, message = "User is not authenticated."),
			@ApiResponse(code = 404, message = "Vertex not found."),
			@ApiResponse(code = 500, message = "Internal error.") })
	public HttpResponse updateVertex(	@PathParam("vertexKey") String vertexKey, @ContentParam String vertexData) {

		String result = "";
		ArangoDriver conn = null;
		try {
			JSONObject o;
			try {
				o = (JSONObject) JSONValue.parseWithException(vertexData);
			} catch (ParseException e1) {
				throw new IllegalArgumentException( "Data is not valid JSON!" );
			}
			if (getActiveAgent().getId() == getActiveNode().getAnonymous()
					.getId()) {
				
				conn = dbm.getConnection();
				String graphName = "";
				String vertexCollection = "";
				String vertexId = "";
				String vertexHandle = "";
				String vertexKeyDb = "";
				
				JSONObject vertexFromDB = null;
				
				Object graphNameObj = new String("graphName");
				Object vertexCollectionObj = new String("collection");
				//Object vertexIdObj = new String("id");
				
				
				//get the graph name from the Json 
				graphName = getKeyFromJSON(graphNameObj, o, true);
				//get the vertex collection name from the Json 
				vertexCollection = getKeyFromJSON(vertexCollectionObj, o,true);
				
				vertexId = vertexKey;
				
				if ( !vertexId.equals("") && ! graphName.equals("")){
					vertexFromDB = getVertexJSON(vertexId, vertexCollection, graphName);
					vertexHandle = getKeyFromJSON(new String(HANDLE), vertexFromDB, false);
				}
				
				//update the JSON according the new input data
				for (Object key: o.keySet()){
					if(vertexFromDB.containsKey(key))
					{
						vertexFromDB.remove(key);						
					}
					vertexFromDB.put((String)key,  o.get(key));
				}
				
				String [] vertexHandleSplit = vertexHandle.split("/"); 
				vertexKeyDb = vertexHandleSplit[1];
				
				DocumentEntity<?> updatedVertex = conn.graphUpdateVertex(graphName, vertexCollection, vertexKeyDb, vertexFromDB, true);
				
				if ( updatedVertex.getCode() == SUCCESSFUL_INSERT_EDGE){
				
					result = "Database updated.";

					// return
					HttpResponse r = new HttpResponse(result);
					r.setStatus(200);
					return r;
				} else {
					// return HTTP Response on error
					HttpResponse er = new HttpResponse("Internal error: Cannot update vertex. Error Code " + updatedVertex.getCode() + ".");
					er.setStatus(500);
					return er;
				}

			} else {
				result = "User in not authenticated";

				// return
				HttpResponse r = new HttpResponse(result);
				r.setStatus(401);
				return r;
			}

		} catch (Exception e) {
			// return HTTP Response on error
			HttpResponse er = new HttpResponse("Internal error: "
					+ e.getMessage());
			er.setStatus(500);
			return er;
		} finally {			
			if (conn != null) {
				try {
					conn = null;
				} catch (Exception e) {
					Context.logError(this, e.getMessage());

					// return HTTP Response on error
					HttpResponse er = new HttpResponse("Internal error: "
							+ e.getMessage());
					er.setStatus(500);
					return er;
				}
			}
		}
	}
	
	/**
	 * Method to update the an edge
	 * 
	 * @param edgeKey  key of the edge we need to update
	 * @return HttpResponse with the result of the method
	 */

	@POST
	@Path("edge/{edgeKey}")
	@Summary("update details for an existing edge.")
	@Notes("Requires authentication. JSON: { \"graphName\": \"Video\", \"collection\": \"newAnnotated\", "
			+ "\"pos\": { \"x\": \"10\", \"y\": \"10\", \"z\": \"10\"}, \"startTime\": \"1.324\", "
			+ "\"duration\": \"0.40\" }")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Vertex details updated successfully."),
			@ApiResponse(code = 401, message = "User is not authenticated."),
			@ApiResponse(code = 404, message = "Vertex not found."),
			@ApiResponse(code = 500, message = "Internal error.") })
	public HttpResponse updateEdge(	@PathParam("edgeKey") String edgeKey, @ContentParam String edgeData) {

		String result = "";
		ArangoDriver conn = null;
		try {
			JSONObject o;
			try {
				o = (JSONObject) JSONValue.parseWithException(edgeData);
			} catch (ParseException e1) {
				throw new IllegalArgumentException( "Data is not valid JSON!" );
			}
			if (getActiveAgent().getId() == getActiveNode().getAnonymous()
					.getId()) {
				
				conn = dbm.getConnection();
				String graphName = "";
				String edgeCollection = "";
				String edgeId = "";
				String edgeHandle = "";
				String edgeKeyDb = "";
				
				JSONObject edgeFromDB = null;
				
				Object graphNameObj = new String("graphName");
				Object edgeCollectionObj = new String("collection");
				
				
				//get the graph name from the Json 
				graphName = getKeyFromJSON(graphNameObj, o, true);
				//get the edge collection name from the Json 
				edgeCollection = getKeyFromJSON(edgeCollectionObj, o,true);
											
				edgeId = edgeKey;
				
				if ( !edgeId.equals("") && ! graphName.equals("")){					
					edgeFromDB = getEdgeJSON(edgeId, edgeCollection, graphName);
					edgeHandle = getKeyFromJSON(new String(HANDLE), edgeFromDB, false);
				}
								
				for (Object key: o.keySet()){
					if(edgeFromDB.containsKey(key))
					{
						edgeFromDB.remove(key);						
					}
					edgeFromDB.put((String)key,  o.get(key));
				}
				
				String [] edgeHandleSplit = edgeHandle.split("/"); 
				edgeKeyDb = edgeHandleSplit[1];
				
				
				EdgeEntity<?> updatedEdge = conn.graphUpdateEdge(graphName, edgeCollection, edgeKeyDb, edgeFromDB, true);
				
				if ( updatedEdge.getCode() == SUCCESSFUL_INSERT_EDGE){
				
					result = "Database updated.";

					// return
					HttpResponse r = new HttpResponse(result);
					r.setStatus(200);
					return r;
				} else {
					// return HTTP Response on error
					HttpResponse er = new HttpResponse("Internal error: Cannot update edge. Error Code " + updatedEdge.getCode() + ".");
					er.setStatus(500);
					return er;
				}

			} else {
				result = "User in not authenticated";

				// return
				HttpResponse r = new HttpResponse(result);
				r.setStatus(401);
				return r;
			}

		} catch (Exception e) {
			// return HTTP Response on error
			HttpResponse er = new HttpResponse("Internal error: "
					+ e.getMessage());
			er.setStatus(500);
			return er;
		} finally {			
			if (conn != null) {
				try {
					conn = null;
				} catch (Exception e) {
					Context.logError(this, e.getMessage());

					// return HTTP Response on error
					HttpResponse er = new HttpResponse("Internal error: "
							+ e.getMessage());
					er.setStatus(500);
					return er;
				}
			}
		}
	}
	
	/**
	 * Method to delete a vertex with a given id
	 * 
	 * @param vertexKey  key of the vertex we need to delete
	 * @return HttpResponse with the result of the method
	 */

	@DELETE
	@Path("vertex/{vertexKey}")
	@Summary("Delete an existing vertex.")
	@Notes("Requires authentication. JSON: { \"graphName\": \"Video\", \"collection\": \"Videos\" }")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Vertex deleted successfully."),
			@ApiResponse(code = 401, message = "User is not authenticated."),
			@ApiResponse(code = 404, message = "Vertex not found."),
			@ApiResponse(code = 500, message = "Internal error.") })
	public HttpResponse deleteVertex(	@PathParam("vertexKey") String vertexKey, @ContentParam String vertexData) {

		String result = "";
		ArangoDriver conn = null;
		try {
			JSONObject o;
			try {
				o = (JSONObject) JSONValue.parseWithException(vertexData);
			} catch (ParseException e1) {
				throw new IllegalArgumentException( "Data is not valid JSON!" );
			}
			if (getActiveAgent().getId() == getActiveNode().getAnonymous()
					.getId()) {
				
				conn = dbm.getConnection();
				String graphName = "";
				String vertexCollection = "";
				String vertexId = "";
				String vertexHandle = "";
				String vertexKeyDb = "";
				
				Object graphNameObj = new String("graphName");
				Object vertexCollectionObj = new String("collection");
				//Object vertexIdObj = new String("id");
				
				
				//get the graph name from the Json 
				graphName = getKeyFromJSON(graphNameObj, o, true);
				//get the vertex collection name from the Json 
				vertexCollection = getKeyFromJSON(vertexCollectionObj, o,true);
				
				vertexId = vertexKey;
				
				if ( !vertexId.equals("") && ! graphName.equals("")){
					vertexHandle = getVertexHandle(vertexId, vertexCollection, graphName);
				}
				String [] vertexHandleSplit = vertexHandle.split("/"); 
				vertexKeyDb = vertexHandleSplit[1];
				
				DeletedEntity deletedVertex = conn.graphDeleteVertex(graphName, vertexCollection, vertexKeyDb);
				if ( deletedVertex.getCode() == SUCCESSFUL_INSERT_EDGE && deletedVertex.getDeleted() == true){
				
					result = "Database updated.";

					// return
					HttpResponse r = new HttpResponse(result);
					r.setStatus(200);
					return r;
				} else {
					// return HTTP Response on error
					HttpResponse er = new HttpResponse("Internal error: Cannot update vertex. Error Code " + deletedVertex.getCode() + ".");
					er.setStatus(500);
					return er;
				}

			} else {
				result = "User in not authenticated";

				// return
				HttpResponse r = new HttpResponse(result);
				r.setStatus(401);
				return r;
			}

		} catch (Exception e) {
			// return HTTP Response on error
			HttpResponse er = new HttpResponse("Internal error: "
					+ e.getMessage());
			er.setStatus(500);
			return er;
		} finally {			
			if (conn != null) {
				try {
					conn = null;
				} catch (Exception e) {
					Context.logError(this, e.getMessage());

					// return HTTP Response on error
					HttpResponse er = new HttpResponse("Internal error: "
							+ e.getMessage());
					er.setStatus(500);
					return er;
				}
			}
		}
	}
	
	/**
	 * Method to delete the an edge
	 * 
	 * @param edgeKey  key of the edge we need to delete
	 * @return HttpResponse with the result of the method
	 */

	@DELETE
	@Path("edge/{edgeKey}")
	@Summary("Delete an existing edge.")
	@Notes("Requires authentication. JSON: { \"graphName\": \"Video\", \"collection\": \"newAnnotated\" }")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Vertex details updated successfully."),
			@ApiResponse(code = 401, message = "User is not authenticated."),
			@ApiResponse(code = 404, message = "Edge not found."),
			@ApiResponse(code = 500, message = "Internal error.") })
	public HttpResponse deleteEdge(	@PathParam("edgeKey") String edgeKey, @ContentParam String edgeData) {

		String result = "";
		ArangoDriver conn = null;
		try {
			JSONObject o;
			try {
				o = (JSONObject) JSONValue.parseWithException(edgeData);
			} catch (ParseException e1) {
				throw new IllegalArgumentException( "Data is not valid JSON!" );
			}
			if (getActiveAgent().getId() == getActiveNode().getAnonymous()
					.getId()) {
				
				conn = dbm.getConnection();
				String graphName = "";
				String edgeCollection = "";
				String edgeId = "";
				String edgeHandle = "";
				String edgeKeyDb = "";
				
				Object graphNameObj = new String("graphName");
				Object edgeCollectionObj = new String("collection");
				
				
				//get the graph name from the Json 
				graphName = getKeyFromJSON(graphNameObj, o, true);
				//get the edge collection name from the Json 
				edgeCollection = getKeyFromJSON(edgeCollectionObj, o,true);
				
				edgeId = edgeKey;
				
				if ( !edgeId.equals("") && ! graphName.equals("")){
					edgeHandle = getEdgeHandle(edgeId, edgeCollection, graphName);
				} //else {
					
				//}
				String [] edgeHandleSplit = edgeHandle.split("/"); 
				edgeKeyDb = edgeHandleSplit[1];
				
				DeletedEntity deletedEdge = conn.graphDeleteEdge(graphName, edgeCollection, edgeKeyDb);
				
				if ( deletedEdge.getCode() == SUCCESSFUL_INSERT_EDGE && deletedEdge.getDeleted()){
				
					result = "Database updated.";

					// return
					HttpResponse r = new HttpResponse(result);
					r.setStatus(200);
					return r;
				} else {
					// return HTTP Response on error
					HttpResponse er = new HttpResponse("Internal error: Cannot update edge. Error Code " + deletedEdge.getCode() + ".");
					er.setStatus(500);
					return er;
				}

			} else {
				result = "User in not authenticated";

				// return
				HttpResponse r = new HttpResponse(result);
				r.setStatus(401);
				return r;
			}

		} catch (Exception e) {
			// return HTTP Response on error
			HttpResponse er = new HttpResponse("Internal error: "
					+ e.getMessage());
			er.setStatus(500);
			return er;
		} finally {			
			if (conn != null) {
				try {
					conn = null;
				} catch (Exception e) {
					Context.logError(this, e.getMessage());

					// return HTTP Response on error
					HttpResponse er = new HttpResponse("Internal error: "
							+ e.getMessage());
					er.setStatus(500);
					return er;
				}
			}
		}
	}
	
	@GET
	@Path("annotations/{vertexId}")
	@ResourceListApi(description = "Return details for a selected graph")
	@Summary("return a JSON with graph details stored for the given graph Name")
	@Notes("query parameter selects the columns that need to be returned in the JSON.")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Vertex annotations"),
			@ApiResponse(code = 404, message = "Vertex id does not exist"),
			@ApiResponse(code = 500, message = "Internal error"), })
	public HttpResponse getVertexAnnotations(@PathParam("vertexId") String vertexId, @QueryParam(name = "part", defaultValue = "*" ) String part, @QueryParam(name = "name", defaultValue = "Video" ) String name, @QueryParam(name = "collection", defaultValue = "Videos" ) String collection) {
		String selectquery = "";
		ArangoDriver conn = null;
		JSONObject ro = null;
		JSONArray qs = new JSONArray();
		try {
			String vertexHandle = "";
			String graphName = "";
			String vertexCollection = "";
			
			JSONObject vertexFromDB = null;
			
			Object graphNameObj = new String("graphName");
			Object vertexCollectionObj = new String("collection");
			//Object vertexIdObj = new String("id");
			
			
			//get the graph name from the Json 
			graphName = name;
			//get the vertex collection name from the Json 
			vertexCollection = collection;

			
			String[] partsOfObject = part.split(",");
			
			conn = dbm.getConnection();
			
			vertexFromDB = getVertexJSON(vertexId, vertexCollection, graphName);
			vertexHandle = getKeyFromJSON(new String(HANDLE), vertexFromDB, false);
			
									
			String getAnnotations = "";
			if (partsOfObject[0].equals("*")){
				getAnnotations = "for i in GRAPH_NEIGHBORS('"+ graphName +"', @selectedVertex, {endVertexCollectionRestriction : 'Annotations'}) return i";
			} else {
				String selectParts = "{";
				for(String p:partsOfObject )
				{
					String partSmall = "'" + p + "': i." + p + ",";
					selectParts += partSmall;
				}
				//replace last character from ',' to '}'
				selectParts = selectParts.substring(0, selectParts.length()-1) + "}";
				
				getAnnotations = "for i in GRAPH_NEIGHBORS('"+ graphName +"', @selectedVertex, {endVertexCollectionRestriction : 'Annotations'}) return " + selectParts;
			}
			Map<String, Object> bindVars = new MapBuilder().put("selectedVertex", vertexFromDB).get();

			CursorEntity<JSONObject> resAnnotation = conn.executeQuery(getAnnotations, bindVars, JSONObject.class, true, 100);

			qs.add("Vertices that have edge with " + vertexId );
			Iterator<JSONObject> iteratorAnnotation = resAnnotation.iterator();
			while (iteratorAnnotation.hasNext()) {
				JSONObject annotation = (JSONObject) iteratorAnnotation.next();				
				qs.add(annotation);
			}
			// prepare statement

			// return HTTP Response on success
			HttpResponse r = new HttpResponse(qs.toJSONString());
			r.setStatus(200);
			return r;

		} catch (Exception e) {
			// return HTTP Response on error
			HttpResponse er = new HttpResponse("Internal error: "
					+ e.getMessage());
			er.setStatus(500);
			return er;
		} finally {			
			if (conn != null) {
				try {
					conn = null;
				} catch (Exception e) {
					Context.logError(this, e.getMessage());

					// return HTTP Response on error
					HttpResponse er = new HttpResponse("Internal error: "
							+ e.getMessage());
					er.setStatus(500);
					return er;
				}
			}
		}

	}
	
	/*@GET
	@Path("graph")
	@ResourceListApi(description = "Return details for a selected graph")
	@Summary("return a JSON with graph details stored for the given graph Name")
	@Notes("query parameter selects the columns that need to be returned in the JSON.")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Video details"),
			@ApiResponse(code = 404, message = "Video id does not exist"),
			@ApiResponse(code = 500, message = "Internal error"), })
	public HttpResponse getGraphDetails() {
		String selectquery = "";
		ArangoDriver conn = null;
		JSONObject ro = null;
		JSONArray qs = new JSONArray();
		try {
			// get connection from connection pool
			conn = dbm.getConnection();
			selectquery = "for v in GRAPH_VERTICES ('Video', {}) return v";
			String query = "for i in GRAPH_EDGES('Video', null) return i";

			CursorEntity<VideoEdge> res = conn.executeQuery(query, null,
					VideoEdge.class, true, 100);

			Iterator<VideoEdge> iterator = res.iterator();
			while (iterator.hasNext()) {
				ro = new JSONObject();
				VideoEdge edge = (VideoEdge) iterator.next();
				ro.put("From: ", edge.getFromCollection());
				ro.put("To: ", edge.getToCollection());
				ro.put("Start Time ", edge.getStartTime());
				ro.put("Duration ", edge.getDuration());
				qs.add(ro);
			}

			CursorEntity<Video> resVertex = conn.executeQuery(selectquery,
					null, Video.class, true, 500);
			Iterator<Video> iteratorVertex = resVertex.iterator();
			while (iteratorVertex.hasNext()) {
				ro = new JSONObject();
				Video v = (Video) iteratorVertex.next();
				ro.put("Id ", v.getId());
				ro.put("Title ", v.getTitle());
				qs.add(ro);
			}

			Map<String, Object> exampleVertexMap = new MapBuilder().put("id",
					"20").get();
			String getVertexByID = "for i in GRAPH_VERTICES('Video', @id,{vertexCollecitonRestriction:'Videos'}) return i";
			Map<String, Object> bindVars = new MapBuilder().put("id",
					exampleVertexMap).get();
			CursorEntity<Video> resVertexById = conn.executeQuery(
					getVertexByID, bindVars, Video.class, true, 1);
			Iterator<Video> iteratorVertexById = resVertexById.iterator();
			Video v = null;
			if (iteratorVertexById.hasNext()) {
				ro = new JSONObject();
				v = (Video) iteratorVertexById.next();
				ro.put("SelectedId ", v.getId());
				ro.put("SelectedTitle ", v.getTitle());
				qs.add(ro);
			}

			String getAnnotations = "for i in GRAPH_NEIGHBORS('Video', @selectedVideo, {endVertexCollectionRestriction : 'Annotations'}) return i.vertex";
			Map<String, Object> bindVars2 = new MapBuilder().put(
					"selectedVideo", v).get();

			CursorEntity<Annotation> resAnnotation = conn.executeQuery(
					getAnnotations, bindVars2, Annotation.class, true, 100);

			qs.add("Vertices that have inbound edge with TestVideo 20");
			Iterator<Annotation> iteratorAnnotation = resAnnotation.iterator();
			while (iteratorAnnotation.hasNext()) {
				ro = new JSONObject();
				Annotation a = (Annotation) iteratorAnnotation.next();
				ro.put("Id ", a.getId());
				ro.put("Title ", a.getTitle());
				qs.add(ro);
			}
			// prepare statement

			// return HTTP Response on success
			HttpResponse r = new HttpResponse(qs.toJSONString());
			r.setStatus(200);
			return r;

		} catch (Exception e) {
			// return HTTP Response on error
			HttpResponse er = new HttpResponse("Internal error: "
					+ e.getMessage());
			er.setStatus(500);
			return er;
		} finally {			
			if (conn != null) {
				try {
					conn = null;
				} catch (Exception e) {
					Context.logError(this, e.getMessage());

					// return HTTP Response on error
					HttpResponse er = new HttpResponse("Internal error: "
							+ e.getMessage());
					er.setStatus(500);
					return er;
				}
			}
		}

	}*/
	
	/**
	 * Read the value stored for the given key in the json input. The value is stored at value 
	 * and returns false if the given Json object does not contain the give key
	 * @param key vale of which is requested
	 * @param json input json
	 * @param remove if set removes the key from the json object.
	 * @return value	output value
	 */
	private String getKeyFromJSON(Object key, JSONObject json, boolean remove)
	{
		String value = "";
		if(json.containsKey(key)){
			value = (String) json.get(key);
			if(remove)
			{
				json.remove(key);
			}
			
		}
		return value;
	}
	
	private String getVertexHandle( String vertexId, String vertexCollection, String graphName ){
		ArangoDriver conn = null;
		String getSourceVertexByID = "";
		String vertexHandle = "";
		
		try {
			conn = dbm.getConnection();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		//get vertex handle
		//Map<String, Object> soruceVertexMap = new MapBuilder().put("id",vertexId).get();
		
		
		if ( vertexCollection.equals("")){
			getSourceVertexByID = "for i in GRAPH_VERTICES('"+ graphName +"', null,{}) FILTER i.id == '"+ vertexId +"' return i._id";
		} else {
			getSourceVertexByID = "for i in GRAPH_VERTICES('"+ graphName +"', null,{vertexCollectionRestriction : '"+ vertexCollection +"'}) FILTER i.id == '"+ vertexId +"' return i._id";
		}
		
		//Map<String, Object> bindVarsSource = new MapBuilder().put("id",soruceVertexMap).get();
		CursorEntity<String> resSourceById = null;
		try {
			resSourceById = conn.executeQuery(	getSourceVertexByID, null, String.class, true, 1);
		} catch (ArangoException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Iterator<String> iteratorSource = resSourceById.iterator();
		if (iteratorSource.hasNext()){
			vertexHandle = iteratorSource.next();
		}
		return vertexHandle;
	}
	
	private JSONObject getVertexJSON( String vertexId, String vertexCollection, String graphName ){
		ArangoDriver conn = null;
		String getSourceVertexByID = "";
		JSONObject vertex = null;
		
		try {
			conn = dbm.getConnection();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		//get vertex handle
		//Map<String, Object> soruceVertexMap = new MapBuilder().put("id",vertexId).get();
		
		
		if ( vertexCollection.equals("")){
			getSourceVertexByID = "for i in GRAPH_VERTICES('"+ graphName +"', null,{}) FILTER i.id == '"+ vertexId +"' return i";
		} else {
			getSourceVertexByID = "for i in GRAPH_VERTICES('"+ graphName +"', null,{vertexCollectionRestriction : '"+ vertexCollection +"'}) FILTER i.id == '"+ vertexId +"' return i";
		}
		
		//Map<String, Object> bindVarsSource = new MapBuilder().put("id",soruceVertexMap).get();
		CursorEntity<JSONObject> resSourceById = null;
		try {
			resSourceById = conn.executeQuery(	getSourceVertexByID, null, JSONObject.class, true, 1);
		} catch (ArangoException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Iterator<JSONObject> iteratorSource = resSourceById.iterator();
		if (iteratorSource.hasNext()){
			vertex = iteratorSource.next();
		}
		return vertex;
	}
	
	
	private String getEdgeHandle( String edgeId, String edgeCollection, String graphName ){
		ArangoDriver conn = null;
		String getSourceEdgeByID = "";
		String edgeHandle = "";
		try {
			conn = dbm.getConnection();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		//get edge handle
		//Map<String, Object> soruceEdgeMap = new MapBuilder().put("id",edgeId).get();		
		if ( edgeCollection.equals("")){
			getSourceEdgeByID = "for i in GRAPH_EDGES('"+ graphName +"', null, {}) FILTER i.id == '"+ edgeId +"' return i._id";
		} else {
			getSourceEdgeByID = "for i in GRAPH_EDGES('"+ graphName +"', null, {edgeCollectionRestriction : '"+ edgeCollection +"'}) FILTER i.id == '"+ edgeId +"' return i._id";
		}
		
		//Map<String, Object> bindVarsSource = new MapBuilder().put("id",soruceEdgeMap).get();
		CursorEntity<String> resSourceById = null;
		try {
			resSourceById = conn.executeQuery(	getSourceEdgeByID, null, String.class, true, 1);
		} catch (ArangoException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Iterator<String> iteratorSource = resSourceById.iterator();
		if (iteratorSource.hasNext()){
			edgeHandle = iteratorSource.next();
		}
		return edgeHandle;
	}
	
	private JSONObject getEdgeJSON( String edgeId, String edgeCollection, String graphName ){
		ArangoDriver conn = null;
		String getSourceEdgeByID = "";
		JSONObject edge = null;
		try {
			conn = dbm.getConnection();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		//get edge handle
		//Map<String, Object> soruceEdgeMap = new MapBuilder().put("id",edgeId).get();		
		if ( edgeCollection.equals("")){
			getSourceEdgeByID = "for i in GRAPH_EDGES('"+ graphName +"', null, {}) FILTER i.id == '"+ edgeId +"' return i";
		} else {
			getSourceEdgeByID = "for i in GRAPH_EDGES('"+ graphName +"', null, {edgeCollectionRestriction : '"+ edgeCollection +"'}) FILTER i.id == '"+ edgeId +"' return i";
		}
		
		//Map<String, Object> bindVarsSource = new MapBuilder().put("id",soruceEdgeMap).get();
		CursorEntity<JSONObject> resSourceById = null;
		try {
			resSourceById = conn.executeQuery(	getSourceEdgeByID, null, JSONObject.class, true, 1);
		} catch (ArangoException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Iterator<JSONObject> iteratorSource = resSourceById.iterator();
		if (iteratorSource.hasNext()){
			edge = iteratorSource.next();
		}
		return edge;
	}
		
	// ================= Swagger Resource Listing & API Declarations
	// =====================

	@GET
	@Path("api-docs")
	@Summary("retrieve Swagger 1.2 resource listing.")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Swagger 1.2 compliant resource listing"),
			@ApiResponse(code = 404, message = "Swagger resource listing not available due to missing annotations."), })
	@Produces(MediaType.APPLICATION_JSON)
	public HttpResponse getSwaggerResourceListing() {
		return RESTMapper.getSwaggerResourceListing(this.getClass());
	}

	@GET
	@Path("api-docs/{tlr}")
	@Produces(MediaType.APPLICATION_JSON)
	@Summary("retrieve Swagger 1.2 API declaration for given top-level resource.")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Swagger 1.2 compliant API declaration"),
			@ApiResponse(code = 404, message = "Swagger API declaration not available due to missing annotations."), })
	public HttpResponse getSwaggerApiDeclaration(@PathParam("tlr") String tlr) {
		return RESTMapper.getSwaggerApiDeclaration(this.getClass(), tlr, epUrl);
	}

	/**
	 * Method for debugging purposes. Here the concept of restMapping validation
	 * is shown. It is important to check, if all annotations are correct and
	 * consistent. Otherwise the service will not be accessible by the
	 * WebConnector. Best to do it in the unit tests. To avoid being
	 * overlooked/ignored the method is implemented here and not in the test
	 * section.
	 * 
	 * @return true, if mapping correct
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
	 * This method is needed for every RESTful application in LAS2peer. There is
	 * no need to change!
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

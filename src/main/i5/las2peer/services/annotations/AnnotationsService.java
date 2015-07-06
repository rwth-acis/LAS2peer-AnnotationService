package i5.las2peer.services.annotations;

import i5.las2peer.api.Service;
import i5.las2peer.restMapper.HttpResponse;
import i5.las2peer.restMapper.MediaType;
import i5.las2peer.restMapper.RESTMapper;
import i5.las2peer.restMapper.annotations.Consumes;
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
import i5.las2peer.services.annotations.annotationTypes.Annotation;
import i5.las2peer.services.annotations.annotationTypes.PlaceTypeAnnotation;
import i5.las2peer.services.annotations.annotationTypes.TimeTypeAnnotation;
import i5.las2peer.services.annotations.database.DatabaseManager;
import i5.las2peer.services.annotations.idGenerateClient.IdGenerateClientClass;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import com.arangodb.ArangoDriver;
import com.arangodb.ArangoException;
import com.arangodb.CursorResultSet;
import com.arangodb.entity.CursorEntity;
import com.arangodb.entity.DeletedEntity;
import com.arangodb.entity.DocumentEntity;
import com.arangodb.entity.EdgeEntity;
import com.arangodb.entity.GraphEntity;
import com.arangodb.util.MapBuilder;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;
import net.minidev.json.parser.ParseException;

import com.google.gson.Gson;

/**
 * LAS2peer Annotations Service
 * 
 * This is a LAS2peer service used to save annotations of objects uploaded in
 * SeViAnno 3.0. This service uses the LAS2peer Web-Connector for RESTful access to it.
 * 
 */
@Path("annotations")
@Version("0.1.6")
@ApiInfo(title = "Annotations Service", 
	description = "<p>A RESTful service for storing annotations for different kinds of objects.</p> "
			+ "This tool can be used to store different types of annotations, for different types of objects.", 
	termsOfServiceUrl = "", 
	contact = "bakiu@dbis.rwth-aachen.de", 
	license = "MIT", 
	licenseUrl = "https://github.com/rwth-acis/las2peer-annotationService/blob/master/LICENSE")
public class AnnotationsService extends Service {

	private String port;
	private String host;
	private String username;
	private String password;
	private String database;
	private String graphName;
	private String annotationContextCollection;
	private String placeTypeAnnotationCollection;
	private String timeTypeAnnotationCollection;
	private String enableCURLLogger;
	private String idGeneratingService;
	private DatabaseManager dbm;
	
	private final static int SUCCESSFUL_INSERT = 201;
	private final static int SUCCESSFUL_INSERT_ANNOTATIONCONTEXT = 202;
	private final static String HANDLE = "_id";
	private final static String COLLECTION = "collection";
	
	private final static Object ANNOTATION = new String("annotationData");
	private final static Object POSITION = new String("position");
	private final static Object AUTHOR = new String("author");
	private final static Object NAME = new String("name");
	private final static Object URI = new String("uri");
	private final static Object TIMESTAMP = new String("timeStamp");
	private final static Object LASTUPDATE = new String("lastUpdate");
	private final static Object TOOLID = new String("toolId");
	
	private final static String SERVICE = "Annotations";
	
	private final static String geographicPositionElements[] = {"altitude","latitude","longitude"};
	private final static String timeElements[] = {"timePoint","duration"};
	
	//depricated but still needs to be as a query parameter
	private final static int MAX_RECORDS = 100;
	
	private String epUrl;
	
	GraphEntity graphNew;

	public AnnotationsService() {
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
	 * Create a new Graph
	 * 
	 * @param graphData Graph details that need to be saved. The data come in a JSON format
	 *            
	 * @return HttpResponse
	 */
	/*

	@PUT
	@Path("graph")
	@Summary("Insert new graph")
	@Notes("Requires authentication. JSON format {\"collection\": \"Videos\", \"collection\": \"Annotations\", \"from\":\"Videos\", \"to\": \"Annotations\"}")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Graph saved successfully."),
			@ApiResponse(code = 400, message = "JSON file is not correct."),
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
				edgeDefHasAnnotated.setCollection(annotationContextCollection);
	
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
					er.setHeader("Content-Type", MediaType.TEXT_PLAIN);
					er.setStatus(400);
					return er;
				}
				if(from.isEmpty()){
					// return HTTP Response on error
					HttpResponse er = new HttpResponse("Internal error: "
							+ "Missing JSON object member with key \""
							+ edgeFrom.toString() + "\"");
					er.setHeader("Content-Type", MediaType.TEXT_PLAIN);
					er.setStatus(400);
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
					er.setHeader("Content-Type", MediaType.TEXT_PLAIN);
					er.setStatus(400);
					return er;
				}
				if(to.isEmpty()){
					// return HTTP Response on error
					HttpResponse er = new HttpResponse("Internal error: "
							+ "Missing JSON object member with key \""
							+ edgeTo.toString() + "\"");
					er.setHeader("Content-Type", MediaType.TEXT_PLAIN);
					er.setStatus(400);
					return er;
				}
				edgeDefHasAnnotated.setTo(to);

				// add the edge definition to the list
				edgeDefinitions.add(edgeDefHasAnnotated);

				
				// Collections for the objects
				Object objectCollection = new String("collection");
				List<String> orphanCollections = new ArrayList<String>();
				try {
					for (Object key: o.keySet()){
						if(key.toString().equals(objectCollection.toString())){
							orphanCollections.add((String) o.get(key));
						}
					}
				} catch (Exception e) {
					// return HTTP Response on error
					HttpResponse er = new HttpResponse("Internal error: "
							+ "Missing JSON object member with key \""
							+ objectCollection.toString() + "\"");
					er.setHeader("Content-Type", MediaType.TEXT_PLAIN);
					er.setStatus(400);
					return er;
				}				
				//orphanCollections.add("Videos");
				//orphanCollections.add("Annotations");

				// Create the graph:
				graphNew = conn.createGraph(graphName, edgeDefinitions,	orphanCollections, true);
								

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
	}*/

	/**
	 * Add new object, for an (nonAnnotation) item.
	 * @param objectData Object details that need to be saved. The data come in a JSON format
	 * @return HttpResponse
	 */

	@POST
	@Path("objects")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Summary("Create new object.")
	@Notes("Requires authentication. The object stores only objects id. "
			+ " Payload specifies the collection where this item "
			+ "should be stored. JSON: { \"collection\": \"Videos\"}")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Object saved successfully."),
			@ApiResponse(code = 400, message = "JSON file is not correct."),
			@ApiResponse(code = 401, message = "User is not authenticated."),
			@ApiResponse(code = 409, message = "Object with the given id already exists."),
			@ApiResponse(code = 500, message = "Internal error.") })
	public HttpResponse addNewObject(@ContentParam String objectData) {

		String result = "";
		ArangoDriver conn = null;
		PreparedStatement stmnt = null;
		ResultSet rs = null;
		try {
			JSONObject o;
			try{	
				o = (JSONObject) JSONValue.parseWithException(objectData);
			} catch (ParseException e1) {
				// return HTTP Response on error
				HttpResponse er = new HttpResponse("Error: "
						+ "Payload cannot be parsed. Please, make sure it is correct!");
				er.setHeader("Content-Type", MediaType.TEXT_PLAIN);
				er.setStatus(400);
				return er;
			} catch (ClassCastException e) {
				// return HTTP Response on error
				HttpResponse er = new HttpResponse("Error: "
						+ "Payload cannot be parsed. Please, make sure it is correct!");
				er.setHeader("Content-Type", MediaType.TEXT_PLAIN);
				er.setStatus(400);
				return er;
			}
						
			if (getActiveAgent().getId() != getActiveNode().getAnonymous()
					.getId()) {
				conn = dbm.getConnection();
				String id = "";
				String graphCollection = "";
				String toolId = "";
				
				Object graphCollectionObj = new String("collection");
				
				graphCollection = getKeyFromJSON(graphCollectionObj, o, true);
				toolId = getKeyFromJSON(TOOLID, o, true);
				
				id = getId();
				
				if (!id.equals("") && !graphCollection.equals("") && !toolId.equals("")) {
					
						o.put("id",id);
						
						//Insert author, timeStamp information
						JSONObject author = getAuthorInformation();
						String timeStamp = getTimeStamp();
						
						o.put(AUTHOR.toString(), author);
						o.put(TIMESTAMP.toString(),timeStamp);
						o.put(TOOLID.toString(),toolId);
						
						DocumentEntity<JSONObject> newObject = conn.graphCreateVertex(graphName, graphCollection, id, o, true);
						if(newObject.getCode() == SUCCESSFUL_INSERT && !newObject.isError()){
							JSONObject newObj = newObject.getEntity();
							// return
							HttpResponse r = new HttpResponse(newObj.toJSONString());
							r.setStatus(200);
							return r;
						}else{
							// return HTTP Response on error
							String response = newObject.getErrorNumber() + ", " + newObject.getErrorMessage();
							HttpResponse er = new HttpResponse("Internal error: Cannot add object. " + response +".");
							er.setHeader("Content-Type", MediaType.TEXT_PLAIN);
							er.setStatus(500);
							return er;
						}				
				}else {
					// return HTTP Response on error
					HttpResponse er = new HttpResponse("Internal error: "
							+ "Missing JSON object member with key \""
							+ graphCollectionObj.toString() + "\""
							+ " and/or \"" + TOOLID.toString() + "\""
							+ "");
					er.setHeader("Content-Type", MediaType.TEXT_PLAIN);
					er.setStatus(400);
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
			er.setHeader("Content-Type", MediaType.TEXT_PLAIN);
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
					er.setHeader("Content-Type", MediaType.TEXT_PLAIN);
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
					er.setHeader("Content-Type", MediaType.TEXT_PLAIN);
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
					er.setHeader("Content-Type", MediaType.TEXT_PLAIN);
					er.setStatus(500);
					return er;
				}
			}
		}
	}
		
	/**
	 * Add new object, for an Annotation item. The collection where this object is added is specified
	 * in the received JSON object. 
	 * @param annotationData Annotation details that need to be saved. The data come in a JSON format
	 * @return HttpResponse
	 */
	@POST
	@Path("annotations")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@ResourceListApi(description = "Annotations store details like title, text of an annotations.")
	@Summary("Create new annotation.")
	@Notes("Requires authentication. JSON format \"collection\": \"TextTypeAnnotations\", "
					+ " \"title\": \"Annotation Insert Test\" ,\"keywords\": \"test annotation\", "
					+ "\"objectId\": " + "\"" + "1111" + "\"" + ","
					+ " \"location\": \"Microservice Test Class\"}")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Annotation saved successfully."),
			@ApiResponse(code = 400, message = "JSON file is not correct."),
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
				// return HTTP Response on error
				HttpResponse er = new HttpResponse("Error: "
						+ "Payload cannot be parsed. Please, make sure it is correct!");
				er.setHeader("Content-Type", MediaType.TEXT_PLAIN);
				er.setStatus(400);
				return er;
			} catch (ClassCastException e) {
				// return HTTP Response on error
				HttpResponse er = new HttpResponse("Error: "
						+ "Payload cannot be parsed. Please, make sure it is correct!");
				er.setHeader("Content-Type", MediaType.TEXT_PLAIN);
				er.setStatus(400);
				return er;
			}
			
			if (getActiveAgent().getId() != getActiveNode().getAnonymous()
					.getId()) {
				
				conn = dbm.getConnection();
				String graphCollection = "";
				String id = "";
				String toolId = "";

				Object graphCollectionObj = new String("collection");
				Object objectIdObj = new String("objectId");
				toolId = getKeyFromJSON(TOOLID, o, true);
				
				id = getId();
				
				graphCollection = getKeyFromJSON(graphCollectionObj, o, true);
				
				if (!id.equals("") && !graphCollection.equals("") && !toolId.equals("")) {
					if ( getObjectHandle(id , graphCollection, graphName).equals("")){
						//Insert author, timeStamp information
						JSONObject author = getAuthorInformation();
						String timeStamp = getTimeStamp();
						
						//o.put(AUTHOR.toString(), author);
						//o.put(TIMESTAMP.toString(),timeStamp);
						
						String objectId = getKeyFromJSON(objectIdObj, o, true);
						
						if (objectId.equals("")){
							// return HTTP Response on error
							HttpResponse er = new HttpResponse("Internal error: "
									+ "Missing JSON object member with key "
									+ "\"" + objectIdObj.toString() + "\""									
									+ "");
							er.setHeader("Content-Type", MediaType.TEXT_PLAIN);
							er.setStatus(400);
							return er;
						}
						
						
						DocumentEntity<Annotation> newAnnotation = conn.graphCreateVertex(graphName, graphCollection, id, new Annotation(id, o, author, timeStamp, toolId), true);
				
						if(newAnnotation.getCode() == SUCCESSFUL_INSERT && !newAnnotation.isError()){
							JSONObject emptyAnnotationContext = addNewAnnotatoinContextEmpty(objectId, newAnnotation.getEntity().getId(), toolId);
							
							if (emptyAnnotationContext != null){
								JSONObject newObj = newAnnotation.getEntity().toJSON();
								newObj.put("annotationContextId", emptyAnnotationContext.get(new String("annotationContextId")));
								// return
								HttpResponse r = new HttpResponse(newObj.toJSONString());
								r.setStatus(200);
								return r;
							}else{
								// return HTTP Response on error
								String response = "Could not create an AnnotationContext.";
								HttpResponse er = new HttpResponse("Internal error: " + response +".");
								er.setHeader("Content-Type", MediaType.TEXT_PLAIN);
								er.setStatus(500);
								return er;
							}
						}else{
							// return HTTP Response on error
							String response = newAnnotation.getErrorNumber() + ", " + newAnnotation.getErrorMessage();
							HttpResponse er = new HttpResponse("Internal error: Cannot add object. " + response +".");
							er.setHeader("Content-Type", MediaType.TEXT_PLAIN);
							er.setStatus(500);
							return er;
						}
					}else{
						// return HTTP Response on object exists
						result = "Object already exists!";
						// return
						HttpResponse r = new HttpResponse(result);
						r.setStatus(409);
						return r;
					}					
				}else {
					// return HTTP Response on error
					HttpResponse er = new HttpResponse("Internal error: "
							+ "Missing JSON object member with key "
							+ "\"" + graphCollectionObj.toString() + "\""
							+ " and/or \"" + TOOLID.toString() + "\""
							+ "");
					er.setHeader("Content-Type", MediaType.TEXT_PLAIN);
					er.setStatus(400);
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
			er.setHeader("Content-Type", MediaType.TEXT_PLAIN);
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
					er.setHeader("Content-Type", MediaType.TEXT_PLAIN);
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
					er.setHeader("Content-Type", MediaType.TEXT_PLAIN);
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
					er.setHeader("Content-Type", MediaType.TEXT_PLAIN);
					er.setStatus(500);
					return er;
				}
			}
		}
	}

	/**
	 * Add new object, for a PlaceTypeAnnotation. The collection where this object is added is specified
	 * in the received JSON object. 
	 * @param annotationData PlaceTypeAnnotation details that need to be saved. The data come in a JSON format
	 * @return HttpResponse
	 */
	@POST
	@Path("annotations/placetype")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@ResourceListApi(description = "PlaceTypeAnnotations store details like title, text of an annotations"
			+ " and the geographicPosition that is specific for an PlaceTypeAnnotation.")
	@Summary("Create new annotation.")
	@Notes("Requires authentication. JSON format \"collection\": \"TextTypeAnnotations\", "
					+ " \"title\": \"Annotation Insert Test\" ,\"keywords\": \"test annotation\", "
					+ "\"objectId\": " + "\"" + "1111" + "\"" + ","
					+ " \"location\": \"Microservice Test Class\", \"geographicPosition\":{ \"altitude\":\"100\", "
									+ "\"latitude\":\"100\", \"longtitude\":\"100\" } }")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Annotation saved successfully."),
			@ApiResponse(code = 400, message = "JSON file is not correct."),
			@ApiResponse(code = 401, message = "User is not authenticated."),
			@ApiResponse(code = 409, message = "Annotation already exists."),
			@ApiResponse(code = 500, message = "Internal error.") })
	public HttpResponse addNewPlaceTypeAnnotation(@ContentParam String annotationData) {

		String result = "";
		ArangoDriver conn = null;
		PreparedStatement stmnt = null;
		ResultSet rs = null;
		try {
			JSONObject o;
			try{	
				o = (JSONObject) JSONValue.parseWithException(annotationData);
			} catch (ParseException e1) {
				// return HTTP Response on error
				HttpResponse er = new HttpResponse("Error: "
						+ "Payload cannot be parsed. Please, make sure it is correct!");
				er.setHeader("Content-Type", MediaType.TEXT_PLAIN);
				er.setStatus(400);
				return er;
			} catch (ClassCastException e) {
				// return HTTP Response on error
				HttpResponse er = new HttpResponse("Error: "
						+ "Payload cannot be parsed. Please, make sure it is correct!");
				er.setHeader("Content-Type", MediaType.TEXT_PLAIN);
				er.setStatus(400);
				return er;
			}
			
			if (getActiveAgent().getId() != getActiveNode().getAnonymous()
					.getId()) {
				
				conn = dbm.getConnection();
				String graphCollection = "";
				String id = "";
				String toolId = "";
				JSONObject geoPosition = null;

				Object graphCollectionObj = new String("collection");
				Object objectIdObj = new String("objectId");
				Object objectGeoPosObj = new String("geographicPosition");
				
				toolId = getKeyFromJSON(TOOLID, o, true);
				geoPosition= getJSONKeyFromJSON(objectGeoPosObj, o, true);
								
				id = getId();
				
				graphCollection = getKeyFromJSON(graphCollectionObj, o, true);
				
				if (!id.equals("") && !graphCollection.equals("") && !toolId.equals("") && geoPosition!=null) {
					if ( getObjectHandle(id , graphCollection, graphName).equals("")){
						//Insert author, timeStamp information
						JSONObject author = getAuthorInformation();
						String timeStamp = getTimeStamp();
						
						//o.put(AUTHOR.toString(), author);
						//o.put(TIMESTAMP.toString(),timeStamp);
						
						String objectId = getKeyFromJSON(objectIdObj, o, true);
						//JSONObject geoPositionValue = convertStringToJSON(geoPosition);
						
						if (objectId.equals("")){
							// return HTTP Response on error
							HttpResponse er = new HttpResponse("Internal error: "
									+ "Missing JSON object member with key "
									+ "\"" + objectIdObj.toString() + "\""									
									+ "");
							er.setHeader("Content-Type", MediaType.TEXT_PLAIN);
							er.setStatus(400);
							return er;
						}
						
						if (!checkGeographicPosition(geoPosition)){
							// return HTTP Response on error
							HttpResponse er = new HttpResponse("Internal error: "
									+ "Geographic position was not specified correctly"
									+ " according the format { \"altitude\":{VALUE}, "
									+ "\"latitude\":{VALUE}, \"longtitude\":{VALUE} }"								
									+ "");
							er.setHeader("Content-Type", MediaType.TEXT_PLAIN);
							er.setStatus(400);
							return er;
						}
						
						PlaceTypeAnnotation placeType = new PlaceTypeAnnotation(id, o, author, timeStamp, toolId, geoPosition);
						DocumentEntity<PlaceTypeAnnotation> newAnnotation = conn.graphCreateVertex(graphName, graphCollection, id, placeType, true);
				
						if(newAnnotation.getCode() == SUCCESSFUL_INSERT && !newAnnotation.isError()){
							JSONObject emptyAnnotationContext = addNewAnnotatoinContextEmpty(objectId, newAnnotation.getEntity().getId(), toolId);
							
							if (emptyAnnotationContext != null){
								JSONObject newObj = newAnnotation.getEntity().toJSON();
								newObj.put("annotationContextId", emptyAnnotationContext.get(new String("annotationContextId")));
								// return
								HttpResponse r = new HttpResponse(newObj.toJSONString());
								r.setStatus(200);
								return r;
							}else{
								// return HTTP Response on error
								String response = "Could not create an AnnotationContext.";
								HttpResponse er = new HttpResponse("Internal error: " + response +".");
								er.setHeader("Content-Type", MediaType.TEXT_PLAIN);
								er.setStatus(500);
								return er;
							}
						}else{
							// return HTTP Response on error
							String response = newAnnotation.getErrorNumber() + ", " + newAnnotation.getErrorMessage();
							HttpResponse er = new HttpResponse("Internal error: Cannot add object. " + response +".");
							er.setHeader("Content-Type", MediaType.TEXT_PLAIN);
							er.setStatus(500);
							return er;
						}
					}else{
						// return HTTP Response on object exists
						result = "Object already exists!";
						// return
						HttpResponse r = new HttpResponse(result);
						r.setStatus(409);
						return r;
					}					
				}else {
					// return HTTP Response on error
					HttpResponse er = new HttpResponse("Internal error: "
							+ "Missing JSON object member with key "
							+ "\"" + graphCollectionObj.toString() + "\""
							+ " and/or \"" + TOOLID.toString() + "\""
							+ " and/or \"" + objectGeoPosObj.toString() + "\""
							+ "");
					er.setHeader("Content-Type", MediaType.TEXT_PLAIN);
					er.setStatus(400);
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
			er.setHeader("Content-Type", MediaType.TEXT_PLAIN);
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
					er.setHeader("Content-Type", MediaType.TEXT_PLAIN);
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
					er.setHeader("Content-Type", MediaType.TEXT_PLAIN);
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
					er.setHeader("Content-Type", MediaType.TEXT_PLAIN);
					er.setStatus(500);
					return er;
				}
			}
		}
	}
	
	/**
	 * Add new object, for a TimeTypeAnnotation. The collection where this object is added is specified
	 * in the received JSON object. 
	 * @param annotationData TimeTypeAnnotation details that need to be saved. The data come in a JSON format
	 * @return HttpResponse
	 */
	@POST
	@Path("annotations/timetype")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@ResourceListApi(description = "TimeTypeAnnotation store details like title, text of an annotations"
			+ " and the time that is specific for an PlaceTypeAnnotation.")
	@Summary("Create new annotation.")
	@Notes("Requires authentication. JSON format \"collection\": \"TextTypeAnnotations\", "
					+ " \"title\": \"Annotation Insert Test\" ,\"keywords\": \"test annotation\", "
					+ "\"objectId\": " + "\"" + "1111" + "\"" + ","
					+ " \"location\": \"Microservice Test Class\", \"time\":{ \"timePoint\":\"2015-06-27 05:14:24\", "
									+ " \"duration\":\"PT1H\" } }")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Annotation saved successfully."),
			@ApiResponse(code = 400, message = "JSON file is not correct."),
			@ApiResponse(code = 401, message = "User is not authenticated."),
			@ApiResponse(code = 409, message = "Annotation already exists."),
			@ApiResponse(code = 500, message = "Internal error.") })
	public HttpResponse addNewTimeTypeAnnotation(@ContentParam String annotationData) {

		String result = "";
		ArangoDriver conn = null;
		PreparedStatement stmnt = null;
		ResultSet rs = null;
		try {
			JSONObject o;
			try{	
				o = (JSONObject) JSONValue.parseWithException(annotationData);
			} catch (ParseException e1) {
				// return HTTP Response on error
				HttpResponse er = new HttpResponse("Error: "
						+ "Payload cannot be parsed. Please, make sure it is correct!");
				er.setHeader("Content-Type", MediaType.TEXT_PLAIN);
				er.setStatus(400);
				return er;
			} catch (ClassCastException e) {
				// return HTTP Response on error
				HttpResponse er = new HttpResponse("Error: "
						+ "Payload cannot be parsed. Please, make sure it is correct!");
				er.setHeader("Content-Type", MediaType.TEXT_PLAIN);
				er.setStatus(400);
				return er;
			}
			
			if (getActiveAgent().getId() != getActiveNode().getAnonymous()
					.getId()) {
				
				conn = dbm.getConnection();
				String graphCollection = "";
				String id = "";
				String toolId = "";
				JSONObject time = null;

				Object graphCollectionObj = new String("collection");
				Object objectIdObj = new String("objectId");
				Object objectTimeObj = new String("time");
				
				toolId = getKeyFromJSON(TOOLID, o, true);
				time= getJSONKeyFromJSON(objectTimeObj, o, true);
								
				id = getId();
				
				graphCollection = getKeyFromJSON(graphCollectionObj, o, true);
				
				if (!id.equals("") && !graphCollection.equals("") && !toolId.equals("") && time!=null) {
					if ( getObjectHandle(id , graphCollection, graphName).equals("")){
						//Insert author, timeStamp information
						JSONObject author = getAuthorInformation();
						String timeStamp = getTimeStamp();
						
						//o.put(AUTHOR.toString(), author);
						//o.put(TIMESTAMP.toString(),timeStamp);
						
						String objectId = getKeyFromJSON(objectIdObj, o, true);
						//JSONObject geoPositionValue = convertStringToJSON(geoPosition);
						
						if (objectId.equals("")){
							// return HTTP Response on error
							HttpResponse er = new HttpResponse("Internal error: "
									+ "Missing JSON object member with key "
									+ "\"" + objectIdObj.toString() + "\""									
									+ "");
							er.setHeader("Content-Type", MediaType.TEXT_PLAIN);
							er.setStatus(400);
							return er;
						}
						
						if (!checkTime(time)){
							// return HTTP Response on error
							HttpResponse er = new HttpResponse("Internal error: "
									+ "Geographic position was not specified correctly"
									+ " according the format { \"altitude\":{VALUE}, "
									+ "\"latitude\":{VALUE}, \"longtitude\":{VALUE} }"								
									+ "");
							er.setHeader("Content-Type", MediaType.TEXT_PLAIN);
							er.setStatus(400);
							return er;
						}
						
						TimeTypeAnnotation timeType = new TimeTypeAnnotation(id, o, author, timeStamp, toolId, time);
						DocumentEntity<TimeTypeAnnotation> newAnnotation = conn.graphCreateVertex(graphName, graphCollection, id, timeType, true);
				
						if(newAnnotation.getCode() == SUCCESSFUL_INSERT && !newAnnotation.isError()){
							JSONObject emptyAnnotationContext = addNewAnnotatoinContextEmpty(objectId, newAnnotation.getEntity().getId(), toolId);
							
							if (emptyAnnotationContext != null){
								JSONObject newObj = newAnnotation.getEntity().toJSON();
								newObj.put("annotationContextId", emptyAnnotationContext.get(new String("annotationContextId")));
								// return
								HttpResponse r = new HttpResponse(newObj.toJSONString());
								r.setStatus(200);
								return r;
							}else{
								// return HTTP Response on error
								String response = "Could not create an AnnotationContext.";
								HttpResponse er = new HttpResponse("Internal error: " + response +".");
								er.setHeader("Content-Type", MediaType.TEXT_PLAIN);
								er.setStatus(500);
								return er;
							}
						}else{
							// return HTTP Response on error
							String response = newAnnotation.getErrorNumber() + ", " + newAnnotation.getErrorMessage();
							HttpResponse er = new HttpResponse("Internal error: Cannot add object. " + response +".");
							er.setHeader("Content-Type", MediaType.TEXT_PLAIN);
							er.setStatus(500);
							return er;
						}
					}else{
						// return HTTP Response on object exists
						result = "Object already exists!";
						// return
						HttpResponse r = new HttpResponse(result);
						r.setStatus(409);
						return r;
					}					
				}else {
					// return HTTP Response on error
					HttpResponse er = new HttpResponse("Internal error: "
							+ "Missing JSON object member with key "
							+ "\"" + graphCollectionObj.toString() + "\""
							+ " and/or \"" + TOOLID.toString() + "\""
							+ " and/or \"" + objectTimeObj.toString() + "\""
							+ "");
					er.setHeader("Content-Type", MediaType.TEXT_PLAIN);
					er.setStatus(400);
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
			er.setHeader("Content-Type", MediaType.TEXT_PLAIN);
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
					er.setHeader("Content-Type", MediaType.TEXT_PLAIN);
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
					er.setHeader("Content-Type", MediaType.TEXT_PLAIN);
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
					er.setHeader("Content-Type", MediaType.TEXT_PLAIN);
					er.setStatus(500);
					return er;
				}
			}
		}
	}

	
	@POST
	@Path("annotationContexts")
	@ResourceListApi(description = " stores the relation data between an object and an annotations.")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "AnnotationContext saved successfully."),
			@ApiResponse(code = 400, message = "JSON file is not correct."),
			@ApiResponse(code = 401, message = "User is not authenticated."),
			@ApiResponse(code = 409, message = "AnnotationContext already exists."),
			@ApiResponse(code = 500, message = "Internal error.") })
	public HttpResponse addNewAnnotationContextNull(@ContentParam String annotationContextData) {

		return null;
	}

	/**
	 * Add new annotationContext
	 * @param annotationContextData Data for the annotationContext we want to store.
	 * @return HttpResponse
	 */
	@POST
	@Path("annotationContexts/{sourceId}/{destId}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	//@ResourceListApi(description = " stores the relation data between an object and an annotations.")
	@Summary("Create new annotationContext.")
	@Notes("Requires authentication. JSON: { "
			+ " \"position\": { \"x\": \"10\", \"y\": \"10\", \"z\": \"10\"}, "
			+ "\"time\": \"1.324\", \"duration\": \"0.40\" } .")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "AnnotationContext saved successfully."),
			@ApiResponse(code = 400, message = "JSON file is not correct."),
			@ApiResponse(code = 401, message = "User is not authenticated."),
			@ApiResponse(code = 409, message = "AnnotationContext already exists."),
			@ApiResponse(code = 500, message = "Internal error.") })
	public HttpResponse addNewAnnotationContext(@PathParam("sourceId") String source, @PathParam("destId") String dest, @ContentParam String annotationContextData) {

		String result = "";
		ArangoDriver conn = null;
		try {
			JSONObject o;
			try{	
				o = (JSONObject) JSONValue.parseWithException(annotationContextData);
			} catch (ParseException e1) {
				// return HTTP Response on error
				HttpResponse er = new HttpResponse("Error: "
						+ "Payload cannot be parsed. Please, make sure it is correct!");
				er.setHeader("Content-Type", MediaType.TEXT_PLAIN);
				er.setStatus(400);
				return er;
			} catch (ClassCastException e) {
				// return HTTP Response on error
				HttpResponse er = new HttpResponse("Error: "
						+ "Payload cannot be parsed. Please, make sure it is correct!");
				er.setHeader("Content-Type", MediaType.TEXT_PLAIN);
				er.setStatus(400);
				return er;
			}
			
			if (getActiveAgent().getId() != getActiveNode().getAnonymous()
					.getId()) {
				
				conn = dbm.getConnection();
				String sourceId = "";
				String destId = "";
				String sourceHandle = "";
				String destHandle = "";
				String id = "";
				String toolId = "";
				
				Object annotationContextSourceObj = new String("source");
				Object annotationContextDestObj = new String("dest");
				
				sourceId = source;//getKeyFromJSON(annotationContextSourceObj, o, true);
				destId = dest;//getKeyFromJSON(annotationContextDestObj, o, true);
				toolId = getKeyFromJSON(TOOLID, o, true);
				
				id = getId();
				
				if ( !sourceId.equals("") && !destId.equals("") &&  !toolId.equals("")){
					
					sourceHandle = getObjectHandle(sourceId, "", graphName);
					//get destination handle
					destHandle = getObjectHandle(destId, "", graphName);
					
				}else{ //not correct json
					// return HTTP Response on error
					HttpResponse er = new HttpResponse("Internal error: "
							+ "Missing JSON object member with key \""
							+ "\"" + annotationContextSourceObj.toString() + "\" and/or "
							+ "\"" + annotationContextDestObj.toString() + "\" " 
							+ " and/or \"" + TOOLID.toString() + "\""
							+ "");
					er.setHeader("Content-Type", MediaType.TEXT_PLAIN);
					er.setStatus(400);
					return er;
				}
				
				//insert the new AnnotationContext
				if (getAnnotationContextHandle(id, annotationContextCollection, graphName).equals("")){
					
					//Insert author, timestamp information
					JSONObject author = getAuthorInformation();
					String timeStamp = getTimeStamp();
					
					o.put(AUTHOR.toString(), author);
					o.put(TIMESTAMP.toString(), timeStamp);
					o.put(LASTUPDATE.toString(), timeStamp);
					o.put(TOOLID.toString(), toolId);
					
					//Insert id information
					o.put("id", id);
					
					//Insert AnnotationContext
					EdgeEntity<?> newAnnotationContext = conn.graphCreateEdge(graphName, annotationContextCollection, id, sourceHandle, destHandle, o, null);
				
					if (newAnnotationContext.getCode() == SUCCESSFUL_INSERT_ANNOTATIONCONTEXT){
						JSONObject newAnnotContextData = getAnnotationContextJSONByHandleWithData(
								(String) ((JSONObject) newAnnotationContext.getEntity()).get(new String(HANDLE)), 
								annotationContextCollection, graphName);
						
						// return
						HttpResponse r = new HttpResponse(newAnnotContextData.toJSONString());
						r.setStatus(200);
						return r;
						
					}else{
						// return HTTP Response on error
						HttpResponse er = new HttpResponse("Internal error: Cannot add annotationContext. Error Code " + newAnnotationContext.getCode() + ".");
						er.setHeader("Content-Type", MediaType.TEXT_PLAIN);
						er.setStatus(500);
						return er;
					}
				}else{
					// return HTTP Response on AnnotationContext exists
					result = "AnnotationContext already exists!";
					// return
					HttpResponse r = new HttpResponse(result);
					r.setStatus(409);
					return r;
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
			er.setHeader("Content-Type", MediaType.TEXT_PLAIN);
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
					er.setHeader("Content-Type", MediaType.TEXT_PLAIN);
					er.setStatus(500);
					return er;
				}
			}
		}
	}
	
	private JSONObject addNewAnnotatoinContextEmpty(String source, String destination, String toolId){
		
		String sourceHandle = "";
		String destHandle = "";
		JSONObject o;
		ArangoDriver conn = null;
		String id = getId();
		
		if ( !source.equals("") && !destination.equals("") ){
			sourceHandle = getObjectHandle(source, "", graphName);
			//get destination handle
			destHandle = getObjectHandle(destination, "", graphName);
			
		}else{ 
			//TODO: throw exception
			return null;
		}
		
		//insert the new AnnotationContext
		if (getAnnotationContextHandle(id, annotationContextCollection, graphName).equals("")){
			
			
			o = new JSONObject();
			//Insert author, timeStamp information
			JSONObject author = getAuthorInformation(); 
			String timeStamp = getTimeStamp();
			
			o.put(AUTHOR.toString(), author);
			o.put(TIMESTAMP.toString(), timeStamp);
			o.put(LASTUPDATE.toString(), timeStamp);
			o.put(TOOLID.toString(), toolId);
			
			//Insert id information
			o.put("id", id);
			
			//Insert AnnotationContext
			try {
				conn = dbm.getConnection();
			} catch (SQLException e1) {				
				e1.printStackTrace();
				return null;
			}
			EdgeEntity<?> newAnnotationContext;
			try {
				newAnnotationContext = conn.graphCreateEdge(graphName, annotationContextCollection, id, sourceHandle, destHandle, o, null);
			} catch (ArangoException e) {
				e.printStackTrace();
				return null;
				
			}
		
			if (newAnnotationContext.getCode() == SUCCESSFUL_INSERT_ANNOTATIONCONTEXT){
				/*JSONObject newAnnotContextData = getAnnotationContextJSONByHandle(
						(String) ((JSONObject) newAnnotationContext.getEntity()).get(new String(HANDLE)), 
						annotationContextCollection, graphName);*/
				
				String annotationContextHandle = (String) ((JSONObject) newAnnotationContext.getEntity()).get(new String(HANDLE));
				String [] annotationHandleSplit = annotationContextHandle.split("/"); 
				
				String annotationContextId = annotationHandleSplit[1];
				
				JSONObject annotationContextJSON = new JSONObject();
				annotationContextJSON.put("annotationContextId", annotationContextId);
				
				// return
				return annotationContextJSON;
				
			}else{
				// return
				return null;
			}
		}else{
			// return 
			return null;
		}
	}


	/**
	 * Method to update an object with a given id
	 * 
	 * @param objectId  key of the object we need to update
	 * @return HttpResponse with the result of the method
	 */

	@PUT
	@Path("objects/{objectId}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Summary("Update given object.")
	@Notes("Requires authentication. The object can also be an annotation. JSON: {\"title\": \"Updated Title :)\" }")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Object details updated successfully."),
			@ApiResponse(code = 400, message = "JSON file is not correct."),
			@ApiResponse(code = 401, message = "User is not authenticated."),
			@ApiResponse(code = 404, message = "Object not found."),
			@ApiResponse(code = 500, message = "Internal error.") })
	public HttpResponse updateObject(	@PathParam("objectId") String objectId, @ContentParam String objectData) {

		String result = "";
		ArangoDriver conn = null;
		try {
			JSONObject o;
			try {
				o = (JSONObject) JSONValue.parseWithException(objectData);
			} catch (ParseException e1) {
				// return HTTP Response on error
				HttpResponse er = new HttpResponse("Error: "
						+ "Payload cannot be parsed. Please, make sure it is correct!");
				er.setHeader("Content-Type", MediaType.TEXT_PLAIN);
				er.setStatus(400);
				return er;
			} catch (ClassCastException e) {
				// return HTTP Response on error
				HttpResponse er = new HttpResponse("Error: "
						+ "Payload cannot be parsed. Please, make sure it is correct!");
				er.setHeader("Content-Type", MediaType.TEXT_PLAIN);
				er.setStatus(400);
				return er;
			}
			
			if (getActiveAgent().getId() != getActiveNode().getAnonymous()
					.getId()) {
				
				conn = dbm.getConnection();
				String objectCollection = "";
				String objectHandle = "";
				String objectKeyDb = "";
				
				JSONObject objectFromDB = null;								
				if ( !objectId.equals("") && ! graphName.equals("")){
					objectFromDB = getObjectJSON(objectId, objectCollection, graphName);
					if (objectFromDB != null)
						objectHandle = getKeyFromJSON(new String(HANDLE), objectFromDB, false);
				} else {
					// return HTTP Response on error
					HttpResponse er = new HttpResponse("Internal error: "
							+ "Missing ObjectId ");
					er.setHeader("Content-Type", MediaType.TEXT_PLAIN);
					er.setStatus(400);
					return er;
				}
				
				if (objectHandle.equals("")){
					
					o.put("id",objectId);
					objectCollection = getKeyFromJSON(new String(COLLECTION), o, true);
					if (!objectCollection.equals("")){
						
						DocumentEntity<JSONObject> newObject = conn.graphCreateVertex(graphName, objectCollection, o, true);
						if(newObject.getCode() == SUCCESSFUL_INSERT && !newObject.isError()){
							JSONObject newObj = newObject.getEntity();
							// return
							HttpResponse r = new HttpResponse(newObj.toJSONString());
							r.setStatus(200);
							return r;
						}else{
							result = "Object could not be added!";
							// return
							HttpResponse r = new HttpResponse(result);
							r.setHeader("Content-Type", MediaType.TEXT_PLAIN);
							r.setStatus(404);
							return r;
						}
					}else{
						result = "\"Collection\" is not definded.";
						// return
						HttpResponse r = new HttpResponse(result);
						r.setHeader("Content-Type", MediaType.TEXT_PLAIN);
						r.setStatus(400);
						return r;
					}
				}
				
				//update the JSON according the new input data
				for (Object key: o.keySet()){
					if (objectFromDB.containsKey(key))
					{
						objectFromDB.remove(key);
						objectFromDB.put((String)key,  o.get(key));
					}else if (objectFromDB.containsKey(ANNOTATION)){
						Object annotationData = objectFromDB.get(ANNOTATION);
						Gson gs = new Gson();				
						String annotation = gs.toJson(annotationData);
						
						JSONObject annotationDataJSON = (JSONObject) JSONValue.parse(annotation);
						if (annotationDataJSON.containsKey(key))
						{
							annotationDataJSON.remove(key);
							annotationDataJSON.put((String)key,  o.get(key));
							objectFromDB.remove(ANNOTATION);
							objectFromDB.put((String)ANNOTATION, annotationDataJSON);
						}
						
					}else
						objectFromDB.put((String)key,  o.get(key));
				}
				
				//write the new update time
				if(objectFromDB.containsKey(LASTUPDATE)){
					objectFromDB.remove(LASTUPDATE);
					objectFromDB.put((String) LASTUPDATE,  getTimeStamp());
				}
				
				String [] objectHandleSplit = objectHandle.split("/"); 
				objectKeyDb = objectHandleSplit[1];
				objectCollection = objectHandleSplit[0];
				
				DocumentEntity<?> updatedObject = conn.graphUpdateVertex(graphName, objectCollection, objectKeyDb, objectFromDB, true);
				
				if ( updatedObject.getCode() == 202){
					//JSONObject updatedObj = (JSONObject) updatedObject.getEntity();
					// return
					HttpResponse r = new HttpResponse(objectFromDB.toJSONString());
					r.setStatus(200);
					return r;
				} else {
					// return HTTP Response on error
					HttpResponse er = new HttpResponse("Internal error: Cannot update object. "
							+ "Error Code " + updatedObject.getCode() + ", " + updatedObject.getErrorMessage() + ".");
					er.setHeader("Content-Type", MediaType.TEXT_PLAIN);
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
			er.setHeader("Content-Type", MediaType.TEXT_PLAIN);
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
					er.setHeader("Content-Type", MediaType.TEXT_PLAIN);
					er.setStatus(500);
					return er;
				}
			}
		}
	}
	
	/**
	 * Method to update the an annotationContext
	 * 
	 * @param annotationContextId  Id of the annotationContext we need to update
	 * @return HttpResponse with the result of the method
	 */

	@PUT
	@Path("annotationContexts/{annotationContextId}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Summary("Update given AnnotationContext.")
	@Notes("Requires authentication. JSON: {  "
			+ "\"position\": { \"x\": \"10\", \"y\": \"10\", \"z\": \"10\"}, \"time\": \"1.324\", "
			+ "\"duration\": \"1.70\" }")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "AnnotationContext updated successfully."),
			@ApiResponse(code = 400, message = "JSON file is not correct."),
			@ApiResponse(code = 401, message = "User is not authenticated."),
			@ApiResponse(code = 404, message = "AnnotationContext not found."),
			@ApiResponse(code = 500, message = "Internal error.") })
	public HttpResponse updateAnnotationContext(@PathParam("annotationContextId") String annotationContextId, @ContentParam String annotationContextData) {

		String result = "";
		ArangoDriver conn = null;
		try {
			JSONObject o;
			try {
				o = (JSONObject) JSONValue.parseWithException(annotationContextData);
			} catch (ParseException e1) {
				// return HTTP Response on error
				HttpResponse er = new HttpResponse("Error: "
						+ "Payload cannot be parsed. Please, make sure it is correct!");
				er.setHeader("Content-Type", MediaType.TEXT_PLAIN);
				er.setStatus(400);
				return er;
			} catch (ClassCastException e) {
				// return HTTP Response on error
				HttpResponse er = new HttpResponse("Error: "
						+ "Payload cannot be parsed. Please, make sure it is correct!");
				er.setHeader("Content-Type", MediaType.TEXT_PLAIN);
				er.setStatus(400);
				return er;
			}
			
			if (getActiveAgent().getId() != getActiveNode().getAnonymous()
					.getId()) {
				
				conn = dbm.getConnection();
				String id = "";
				String annotationContextHandle = "";
				String annotationContextKeyDb = "";
				
				JSONObject annotationContextFromDB = null;
				
				id = annotationContextId;
				
				if ( !id.equals("")){					
					annotationContextFromDB = getAnnotationContextJSON(id, annotationContextCollection, graphName);
					annotationContextHandle = getKeyFromJSON(new String(HANDLE), annotationContextFromDB, false);
				} else {
					// return HTTP Response on error
					HttpResponse er = new HttpResponse("Internal error: "
							+ "Not specified annotationContextId!");
					er.setHeader("Content-Type", MediaType.TEXT_PLAIN);
					er.setStatus(400);
					return er;
				}
				
				if (annotationContextHandle.equals("")){
					// return HTTP Response on AnnotationContext not found
					result = "AnnotationContext is not found!";
					// return
					HttpResponse r = new HttpResponse(result);
					r.setHeader("Content-Type", MediaType.TEXT_PLAIN);
					r.setStatus(404);
					return r;
				}
								
				//update the JSON according the new input data
				for (Object key: o.keySet()){
					if (annotationContextFromDB.containsKey(key))
					{
						annotationContextFromDB.remove(key);
						annotationContextFromDB.put((String)key,  o.get(key));
						continue;
					}
					if (annotationContextFromDB.containsKey(ANNOTATION)){
						Object annotationContextDatas = annotationContextFromDB.get(ANNOTATION);
						Gson gs = new Gson();				
						String annotationContext = gs.toJson(annotationContextDatas);
						
						JSONObject annotationDataContextJSON = (JSONObject) JSONValue.parse(annotationContext);
						if (annotationDataContextJSON.containsKey(key))
						{
							annotationDataContextJSON.remove(key);
							annotationDataContextJSON.put((String)key,  o.get(key));
							annotationContextFromDB.remove(ANNOTATION);
							annotationContextFromDB.put((String)ANNOTATION, annotationDataContextJSON);
							continue;
						}
						
					}
					if (annotationContextFromDB.containsKey(POSITION)){
						Object annotationContextDatas = annotationContextFromDB.get(POSITION);
						Gson gs = new Gson();				
						String annotationContext = gs.toJson(annotationContextDatas);
						
						JSONObject annotationDataContextJSON = (JSONObject) JSONValue.parse(annotationContext);
						if (annotationDataContextJSON.containsKey(key))
						{
							annotationDataContextJSON.remove(key);
							annotationDataContextJSON.put((String)key,  o.get(key));
							annotationContextFromDB.remove(POSITION);
							annotationContextFromDB.put((String)POSITION, annotationDataContextJSON);
							continue;
						}
						
					}/*else if (annotationContextFromDB.containsKey(AUTHOR)){
						Object annotationContextDatas = annotationContextFromDB.get(AUTHOR);
						Gson gs = new Gson();				
						String annotationContext = gs.toJson(annotationContextDatas);
						
						JSONObject annotationDataContextJSON = (JSONObject) JSONValue.parse(annotationContext);
						if (annotationDataContextJSON.containsKey(key))
						{
							annotationDataContextJSON.remove(key);
							annotationDataContextJSON.put((String)key,  o.get(key));
							annotationContextFromDB.remove(AUTHOR);
							annotationContextFromDB.put((String)AUTHOR, annotationDataContextJSON);
						}
						
					}*/
					
					//no changes made till now
					annotationContextFromDB.put((String)key,  o.get(key));
				}
				
				//write the new update time
				if(annotationContextFromDB.containsKey(LASTUPDATE)){
					annotationContextFromDB.remove(LASTUPDATE);
					annotationContextFromDB.put((String) LASTUPDATE,  getTimeStamp());
				}
				
				String [] annotationContextHandleSplit = annotationContextHandle.split("/"); 
				annotationContextKeyDb = annotationContextHandleSplit[1];
				
				//update the database
				EdgeEntity<?> updatedAnnotationContext = conn.graphUpdateEdge(graphName, annotationContextCollection, annotationContextKeyDb, annotationContextFromDB, true);
				if ( updatedAnnotationContext.getCode() == SUCCESSFUL_INSERT_ANNOTATIONCONTEXT){

					// return
					HttpResponse r = new HttpResponse(annotationContextFromDB.toJSONString());
					r.setStatus(200);
					return r;
				} else {
					// return HTTP Response on error
					HttpResponse er = new HttpResponse("Internal error: Cannot update AnnotationContext. Error Code " + updatedAnnotationContext.getCode() + ".");
					er.setHeader("Content-Type", MediaType.TEXT_PLAIN);
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
			er.setHeader("Content-Type", MediaType.TEXT_PLAIN);
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
					er.setHeader("Content-Type", MediaType.TEXT_PLAIN);
					er.setStatus(500);
					return er;
				}
			}
		}
	}
	
	/**
	 * Method to delete an Object with a given id
	 * 
	 * @param objectId  id of the Object we need to delete
	 * @return HttpResponse with the result of the method
	 */

	@DELETE
	@Path("objects/{objectId}")
	@Produces(MediaType.TEXT_PLAIN)
	@Summary("Delete given object.")
	@Notes("Requires authentication.")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Object deleted successfully."),
			@ApiResponse(code = 401, message = "User is not authenticated."),
			@ApiResponse(code = 403, message = "User cannot delete this object."),
			@ApiResponse(code = 404, message = "Object not found."),
			@ApiResponse(code = 500, message = "Internal error.") })
	public HttpResponse deleteObject(@PathParam("objectId") String objectId) {

		String result = "";
		ArangoDriver conn = null;
		try {
			//JSONObject o;
			/*try {
				o = (JSONObject) JSONValue.parseWithException(objectData);
			} catch (ParseException e1) {
				throw new IllegalArgumentException( "Data is not valid JSON!" );
			}*/
			if (getActiveAgent().getId() != getActiveNode().getAnonymous()
					.getId()) {
				
				conn = dbm.getConnection();
				String objectCollection = "";
				String id = "";
				String objectHandle = "";
				String objectKeyDb = "";
						
				id = objectId;
				
				JSONObject objectFromDB = null;								
				objectFromDB = getObjectJSON(id, objectCollection, graphName);
				objectHandle = getKeyFromJSON(HANDLE, objectFromDB, false);
				
				if (objectHandle.equals("")){
					// return HTTP Response on Vertex not found
					result = "Object is not found!";
					// return
					HttpResponse r = new HttpResponse(result);
					r.setStatus(404);
					return r;
				}
				
				if (objectFromDB.containsKey("author")){
					Object author =  objectFromDB.get("author");
					Gson g = new Gson();				
					String object = g.toJson(author);
					
					JSONObject authorJSON = (JSONObject) JSONValue.parse(object);
					
					if (authorJSON.containsKey("sub")){
						if (!authorJSON.get("sub").equals(getActiveUserInfo().get("sub"))){
							// return HTTP Response on Vertex not found
							result = "User not authorized!";
							// return
							HttpResponse r = new HttpResponse(result);
							r.setStatus(403);
							return r;
						}
					}
				}
				
				String [] objectHandleSplit = objectHandle.split("/"); 
				objectKeyDb = objectHandleSplit[1];
				objectCollection = objectHandleSplit[0];
				
				DeletedEntity deletedObject = conn.graphDeleteVertex(graphName, objectCollection, objectKeyDb);
				
				if ( deletedObject.getCode() == SUCCESSFUL_INSERT_ANNOTATIONCONTEXT && deletedObject.getDeleted() == true){
					result = "Database updated. Object deleted";

					// return
					HttpResponse r = new HttpResponse(result);
					r.setStatus(200);
					return r;
				} else {
					// return HTTP Response on error
					HttpResponse er = new HttpResponse("Internal error: Cannot update object. Error Code " + deletedObject.getCode() + ".");
					er.setHeader("Content-Type", MediaType.TEXT_PLAIN);
					er.setStatus(500);
					return er;
				}

			} else {
				result = "User in not authenticated";

				// return
				HttpResponse r = new HttpResponse(result);
				r.setHeader("Content-Type", MediaType.TEXT_PLAIN);
				r.setStatus(401);
				return r;
			}

		} catch (Exception e) {
			// return HTTP Response on error
			HttpResponse er = new HttpResponse("Internal error: "+ e.getMessage());
			er.setHeader("Content-Type", MediaType.TEXT_PLAIN);
			er.setStatus(500);
			return er;
		} finally {			
			if (conn != null) {
				try {
					conn = null;
				} catch (Exception e) {
					Context.logError(this, e.getMessage());

					// return HTTP Response on error
					HttpResponse er = new HttpResponse("Internal error: " + e.getMessage());
					er.setHeader("Content-Type", MediaType.TEXT_PLAIN);
					er.setStatus(500);
					return er;
				}
			}
		}
	}
	
	/**
	 * Method to delete the an AnnotationContext
	 * 
	 * @param annotationContextId  id of the annotationContext we need to delete
	 * @return HttpResponse with the result of the method
	 */

	@DELETE
	@Path("annotationContexts/{annotationContextId}")
	@Produces(MediaType.TEXT_PLAIN)
	@Summary("Delete given annotationContext.")
	@Notes("Requires authentication.")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "AnnotationContext deleted successfully."),
			@ApiResponse(code = 401, message = "User is not authenticated."),
			@ApiResponse(code = 403, message = "User cannot delete this annotationContext."),
			@ApiResponse(code = 404, message = "AnnotationContext not found."),
			@ApiResponse(code = 500, message = "Internal error.") })
	public HttpResponse deleteAnnotationContext( @PathParam("annotationContextId") String annotationContextId) {

		String result = "";
		ArangoDriver conn = null;
		try {

			if (getActiveAgent().getId() != getActiveNode().getAnonymous()
					.getId()) {
				
				conn = dbm.getConnection();
				String id = "";
				String annotationContextHandle = "";
				String annotationContextKeyDb = "";				
				
				id = annotationContextId;
				
				JSONObject annotationContextFromDB = null;								
				annotationContextFromDB = getAnnotationContextJSON(id, annotationContextCollection, graphName);
				annotationContextHandle = getKeyFromJSON(HANDLE, annotationContextFromDB, false);
				
				if (annotationContextHandle.equals("")){
					// return HTTP Response on AnnotationContext not found
					result = "annotationContext is not found!";
					// return
					HttpResponse r = new HttpResponse(result);
					r.setStatus(404);
					return r;
				}
				
				if (annotationContextFromDB.containsKey("author")){
					Object author =  annotationContextFromDB.get("author");
					Gson g = new Gson();				
					String object = g.toJson(author);
					
					JSONObject authorJSON = (JSONObject) JSONValue.parse(object);
					if (authorJSON.containsKey("sub")){
						if (!authorJSON.get("sub").equals(getActiveUserInfo().get("sub"))){
							// return HTTP Response on Vertex not found
							result = "User not authorized!";
							// return
							HttpResponse r = new HttpResponse(result);
							r.setStatus(403);
							return r;
						}
					}
				}
				
				String [] annotationContextHandleSplit = annotationContextHandle.split("/"); 
				annotationContextKeyDb = annotationContextHandleSplit[1];
				
				DeletedEntity deletedAnnotationContext = conn.graphDeleteEdge(graphName, annotationContextCollection, annotationContextKeyDb);
				
				if ( deletedAnnotationContext.getCode() == SUCCESSFUL_INSERT_ANNOTATIONCONTEXT && deletedAnnotationContext.getDeleted()){
				
					result = "Database updated. annotationContext deleted";

					// return
					HttpResponse r = new HttpResponse(result);
					r.setStatus(200);
					return r;
				} else {
					// return HTTP Response on error
					HttpResponse er = new HttpResponse("Internal error: Cannot update annotationContext. Error Code " + deletedAnnotationContext.getCode() + ".");
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
	 * Method to retrieve all annotations of a given object
	 * 
	 * @param objectId id of the source object. e.g. Annotations for a Video
	 * @param part part of the requested output
	 * @param collection collection where the object is stored
	 * @return JSONArray of objectId-neighbors together with AnnotationContext information
	 */
	@GET
	@Path("objects/{objectId}/annotations")
	@Produces(MediaType.APPLICATION_JSON)
	@Summary("Retrieve annotations of a given object."
			+ "")
	@Notes("Return a JSON with the annotations. Query parameter \"part\" selects the columns that need to be returned in the JSON.")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Object annotations retrived successfully."),
			@ApiResponse(code = 404, message = "Object id does not exist."),
			@ApiResponse(code = 500, message = "Internal error."), })
	public HttpResponse getObjectAnnotations(@PathParam("objectId") String objectId, @QueryParam(name = "part", defaultValue = "*" ) String part, @QueryParam(name = "collection", defaultValue = "" ) String collection) {
		ArangoDriver conn = null;
		JSONArray qs = new JSONArray();
		try {
			String objectCollection = "";
			
			JSONObject objectFromDB = null;
			
			//Object objectIdObj = new String("id");
			
			//get the object collection name from the Json 
			objectCollection = collection;

			
			String[] partsOfObject = part.split(",");
			
			conn = dbm.getConnection();

			objectFromDB = getObjectJSON(objectId, objectCollection, graphName);			
						
			if (objectFromDB==null){
				// return HTTP Response on Vertex not found
				String result = "Object is not found!";
				// return
				HttpResponse r = new HttpResponse(result);
				r.setHeader("Content-Type", MediaType.TEXT_PLAIN);
				r.setStatus(404);
				return r;
			}
						
			String getAnnotations = "";
			if (partsOfObject[0].equals("*")){
				getAnnotations = "for i in GRAPH_NEIGHBORS('"+ graphName +"', @selectedObject, {includeData: 'true'})  "
						+ "SORT i.vertex.id, i.path.edges.time, i.path.edges.duration return i";
			} else {
				String selectParts = "{";
				for(String p:partsOfObject )
				{
					//Changed to avoid confusion between the conventions used in the service
					// and conventions known by ArangoDB
					if (p.equals("object")){
						p = "vertex";
					}else if (p.equals("annotationContext")){
						p = "edges";
					}
					String partSmall = "'" + p + "': i." + p + ",";
					selectParts += partSmall;
				}
				//replace last character from ',' to '}'
				selectParts = selectParts.substring(0, selectParts.length()-1) + "}";
				
				getAnnotations = "for i in GRAPH_NEIGHBORS('"+ graphName +"', @selectedObject, {includeData: 'true'})"
						+ " SORT i.vertex.id, i.path.edges.time, i.path.edges.duration return " + selectParts;
			}
			Map<String, Object> bindVars = new MapBuilder().put("selectedObject", objectFromDB).get();

			CursorResultSet<JSONObject> rs = conn.executeQueryWithResultSet(getAnnotations, bindVars, JSONObject.class, true, 0);
			//qs.add("objects that have AnnotationContext with " + objectId );
			for (JSONObject obj : rs) {
				qs.add(obj);
			}
			
			JSONArray modification = modifyJSON(qs);
			
			//compose JSONObject
			JSONObject results = new JSONObject();
			results.put("annotations", modification);
			//modification.add(0, new String("Annotations that have an annotationContext with " + objectHandle ));
			// prepare statement

			// return HTTP Response on success
			HttpResponse r = new HttpResponse(results.toJSONString());
			r.setStatus(200);
			return r;

		} catch (Exception e) {
			// return HTTP Response on error
			HttpResponse er = new HttpResponse("Internal error: "
					+ e.getMessage());
			er.setHeader("Content-Type", MediaType.TEXT_PLAIN);
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
					er.setHeader("Content-Type", MediaType.TEXT_PLAIN);
					er.setStatus(500);
					return er;
				}
			}
		}

	}
	
	/**
	 * Method to retrieve all annotations containing (some of) the given keywords
	 * 
	 * @param query (list of) keywords
	 * @param part part of the requested output
	 * @param collection collection where the object is stored
	 * @return JSONArray of objectId-neighbors together with AnnotationContext information
	 */
	@GET
	@Path("annotations")
	@Produces(MediaType.APPLICATION_JSON)
	@Summary("Retrieve annotations by keyword."
			+ "")
	@Notes("Return a JSON with the annotations. Query parameter \"part\" selects the columns that need "
			+ "to be returned in the JSON. \"part\" can have values: collection, contextId, duration, id, "
			+ "keywords, location, objectCollection, objectId, position, text, time, title. To include"
			+ " more than one part, combine values separated by \",\" ")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Annotations retrived successfully."),
			@ApiResponse(code = 400, message = "No defined query."),
			@ApiResponse(code = 404, message = "No Annotations found."),
			@ApiResponse(code = 500, message = "Internal error."), })
	public HttpResponse getAnnotationsByKeyword(@QueryParam(name = "q", defaultValue = "" ) String query, @QueryParam(name = "part", defaultValue = "*" ) String part, @QueryParam(name = "collection", defaultValue = "" ) String collection) {
		ArangoDriver conn = null;
		JSONArray qs = new JSONArray();
		try {
			
			String getAnnotations = "";
			//Object objectIdObj = new String("id");
			
			String[] partsOfObject = part.split(",");
			
			
			String[] partsOfQuery = query.split(",");
			String selectParts = "";
			
			selectParts = composeSelectPartsForSearchByKeyword(part);
			
			conn = dbm.getConnection();
			String collectionPart = "";
			if (collection.equals("")){
				collectionPart = "{}";
			}else{
				collectionPart = "{vertexCollectionRestriction : '"+ collection +"'}";
			}
			
			String filter = "";
			if (query.equals("")){
				/*// return HTTP Response on Vertex not found
				String result = "No query found!";
				// return
				HttpResponse r = new HttpResponse(result);
				r.setHeader("Content-Type", MediaType.TEXT_PLAIN);
				r.setStatus(400);
				return r;*/
			}else{
			
				//construct filter
				
				for(String q:partsOfQuery){
					if (filter.equals(""))
						filter += " FILTER (CONTAINS(LOWER(i.title), '"+ q.toLowerCase() + "') OR CONTAINS(LOWER(i.text), '"+ q.toLowerCase() + "')  OR CONTAINS(LOWER(i.keywords), '"+ q.toLowerCase() + "'))";
					else
						filter += " AND (CONTAINS(LOWER(i.title), '"+ q.toLowerCase() + "') OR CONTAINS(LOWER(i.text), '"+ q.toLowerCase() + "') OR CONTAINS(LOWER(i.keywords), '"+ q.toLowerCase() + "'))";
				}
			}	
			selectParts = "{" + selectParts.substring(0, selectParts.length()-1) + "}";
				
			getAnnotations = " let l = (for i in GRAPH_VERTICES('" + graphName + "', null, "+ collectionPart + ")  "
					+ filter + " "
					+ "For u in GRAPH_NEIGHBORS('" + graphName + "', i, {includeData: 'true', direction : 'inbound'}) "
					+ "return " + selectParts + " " 
					+ " ) return unique(l)";
			
			CursorResultSet<JSONArray> rs = conn.executeQueryWithResultSet(getAnnotations, null, JSONArray.class, true, MAX_RECORDS);
			//qs.add("objects that have AnnotationContext with " + objectId );
			Iterator<JSONArray> iteratorAnnotation = rs.iterator();
			if (iteratorAnnotation.hasNext()) {
				JSONArray annotation = (JSONArray) iteratorAnnotation.next();
				
				HttpResponse r = new HttpResponse(annotation.toJSONString());
				r.setStatus(200);
				return r;
			}else{
				String result = "No entries found";
				HttpResponse r = new HttpResponse(result);
				r.setStatus(404);
				return r;
			}
			
			//compose JSONObject
			/*JSONObject results = new JSONObject();
			results.put("annotations", qs);
			// return HTTP Response on success
			HttpResponse r = new HttpResponse(results.toJSONString());
			r.setStatus(200);
			return r;*/

		} catch (Exception e) {
			// return HTTP Response on error
			HttpResponse er = new HttpResponse("Internal error: "
					+ e.getMessage());
			er.setHeader("Content-Type", MediaType.TEXT_PLAIN);
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
					er.setHeader("Content-Type", MediaType.TEXT_PLAIN);
					er.setStatus(500);
					return er;
				}
			}
		}

	}
	
	/**
	 * Method to retrieve a given object
	 * 
	 * @param objectId id of requested object
	 * @param part requested part of the results
	 * @return httpResponse
	 */
	@GET
	@Path("objects/{objectId}")
	@Produces(MediaType.APPLICATION_JSON)
	@Summary("Retrieve given object.")
	@Notes("Returns a JSON with the object details. Query parameter \"part\" selects the columns"
			+ " that need to be returned in the JSON. \"part\" can have values: id, "
			+ "keywords, author, annotationData, text, title. To include"
			+ " more than one part, combine values separated by \",\" ")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Object retrived successfully."),
			@ApiResponse(code = 404, message = "Object id does not exist."),
			@ApiResponse(code = 500, message = "Internal error."), })
	public HttpResponse getObject(@PathParam("objectId") String objectId, @QueryParam(name = "part", defaultValue = "*" ) String part) {
		ArangoDriver conn = null;
		JSONObject objectFromDB = null;
		try {
									
			//Object objectIdObj = new String("id");
			
			conn = dbm.getConnection();
			Context.logMessage(this, conn.toString());
			
			objectFromDB = getObjectJSON(objectId, "", graphName, part);
				//vertexHandle = getKeyFromJSON(new String(HANDLE), objectFromDB, false);
				
			
			if (objectFromDB == null){
				// return HTTP Response on Vertex not found
				String result = "Object is not found!";
				// return
				HttpResponse r = new HttpResponse(result);
				r.setHeader("Content-Type", MediaType.TEXT_PLAIN);
				r.setStatus(404);
				return r;
			} else {
				// return HTTP Response on successL
				HttpResponse r = new HttpResponse(objectFromDB.toJSONString());
				r.setStatus(200);
				return r;
			}

		} catch (Exception e) {
			// return HTTP Response on error
			Context.logMessage(this, objectFromDB.toString());
			HttpResponse er = new HttpResponse("Internal error: "
					+ e.getMessage());
			er.setHeader("Content-Type", MediaType.TEXT_PLAIN);
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
					er.setHeader("Content-Type", MediaType.TEXT_PLAIN);
					er.setStatus(500);
					return er;
				}
			}
		}

	}
	
	/**
	 * Method to return all the objects in a collection
	 * 
	 * @param part parts needed of output
	 * @param collection collection where to find objects
	 * @return httpResponse
	 */
	
	@GET
	@Path("objects")
	@Produces(MediaType.APPLICATION_JSON)
	@ResourceListApi(description = "Objects that can be annotated.")
	@Summary("List objects.")
	@Notes("Returns a JSON with objects stored in the given collection. Query parameter \"part\" selects "
			+ "the columns that need to be returned in the JSON.  \"part\" can have values: id, "
			+ "keywords, author, annotationData, text, title. To include"
			+ " more than one part, combine values separated by \",\"")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Objects selected and returned successfully."),
			@ApiResponse(code = 404, message = "No objects found."),
			@ApiResponse(code = 500, message = "Internal error."), })
	public HttpResponse getObjects(@QueryParam(name = "part", defaultValue = "*" ) String part, @QueryParam(name = "collection", defaultValue = "" ) String collection) {
		ArangoDriver conn = null;
		try {
			String objectCollection = "";
			
			JSONArray objectsFromDB = null;
			
			//Object objectIdObj = new String("id");
					
			//get the vertex collection name from the Json 
			objectCollection = collection;
			
			conn = dbm.getConnection();
			
			objectsFromDB = getObjectsJSON (objectCollection, graphName, part);
			
			if (objectsFromDB.isEmpty()){
				// return HTTP Response on Vertex not found
				String result = "No objects found.";
				// return
				HttpResponse r = new HttpResponse(result);
				r.setHeader("Content-Type", MediaType.TEXT_PLAIN);
				r.setStatus(404);
				return r;
			} else {
				// return HTTP Response on successL
				HttpResponse r = new HttpResponse(objectsFromDB.toJSONString());
				r.setStatus(200);
				return r;
			}

		} catch (Exception e) {
			// return HTTP Response on error
			HttpResponse er = new HttpResponse("Internal error: "
					+ e.getMessage());
			er.setHeader("Content-Type", MediaType.TEXT_PLAIN);
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
					er.setHeader("Content-Type", MediaType.TEXT_PLAIN);
					er.setStatus(500);
					return er;
				}
			}
		}

	}
	
	/**
	 * Get AnnotationContexts that connect two objects
	 * @param sourceId the source of AnnotationContexts
	 * @param destId destination of AnnotationContexts
	 * @param part select only parts of attributes
	 * @param collection collection of AnnotationContexts
	 * @return JSONArray with AnnotationContexts
	 */
	@GET
	@Path("annotationContexts/{sourceId}/{destId}")
	@Produces(MediaType.APPLICATION_JSON)
	@Summary("Retrieve annotationContext information between a given object and a given annotation")
	@Notes("Return a JSON with annotationContexts details. Query parameter \"part\" selects the columns"
			+ " that need to be returned in the JSON.  \"part\" can have values: id, "
			+ "position, time, duration. To include"
			+ " more than one part, combine values separated by \",\"")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "AnnotationContexts selected successfully."),
			@ApiResponse(code = 400, message = "JSON file is not correct."),
			@ApiResponse(code = 404, message = "Id(s) do not exist."),
			@ApiResponse(code = 500, message = "Internal error."), })
	public HttpResponse getAnnotationContexts(@PathParam("sourceId") String sourceId, @PathParam("destId") String destId, @QueryParam(name = "part", defaultValue = "*" ) String part, @QueryParam(name = "collection", defaultValue = "" ) String collection) {
		ArangoDriver conn = null;
		try {
			String sourceHandle = "";
			String destHandle = "";
			JSONArray annotationContextsFromDB = null;
			
			//Object objectIdObj = new String("id");
			
			conn = dbm.getConnection();
			
			sourceHandle = getObjectHandle(sourceId, "", graphName);
			destHandle = getObjectHandle(destId, "", graphName);
			
			if (sourceHandle.equals("")||destHandle.equals("")){
				String result = "Objects not found!";
				// return
				HttpResponse r = new HttpResponse(result);
				r.setHeader("Content-Type", MediaType.TEXT_PLAIN);
				r.setStatus(404);
				return r;
			}
			
			annotationContextsFromDB = getAnnotationContextsJSON(annotationContextCollection, graphName, sourceHandle, destHandle, part);
			
			if (annotationContextsFromDB.isEmpty()){
				// return HTTP Response on Vertex not found
				String result = "No annotationContexts found!";
				// return
				HttpResponse r = new HttpResponse(result);
				r.setHeader("Content-Type", MediaType.TEXT_PLAIN);
				r.setStatus(404);
				return r;
			} else {
				// return HTTP Response on successL
				HttpResponse r = new HttpResponse(annotationContextsFromDB.toJSONString());
				r.setStatus(200);
				return r;
			}

		} catch (Exception e) {
			// return HTTP Response on error
			HttpResponse er = new HttpResponse("Internal error: "
					+ e.getMessage());
			er.setHeader("Content-Type", MediaType.TEXT_PLAIN);
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
					er.setHeader("Content-Type", MediaType.TEXT_PLAIN);
					er.setStatus(500);
					return er;
				}
			}
		}

	}
	
	/**
	 * Method to return all collections in a graph.
	 * @return collections in a JSONObject
	 */
	@GET
	@Path("collections")
	@Produces(MediaType.APPLICATION_JSON)
	@ResourceListApi(description = "Collections are containers of objects.")
	@Summary("List collections.")
	@Notes("Returns a JSON with a list containing collection names for the configured graph.")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "List returned successfully."),
			@ApiResponse(code = 404, message = "No collections found."),
			@ApiResponse(code = 500, message = "Internal error."), })
	public HttpResponse getCollections() {
		ArangoDriver conn = null;
		try {
			
			JSONArray collectionsJSON = new JSONArray();
			String getCollectionsQuery = "";
			
			getCollectionsQuery = "for i in _graphs Filter i._key == '" + graphName + "'"
					+ " FOR c IN i.orphanCollections  return c ";
			
			conn = dbm.getConnection();
			CursorResultSet<String> rs = null;
			try {
				rs = conn.executeQueryWithResultSet(getCollectionsQuery, null, String.class, true, MAX_RECORDS);
			} catch (ArangoException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			for (String obj : rs) {
				collectionsJSON.add(obj);
			}

			if (collectionsJSON.isEmpty()){
				// return HTTP Response on Vertex not found
				String result = "No collections found.";
				// return
				HttpResponse r = new HttpResponse(result);
				r.setHeader("Content-Type", MediaType.TEXT_PLAIN);
				r.setStatus(404);
				return r;
			} else {
				// return HTTP Response on success
				JSONObject collections = new JSONObject();
				collections.put("collections", collectionsJSON);
				HttpResponse r = new HttpResponse(collections.toJSONString());
				r.setStatus(200);
				return r;
			}

		} catch (Exception e) {
			// return HTTP Response on error
			HttpResponse er = new HttpResponse("Internal error: "
					+ e.getMessage());
			er.setHeader("Content-Type", MediaType.TEXT_PLAIN);
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
					er.setHeader("Content-Type", MediaType.TEXT_PLAIN);
					er.setStatus(500);
					return er;
				}
			}
		}

	}
	
	@GET
	@Path("users")
	@ResourceListApi(description = "Users working with this service.")
	public HttpResponse getUserNull() {
		return null;
	}
	
	/**
	 * Method to retrieve all annotations created by a given user
	 * 
	 * @param userSub sub of the user
	 * @param part part of the requested output
	 * @param collection collection where the object is stored
	 * @return JSONArray of objectId-neighbors together with AnnotationContext information
	 */
	@GET
	@Path("users/{userSub}/annotations")
	@Produces(MediaType.APPLICATION_JSON)
	@Summary("Retrieve annotations created by a given user."
			+ "")
	@Notes("Return a JSON with the annotations. Query parameter \"part\" selects the columns that need to be returned in the JSON.")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "User annotations retrived successfully."),
			@ApiResponse(code = 404, message = "User sub does not exist."),
			@ApiResponse(code = 500, message = "Internal error."), })
	public HttpResponse getUserAnnotations(@PathParam("userSub") String userSub, @QueryParam(name = "part", defaultValue = "*" ) String part, @QueryParam(name = "collection", defaultValue = "" ) String collection) {
		ArangoDriver conn = null;
		JSONArray qs = new JSONArray();
		try {
			
			String collectionPart = "";
			if (collection.equals("")){
				collectionPart = "{ direction : 'inbound' }";
			}else{
				collectionPart = "{ direction : 'inbound', vertexCollectionRestriction : '"+ collection +"'}";
			}
			
			String[] partsOfObject = part.split(",");
			
			conn = dbm.getConnection();
						
			String getAnnotations = "";
			if (partsOfObject[0].equals("*")){
				getAnnotations = "for i in GRAPH_VERTICES('"+ graphName +"', null, " + collectionPart + ")  "
						+ " FILTER i.author.sub == '"+ userSub +"'"
						+ " SORT i._key return i";
			} else {
				String selectParts = "{";
				for(String p:partsOfObject )
				{
					String partSmall = "'" + p + "': i." + p + ",";
					selectParts += partSmall;
				}
				//replace last character from ',' to '}'
				selectParts = selectParts.substring(0, selectParts.length()-1) + "}";
				
				getAnnotations = "for i in GRAPH_VERTICES('"+ graphName +"', null, " + collectionPart + ")  "
						+ " FILTER i.author.sub == '"+ userSub +"'"
						+ " SORT i._key return " + selectParts;
			}

			CursorResultSet<JSONObject> rs = conn.executeQueryWithResultSet(getAnnotations, null, JSONObject.class, true, 0);
			//qs.add("objects that have AnnotationContext with " + objectId );
			for (JSONObject obj : rs) {
				qs.add(obj);
			}

			// return HTTP Response on success
			HttpResponse r = new HttpResponse(qs.toJSONString());
			r.setStatus(200);
			return r;

		} catch (Exception e) {
			// return HTTP Response on error
			HttpResponse er = new HttpResponse("Internal error: "
					+ e.getMessage());
			er.setHeader("Content-Type", MediaType.TEXT_PLAIN);
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
					er.setHeader("Content-Type", MediaType.TEXT_PLAIN);
					er.setStatus(500);
					return er;
				}
			}
		}

	}
	
	/**
	 * Method to retrieve all objects created by a given user
	 * 
	 * @param userSub sub of the user
	 * @param part part of the requested output
	 * @param collection collection where the object is stored
	 * @return JSONArray of objectId-neighbors together with AnnotationContext information
	 */
	@GET
	@Path("users/{userSub}/objects")
	@Produces(MediaType.APPLICATION_JSON)
	@Summary("Retrieve objects created by a given user."
			+ "")
	@Notes("Return a JSON with the annotations. Query parameter \"part\" selects the columns that need to be returned in the JSON.")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "User annotations retrived successfully."),
			@ApiResponse(code = 404, message = "User sub does not exist."),
			@ApiResponse(code = 500, message = "Internal error."), })
	public HttpResponse getUserObjects(@PathParam("userSub") String userSub, @QueryParam(name = "part", defaultValue = "*" ) String part, @QueryParam(name = "collection", defaultValue = "" ) String collection) {
		ArangoDriver conn = null;
		JSONArray qs = new JSONArray();
		try {
			
			String collectionPart = "";
			if (collection.equals("")){
				collectionPart = "{ direction : 'outbound' }";
			}else{
				collectionPart = "{ direction : 'outbound', vertexCollectionRestriction : '"+ collection +"'}";
			}
			
			String[] partsOfObject = part.split(",");
			
			conn = dbm.getConnection();
						
			String getAnnotations = "";
			if (partsOfObject[0].equals("*")){
				getAnnotations = "for i in GRAPH_VERTICES('"+ graphName +"', null, " + collectionPart + ")  "
						+ " FILTER i.author.sub == '"+ userSub +"'"
						+ " SORT i._key return i";
			} else {
				String selectParts = "{";
				for(String p:partsOfObject )
				{
					String partSmall = "'" + p + "': i." + p + ",";
					selectParts += partSmall;
				}
				//replace last character from ',' to '}'
				selectParts = selectParts.substring(0, selectParts.length()-1) + "}";
				
				getAnnotations = "for i in GRAPH_VERTICES('"+ graphName +"', null, " + collectionPart + ")  "
						+ " FILTER i.author.sub == '"+ userSub +"'"
						+ " SORT i._key return " + selectParts;
			}

			CursorResultSet<JSONObject> rs = conn.executeQueryWithResultSet(getAnnotations, null, JSONObject.class, true, 0);
			//qs.add("objects that have AnnotationContext with " + objectId );
			for (JSONObject obj : rs) {
				qs.add(obj);
			}

			// return HTTP Response on success
			HttpResponse r = new HttpResponse(qs.toJSONString());
			r.setStatus(200);
			return r;

		} catch (Exception e) {
			// return HTTP Response on error
			HttpResponse er = new HttpResponse("Internal error: "
					+ e.getMessage());
			er.setHeader("Content-Type", MediaType.TEXT_PLAIN);
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
					er.setHeader("Content-Type", MediaType.TEXT_PLAIN);
					er.setStatus(500);
					return er;
				}
			}
		}

	}
	/**
	 * Import data from AchSo! files
	 * @param annotationContextData Data for the annotationContext we want to store.
	 * @return HttpResponse
	 */
	@POST
	@Path("import")
	@Consumes(MediaType.APPLICATION_JSON)
	@Summary("Import from AchSo! files")
	@Notes("Requires authentication.")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Imported successfully."),
			@ApiResponse(code = 500, message = "Internal error.") })
	public HttpResponse importVideoWithAnnotations(@ContentParam String annotationContextData) {

		String result = "";
		ArangoDriver conn = null;
		try {
			JSONObject o;
			try{	
				o = (JSONObject) JSONValue.parseWithException(annotationContextData);
			} catch (ParseException e1) {
				// return HTTP Response on error
				HttpResponse er = new HttpResponse("Error: "
						+ "Payload cannot be parsed. Please, make sure it is correct!");
				er.setHeader("Content-Type", MediaType.TEXT_PLAIN);
				er.setStatus(400);
				return er;
			} catch (ClassCastException e) {
				// return HTTP Response on error
				HttpResponse er = new HttpResponse("Error: "
						+ "Payload cannot be parsed. Please, make sure it is correct!");
				er.setHeader("Content-Type", MediaType.TEXT_PLAIN);
				er.setStatus(400);
				return er;
			}
			
			if (getActiveAgent().getId() != getActiveNode().getAnonymous()
					.getId()) {
				
				conn = dbm.getConnection();
				String graphCollection = "";
				String sourceHandle = "";
				String destHandle = "";
				String id = "";
				String toolId = "AchSo!";
				
				Object graphCollectionObj = new String("collection");
				Object annotationsObj = new String("annotations");
				Object positionObj = new String("position");
				Object time = new String("time");
				Object idObj = new String("id");
				JSONArray annotatoionArray = null;
				JSONObject newAnnotationContext = null;
				
				//get the graph data from the Json 
				id = getKeyFromJSON(idObj, o, false);
				graphCollection = getKeyFromJSON(graphCollectionObj, o,false);
				
				DocumentEntity<JSONObject> newObject = null;
				DocumentEntity<Annotation> annotation = null;
				EdgeEntity<?> annotationContext = null;
				
				if (!id.equals("")){
					sourceHandle = getObjectHandle(id, "", graphName);
					if (sourceHandle.equals("")){
						JSONObject newNode = new JSONObject();
						newNode.put("id", o.get(idObj));
						newNode.put("toolId", toolId);
						newObject = conn.graphCreateVertex(graphName, graphCollection, o, true);
						sourceHandle = newObject.getDocumentHandle();
					}
				}
										
				//get Annotations one by one
				if (o.containsKey(annotationsObj)){
					annotatoionArray = (JSONArray) o.get(annotationsObj);
					for (Object newJSON:annotatoionArray){
						//JSONObject newJS = (JSONObject) JSONValue.parse(edge);
							JSONObject newAnnotation = (JSONObject) JSONValue.parse(newJSON.toString());
							newAnnotationContext = new JSONObject();
							if (newAnnotation.containsKey(positionObj)){
								newAnnotationContext.put("position", newAnnotation.get(positionObj));
								newAnnotation.remove(positionObj);
							}
							if (newAnnotation.containsKey(time)){
								newAnnotationContext.put("time", newAnnotation.get(time));
								newAnnotation.remove(time);
							}
							//get author, timestamp information
							JSONObject author = getAuthorInformation();
							String timeStamp = getTimeStamp();
							
							String newid = getId();
							annotation = conn.graphCreateVertex(graphName, "Annotations", newid, new Annotation(newid, newAnnotation, author, timeStamp, toolId), true);
							destHandle = annotation.getDocumentHandle();
							//add new annotationContext
							String newid2 = getId();
							newAnnotationContext.put("id", newid2);
							newAnnotationContext.put("toolId", toolId);
							annotationContext = conn.graphCreateEdge(graphName, "newAnnotated", newid2, sourceHandle, destHandle, newAnnotationContext, null);
						}
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
			er.setHeader("Content-Type", MediaType.TEXT_PLAIN);
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
					er.setHeader("Content-Type", MediaType.TEXT_PLAIN);
					er.setStatus(500);
					return er;
				}
			}
		}
	}
	
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
	
	private JSONObject getJSONKeyFromJSON(Object key, JSONObject json, boolean remove)
	{
		JSONObject value = null;
		if(json.containsKey(key)){
			value = (JSONObject) json.get(key);
			if(remove)
			{
				json.remove(key);
			}
			
		}
		return value;
	}
	
	/**
	 * Method to create a JSONObject containing information about the user signed in
	 * @return authorInformation
	 */
	private JSONObject getAuthorInformation(){
		JSONObject authorInformation = null;
		try {
			authorInformation = getActiveUserInfo();
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		authorInformation.put(NAME.toString(), (String)((UserAgent) getActiveAgent()).getLoginName());
		//authorInformation.put("sub", authorInformation.get("sub"));
		authorInformation.put(URI.toString(), "");
		return authorInformation;
	}
	
	private JSONObject getActiveUserInfo() throws ParseException {

		if(this.getActiveAgent() instanceof UserAgent){
			UserAgent me = (UserAgent) this.getActiveAgent();
			JSONObject o;

			if(me.getUserData() != null){
				System.err.println(me.getUserData());
				o = (JSONObject) JSONValue.parseWithException((String) me.getUserData());
			} else {
				o = new JSONObject();

				if(getActiveNode().getAnonymous().getId() == getActiveAgent().getId()){
					o.put("sub","anonymous");		
				} else {

					String md5ide = new String(""+me.getId());
					o.put("sub", md5ide);
				}
			}
			return o;

		} else {
			return new JSONObject();
		}
	}
	
	/**
	 * Method to create a JSONObject containing the timestamp
	 * @return timeStamp
	 */
	private String getTimeStamp(){
		Timestamp timeStamp = null;
		Date date = new Date();
		
		timeStamp = new Timestamp(date.getTime());
		return timeStamp.toString();
	}
	
	/**
	 * Get information about one object (specified id)
	 * 
	 * @param objectId id of the requested object
	 * @param objectCollection collection where this object belongs
	 * @param graphName graph name
	 * @return objectHandle - for the requested object
	 */
	
	private String getObjectHandle( String objectId, String objectCollection, String graphName ){
		ArangoDriver conn = null;
		String getSourceObjectByID = "";
		String objectHandle = "";
		
		try {
			conn = dbm.getConnection();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		//get object handle
		//Map<String, Object> soruceObjectMap = new MapBuilder().put("id",objectId).get();
		
		
		if ( objectCollection.equals("")){
			getSourceObjectByID = "for i in GRAPH_VERTICES('"+ graphName +"', null,{}) FILTER i.id == '"+ objectId +"' return i._id";
		} else {
			getSourceObjectByID = "for i in GRAPH_VERTICES('"+ graphName +"', null,{vertexCollectionRestriction : '"+ objectCollection +"'}) FILTER i.id == '"+ objectId +"' return i._id";
		}
		
		//Map<String, Object> bindVarsSource = new MapBuilder().put("id",soruceObjectMap).get();
		//CursorEntity<String> resSourceById = null;
		CursorResultSet<String> rs = null;
		try {
			rs = conn.executeQueryWithResultSet(getSourceObjectByID, null, String.class, true, 1, false);
		} catch (ArangoException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Iterator<String> iteratorSource = rs.iterator();
		if (iteratorSource.hasNext()){
			objectHandle = iteratorSource.next();
		}
		return objectHandle;
	}
	
	/**
	 * Get information about one object (specified id)
	 * 
	 * @param objectId id of the requested object
	 * @param objectCollection collection where this object belongs
	 * @param graphName graph name
	 * @return data for the object in JSON format
	 */
	private JSONObject getObjectJSON( String objectId, String objectCollection, String graphName ){
		ArangoDriver conn = null;
		String getSourceObjectByID = "";
		JSONObject object = null;
		
		try {
			conn = dbm.getConnection();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		//get object handle
		//Map<String, Object> soruceObjectMap = new MapBuilder().put("id",objectId).get();
		
		
		if ( objectCollection.equals("")){
			getSourceObjectByID = "for i in GRAPH_VERTICES('"+ graphName +"', null,{}) FILTER i.id == '"+ objectId +"' return i";
		} else {
			getSourceObjectByID = "for i in GRAPH_VERTICES('"+ graphName +"', null,{vertexCollectionRestriction : '"+ objectCollection +"'}) FILTER i.id == '"+ objectId +"' return i";
		}
		
		//Map<String, Object> bindVarsSource = new MapBuilder().put("id",soruceObjectMap).get();
		//CursorEntity<JSONObject> resSourceById = null;
		CursorResultSet<JSONObject> rs = null;
		try {
			rs = conn.executeQueryWithResultSet(getSourceObjectByID, null, JSONObject.class, true, 1, false);
		} catch (ArangoException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Iterator<JSONObject> iteratorSource = rs.iterator();
		if (iteratorSource.hasNext()){
			object = iteratorSource.next();
		}
		return object;
	}
	
	/**
	 * Get parts of information about one object (specified id) 
	 * 
	 * @param objectId id of the requested object
	 * @param objectCollection collection where this object belongs
	 * @param graphName graph name
	 * @return data for the object in JSON format
	 */
	private JSONObject getObjectJSON( String objectId, String objectCollection, String graphName, String part ){
		ArangoDriver conn = null;
		String getSourceObjectByID = "";
		String returnStatement = " return i";
		JSONObject object = null;
		
		try {
			conn = dbm.getConnection();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			Context.logError(this, "Message: " + e.getMessage() + ". ToString: " + e.toString());
			e.printStackTrace();
		}
		
		//get object handle
		//Map<String, Object> sorucObjectMap = new MapBuilder().put("id",objectId).get();
		
		//return modification
		String selectParts = "";
		if ( part.equals("*") ||  part.equals("")){
			returnStatement = " return i";
		} else {
			String[] partsOfObject = part.split(",");
			selectParts = "{";
			for(String p:partsOfObject )
			{
				String partSmall = "'" + p + "': i." + p + ",";
				selectParts += partSmall;
			}
			//replace last character from ',' to '}'
			selectParts = selectParts.substring(0, selectParts.length()-1) + "}";
			returnStatement = " return " + selectParts;
		}
		
		if ( objectCollection.equals("")){
			getSourceObjectByID = "for i in GRAPH_VERTICES('"+ graphName +"', null,{}) FILTER i.id == '"+ objectId + "' " + returnStatement;
		} else {
			getSourceObjectByID = "for i in GRAPH_VERTICES('"+ graphName +"', null,{vertexCollectionRestriction : '"+ objectCollection +"'}) FILTER i.id == '"+ objectId + "' " + returnStatement;
		}
		
		CursorResultSet<JSONObject> rs = null;
		try {
			rs = conn.executeQueryWithResultSet(getSourceObjectByID, null, JSONObject.class, true, 1, false);
		} catch (ArangoException e) {
			// TODO Auto-generated catch block
			Context.logError(this, "Message2: " + e.getMessage() + ". ToString2: " + e.toString());
			e.printStackTrace();
		}
		Iterator<JSONObject> iteratorSource = rs.iterator();
		if (iteratorSource.hasNext()){
			object = iteratorSource.next();
		}
		return object;
	}
	
	
	/**
	 * Get parts of information about objects
	 * 
	 * @param objectCollection collection where this object belongs
	 * @param graphName graph name
	 * @return data for the object in JSON format
	 */
	private JSONArray getObjectsJSON( String objectCollection, String graphName, String part ){
		ArangoDriver conn = null;
		String getSourceObjectByID = "";
		String returnStatement = " return i";
		JSONArray objectsJSON = new JSONArray();
		
		try {
			conn = dbm.getConnection();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		String selectParts = "";
		if ( part.equals("*") ||  part.equals("")){
			returnStatement = " return i";
		} else {
			String[] partsOfObject = part.split(",");
			selectParts = "{";
			for(String p:partsOfObject )
			{
				String partSmall = "'" + p + "': i." + p + ",";
				selectParts += partSmall;
			}
			//replace last character from ',' to '}'
			selectParts = selectParts.substring(0, selectParts.length()-1) + "}";
			returnStatement = " return " + selectParts;
		}
		
		if ( objectCollection.equals("")){
			getSourceObjectByID = "for i in GRAPH_VERTICES('"+ graphName +"', null,{}) " + returnStatement;
		} else {
			getSourceObjectByID = "for i in GRAPH_VERTICES('"+ graphName +"', null,{vertexCollectionRestriction : '"+ objectCollection +"'})  " + returnStatement;
		}
		
		CursorResultSet<JSONObject> rs = null;
		try {
			rs = conn.executeQueryWithResultSet(getSourceObjectByID, null, JSONObject.class, true, MAX_RECORDS);
		} catch (ArangoException e) {
			// TODO Auto-generated catch block
			Context.logError(this, "Message3: " + e.getMessage() + ". ToString3: " + e.toString());
			e.printStackTrace();
		}
		
		for (JSONObject obj : rs) {
			objectsJSON.add(obj);
		}
		return objectsJSON;
	}
	
	/**
	 * Get information about one annotationContext (specified id)
	 * 
	 * @param annotationContextId id of the requested annotationContext
	 * @param annotationContextCollection collection where this annotationContext belongs
	 * @param graphName  graph name
	 * @return handle for the requested annotationContext
	 */

	private String getAnnotationContextHandle( String annotationContextId, String annotationContextCollection, String graphName ){
		ArangoDriver conn = null;
		String getSourceAnnotationContextByID = "";
		String annotationContextHandle = "";
		try {
			conn = dbm.getConnection();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			Context.logError(this, "Message4: " + e.getMessage() + ". ToString4: " + e.toString());
			e.printStackTrace();
		}
		
		//get annotationContext handle
		//Map<String, Object> soruceannotationContextMap = new MapBuilder().put("id",annotationContextId).get();		
		if ( annotationContextCollection.equals("")){
			getSourceAnnotationContextByID = "for i in GRAPH_EDGES('"+ graphName +"', null, {includeData: 'true'}) FILTER i.id == '"+ annotationContextId +"' return i._id";
		} else {
			getSourceAnnotationContextByID = "for i in GRAPH_EDGES('"+ graphName +"', null, {includeData: 'true', edgeCollectionRestriction : '"+ annotationContextCollection +"'}) FILTER i.id == '"+ annotationContextId +"' return i._id";
		}
		
		CursorResultSet<String> rs = null;
		try {
			 rs = conn.executeQueryWithResultSet(getSourceAnnotationContextByID, null, String.class, true, 1,false);
		} catch (ArangoException e) {
			// TODO Auto-generated catch block
			Context.logError(this, "Message5: " + e.getMessage() + ". ToString5: " + e.toString());
			e.printStackTrace();
		}
		Iterator<String> iteratorSource = rs.iterator();
		if (iteratorSource.hasNext()){
			annotationContextHandle = iteratorSource.next();
		}
		return annotationContextHandle;
	}
	
	/**
	 * Get information about one annotationContext (specified id)
	 * 
	 * @param annotationContextId id of the requested annotationContext
	 * @param annotationContextCollection collection where this annotationContext belongs
	 * @param graphName  graph name
	 * @return data for the specified annotationContext in JSON format
	 */
	private JSONObject getAnnotationContextJSON( String annotationContextId, String annotationContextCollection, String graphName ){
		ArangoDriver conn = null;
		String getSourceAnnotationContextByID = "";
		JSONObject annotationContext = null;
		try {
			conn = dbm.getConnection();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		//get annotationContext handle
		//Map<String, Object> soruceannotationContextMap = new MapBuilder().put("id",annotationContextId).get();		
		if ( annotationContextCollection.equals("")){
			getSourceAnnotationContextByID = "for i in GRAPH_EDGES('"+ graphName +"', null, {includeData: 'true'}) FILTER i.id == '"+ annotationContextId +"' return i";
		} else {
			getSourceAnnotationContextByID = "for i in GRAPH_EDGES('"+ graphName +"', null, {includeData: 'true', edgeCollectionRestriction : '"+ annotationContextCollection +"'}) FILTER i.id == '"+ annotationContextId +"' return i";
		}
		
		CursorResultSet<JSONObject> rs = null;
		try {
			rs = conn.executeQueryWithResultSet(getSourceAnnotationContextByID, null, JSONObject.class, true, 1, false);
		} catch (ArangoException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Iterator<JSONObject> iteratorSource = rs.iterator();
		if (iteratorSource.hasNext()){
			annotationContext = iteratorSource.next();
		}
		return annotationContext;
	}
	
	/**
	 * Get information about one annotationContext (specified id)
	 * 
	 * @param annotationContextId id of the requested annotationContext
	 * @param annotationContextCollection collection where this annotationContext belongs
	 * @param graphName  graph name
	 * @return data for the specified annotationContext in JSON format
	 */
	private JSONObject getAnnotationContextJSONByHandle( String annotationContextHandle, String annotationContextCollection, String graphName ){
		ArangoDriver conn = null;
		String getSourceAnnotationContextByID = "";
		JSONObject annotationContext = null;
		try {
			conn = dbm.getConnection();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		//get annotationContext handle
		if ( annotationContextCollection.equals("")){
			//getSourceAnnotationContextByID = "for i in GRAPH_EDGES('"+ graphName +"', null, {}) FILTER i._id == '"+ annotationContextHandle +"' return i";
			getSourceAnnotationContextByID = "for i in GRAPH_EDGES('"+ graphName +"', null, {}) FILTER i == '"+ annotationContextHandle +"' return i";
		} else {
			//getSourceAnnotationContextByID = "for i in GRAPH_EDGES('"+ graphName +"', null, {edgeCollectionRestriction : '"+ annotationContextCollection +"'}) FILTER i._id == '"+ annotationContextHandle +"' return i";
			getSourceAnnotationContextByID = "for i in GRAPH_EDGES('"+ graphName +"', null, {edgeCollectionRestriction : '"+ annotationContextCollection +"'}) FILTER i == '"+ annotationContextHandle +"' return i";
		}
		
		CursorEntity<String> resSourceById = null;
		CursorResultSet<String> rs = null;
		try {
			resSourceById = conn.executeQuery(	getSourceAnnotationContextByID, null, String.class, true, 1);
		} catch (ArangoException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Iterator<String> iteratorSource = resSourceById.iterator();
		if (iteratorSource.hasNext()){
			annotationContext = new JSONObject();
			annotationContext.put("_id", iteratorSource.next());
		}
		return annotationContext;
	}
	
	/**
	 * Get information about one annotationContext (specified id)
	 * 
	 * @param annotationContextId id of the requested annotationContext
	 * @param annotationContextCollection collection where this annotationContext belongs
	 * @param graphName  graph name
	 * @return data for the specified annotationContext in JSON format
	 */
	private JSONObject getAnnotationContextJSONByHandleWithData( String annotationContextHandle, String annotationContextCollection, String graphName ){
		ArangoDriver conn = null;
		String getSourceAnnotationContextByID = "";
		JSONObject annotationContext = null;
		try {
			conn = dbm.getConnection();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		//get annotationContext handle
		if ( annotationContextCollection.equals("")){
			//getSourceAnnotationContextByID = "for i in GRAPH_EDGES('"+ graphName +"', null, {}) FILTER i._id == '"+ annotationContextHandle +"' return i";
			getSourceAnnotationContextByID = "for i in GRAPH_EDGES('"+ graphName +"', null, {includeData: 'true'}) FILTER i._id == '"+ annotationContextHandle +"' return i";
		} else {
			//getSourceAnnotationContextByID = "for i in GRAPH_EDGES('"+ graphName +"', null, {edgeCollectionRestriction : '"+ annotationContextCollection +"'}) FILTER i._id == '"+ annotationContextHandle +"' return i";
			getSourceAnnotationContextByID = "for i in GRAPH_EDGES('"+ graphName +"', null, {includeData: 'true', edgeCollectionRestriction : '"+ annotationContextCollection +"'}) FILTER i._id == '"+ annotationContextHandle +"' return i";
		}
		
		CursorEntity<JSONObject> resSourceById = null;
		CursorResultSet<JSONObject> rs = null;
		try {
			resSourceById = conn.executeQuery(	getSourceAnnotationContextByID, null, JSONObject.class, true, 1);
		} catch (ArangoException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Iterator<JSONObject> iteratorSource = resSourceById.iterator();
		if (iteratorSource.hasNext()){
			annotationContext = iteratorSource.next();
		}
		return annotationContext;
	}
	
	/**
	 * Method to return all annotationContexts between an Object and an annotation
	 * 
	 * @param annotationContextCollection collection where annotationContexts are stored
	 * @param graphName name of the graph
	 * @param sourceHandle Handle for the object (source)
	 * @param destHandle Handle for the annotation (destination)
	 * @param part parts of the information we want to retrieve
	 * @return JSONArray with all the annotationContexts between the specified Objects
	 */
	private JSONArray getAnnotationContextsJSON(String annotationContextCollection, String graphName, String sourceHandle, String destHandle, String part ){
		ArangoDriver conn = null;
		String getSourceAnnotationContextByID = "";
		String returnStatement = " return i";
		JSONArray annotationContexts = new JSONArray();
		JSONObject annotationContext = null;
		
		try {
			conn = dbm.getConnection();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		//return modification
		String selectParts = "";
		if ( part.equals("*") ||  part.equals("")){
			returnStatement = " return i";
		} else {
			String[] partsOfObject = part.split(",");
			selectParts = "{";
			for(String p:partsOfObject )
			{
				String partSmall = "'" + p + "': i." + p + ",";
				selectParts += partSmall;
			}
			//replace last character from ',' to '}'
			selectParts = selectParts.substring(0, selectParts.length()-1) + "}";
			returnStatement = " return " + selectParts;
		}
				
		//Map<String, Object> soruceannotationContextMap = new MapBuilder().put("id",annotationContextId).get();		
		if ( annotationContextCollection.equals("")){
			getSourceAnnotationContextByID = "for i in GRAPH_EDGES('"+ graphName +"', null, {includeData: 'true'}) FILTER i._from == '"+ sourceHandle +"'  &&  i._to =='" + destHandle + "' " + returnStatement;
		} else {
			getSourceAnnotationContextByID = "for i in GRAPH_EDGES('"+ graphName +"', null, {includeData: 'true', edgeCollectionRestriction : '"+ annotationContextCollection +"'}) FILTER i._from == '"+ sourceHandle +"'  &&  i._to =='" + destHandle + "' " + returnStatement;
		}
		
		//Map<String, Object> bindVarsSource = new MapBuilder().put("id",soruceannotationContextMap).get();
		CursorEntity<JSONObject> resSourceById = null;
		try {
			resSourceById = conn.executeQuery(	getSourceAnnotationContextByID, null, JSONObject.class, true, MAX_RECORDS);
		} catch (ArangoException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Iterator<JSONObject> iteratorSource = resSourceById.iterator();
		while (iteratorSource.hasNext()){
			annotationContext = iteratorSource.next();
			annotationContexts.add(annotationContext);
		}
		return annotationContexts;
	}
		
	/**
	 * Modify JSONArray returned from ArangoDB. It contains information about all the
	 * annotations related to one object, and the annotationContexts' information regarding each annotation
	 * @param array JSONArray returned from ArangoDB. 
	 * @return modified JSONArray in a format { {annotationContext, object}, {annotationContext, object}, ...}
	 */
	private JSONArray modifyJSON(JSONArray array){
		JSONArray modifiy = new JSONArray();
		
		String vertexObject = new String("vertex");
		String edgesObject = new String("edges");
		String pathObject = new String("path");
		
		String ids = "";
		JSONObject newJSONObject = null;
		JSONArray annotations = null;
		for (int i = 0; i < array.size(); i++){
			JSONObject o = (JSONObject) array.get(i);
			
			if (o.containsKey(vertexObject)){
				
				Object objectJson =  o.get(vertexObject);
				Gson g = new Gson();				
				String object = g.toJson(objectJson);
				
				JSONObject newJS = (JSONObject) JSONValue.parse(object);
				
				//Do not include more than once each annotations in the JSONArray
				String id = (String) newJS.get("id");
				if(!ids.contains(id)){
					
					//store the object together with the annotations
					if(!ids.equals("")){
						if (annotations!=null && !annotations.isEmpty())
							newJSONObject.put("annotationContexts", annotations);
						modifiy.add(newJSONObject);
					}
					
					//reset Json objects					
					newJSONObject = null;
					annotations = null;
					newJSONObject = new JSONObject();
					annotations = new JSONArray();
					
					newJS.remove(new String("_id"));
					newJS.remove(new String("_key"));
					newJS.remove(new String("_rev"));
					
					newJSONObject.put("annotation", newJS);
					
					
					if (o.containsKey(pathObject)){
						Object pathJson =  o.get(pathObject);
						Gson gs = new Gson();				
						String path = gs.toJson(pathJson);
						
						JSONObject pathJS = (JSONObject) JSONValue.parse(path);
						
						if (pathJS.containsKey(edgesObject)){
													
							JSONArray annotationContextArray = (JSONArray) pathJS.get(edgesObject);
							
							JSONArray modifiedAnnotationContextArray = modifyAnnotationContextArray(annotationContextArray);
							//==================================================
							JSONObject annotationContextJSON = (JSONObject) modifiedAnnotationContextArray.get(0);
							
							annotations.add(annotationContextJSON);
						}
					}
					
					ids = ids + id +"|";
				}else{
					if (o.containsKey(pathObject)){
						Object pathJson =  o.get(pathObject);
						Gson gs = new Gson();				
						String path = gs.toJson(pathJson);
						
						JSONObject pathJS = (JSONObject) JSONValue.parse(path);
						
						if (pathJS.containsKey(edgesObject)){
													
							JSONArray annotationContextArray = (JSONArray) pathJS.get(edgesObject);
							
							JSONArray modifiedAnnotationContextArray = modifyAnnotationContextArray(annotationContextArray);
							//==================================================
							JSONObject annotationContextJSON = (JSONObject) modifiedAnnotationContextArray.get(0);
							
							//modifiy.add(modifiedAnnotationContextArray);
							annotations.add(annotationContextJSON);
							//newJSONObject.put("annotationContext", annotationContextJSON);
						}
					}
				}
							
			}
			
		}
		//add the last object
		if (annotations!=null && !annotations.isEmpty())
			newJSONObject.put("annotationContexts", annotations);
		if (newJSONObject != null)
			modifiy.add(newJSONObject);
		
		return modifiy;
	}
	
	/**
	 * Remove useless information about annotationContexts
	 * @param annotationContextArray annotationContextArray from ArangoDB
	 * @return modified array without good-for-nothing information
	 */
	private JSONArray modifyAnnotationContextArray(JSONArray annotationContextArray){
		JSONArray modified = new JSONArray();
		
		for (Object newJS:annotationContextArray){
			//JSONObject newJS = (JSONObject) JSONValue.parse(edge);
				JSONObject newAnnotationContext = (JSONObject) JSONValue.parse(newJS.toString());
				newAnnotationContext.remove(new String("_id"));
				newAnnotationContext.remove(new String("_key"));
				newAnnotationContext.remove(new String("_rev"));
				newAnnotationContext.remove(new String("_from"));
				newAnnotationContext.remove(new String("_to"));
				modified.add(newAnnotationContext);
			}
		return modified;
		
	}
	
	private String getId(){
		String id = "";
		IdGenerateClientClass client = new IdGenerateClientClass(idGeneratingService);
		id = client.sendRequest(SERVICE);
		
		return id;
	}
	
	/**
	 * Method to compose string for search queries
	 * @param part part that we want to select
	 * @return composed string for the query
	 */
	private String composeSelectPartsForSearchByKeyword(String part){
		String returnPart = "";
		//Collection<Map<String,String>> maps = new HashSet<Map<String,String>>();
		
		HashMap<String, String> item = new HashMap<String, String>();
		
		item.put("objectCollection", "CONCAT(SPLIT(u.vertex._id, '/', 1),'')");
		//maps.add(item);
		
		//item.clear();
		item.put("objectId", "u.vertex.id");
		//maps.add(item);
		
		//item.clear();
		item.put("position", "u.path.edges[0].position");
		//maps.add(item);

		//item.clear();
		item.put("time", "u.path.edges[0].time");
		//maps.add(item);

		//item.clear();
		item.put("contextId", "u.path.edges[0].id");
		//maps.add(item);

		//item.clear();
		item.put("duration", "u.path.edges[0].duration");
		//maps.add(item);

		//item.clear();
		item.put("collection", "CONCAT(SPLIT(i._id, '/', 1),'')");
		//maps.add(item);

		//item.clear();
		item.put("id", "i.id");
		//maps.add(item);

		//item.clear();
		item.put("title", "i.title");
		//maps.add(item);

		//item.clear();
		item.put("text", "i.text");
		//maps.add(item);

		//item.clear();
		item.put("keywords", "i.keywords");
		//maps.add(item);

		//item.clear();
		item.put("location", "i.annotationData.location");
		//maps.add(item);

		String parts[] = part.split(",");
		
		if (parts[0].equals("*")){
			for(Entry<String, String> entry : item.entrySet()){
					String partSmall = "'" + entry.getKey() + "': " + entry.getValue() + ",";
					returnPart += partSmall;
			}
		}else{
			for(String p:parts){
				if(item.containsKey(p)){
					String partSmall = "'" + p + "': " + item.get(p) + ",";
					returnPart += partSmall;
				}
			}
		}
		
		return returnPart;
	}
	
	/**
	 * Check if the received JSON is correct for the specific type
	 * @param geographicPosition
	 * @return if the received JSON contains all the attributes
	 */
	private boolean checkGeographicPosition(JSONObject geographicPosition){
		for(String key:geographicPositionElements){
			if(!geographicPosition.containsKey(key))
				return false;
		}
		return true;
	}
	
	/**
	 * Check if the received JSON is correct for the specific type
	 * @param time
	 * @return if the received JSON contains all the attributes
	 */
	private boolean checkTime(JSONObject time){
		for(String key:timeElements){
			if(!time.containsKey(key))
				return false;
		}
		return true;
	}
	
	/**
	 * Convert a String to JSONObject.
	 * @param jsonString input String
	 * @return return the JSONObject
	 */
	private JSONObject convertStringToJSON(String jsonString){
		Object objectJson = (Object) jsonString;
		Gson g = new Gson();				
		String object = g.toJson(objectJson);
		
		return (JSONObject) JSONValue.parse(object);
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

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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.logging.Logger;

import com.arangodb.ArangoDriver;
import com.arangodb.entity.CursorEntity;
import com.arangodb.entity.DocumentEntity;
import com.arangodb.entity.EdgeDefinitionEntity;
import com.arangodb.entity.EdgeEntity;
import com.arangodb.entity.GraphEntity;
import com.arangodb.entity.PlainEdgeEntity;
import com.arangodb.util.MapBuilder;
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
	GraphEntity graphNew;
	DocumentEntity<Video>  id1, id2;
	DocumentEntity<Annotation>  ann1, ann2, ann3;

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
	@Path("graph")
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
				EdgeDefinitionEntity edgeDefHasAnnotated = new EdgeDefinitionEntity();

				// Define the edge collection...
				edgeDefHasAnnotated.setCollection("newAnnotated");

				// ... and the vertex collection(s) where an edge starts...
				List<String> from = new ArrayList<String>();
				from.add("Videos");
				edgeDefHasAnnotated.setFrom(from);

				// ... and ends.
				List<String> to = new ArrayList<String>();
				to.add("Annotations");
				edgeDefHasAnnotated.setTo(to);

				// add the edge definition to the list
				edgeDefinitions.add(edgeDefHasAnnotated);

				// We do not need any orphan collections, so this is just an empty list
				List<String> orphanCollections = new ArrayList<String>();
				orphanCollections.add("Videos");
				orphanCollections.add("Annotations");

				// Create the graph:
				graphNew = conn.createGraph("Video", edgeDefinitions, orphanCollections, true);
				
				result = "Name: " + graphNew.getName() + "Key: " + graphNew.getDocumentHandle();
				
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
	
	/**
	 * Add new vertexes
	 * @return HttpResponse 
	 */
	
	@PUT
	@Path("vertex")
	@Summary("Insert new vertex")
	@Notes("Requires authentication.")
	@ApiResponses(value={
			@ApiResponse(code = 200, message = "Vertex saved successfully."),
			@ApiResponse(code = 401, message = "User is not authenticated."),
			@ApiResponse(code = 409, message = "Vertex already exists."),
			@ApiResponse(code = 500, message = "Internal error.")	
	})
	public HttpResponse addNewVertex(){
		
		String result = "";
		ArangoDriver conn = null;
		PreparedStatement stmnt = null;
		ResultSet rs = null;
		java.util.Date date= new java.util.Date();
		try {
			JSONObject o;
			conn= dbm.getConnection();
			if(getActiveAgent().getId() == getActiveNode().getAnonymous().getId()){
				
				/*id1 = conn.graphCreateVertex("Video", "Videos", new Video("1", "TestVideo1"), true);
				id2 = conn.graphCreateVertex("Video", "Videos", new Video("2", "TestVideo2"), true);
				ann1 = conn.graphCreateVertex("Video", "Annotations", new Annotation("1", "Annotation1"), true);
				ann2 = conn.graphCreateVertex("Video", "Annotations", new Annotation("2", "Annotation2"), true);
				ann3 = conn.graphCreateVertex("Video", "Annotations", new Annotation("3", "Annotation3"), true);*/
				
				for(int i = 500; i < 1000; i++) {
					conn.graphCreateVertex("Video", "Videos", new Video(Integer.toString(i), 
							"TestVideo" + Integer.toString(i) ), true);
				}
				conn = null;
				conn= dbm.getConnection();
				for(int i = 500; i < 1000; i++) {
					ann3 = conn.graphCreateVertex("Video", "Annotations", 
							new Annotation(Integer.toString(i), "Annotation"  + Integer.toString(i)), true);
				}
				result = "Comleted Succesfully";

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

	/**
	 * Add new vertexes
	 * @return HttpResponse 
	 */
	
	@PUT
	@Path("edge")
	@Summary("Insert new edge")
	@Notes("Requires authentication.")
	@ApiResponses(value={
			@ApiResponse(code = 200, message = "Edge saved successfully."),
			@ApiResponse(code = 401, message = "User is not authenticated."),
			@ApiResponse(code = 409, message = "Edge already exists."),
			@ApiResponse(code = 500, message = "Internal error.")	
	})
	public HttpResponse addNewEdge(){
		
		String result = "";
		ArangoDriver conn = null;
		PreparedStatement stmnt = null;
		ResultSet rs = null;
		java.util.Date date= new java.util.Date();
		ArrayList<String> videoID = new ArrayList<String>();
		ArrayList<String> annotationID;
		Random rand = new Random();
		try {
			JSONObject o;
			conn= dbm.getConnection();
			
			if(getActiveAgent().getId() == getActiveNode().getAnonymous().getId()){
				
				 conn = null;
				 conn= dbm.getConnection();
								
				String geteAnnotationID = "for i in GRAPH_VERTICES('Video',  null,{vertexCollectionRestriction : 'Annotations' }) return i._id";
			    //Map<String, Object> bindVarseAnnotation = new MapBuilder().put("id", exampleAnnotationMap).get();
			    CursorEntity<String> reseAnnotationById = conn.executeQuery(geteAnnotationID, null, String.class, true, 500);
			    Iterator<String> iteratorAnnotationById = reseAnnotationById.iterator();
			    String annKey = "";
			    annotationID = new ArrayList<String>();
			    while(iteratorAnnotationById.hasNext()) {
			    	annotationID.add(iteratorAnnotationById.next());
			    }
			    
			    
			    conn = null;
			    conn= dbm.getConnection();
			    
			    String getVertexByID = "for i in GRAPH_VERTICES('Video',  {},{vertexCollecitonRestriction : 'Videos' }) return i._id";
				CursorEntity<String> resVertexById = conn.executeQuery(getVertexByID, null, String.class, true, 500);
			    Iterator<String> iteratorVertexById = resVertexById.iterator();
			    String videoKey = ""; 
				
			    while( iteratorVertexById.hasNext()) {
			    	videoID.add(iteratorVertexById.next());
			    }
			    
			    conn = null;
			    conn= dbm.getConnection();
			    
			    
				for(int i = 0; i < 400; i++) {
					int rand_indexVideo = Math.abs(rand.nextInt() % 500);
					int rand_indexAnnotation = Math.abs(rand.nextInt() % 500);
					double rand_startTime = rand.nextDouble();
					double rand_duration = rand.nextDouble();
				EdgeEntity<?> edge1 = conn.graphCreateEdge(
					      "Video",
					      "newAnnotated",
					      null,
					      videoID.get(rand_indexVideo),
					      annotationID.get(rand_indexAnnotation),
					      new VideoEdge(rand_startTime, rand_duration),
					      null);
				EdgeEntity<?> edge2 = conn.graphCreateEdge(
					      "Video",
					      "newAnnotated",
					      null,
					      videoID.get(rand_indexVideo),
					      annotationID.get(rand_indexAnnotation),
					      new VideoEdge(rand_startTime, rand_duration),
					      null);
				}
				
				//id1 = conn.graphCreateVertex("Video", "Videos", new Video("503", "TestVideo503"), true);
				
			    //Map<String, Object> bindVars = new MapBuilder().put("id", exampleVertexMap).get();
			    /*
			    conn = null;
			    conn= dbm.getConnection();
			    String geteAnnotationID = "for i in GRAPH_VERTICES('Video', {},{vertexCollecitonRestriction:'Annotations'}) return i._id";
			    //Map<String, Object> bindVarseAnnotation = new MapBuilder().put("id", exampleAnnotationMap).get();
			    CursorEntity<String> reseAnnotationById = conn.executeQuery(geteAnnotationID, null, String.class, true, 500);
			    Iterator<String> iteratorAnnotationById = reseAnnotationById.iterator();
			    String annKey = "";
			    while(iteratorAnnotationById.hasNext()) {
			    	annotationID.add(iteratorAnnotationById.next());
			    }
			    conn = null;
			    conn = dbm.getConnection();
				for(int i = 0;i<1000;i++){
					Random r = new Random();
					int indexVideo = Math.abs(r.nextInt() % 500);
					int indexAnnotation = Math.abs(r.nextInt() % 500);
				    
				    VideoEdge ve = new VideoEdge( 10*r.nextDouble(),r.nextDouble());
				    EdgeEntity<?> edge1 = conn.graphCreateEdge("Video", "newAnnotated", null, 
				    		videoID.get(indexVideo), annotationID.get(indexAnnotation),
						      ve,
						      true);
				    EdgeEntity<?> edge2 = conn.graphCreateEdge(
						      "Video",
						      "newAnnotated", null,
						      videoID.get(indexVideo),
						      annotationID.get(indexAnnotation),
						      new VideoEdge( 10*r.nextDouble(),r.nextDouble()),
						      true);
				    Logger.getLogger("Test");
				   }
				   */
								
				/*EdgeEntity<String> edg1 = conn.graphCreateEdge("Video", "annotated", null, id1.getDocumentHandle(), ann1.getDocumentHandle());
				EdgeEntity<String> edg2 = conn.graphCreateEdge("Video", "annotated", null, id1.getDocumentHandle(), ann3.getDocumentHandle());
				EdgeEntity<String> edg3 = conn.graphCreateEdge("Video", "annotated", null, id2.getDocumentHandle(), ann2.getDocumentHandle());
				EdgeEntity<String> edg4 = conn.graphCreateEdge("Video", "annotated", null, id2.getDocumentHandle(), ann3.getDocumentHandle());*/
				
				result = "Comleted Succesfully";
				
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
	
	@GET
	@Path("graph")
	@ResourceListApi(description = "Return details for a selected graph")
	@Summary("return a JSON with graph details stored for the given graph Name")
	@Notes("query parameter selects the columns that need to be returned in the JSON.")
	@ApiResponses(value={
			@ApiResponse(code = 200, message = "Video details"),
			@ApiResponse(code = 404, message = "Video id does not exist"),
			@ApiResponse(code = 500, message = "Internal error"),
	})
	public HttpResponse getGraphDetails() {
		String selectquery ="";
		ArangoDriver conn = null;
		PreparedStatement stmnt = null;
		ResultSet rs = null;
		JSONObject ro=null;
		JSONArray qs = new JSONArray();
		try {
			// get connection from connection pool
			conn = dbm.getConnection();
			selectquery = "for v in GRAPH_VERTICES ('Video', {}) return v";
		    String query = "for i in GRAPH_EDGES('Video', null) return i";
		    
		    CursorEntity<VideoEdge> res = conn.executeQuery(query, null, VideoEdge.class, true, 100);
		   
		    Iterator<VideoEdge> iterator = res.iterator();
		    while(iterator.hasNext()) {
		    	ro = new JSONObject();
		    	VideoEdge edge = (VideoEdge) iterator.next();
		    	ro.put("From: ", edge.getFromCollection());
		    	ro.put("To: ", edge.getToCollection());
		    	ro.put("Start Time ", edge.getStartTime());
		    	ro.put("Duration ", edge.getDuration());
		    	qs.add(ro);
		    }
		    
		    
		    CursorEntity<Video> resVertex = conn.executeQuery(selectquery, null, Video.class, true, 500);
		    Iterator<Video> iteratorVertex = resVertex.iterator();
		    while(iteratorVertex.hasNext()) {
		    	ro = new JSONObject();
		    	Video v = (Video) iteratorVertex.next();
		    	ro.put("Id ", v.getId());
		    	ro.put("Title ", v.getTitle());
		    	qs.add(ro);
		    }
		    
		    Map<String, Object> exampleVertexMap = new MapBuilder().put("id", "20").get();
		    String getVertexByID = "for i in GRAPH_VERTICES('Video', @id,{vertexCollecitonRestriction:'Videos'}) return i";
		    Map<String, Object> bindVars = new MapBuilder().put("id", exampleVertexMap).get();
		    CursorEntity<Video> resVertexById = conn.executeQuery(getVertexByID, bindVars, Video.class, true, 1);
		    Iterator<Video> iteratorVertexById = resVertexById.iterator();
		    Video v = null;
		    if(iteratorVertexById.hasNext()) {
		    	ro = new JSONObject();
		    	v = (Video) iteratorVertexById.next();
		    	ro.put("SelectedId ", v.getId());
		    	ro.put("SelectedTitle ", v.getTitle());
		    	qs.add(ro);
		    }
		    
		    String getAnnotations = "for i in GRAPH_NEIGHBORS('Video', @selectedVideo, {endVertexCollectionRestriction : 'Annotations'}) return i.vertex";
		    Map<String, Object> bindVars2 = new MapBuilder().put("selectedVideo",v).get();
		    
		    CursorEntity<Annotation> resAnnotation = conn.executeQuery(getAnnotations, bindVars2, Annotation.class, true, 100);
		    
		    qs.add("Vertices that have inbound edge with TestVideo 20");
		    Iterator<Annotation> iteratorAnnotation = resAnnotation.iterator();
		    while(iteratorAnnotation.hasNext()) {
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

package i5.las2peer.services.annotations;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import i5.las2peer.p2p.LocalNode;
import i5.las2peer.restMapper.data.Pair;
import i5.las2peer.security.ServiceAgent;
import i5.las2peer.security.UserAgent;
import i5.las2peer.services.annotations.AnnotationsService;
import i5.las2peer.testing.MockAgentFactory;
import i5.las2peer.webConnector.WebConnector;
import i5.las2peer.webConnector.client.ClientResponse;
import i5.las2peer.webConnector.client.MiniClient;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Random;

import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;
import net.minidev.json.parser.ParseException;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;


/**
 *Test Class  
 *
 */
public class ServiceTest {
	
	private static final String HTTP_ADDRESS = "http://127.0.0.1";
    private static final int HTTP_PORT = WebConnector.DEFAULT_HTTP_PORT;
	
	private static LocalNode node;
	private static WebConnector connector;
	private static ByteArrayOutputStream logStream;
	
	private static UserAgent testAgent;
	private static final String testPass = "adamspass";
	
	private static final String testServiceClass = "i5.las2peer.services.annotations.AnnotationsService";
	
	private static final String mainPath = "annotations/";
	
	private static final String objectCollection = "Videos";
	private static final String objectCollectionSecond = "Images";
	private static final String annotationCollection = "TextTypeAnnotation";
	private static final String annotationCollectionSecond = "LocationTypeAnnotation";
	
	/**
	 * Called before the tests start.
	 * 
	 * Sets up the node and initializes connector and users that can be used throughout the tests.
	 * 
	 * @throws Exception
	 */
	@BeforeClass
	public static void startServer() throws Exception {
		
		//start node
		node = LocalNode.newNode();
		node.storeAgent(MockAgentFactory.getAdam());
		node.launch();
		
		ServiceAgent testService = ServiceAgent.generateNewAgent(testServiceClass, "a pass");
		testService.unlockPrivateKey("a pass");
		
		node.registerReceiver(testService);
		
		//start connector
		logStream = new ByteArrayOutputStream ();
		
		connector = new WebConnector(true,HTTP_PORT,false,1000);
		connector.setSocketTimeout(10000);
		connector.setLogStream(new PrintStream (logStream));
		connector.start ( node );
        Thread.sleep(1000); //wait a second for the connector to become ready
		testAgent = MockAgentFactory.getAdam();
		
        connector.updateServiceList();
        //avoid timing errors: wait for the repository manager to get all services before continuing
        try
        {
            System.out.println("waiting..");
            Thread.sleep(10000);
        }
        catch(InterruptedException e)
        {
            e.printStackTrace();
        }
		
	}
	
	
	/**
	 * Called after the tests have finished.
	 * Shuts down the server and prints out the connector log file for reference.
	 * 
	 * @throws Exception
	 */
	@AfterClass
	public static void shutDownServer () throws Exception {
		
		connector.stop();
		node.shutDown();
		
        connector = null;
        node = null;
        
        LocalNode.reset();
		
		System.out.println("Connector-Log:");
		System.out.println("--------------");
		
		System.out.println(logStream.toString());
		
    }
	
	
	/**
	 * Test the ServiceClass for valid rest mapping.
	 * Important for development.
	 */
	@Test
	public void testDebugMapping()
	{
		AnnotationsService cl = new AnnotationsService();
		assertTrue(cl.debugMapping());
	}
	
	/**
	 * Tests the AnnotationService for getting all collections
	 */
	@Test
	public void testGetCollections()
	{
		//AnnotationsClass cl = new AnnotationsClass();
		MiniClient c = new MiniClient();
		c.setAddressPort(HTTP_ADDRESS, HTTP_PORT);
		try
		{
			c.setLogin(Long.toString(testAgent.getId()), testPass);
			
			//retrieve the collection information
			ClientResponse select=c.sendRequest("GET", mainPath +"collections", ""); 
            assertEquals(200, select.getHttpCode());
            assertTrue(select.getResponse().trim().contains(objectCollection)); 
			System.out.println("Result of select in 'testGetCollections': " + select.getResponse().trim());
			
		}
		catch(Exception e)
		{
			e.printStackTrace();
			fail ( "Exception: " + e );
		}
		
	}
	
	/**
	 * Tests the AnnotationService for adding new nodes (for objects)
	 */
	@Test
	public void testCreateObjectNode()
	{
		//AnnotationsClass cl = new AnnotationsClass();
		MiniClient c = new MiniClient();
		c.setAddressPort(HTTP_ADDRESS, HTTP_PORT);
		JSONObject o;
		try
		{
			c.setLogin(Long.toString(testAgent.getId()), testPass);
			
			//add a new object
            ClientResponse result=c.sendRequest("POST", mainPath +"objects", "{\"collection\": \"" + objectCollection + "\", \"toolId\":\"TestCase\"}", "application/json", "*/*", new Pair[]{});
            assertEquals(200, result.getHttpCode());
            assertTrue(result.getResponse().trim().contains("id")); 
			System.out.println("Result of 'testCreateObjectNode': " + result.getResponse());
			try{	
				o = (JSONObject) JSONValue.parseWithException(result.getResponse());
			} catch (ParseException e1) {
				throw new IllegalArgumentException("Data is not valid JSON!");
			}
			String id = (String) o.get(new String("id"));
			
			//check if object exists -> should pass
			//retrieve the object information
			ClientResponse select=c.sendRequest("GET", mainPath +"objects/" + id + "?part=id", ""); 
            assertEquals(200, select.getHttpCode());
            assertTrue(select.getResponse().trim().contains(id)); 
			System.out.println("Result of select in 'testCreateObjectNode': " + select.getResponse().trim());
			
			//add same object -> should fail with corresponding message
			/*ClientResponse insertAgain=c.sendRequest("POST", mainPath +"objects", "{\"collection\": \"" + objectCollection + "\", \"toolId\":\"TestCase\"}"); 
            assertEquals(409, insertAgain.getHttpCode());
            assertTrue(insertAgain.getResponse().trim().contains("already")); 
			System.out.println("Result of try insert again 'testCreateObjectNode': " + insertAgain.getResponse().trim());*/
			
			//delete object
			ClientResponse delete=c.sendRequest("DELETE", mainPath +"objects/" + id + "", ""); 
            assertEquals(200, delete.getHttpCode());
            assertTrue(delete.getResponse().trim().contains("deleted"));
            System.out.println("Result of delete in 'testCreateObjectNode': " + delete.getResponse().trim());
            
            //check if object exists -> should fail
			ClientResponse selectAfterDelete=c.sendRequest("GET", mainPath +"objects/" + id + "?part=id", ""); 
            assertEquals(404, selectAfterDelete.getHttpCode());
            assertTrue(selectAfterDelete.getResponse().trim().contains("not")); 
			System.out.println("Result of select after delete in 'testCreateObjectNode': " + selectAfterDelete.getResponse().trim());
		}
		catch(Exception e)
		{
			e.printStackTrace();
			fail ( "Exception: " + e );
		}
		
	}
	
		
	/**
	 * Tests the AnnotationService for adding new nodes (as Annotation)
	 */
	@Test
	public void testCreateAnnotationNode()
	{
		//AnnotationsClass cl = new AnnotationsClass();
		MiniClient c = new MiniClient();
		c.setAddressPort(HTTP_ADDRESS, HTTP_PORT);
		JSONObject o, object;
		try
		{
			c.setLogin(Long.toString(testAgent.getId()), testPass);
			
			//add a new object
            ClientResponse addObjectCollection=c.sendRequest("POST", mainPath +"objects", "{\"collection\": \"" + objectCollection + "\", \"toolId\":\"TestCase\"}", "application/json", "*/*", new Pair[]{});
            assertEquals(200, addObjectCollection.getHttpCode());
            assertTrue(addObjectCollection.getResponse().trim().contains("id")); 
			System.out.println("Result of 'testCreateAnnotationNode': " + addObjectCollection.getResponse());
			try{	
				object = (JSONObject) JSONValue.parseWithException(addObjectCollection.getResponse());
			} catch (ParseException e1) {
				throw new IllegalArgumentException("Data is not valid JSON!");
			}
			String objectId = (String) object.get(new String("id"));
				
			//add a new annotation
			ClientResponse result=c.sendRequest("POST", mainPath +"annotations", "{\"collection\": \"" + annotationCollection + "\","
					+ " \"title\": \"Annotation Insert Test\" ,\"keywords\": \"test annotation\", \"objectId\": " + "\"" + objectId + "\"" + ","
					+ " \"location\": \"Microservice Test Class\", \"toolId\":\"TestCase\"}", "application/json", "*/*", new Pair[]{}); 
	        assertEquals(200, result.getHttpCode());
	        assertTrue(result.getResponse().trim().contains("id")); 
			System.out.println("Result of insert 'testCreateAnnotationNode': " + result.getResponse().trim());
			try{	
				o = (JSONObject) JSONValue.parseWithException(result.getResponse());
			} catch (ParseException e1) {
				throw new IllegalArgumentException("Data is not valid JSON!");
			}
			String id = (String) o.get(new String("id"));
			
			//retrieve the annotation 			
			//check if annotation exists
			ClientResponse select=c.sendRequest("GET", mainPath +"objects/" + id +"?part=id,title", ""); 
            assertEquals(200, select.getHttpCode());
            assertTrue(select.getResponse().trim().contains("Annotation Insert Test")); 
			System.out.println("Result of select in 'testCreateAnnotationNode': " + select.getResponse().trim());
			
			//add same annotation
			/*ClientResponse insertAgain=c.sendRequest("POST", mainPath +"annotations", "{\"collection\": \"" + annotationCollection + "\", 
			 * \"Title\": \"Annotation Insert Test\" , \"Location\": \"Microservice Test Class\"}"); 
	        assertEquals(409, insertAgain.getHttpCode());
	        assertTrue(insertAgain.getResponse().trim().contains("already")); 
			System.out.println("Result of insert again 'testCreateAnnotationNode': " + insertAgain.getResponse().trim());*/
			
			//delete annotation
			ClientResponse delete=c.sendRequest("DELETE", mainPath +"objects/" + id +"", ""); 
            assertEquals(200, delete.getHttpCode());
            assertTrue(delete.getResponse().trim().contains("deleted"));
            System.out.println("Result of delete in 'testCreateAnnotationNode': " + delete.getResponse().trim());
			
			//check if annotation exists
            ClientResponse selectAgain=c.sendRequest("GET", mainPath +"objects/" + id +"?part=id,title", ""); 
            assertEquals(404, selectAgain.getHttpCode());
            assertTrue(selectAgain.getResponse().trim().contains("not")); 
			System.out.println("Result of select again in 'testCreateAnnotationNode': " + selectAgain.getResponse().trim());
		
			//delete object
			ClientResponse deleteObjectCollection=c.sendRequest("DELETE", mainPath +"objects/" + objectId +"", ""); 
            assertEquals(200, deleteObjectCollection.getHttpCode());
            assertTrue(deleteObjectCollection.getResponse().trim().contains("deleted"));
            System.out.println("Result of delete in 'testCreateAnnotationNode': " + deleteObjectCollection.getResponse().trim());
		}
		catch(Exception e)
		{
			e.printStackTrace();
			fail ( "Exception: " + e );
		}
	}
	
	
	/**
	 * Tests the AnnotationService for adding new PlaceTypeAnnotations
	 */
	@Test
	public void testCreatePlaceTypeAnnotationNode()
	{
		//AnnotationsClass cl = new AnnotationsClass();
		MiniClient c = new MiniClient();
		c.setAddressPort(HTTP_ADDRESS, HTTP_PORT);
		JSONObject o, object;
		try
		{
			c.setLogin(Long.toString(testAgent.getId()), testPass);
			
			//add a new object
            ClientResponse addObjectCollection=c.sendRequest("POST", mainPath +"objects", "{\"collection\": \"" + objectCollection + "\", \"toolId\":\"TestCase\"}", "application/json", "*/*", new Pair[]{});
            assertEquals(200, addObjectCollection.getHttpCode());
            assertTrue(addObjectCollection.getResponse().trim().contains("id")); 
			System.out.println("Result of 'testCreatePlaceTypeAnnotationNode': " + addObjectCollection.getResponse());
			try{	
				object = (JSONObject) JSONValue.parseWithException(addObjectCollection.getResponse());
			} catch (ParseException e1) {
				throw new IllegalArgumentException("Data is not valid JSON!");
			}
			String objectId = (String) object.get(new String("id"));
				
			//add a new annotation
			ClientResponse result=c.sendRequest("POST", mainPath +"annotations/placetype", "{\"collection\": \"" + annotationCollection + "\","
					+ " \"title\": \"Annotation Insert Test\" ,\"keywords\": \"test annotation\", \"objectId\": " + "\"" + objectId + "\"" + ","
					+ " \"location\": \"Microservice Test Class\", \"toolId\":\"TestCase\", "
					+ " \"geographicPosition\":{\"altitude\":\"100\", \"latitude\":\"100\", "
					+ " \"longitude\":\"100\"} }", "application/json", "*/*", new Pair[]{}); 
	        assertEquals(200, result.getHttpCode());
	        assertTrue(result.getResponse().trim().contains("id")); 
			System.out.println("Result of insert 'testCreatePlaceTypeAnnotationNode': " + result.getResponse().trim());
			try{	
				o = (JSONObject) JSONValue.parseWithException(result.getResponse());
			} catch (ParseException e1) {
				throw new IllegalArgumentException("Data is not valid JSON!");
			}
			String id = (String) o.get(new String("id"));
			
			//retrieve the annotation 			
			//check if annotation exists
			ClientResponse select=c.sendRequest("GET", mainPath +"objects/" + id +"?part=id,title", ""); 
            assertEquals(200, select.getHttpCode());
            assertTrue(select.getResponse().trim().contains("Annotation Insert Test")); 
			System.out.println("Result of select in 'testCreatePlaceTypeAnnotationNode': " + select.getResponse().trim());
			
			//add same annotation
			/*ClientResponse insertAgain=c.sendRequest("POST", mainPath +"annotations", "{\"collection\": \"" + annotationCollection + "\", 
			 * \"Title\": \"Annotation Insert Test\" , \"Location\": \"Microservice Test Class\"}"); 
	        assertEquals(409, insertAgain.getHttpCode());
	        assertTrue(insertAgain.getResponse().trim().contains("already")); 
			System.out.println("Result of insert again 'testCreateAnnotationNode': " + insertAgain.getResponse().trim());*/
			
			//delete annotation
			ClientResponse delete=c.sendRequest("DELETE", mainPath +"objects/" + id +"", ""); 
            assertEquals(200, delete.getHttpCode());
            assertTrue(delete.getResponse().trim().contains("deleted"));
            System.out.println("Result of delete in 'testCreatePlaceTypeAnnotationNode': " + delete.getResponse().trim());
			
			//check if annotation exists
            ClientResponse selectAgain=c.sendRequest("GET", mainPath +"objects/" + id +"?part=id,title", ""); 
            assertEquals(404, selectAgain.getHttpCode());
            assertTrue(selectAgain.getResponse().trim().contains("not")); 
			System.out.println("Result of select again in 'testCreatePlaceTypeAnnotationNode': " + selectAgain.getResponse().trim());
		
			//delete object
			ClientResponse deleteObjectCollection=c.sendRequest("DELETE", mainPath +"objects/" + objectId +"", ""); 
            assertEquals(200, deleteObjectCollection.getHttpCode());
            assertTrue(deleteObjectCollection.getResponse().trim().contains("deleted"));
            System.out.println("Result of delete in 'testCreatePlaceTypeAnnotationNode': " + deleteObjectCollection.getResponse().trim());
		}
		catch(Exception e)
		{
			e.printStackTrace();
			fail ( "Exception: " + e );
		}
	}
	
	/**
	 *  Tests adding an annotation to an object (by creating the corresponding annotationContext)
	 */
	@Test
	public void testAddAnnotationToObject()
	{
		//AnnotationsClass cl = new AnnotationsClass();
		MiniClient c = new MiniClient();
		c.setAddressPort(HTTP_ADDRESS, HTTP_PORT);
		JSONObject object;
		JSONObject annotation;
		JSONObject annotationContext;
		try
		{
			c.setLogin(Long.toString(testAgent.getId()), testPass);	
			
			//add a new object
            ClientResponse addObjectCollection=c.sendRequest("POST", mainPath +"objects", "{\"collection\": \"" + objectCollection + "\", \"toolId\":\"TestCase\"}", "application/json", "*/*", new Pair[]{});
            assertEquals(200, addObjectCollection.getHttpCode());
            assertTrue(addObjectCollection.getResponse().trim().contains("id")); 
			System.out.println("Result of 'testAddAnnotationToObject': " + addObjectCollection.getResponse());
			try{	
				object = (JSONObject) JSONValue.parseWithException(addObjectCollection.getResponse());
			} catch (ParseException e1) {
				throw new IllegalArgumentException("Data is not valid JSON!");
			}
			String objectId = (String) object.get(new String("id"));
			
			//check if object exists -> should pass
			//retrieve the object information
			ClientResponse selectObject=c.sendRequest("GET", mainPath +"objects/" + objectId + "?part=id", ""); 
            assertEquals(200, selectObject.getHttpCode());
            assertTrue(selectObject.getResponse().trim().contains(objectId)); 
			System.out.println("Result of get object @ 'testAddAnnotationToObject': " + selectObject.getResponse().trim());
			
			//create a new annotation for the object
			ClientResponse addAnnotation=c.sendRequest("POST", mainPath +"annotations", "{\"collection\": \"" + annotationCollection + "\","
					+ " \"title\": \"Annotation Insert Test\" ,\"keywords\": \"test annotation\", \"objectId\": " + "\"" + objectId + "\"" + ","
					+ " \"location\": \"Microservice Test Class\", \"toolId\":\"TestCase\"}", "application/json", "*/*", new Pair[]{});
	        assertEquals(200, addAnnotation.getHttpCode());
	        assertTrue(addAnnotation.getResponse().trim().contains("id")); 
			System.out.println("Result of insert 'testAddAnnotationToObject': " + addAnnotation.getResponse().trim());
			try{	
				annotation = (JSONObject) JSONValue.parseWithException(addAnnotation.getResponse());
			} catch (ParseException e1) {
				throw new IllegalArgumentException("Data is not valid JSON!");
			}
			String annotationId = (String) annotation.get(new String("id"));
			String annotationContextId = (String) annotation.get(new String("annotationContextId"));			
			
			//check if annotation exists
			ClientResponse selectAnnotation=c.sendRequest("GET", mainPath +"objects/" + annotationId + "?part=id,title", ""); 
            assertEquals(200, selectAnnotation.getHttpCode());
            assertTrue(selectAnnotation.getResponse().trim().contains(annotationId)); 
			System.out.println("Result of select in 'testAddAnnotationToObject': " + selectAnnotation.getResponse().trim());
			
			//add new annotationContext
			ClientResponse updateAnnotationContext=c.sendRequest("PUT", mainPath +"annotationContexts/" + annotationContextId + "", "{ "
					+ "\"position\": { \"x\": \"10\", \"y\": \"10\", \"z\": \"10\"}, "
					+ "\"time\": \"1.324\", \"duration\": \"0.40\" }", "application/json", "*/*", new Pair[]{});
	        assertEquals(200, updateAnnotationContext.getHttpCode());
	        assertTrue(updateAnnotationContext.getResponse().trim().contains("id")); 
			System.out.println("Result of insertAnnotationContext @ 'testAddAnnotationToObject': " + updateAnnotationContext.getResponse().trim());
			try{	
				annotationContext = (JSONObject) JSONValue.parseWithException(updateAnnotationContext.getResponse());
			} catch (ParseException e1) {
				throw new IllegalArgumentException("Data is not valid JSON!");
			}
			annotationContextId = (String) annotationContext.get(new String("id"));
			
			//check if the annotationContext exists
			ClientResponse selectAnnotationContext=c.sendRequest("GET", mainPath +"annotationContexts/" + objectId + "/" + annotationId + "?part=id&collection=Annotated", ""); 
            assertEquals(200, selectAnnotationContext.getHttpCode());
            assertTrue(selectAnnotationContext.getResponse().trim().contains(annotationContextId)); 
			System.out.println("Result of select in 'testAddAnnotationToObject': " + selectAnnotationContext.getResponse().trim());
			
			//create a new annotationContext (with the same information)
			ClientResponse addSecondTypeAnnotationContextAgain=c.sendRequest("POST", mainPath +"annotationContexts/" + objectId + "/" + annotationId + "", "{ "
					+ "\"position\": { \"x\": \"10\", \"y\": \"10\", \"z\": \"10\"}, "
					+ "\"time\": \"1.324\", \"duration\": \"0.40\", \"toolId\":\"TestCase\"}", "application/json", "*/*", new Pair[]{});
	        assertEquals(200, addSecondTypeAnnotationContextAgain.getHttpCode());
	        assertTrue(addSecondTypeAnnotationContextAgain.getResponse().trim().contains("id")); 
			System.out.println("Result of insertannotationContext @ 'testAddAnnotationToObject': " + addSecondTypeAnnotationContextAgain.getResponse().trim());
			try{	
				annotationContext = (JSONObject) JSONValue.parseWithException(addSecondTypeAnnotationContextAgain.getResponse());
			} catch (ParseException e1) {
				throw new IllegalArgumentException("Data is not valid JSON!");
			}
			String annotationContextId2 = (String) annotationContext.get(new String("id"));
					
			
			//retrieve the existing annotationContexts & data 
			ClientResponse selectAnnotationContextAgain=c.sendRequest("GET", mainPath +"annotationContexts/"
			+ objectId + "/" + annotationId + "?part=id,position&collection=Annotated", ""); 
            assertEquals(200, selectAnnotationContextAgain.getHttpCode());
            System.out.println("Result of select in 'testAddAnnotationToObject': " + selectAnnotationContextAgain.getResponse());
            assertTrue(selectAnnotationContextAgain.getResponse().trim().contains(annotationContextId));
            assertTrue(selectAnnotationContextAgain.getResponse().trim().contains(annotationContextId2));
			System.out.println("Result of select in 'testAddAnnotationToObject': " + selectAnnotationContextAgain.getResponse().trim());
			
			//delete all annotationContexts 
			ClientResponse deleteAnnotationContext=c.sendRequest("DELETE", mainPath +"annotationContexts/" + annotationContextId + "", ""); 
            assertEquals(200, deleteAnnotationContext.getHttpCode());
            assertTrue(deleteAnnotationContext.getResponse().trim().contains("deleted"));
            System.out.println("Result of delete in 'testAddAnnotationToObject': " + deleteAnnotationContext.getResponse().trim());
            
            ClientResponse deleteAnnotationContext2=c.sendRequest("DELETE", mainPath +"annotationContexts/" + annotationContextId2 + "", ""); 
            assertEquals(200, deleteAnnotationContext2.getHttpCode());
            assertTrue(deleteAnnotationContext2.getResponse().trim().contains("deleted"));
            System.out.println("Result of delete in 'testAddAnnotationToObject': " + deleteAnnotationContext2.getResponse().trim());
			//check if any annotationContext still exists
			
			//delete annotation
			ClientResponse deleteAnnotation=c.sendRequest("DELETE", mainPath +"objects/" + annotationId + "", ""); 
            assertEquals(200, deleteAnnotation.getHttpCode());
            assertTrue(deleteAnnotation.getResponse().trim().contains("deleted"));
            System.out.println("Result of delete annotation in 'testAddAnnotationToObject': " + deleteAnnotation.getResponse().trim());
			
            //delete object
			ClientResponse deleteObjectCollection=c.sendRequest("DELETE", mainPath +"objects/" + objectId + "", ""); 
            assertEquals(200, deleteObjectCollection.getHttpCode());
            assertTrue(deleteObjectCollection.getResponse().trim().contains("deleted"));
            System.out.println("Result of delete object @ 'testAddAnnotationToObject': " + deleteObjectCollection.getResponse().trim());
		}
		catch(Exception e)
		{
			e.printStackTrace();
			fail ( "Exception: " + e );
		}
		
	}
	
	/**
	 *  Tests adding an annotation to an object (by creating the corresponding annotationContext)
	 */
	@Test
	public void testUpdateAnnotationToObject()
	{
		//AnnotationsClass cl = new AnnotationsClass();
		MiniClient c = new MiniClient();
		c.setAddressPort(HTTP_ADDRESS, HTTP_PORT);
		
		JSONObject object;
		JSONObject annotation;
		JSONObject annotationContext;
		
		try
		{
			c.setLogin(Long.toString(testAgent.getId()), testPass);	
			
			//add a new object
            ClientResponse addObjectCollection=c.sendRequest("POST", mainPath +"objects", "{\"collection\": \"" + objectCollection + "\", \"toolId\":\"TestCase\"}", "application/json", "*/*", new Pair[]{});
            assertEquals(200, addObjectCollection.getHttpCode());
            assertTrue(addObjectCollection.getResponse().trim().contains("id")); 
			System.out.println("Result of 'testUpdateAnnotationToObject': " + addObjectCollection.getResponse());
			try{	
				object = (JSONObject) JSONValue.parseWithException(addObjectCollection.getResponse());
			} catch (ParseException e1) {
				throw new IllegalArgumentException("Data is not valid JSON!");
			}
			String objectId = (String) object.get(new String("id"));
			
			//check if object exists -> should pass
			//retrieve the object information
			ClientResponse selectObject=c.sendRequest("GET", mainPath +"objects/" + objectId + "?part=id", ""); 
            assertEquals(200, selectObject.getHttpCode());
            assertTrue(selectObject.getResponse().trim().contains(objectId)); 
			System.out.println("Result of get object @ 'testUpdateAnnotationToObject': " + selectObject.getResponse().trim());
			
			//create a new annotation for the object
			ClientResponse addAnnotation=c.sendRequest("POST", mainPath + "annotations", "{\"collection\": \"" + annotationCollection + "\","
					+ " \"title\": \"Annotation Insert Test\" , \"objectId\": " + "\"" + objectId + "\"" + ","
					+ " \"location\": \"Microservice Test Class\", \"toolId\":\"TestCase\"}", "application/json", "*/*", new Pair[]{});
	        assertEquals(200, addAnnotation.getHttpCode());
	        assertTrue(addAnnotation.getResponse().trim().contains("id")); 
			System.out.println("Result of insert  'testUpdateAnnotationToObject': " + addAnnotation.getResponse().trim());
			try{	
				annotation = (JSONObject) JSONValue.parseWithException(addAnnotation.getResponse());
			} catch (ParseException e1) {
				throw new IllegalArgumentException("Data is not valid JSON!");
			}
			String annotationId = (String) annotation.get(new String("id"));
			String annotationContextId = (String) annotation.get(new String("annotationContextId"));	
			
			//check if annotation exists
			ClientResponse selectAnnotation=c.sendRequest("GET", mainPath +"objects/" + annotationId +"?part=id,title", ""); 
            assertEquals(200, selectAnnotation.getHttpCode());
            assertTrue(selectAnnotation.getResponse().trim().contains(annotationId)); 
			System.out.println("Result of select in 'testUpdateAnnotationToObject': " + selectAnnotation.getResponse().trim());
			
			//update empty new AnnotationContext
			ClientResponse addSecondTypeAnnotationContext=c.sendRequest("PUT", mainPath +"annotationContexts/" + annotationContextId + "", "{ "
					+ "\"position\": { \"x\": \"10\", \"y\": \"10\", \"z\": \"10\"}, "
					+ "\"time\": \"1.324\", \"duration\": \"0.40\" }", "application/json", "*/*", new Pair[]{}); 
	        assertEquals(200, addSecondTypeAnnotationContext.getHttpCode());
	        assertTrue(addSecondTypeAnnotationContext.getResponse().trim().contains("id")); 
			System.out.println("Result of insertAnnotationContext @ 'testUpdateAnnotationToObject': " + addSecondTypeAnnotationContext.getResponse().trim());
			try{	
				annotationContext = (JSONObject) JSONValue.parseWithException(addSecondTypeAnnotationContext.getResponse());
			} catch (ParseException e1) {
				throw new IllegalArgumentException("Data is not valid JSON!");
			}
			annotationContextId = (String) annotationContext.get(new String("id"));
			
			//check if the AnnotationContext exists
			ClientResponse selectAnnotationContext=c.sendRequest("GET", mainPath +"annotationContexts/" + objectId + "/" + annotationId + "?part=id", ""); 
            assertEquals(200, selectAnnotationContext.getHttpCode());
            assertTrue(selectAnnotationContext.getResponse().trim().contains(annotationContextId)); 
			System.out.println("Result of select in 'testUpdateAnnotationToObject': " + selectAnnotationContext.getResponse().trim());
			
			//update AnnotationContext
			ClientResponse updateAnnotationContext=c.sendRequest("PUT", mainPath +"annotationContexts/" + annotationContextId + "", "{ "
					+ " \"time\": \"2.167\" }", "application/json", "*/*", new Pair[]{});
	        assertEquals(200, updateAnnotationContext.getHttpCode());
	        assertTrue(updateAnnotationContext.getResponse().trim().contains("2.167")); 
			System.out.println("Result of updateAnnotationContext @ 'testUpdateAnnotationToObject': " + updateAnnotationContext.getResponse().trim());
			
			//check if the AnnotationContext updated
			ClientResponse selectUpdatedAnnotationContext=c.sendRequest("GET", mainPath +"annotationContexts/" + objectId + "/" + annotationId + "?part=id,time&collection=Annotated", ""); 
            assertEquals(200, selectUpdatedAnnotationContext.getHttpCode());
            assertTrue(selectUpdatedAnnotationContext.getResponse().trim().contains("2.167")); 
			System.out.println("Result of select in 'testUpdateAnnotationToObject': " + selectUpdatedAnnotationContext.getResponse().trim());
			
			//delete AnnotationContext 
			ClientResponse deleteAnnotationContext=c.sendRequest("DELETE", mainPath +"annotationContexts/" + annotationContextId + "", ""); 
            assertEquals(200, deleteAnnotationContext.getHttpCode());
            assertTrue(deleteAnnotationContext.getResponse().trim().contains("deleted"));
            System.out.println("Result of delete in 'testUpdateAnnotationToObject': " + deleteAnnotationContext.getResponse().trim());
                       
			//check if any AnnotationContext still exists
			
			//delete annotation
			ClientResponse deleteAnnotation=c.sendRequest("DELETE", mainPath +"objects/" + annotationId + "", ""); 
            assertEquals(200, deleteAnnotation.getHttpCode());
            assertTrue(deleteAnnotation.getResponse().trim().contains("deleted"));
            System.out.println("Result of delete annotation in 'testUpdateAnnotationToObject': " + deleteAnnotation.getResponse().trim());
			
            //delete object
			ClientResponse deleteObjectCollection=c.sendRequest("DELETE", mainPath +"objects/" + objectId + "", ""); 
            assertEquals(200, deleteObjectCollection.getHttpCode());
            assertTrue(deleteObjectCollection.getResponse().trim().contains("deleted"));
            System.out.println("Result of delete object @ 'testUpdateAnnotationToObject': " + deleteObjectCollection.getResponse().trim());
		}
		catch(Exception e)
		{
			e.printStackTrace();
			fail ( "Exception: " + e );
		}
		
	}
	
	/**
	 * Tests the AnnotationService for updating  nodes
	 */
	@Test
	public void testUpdateNode()
	{
		//AnnotationsClass cl = new AnnotationsClass();
		MiniClient c = new MiniClient();
		c.setAddressPort(HTTP_ADDRESS, HTTP_PORT);
		JSONObject annotation;
		JSONObject object;
		try
		{
			c.setLogin(Long.toString(testAgent.getId()), testPass);	
			
			//add a new object
            ClientResponse addObjectCollection=c.sendRequest("POST", mainPath +"objects", "{\"collection\": \"" + objectCollection + "\", \"toolId\":\"TestCase\"}", "application/json", "*/*", new Pair[]{});
            assertEquals(200, addObjectCollection.getHttpCode());
            assertTrue(addObjectCollection.getResponse().trim().contains("id")); 
			System.out.println("Result of 'testUpdateNode': " + addObjectCollection.getResponse());
			try{	
				object = (JSONObject) JSONValue.parseWithException(addObjectCollection.getResponse());
			} catch (ParseException e1) {
				throw new IllegalArgumentException("Data is not valid JSON!");
			}
			String objectId = (String) object.get(new String("id"));
			
			//add a new annotation
			ClientResponse result=c.sendRequest("POST", mainPath +"annotations", "{\"collection\": \"" + annotationCollection + "\","
					+ " \"title\": \"Annotation Insert Test\" ,\"keywords\": \"test annotation\" , \"objectId\": " + "\"" + objectId + "\"" + ","
					+ "\"location\": \"Microservice Test Class\", \"toolId\":\"TestCase\"}", "application/json", "*/*", new Pair[]{});
	        assertEquals(200, result.getHttpCode());
	        assertTrue(result.getResponse().trim().contains("id")); 
			System.out.println("Result of insert 'testUpdateNode': " + result.getResponse().trim());
			try{	
				annotation = (JSONObject) JSONValue.parseWithException(result.getResponse());
			} catch (ParseException e1) {
				throw new IllegalArgumentException("Data is not valid JSON!");
			}
			String annotationId = (String) annotation.get(new String("id"));
			
			//retrieve the annotation 			
			//check if annotation exists
			ClientResponse select=c.sendRequest("GET", mainPath +"objects/" + annotationId +"?part=id,title", ""); 
            assertEquals(200, select.getHttpCode());
            assertTrue(select.getResponse().trim().contains(annotationId)); 
			System.out.println("Result of select @ 'testUpdateNode': " + select.getResponse().trim());
			
			//update annotation
			ClientResponse updateNode=c.sendRequest("PUT", mainPath +"objects/" + annotationId +"", "{ "
					+ " \"location\": \"UpdatedLocation :)\" }}", "application/json", "*/*", new Pair[]{});
	        assertEquals(200, updateNode.getHttpCode());
	        assertTrue(updateNode.getResponse().trim().contains("UpdatedLocation")); 
			System.out.println("Result of update @ 'testUpdateNode': " + updateNode.getResponse().trim());
			
			//retrieve the annotation 			
			//check if updated
			ClientResponse selectAgain=c.sendRequest("GET", mainPath +"objects/" + annotationId +"?part=id,annotationData.location", ""); 
            assertEquals(200, selectAgain.getHttpCode());
            assertTrue(selectAgain.getResponse().trim().contains("UpdatedLocation")); 
			System.out.println("Result of selectAgain @ 'testUpdateNode': " + selectAgain.getResponse().trim());
			
			//delete annotation
			ClientResponse delete=c.sendRequest("DELETE", mainPath +"objects/" + annotationId +"", ""); 
            assertEquals(200, delete.getHttpCode());
            assertTrue(delete.getResponse().trim().contains("deleted"));
            System.out.println("Result of delete in 'testUpdateNode': " + delete.getResponse().trim());
			
			//check if annotation exists
            ClientResponse selectAgainNew=c.sendRequest("GET", mainPath +"objects/" + annotationId +"?part=id,title", ""); 
            assertEquals(404, selectAgainNew.getHttpCode());
            assertTrue(selectAgainNew.getResponse().trim().contains("not")); 
			System.out.println("Result of select again in 'testUpdateNode': " + selectAgainNew.getResponse().trim());
		
			//delete object
			ClientResponse deleteObjectCollection=c.sendRequest("DELETE", mainPath +"objects/" + objectId +"", ""); 
            assertEquals(200, deleteObjectCollection.getHttpCode());
            assertTrue(deleteObjectCollection.getResponse().trim().contains("deleted"));
            System.out.println("Result of delete in 'testUpdateNode': " + deleteObjectCollection.getResponse().trim());
		}
		catch(Exception e)
		{
			e.printStackTrace();
			fail ( "Exception: " + e );
		}
		
	}
	 
	//Tests to create objects 
	/**
	 * Tests the AnnotationService for creating a lot of nodes
	 */
	@Ignore("Used only for performance tests")
	@Test
	public void testCreateNodes()
	{
		//AnnotationsClass cl = new AnnotationsClass();
		MiniClient c = new MiniClient();
		c.setAddressPort(HTTP_ADDRESS, HTTP_PORT);
		JSONObject o;
		try
		{
			c.setLogin(Long.toString(testAgent.getId()), testPass);
			for (int i = 0; i < 100; i++){
				//add a new objectCollection
	            ClientResponse result=c.sendRequest("POST", mainPath +"objects", "{\"collection\": \"" + objectCollection + "\", \"toolId\":\"TestCase\"}", "application/json", "*/*", new Pair[]{});
	            assertEquals(200, result.getHttpCode());
	            assertTrue(result.getResponse().trim().contains("id")); 
				System.out.println("Result of 'testCreateNodes': " + result.getResponse());
			}
			
			
			for (int i = 0; i < 100; i++){
				//add a new objectCollectionSecond
	            ClientResponse result=c.sendRequest("POST", mainPath +"objects", "{\"collection\": \"" + objectCollectionSecond + "\", \"toolId\":\"TestCase\"}", "application/json", "*/*", new Pair[]{});
	            assertEquals(200, result.getHttpCode());
	            assertTrue(result.getResponse().trim().contains("id")); 
				System.out.println("Result of 'testCreateNodes': " + result.getResponse());
			}
			
		}
		catch(Exception e)
		{
			e.printStackTrace();
			fail ( "Exception: " + e );
		}
		
	}
	
	//Tests to create objects 
	/**
	 * Tests the AnnotationService for creating a lot of annotations of different types
	 */
	@Ignore("Used only for performance tests")
	@Test
	public void testCreateAnnotations()
	{
		//AnnotationsClass cl = new AnnotationsClass();
		MiniClient c = new MiniClient();
		c.setAddressPort(HTTP_ADDRESS, HTTP_PORT);
		JSONObject o;
		try
		{
			c.setLogin(Long.toString(testAgent.getId()), testPass);
			
			//get objects
			ClientResponse selectObjectCollection=c.sendRequest("GET", mainPath +"objects/?part=id&collection=" + objectCollection + "", ""); 
            assertEquals(200, selectObjectCollection.getHttpCode());
            assertTrue(selectObjectCollection.getResponse().trim().contains("id")); 
            String responseObjectCollection =  selectObjectCollection.getResponse();
			System.out.println("Result of 'Select in testCreateAnnotations': " + responseObjectCollection);
			responseObjectCollection = responseObjectCollection.replaceAll("\\W", "");
			responseObjectCollection = responseObjectCollection.replaceAll("id", ",");
			responseObjectCollection = responseObjectCollection.substring(1, responseObjectCollection.length());
			String objectCollectionIdArray[] = responseObjectCollection.split(",");
			System.out.println("Result of 'Select in testCreateAnnotations': " + responseObjectCollection);
						
			for (int i = 0; i < 100; i++){
				//add a new text annotation
				Random rand = new Random();
				int objectCollectionId = rand.nextInt(objectCollectionIdArray.length);
				ClientResponse result=c.sendRequest("POST", mainPath +"annotations", "{\"collection\": \"" + annotationCollection + "\","
						+ " \"title\": \"Annotation Text Insert " + i + "\"  , \"objectId\": " + "\"" + objectCollectionIdArray[objectCollectionId] + "\"" + ","
						+ " \"keywords\": \"test annotation\", \"location\": \"Microservice Test Class\", \"toolId\":\"TestCase\"}", "application/json", "*/*", new Pair[]{});
		        assertEquals(200, result.getHttpCode());
		        assertTrue(result.getResponse().trim().contains("id")); 
				System.out.println("Result of insert 'testCreateAnnotations': " + result.getResponse().trim());
			}
			
			
			for (int i = 0; i < 100; i++){
				//add a new annotation
				Random rand = new Random();
				int objectCollectionId = rand.nextInt(objectCollectionIdArray.length);
				ClientResponse result=c.sendRequest("POST", mainPath +"annotations", "{\"collection\": \"" + annotationCollectionSecond + "\","
						+ " \"title\": \"Location Annotation Insert " + i + "\" , \"objectId\": " + "\"" + objectCollectionIdArray[objectCollectionId] + "\"" + ","
						+ " \"keywords\": \"test annotation\" , \"location\": \"Aachen\", \"toolId\":\"TestCase\"}", "application/json", "*/*", new Pair[]{});
		        assertEquals(200, result.getHttpCode());
		        assertTrue(result.getResponse().trim().contains("id")); 
				System.out.println("Result of insert 'testCreateAnnotations': " + result.getResponse().trim());
			}
			
		}
		catch(Exception e)
		{
			e.printStackTrace();
			fail ( "Exception: " + e );
		}
		
	}
	
	//Tests to create AnnotationContext
	/**
	 * Tests the AnnotationService for creating a lot of AnnotationContexts
	 */
	@Ignore("Used only for performance tests")
	@Test
	public void testCreateAnnotationContexts()
	{
		//AnnotationsClass cl = new AnnotationsClass();
		MiniClient c = new MiniClient();
		c.setAddressPort(HTTP_ADDRESS, HTTP_PORT);
		try
		{
			c.setLogin(Long.toString(testAgent.getId()), testPass);
			
			//get objects
			ClientResponse selectObjectCollection=c.sendRequest("GET", mainPath +"objects/?part=id&collection=" + objectCollection + "", ""); 
            assertEquals(200, selectObjectCollection.getHttpCode());
            assertTrue(selectObjectCollection.getResponse().trim().contains("id")); 
            String responseObjectCollection =  selectObjectCollection.getResponse();
			System.out.println("Result of 'Select in testCreateAnnotationContexts': " + responseObjectCollection);
			responseObjectCollection = responseObjectCollection.replaceAll("\\W", "");
			responseObjectCollection = responseObjectCollection.replaceAll("id", ",");
			responseObjectCollection = responseObjectCollection.substring(1, responseObjectCollection.length());
			String objectCollectionIdArray[] = responseObjectCollection.split(",");
			System.out.println("Result of 'Select in testCreateAnnotationContexts': " + responseObjectCollection);
			//get objects second type
			ClientResponse selectCollectionSecond=c.sendRequest("GET", mainPath +"objects/?part=id&collection=" + objectCollectionSecond + "", ""); 
            assertEquals(200, selectCollectionSecond.getHttpCode());
            assertTrue(selectCollectionSecond.getResponse().trim().contains("id")); 
            String responseCollectionSecond =  selectCollectionSecond.getResponse();
			System.out.println("Result of 'Select in testCreateAnnotationContexts': " + responseCollectionSecond);
			responseCollectionSecond = responseCollectionSecond.replaceAll("\\W", "");
			responseCollectionSecond = responseCollectionSecond.replaceAll("id", ",");
			responseCollectionSecond = responseCollectionSecond.substring(1, responseCollectionSecond.length());
			String objectCollectionSecondIdArray[] = responseCollectionSecond.split(",");
			System.out.println("Result of 'Select in testCreateAnnotationContexts': " + responseCollectionSecond);
			
			//get text type annotations
			ClientResponse selectAnnotationCollection=c.sendRequest("GET", mainPath +"objects/?part=id&collection=" + annotationCollection + "", ""); 
            assertEquals(200, selectAnnotationCollection.getHttpCode());
            assertTrue(selectAnnotationCollection.getResponse().trim().contains("id")); 
            String responseAnnotationCollection =  selectAnnotationCollection.getResponse();
			System.out.println("Result of 'Select in testCreateAnnotationContexts': " + responseAnnotationCollection);
			responseAnnotationCollection = responseAnnotationCollection.replaceAll("\\W", "");
			responseAnnotationCollection = responseAnnotationCollection.replaceAll("id", ",");
			responseAnnotationCollection = responseAnnotationCollection.substring(1, responseAnnotationCollection.length());
			String annotationCollectionIdArray[] = responseAnnotationCollection.split(",");
			System.out.println("Result of 'Select in testCreateAnnotationContexts': " + responseAnnotationCollection);
			
			//get first type annotations
			ClientResponse selectAnnotationCollectionSecond=c.sendRequest("GET", mainPath +"objects/?part=id&collection=" + annotationCollectionSecond + "", ""); 
            assertEquals(200, selectAnnotationCollectionSecond.getHttpCode());
            assertTrue(selectAnnotationCollectionSecond.getResponse().trim().contains("id")); 
            String responseAnnotationCollectionSecond =  selectAnnotationCollectionSecond.getResponse();
			System.out.println("Result of 'Select in testCreateAnnotationContexts': " + responseAnnotationCollectionSecond);
			responseAnnotationCollectionSecond = responseAnnotationCollectionSecond.replaceAll("\\W", "");
			responseAnnotationCollectionSecond = responseAnnotationCollectionSecond.replaceAll("id", ",");
			responseAnnotationCollectionSecond = responseAnnotationCollectionSecond.substring(1, responseAnnotationCollectionSecond.length());
			String annotationCollectionSecondIdArray[] = responseAnnotationCollectionSecond.split(",");
			System.out.println("Result of 'Select in testCreateAnnotationContexts': " + responseAnnotationCollectionSecond);
			
			//create annotation context between objects and Annotations
			for (int i = 0; i < 100; i++){
				Random rand = new Random();
				int objectCollectionId = rand.nextInt(objectCollectionIdArray.length);
				int annotationId = rand.nextInt(annotationCollectionIdArray.length);
				
				System.out.println("Result of ids: " + objectCollectionId + " " + annotationId);
				//add new AnnotationContext
				ClientResponse addSecondTypeAnnotationContext=c.sendRequest("POST", mainPath +"annotationContexts/" + objectCollectionIdArray[objectCollectionId] + "/" + annotationCollectionIdArray[annotationId] + "", "{ "
						+ "\"position\": { \"x\": \"10\", \"y\": \"10\", \"z\": \"10\"}, "
						+ "\"time\": \"1.324\", \"duration\": \"0.40\", \"toolId\":\"TestCase\"}", "application/json", "*/*", new Pair[]{}); 
		        assertEquals(200, addSecondTypeAnnotationContext.getHttpCode());
		        assertTrue(addSecondTypeAnnotationContext.getResponse().trim().contains("id")); 
				System.out.println("Result of insertAnnotationContext @ 'testCreateAnnotationContexts': " + addSecondTypeAnnotationContext.getResponse().trim());
				
				//add new AnnotationContext
				int objectCollectionId2 = rand.nextInt(objectCollectionIdArray.length);
				int annotationId2 = rand.nextInt(annotationCollectionSecondIdArray.length);
				System.out.println("Result of ids: " + objectCollectionId2 + " " + annotationId2);
				ClientResponse addFirstTypeAnnotationContext=c.sendRequest("POST", mainPath +"annotationContexts/" + objectCollectionIdArray[objectCollectionId2] + "/" + annotationCollectionSecondIdArray[annotationId2] + "", "{ "
						+ "\"position\": { \"x\": \"10\", \"y\": \"10\", \"z\": \"10\"}, "
						+ "\"time\": \"1.324\", \"duration\": \"0.40\", \"toolId\":\"TestCase\"}", "application/json", "*/*", new Pair[]{}); 
		        assertEquals(200, addSecondTypeAnnotationContext.getHttpCode());
		        assertTrue(addSecondTypeAnnotationContext.getResponse().trim().contains("id")); 
				System.out.println("Result of insertAnnotationContext @ 'testCreateAnnotationContexts': " + addSecondTypeAnnotationContext.getResponse().trim());
			}
			
			//create annotation context between objects of second type and Annotations
			for (int i = 0; i < 100; i++){
				Random rand = new Random();
				int objectCollectionSecondId = rand.nextInt(objectCollectionSecondIdArray.length);
				int annotationId = rand.nextInt(annotationCollectionIdArray.length);
				int objectCollectionSecondId2 = rand.nextInt(objectCollectionSecondIdArray.length);
				int annotationId2 = rand.nextInt(annotationCollectionSecondIdArray.length);
				
				//add new AnnotationContext
				ClientResponse addSecondTypeAnnotationContext=c.sendRequest("POST", mainPath +"annotationContexts/" + objectCollectionSecondIdArray[objectCollectionSecondId] + "/" + annotationCollectionIdArray[annotationId] + "", "{ "
						+ "\"position\": { \"x\": \"10\", \"y\": \"10\", \"z\": \"10\"}, "
						+ "\"time\": \"1.324\", \"duration\": \"0.40\", \"toolId\":\"TestCase\"}", "application/json", "*/*", new Pair[]{}); 
		        assertEquals(200, addSecondTypeAnnotationContext.getHttpCode());
		        assertTrue(addSecondTypeAnnotationContext.getResponse().trim().contains("id")); 
				System.out.println("Result of insertAnnotationContext @ 'testCreateAnnotationContexts': " + addSecondTypeAnnotationContext.getResponse().trim());
				
				//add new AnnotationContext
				ClientResponse addFirstTypeAnnotationContext=c.sendRequest("POST", mainPath +"annotationContexts/" + objectCollectionSecondIdArray[objectCollectionSecondId2] + "/" + annotationCollectionSecondIdArray[annotationId2] + "", "{ "
						+ "\"position\": { \"x\": \"10\", \"y\": \"10\", \"z\": \"10\"}, "
						+ "\"time\": \"1.324\", \"duration\": \"0.40\", \"toolId\":\"TestCase\"}", "application/json", "*/*", new Pair[]{}); 
		        assertEquals(200, addFirstTypeAnnotationContext.getHttpCode());
		        assertTrue(addFirstTypeAnnotationContext.getResponse().trim().contains("id")); 
				System.out.println("Result of insertAnnotationContext @ 'testCreateAnnotationContexts': " + addFirstTypeAnnotationContext.getResponse().trim());
			}
			
			//create annotationcontext between Annotations
			for (int i = 0; i < 100; i++){
				Random rand = new Random();
				int annotationId = rand.nextInt(annotationCollectionIdArray.length);
				int annotationId2 = rand.nextInt(annotationCollectionSecondIdArray.length);
				
				//add new AnnotationContext
				ClientResponse addSecondTypeAnnotationContext=c.sendRequest("POST", mainPath +"annotationContexts/" + annotationCollectionSecondIdArray[annotationId2] + "/" + annotationCollectionIdArray[annotationId] + "", "{ "
						+ "\"position\": { \"x\": \"10\", \"y\": \"10\", \"z\": \"10\"}, "
						+ "\"time\": \"1.324\", \"duration\": \"0.40\", \"toolId\":\"TestCase\"}", "application/json", "*/*", new Pair[]{}); 
		        assertEquals(200, addSecondTypeAnnotationContext.getHttpCode());
		        assertTrue(addSecondTypeAnnotationContext.getResponse().trim().contains("id")); 
				System.out.println("Result of insertAnnotationContext @ 'testCreateAnnotationContexts': " + addSecondTypeAnnotationContext.getResponse().trim());
			}
			
		}
		catch(Exception e)
		{
			e.printStackTrace();
			fail ( "Exception: " + e );
		}
		
	}
}


package i5.las2peer.services.annotations;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import i5.las2peer.p2p.LocalNode;
import i5.las2peer.security.ServiceAgent;
import i5.las2peer.security.UserAgent;
import i5.las2peer.services.annotations.AnnotationsClass;
import i5.las2peer.testing.MockAgentFactory;
import i5.las2peer.webConnector.WebConnector;
import i5.las2peer.webConnector.client.ClientResponse;
import i5.las2peer.webConnector.client.MiniClient;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;
import net.minidev.json.parser.ParseException;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;


/**
 * Example Test Class demonstrating a basic JUnit test structure.
 * 
 * 
 *
 */
public class ServiceTest {
	
	private static final String HTTP_ADDRESS = "http://127.0.0.1";
	private static final int HTTP_PORT = 8083;//WebConnector.DEFAULT_HTTP_PORT;
	
	private static LocalNode node;
	private static WebConnector connector;
	private static ByteArrayOutputStream logStream;
	
	private static UserAgent testAgent;
	private static final String testPass = "adamspass";
	
	private static final String testServiceClass = "i5.las2peer.services.annotations.AnnotationsClass";
	
	private static final String mainPath = "annotations/";
	
	
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
	 * 
	 * Tests the validation method.
	 * 
	 */
	@Test
	public void testValidateLogin()
	{
		MiniClient c = new MiniClient();
		c.setAddressPort(HTTP_ADDRESS, HTTP_PORT);
		
		try
		{
			c.setLogin(Long.toString(testAgent.getId()), testPass);
            ClientResponse result=c.sendRequest("GET", mainPath +"validation", "");
            assertEquals(200, result.getHttpCode());
            assertTrue(result.getResponse().trim().contains("adam")); //login name is part of response
			System.out.println("Result of 'testValidateLogin': " + result.getResponse());
		}
		catch(Exception e)
		{
			e.printStackTrace();
			fail ( "Exception: " + e );
		}
		
    }
	
	

	/**
	 * Test the ServiceClass for valid rest mapping.
	 * Important for development.
	 */
	@Test
	public void testDebugMapping()
	{
		AnnotationsClass cl = new AnnotationsClass();
		assertTrue(cl.debugMapping());
	}
	
	/**
	 * Tests the AnnotationService for adding new nodes (for Videos)
	 */
	@Test
	public void testCreateVideoNode()
	{
		//AnnotationsClass cl = new AnnotationsClass();
		MiniClient c = new MiniClient();
		c.setAddressPort(HTTP_ADDRESS, HTTP_PORT);
		JSONObject o;
		try
		{
			c.setLogin(Long.toString(testAgent.getId()), testPass);
			
			//add a new video
            ClientResponse result=c.sendRequest("POST", mainPath +"objects", "{\"collection\": \"Videos\"}"); 
            assertEquals(200, result.getHttpCode());
            assertTrue(result.getResponse().trim().contains("id")); 
			System.out.println("Result of 'testCreateVideoNode': " + result.getResponse());
			try{	
				o = (JSONObject) JSONValue.parseWithException(result.getResponse());
			} catch (ParseException e1) {
				throw new IllegalArgumentException("Data is not valid JSON!");
			}
			String id = (String) o.get(new String("id"));
			
			//check if video exists -> should pass
			//retrieve the video information
			ClientResponse select=c.sendRequest("GET", mainPath +"objects/" + id + "?part=id", ""); 
            assertEquals(200, select.getHttpCode());
            assertTrue(select.getResponse().trim().contains(id)); 
			System.out.println("Result of 'Select in testCreateVideoNode': " + select.getResponse().trim());
			
			//add same video -> should fail with corresponding message
			/*ClientResponse insertAgain=c.sendRequest("POST", mainPath +"objects", "{\"collection\": \"Videos\" }"); 
            assertEquals(409, insertAgain.getHttpCode());
            assertTrue(insertAgain.getResponse().trim().contains("already")); 
			System.out.println("Result of try insert again 'testCreateVideoNode': " + insertAgain.getResponse().trim());*/
			
			//delete video
			ClientResponse delete=c.sendRequest("DELETE", mainPath +"objects/" + id + "", ""); 
            assertEquals(200, delete.getHttpCode());
            assertTrue(delete.getResponse().trim().contains("deleted"));
            System.out.println("Result of delete in 'testCreateVideoNode': " + delete.getResponse().trim());
            
            //check if video exists -> should fail
			ClientResponse selectAfterDelete=c.sendRequest("GET", mainPath +"objects/" + id + "?part=id", ""); 
            assertEquals(404, selectAfterDelete.getHttpCode());
            assertTrue(selectAfterDelete.getResponse().trim().contains("not")); 
			System.out.println("Result of select after delete in 'testCreateVideoNode': " + selectAfterDelete.getResponse().trim());
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
		JSONObject o;
		try
		{
			c.setLogin(Long.toString(testAgent.getId()), testPass);		
			//add a new annotation
			ClientResponse result=c.sendRequest("POST", mainPath +"annotations", "{\"collection\": \"Annotations\","
					+ " \"title\": \"Annotation Insert Test\" , \"location\": \"Microservice Test Class\"}"); 
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
			/*ClientResponse insertAgain=c.sendRequest("POST", mainPath +"annotations", "{\"collection\": \"Annotations\", 
			 * \"Title\": \"Annotation Insert Test\" , \"Location\": \"Microservice Test Class\"}"); 
	        assertEquals(409, insertAgain.getHttpCode());
	        assertTrue(insertAgain.getResponse().trim().contains("already")); 
			System.out.println("Result of insert again 'testCreateAnnotationNode': " + insertAgain.getResponse().trim());*/
			
			//delete annotation
			ClientResponse delete=c.sendRequest("DELETE", mainPath +"objects/" + id +"", ""); 
            assertEquals(200, delete.getHttpCode());
            assertTrue(delete.getResponse().trim().contains("deleted"));
            System.out.println("Result of delete in 'testCreateVideoNode': " + delete.getResponse().trim());
			
			//check if annotation exists
            ClientResponse selectAgain=c.sendRequest("GET", mainPath +"objects/" + id +"?part=id,title", ""); 
            assertEquals(404, selectAgain.getHttpCode());
            assertTrue(selectAgain.getResponse().trim().contains("not")); 
			System.out.println("Result of select again in 'testCreateAnnotationNode': " + selectAgain.getResponse().trim());
		}
		catch(Exception e)
		{
			e.printStackTrace();
			fail ( "Exception: " + e );
		}
	}
	
	/**
	 *  Tests adding an annotation to a video (by creating the corresponding annotationContext)
	 */
	@Test
	public void testAddAnnotationToVideo()
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
			
			//add a new video
            ClientResponse addVideo=c.sendRequest("POST", mainPath +"objects", "{\"collection\": \"Videos\"}"); 
            assertEquals(200, addVideo.getHttpCode());
            assertTrue(addVideo.getResponse().trim().contains("id")); 
			System.out.println("Result of 'testCreateVideoNode': " + addVideo.getResponse());
			try{	
				object = (JSONObject) JSONValue.parseWithException(addVideo.getResponse());
			} catch (ParseException e1) {
				throw new IllegalArgumentException("Data is not valid JSON!");
			}
			String objectId = (String) object.get(new String("id"));
			
			//check if video exists -> should pass
			//retrieve the video information
			ClientResponse selectVideo=c.sendRequest("GET", mainPath +"objects/" + objectId + "?part=id", ""); 
            assertEquals(200, selectVideo.getHttpCode());
            assertTrue(selectVideo.getResponse().trim().contains(objectId)); 
			System.out.println("Result of get video @ 'testAddAnnotationToVideo': " + selectVideo.getResponse().trim());
			
			//create a new annotation for the video
			ClientResponse addAnnotation=c.sendRequest("POST", mainPath +"annotations", "{\"collection\": \"Annotations\","
					+ " \"title\": \"Annotation Insert Test\" , \"location\": \"Microservice Test Class\"}"); 
	        assertEquals(200, addAnnotation.getHttpCode());
	        assertTrue(addAnnotation.getResponse().trim().contains("id")); 
			System.out.println("Result of insert 'testCreateAnnotationNode': " + addAnnotation.getResponse().trim());
			try{	
				annotation = (JSONObject) JSONValue.parseWithException(addAnnotation.getResponse());
			} catch (ParseException e1) {
				throw new IllegalArgumentException("Data is not valid JSON!");
			}
			String annotationId = (String) annotation.get(new String("id"));
						
			//check if annotation exists
			ClientResponse selectAnnotation=c.sendRequest("GET", mainPath +"objects/" + annotationId + "?part=id,title", ""); 
            assertEquals(200, selectAnnotation.getHttpCode());
            assertTrue(selectAnnotation.getResponse().trim().contains(annotationId)); 
			System.out.println("Result of select in 'testCreateAnnotationNode': " + selectAnnotation.getResponse().trim());
			
			//add new annotationContext
			ClientResponse addAnnotationContext=c.sendRequest("POST", mainPath +"annotationContexts", "{ "
					+ "\"source\": \"" + objectId + "\", \"dest\": \"" + annotationId + "\", "
					+ "\"position\": { \"x\": \"10\", \"y\": \"10\", \"z\": \"10\"}, "
					+ "\"time\": \"1.324\", \"duration\": \"0.40\" }"); 
	        assertEquals(200, addAnnotationContext.getHttpCode());
	        assertTrue(addAnnotationContext.getResponse().trim().contains("id")); 
			System.out.println("Result of insertAnnotationContext @ 'testAddAnnotationToVideo': " + addAnnotationContext.getResponse().trim());
			try{	
				annotationContext = (JSONObject) JSONValue.parseWithException(addAnnotationContext.getResponse());
			} catch (ParseException e1) {
				throw new IllegalArgumentException("Data is not valid JSON!");
			}
			String annotationContextId = (String) annotationContext.get(new String("id"));
			
			//check if the annotationContext exists
			ClientResponse selectAnnotationContext=c.sendRequest("GET", mainPath +"annotationContexts/" + objectId + "/" + annotationId + "?part=id&collection=Annotated", ""); 
            assertEquals(200, selectAnnotationContext.getHttpCode());
            assertTrue(selectAnnotationContext.getResponse().trim().contains(annotationContextId)); 
			System.out.println("Result of select in 'testCreateAnnotationNode': " + selectAnnotationContext.getResponse().trim());
			
			//create a new annotationContext (with the same information)
			ClientResponse addAnnotationContextAgain=c.sendRequest("POST", mainPath +"annotationContexts", "{ "
					+ "\"source\": \"" + objectId + "\", \"dest\": \"" + annotationId + "\", "
					+ "\"position\": { \"x\": \"10\", \"y\": \"10\", \"z\": \"10\"}, "
					+ "\"time\": \"1.324\", \"duration\": \"0.40\" }"); 
	        assertEquals(200, addAnnotationContextAgain.getHttpCode());
	        assertTrue(addAnnotationContextAgain.getResponse().trim().contains("id")); 
			System.out.println("Result of insertannotationContext @ 'testAddAnnotationToVideo': " + addAnnotationContextAgain.getResponse().trim());
			try{	
				annotationContext = (JSONObject) JSONValue.parseWithException(addAnnotationContextAgain.getResponse());
			} catch (ParseException e1) {
				throw new IllegalArgumentException("Data is not valid JSON!");
			}
			String annotationContextId2 = (String) annotationContext.get(new String("id"));
					
			
			//retrieve the existing annotationContexts & data 
			ClientResponse selectAnnotationContextAgain=c.sendRequest("GET", mainPath +"annotationContexts/"
			+ objectId + "/" + annotationId + "?part=id,position&collection=Annotated", ""); 
            assertEquals(200, selectAnnotationContextAgain.getHttpCode());
            System.out.println("Result of select in 'testCreateAnnotationNode': " + selectAnnotationContextAgain.getResponse());
            assertTrue(selectAnnotationContextAgain.getResponse().trim().contains(annotationContextId));
            assertTrue(selectAnnotationContextAgain.getResponse().trim().contains(annotationContextId2));
			System.out.println("Result of select in 'testCreateAnnotationNode': " + selectAnnotationContextAgain.getResponse().trim());
			
			//delete all annotationContexts 
			ClientResponse deleteAnnotationContext=c.sendRequest("DELETE", mainPath +"annotationContexts/" + annotationContextId + "", ""); 
            assertEquals(200, deleteAnnotationContext.getHttpCode());
            assertTrue(deleteAnnotationContext.getResponse().trim().contains("deleted"));
            System.out.println("Result of delete in 'testCreateVideoNode': " + deleteAnnotationContext.getResponse().trim());
            
            ClientResponse deleteAnnotationContext2=c.sendRequest("DELETE", mainPath +"annotationContexts/" + annotationContextId2 + "", ""); 
            assertEquals(200, deleteAnnotationContext2.getHttpCode());
            assertTrue(deleteAnnotationContext2.getResponse().trim().contains("deleted"));
            System.out.println("Result of delete in 'testCreateVideoNode': " + deleteAnnotationContext2.getResponse().trim());
			//check if any annotationContext still exists
			
			//delete annotation
			ClientResponse deleteAnnotation=c.sendRequest("DELETE", mainPath +"objects/" + annotationId + "", ""); 
            assertEquals(200, deleteAnnotation.getHttpCode());
            assertTrue(deleteAnnotation.getResponse().trim().contains("deleted"));
            System.out.println("Result of delete annotation in 'testCreateVideoNode': " + deleteAnnotation.getResponse().trim());
			
            //delete video
			ClientResponse deleteVideo=c.sendRequest("DELETE", mainPath +"objects/" + objectId + "", ""); 
            assertEquals(200, deleteVideo.getHttpCode());
            assertTrue(deleteVideo.getResponse().trim().contains("deleted"));
            System.out.println("Result of delete video @ 'testAddAnnotationToVideo': " + deleteVideo.getResponse().trim());
		}
		catch(Exception e)
		{
			e.printStackTrace();
			fail ( "Exception: " + e );
		}
		
	}
	
	/**
	 *  Tests updating an annotation to a video (by creating the corresponding annotationContext)
	 */
	@Test
	public void testUpdateAnnotationToVideo()
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
			
			//add a new video
            ClientResponse addVideo=c.sendRequest("POST", mainPath +"objects", "{\"collection\": \"Videos\" }"); 
            assertEquals(200, addVideo.getHttpCode());
            assertTrue(addVideo.getResponse().trim().contains("id")); 
			System.out.println("Result of 'testCreateVideoNode': " + addVideo.getResponse());
			try{	
				object = (JSONObject) JSONValue.parseWithException(addVideo.getResponse());
			} catch (ParseException e1) {
				throw new IllegalArgumentException("Data is not valid JSON!");
			}
			String objectId = (String) object.get(new String("id"));
			
			//check if video exists -> should pass
			//retrieve the video information
			ClientResponse selectVideo=c.sendRequest("GET", mainPath +"objects/" + objectId + "?part=id", ""); 
            assertEquals(200, selectVideo.getHttpCode());
            assertTrue(selectVideo.getResponse().trim().contains(objectId)); 
			System.out.println("Result of get video @ 'testUpdateAnnotationToVideo': " + selectVideo.getResponse().trim());
			
			//create a new annotation for the video
			ClientResponse addAnnotation=c.sendRequest("POST", mainPath + "annotations", "{\"collection\": \"Annotations\", \"title\": \"Annotation Insert Test\" , \"location\": \"Microservice Test Class\"}"); 
	        assertEquals(200, addAnnotation.getHttpCode());
	        assertTrue(addAnnotation.getResponse().trim().contains("id")); 
			System.out.println("Result of insert  'testCreateAnnotationNode': " + addAnnotation.getResponse().trim());
			try{	
				annotation = (JSONObject) JSONValue.parseWithException(addAnnotation.getResponse());
			} catch (ParseException e1) {
				throw new IllegalArgumentException("Data is not valid JSON!");
			}
			String annotationId = (String) annotation.get(new String("id"));
						
			//check if annotation exists
			ClientResponse selectAnnotation=c.sendRequest("GET", mainPath +"objects/" + annotationId +"?part=id,title", ""); 
            assertEquals(200, selectAnnotation.getHttpCode());
            assertTrue(selectAnnotation.getResponse().trim().contains(annotationId)); 
			System.out.println("Result of select in 'testUpdateAnnotationToVideo': " + selectAnnotation.getResponse().trim());
			
			//add new AnnotationContext
			ClientResponse addAnnotationContext=c.sendRequest("POST", mainPath +"annotationContexts", "{ "
					+ "\"source\": \"" + objectId + "\", \"dest\": \"" + annotationId + "\", "
					+ "\"position\": { \"x\": \"10\", \"y\": \"10\", \"z\": \"10\"}, "
					+ "\"time\": \"1.324\", \"duration\": \"0.40\" }"); 
	        assertEquals(200, addAnnotationContext.getHttpCode());
	        assertTrue(addAnnotationContext.getResponse().trim().contains("id")); 
			System.out.println("Result of insertAnnotationContext @ 'testAddAnnotationToVideo': " + addAnnotationContext.getResponse().trim());
			try{	
				annotationContext = (JSONObject) JSONValue.parseWithException(addAnnotationContext.getResponse());
			} catch (ParseException e1) {
				throw new IllegalArgumentException("Data is not valid JSON!");
			}
			String annotationContextId = (String) annotationContext.get(new String("id"));
			
			//check if the AnnotationContext exists
			ClientResponse selectAnnotationContext=c.sendRequest("GET", mainPath +"annotationContexts/" + objectId + "/" + annotationId + "?part=id&collection=Annotated", ""); 
            assertEquals(200, selectAnnotationContext.getHttpCode());
            assertTrue(selectAnnotationContext.getResponse().trim().contains(annotationContextId)); 
			System.out.println("Result of select in 'testCreateAnnotationNode': " + selectAnnotationContext.getResponse().trim());
			
			//update AnnotationContext
			ClientResponse updateAnnotationContext=c.sendRequest("PUT", mainPath +"annotationContexts/" + annotationContextId + "", "{ "
					+ " \"time\": \"2.167\" }"); 
	        assertEquals(200, updateAnnotationContext.getHttpCode());
	        assertTrue(updateAnnotationContext.getResponse().trim().contains("2.167")); 
			System.out.println("Result of updateAnnotationContext @ 'testUpdateAnnotationToVideo': " + updateAnnotationContext.getResponse().trim());
			
			//check if the AnnotationContext updated
			ClientResponse selectUpdatedAnnotationContext=c.sendRequest("GET", mainPath +"annotationContexts/" + objectId + "/" + annotationId + "?part=id,time&collection=Annotated", ""); 
            assertEquals(200, selectUpdatedAnnotationContext.getHttpCode());
            assertTrue(selectUpdatedAnnotationContext.getResponse().trim().contains("2.167")); 
			System.out.println("Result of select in 'testUpdateAnnotationToVideo': " + selectUpdatedAnnotationContext.getResponse().trim());
			
			//delete AnnotationContext 
			ClientResponse deleteAnnotationContext=c.sendRequest("DELETE", mainPath +"annotationContexts/" + annotationContextId + "", ""); 
            assertEquals(200, deleteAnnotationContext.getHttpCode());
            assertTrue(deleteAnnotationContext.getResponse().trim().contains("deleted"));
            System.out.println("Result of delete in 'testUpdateAnnotationToVideo': " + deleteAnnotationContext.getResponse().trim());
                       
			//check if any AnnotationContext still exists
			
			//delete annotation
			ClientResponse deleteAnnotation=c.sendRequest("DELETE", mainPath +"objects/" + annotationId + "", ""); 
            assertEquals(200, deleteAnnotation.getHttpCode());
            assertTrue(deleteAnnotation.getResponse().trim().contains("deleted"));
            System.out.println("Result of delete annotation in 'testUpdateAnnotationToVideo': " + deleteAnnotation.getResponse().trim());
			
            //delete video
			ClientResponse deleteVideo=c.sendRequest("DELETE", mainPath +"objects/" + objectId + "", ""); 
            assertEquals(200, deleteVideo.getHttpCode());
            assertTrue(deleteVideo.getResponse().trim().contains("deleted"));
            System.out.println("Result of delete video @ 'testUpdateAnnotationToVideo': " + deleteVideo.getResponse().trim());
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
		try
		{
			c.setLogin(Long.toString(testAgent.getId()), testPass);		
			//add a new annotation
			ClientResponse result=c.sendRequest("POST", mainPath +"annotations", "{\"collection\": \"Annotations\", \"title\": \"Annotation Insert Test\" , \"location\": \"Microservice Test Class\"}"); 
	        assertEquals(200, result.getHttpCode());
	        assertTrue(result.getResponse().trim().contains("id")); 
			System.out.println("Result of insert 'testCreateAnnotationNode': " + result.getResponse().trim());
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
					+ " \"location\": \"UpdatedLocation :)\" }}"); 
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
		}
		catch(Exception e)
		{
			e.printStackTrace();
			fail ( "Exception: " + e );
		}
		
	}
	 
	
}


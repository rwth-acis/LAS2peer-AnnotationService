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
	private static final int HTTP_PORT = WebConnector.DEFAULT_HTTP_PORT;
	
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
		
		try
		{
			c.setLogin(Long.toString(testAgent.getId()), testPass);
			
			//add a new video
            ClientResponse result=c.sendRequest("PUT", mainPath +"vertex", "{\"graphName\": \"Video\", \"collection\": \"Videos\", \"id\": \"idTestingInsert\"}"); 
            assertEquals(200, result.getHttpCode());
            assertTrue(result.getResponse().trim().contains("Succesfully")); 
			System.out.println("Result of 'testCreateVideoNode': " + result.getResponse().trim());
			
			//check if video exists -> should pass
			//retrieve the video information
			ClientResponse select=c.sendRequest("GET", mainPath +"vertices/idTestingInsert?part=id&collection=Videos", ""); 
            assertEquals(200, select.getHttpCode());
            assertTrue(select.getResponse().trim().contains("idTestingInsert")); 
			System.out.println("Result of 'Select in testCreateVideoNode': " + select.getResponse().trim());
			
			//add same video -> should fail with corresponding message
			ClientResponse insertAgain=c.sendRequest("PUT", mainPath +"vertex", "{\"graphName\": \"Video\", \"collection\": \"Videos\", \"id\": \"idTestingInsert\"}"); 
            assertEquals(409, insertAgain.getHttpCode());
            assertTrue(insertAgain.getResponse().trim().contains("already")); 
			System.out.println("Result of try insert again 'testCreateVideoNode': " + insertAgain.getResponse().trim());
			
			//delete video
			ClientResponse delete=c.sendRequest("DELETE", mainPath +"vertex/idTestingInsert?name=Video&collection=Videos", ""); 
            assertEquals(200, delete.getHttpCode());
            assertTrue(delete.getResponse().trim().contains("deleted"));
            System.out.println("Result of delete in 'testCreateVideoNode': " + delete.getResponse().trim());
            
            //check if video exists -> should fail
			ClientResponse selectAfterDelete=c.sendRequest("GET", mainPath +"vertices/idTestingInsert?part=id&collection=Videos", ""); 
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
		try
		{
			c.setLogin(Long.toString(testAgent.getId()), testPass);		
			//add a new annotation
			ClientResponse result=c.sendRequest("PUT", mainPath +"annotation", "{\"graphName\": \"Video\", \"collection\": \"Annotations\", \"id\": \"idTestingAnnotationInsert\", \"Title\": \"Annotation Insert Test\" , \"Location\": \"Microservice Test Class\"}"); 
	        assertEquals(200, result.getHttpCode());
	        assertTrue(result.getResponse().trim().contains("Succesfully")); 
			System.out.println("Result of insert 'testCreateAnnotationNode': " + result.getResponse().trim());
			
			//retrieve the annotation 			
			//check if annotation exists
			ClientResponse select=c.sendRequest("GET", mainPath +"vertices/idTestingAnnotationInsert?part=id,annotationData.Title&collection=Annotations", ""); 
            assertEquals(200, select.getHttpCode());
            assertTrue(select.getResponse().trim().contains("idTestingAnnotationInsert")); 
			System.out.println("Result of select in 'testCreateAnnotationNode': " + select.getResponse().trim());
			
			//add same annotation
			ClientResponse insertAgain=c.sendRequest("PUT", mainPath +"annotation", "{\"graphName\": \"Video\", \"collection\": \"Annotations\", \"id\": \"idTestingAnnotationInsert\", \"Title\": \"Annotation Insert Test\" , \"Location\": \"Microservice Test Class\"}"); 
	        assertEquals(409, insertAgain.getHttpCode());
	        assertTrue(insertAgain.getResponse().trim().contains("already")); 
			System.out.println("Result of insert again 'testCreateAnnotationNode': " + insertAgain.getResponse().trim());
			
			//delete annotation
			ClientResponse delete=c.sendRequest("DELETE", mainPath +"vertex/idTestingAnnotationInsert?name=Video&collection=Annotations", ""); 
            assertEquals(200, delete.getHttpCode());
            assertTrue(delete.getResponse().trim().contains("deleted"));
            System.out.println("Result of delete in 'testCreateVideoNode': " + delete.getResponse().trim());
			
			//check if annotation exists
            ClientResponse selectAgain=c.sendRequest("GET", mainPath +"vertices/idTestingAnnotationInsert?part=id,annotationData.Title&collection=Annotations", ""); 
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
	 *  Tests adding an annotation to a video (by creating the corresponding edge)
	 */
	@Test
	public void testAddAnnotationToVideo()
	{
		//AnnotationsClass cl = new AnnotationsClass();
		MiniClient c = new MiniClient();
		c.setAddressPort(HTTP_ADDRESS, HTTP_PORT);
		
		try
		{
			c.setLogin(Long.toString(testAgent.getId()), testPass);	
			
			//add a new video
            ClientResponse addVideo=c.sendRequest("PUT", mainPath +"vertex", "{\"graphName\": \"Video\", \"collection\": \"Videos\", \"id\": \"idTestingInsert\"}"); 
            assertEquals(200, addVideo.getHttpCode());
            assertTrue(addVideo.getResponse().trim().contains("Succesfully")); 
			System.out.println("Result of insertVideo @ 'testAddAnnotationToVideo': " + addVideo.getResponse().trim());
			
			//check if video exists -> should pass
			//retrieve the video information
			ClientResponse selectVideo=c.sendRequest("GET", mainPath +"vertices/idTestingInsert?part=id&collection=Videos", ""); 
            assertEquals(200, selectVideo.getHttpCode());
            assertTrue(selectVideo.getResponse().trim().contains("idTestingInsert")); 
			System.out.println("Result of get video @ 'testAddAnnotationToVideo': " + selectVideo.getResponse().trim());
			
			//create a new annotation for the video
			ClientResponse addAnnotation=c.sendRequest("PUT", mainPath +"annotation", "{\"graphName\": \"Video\", \"collection\": \"Annotations\", \"id\": \"idTestingAnnotationInsert\", \"Title\": \"Annotation Insert Test\" , \"Location\": \"Microservice Test Class\"}"); 
	        assertEquals(200, addAnnotation.getHttpCode());
	        assertTrue(addAnnotation.getResponse().trim().contains("Succesfully")); 
			System.out.println("Result of insert 'testCreateAnnotationNode': " + addAnnotation.getResponse().trim());
						
			//check if annotation exists
			ClientResponse selectAnnotation=c.sendRequest("GET", mainPath +"vertices/idTestingAnnotationInsert?part=id,annotationData.Title&collection=Annotations", ""); 
            assertEquals(200, selectAnnotation.getHttpCode());
            assertTrue(selectAnnotation.getResponse().trim().contains("idTestingAnnotationInsert")); 
			System.out.println("Result of select in 'testCreateAnnotationNode': " + selectAnnotation.getResponse().trim());
			
			//add new edge
			ClientResponse addEdge=c.sendRequest("PUT", mainPath +"edge", "{\"graphName\": \"Video\","
					+ " \"collection\": \"newAnnotated\", \"id\": \"idTestingEdgeInsert\", \"source\": \"idTestingInsert\", "
					+ "\"dest\": \"idTestingAnnotationInsert\", \"destCollection\": \"Annotations\", "
					+ "\"pos\": { \"x\": \"10\", \"y\": \"10\", \"z\": \"10\"}, "
					+ "\"startTime\": \"1.324\", \"duration\": \"0.40\" }"); 
	        assertEquals(200, addEdge.getHttpCode());
	        assertTrue(addEdge.getResponse().trim().contains("Succesfully")); 
			System.out.println("Result of insertEdge @ 'testAddAnnotationToVideo': " + addEdge.getResponse().trim());
			
			//check if the edge exists
			ClientResponse selectEdge=c.sendRequest("GET", mainPath +"edges/idTestingInsert/idTestingAnnotationInsert?part=id&collection=newAnnotated", ""); 
            assertEquals(200, selectEdge.getHttpCode());
            assertTrue(selectEdge.getResponse().trim().contains("idTestingEdgeInsert")); 
			System.out.println("Result of select in 'testCreateAnnotationNode': " + selectEdge.getResponse().trim());
			
			//create a new edge (with the same information)
			ClientResponse addEdgeAgain=c.sendRequest("PUT", mainPath +"edge", "{\"graphName\": \"Video\","
					+ " \"collection\": \"newAnnotated\", \"id\": \"idTestingEdgeInsert2\", \"source\": \"idTestingInsert\", "
					+ "\"dest\": \"idTestingAnnotationInsert\", \"destCollection\": \"Annotations\", "
					+ "\"pos\": { \"x\": \"10\", \"y\": \"10\", \"z\": \"10\"}, "
					+ "\"startTime\": \"1.324\", \"duration\": \"0.40\" }"); 
	        assertEquals(200, addEdgeAgain.getHttpCode());
	        assertTrue(addEdgeAgain.getResponse().trim().contains("Succesfully")); 
			System.out.println("Result of insertEdge @ 'testAddAnnotationToVideo': " + addEdgeAgain.getResponse().trim());
			
			//retrieve the existing edges & data 
			ClientResponse selectEdgeAgain=c.sendRequest("GET", mainPath +"edges/idTestingInsert/idTestingAnnotationInsert?part=id,pos&collection=newAnnotated", ""); 
            assertEquals(200, selectEdgeAgain.getHttpCode());
            System.out.println("Result of select in 'testCreateAnnotationNode': " + selectEdgeAgain.getResponse());
            assertTrue(selectEdgeAgain.getResponse().trim().contains("idTestingEdgeInsert"));
            assertTrue(selectEdgeAgain.getResponse().trim().contains("idTestingEdgeInsert2"));
			System.out.println("Result of select in 'testCreateAnnotationNode': " + selectEdgeAgain.getResponse().trim());
			
			//delete all edges 
			ClientResponse deleteEdge=c.sendRequest("DELETE", mainPath +"edge/idTestingEdgeInsert?name=Video&collection=newAnnotated", ""); 
            assertEquals(200, deleteEdge.getHttpCode());
            assertTrue(deleteEdge.getResponse().trim().contains("deleted"));
            System.out.println("Result of delete in 'testCreateVideoNode': " + deleteEdge.getResponse().trim());
            
            ClientResponse deleteEdge2=c.sendRequest("DELETE", mainPath +"edge/idTestingEdgeInsert2?name=Video&collection=newAnnotated", ""); 
            assertEquals(200, deleteEdge2.getHttpCode());
            assertTrue(deleteEdge2.getResponse().trim().contains("deleted"));
            System.out.println("Result of delete in 'testCreateVideoNode': " + deleteEdge2.getResponse().trim());
			//check if any edge still exists
			
			//delete annotation
			ClientResponse deleteAnnotation=c.sendRequest("DELETE", mainPath +"vertex/idTestingAnnotationInsert?name=Video&collection=Annotations", ""); 
            assertEquals(200, deleteAnnotation.getHttpCode());
            assertTrue(deleteAnnotation.getResponse().trim().contains("deleted"));
            System.out.println("Result of delete in 'testCreateVideoNode': " + deleteAnnotation.getResponse().trim());
			
            //delete video
			ClientResponse deleteVideo=c.sendRequest("DELETE", mainPath +"vertex/idTestingInsert?name=Video&collection=Videos", ""); 
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
	
	
	
}

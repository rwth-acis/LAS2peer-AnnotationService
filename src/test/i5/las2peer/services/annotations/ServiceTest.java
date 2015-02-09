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
		AnnotationsClass cl = new AnnotationsClass();
		//create db connection
		
		//add a new video
		
		//retrieve the video information
		
		//check if video exists -> should pass
		
		//add same video -> should fail with corresponding message
		
		//delete video
		
		//check if video exists -> should fail
		
		//close db connection
	}
	
	/**
	 * Tests the AnnotationService for adding new nodes (as Annotation)
	 */
	@Test
	public void testCreateAnnotationNode()
	{
		AnnotationsClass cl = new AnnotationsClass();
		
		//create db connection
		
		//add a new annotation --> what happens if the annotation is not linked to any video?
		
		//retrieve the annotation 
		
		//check if annotation exists -> should pass
		
		//add same annotation -> should fail with corresponding message
		
		//delete annotation
		
		//check if annotation exists -> should fail
		
		//close db connection
	}
	
	
	
	/**
	 *  Tests adding an annotation to a video (by creating the corresponding edge)
	 */
	@Test
	public void testAddAnnotationToVideo()
	{
		AnnotationsClass cl = new AnnotationsClass();
		// create db connection
		
		// add a new video
		
		//check if video exists
		
		//create a new annotation for the video
		
		//check if annotation exists
		
		//check if the edge exists
		
		//retrieve edge data
		
		//create a new edge (with the same information)
		
		//retrieve the existing edges & data 
		
		//delete all edges 
		
		//check if any edge still exists
		
		//delete annotation and video
		
		//close db connection
		
	}
	
}

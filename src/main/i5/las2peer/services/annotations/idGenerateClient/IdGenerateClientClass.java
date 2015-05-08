package i5.las2peer.services.annotations.idGenerateClient;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Scanner;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

public class IdGenerateClientClass {

	private final String USER_AGENT = "Mozilla/5.0";
	
	private URI url;
	
	public IdGenerateClientClass(String url){
			try {
				this.url = new URI(url);
			} catch (URISyntaxException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		
	}
	/**
	 * Send request to the IdGenertating service. The request contains information about the 
	 * service that called the generation, the method and the OIDC user.
	 * @param callerService service that called the generation
	 * @return id the generated id
	 */
	public String sendRequest(String callerService){
		
		String output = "";
		Scanner in = null;
		CloseableHttpResponse response = null;
				
		try {
			
			CloseableHttpClient httpClient = HttpClients.createDefault();
	        HttpPost post = new HttpPost(url);
	        
	        post.setHeader("User-Agent", USER_AGENT);
	        
	        String str = "{\"service\": \"" + callerService + "\"}";
	        
	        StringEntity input = new StringEntity(str);
	        input.setContentType("application/json");
	        post.setEntity(input);	        
	        
	        response = httpClient.execute(post);
	        
	        HttpEntity entity = response.getEntity();
	        	        
	        in = new Scanner(entity.getContent());
            if (in.hasNext()) {
            	
            	output=in.next();
            }
            EntityUtils.consume(entity);
            
            in.close();
            response.close();			
		
		} catch (IOException e) {
			e.printStackTrace();
		}
		finally
        {
            in.close();
            //response.close();
        }
		return output;
		
	}
	
}

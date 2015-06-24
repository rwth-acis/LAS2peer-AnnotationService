package i5.las2peer.services.annotations.annotationTypes;

import com.google.gson.Gson;

import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;

public class Annotation{
	private String id;
	private JSONObject annotationData;
	private JSONObject author;
	private String timeStamp;
	private String lastUpdate;
	private String toolId;
	//private JSONObject annotation;
	//private JSONObject position;
	private String title;
	private String text;
	private String keywords;
	//private String time;
	
	private final static Object POSITION = new String("position");
	private final static Object TITLE = new String("title");
	private final static Object TIME = new String("time");
	private final static Object TEXT = new String("text");
	private final static Object KEYWORDS = new String("keywords");
	private final static Object TIMESTAMP = new String("timeStamp");
	private final static Object LASTUPDATE = new String("lastUpdate");

	/*public Annotation(String id, JSONObject annotationData, JSONObject author){
			//, JSONObject position, String time) {
		this.id = id;
		this.annotationData = annotationData;
		this.author = author;
		//this.position = position;
		//this.time = time;
	}*/
	
	public Annotation(String id, JSONObject unstructuredAnnotationData, JSONObject author, String timeStamp, String toolId) {
		this.id = id;
		this.author = author;
		this.timeStamp = timeStamp;
		this.lastUpdate = timeStamp;
		this.toolId = toolId;
		annotationData = new JSONObject();
		text = "";
		title = "";
		keywords = "";
		structureAnnotations(unstructuredAnnotationData);
		
	}
	
	public JSONObject toJSON(){
		JSONObject annotation = new JSONObject();
		annotation.put("id", id);
		annotation.put("author", author);
		annotation.put(TIMESTAMP.toString(), timeStamp);
		annotation.put(LASTUPDATE.toString(), lastUpdate);
		//annotation.put(POSITION.toString(), position);
		annotation.put(TEXT.toString(), text);
		annotation.put(TITLE.toString(), title);
		annotation.put(KEYWORDS.toString(), keywords);
		annotation.put("toolId", toolId);
		//annotation.put("time", time);
		annotation.put("annotationData", annotationData);
		return annotation;
	}
	/**
	 * Structure the JSON input
	 * @param unstructuredAnnotationData
	 */
	private void structureAnnotations(JSONObject unstructuredAnnotationData){
		/*if (unstructuredAnnotationData.containsKey(POSITION)){
			
			position = structurePosition(unstructuredAnnotationData);
		}*/
		if (unstructuredAnnotationData.containsKey(TEXT)){
			this.text = structureText(unstructuredAnnotationData) ;
			//text = structureText(unstructuredAnnotationData);
		}
		/*if (unstructuredAnnotationData.containsKey(TIME)){
			
			time = structureTime(unstructuredAnnotationData);
		}*/
		if (unstructuredAnnotationData.containsKey(TITLE)){
			this.title = structureTitle(unstructuredAnnotationData) ;
			//title = structureTitle(unstructuredAnnotationData);
		}
		if (unstructuredAnnotationData.containsKey(KEYWORDS)){
			this.keywords = structureKeywords(unstructuredAnnotationData) ;
			//title = structureTitle(unstructuredAnnotationData);
		}
		annotationData = unstructuredAnnotationData;
	}
	
	/**
	 * Method to extract position information from the unstructured input
	 * @param unstructuredAnnotationData unstructured input
	 * @return position information in a JSONObject
	 */
	private JSONObject structurePosition(JSONObject unstructuredAnnotationData){
		Object objectJson =  unstructuredAnnotationData.get(POSITION);
		Gson g = new Gson();				
		String object = g.toJson(objectJson);
		unstructuredAnnotationData.remove(POSITION);
		
		return (JSONObject) JSONValue.parse(object);
	}
	
	/**
	 * Method to extract text information from the unstructured input
	 * @param unstructuredAnnotationData
	 * @return text as a String
	 */
	private String structureText(JSONObject unstructuredAnnotationData){
		String objectJson =  (String) unstructuredAnnotationData.get(TEXT);

		
		unstructuredAnnotationData.remove(TEXT);		
		return  objectJson;
	}
	
	/**
	 * Method to extract keywords information from the unstructured input
	 * @param unstructuredAnnotationData
	 * @return keywords as a String
	 */
	private String structureKeywords(JSONObject unstructuredAnnotationData){
		String objectJson =  (String) unstructuredAnnotationData.get(KEYWORDS);

		
		unstructuredAnnotationData.remove(KEYWORDS);		
		return  objectJson;
	}
	/**
	 * Method to extract time information from the unstructured input
	 * @param unstructuredAnnotationData
	 * @return time as a String
	 */
	private String structureTime(JSONObject unstructuredAnnotationData){
		String objectJson =  (String) unstructuredAnnotationData.get(TIME);
		unstructuredAnnotationData.remove(TIME);
		
		return objectJson;
	}
	
	/**
	 * Method to extract title information from the unstructured input
	 * @param unstructuredAnnotationData
	 * @return title as a String
	 */
	private String structureTitle(JSONObject unstructuredAnnotationData){
		String objectJson =  (String) unstructuredAnnotationData.get(TITLE);
		unstructuredAnnotationData.remove(TITLE);
		return objectJson;
	}
	
	/**
	 * Get Id of the annotation
	 * @return id String id
	 */
	public String getId() {
		return id;
	}

}

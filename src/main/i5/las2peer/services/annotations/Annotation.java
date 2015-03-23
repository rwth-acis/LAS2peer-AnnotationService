package i5.las2peer.services.annotations;

import com.google.gson.Gson;

import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;

public class Annotation{
	private String id;
	private JSONObject annotationData;
	private JSONObject author;
	//private JSONObject position;
	private String title;
	private String text;
	//private String time;
	
	private final static Object POSITION = new String("position");
	private final static Object TITLE = new String("title");
	private final static Object TIME = new String("time");
	private final static Object TEXT = new String("text");

	/*public Annotation(String id, JSONObject annotationData, JSONObject author){
			//, JSONObject position, String time) {
		this.id = id;
		this.annotationData = annotationData;
		this.author = author;
		//this.position = position;
		//this.time = time;
	}*/
	
	public Annotation(String id, JSONObject unstructuredAnnotationData, JSONObject author) {
		this.id = id;
		this.author = author;
		structureAnnotations(unstructuredAnnotationData);
		
	}
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}
	
	public JSONObject getAnnotationData() {
		return annotationData;
	}

	public void setAnnotationData(JSONObject annotationData) {
		this.annotationData = annotationData;
	}
	
	public JSONObject toJSON(){
		JSONObject annotation = new JSONObject();
		annotation.put("id", id);
		annotation.put("author", author);
		//annotation.put("position", position);
		annotation.put(TEXT.toString(), text);
		annotation.put(TITLE.toString(), title);
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
			
			text = structureText(unstructuredAnnotationData);
		}
		/*if (unstructuredAnnotationData.containsKey(TIME)){
			
			time = structureTime(unstructuredAnnotationData);
		}*/
		if (unstructuredAnnotationData.containsKey(TITLE)){
			
			title = structureTitle(unstructuredAnnotationData);
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
		Object objectJson =  unstructuredAnnotationData.get(TEXT);
		Gson g = new Gson();				
		String object = g.toJson(objectJson);
		unstructuredAnnotationData.remove(TEXT);
		
		return object;
	}
	
	/**
	 * Method to extract time information from the unstructured input
	 * @param unstructuredAnnotationData
	 * @return time as a String
	 */
	private String structureTime(JSONObject unstructuredAnnotationData){
		Object objectJson =  unstructuredAnnotationData.get(TIME);
		Gson g = new Gson();				
		String object = g.toJson(objectJson);
		unstructuredAnnotationData.remove(TIME);
		
		return object;
	}
	
	/**
	 * Method to extract title information from the unstructured input
	 * @param unstructuredAnnotationData
	 * @return title as a String
	 */
	private String structureTitle(JSONObject unstructuredAnnotationData){
		Object objectJson =  unstructuredAnnotationData.get(TITLE);
		Gson g = new Gson();				
		String object = g.toJson(objectJson);
		unstructuredAnnotationData.remove(TITLE);
		
		return object;
	}

}

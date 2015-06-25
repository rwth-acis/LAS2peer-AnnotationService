package i5.las2peer.services.annotations.annotationTypes;

import net.minidev.json.JSONObject;

public class TimeTypeAnnotation extends Annotation{
	
	private JSONObject time;
	
	public TimeTypeAnnotation(String id,
			JSONObject unstructuredAnnotationData, JSONObject author,
			String timeStamp, String toolId) {
		super(id, unstructuredAnnotationData, author, timeStamp, toolId);
		// TODO Auto-generated constructor stub
	}
	
	public TimeTypeAnnotation(String id,
			JSONObject unstructuredAnnotationData, JSONObject author,
			String timeStamp, String toolId, JSONObject time) {
		super(id, unstructuredAnnotationData, author, timeStamp, toolId);
		this.time = time;
	}
	public JSONObject toJSON(){
		JSONObject annotation = new JSONObject();
		annotation = super.toJSON();
		annotation.put("time", time);
		return annotation;
	}
	
}

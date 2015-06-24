package i5.las2peer.services.annotations.annotationTypes;

import net.minidev.json.JSONObject;

public class PlaceTypeAnnotation extends Annotation{
	
	private JSONObject geographicPosition;
	
	public PlaceTypeAnnotation(String id,
			JSONObject unstructuredAnnotationData, JSONObject author,
			String timeStamp, String toolId) {
		super(id, unstructuredAnnotationData, author, timeStamp, toolId);
		// TODO Auto-generated constructor stub
	}
	
	public PlaceTypeAnnotation(String id,
			JSONObject unstructuredAnnotationData, JSONObject author,
			String timeStamp, String toolId, JSONObject position) {
		super(id, unstructuredAnnotationData, author, timeStamp, toolId);
		geographicPosition = position;
	}
	public JSONObject toJSON(){
		JSONObject annotation = new JSONObject();
		annotation = super.toJSON();
		annotation.put("geographicPosition", geographicPosition);
		return annotation;
	}
	
}

package i5.las2peer.services.annotations;

import net.minidev.json.JSONObject;

public class Annotation{
	String id;
	JSONObject annotationData;
	String title;

	public Annotation(String id, JSONObject annotationData) {
		this.id = id;
		this.annotationData = annotationData;
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

}

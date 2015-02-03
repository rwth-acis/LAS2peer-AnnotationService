package i5.las2peer.services.annotations;

import com.arangodb.entity.EdgeEntity;
import com.arangodb.entity.PlainEdgeEntity;


public class VideoEdge extends PlainEdgeEntity {
	double duration;
	double startTime;
	
	public VideoEdge(double duration, double startTime) {
		super();
		this.duration = duration;
		this.startTime = startTime;
	}
	
	public double getDuration() {
		return duration;
	}
	public void setDuration(double duration) {
		this.duration = duration;
	}
	public double getStartTime() {
		return startTime;
	}
	public void setStartTime(double startTime) {
		this.startTime = startTime;
	}
	
	

}

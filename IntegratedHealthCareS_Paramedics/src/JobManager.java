
 public class JobManager {
	public static JobManager getNodeFactory() { 
		return new JobManager(); 
	}
	
	private int nodeId;
	private JobManager() {
		nodeId = 1;
	}
	

	public Jobs getNode(int startTime, int endTime, int reqQualification, int reqTime) {
		return new Jobs(nodeId++, startTime, endTime, reqQualification, reqTime);
	}
	
	public Jobs getCustomNode(int id,int startTime, int endTime, int reqQualification, int reqTime) {
		return new Jobs(id, startTime, endTime, reqQualification, reqTime);
	}
}

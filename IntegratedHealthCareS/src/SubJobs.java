import java.util.ArrayList;

/*
 * This class separate the jobs into pick up and delivery tasks. Save the list of jobs per pairs
*/
public class SubJobs extends Jobs {
	private ArrayList<Jobs> jobList= new ArrayList<Jobs>();
	
	
	public SubJobs(Jobs present,Jobs future, SubRoute wr) {
		super(present.getId(), present.getStartTime(), present.getEndTime(),present.getReqQualification(),present.getReqTime()); 
		present.setPair(future);
		for(Jobs j:wr.getJobSequence()) {
			jobList.add(j);
		}	
	}

	public SubJobs(Jobs present,Jobs future) {
		super(present.getId(), present.getStartTime(), present.getEndTime(),present.getReqQualification(),present.getReqTime()); 
	}

	// Getters
	public ArrayList<Jobs> getJobList() {
		return jobList;
	}

	// Setters
	public void setJobList(ArrayList<Jobs> jobList) {
		this.jobList = jobList;
	}
	
	

}

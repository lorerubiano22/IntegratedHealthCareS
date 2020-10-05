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

	public SubJobs(Jobs j) {
		super(j.getId(), j.getStartTime(), j.getEndTime(),j.getReqQualification(),j.getReqTime());
		this.setPatient(j.isPatient());
		this.setClient(j.isClient());
		this.setMedicalCentre(j.isMedicalCentre());
		this.setIdUser(j.getIdUser());
		this.setTotalPeople(j.getTotalPeople());
	}

	// Getters
	public ArrayList<Jobs> getJobList() {
		return jobList;
	}

	// Setters
	public void setJobList(ArrayList<Jobs> jobList) {
		this.jobList = jobList;
	}


	public String toString() 
	{   String s = "";
	s = s.concat("\nId: " + (this.getSubJobKey()));
	s = s.concat("\n start time: " + (this.getStartTime()));
	s = s.concat("\n end time: " + (this.getEndTime()));
	s = s.concat("\n arrival time: " + (this.getArrivalTime()));
	s = s.concat("\n start service:" + this.getstartServiceTime());
	return s;
	}



}

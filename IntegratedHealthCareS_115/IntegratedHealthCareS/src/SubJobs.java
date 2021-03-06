import java.util.ArrayList;
import java.util.HashMap;

/*
 * This class separate the jobs into pick up and delivery tasks. Save the list of jobs per pairs
 */
public class SubJobs extends Jobs {

	Edge preEdge;
	Edge postEdge;
	double additionalWaintingTime=0;
	double timeWindowViolation=0;
	//private ArrayList<Jobs> jobList= new ArrayList<Jobs>();
	private double[] frequencyRoute;
//	private double possibleStartServiceTime;
//	private double possibleEndServiceTime;


	public SubJobs(Jobs present,Jobs future, SubRoute wr) {
		super(present.getId(), present.getStartTime(), present.getEndTime(),present.getReqQualification(),present.getReqTime()); 
		present.setPair(future);
		present.setWalkingRoute(wr.getJobSequence());
		future.setWalkingRoute(wr.getJobSequence());
		//		for(Jobs j:wr.getJobSequence()) {
		//			jobList.add(j);
		//		}	
	}

	public SubJobs(Jobs present,Jobs future) {
		super(present.getId(), present.getStartTime(), present.getEndTime(),present.getReqQualification(),present.getReqTime()); 
	}

	public SubJobs(Jobs j) {
		super(j);
		this.setId(j.getId());
		this.setStartTime(j.getStartTime());
		this.setEndTime(j.getEndTime());
		this.setReqQualification(j.getReqQualification());
		this.setserviceTime(j.getReqTime());
		this.setPatient(j.isPatient());
		this.setClient(j.isClient());
		this.setMedicalCentre(j.isMedicalCentre());
		this.setIdUser(j.getIdUser());
		this.setSoftStartTime(j.getSoftStartTime());
		this.setSoftEndTime(j.getSoftEndTime());
		//this.setTotalPeople(j.getTotalPeople());
		this.setloadUnloadTime(j.getloadUnloadTime());
		this.setloadUnloadRegistrationTime(j.getloadUnloadRegistrationTime());
		this.setdeltaArrivalDeparture(j.getdeltaArrivalDeparture());
		this.setdeltaArrivalStartServiceTime(j.getdeltaArrivalStartServiceTime());
		this.setdeltarStartServiceTimeEndServiceTime(j.getdeltarStartServiceTimeEndServiceTime());
		this.setsortETWSizeCriterion(j.getsortETWSizeCriterion());
		this.setsortLTWSizeCriterion(j.getsortLTWSizeCriterion());
		
//		if(!j.getAssignedJobToMedicalCentre().isEmpty()) {
//			for(Jobs i:j.getAssignedJobToMedicalCentre()) {
//				this.getAssignedJobToMedicalCentre().add(i);
//			}
//		}
	}

	// Getters
	//public ArrayList<Jobs> getJobList() {return jobList;}
	public Edge getPreEdge() {return preEdge;}
	public Edge getPostEdge() {return postEdge;}
	public double getAdditionalWaintingTime() {return additionalWaintingTime;}
	public double getFrecuencyRoute(int i){return frequencyRoute[i];}
	public double[] getFrecuencyRoute(){return frequencyRoute;}
	public double getAdditionaWaitingTime() {return additionalWaintingTime;}
	public double getTimeWindowViolation() {return timeWindowViolation;}
//	public double getPossibleEndServiceTime() {return possibleEndServiceTime;}
//
//	public double getPossibleStartServiceTime() {return possibleStartServiceTime;}

	// Setters
	//public void setJobList(ArrayList<Jobs> jobList) {this.jobList = jobList;}
	public void setFrecuencyRoute(double[] fr) {this.frequencyRoute = fr;}
	public void setAdditionalWaitingTime(double time) {this.additionalWaintingTime = time;}
	public void setTimeWindowViolation(double time) {this.timeWindowViolation = time;}
//	public void setPossibleEndServiceTime(double end) {possibleEndServiceTime=end;}
//
//	public void setPossibleStartServiceTime(double start) {	possibleStartServiceTime=start;}

	public String toString() 
	{   String s = "";
	s = s.concat("\nId: " + (this.getSubJobKey()));
	s = s.concat("\n start time: " + (this.getStartTime()));
	s = s.concat("\n end time: " + (this.getEndTime()));
	s = s.concat("\n soft start time: " + (this.getSoftStartTime()));
	s = s.concat("\n soft end time: " + (this.getSoftEndTime()));
	s = s.concat("\n arrival time: " + (this.getArrivalTime()));
	s = s.concat("\n departure time: " + (this.getDepartureTime()));
	s = s.concat("\n start service:" + this.getstartServiceTime());
	s = s.concat("\n service time duration: " + (this.getReqTime()));
	s = s.concat("\n end service:" + this.getendServiceTime());
	s = s.concat("\n preparation time: " + (this.getloadUnloadRegistrationTime()));
	return s;
	}





}

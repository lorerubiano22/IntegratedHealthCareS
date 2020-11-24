import java.util.ArrayList;
import java.util.Comparator;

public class Couple {
	private int id=0;
	private Jobs present;
	private Jobs future;
	private double directConnection;
	private double detour;
	private Inputs inp;
	private int qualification;
	private ArrayList<Jobs> jobList= new ArrayList<Jobs>();// in case that it this request means a walking route
	private ArrayList<Jobs> startEndNodes= new ArrayList<Jobs>() ;// in case that it this request means a walking route

	public Couple(Jobs j, Jobs j2, int directConnectionDistance, double percentage) {
		present=j;
		future=j2;
		detour=(int)directConnectionDistance*(percentage);
		directConnection=directConnectionDistance;	
		qualification=Math.max(present.getReqQualification(), future.getReqQualification());
		present.setReqQualification(qualification);
		future.setReqQualification(qualification);
	}



	public Couple(Jobs j, Jobs j2) {
		present=j;
		future=j2;
		detour=0;
		directConnection=0;	
		qualification=Math.max(j.getReqQualification(), j2.getReqQualification());
		startEndNodes.add(present);
		startEndNodes.add(future);
		present.setReqQualification(qualification);
		future.setReqQualification(qualification);
	}
	
	
	public Couple(SubJobs j, SubJobs j2) {
		if(j.getstartServiceTime()<j2.getstartServiceTime()) {
			present=j;
			future=j2;
		}
		else {
			present=j2;
			future=j;
		}
		directConnection=0;	
		startEndNodes.add(j2);
		startEndNodes.add(j);
		qualification=Math.max(j.getReqQualification(), j2.getReqQualification());
	}
	public Couple(Inputs input) {
		inp=input;
	}

	public Couple(Jobs dropOffNode, Jobs pickUpNode, SubRoute r, int directConnectionDistance, double percentage) {
		present=dropOffNode;
		future=pickUpNode;
		jobList= new ArrayList<Jobs>(); 
		for(Jobs j: r.getJobSequence()){ // setting the jobs sequence
			jobList.add(j);
		}
		detour=(int)directConnectionDistance*(percentage);
		directConnection=directConnectionDistance;	
		qualification=Math.max(present.getReqQualification(), future.getReqQualification());
		startEndNodes.add(present);
		startEndNodes.add(future);
		present.setReqQualification(qualification);
		future.setReqQualification(qualification);
	}

	// Setter
	public void setIdCouple(int id) {this.id=id;}
	public void setJobList(ArrayList<Jobs> list) {jobList=list;}
	public void setStartEndNodes(ArrayList<Jobs> firstLastNode) {startEndNodes=firstLastNode;}

	// Getters
	public Jobs getPresent() {	return present;}
	public int getIdCouple() {return id;}
	public Jobs getFuture() {return future;}
	public double getDirectConnection() {return directConnection;}
	public double getQualification() {return qualification;}
	public double getDetour() {return detour;}
	public ArrayList<Jobs> getJobList() {return jobList;}
	public ArrayList<Jobs> getStartEndNodes() {return startEndNodes;}

	@Override
	public String toString() {
		String s = "";
		s = s.concat("\nID couple: " + id);
		s = s.concat("\npresent Job: " + present);
		s = s.concat("\nfuture Job: " + future);
		s = s.concat("\n detour: " + this.detour);
		s = s.concat("\nDirect Connection: " + (this.directConnection));
		return s;
	}
	
	public static Comparator<Couple> ReqQualification = new Comparator<Couple>() { // sort the list of couple in a descending order
		@Override
		public int compare(Couple o1, Couple o2) {
			if (o1.getQualification() > o2.getQualification())
				return -1;
			if (o1.getQualification() < o2.getQualification())
				return 1;
			return 0;
		}
	};



}

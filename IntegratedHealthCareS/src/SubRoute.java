import java.util.HashMap;
import java.util.LinkedList;

public class SubRoute {

	// Walking route attributes:
	// list of edges
	// list of jobs
	// walking time
	// pick-up Node
	// drop-off node
	

	private int slotID=0;
	private Jobs dropOffNode;  // pick-up Node
	private Jobs pickUpNode; // drop-off node
	private double totalTravelTime = 0.0; // route total travel time
	private double totalServiceTime = 0.0; // route total service time in the route
	private LinkedList<Jobs> jobSequence; // list of subroutes which describe the job sequence respecting all restrictions
	private HashMap<Integer,Jobs> jobList;
	private LinkedList<Edge> edges; // list of edges in the walking route
	private double durationWalkingRoute=0; // service time + walking time + waitingTimeRoute
	private double waitingTimeRoute=0; // waiting time
	private double startServiceTime=0;
	private int reqQualification=-1;


	public SubRoute(){ 
		edges = new LinkedList<Edge>();  
		jobSequence = new LinkedList<Jobs>();
		jobList= new HashMap<>();
	}

	// Setters

	
	public void setEdges(LinkedList<Edge> edges) {
		this.edges = edges;
	}
	
	public void addJobSequence(Jobs i, int jobPosition, double serviceStartTime) {
		if(this.reqQualification<i.getReqQualification()) {
			this.reqQualification=i.getReqQualification();
		}
		Jobs jobToInsert= new Jobs(i,serviceStartTime);
		jobSequence.add(jobPosition, jobToInsert);
		jobList.put(i.getId(),i);
		this.startServiceTime=this.getJobSequence().getFirst().getstartServiceTime();
	}

	public void setDropOffNode(Jobs dropOffNode) {
		this.dropOffNode = dropOffNode;
	}

	
	
	public void setPickUpNode(Jobs pickUpNode) {
		this.pickUpNode = pickUpNode;
	}

	public void setTotalTravelTime(double totalTravelTime) {
		this.totalTravelTime = totalTravelTime;
	}

	public void setTotalServiceTime(double serviceTime) {
		this.totalServiceTime = serviceTime;
	}
	
	
	
	public void setDurationWalkingRoute(double durationWR) {
		this.durationWalkingRoute = durationWR;
	}
	
	public void setwaitingTimeRoute(double WWR) {
		this.waitingTimeRoute = WWR;
	}

	public void setSlotID(int slotID) { this.slotID = slotID; }

	// Getters
	public HashMap<Integer,Jobs> getJobList() {
		return jobList;
	}

	public int getSlotID() {
		return slotID;
	}

	public LinkedList<Edge> getEdges() {
		return edges;
	}
	public LinkedList<Jobs> getJobSequence() {
		return jobSequence;
	}

	public Jobs getDropOffNode() {
		return dropOffNode;
	}

	public Jobs getPickUpNode() {
		return pickUpNode;
	}

	public double getTotalTravelTime() {
		return totalTravelTime;
	}

	public double getTotalServiceTime() {
		return totalServiceTime;
	}
	
	public double getDurationWalkingRoute() {
		return durationWalkingRoute;
	}
	
	
	
	
	public int getSkill() {return reqQualification;}

	// Auxiliar methods

	public void updateInfWalkingRoute(Inputs inp) {
		double cumulativeWalkingDistance=0;
		double serviceTime=0;
		double waitingTime=0;
		if(this.getJobSequence().size()>1) {
			for(int i=0;i<this.getJobSequence().size()-1;i++) {
				double travelTime=inp.getWalkCost().getCost(this.getJobSequence().get(i).getId(), this.getJobSequence().get(i+1).getId());
				serviceTime+=this.getJobSequence().get(i).getReqTime();
				cumulativeWalkingDistance+=travelTime;
				waitingTime+=this.getJobSequence().get(i).getWaitingTime();
			}
			waitingTimeRoute+=waitingTime;
			serviceTime+=this.getJobSequence().getLast().getReqTime();
			this.setTotalServiceTime(serviceTime);
			this.setTotalTravelTime(cumulativeWalkingDistance);
			this.setwaitingTimeRoute(waitingTime);
			this.setDurationWalkingRoute(serviceTime+cumulativeWalkingDistance+waitingTime);
		}

		// TO DO: CREATE EDGES - BIG JOB TO DRIVING ROUTES <- MISSING 
	}



	@Override
	public String toString() 
	{   String s = "";
	s = s.concat("\nID route: " + this.slotID);
	s = s.concat("\nRequired Qualification: " + this.reqQualification);
	s = s.concat("\nEdge cumulative walking time: " + this.totalTravelTime);
	s = s.concat("\nEdge cumulative service time: " + this.totalServiceTime);
	s = s.concat("\nEdge cumulative waiting time: " + this.waitingTimeRoute);
	s=s.concat("\nEdge start service time: " + this.startServiceTime);
	s = s.concat("\nEdge total of job in the walking route: " + (this.getJobSequence().size()));
	s = s.concat("\n List of jobs: ");
	for(Jobs j:this.getJobSequence()) {
		s = s.concat(" j_( Id" + j.getId()+", B_"+j.getstartServiceTime()+") ");	
	}
	s = s.concat("\n Information Jobs: ");
	for(Jobs j:this.getJobSequence()) {
		s = s.concat("\n"+j.toString());	
	}
	s = s.concat("\nEdge route duration (service+ walking time): " + (this.durationWalkingRoute));
	return s;
	}



}

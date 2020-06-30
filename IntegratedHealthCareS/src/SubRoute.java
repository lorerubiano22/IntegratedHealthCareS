import java.util.HashMap;
import java.util.LinkedList;

public class SubRoute {

	// Walking route attributes:
	// list of edges
	// list of jobs
	// walking time
	// pick-up Node
	// drop-off node
	private int slotID=-1;
	private Jobs dropOffNode;  // pick-up Node
	private Jobs pickUpNode; // drop-off node
	private double totalTravelTime = 0.0; // route total travel time
	private double totalServiceTime = 0.0; // route total service time in the route
	private LinkedList<Jobs> jobSequence; // list of subroutes which describe the job sequence respecting all restrictions
	private HashMap<Integer,Jobs> jobList;
	private LinkedList<Edge> edges; // list of edges in the walking route
	private double durationWalkingRoute=0; // service time + walking time

	public SubRoute(){   
		edges = new LinkedList<Edge>();  
		jobSequence = new LinkedList<Jobs>();
		jobList= new HashMap<>();
	}

	// Setters
	public void setEdges(LinkedList<Edge> edges) {
		this.edges = edges;
	}
	public void addJobSequence(Jobs i) {
		jobSequence.add(i);
		jobList.put(i.getId(),i);
	}

	public void setDropOffNode(Jobs dropOffNode) {
		this.dropOffNode = dropOffNode;
	}

	public void setPickUpNode(Jobs pickUpNode) {
		this.pickUpNode = pickUpNode;
	}

	public void setdurationWalkingRoute(double durationWalkingRoute) {
		this.durationWalkingRoute = durationWalkingRoute;
	}
	public void setTotalTravelTime(double totalTravelTime) {
		this.totalTravelTime = totalTravelTime;
	}
	
	public void setTotalServiceTime(double serviceTime) {
		this.totalServiceTime = serviceTime;
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
	public double getdurationWalkingRoute() {
		return durationWalkingRoute;
	}

	// Auxiliar methods
	
	public void updateInfWalkingRoute(Inputs inp) {
	double cumulativeWalkingDistance=0;
		double serviceTime=0;
		if(this.getJobSequence().size()>1) {
			for(int i=0;i<this.getJobSequence().size()-1;i++) {
				double travelTime=inp.getWalkCost().getCost(this.getJobSequence().get(i).getId(), this.getJobSequence().get(i+1).getId());
				serviceTime+=this.getJobSequence().get(i).getReqTime();
				cumulativeWalkingDistance+=travelTime;
			}
			serviceTime+=this.getJobSequence().getLast().getReqTime();
			this.setTotalServiceTime(serviceTime);
			this.setTotalTravelTime(cumulativeWalkingDistance);
			this.setdurationWalkingRoute(serviceTime+cumulativeWalkingDistance);
		}
		
		// TO DO: CREATE EDGES <- MISSING 
	}
	
	
	
	@Override
	public String toString() 
	{   String s = "";
	s = s.concat("\nEdge cumulative walking time: " + this.totalTravelTime);
	s = s.concat("\nEdge cumulative service time: " + this.totalServiceTime);
	s = s.concat("\nEdge total of job in the walking route: " + (this.getJobSequence().size()));
	s = s.concat("\nEdge route duration (service+ walking time): " + (this.durationWalkingRoute));
	return s;
	}

	

}

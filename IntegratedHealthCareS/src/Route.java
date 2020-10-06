import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

public class Route {

	private int id=0;
	private double travelTime = 0.0; // travel time
	private double serviceTime = 0.0; // travel time
	private double waitingTime = 0.0; // travel time
	private double durationRoute = 0.0; // route total costs
	private int passengers = 0; // route total demand
	private double amountParamedics=0;// los paramedicos que salen del depot
	private double homeCareStaff=0;// los paramedicos que salen del depot
	private HashMap<String, Edge> edges; // edges list
	private LinkedList<Couple> jobsList= new LinkedList<Couple>(); // subjobs list (pick up and delivary)
	private LinkedList<SubJobs> subJobsList=new LinkedList<SubJobs>(); // subjobs list (pick up and delivary)
	private LinkedList<ArrayList<SubJobs>> partsList=new LinkedList<ArrayList<SubJobs>>(); // subjobs list (pick up and delivary)
	private HashMap<String, SubJobs> positionJobs=new HashMap<>();
	private HashMap<Integer, Jobs>  futureSubJobsList=new HashMap<Integer, Jobs> ();
	private Schift  schift; 

	// Constructors
	public Route(Route r) {
		id=r.getIdRoute();
		travelTime = r.getTravelTime(); // travel time
		serviceTime = r.getServiceTime(); // travel time
		waitingTime = r.getWaitingTime(); // travel time
		durationRoute = r.getDurationRoute(); // route total costs
		passengers = r.getPassengers(); // route total demand
		homeCareStaff=r.getHomeCareStaff();
		amountParamedics=r.getAmountParamedic();
		copyEdges(r.edges); // edges list
		copyCouples(r.jobsList); // subjobs list (pick up and delivary)
		copySubJobs(r.subJobsList); // subjobs list (pick up and delivary)
		copyDirectories(r.getJobsDirectory());
	}


	public Route() {
		id=0;
		travelTime = 0.0; // travel time
		serviceTime = 0.0; // travel time
		waitingTime = 0.0; // travel time
		durationRoute = 0.0; // route total costs
		passengers = 0; // route total demand
		edges=new HashMap<String,Edge>(); // edges list
		jobsList= new LinkedList<Couple>(); // subjobs list (pick up and delivary)
		subJobsList=new LinkedList<SubJobs>(); // subjobs list (pick up and delivary)
		positionJobs=new HashMap<String, SubJobs>();
	}

	private void copySubJobs( LinkedList<SubJobs>  SubJobs) {
		subJobsList=new LinkedList<SubJobs>();
		for(SubJobs j:SubJobs) {
			subJobsList.add(j);
		}
	}




	private void copyDirectories(HashMap<String, SubJobs> jobsDirectory) {
		positionJobs=new HashMap<String, SubJobs>();
		for(SubJobs j:jobsDirectory.values()) {
			positionJobs.put(j.getSubJobKey(),new SubJobs(j));	
		}

	}


	private void copyEdges(HashMap<String, Edge> edges2) {
		edges=new HashMap<String, Edge>();
		for(Edge e:edges2.values()) {
			edges.put(e.getEdgeKey(),e);
		}
	}

	private void copyCouples(LinkedList<Couple> jobsList2) {
		jobsList= new LinkedList<Couple>();
		for(Couple c:jobsList2) {
			jobsList.add(c);
		}
	}



	// Setters
	public void setDurationRoute(double durationRoute) {this.durationRoute = durationRoute;}
	public void setTravelTime(double tv) {this.travelTime = tv;}
	public void setServiceTime(double st) {this.serviceTime = st;}
	public void setWaitingTime(double wt) {this.waitingTime = wt;}
	public void setPassengers(int passengers) {this.passengers = passengers;}
	public void setEdges(HashMap<String, Edge> edges) {this.edges = edges;}
	public void setJobsList(LinkedList<Couple> JobsList) {this.jobsList = JobsList;}
	public void setSubJobsList(LinkedList<SubJobs> subJobsList) {this.subJobsList = subJobsList;}
	public void setIdRoute(int idVehicle) { id=idVehicle;}
	public void setHomeCareStaff(double homeCareStaff) {this.homeCareStaff = homeCareStaff;}
	public void setAmountParamedic(double paramedic) {this.amountParamedics = paramedic;}
	public void setSchiftRoute(Schift s) {schift=s;
	for(ArrayList<SubJobs> a: schift.getRouteParts()) {
		partsList.add(a);
	}
	}


	// Getters
	public HashMap<String, SubJobs> getJobsDirectory(){return positionJobs;}
	public double getDurationRoute() {return durationRoute;}
	public double getServiceTime() {return serviceTime;}
	public double getWaitingTime() {return waitingTime;}
	public double getTravelTime() {return travelTime;}
	public int getIdRoute() {return id;}
	public int getPassengers() {return passengers;}
	public LinkedList<Couple> getJobsList() {return jobsList;}
	public LinkedList<SubJobs> getSubJobsList() {return subJobsList;} // present jobs
	public HashMap<Integer, Jobs>  getSubFutureJobsList() {return futureSubJobsList;} // future jobs
	public HashMap<String, Edge> getEdges() {return edges;}
	public double getAmountParamedic() {return amountParamedics;}
	public double getHomeCareStaff() {return homeCareStaff;}
	public LinkedList<ArrayList<SubJobs>> getPartsRoute() {return partsList;}
	public Schift getSchiftRoute() {return schift;}

	public void computeTravelTime(Inputs inp) {
		double travelTimeDuration=0;
		for(int j=0; j<this.getSubJobsList().size();j++) {
			Jobs jNode=this.getSubJobsList().get(j);
			if(j!=this.getSubJobsList().size()-1 && jNode.getId()!=0) { // travel time
				Jobs kNode=this.getSubJobsList().get(j+1);
				double time=inp.getCarCost().getCost(jNode.getId()-1, kNode.getId()-1);
				travelTimeDuration+=time;
			}
		}
		travelTime=travelTimeDuration;
	}


	public void computePassenger() {
		amountParamedics=0;
		homeCareStaff=0;
		int totalPassenger =getSubJobsList().get(0).getTotalPeople();
		this.setPassengers(totalPassenger);
		if(totalPassenger==1) { // solo una persona es asignada al vehículo
			if(getSubJobsList().get(1).isMedicalCentre() || getSubJobsList().get(1).isPatient()) {
				this.amountParamedics=totalPassenger;
			}
			else {
				homeCareStaff=totalPassenger;
			}
		}
	}


	public void updatingJobsList() {
		positionJobs.clear();
		for(SubJobs nodeI:this.getSubJobsList()) {
			this.positionJobs.put(nodeI.getSubJobKey(), nodeI);	
		}
	}

	public void updateRoute(Inputs inp) {
		// Consider the list of jobs positions
		// reading part
		subJobsList.clear();
		
		for(ArrayList<SubJobs> part:this.getPartsRoute()) {
			for(SubJobs sj:part) {
				subJobsList.add(sj);
			}	
		}
		// service time
		this.computeServiceTime();
		// waiting time
		this.computeWaitingTime();
		// travel time
		this.computeTravelTime(inp);
		this.computePassenger();
		// duration route
		double duration= this.getServiceTime()+this.getTravelTime()+this.getWaitingTime();
		this.setDurationRoute(subJobsList.get(subJobsList.size()-1).getDepartureTime()-subJobsList.get(0).getDepartureTime());
		double idleTime=Math.max(0, (this.getDurationRoute()-duration));
		this.setWaitingTime(this.getWaitingTime()+idleTime);
		updatingJobsList();
	}


	private void computeWaitingTime() {
		double waiting=0;
		for(Jobs j:this.positionJobs.values() ) {
			waiting+=j.getWaitingTime();
		}
		this.setWaitingTime(waiting);
	}

	private void computeServiceTime() {
		double service=0;
		for(Jobs j:this.positionJobs.values() ) {
			service+=j.getReqTime();
		}
		this.setServiceTime(service);
	}



	public String toString() 
	{   String s = "";
	s = s.concat("\nRute duration: " + (this.getDurationRoute()));
	s = s.concat("\nRute waiting time: " + (this.getWaitingTime()));
	s = s.concat("\nRuta demand:" + this.getPassengers());
	s = s.concat("\njobs: ");
	for(SubJobs j:this.subJobsList) {
		s = s.concat("\nID " + j.getSubJobKey() + " arrival time "+ j.getArrivalTime()+ " departure time "+ j.getDepartureTime()+ " start service " + j.getstartServiceTime()+ " waiting time " + j.getWaitingTime());	
	}
	return s;
	}

	

}

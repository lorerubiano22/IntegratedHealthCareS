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
	private LinkedList<Edge> edges; // edges list
	private LinkedList<Couple> jobsList= new LinkedList<Couple>(); // subjobs list (pick up and delivary)
	private LinkedList<Jobs> subJobsList=new LinkedList<Jobs>(); // subjobs list (pick up and delivary)
	private HashMap<Integer, Jobs> positionJobs=new HashMap<Integer, Jobs>();
	private HashMap<Integer, Jobs>  futureSubJobsList=new HashMap<Integer, Jobs> (); 

	// Constructors
	public Route(Route r) {
		id=r.getIdRoute();
		travelTime = r.getTravelTime(); // travel time
		serviceTime = r.getServiceTime(); // travel time
		waitingTime = r.getWaitingTime(); // travel time
		durationRoute = r.getDurationRoute(); // route total costs
		passengers = r.getPassengers(); // route total demand
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
		edges=new LinkedList<Edge>(); // edges list
		jobsList= new LinkedList<Couple>(); // subjobs list (pick up and delivary)
		subJobsList=new LinkedList<Jobs>(); // subjobs list (pick up and delivary)
		positionJobs=new HashMap<Integer, Jobs>();
	}

	private void copySubJobs( LinkedList<Jobs>  SubJobs) {
		subJobsList=new LinkedList<Jobs>();
		for(Jobs j:SubJobs) {
			subJobsList.add(j);
		}
	}




	private void copyDirectories(HashMap<Integer, Jobs> jobsDirectory) {
		positionJobs=new HashMap<Integer, Jobs>();
		for(Jobs j:jobsDirectory.values()) {
			positionJobs.put(j.getId(),new Jobs(j));	
		}

	}


	private void copyEdges(LinkedList<Edge> edges2) {
		edges=new LinkedList<Edge>();
		for(Edge e:edges2) {
			edges.add(e);
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
	public void setEdges(LinkedList<Edge> edges) {this.edges = edges;}
	public void setJobsList(LinkedList<Couple> JobsList) {this.jobsList = JobsList;}
	public void setSubJobsList(LinkedList<Jobs> subJobsList) {this.subJobsList = subJobsList;}
	public void setIdRoute(int idVehicle) { id=idVehicle;}

	// Getters
	public HashMap<Integer, Jobs> getJobsDirectory(){return positionJobs;}
	public double getDurationRoute() {return durationRoute;}
	public double getServiceTime() {return serviceTime;}
	public double getWaitingTime() {return waitingTime;}
	public double getTravelTime() {return travelTime;}
	public int getIdRoute() {return id;}
	public int getPassengers() {return passengers;}
	public LinkedList<Couple> getJobsList() {return jobsList;}
	public LinkedList<Jobs> getSubJobsList() {return subJobsList;} // present jobs
	public HashMap<Integer, Jobs>  getSubFutureJobsList() {return futureSubJobsList;} // future jobs
	public LinkedList<Edge> getEdges() {return edges;}



	public void addCouple(Couple c) {
		jobsList.add(c); // this couple has to have updated the times (service start time, arrival)
		subJobsList.add(c.getPresent());
		subJobsList.add(c.getFuture());
	}
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
		int totalPassenger =0;
		// travel time
		for(int j=0; j<getSubJobsList().size();j++) {
			Jobs nn=getSubJobsList().get(j);
			totalPassenger+=getSubJobsList().get(j).getTotalPeople();
		}
		this.setPassengers(totalPassenger);
	}
	public void updatingJobsList() {
		for(int id=0;id<this.getSubJobsList().size();id++) {
			Jobs nodeI=this.getSubJobsList().get(id);
			if(nodeI.isMedicalCentre()) {
				this.positionJobs.put(nodeI.getIdUser(), nodeI);	}
			else {
				this.positionJobs.put(nodeI.getId(), nodeI);
			}
		}

	}
	public void updateRoute(Inputs inp) {
		// Consider the list of jobs positions
		// service time
		this.computeServiceTime();
		// waiting time
		this.computeWaitingTime();
		// travel time
		this.computeTravelTime(inp);
		this.computePassenger();
		// duration route
		double duration= this.getServiceTime()+this.getTravelTime()+this.getWaitingTime();
		this.setDurationRoute(duration);

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


	public void updateInformationRoute() {
		// variables a actualizar
		//		private double travelTime = 0.0; // travel time
		//		private double serviceTime = 0.0; // travel time
		//		private double waitingTime = 0.0; // travel time
		//		private double durationRoute = 0.0; // route total costs
		//		private int passengers = 0; // route total demand
		//		private LinkedList<Edge> edges; // edges list
		//		private LinkedList<Couple> jobsList= new LinkedList<Couple>(); // subjobs list (pick up and delivary)
		//		private LinkedList<Jobs> subJobsList=new LinkedList<Jobs>(); // subjobs list (pick up and delivary)
		//		private HashMap<Integer, Jobs> positionJobs=new HashMap<Integer, Jobs>();
		//		private HashMap<Integer, Jobs>  futureSubJobsList=new HashMap<Integer, Jobs> (); 
		
		// 1 Compute the start service time
for(int i=this.subJobsList) {
	
}

		// 2. Compute the end service time

		// 3. Compute the arrival time

		// 4. Compute the waiting time

		// 5. Compute service time in the route

		// 6. Computing waiting time in the route

	}

	public String toString() 
	{   String s = "";
	s = s.concat("\nRute duration: " + (this.getDurationRoute()));
	s = s.concat("\nRuta demand:" + this.getPassengers());
	s = s.concat("\njobs: ");
	for(Jobs j:this.subJobsList) {
		s = s.concat("\nID " + j.getId() + "arrival "+ j.getArrivalTime()+ "start service" + j.getstartServiceTime());	
	}
	return s;
	}


	public void updatingJobsFutureList(Route r, Jobs j) {
		r.getSubFutureJobsList().put(j.getId(), j);

	}





}

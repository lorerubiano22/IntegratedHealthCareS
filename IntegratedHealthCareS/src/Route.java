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
		this.setDurationRoute(duration);
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
		double serviceT=0;
		for(int i=this.subJobsList.size()-1;i>=0;i--) {
			Jobs j=this.subJobsList.get(i);
			serviceT+=j.getReqTime();
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
	s = s.concat("\nRute waiting time: " + (this.getWaitingTime()));
	s = s.concat("\nRuta demand:" + this.getPassengers());
	s = s.concat("\njobs: ");
	for(SubJobs j:this.subJobsList) {
		s = s.concat("\nID " + j.getSubJobKey() + " arrival time "+ j.getArrivalTime()+ " departure time "+ j.getDepartureTime()+ " start service " + j.getstartServiceTime()+ " waiting time " + j.getWaitingTime());	
	}
	return s;
	}


	public void updatingJobsFutureList(Route r, Jobs j) {
		r.getSubFutureJobsList().put(j.getId(), j);

	}





	private void computingWaitingTimes(Test test) {
		for(int i=1; i<this.subJobsList.size();i++ ) { // 2. fixing arrival time
			double possibleStartServiceTime=0;
			Jobs currentJob=this.subJobsList.get(i);
			if(currentJob.isClient()) {
				possibleStartServiceTime=currentJob.getArrivalTime()+test.getloadTimeHomeCareStaff();
			}
			else {
				possibleStartServiceTime=currentJob.getArrivalTime()+test.getloadTimePatient()+test.getRegistrationTime();
			}
			currentJob.setWaitingTime(possibleStartServiceTime, currentJob.getStartTime());
			System.out.println("\nRoute Print\n"+this.toString());
		}
	}


	private void updatingFirstJobInTheRoute(Jobs previousJob, double startServiceTime, Test test, Inputs input) {
		previousJob.setStartServiceTime(startServiceTime); //		1. actualizar el tiempo en que inicia el servicio ----private double startServiceTime; // time when the service start
		previousJob.setEndServiceTime(previousJob.getstartServiceTime()+previousJob.getReqTime()); //		2. Actualizar el tiempo en el que se termina el servicio ----- private double endServiceTime; // time when the service start

		double arrivalTime=0;//		3. Actualizar la hora en que el vehículo/ las personas llega a un nodo private double arrivalTime=0; // time for vehicle
		if(previousJob.isClient()) {
			arrivalTime=previousJob.getstartServiceTime()-test.getloadTimeHomeCareStaff();
		}
		else {
			arrivalTime=previousJob.getstartServiceTime()-test.getloadTimePatient()-test.getRegistrationTime();
		}
		previousJob.setarrivalTime(arrivalTime);

		double departure=0;//		4. Actualizar la hora en la que parte de nuevo el vehículo departure Time
		if(previousJob.isClient()) {
			departure=previousJob.getArrivalTime()+test.getloadTimeHomeCareStaff();
		}
		else {
			departure=previousJob.getArrivalTime()+test.getloadTimePatient();
		}
		previousJob.setdepartureTime(departure);		
	}


	private void updateTimesForJobI(Jobs previousJob, Jobs currentJob, Test test, Inputs input) {
		double travelTime= input.getCarCost().getCost(previousJob.getId()-1, currentJob.getId()-1);// sin detour
		double arrivalTime=previousJob.getDepartureTime()+travelTime;// 3. Actualizar la hora en que el vehículo/ las personas llega a un nodo private double arrivalTime=0; // time for vehicle
		currentJob.setarrivalTime(arrivalTime);
		double departureTime=computingDepartureTime(currentJob,test);// 4. Actualizar la hora en la que parte de nuevo el vehículo departure time
		currentJob.setdepartureTime(departureTime);
		double serviceStartTime=Math.max(currentJob.getArrivalTime(), currentJob.getStartTime()); //		1. actualizar el tiempo en que inicia el servicio ----private double startServiceTime; // time when the service start
		currentJob.setStartServiceTime(serviceStartTime);
		currentJob.setEndServiceTime(currentJob.getstartServiceTime()+currentJob.getReqTime());  //		2. Actualizar el tiempo en el que se termina el servicio ----- private double endServiceTime; // time when the service start
	}


	private double computingDepartureTime(Jobs currentJob, Test test) {
		double departure=0;
		if(currentJob.isClient()){
			departure=currentJob.getArrivalTime()+test.getloadTimeHomeCareStaff();
		}
		else {
			departure=currentJob.getArrivalTime()+test.getloadTimePatient();
		}
		return departure;
	}


}

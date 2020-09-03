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
	for(Jobs j:this.subJobsList) {
		s = s.concat("\nID " + j.getId() + " arrival time "+ j.getArrivalTime()+ " departure time "+ j.getDepartureTime()+ " start service " + j.getstartServiceTime()+ " waiting time " + j.getWaitingTime());	
	}
	return s;
	}


	public void updatingJobsFutureList(Route r, Jobs j) {
		r.getSubFutureJobsList().put(j.getId(), j);

	}


	public void aheadTime(Inputs input, Test test) { // los start service time no se tocan!!
		// Se actualizan la información de cada trabajo
		// 1. Fixing start and end service time for each job 
		Jobs previousJob=this.subJobsList.get(0);
		updatingFirstJobInTheRoute(previousJob, previousJob.getEndTime(),test,input);
		//	previousJob.setStartServiceTime(previousJob.getEndTime());
		for(int i=1; i<this.subJobsList.size();i++ ) { // 2. fixing arrival time
			Jobs currentJob=this.subJobsList.get(i);
			updateTimesForJobI(previousJob,currentJob,test,input); // copiar la lista de las variables a actualizar en esta parte
			System.out.println("\nRoute Print\n"+this.toString());

		}
		computingWaitingTimes(test);
		closingTheRoute(input,test);// Adding the depot as the end Job
		System.out.println("\nRoute Print\n"+this.toString());
		// compute the waiting time
		// 3. fixing waiting time
		// update the list of jobs directory
	}


	private void closingTheRoute(Inputs input, Test test) {
		Jobs startRoute=new Jobs(input.getNodes().get(0));
		// 1. Set an arrival time
		//private double arrivalTime=0; // time for vehicle
		Jobs firstJob=this.subJobsList.get(0);
		double travelTime=input.getCarCost().getCost(startRoute.getId()-1, firstJob.getId()-1);
		double departureTimeFirstJob= firstJob.getArrivalTime()-travelTime;
		double loadTimeFirstJob= totalTimeforLoadingVehicle(); // falta calcular el tiempo necesario para el carge de vehículos
		double arrivalStartFirstJob=departureTimeFirstJob-loadTimeFirstJob;
		this.subJobsList.add(0,startRoute);


		// 1. Set an arrival time
		//private double arrivalTime=0; // time for vehicle
		double arrivalStart=0;
				// 2. Set a departure time
				//private double departureTime=0; // time for vehicle


				// 3. Set a time window  
				//private double hardstartTime; // input data early time window 
				// double hardendTime; // input data end time window

				// tipo de nodo/ usuario del servicio
				// private boolean isPatient;// type of job in the system- patient job at patient home
				// private boolean isMedicalCentre;// type of job in the system- patient job at medical centre home
				// private boolean isClient;// type of job in the system- patient job at client home

				// how many people from depot?

				Jobs endRoute=new Jobs(input.getNodes().get(0));

		this.subJobsList.add(endRoute);

	}


	private double totalTimeforLoadingVehicle() {
		// TODO Auto-generated method stub
		return 0;
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

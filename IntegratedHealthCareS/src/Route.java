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
	private double driver=0;// los paramedicos que salen del depot
	private HashMap<String, Edge> edges; // edges list
	private LinkedList<Couple> jobsList= new LinkedList<Couple>(); // subjobs list (pick up and delivary)
	private LinkedList<SubJobs> subJobsList=new LinkedList<SubJobs>(); // subjobs list (pick up and delivary)
	private LinkedList<Parts> partsList=new LinkedList<Parts>(); // subjobs list (pick up and delivary)
	private HashMap<String, SubJobs> positionJobs=new HashMap<>();
	private HashMap<Integer, Jobs>  futureSubJobsList=new HashMap<Integer, Jobs> ();
	private double idleTime=0;
	private Schift  schift; 
	private double  loadUnloadRegistration=0; 

	// Constructors
	public Route(Route r) {
		id=r.getIdRoute();
		travelTime = r.getTravelTime(); // travel time
		serviceTime = r.getServiceTime(); // travel time
		waitingTime = r.getWaitingTime(); // travel time
		durationRoute = r.getDurationRoute(); // route total costs
		passengers = r.getPassengers(); // route total demand
		homeCareStaff=r.getHomeCareStaff();
		loadUnloadRegistration=r.getloadUnloadRegistrationTime();// load unloading time
		amountParamedics=r.getAmountParamedic();
		driver=r.getAmountDriver();
		idleTime=r.getIdleTime();
		copyEdges(r.edges); // edges list
		copyCouples(r.jobsList); // subjobs list (pick up and delivary)
		copySubJobs(r.subJobsList); // subjobs list (pick up and delivary)
		copyDirectories(r.getJobsDirectory());
		copyPart(r.getPartsRoute());
		if(r.schift!=null) {
			schift= new Schift(r.getSchiftRoute());
		}

	}


	private void copyPart(LinkedList<Parts> linkedList) {
		partsList=new LinkedList<Parts>();
		for(Parts part:linkedList) {
			Parts newPart= new Parts(part);
			partsList.add(newPart);
		}
	}


	public Route() {
		id=0;
		travelTime = 0.0; // travel time
		serviceTime = 0.0; // travel time
		waitingTime = 0.0; // travel time
		durationRoute = 0.0; // route total costs
		passengers = 0; // route total demand
		idleTime=0;
		loadUnloadRegistration=0;
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
	public void setSchiftRoute(Schift s) {schift=s;}
	public void setAmountDriver(double d) {this.driver = d;}
	public void setIdleTime(double idleTime) {this.idleTime = idleTime;}
	public void setIloadUnloadRegistrationTime(double i) {this.loadUnloadRegistration = i;}


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
	public LinkedList<Parts> getPartsRoute() {return partsList;}
	public Schift getSchiftRoute() {return schift;}
	public double getAmountDriver() {return driver;}
	public double getIdleTime() {return idleTime;}
	public double getloadUnloadRegistrationTime() {return loadUnloadRegistration;}
	
	// Auxiliar methods

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



	public void updatingJobsList() {
		positionJobs.clear();
		for(SubJobs nodeI:this.getSubJobsList()) {
			this.positionJobs.put(nodeI.getSubJobKey(), nodeI);	
		}
	}

	public void updateRouteFromParts(Inputs inp, Test test) {
		// Consider the list of jobs positions
		// reading part
		subJobsList.clear();
	
		for(Parts part:this.getPartsRoute()) {
			Parts partObject= new Parts(part);
			for(SubJobs sj:partObject.getListSubJobs()) {
				subJobsList.add(sj);
			}	
		}
		if(this.getPartsRoute().size()>2 && !this.getSubJobsList().isEmpty()) {
			// service time
			this.computeServiceTime();
			// waiting time
			this.computeWaitingTime(test);
			// travel time
			this.computeTravelTime(inp);
			//this.computePassenger();
			// duration route
			double duration= this.getServiceTime()+this.getTravelTime()+this.getWaitingTime()+this.getloadUnloadRegistrationTime();
			this.setDurationRoute(subJobsList.get(subJobsList.size()-1).getDepartureTime()-subJobsList.get(0).getDepartureTime());
			double idleTime=Math.max(0, (this.getDurationRoute()-duration));
			this.setIdleTime(this.getWaitingTime()+idleTime);
			updatingJobsList();	// actualizar la lista de directorio de trabajos
		}
	}


	private void computeWaitingTime(Test test) {
		double waiting=0;
		double loadRegistrationTime=0;
		for(Jobs j:this.positionJobs.values() ) {
			double w=0;
			double aditionaltime=0;
			if(j.isClient()) {
				aditionaltime=test.getloadTimeHomeCareStaff();
			}
			else {
				if(j.isMedicalCentre() && j.getTotalPeople()<0) {
					aditionaltime=test.getloadTimePatient()+test.getRegistrationTime();
				}
				else {
					aditionaltime=test.getloadTimePatient();
				}
			}
			if((j.getArrivalTime()+aditionaltime)<j.getstartServiceTime()) {
				w=j.getstartServiceTime()-(j.getArrivalTime()+aditionaltime);
			}
			j.setWaitingTime(w);
			System.out.println(j.toString());
			waiting+=j.getWaitingTime();
			loadRegistrationTime+=aditionaltime;
		}
		this.setWaitingTime(waiting);
		this.setIloadUnloadRegistrationTime(loadRegistrationTime);
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
	s = s.concat("\nRute idle time: " + (this.getIdleTime()));
	s = s.concat("\nRuta passengers:" + this.getPassengers());
	s = s.concat("\njobs: ");
	for(Parts p:this.getPartsRoute()) {
		for(SubJobs j:p.getListSubJobs()) {
			s = s.concat("\nID " + j.getSubJobKey() + " arrival time "+ j.getArrivalTime()+ " departure time "+ j.getDepartureTime()+ " start service time" + j.getstartServiceTime()+ " req time " + j.getReqTime()+ " waiting time " + j.getWaitingTime());	
		}
		s = s.concat("\n\n");
	}
	return s;
	}


	public void removingParts(Parts parts) {
		HashMap<String,SubJobs> toRmove= gettingNodeList(parts);
		boolean isThepartToRemove=false;
		for(Parts a:this.getPartsRoute()) {
			for(SubJobs j:a.getListSubJobs()) {
				if(toRmove.containsKey(j.getSubJobKey())) {
					isThepartToRemove=true;
					break;
				}
			}
			if(isThepartToRemove) {
				this.getPartsRoute().remove(a);
				break;
			}
		}
	}


	private HashMap<String, SubJobs> gettingNodeList(Parts partToRemove) {
		HashMap<String,SubJobs> toRmove= new HashMap<String,SubJobs> ();
		for(SubJobs j:partToRemove.getListSubJobs()) {
			toRmove.put(j.getSubJobKey(), j);
		}
		return toRmove;
	}



}

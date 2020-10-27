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
		driver=r.getAmountDriver();
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


	public void computePassenger() {
		amountParamedics=0;
		homeCareStaff=0;
		int totalPassenger =getSubJobsList().get(0).getTotalPeople();
		this.setPassengers(totalPassenger);
		if(totalPassenger==1) { // solo una persona es asignada al veh�culo
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

	public void updateRouteFromParts(Inputs inp) {

		// Consider the list of jobs positions
		// reading part
		subJobsList.clear();

		for(Parts part:this.getPartsRoute()) {
			Parts partObject= new Parts(part);
			for(SubJobs sj:partObject.getListSubJobs()) {
				subJobsList.add(sj);
			}	
		}
		if(this.getPartsRoute().size()>2) {
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

			updatingJobsList();	// actualizar la lista de directorio de trabajos

		}
	}

	public void updateRouteFromSubJobsList(Inputs inp, Test test) {

		// Consider the list of jobs positions
		// reading part
		settingNewPart(inp,test);

		if(this.getPartsRoute().size()>2) {
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
			updatingJobsList();	// actualizar la lista de directorio de trabajos
		}
	}




	private void settingNewPart(Inputs inp, Test test) {
		SubJobs depot=null;
		int a=0;
		if(this.getSubJobsList().get(0).getId()==1) { // it is for the route which are already merging
		a=1;
			this.getPartsRoute().clear();
			depot=this.getSubJobsList().get(0);
		}
		else {
			if(this.getPartsRoute().get(0).getListSubJobs().get(0).getId()==1) {
				depot=this.getPartsRoute().get(0).getListSubJobs().get(0);
			}
		}
		int passengers=depot.getTotalPeople();
		ArrayList<Parts> parts= new ArrayList<>();
		ArrayList<SubJobs> listSubJobs= new ArrayList<>();
		listSubJobs.add(this.getSubJobsList().getFirst());
		Parts part=new Parts();
		part.setListSubJobs(listSubJobs, inp, test);
		parts.add(part);
		part=new Parts();
		listSubJobs= new ArrayList<>();
		for(int i=a;i<this.getSubJobsList().size()-1;i++) {
			SubJobs j=this.getSubJobsList().get(i);
			passengers+=j.getTotalPeople();
			listSubJobs.add(j);
			if(passengers==0) {
				part=new Parts();
				part.setListSubJobs(listSubJobs, inp, test);
				parts.add(part);
				listSubJobs= new ArrayList<>();
			}
			if(this.getSubJobsList().getLast()==this.getSubJobsList().get(i+1)) {
				part=new Parts();
				part.setListSubJobs(listSubJobs, inp, test);
				parts.add(part);
			}
		}		
		listSubJobs= new ArrayList<>();
		if(this.getSubJobsList().getLast().getId()==1) {
			listSubJobs.add(this.getSubJobsList().getLast());
			this.getSubJobsList().removeFirst();
			this.getSubJobsList().removeLast();
		}
		else {
			listSubJobs.add(this.getPartsRoute().getLast().getListSubJobs().get(0));
		}
		
		part=new Parts();
		part.setListSubJobs(listSubJobs, inp, test);
		parts.add(part);
		
		this.getPartsRoute().clear();
		for(Parts p:parts) {
			this.getPartsRoute().add(p);
		}
		System.out.print(this.toString());
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
		s = s.concat("\nID " + j.getSubJobKey() + " arrival time "+ j.getArrivalTime()+ " departure time "+ j.getDepartureTime()+ " start service time" + j.getstartServiceTime()+ " waiting time " + j.getWaitingTime());	
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

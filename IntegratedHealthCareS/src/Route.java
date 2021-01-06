import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

public class Route {

	private int id=0;
	private double travelTime = 0.0; // travel time
	private double travelTimeDriver = 0.0; // travel time
	private double serviceTime = 0.0; // travel time
	private double waitingTime = 0.0; // travel time
	private double durationRoute = 0.0; // route total costs
	private int passengers = 0; // route total demand
	private double amountParamedics=0;// los paramedicos que salen del depot
	private double homeCareStaff=0;// los paramedicos que salen del depot
	private double driver=0;// los paramedicos que salen del depot
	private double detour=0;// los paramedicos que salen del depot
	private double detourPromParamedic=0;// detour prom paramedic
	private double detourPromHomeCareStaff=0;// detour prom home care staff
	private HashMap<String, Edge> edges; // edges list
	private LinkedList<Couple> jobsList= new LinkedList<Couple>(); // subjobs list (pick up and delivary)
	private LinkedList<SubJobs> subJobsList=new LinkedList<SubJobs>(); // subjobs list (pick up and delivary)
	private LinkedList<Parts> partsList=new LinkedList<Parts>(); // subjobs list (pick up and delivary)
	private HashMap<String, SubJobs> positionJobs=new HashMap<>();
	private HashMap<Integer, Jobs>  futureSubJobsList=new HashMap<Integer, Jobs> ();
	private double idleTime=0;
	private Schift  schift; 
	private double  loadUnloadRegistration=0; 
	private double driverCost=0;// los paramedicos que salen del depot
	private double homeCareStaffCost=0;// los paramedicos que salen del depot
	private double additionalWaitingTime=0;
	double timeWindowViolation=0;
	double detourViolation=0;

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
		additionalWaitingTime=r.getAdditionalwaitingTime();
		amountParamedics=r.getAmountParamedic();
		driver=r.getAmountDriver();
		idleTime=r.getIdleTime();
		detour=r.getdetour();
		driverCost=r.driverCost;// los paramedicos que salen del depot
		homeCareStaffCost=r.driverCost;// los paramedicos que salen del depot
		detourPromParamedic=r.getdetourPromParamedic();// detour prom paramedic
		detourPromHomeCareStaff=r.getdetourPromHomeCareStaff();// detour prom home care staff
		copyEdges(r.edges); // edges list
		copyCouples(r.jobsList); // subjobs list (pick up and delivary)
		copySubJobs(r.subJobsList); // subjobs list (pick up and delivary)
		copyDirectories(r.getJobsDirectory());
		copyPart(r.getPartsRoute());
		if(r.schift!=null) {
			schift= new Schift(r.getSchiftRoute());
		}
	}





	private double getdetour() {return detour; }
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
	public void setDetour(double d) {this.detour = d;}
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
	public void setdriverCost(double i) {this.driverCost = i;}
	public void sethomeCareStaffCost(double i) {this.homeCareStaffCost = i;}
	public void setAdditionalWaitingTime(double wt) {additionalWaitingTime=wt;}
	public void settimeWindowViolation(double wt) {timeWindowViolation=wt;}
	public void setdetourViolation(double detour) {detourViolation= detour;}
	public void setdetourPromParamedic(double wt) {detourPromParamedic=wt;}
	public void setdetourPromHomeCareStaff(double detour) {detourPromHomeCareStaff= detour;}



	// Getters
	public HashMap<String, SubJobs> getJobsDirectory(){return positionJobs;}
	public double getDurationRoute() {return durationRoute;}
	public double getServiceTime() {return serviceTime;}
	public double getWaitingTime() {return (int)waitingTime;}
	public double gettimeWindowViolation() {return timeWindowViolation;}
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
	public double getDriverTime() {return travelTimeDriver;}
	public double getAdditionalwaitingTime() {	return additionalWaitingTime;}
	public double getdetourViolation() {	return detourViolation;}
	public double getdriverCost() {return driverCost;}
	public double gethomeCareStaffCost() {return homeCareStaffCost;}
	public double getDetour() {return detour;}
	public double getdetourPromParamedic() {return detourPromParamedic;}
	public double getdetourPromHomeCareStaff() {return detourPromHomeCareStaff;}




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

	public void computeDriverTravelTime(Inputs inp) {
		double travelTimeDuration=0;
		for(int j=0; j<this.getSubJobsList().size();j++) {
			Jobs jNode=this.getSubJobsList().get(j);
			double tvToDepot=inp.getCarCost().getCost(jNode.getId()-1, 0);
			if(j!=this.getSubJobsList().size()-1 && jNode.getId()!=0) { // travel time
				Jobs kNode=this.getSubJobsList().get(j+1);
				double tvFromoDepot=inp.getCarCost().getCost(0,kNode.getId()-1);
				if(jNode.getDepartureTime()+tvFromoDepot+tvToDepot<kNode.getArrivalTime()) {
					travelTimeDuration+=tvToDepot;
					travelTimeDuration+=tvFromoDepot;
				}
				//				else{double time=inp.getCarCost().getCost(jNode.getId()-1, kNode.getId()-1);
				//				travelTimeDuration+=time;}
			}
		}
		travelTimeDriver=travelTimeDuration+travelTime;
	}


	public void updatingJobsList() {
		positionJobs.clear();
		for(SubJobs nodeI:this.getSubJobsList()) {
			this.positionJobs.put(nodeI.getSubJobKey(), nodeI);	
		}
	}

	public void updateRouteFromParts(Inputs inp, Test test, HashMap<Integer, SubRoute> jobsInWalkingRoute) {
		// Consider the list of jobs positions
		// reading part
		subJobsList.clear();

		LinkedList<Parts> partInRoute= new LinkedList<Parts>();
		for(Parts part:this.getPartsRoute()) {
			if(!part.getListSubJobs().isEmpty()) {
				partInRoute.add(part);
			}
		}
		this.getPartsRoute().clear();
		for(Parts p:partInRoute) {
			this.getPartsRoute().add(p);
		}
		for(Parts part:this.getPartsRoute()) {
			Parts partObject= new Parts(part);
			for(SubJobs sj:partObject.getListSubJobs()) {
				if(sj.getId()!=1) {
					subJobsList.add(sj);
					positionJobs.put(sj.getSubJobKey(),sj);
				}
			}	
		}


		if(this.getPartsRoute().size()>2 && !this.getSubJobsList().isEmpty()) {
			// service time
			this.computeServiceTime(inp,jobsInWalkingRoute);
			// waiting time
			this.computeWaitingTime(test);
			// travel time
			this.computeTravelTime(inp);
			//this.computePassenger();
			this.computeDriverTravelTime(inp);

			this.computePenalizationParameters();

			// duration route
			double duration= this.getServiceTime()+this.getTravelTime()+this.getWaitingTime()+this.getloadUnloadRegistrationTime();
			this.setDurationRoute(subJobsList.get(subJobsList.size()-1).getDepartureTime()-subJobsList.get(0).getDepartureTime());
			double idleTime=Math.max(0, (this.getDurationRoute()-duration));
			this.setIdleTime(this.getWaitingTime()+idleTime);
			updatingJobsList();	// updating subjobs list
		}
	}


	private void computePenalizationParameters() {
		double penalization=0;
		for(SubJobs s:this.getSubJobsList()) {
			penalization+=	s.getAdditionalWaintingTime();
		}
		this.setAdditionalWaitingTime(penalization);
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

	public void computeServiceTime(Inputs inp, HashMap<Integer, SubRoute> jobsInWalkingRoute) {
		double service=0;
		HashMap<String, Jobs> assigned=new HashMap<String, Jobs>();

		for(Jobs j:this.positionJobs.values() ) {
			assigned.put(j.getSubJobKey(), j);
			if(jobsInWalkingRoute.containsKey(j.getId())) {
				SubRoute r=jobsInWalkingRoute.get(j.getId());
				for(int i=1;i<r.getJobSequence().size();i++) {
					Jobs sj=r.getJobSequence().get(i);
					assigned.put(sj.getSubJobKey(), sj);
				}
			}
		}

		for(Jobs j:assigned.values() ) {
			if(j.getId()==27 || j.getId()==13) {
				System.out.println(j.toString());
			}
			service+=j.getReqTime();
			//service+=inp.getdirectoryNodes().get(j.getId());
		}
		this.setServiceTime(service);
	}



	public String toString() 
	{   String s = "";
	//	s = s.concat("\nRute duration: " + (this.getDurationRoute()));
	//	s = s.concat("\nRute waiting time: " + (this.getWaitingTime()));
	//	s = s.concat("\nRute service time: " + (this.getServiceTime()));
	//	s = s.concat("\nRute idle time: " + (this.getIdleTime()));
	//	s = s.concat("\nRuta home care staff:" + this.getHomeCareStaff());
	//	s = s.concat("\nRuta paramedic staff:" + this.getAmountParamedic());
	s = s.concat("\njobs: ");
	for(Parts p:this.getPartsRoute()) {
		for(SubJobs j:p.getListSubJobs()) {
			String type="";
			if(j.isClient()) {
				type="c";
			}
			if(j.isPatient()) {
				type="p";
			}
			s = s.concat(" ( " + j.getSubJobKey()+type+" A  "+(int)j.getArrivalTime()+"  B  "+(int)j.getstartServiceTime()+ " end service "+ (int)j.getendServiceTime()+"   D  "+(int)j.getDepartureTime()+"  reqTime_"+j.getReqTime()+"  TW ["+(int)j.getStartTime()+";"+(int)j.getEndTime()+"]"+") \n");
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





	public void checkingTimesRoute(Test test, Inputs inp) {

		for(int i=1;i<this.getSubJobsList().size();i++) {
			SubJobs a=this.getSubJobsList().get(i-1);
			SubJobs b=this.getSubJobsList().get(i);
			double tv=inp.getCarCost().getCost(a.getId()-1, b.getId()-1);

			// los tiempos definitivos, los tiempos reales 
			double arrivalTimeVehicle=a.getDepartureTime()+tv;
			double startServiceTime=Math.max(arrivalTimeVehicle+a.getloadUnloadRegistrationTime()+a.getloadUnloadTime(),b.getStartTime());
			double endServiceTime=startServiceTime+b.getReqTime();
			double departureServiceTime=arrivalTimeVehicle+a.getloadUnloadTime();
			b.setarrivalTime(arrivalTimeVehicle);
			b.setStartServiceTime(startServiceTime);
			b.setEndServiceTime(endServiceTime);
			b.setdepartureTime(departureServiceTime);
			System.out.println(b.toString());
		}
		System.out.println(this.toString());
	}





	public void checkingTimeWindows(Test test, Inputs inp) {
		double penalization=0;
		double penalizationRoute=0;
		for(int p=1;p<this.getPartsRoute().size()-1;p++) {
			for(SubJobs j:this.getPartsRoute().get(p).getListSubJobs()) {
				if(j.isClient() || j.isMedicalCentre()) {
					if(j.getTotalPeople()<0) {// drop-off
						if(j.getstartServiceTime()>j.getEndTime()) {
							penalization+=Math.abs(j.getEndTime()-j.getstartServiceTime());
							penalizationRoute+=penalization;
						}
						j.setTimeWindowViolation(penalization);

					}
				}
			}
		}
		this.settimeWindowViolation(penalizationRoute);
		System.out.println(this.toString());
	}

	public void checkingWaitingTimes(Test test, Inputs inp) {
		double penalization=0;
		double penalizationRoute=0;
		double additionalWaitingRoute=0;
		for(int p=1;p<this.getPartsRoute().size()-1;p++) {
			for(SubJobs j:this.getPartsRoute().get(p).getListSubJobs()) { // se producen despues de terminar un servicio
				System.out.println("Id "+j.getSubJobKey()+" Arrival "+ j.getArrivalTime()+ " Start service time " + j.getstartServiceTime());
				System.out.println(" Calculated Start service time" + (j.getArrivalTime()+j.getdeltaArrivalStartServiceTime()));
				if(j.getTotalPeople()<0) { // drop off 
					// Home health care
					if(j.isClient()) { // hard time window
						if((j.getArrivalTime()+test.getloadTimeHomeCareStaff())<j.getstartServiceTime()) { // llega antes el personal tiene que esperar al cliente
							penalization=j.getstartServiceTime()-(j.getArrivalTime()+test.getloadTimeHomeCareStaff());
							penalizationRoute+=penalization;
						}	
					}
					else {
						if(j.isMedicalCentre()) { // fixed time
							if((j.getArrivalTime()+test.getRegistrationTime()+test.getloadTimePatient())<j.getstartServiceTime()) { // llega antes el personal tiene que esperar al cliente
								penalization=j.getstartServiceTime()-(j.getArrivalTime()+test.getRegistrationTime()+test.getloadTimePatient());
								penalizationRoute+=penalization;
							}	
						}
						else { // patient
							if((j.getArrivalTime()+test.getloadTimePatient())<j.getstartServiceTime()) { // llega antes el personal tiene que esperar al cliente
								penalization=j.getstartServiceTime()-(j.getArrivalTime()+test.getloadTimePatient());
								penalizationRoute+=penalization;
							}
						}
					}

					j.setWaitingTime(Math.abs(penalization));
					j.setAdditionalWaitingTime(Math.abs(additionalWaitingRoute));
				}
				else { // pick up
					if(j.isClient() || j.isMedicalCentre()) {
						if(j.getArrivalTime()>j.getStartTime()) {
							penalization=j.getArrivalTime()-j.getStartTime();
							penalizationRoute+=penalization;}}
				}

			}
		if(penalization<0) {
			System.out.println("Stop");
		}	
		}
		this.setWaitingTime(penalizationRoute);
		this.setAdditionalWaitingTime(additionalWaitingRoute);
		System.out.println(this.toString());
	}





	public void checkingDetour(Test test, Inputs inp, Solution initialSol) {
		double penalization=0;
		double penalizationRotue=0;
		double detour=0;
		double detourHomeCareStaff=0;
		double detourParamedic=0;
		// calculating detour
		computingDetours(inp,test,initialSol);
		// calculating additional times per edge
		HashMap<String, SubJobs> subJobsList= new HashMap<String, SubJobs>();
		for(Parts p:this.getPartsRoute()) {
			for(SubJobs sj:p.getListSubJobs()) {
				subJobsList.put(sj.getSubJobKey(), sj);
			}	
		}

		for(Route r: initialSol.getRoutes()) {

			for(Edge e:r.getEdges().values()) {
				SubJobs origen=e.getOrigin();
				SubJobs end=e.getEnd();
				if(origen.getId()==1 || end.getId()==1) {
					System.out.println("Stop");
				}
				if(subJobsList.containsKey(origen.getSubJobKey()) && subJobsList.containsKey(end.getSubJobKey()) ) {
					this.getEdges().put(e.getEdgeKey(), e);
					if(e.gettravelTimeInRoute()>e.getDetour()) {
						penalization=e.gettravelTimeInRoute()-e.getDetour();
						penalizationRotue+=penalization;
					}
					e.setadditionalTime(penalization);
					if(e.gettravelTimeInRoute()<e.getTime()) {
						System.out.println("Stop");
					}
					if(r.getHomeCareStaff()==1) {
						detourHomeCareStaff+=(e.gettravelTimeInRoute()-e.getTime());
					}
					if(r.getAmountParamedic()==1) {
						detourParamedic+=(e.gettravelTimeInRoute()-e.getTime());
					}
					detour+=(e.gettravelTimeInRoute()-e.getTime());
					if(detour>0) {
						System.out.println("Stop");
					}}
			}
		}
		this.setdetourPromHomeCareStaff(detourHomeCareStaff);
		this.setdetourPromParamedic(detourParamedic);
		this.setdetourViolation(penalizationRotue);		
		this.setDetour(detour);
	}





	private void computingDetours(Inputs inp, Test test, Solution initialSol) {
		String depotS="P1";
		HashMap<String, SubJobs> subJobsList= new HashMap<String, SubJobs>();
		ArrayList<SubJobs> sequence= new ArrayList<SubJobs>();
		for(Parts p:this.getPartsRoute()) {
			for(SubJobs sj:p.getListSubJobs()) {
				subJobsList.put(sj.getSubJobKey(), sj);
				sequence.add(sj);
			}	
		}
		for(Route route:initialSol.getRoutes()) {
			for(Edge e: route.getEdges().values()) { // is there a detour??
				SubJobs origen=e.getOrigin();
				SubJobs end=e.getEnd();
				if(origen.getId()==1 || end.getId()==1) {
					System.out.println("Stop");
				}
				if(subJobsList.containsKey(origen.getSubJobKey()) && subJobsList.containsKey(end.getSubJobKey()) ) {
					double travelTime=0;
					boolean startCount=false;
					for(int i=1; i<sequence.size();i++) { // iterating over the rotue
						SubJobs r=sequence.get(i-1);
						SubJobs s=sequence.get(i);

						if(r.getSubJobKey().equals(origen.getSubJobKey())) {
							startCount=true;
						}
						if(startCount) {
							travelTime+=inp.getCarCost().getCost(r.getId()-1, s.getId()-1);
						}
						if(s.getSubJobKey().equals(end.getSubJobKey())) {
							break;
						}
					}
					e.setTravelTimeInRoute(travelTime);
					if(travelTime> e.getTime()) {
						depotS="P1";
					}
				}
			}
		}

	}





	public void computeHomCareStaffCost(Solution initialSol) {
		double distance=0;
		for(Edge e:this.edges.values()) {
			distance+=e.gettravelTimeInRoute();
		}

	}


	public static Comparator<Route> SORT_BY_EarlyJob = new Comparator<Route>() { 

		@Override 

		public int compare(Route r1, Route r2) { 

			if (r1.getSubJobsList().get(0).getstartServiceTime() < r2.getSubJobsList().get(0).getstartServiceTime()) 

				return 1; 

			if (r1.getSubJobsList().get(0).getstartServiceTime() > r2.getSubJobsList().get(0).getstartServiceTime()) 

				return -1; 

			return 0; 

		} 

	}; 

	public static Comparator<Route> SORT_BY_RouteLength = new Comparator<Route>() { 

		@Override 

		public int compare(Route r1, Route r2) { 

			if (r1.getSubJobsList().size() > r2.getSubJobsList().size() ) 

				return 1; 

			if (r1.getSubJobsList().size() <= r2.getSubJobsList().size() ) 

				return -1; 

			return 0; 

		} 

	};





	public void updateRouteFromSubJobs(Inputs inp, Test test, HashMap<Integer, SubRoute> jobsInWalkingRoute,
			ArrayList<SubJobs> partStart, ArrayList<SubJobs> partEnd, ArrayList<SubJobs> listSubJobs) {
		this.getPartsRoute().clear();
		Parts partObject= new Parts();

		partObject.setListSubJobs(partStart,inp,test);
		this.getPartsRoute().add(partObject);

		// 1. hacer las partes
		double passengers=1;
		ArrayList<SubJobs> part= new ArrayList<SubJobs>();
		part= new ArrayList<SubJobs>();
		partObject= new Parts();
		this.getPartsRoute().add(partObject);
		for(int i=0;i<listSubJobs.size();i++) {
			SubJobs sj=listSubJobs.get(i);
			passengers+=sj.getTotalPeople();
			if(passengers!=0) {
				partObject.getListSubJobs().add(sj);
				if(i==listSubJobs.size()-1) {
					this.updateRouteFromParts(inp,test,jobsInWalkingRoute);
					System.out.println(this.toString());
				}
			}
			else {
				partObject.getListSubJobs().add(sj);
				partObject= new Parts();
				this.getPartsRoute().add(partObject);
				System.out.println(this.toString());
			}		
		}
		partObject= new Parts();
		partObject.setListSubJobs(partEnd,inp,test);
		this.getPartsRoute().add(partObject);
		this.updateRouteFromParts(inp,test,jobsInWalkingRoute);

	}





	public void totalMedicalStaff() {
		double homeCareStaff=0;
		double paramedic=0;

		double auxhhc=0;
		double auxparamedic=0;
		for(SubJobs j:this.getSubJobsList()) {
			if(j.getTotalPeople()>0 && j.isPatient()) {
				auxparamedic+=j.getTotalPeople();
				if(auxparamedic!=0) {
					paramedic++;
				}
			}
			if(j.getTotalPeople()<0 && j.isPatient()) {
				auxparamedic+=j.getTotalPeople();
			}

			if(j.getTotalPeople()<0 && j.isClient()) {
				auxhhc+=j.getTotalPeople();
				if(auxhhc!=0) {
					homeCareStaff++;
				}
			}
			if(j.getTotalPeople()>0 && j.isClient()) {
				auxhhc+=j.getTotalPeople();
			}

		}
		//		if(auxhhc>homeCareStaff) {
		//			homeCareStaff=auxhhc;
		//		}
		//		if(auxparamedic>paramedic) {
		//			paramedic=auxparamedic;
		//		}
		this.setAmountParamedic(paramedic);
		this.setHomeCareStaff(homeCareStaff);
		System.out.println("total HHC"+ homeCareStaff);
		System.out.println("total Paramedic"+ paramedic);
		System.out.println("total");

	}





	public void countingMedicalStaff() {
		double paramedics=0;
		double paramedicsAux=0;
		double homeCareStaff=0;
		double homeCareStaffAUX=0;
		System.out.println(this.toString());
		for(SubJobs j:this.getSubJobsList()) {
			if(j.isPatient() ) {
				if(j.getTotalPeople()>0) {
					paramedicsAux++;
					if(paramedicsAux>0) {
						paramedics++;
					}
				}
				else {
					paramedicsAux--;
				}
			}
			if(j.isClient() ) {
				if(j.getTotalPeople()<0) {
					homeCareStaffAUX++;
					if(homeCareStaffAUX>0) {
						homeCareStaff++;
					}
				}
				else {
					homeCareStaffAUX--;
				}
			}

		}
		this.setAmountParamedic(paramedics);	
		this.setHomeCareStaff(homeCareStaff);
	}





	public void computeTravelTime() {
		double distance= 0;
		for(Edge e:this.getEdges().values()) {
			distance+= e.getTime();
		}
		this.setTravelTime(distance);
	}





}

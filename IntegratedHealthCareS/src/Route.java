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
	private double travelTimeParamedic = 0.0; // travel time
	private double travelTimeHHC = 0.0; // travel time
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
	int qualificationLevel=-1;

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
		timeWindowViolation=r.gettimeWindowViolation();
		detourViolation=r.detourViolation;
		travelTimeParamedic = r.travelTimeParamedic; // travel time
		travelTimeHHC = r.travelTimeHHC; // travel time
		copyEdges(r.edges); // edges list
		copyCouples(r.jobsList); // subjobs list (pick up and delivary)
		copySubJobs(r.subJobsList); // subjobs list (pick up and delivary)
		copyDirectories(this.subJobsList);
		copyPart(r.getPartsRoute());

		qualificationLevel=-1;
		for(SubJobs j:this.subJobsList) {
			if(qualificationLevel<j.getReqQualification()) {
				qualificationLevel=j.getReqQualification();
			}		
		}



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
		travelTimeParamedic =0; // travel time
		travelTimeHHC = 0; // travel time
		loadUnloadRegistration=0;
		edges=new HashMap<String,Edge>(); // edges list
		jobsList= new LinkedList<Couple>(); // subjobs list (pick up and delivary)
		subJobsList=new LinkedList<SubJobs>(); // subjobs list (pick up and delivary)
		positionJobs=new HashMap<String, SubJobs>();
		qualificationLevel=-1;
	}

	private void copySubJobs( LinkedList<SubJobs>  SubJobs) {
		subJobsList=new LinkedList<SubJobs>();
		for(SubJobs j:SubJobs) {
			subJobsList.add(new SubJobs(j));
		}
	}




	private void copyDirectories(LinkedList<SubJobs> subJobsList2) {
		positionJobs=new HashMap<String, SubJobs>();
		for(SubJobs j:subJobsList2) {
			positionJobs.put(j.getSubJobKey(),j);	
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
	public void setQualificationLevel(int q) {qualificationLevel=q;}


	// Getters

	public int getQualificationLevel() {return qualificationLevel;}
	public HashMap<String, SubJobs> getJobsDirectory(){return positionJobs;}
	public double getDurationRoute() {return durationRoute;}
	public double getServiceTime() {return serviceTime;}
	public double getWaitingTime() {return (int)waitingTime;}
	public double gettimeWindowViolation() {return timeWindowViolation;}
	public double getTravelTime() {return travelTime;}
	public double getTravelTimeParamedic() {return travelTimeParamedic;}
	public double getTravelTimeHHC() {return travelTimeHHC;}
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
		for(Edge e:this.edges.values()) {
			if(e.getTime()==0) {
				System.out.println("computeTravelTime=0");
			}

			travelTimeDuration+=e.getTime();
		}
		this.setTravelTime(travelTimeDuration);

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
	s = s.concat("\nRute duration: " + (this.getDurationRoute()));
	s = s.concat("\nRute waiting time: " + (this.getWaitingTime()));
	//s = s.concat("\nRute service time: " + (this.getServiceTime()));
	//s = s.concat("\nRute idle time: " + (this.getIdleTime()));
	s = s.concat("\nAmount home care staff:" + this.getHomeCareStaff());
	s = s.concat("\nAmount paramedic staff:" + this.getAmountParamedic());
	s = s.concat("\nHome care staff and paramedic cost:" + this.gethomeCareStaffCost());
	s = s.concat("\nDriver cost:" + this.getdriverCost());
	s = s.concat("\njobs: ");
	for(Parts p:this.getPartsRoute()) {
		for(SubJobs j:p.getListSubJobs()) {
			//for(SubJobs j:this.getSubJobsList()) {
			String type="";
			if(j.isClient()) {
				type="c";
			}
			if(j.isPatient()) {
				type="p";
			}
			s = s.concat(" ( " + j.getSubJobKey()+type+" A  "+(int)j.getArrivalTime()+"  B  "+(int)j.getstartServiceTime()+ " end service "+ (int)j.getendServiceTime()+"   D  "+(int)j.getDepartureTime()+"  reqTime_"+j.getReqTime()+"  TW ["+(int)j.getSoftStartTime()+";"+(int)j.getSoftEndTime()+"]"+") \n");
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
						if(j.getstartServiceTime()>j.getSoftEndTime()) {
							penalization+=Math.abs(j.getSoftEndTime()-j.getstartServiceTime());
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
		double waitingTime=0;
		double additionalWaitingRoute=0;
		for(SubJobs j:this.getSubJobsList()) {
			double additionaltime=determineAdditionalTime(j,test);

			double delta=(j.getstartServiceTime()-(j.getArrivalTime()+additionaltime));
			if(delta>test.getCumulativeWaitingTime()) {
				additionalWaitingRoute+=delta;
			}
			if(delta>0) {
				waitingTime+=delta;
				System.out.println(j.toString());
			}
			if(delta<0) {
				System.out.println(j.toString());
			}
			j.setWaitingTime(delta);
		}

		this.setWaitingTime(waitingTime);
		this.setAdditionalWaitingTime(additionalWaitingRoute);
		System.out.println(this.toString());
	}




	private double determineAdditionalTime(SubJobs j,Test test) {
		double additionalTime=0;
		if(j.getTotalPeople()<0) {
			additionalTime=timeDropOffBeforeService(j,test); //medical centre, patient, client
		}
		else {
			//additionalTime=timePickUpAfterService(j);
		}
		return additionalTime;
	}




	private double timeDropOffBeforeService(SubJobs j,Test test) {
		double additionalTime=0;
		//medical centre, patient, client
		if(j.isMedicalCentre()) {// medical centre
			//additionalTime=test.getRegistrationTime();
			additionalTime=test.getloadTimePatient()+test.getRegistrationTime();
		}
		else {
			if(j.isClient()) {// client
				additionalTime=test.getloadTimeHomeCareStaff();
			}
			else {//patient
				//	additionalTime=test.getloadTimePatient();

			}
		}
		return additionalTime;
	}





	public void checkingDetour(Test test, Inputs inp, Solution initialSol) {
		double detour=0;
		double detourToPenalize=0;
		double dist=0;
		double distRef=0;
		for(Edge e: this.edges.values()) {
			distRef=e.getTime();
			SubJobs origen=e.getOrigin();
			SubJobs end=e.getEnd();
			if(origen.getSubJobKey().equals("P10")) {
				System.out.println(" Edge "+ e.toString());
			}
			if(origen.getSubJobKey().equals("D52") && end.getSubJobKey().equals("P73")) {
				System.out.println(" Edge "+ e.toString());
			}
			if(origen.getSubJobKey().equals("P70") && end.getSubJobKey().equals("D4770")) {
				System.out.println(" Edge "+ e.toString());
			}


			Route r1=null;

			Route r2=null;
			if(origen.getId()!=1 && end.getId()!=1) {
				r1=selectionRoute(origen,initialSol);
				r2=selectionRoute(end,initialSol);
			}
			else {
				if(origen.getId()==1 ) {
					r1=selectionRoute(end,initialSol);

					r2=selectionRoute(end,initialSol);
				}
				else {
					r1=selectionRoute(origen,initialSol);
					r2=selectionRoute(origen,initialSol);
				}
			}
			if(r1==r2) { // there is a connection in the route
				dist=0;
				boolean startCount=false;
				boolean newStart=false;
				ArrayList<SubJobs> copy= new ArrayList<SubJobs>();
				for(Parts p:r1.getPartsRoute()) {
					for(SubJobs j:p.getListSubJobs()) {
						copy.add(j);
					}
				}
				boolean breakCycle=false;
				for(int i=1;i<copy.size();i++) {
					SubJobs Ijob=copy.get(i-1);
					SubJobs Jjob=copy.get(i);
					if(Ijob.getSubJobKey().equals("P10") ) {
						System.out.println(" Edge "+ e.toString());
					}
					if(Jjob.getSubJobKey().equals("P10") ) {
						System.out.println(" Edge "+ e.toString());
					}
					if(Ijob.getSubJobKey().equals("P10") && origen.getSubJobKey().equals("P10")) {
						System.out.println(" Edge "+ e.toString());
					}
					
					
					if(origen.getSubJobKey().equals(Ijob.getSubJobKey())) {
						startCount=true;
						dist=0;
					}
					if(Ijob.getSubJobKey().equals("P1") && startCount) { // cuando el personal pasa idle time en el depot
						startCount=true;
						newStart=true;
						distRef=inp.getCarCost().getCost(Ijob.getId()-1, end.getId()-1);
						dist=0;
					}
					if(startCount) {
						dist+=inp.getCarCost().getCost(Ijob.getId()-1, Jjob.getId()-1);
					}

					if(Jjob.getSubJobKey().equals(end.getSubJobKey()) && startCount) {
						breakCycle=true;
						break;
					}
					if(breakCycle) {
						break;
					}
				}


				if(distRef>dist) {
					System.out.println(" Edge "+ e.toString());
				}
			}

			if(distRef<dist) {
				detour+=(dist-distRef);
				if(dist>distRef*test.getDetour()) {
					detourToPenalize=(dist-distRef*test.getDetour()); 
				}
			}
		}

		if(detour<0 || detourToPenalize<0) {
			System.out.println("detour<0 || detourToPenalize<0");
		}
		this.setDetour(detour);
		this.setdetourViolation(detourToPenalize);
		if(this.getAmountParamedic()>0) {
			this.setdetourPromParamedic(detour);
		}
		else {
			this.setdetourPromHomeCareStaff(detour);
		}

		System.out.println(" Finish ");
	}





	private Route selectionRoute(SubJobs present, Solution diversifiedSolneighborhood) {
		Route r=null;
		String key="";
		if(present==null){
			key="D32";
		}
		else {
			key=present.getSubJobKey();
		}
		for(Route routeInRoute:diversifiedSolneighborhood.getRoutes()) {
			HashMap<String, SubJobs> subJobsList= new HashMap<String, SubJobs>();
			if(routeInRoute.getPartsRoute().isEmpty()) {
				System.out.print("Stop");
			}
			for(Parts p:routeInRoute.getPartsRoute()) {
				if(p.getListSubJobs().isEmpty()) {
					System.out.print("Stop");
				}
				for(SubJobs j:p.getListSubJobs()) {
					subJobsList.put(j.getSubJobKey(), j);
				}
				if(subJobsList.containsKey(key)) {
					r=routeInRoute;
					break;
				}
			}
		}
		return r;
	}

	private Route selectionRoute1(SubJobs present, Solution diversifiedSolneighborhood) {
		Route r=null;
		String key="";
		if(present==null){
			key="P26";
		}
		else {
			key=present.getSubJobKey();
		}
		for(Route routeInRoute:diversifiedSolneighborhood.getRoutes()) {
			HashMap<String, SubJobs> subJobsList= new HashMap<String, SubJobs>();
			if(routeInRoute.getPartsRoute().isEmpty()) {
				System.out.print("Stop");
			}
			for(Parts p:routeInRoute.getPartsRoute()) {
				if(p.getListSubJobs().isEmpty()) {
					System.out.print("Stop");
				}
				for(SubJobs j:p.getListSubJobs()) {
					subJobsList.put(j.getSubJobKey(), j);
				}
				if(subJobsList.containsKey(key)) {
					r=routeInRoute;
					break;
				}
			}
		}
		return r;
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

		for(int i=1;i<sequence.size()-1;i++) {
			SubJobs origen=sequence.get(i-1);
			SubJobs end=sequence.get(i);
			Edge ef=new Edge(origen,end,inp,test);
			Edge e=null;
			if(this.getEdges().containsKey(ef.getEdgeKey())) {
				e=this.getEdges().get(ef.getEdgeKey());
			}

		}
		ArrayList<Edge> connectionsList= new ArrayList<>();
		for(int i=1;i<sequence.size()-1;i++) {
			SubJobs origen=sequence.get(i-1);
			SubJobs end=sequence.get(i);
			if(origen.getId()!=end.getId()) {
				Edge e= new Edge(origen,end, inp, test);
				connectionsList.add(e);
			}
		}

		for(Route route:initialSol.getRoutes()) {
			for(Edge e: route.getEdges().values()) { // is there a detour??
				SubJobs origen=e.getOrigin();
				SubJobs end=e.getEnd();
				if(origen.getId()==1 || end.getId()==1) {
					System.out.println("Stop");
				}
				if(origen.getSubJobKey().equals("P1") && end.getSubJobKey().equals("P72")) {
					System.out.println("Stop");
				}

				if(subJobsList.containsKey(origen.getSubJobKey()) && subJobsList.containsKey(end.getSubJobKey()) ) {
					double travelTime=0;
					boolean startCount=false;
					for(int i=1; i<sequence.size();i++) { // iterating over the rotue
						SubJobs r=sequence.get(i-1);
						SubJobs s=sequence.get(i);
						if(r.getSubJobKey().equals("P1") || s.getSubJobKey().equals("P58")) {
							System.out.println("Stop");
						}
						if(r.getSubJobKey().equals(origen.getSubJobKey())) {
							startCount=true;
							travelTime=0;
						}
						if(startCount) {
							travelTime+=inp.getCarCost().getCost(r.getId()-1, s.getId()-1);
						}
						if(s.getSubJobKey().equals(end.getSubJobKey())) {
							break;
						}
					}
					e.setTravelTimeInRoute(travelTime);
					System.out.println("Print edge in the vehicle shift "+e.toString());
					if(this.getEdges().containsKey(e.getEdgeKey())) {
						Edge edgeInRoute=this.getEdges().get(e.getEdgeKey());
						this.getEdges().get(e.getEdgeKey()).setTravelTimeInRoute(travelTime);
						System.out.println("Print edge in the vehicle route "+edgeInRoute.toString());
					}
					else {System.out.println("sTOP");

					}
					System.out.println(e.toString());
					if(e.getTime()> e.gettravelTimeInRoute()) {
						depotS="P1";
					}
				}
			}
		}
		System.out.println("Stop");
	}





	public void computeHomCareStaffCost(Test test, Inputs inp) {

		// distance
		double distance=0;
		for(Edge e:this.edges.values()) {
			if(e.gettravelTimeInRoute()==0) {
				double workingTime =0;
			}
			distance+=e.gettravelTimeInRoute();
		}
		// load time
		double factor=0;
		if(this.amountParamedics>0) {
			factor=test.getloadTimePatient();
		}
		else {
			factor=test.getloadTimeHomeCareStaff();
		}
		double loadUnloadTIme=factor*this.subJobsList.size();

		// time for registration and waiting time
		double additional =0;
		double waitingTime =0;
		for(SubJobs j:this.getSubJobsList()) {
			additional+=j.getloadUnloadRegistrationTime();
			waitingTime+=j.getWaitingTime();
		}
		double variableCost=0;
		if(this.getHomeCareStaff()>0) {
			variableCost=loadUnloadTIme;
		}

		this.setTravelTime(distance+loadUnloadTIme);
		this.setDurationRoute(distance+variableCost+waitingTime);
		if(this.amountParamedics>0) {
			travelTimeParamedic=distance+loadUnloadTIme;
		}
		else {
			travelTimeHHC=distance+loadUnloadTIme;
		}
		this.homeCareStaffCost=this.getDurationRoute();
	}


	public static Comparator<Route> SORT_BY_EarlyJob = new Comparator<Route>() { 

		@Override 

		public int compare(Route r1, Route r2) { 

			if (r1.getSubJobsList().get(0).getstartServiceTime() > r2.getSubJobsList().get(0).getstartServiceTime()) 

				return 1; 

			if (r1.getSubJobsList().get(0).getstartServiceTime() < r2.getSubJobsList().get(0).getstartServiceTime()) 

				return -1; 

			return 0; 

		} 

	}; 

	public static Comparator<Route> SORT_BY_departureTimeDepot = new Comparator<Route>() { 
		// earliest to latest
		@Override 

		public int compare(Route r1, Route r2) { 

			if (r1.getPartsRoute().get(0).getListSubJobs().get(0).getArrivalTime() > r2.getPartsRoute().get(0).getListSubJobs().get(0).getArrivalTime()) 

				return 1; 

			if (r1.getPartsRoute().get(0).getListSubJobs().get(0).getArrivalTime() < r2.getPartsRoute().get(0).getListSubJobs().get(0).getArrivalTime()) 

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
		this.getSubJobsList().clear();
		this.getJobsDirectory().clear();
		for(Parts p:this.getPartsRoute()) {
			for(SubJobs j:p.getListSubJobs()) {
				if(j.getId()!=1) {
					this.getSubJobsList().add(j);
					this.getJobsDirectory().put(j.getSubJobKey(), j);
				}
			}
		}
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





	public void checkingConnectionsRoute(Test test, Inputs inp) {
		// empty parts
		boolean removing=true;
		do {
			removing=false;
			for(Parts p:this.getPartsRoute()) {
				if(p.getListSubJobs().isEmpty()) {
					this.getPartsRoute().remove(p);
					removing=true;
					break;
				}
			}
		}
		while(removing);

		// connections
		ArrayList<SubJobs> jobsList= new ArrayList<SubJobs>(); 
		for(Parts p: this.getPartsRoute()) { // iterando por partes
			for(SubJobs sj:p.getListSubJobs()) {
				jobsList.add(sj);
			}
		}
		ArrayList<Edge> edgesList= new ArrayList<Edge>(); 
		double travelTimeDuration=0;
		for(int i=1;i<jobsList.size();i++) {
			SubJobs iNode=jobsList.get(i-1);
			SubJobs jNode=jobsList.get(i);
			Edge newEdge= new Edge(iNode,jNode,inp,test);
			edgesList.add(newEdge);
			travelTimeDuration=newEdge.getTime();
			newEdge.setTravelTimeInRoute(travelTimeDuration);
		}
		this.getEdges().clear();
		for(Edge e:edgesList) {
			this.getEdges().put(e.getEdgeKey(), e);
		}
	}





	public void computeDriverCost(Test test, Inputs inp) {
		double driverCost=0; // considera los tiempos que el conductor tiene que esperar para el cargue y descarge de personas
		double departureDepot=0;
		double arrivalDepot=0;
		for(Parts p:this.getPartsRoute()) {

			if(p.getListSubJobs().size()==1 && p.getListSubJobs().get(0).getSubJobKey().equals("P1")) { // part 1
				departureDepot= p.getListSubJobs().get(0).getArrivalTime();
			}
			if(p.getListSubJobs().size()==1 && p.getListSubJobs().get(0).getSubJobKey().equals("D1")) { // part 1
				arrivalDepot= p.getListSubJobs().get(0).getArrivalTime();
			}
			if(departureDepot!=arrivalDepot && departureDepot!=0 && arrivalDepot!=0) {
				driverCost+=(arrivalDepot-departureDepot);
				arrivalDepot= 0;
				departureDepot= 0;	
			}
		}
		this.setdriverCost(driverCost);
	}





	public void settingConnections(Solution solution, Test test, Inputs inp) {
		// setting las connexiones pick-drop 
		this.edges.clear();

		SubJobs originNode=this.getPartsRoute().get(0).getDirectorySubjobs().get("P1");
		int i=1;
		for(SubJobs j: this.subJobsList) {
			if(j.getSubJobKey().equals("P10")) {
				System.out.println("Sol");	
			}
			if(i>0) {
				Edge e= new Edge(originNode,j,inp,test);
				double distanceInRoute=computingDistance(e,inp,solution);
				if(distanceInRoute>10) {
					System.out.println("Sol");	
				}
				e.setTravelTimeInRoute(distanceInRoute);
				this.edges.put(e.getEdgeKey(), e);
			}
			i+=j.getTotalPeople();
			originNode=this.getJobsDirectory().get(j.getSubJobKey());
			if(originNode.getSubJobKey().equals("P7")) {
				System.out.println("Sol");	
			}
		}
		SubJobs endNode=this.getPartsRoute().get(this.getPartsRoute().size()-1).getDirectorySubjobs().get("D1");
		Edge e= new Edge(originNode,endNode,inp,test);
		this.edges.put(e.getEdgeKey(), e);
	}





	private double computingDistance(Edge e, Inputs inp, Solution solution) {
		double distance=0;
		SubJobs end=e.getEnd();
		Route r=selectionRoute(end,solution);
		ArrayList<SubJobs> jobsList= new ArrayList<SubJobs>();

		for(Parts p:r.getPartsRoute()) {
			for(SubJobs j:p.getListSubJobs()) {
				jobsList.add(j);
			}	
		}
		boolean count=false;
		for(int jindex=1;jindex<jobsList.size();jindex++) {
			SubJobs j=jobsList.get(jindex-1);
			SubJobs k=jobsList.get(jindex);
			if(j.getSubJobKey().equals("D5")) {
				System.out.println("Sol");
			}
			if(j.getSubJobKey().equals(e.getOrigin().getSubJobKey())) {
				if(j.getSubJobKey().equals("D61")) {
					System.out.println("Sol");
				}
				count=true;
				distance=0;
			}


			if(count) {

				distance+=inp.getCarCost().getCost(j.getId()-1, k.getId()-1);
			}
			if(distance>10) {
				System.out.println("Sol");	
			}
			if(k.getSubJobKey().equals(e.getEnd().getSubJobKey())) {
				break;
			}
		}

		return distance;
	}





	public void checkingDetour() {
		detour=0;
		for(Edge e:this.getEdges().values()) {
			if(e.gettravelTimeInRoute()>e.getTime()) {
				detour+=(e.gettravelTimeInRoute()-e.getTime());
				if(detour>15) {
					System.out.println("Stop");
				}
			}

		}
		this.setDetour(detour);
		if(this.amountParamedics>0) {
			this.setdetourPromParamedic(detour);
		}
		else {
			this.setdetourPromHomeCareStaff(detour);
		}
	}




}

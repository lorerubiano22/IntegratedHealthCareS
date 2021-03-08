import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;



public class Solution {
	private static long nInstances = 0; // number of instances
	private long id; // solution ID
	private LinkedList<Route> routes; // list of routes in this solution
	private int passengers=0;// home care staff and + paramedic transported que salen del depot
	private double durationSolution = 0.0; // Travel distance = waiting time + driving time
	private double waitingTime=0;// Total waiting time
	private double detourDuration=0;// Total detour time
	private double detourPromParamedico=0;// Total detour prom paramedic
	private double detourPromHomeCareStaff=0;// Total detour prom home Care Staff
	private double traveltimeParamedico=0;//// travel time paramedic
	private double traveltimeHomeCareStaff=0;// travel time  home Care Staff
	private double serviceTime=0;
	private double drivingTime=0;
	private double walkingTime=0;
	private double idleTimeSol=0;  
	private double paramedic=0;// los paramedicos que salen del depot
	private double homeCareStaff=0;// los paramedicos que salen del depot
	private double driverCost=0;// los paramedicos que salen del depot
	private double homeCareStaffCost=0;// los paramedicos que salen del depot
	private double travelTimeHHC=0;// los paramedicos que salen del depot
	private double additionalWaitingTime=0; // 
	private double timeWindowViolation=0;
	private double detourViolation=0;
	private double penalization=0;
	private double objectiveFunction=0;
	private Solution shifts;
	private LinkedList<SubRoute> walkingList= new LinkedList<SubRoute>();

	public Solution(Solution initialSol) {

		id=initialSol.id; // solution ID
		durationSolution = initialSol.durationSolution; // Travel distance = waiting time + driving time
		routes = new LinkedList<Route>();
		routes = copyRoutes(initialSol); // list of routes in this solution
		detourDuration=initialSol.getdetourDuration();
		detourPromParamedico=initialSol.detourPromParamedico;// Total detour prom paramedic
		detourPromHomeCareStaff=initialSol.detourPromHomeCareStaff;// Total detour prom home Care Staff
		idleTimeSol=initialSol.idleTimeSol;
		traveltimeParamedico=initialSol.traveltimeParamedico;// travel time paramedic
		traveltimeHomeCareStaff=initialSol.traveltimeHomeCareStaff;// travel time  home Care Staff
		passengers=initialSol.passengers;// home care staff and + paramedic transported que salen del depot
		waitingTime=initialSol.waitingTime;// Total waiting time
		driverCost=initialSol.driverCost;// los paramedicos que salen del depot
		homeCareStaffCost=initialSol.homeCareStaffCost;// los paramedicos que salen del depot
		travelTimeHHC=initialSol.travelTimeHHC;
		serviceTime=initialSol.serviceTime;
		drivingTime=initialSol.drivingTime;
		walkingTime=initialSol.walkingTime;
		paramedic=initialSol.paramedic;// los paramedicos que salen del depot
		homeCareStaff=initialSol.homeCareStaff;// los paramedicos que salen del depot
		additionalWaitingTime=initialSol.additionalWaitingTime;
		timeWindowViolation=initialSol.timeWindowViolation;
		detourViolation=initialSol.detourViolation;
		penalization=initialSol.penalization;
		objectiveFunction=initialSol.objectiveFunction;
		shifts=initialSol.shifts;
		walkingList=initialSol.walkingList;
	}


	public Solution() {
		nInstances++;
		id = nInstances;
		routes = new LinkedList<Route>();
	}


	// Setters 
	public void setId(long id) {this.id = id;}
	public void setDurationSolution(double costs) {this.durationSolution = costs;}
	public void setRoutes(LinkedList<Route> routes) {this.routes = routes;}
	public void setObjectiveFunction(double of) {this.objectiveFunction = of;}
	public void setPassengers(int p) {this.passengers = p;}
	public void setShift(Solution sol) {
		this.shifts = sol;
		// set qualification level
		for(Route r:shifts.getRoutes()) {
			int qualification=-1;
			for(SubJobs j:r.getSubJobsList()) {
				if(j.getReqQualification()>qualification) {
					qualification=j.getReqQualification();
				}
			}
			r.setQualificationLevel(qualification);
		}
	}

	public void setParamedic(double w) {paramedic=w;}
	public void setHomeCareStaff(double w) {homeCareStaff=w;}
	public void setWaitingTime(double w) {waitingTime=w;}
	public void setServiceTime(double s) {serviceTime=s;}
	public void setdrivingTime(double s) {drivingTime=s;}
	public void setWalkingTime(double w) {walkingTime=w;}
	public void setIdleTime(double idleTime) {idleTimeSol=	idleTime;}
	public void setdriverCost(double dr) {driverCost=	dr;}
	public void sethomeCareStaffCost(double dr) {homeCareStaffCost=	dr;}
	public void sethomeCareStaffTravelTime(double dr) {travelTimeHHC=	dr;}

	public void setwAdditionalWaitingTime(double dr) {additionalWaitingTime=	dr;}
	public void settimeWindowViolation(double dr) {timeWindowViolation=	dr;}
	public void setdetourViolation(double detour) {detourViolation= detour;}
	public void setdetourDuration(double detour) {detourDuration= detour;}
	public void setdetourPromParamedics(double detour) {detourPromParamedico= detour;}
	public void setdetourPromHomeCareStaff(double detour) {detourPromHomeCareStaff= detour;}
	public void setWalkingRoutes(LinkedList<SubRoute> walkingList) {
		for(SubRoute sr: walkingList) {
			this.walkingList.add(new SubRoute(sr));
		}

	}





	// Getters
	public long getId() { return id;}
	public LinkedList<SubRoute> getWalkingRoute() { return walkingList;}
	public double getDurationSolution() { return durationSolution;}
	public int getPassengers() { return passengers;}
	public LinkedList<Route> getRoutes() {return routes;}
	public double getWaitingTime(){return waitingTime; }
	public double getServiceTime() {return serviceTime;}
	public double getdrivingTime(){return drivingTime;}
	public double getWalkingTime() {return walkingTime;}
	public double getdriverCost() {return driverCost;}
	public double gethomeCareStaffCost() {return homeCareStaffCost;}
	public double gethomeCareStaffTravelTime() {return travelTimeHHC;}
	public double geAdditionalWaitingTime() {return additionalWaitingTime;}
	public double getimeWindowViolation() {return timeWindowViolation;}
	public double getdetourViolation() {	return detourViolation;}
	public double getobjectiveFunction() {	return objectiveFunction;}
	public double getdetourDuration() {	return Math.ceil(detourDuration);}
	public Solution getShift() {return shifts;}



	// auxiliar methods

	private LinkedList<Route> copyRoutes(Solution initialSol) {
		LinkedList<Route> copyRoutes= new LinkedList<Route>();
		// copy Routes
		for(Route r:initialSol.getRoutes()) {
			copyRoutes.add(new Route(r));
		}
		return copyRoutes;
	}

	@Override
	public String toString() 
	{   String s = "";
	s = s.concat("\nID Solution: " + id);
	//s = s.concat("\nFO: Duration: " + durationSolution);
	s = s.concat("\nFO: Travel time: " + drivingTime);
	//s = s.concat("\nFO: Service time: " + serviceTime);
	s = s.concat("\nWalking time: " + walkingTime);
	s = s.concat("\n Waiting time: " + waitingTime);
	s = s.concat("\n Amount home care staff: " + homeCareStaff);
	s = s.concat("\n Amount paramedic: " + paramedic);
	//s= s.concat("\n medical staff cost: "+ homeCareStaffCost);
	s= s.concat("\n driver cost: "+ driverCost);
	s= s.concat("\n home care staff and paramedic cost: "+ homeCareStaffCost);
	s= s.concat("\n home care staff and paramedic travel time: "+ gethomeCareStaffTravelTime());
	s= s.concat("\n home care staff  travel time: "+ traveltimeHomeCareStaff);
	s= s.concat("\n paramedic travel time: "+ traveltimeParamedico);
	s= s.concat("\n detour: "+ detourDuration);
	s= s.concat("\n detour average paramedic: "+ Math.ceil(detourPromParamedico));
	s= s.concat("\n detour average home care staff: "+ Math.ceil(detourPromHomeCareStaff));
	s= s.concat("\n time window violation: "+ timeWindowViolation);
	s= s.concat("\n waiting Time to penalize: "+ additionalWaitingTime);
	s= s.concat(" Detour time to penalize: "+ detourViolation);
	s = s.concat("\n List of jobs: ");
	for(Route r:routes) {
		if(!r.getSubJobsList().isEmpty()) {
			s= s.concat("\n Route: "+ r.getIdRoute());
			s= s.concat(" travelTime: "+ r.getTravelTime());
			s= s.concat(" waitingTime: "+ r.getWaitingTime());
			s= s.concat(" serviceTime: "+ r.getServiceTime());
			s= s.concat(" detour: "+ r.getDetour());
			s= s.concat(" detour to penalize: "+ r.getdetourViolation());
			s= s.concat(" waiting Time to penalize: "+ r.getAdditionalwaitingTime());
			s= s.concat(" durationRoute: "+ r.getDurationRoute());
			s= s.concat("\n medical staff cost: "+ r.gethomeCareStaffCost());
			s= s.concat("\n driver cost: "+ r.getdriverCost());
			s= s.concat("\n");
			for(Parts p:r.getPartsRoute()) {
				for(SubJobs j:p.getListSubJobs()) {
					String type="";
					if(j.isClient()) {
						type="c";
					}
					if(j.isPatient()) {
						type="p";
					}
					s = s.concat(" ( " + j.getSubJobKey()+type+" A  "+(int)j.getArrivalTime()+"  B  "+(int)j.getstartServiceTime()+ " end service "+ (int)j.getendServiceTime()+"   D  "+(int)j.getDepartureTime()+"  reqTime_"+j.getReqTime()+"  TW ["+(int)j.getSoftStartTime()+";"+(int)j.getSoftEndTime()+"]"+") \n");
				}
				s= s.concat("\n");
			}
		}	
	}
	return s;
	}


	public void checkingSolution(Inputs inp, Test test, HashMap<Integer, SubRoute> jobsInWalkingRoute, Solution initialSol) {

		//System.out.println("Shift");
		//System.out.println(initialSol.toString());
		int id=-1;



		for(Route r: this.getRoutes()) {
			r.checkingConnectionsRoute(test,inp);
			//
			id++;
			r.setIdRoute(id);
			r.setDurationRoute(Math.abs(r.getPartsRoute().get(0).getListSubJobs().get(0).getArrivalTime()-r.getPartsRoute().get(r.getPartsRoute().size()-1).getListSubJobs().get(0).getArrivalTime()));
			r.computeServiceTime(inp,jobsInWalkingRoute);

			r.checkingTimeWindows(test,inp); // revisar las ventanas de tiempo si se pueden mover
			// revisar los tiempos de espera
			r.computeDriverCost(test,inp);
			r.computeTravelTime(inp); // calcula las distancias entre puntos

			//System.out.println(this.toString());
			r.updatingJobsList();
			r.totalMedicalStaff();
		}
		// computing detour
		//		for(Route r: initialSol.getRoutes()) {
		//			r.settingConnections(this,test,inp);
		//			r.checkingWaitingTimes(test,inp);
		//			r.checkingDetour(test,inp,this);	
		//			r.computeHomCareStaffCost(test,inp);
		//		}
		this.computeCosts( inp,  test,initialSol,jobsInWalkingRoute);
	}

	private void checkingConnectionsRoute(Solution initialSol, Inputs inp, Test test) {
		// el objetivo es cargar la lista de conexiones a la nueva solución
		// lista de los trabajos contenidos en cada ruta
		//		for(Route r:this.getRoutes()) {
		//			r.getEdges().clear();
		//		}
		// formar las connexiones
		//System.out.println(initialSol.toString());
		double travelTime=0;
		for(Route r:this.getRoutes()) {
			for(Edge e:r.getEdges().values()) {
				travelTime+=e.getTime();
			}	
		}
		//System.out.println("Stop "+travelTime);
	}


	private void transferingInformation(Route r, Route shift) {
		if(r!=null) {
			HashMap<String, SubJobs> subJobsList= new HashMap<String, SubJobs>();
			for(Parts p:r.getPartsRoute()) {
				for(SubJobs j:p.getListSubJobs()) {
					subJobsList.put(j.getSubJobKey(), j);
				}	
			}
			for(Parts p:r.getPartsRoute()) {
				for(SubJobs j:p.getListSubJobs()) {
					if(subJobsList.containsKey(j.getSubJobKey())) {
						SubJobs jobsInRoute=subJobsList.get(j.getSubJobKey());
						j=new SubJobs(jobsInRoute);}
				}	
			}

		}

	}
















	public void computeCosts(Inputs inp, Test test, Solution initialSol, HashMap<Integer, SubRoute> jobsInWalkingRoute) {
		double durationSolution = 0.0; // Travel distance = waiting time + driving time
		double waitingTime=0;// Total waiting time
		double serviceTime=0;
		double drivingTime=0;
		double paramedic=0;// los paramedicos que salen del depot
		double homeCareStaff=0;// los paramedicos que salen del depot
		double driverCost=0;// los paramedicos que salen del depot

		// infeasible solutions
		double additionalWaitingTime=0; // 
		double timeWindowViolation=0;
		double travelTimeDriverCost=0;
		for(Route r:this.getRoutes()) {

			serviceTime+=r.getServiceTime();
			drivingTime+=r.getTravelTime();
			timeWindowViolation+=r.gettimeWindowViolation();
			durationSolution+=r.getDurationRoute();
			travelTimeDriverCost+=r.getdriverCost();
		}
		// cost for home care staff

		double travelTime=0;
		double drivingTimeMedicalStaff=0;
		double detourParamedic=0;
		double detourhhc=0;
		double detourViolation=0;
		double detour=0;
		double tvParamedic=0;
		double tvMedicalStaff=0;
		double load=0;
		for(Route r:initialSol.getRoutes()) {
			//System.out.println("Total paramedics" + r.getAmountParamedic());
			travelTime+=r.getTravelTime();
			paramedic+=r.getAmountParamedic();
			homeCareStaff+=r.getHomeCareStaff();
			drivingTimeMedicalStaff+=r.gethomeCareStaffCost();
			waitingTime+=r.getWaitingTime();
			additionalWaitingTime+=r.getAdditionalwaitingTime();
			if(r.getAmountParamedic()>0) {
				detourParamedic+=r.getdetourPromParamedic();
			}
			else {
				detourhhc+=r.getdetourPromHomeCareStaff();
			}
			detour+=r.getDetour();
			detourViolation+=r.getdetourViolation();
			if(r.getAmountParamedic()>0) {
				load+=(test.getloadTimePatient()*r.getSubJobsList().size());
				tvParamedic+=r.getTravelTime();
			}
			else {
				tvMedicalStaff+=r.getTravelTime();
			}
		}
		double check=drivingTimeMedicalStaff-waitingTime+load;
		traveltimeParamedico=tvParamedic;// travel time paramedic
		traveltimeHomeCareStaff=tvMedicalStaff;// travel time  home Care Staff



		initialSol.setdrivingTime(travelTime);
		// calcula 
		//double travelTimeDriverCost=0;
		//for(Route r:this.getRoutes()) {
		//	for(Edge e:r.getEdges().values()) {
		//		travelTimeDriverCost+=e.getTime();
		//	}
		//}

		//System.out.println("Total travel time" + travelTimeDriverCost);
		this.sethomeCareStaffTravelTime(initialSol.getdrivingTime());
		this.setdetourPromHomeCareStaff(detourhhc/homeCareStaff);
		this.setdetourPromParamedics(detourParamedic/paramedic);
		this.setDurationSolution(durationSolution);
		this.setWaitingTime(Math.abs(waitingTime));
		this.setServiceTime(Math.abs(serviceTime));
		this.setdrivingTime(Math.abs(travelTimeDriverCost));
		this.setParamedic(paramedic);
		this.setHomeCareStaff(homeCareStaff);
		this.setwAdditionalWaitingTime(Math.abs(additionalWaitingTime));
		this.setdetourViolation(Math.abs(detourViolation));
		this.settimeWindowViolation(Math.abs(timeWindowViolation));
		this.setdetourDuration(Math.abs(detour));
		this.setdetourViolation(detourViolation);
		//this.setWalkingTime(jobsInWalkingRoute.);
		double distance=0;
		for(SubRoute r:jobsInWalkingRoute.values()) {
			distance+=r.getTotalTravelTime();
		}
		this.setWalkingTime(distance);

		int additionalVehicles=0;
		if(inp.getVehicles().get(0).getQuantity()<this.getRoutes().size()) {
			additionalVehicles=this.getRoutes().size()-inp.getVehicles().get(0).getQuantity();
		}


		double penalization=additionalWaitingTime+detourViolation+timeWindowViolation+50*(additionalVehicles);
		//double penalization=additionalWaitingTime+detourViolation+timeWindowViolation;
		// cost <- driver : driving cost  // home care staff and paramedic <- driving cost + waiting time
		driverCost=this.getdrivingTime();// los paramedicos que salen del depot
		this.setdriverCost(driverCost);


		// computing costo for medical staff paramedic and home care staff


		this.sethomeCareStaffCost(drivingTimeMedicalStaff);

		if(test.gethomeCareStaffObjective()==1 && test.getdriverObjective()==0) {
			objectiveFunction=this.driverCost+this.homeCareStaffCost+penalization;
			//objectiveFunction=this.driverCost+this.homeCareStaffCost+penalization;
			//objectiveFunction=this.homeCareStaffCost + this.waitingTime + this.detourDuration;
		}
		else {
			if(test.gethomeCareStaffObjective()==0 && test.getdriverObjective()==1) {
				objectiveFunction=this.driverCost+50*(additionalVehicles);
				//objectiveFunction=this.driverCost+this.homeCareStaffCost+penalization;
			}
			else {
				objectiveFunction=this.driverCost+this.homeCareStaffCost + this.waitingTime + this.detourDuration+penalization;
			}
		}

	}






}


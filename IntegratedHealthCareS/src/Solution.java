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
	private double serviceTime=0;
	private double drivingTime=0;
	private double walkingTime=0;
	private double idleTimeSol=0;  
	private double paramedic=0;// los paramedicos que salen del depot
	private double homeCareStaff=0;// los paramedicos que salen del depot
	private double driverCost=0;// los paramedicos que salen del depot
	private double homeCareStaffCost=0;// los paramedicos que salen del depot
	private double additionalWaitingTime=0; // 
	private double timeWindowViolation=0;
	private double detourViolation=0;
	private double penalization=0;
	private double objectiveFunction=0;
	private Solution shifts;

	public Solution(Solution initialSol) {
		id=initialSol.id; // solution ID
		durationSolution = initialSol.durationSolution; // Travel distance = waiting time + driving time
		routes = new LinkedList<Route>();
		routes = copyRoutes(initialSol); // list of routes in this solution
		detourDuration=initialSol.getdetourDuration();
		detourPromParamedico=initialSol.detourPromParamedico;// Total detour prom paramedic
		detourPromHomeCareStaff=initialSol.detourPromHomeCareStaff;// Total detour prom home Care Staff
		idleTimeSol=initialSol.idleTimeSol;
		passengers=initialSol.passengers;// home care staff and + paramedic transported que salen del depot
		waitingTime=initialSol.waitingTime;// Total waiting time
		driverCost=initialSol.driverCost;// los paramedicos que salen del depot
		homeCareStaffCost=initialSol.homeCareStaffCost;// los paramedicos que salen del depot
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
	public void setShift(Solution sol) {this.shifts = sol;}
	
	public void setParamedic(double w) {paramedic=w;}
	public void setHomeCareStaff(double w) {homeCareStaff=w;}
	public void setWaitingTime(double w) {waitingTime=w;}
	public void setServiceTime(double s) {serviceTime=s;}
	public void setdrivingTime(double s) {drivingTime=s;}
	public void setWalkingTime(double w) {walkingTime=w;}
	public void setIdleTime(double idleTime) {idleTimeSol=	idleTime;}
	public void setdriverCost(double dr) {driverCost=	dr;}
	public void sethomeCareStaffCost(double dr) {homeCareStaffCost=	dr;}
	public void setwAdditionalWaitingTime(double dr) {additionalWaitingTime=	dr;}
	public void settimeWindowViolation(double dr) {timeWindowViolation=	dr;}
	public void setdetourViolation(double detour) {detourViolation= detour;}
	public void setdetourDuration(double detour) {detourDuration= detour;}
	public void setdetourPromParamedics(double detour) {detourPromParamedico= detour;}
	public void setdetourPromHomeCareStaff(double detour) {detourPromHomeCareStaff= detour;}



	// Getters
	public long getId() { return id;}
	public double getDurationSolution() { return durationSolution;}
	public int getPassengers() { return passengers;}
	public LinkedList<Route> getRoutes() {return routes;}
	public double getWaitingTime(){return waitingTime; }
	public double getServiceTime() {return serviceTime;}
	public double getdrivingTime(){return drivingTime;}
	public double getWalkingTime() {return walkingTime;}
	public double getdriverCost() {return driverCost;}
	public double gethomeCareStaffCost() {return homeCareStaffCost;}
	public double geAdditionalWaitingTime() {return additionalWaitingTime;}
	public double getimeWindowViolation() {return timeWindowViolation;}
	public double getdetourViolation() {	return detourViolation;}
	public double getobjectiveFunction() {	return objectiveFunction;}
	public double getdetourDuration() {	return detourDuration;}
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
	s = s.concat("\nFO: Duration: " + durationSolution);
	s = s.concat("\nFO: Travel time: " + drivingTime);
	//s = s.concat("\nFO: Service time: " + serviceTime);
	s = s.concat("\nWalking time: " + walkingTime);
	s = s.concat("\n Waiting time: " + waitingTime);
	s = s.concat("\n Amount home care staff: " + homeCareStaff);
	s = s.concat("\n Amount paramedic: " + paramedic);
	//s= s.concat("\n medical staff cost: "+ homeCareStaffCost);
	s= s.concat("\n driver cost: "+ driverCost);
	s= s.concat("\n home care staff and paramedic cost: "+ homeCareStaffCost);
	s= s.concat("\n detour: "+ detourDuration);
	s= s.concat("\n detour prom paramedic: "+ detourPromParamedico);
	s= s.concat("\n detour prom home care staff: "+ detourPromHomeCareStaff);
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
					s = s.concat(" ( " + j.getSubJobKey()+type+" A  "+(int)j.getArrivalTime()+"  B  "+(int)j.getstartServiceTime()+ " end service "+ (int)j.getendServiceTime()+"   D  "+(int)j.getDepartureTime()+"  reqTime_"+j.getReqTime()+"  TW ["+(int)j.getStartTime()+";"+(int)j.getEndTime()+"]"+") \n");
				}
				s = s.concat("\n\n");
			}
		}	
	}
	return s;
	}


	public void checkingSolution(Inputs inp, Test test, HashMap<Integer, SubRoute> jobsInWalkingRoute, Solution initialSol) {
		//slackMethod(inp,test);
		System.out.println("Shift");
		System.out.println(initialSol.toString());
		int id=-1;
		checkingConnectionsRoute(initialSol,inp,test);
		for(Route r: this.getRoutes()) {
			//r.getEdges().clear();
			// checking
			for(Parts p: r.getPartsRoute()) {
				for(SubJobs sj:p.getListSubJobs()) {
					if(sj.getId()!=1) {
						if(!r.getJobsDirectory().containsKey(sj.getSubJobKey())) {
							System.out.println("Stop");
						}}
				}}
			//
			id++;
		//	computeStartTimeRoute(r.getPartsRoute().get(1).getListSubJobs().get(0),r,inp,test);
			//computeEndTimeRoute(r.getPartsRoute().get(r.getPartsRoute().size()-2).getListSubJobs().get(r.getPartsRoute().get(r.getPartsRoute().size()-2).getListSubJobs().size()-1),r,inp,test);
			r.setIdRoute(id);
			r.setDurationRoute(Math.abs(r.getPartsRoute().get(0).getListSubJobs().get(0).getArrivalTime()-r.getPartsRoute().get(r.getPartsRoute().size()-1).getListSubJobs().get(0).getArrivalTime()));
			//r.setDurationRoute(r.getSubJobsList().getLast().getDepartureTime()-r.getSubJobsList().getFirst().getArrivalTime());
			r.computeServiceTime(inp,jobsInWalkingRoute);
			// revisar las ventanas de tiempo si se pueden mover
			r.checkingTimeWindows(test,inp);
			// revisar los tiempos de espera
			r.checkingWaitingTimes(test,inp);
			// revisar los detours
			r.checkingDetour(test,inp,initialSol);	
			// metrics Home- care staff cost
			r.computeHomCareStaffCost(initialSol);
			r.computeTravelTime(inp);
			System.out.println(this.toString());
			r.updatingJobsList();
			r.totalMedicalStaff();
		}
		this.computeCosts( inp,  test,initialSol,jobsInWalkingRoute);

	}

	private void checkingConnectionsRoute(Solution initialSol, Inputs inp, Test test) {
		// el objetivo es cargar la lista de conexiones a la nueva solución
		// lista de los trabajos contenidos en cada ruta
//		for(Route r:this.getRoutes()) {
//			r.getEdges().clear();
//		}
		// formar las connexiones
System.out.println(initialSol.toString());
		for(Route shift:initialSol.getRoutes()) {
			System.out.println(shift.toString());
			Route r=null;
			for(Edge e: shift.getEdges().values()) { 
				SubJobs origenNode=null;
				SubJobs endNode=null;
				SubJobs origen=e.getOrigin();
				SubJobs end=e.getEnd();
				String key=origen.getSubJobKey()+end.getSubJobKey();
				
				
				r= selectionRoute(origen,end); // it could by false because the shift of a person could be splitted in more than 1 vehicle
				if(r!=null) {
					Edge edgeFromRoute= r.getEdges().get(key);// to remove
					ArrayList<SubJobs> sequence= new ArrayList<SubJobs>();
					for(Parts p: r.getPartsRoute()) {
						for(SubJobs j: p.getListSubJobs()) {
							sequence.add(j);
						}
					}
					for(int i=1;i<sequence.size();i++) {
						SubJobs a=sequence.get(i-1);
						SubJobs b=sequence.get(i);
						double travelTime=0;
						if(a.getSubJobKey().equals(origen.getSubJobKey())) {
							origenNode=a;
							travelTime=0;
						}
						if(b.getSubJobKey().equals(end.getSubJobKey())) {
							endNode=b;
							travelTime+=inp.getCarCost().getCost(a.getId()-1, b.getId()-1);
						}
						

						if(origenNode!= null && endNode!= null ) {
							if(travelTime==0) {
								System.out.println("Stop");
							}
							e= new Edge(origenNode,endNode,inp,test);
							e.setTravelTimeInRoute(travelTime);
							 origen=origenNode;
							end=endNode;
							r.getEdges().put(e.getEdgeKey(), e);
							break;
						}
System.out.println("check ");// to remove);
					}
				}
			}
			//transferingInformation(r, shift);
			System.out.println(shift.toString());
		}

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


	private void transferingInformation(Route shift) {
		// TODO Auto-generated method stub
		
	}


	private Route selectionRoute(SubJobs origen, SubJobs end) {
		Route r= null;
		for(Route routeInRoute:this.getRoutes()) {
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
				if(subJobsList.containsKey(origen.getSubJobKey()) && subJobsList.containsKey(end.getSubJobKey())) {
					r=routeInRoute;
					break;
				}
			}
		}
		return r;
	}


	private void slackMethod(Inputs inp, Test test) {
		//		for(Route r:this.getRoutes()) {
		//			for(int i=1;i<r.getSubJobsList().size();i++) {
		//				SubJobs nodeI=r.getSubJobsList().get(i-1);
		//				SubJobs nodeJ=r.getSubJobsList().get(i);
		//			
		//				double tv=inp.getCarCost().getCost(nodeI.getId()-1, nodeJ.getId()-1);
		//				double possibleArrival=nodeI.getDepartureTime()+tv;
		//				double possibleStartServiceTime= Math.max(possibleArrival+nodeJ.getdeltaArrivalStartServiceTime(), nodeJ.getStartTime());
		//				double possibleEndServiceTime= possibleStartServiceTime+nodeJ.getdeltarStartServiceTimeEndServiceTime();
		//				double possibleDepartureTime=possibleArrival+nodeJ.getdeltaArrivalDeparture();
		//			if(possibleStartServiceTime!=nodeJ.getstartServiceTime() && nodeJ.getstartServiceTime()>nodeJ.getEndTime()) {
		//				System.out.println(nodeI.toString());
		//			}
		//			nodeJ.setarrivalTime(possibleArrival);
		//			nodeJ.setStartServiceTime(possibleStartServiceTime);
		//			nodeJ.setEndServiceTime(possibleEndServiceTime);
		//			nodeJ.setdepartureTime(possibleDepartureTime);
		//			}
		//		}
		//		System.out.println(this.toString());
		for(Route r:this.getRoutes()) {
			if(r.getSubJobsList().size()>2) {
				for(int i= r.getSubJobsList().size()-1;i>0;i--) {
					SubJobs ref=r.getSubJobsList().get(i);// following job
					SubJobs changing=r.getSubJobsList().get(i-1);// following job
					if(changing.getId()==21) {
						System.out.println("Stop");
					}
					double arrivalTimeRef= ref.getArrivalTime();
					double tvRefChanging=inp.getCarCost().getCost(changing.getId()-1, ref.getId()-1);
					double possibleDeparture=arrivalTimeRef-tvRefChanging;
					double possibleArrivalTime=possibleDeparture-changing.getdeltaArrivalDeparture();
					double possibleStartService=possibleArrivalTime-changing.getdeltaArrivalStartServiceTime();
					double startService=changing.getstartServiceTime();
					if(changing.getstartServiceTime()>=changing.getStartTime() && changing.getendServiceTime()<=changing.getEndTime()) {
						if(possibleStartService>=changing.getStartTime() && possibleStartService<=changing.getEndTime()) {
							startService= Math.max(changing.getstartServiceTime(), possibleStartService);
						}
					}
					else {
						if(changing.getstartServiceTime()>=changing.getEndTime()) {
							startService= Math.min(changing.getstartServiceTime(), possibleStartService);
						}
						else {
							if(changing.getstartServiceTime()<changing.getStartTime()) {
								startService= Math.max(changing.getstartServiceTime(), possibleStartService);
							}
						}
					}
					double arrivalTime=startService-changing.getdeltaArrivalStartServiceTime();
					double departureTime=arrivalTime+changing.getdeltaArrivalDeparture();
					double endServiceTime=startService+changing.getdeltarStartServiceTimeEndServiceTime();
					changing.setStartServiceTime(startService);
					changing.setarrivalTime(arrivalTime);
					changing.setdepartureTime(departureTime);
					changing.setEndServiceTime(endServiceTime);
				}
			}
			System.out.println(r.toString());
			System.out.println("close");
		}

	}


	private void computeEndTimeRoute(SubJobs lastJob, Route r, Inputs inp, Test test) {
		// 1. Compute travel time
		SubJobs depot=r.getPartsRoute().get(r.getPartsRoute().size()-1).getListSubJobs().get(0);
		double tv=inp.getCarCost().getCost(lastJob.getId()-1, depot.getId()-1);
		double arrivalTime=	lastJob.getDepartureTime()+tv;
		depot.setarrivalTime(arrivalTime);
		depot.setdepartureTime(arrivalTime);
		depot.setdepartureTime(arrivalTime);
		depot.setStartServiceTime(arrivalTime);
		depot.setEndServiceTime(arrivalTime);
		depot.setserviceTime(0);
		System.out.println(r.toString());
	}
	private void computeStartTimeRoute(SubJobs firstJob, Route r, Inputs inp, Test test) {
		// 1. Compute travel time
		SubJobs depot=r.getPartsRoute().get(0).getListSubJobs().get(0);
		double tv=inp.getCarCost().getCost(depot.getId()-1,firstJob.getId()-1);
		double arrivalTime=	firstJob.getArrivalTime()-tv-test.getloadTimeHomeCareStaff();
		depot.setarrivalTime(arrivalTime);
		depot.setdepartureTime(arrivalTime);
		depot.setStartServiceTime(arrivalTime);
		depot.setEndServiceTime(arrivalTime);
		depot.setserviceTime(0);
		System.out.println(r.toString());
	}






	public void computeCosts(Inputs inp, Test test, Solution initialSol, HashMap<Integer, SubRoute> jobsInWalkingRoute) {
		double durationSolution = 0.0; // Travel distance = waiting time + driving time
		double waitingTime=0;// Total waiting time
		double serviceTime=0;
		double drivingTime=0;
		double paramedic=0;// los paramedicos que salen del depot
		double homeCareStaff=0;// los paramedicos que salen del depot
		double driverCost=0;// los paramedicos que salen del depot
		double homeCareStaffCost=0;// los paramedicos que salen del depot
		// infeasible solutions
		double additionalWaitingTime=0; // 
		double timeWindowViolation=0;
		double detourViolation=0;
		double detour=0;
		double detourParamedic=0;
		double detourHomeCareStaff=0;
		for(Route r:this.getRoutes()) {
			waitingTime+=r.getWaitingTime();
			serviceTime+=r.getServiceTime();
			additionalWaitingTime+=r.getAdditionalwaitingTime();
			timeWindowViolation+=r.gettimeWindowViolation();
			detour+=r.getDetour();
			detourParamedic+=r.getdetourPromParamedic();
			detourHomeCareStaff+=r.getdetourPromHomeCareStaff();
			detourViolation+=r.getdetourViolation();
			durationSolution+=r.getDurationRoute();
		}
		double drivingTimeMedicalStaff=0;
		for(Route r:initialSol.getRoutes()) {
			System.out.println("Total paramedics" + r.getAmountParamedic());
			paramedic+=r.getAmountParamedic();
			homeCareStaff+=r.getHomeCareStaff();
			for(Edge e:r.getEdges().values()) {
				drivingTimeMedicalStaff+=e.getTime();}
		}

		double travelTimeDriverCost=0;
		for(Route r: this.getRoutes()) {
			drivingTime=0;
			for(Edge e: r.getEdges().values()) {
				drivingTime+=e.gettravelTimeInRoute();	
			}
			r.setTravelTime(drivingTime);
			travelTimeDriverCost+=drivingTime;
		}

		this.setdetourPromHomeCareStaff(detourHomeCareStaff/homeCareStaff);
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
		//this.setWalkingTime(jobsInWalkingRoute.);
		double distance=0;
		for(SubRoute r:jobsInWalkingRoute.values()) {
			distance+=r.getTotalTravelTime();
		}
			this.setWalkingTime(distance);



		double penalization=additionalWaitingTime+detourViolation+timeWindowViolation+1000*this.getRoutes().size();
		//double penalization=additionalWaitingTime+detourViolation+timeWindowViolation;
		// cost <- driver : driving cost  // home care staff and paramedic <- driving cost + waiting time
		driverCost=this.getdrivingTime();// los paramedicos que salen del depot
		this.setdriverCost(driverCost);


		// computing costo for medical staff paramedic and home care staff

		homeCareStaffCost=drivingTimeMedicalStaff+this.waitingTime;// los paramedicos que salen del depot
		this.sethomeCareStaffCost(homeCareStaffCost);

		if(test.gethomeCareStaffObjective()==1) {
			objectiveFunction=this.homeCareStaffCost+penalization;
		}
		else {
			objectiveFunction=this.driverCost+penalization;
		}

	}





}


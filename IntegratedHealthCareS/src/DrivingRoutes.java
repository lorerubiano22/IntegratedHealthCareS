import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

public class DrivingRoutes {

	private Inputs inp; // input problem
	private Test test; // input problem
	private Random rn;
	private  ArrayList<Route> routeList= new ArrayList<Route>();
	private  HashMap<Integer, Jobs> subJobs= new HashMap<>();
	private  ArrayList<Couple> subJobsList= new ArrayList<Couple>();
	private  ArrayList<Couple> subJobsHighestQualification= new ArrayList<Couple>();
	private  ArrayList<Couple> subJobsMediumQualification= new ArrayList<Couple>();
	private  ArrayList<Couple> subJobsLowestQualification= new ArrayList<Couple>();
	private  ArrayList<Couple> subJobspatients= new ArrayList<Couple>();
	private HashMap<Integer, Jobs>assignedJobs=new HashMap<Integer, Jobs>();
	private HashMap<Integer, Jobs>jobsVehicle=new HashMap<Integer, Jobs>();
	ArrayList<ArrayList<Jobs>> schift= new ArrayList<>();


	public DrivingRoutes(Inputs i, Random r, Test t, ArrayList<Couple> subJobsList) {
		inp=i;
		test=t;
		rn=r;
		this.subJobsList=subJobsList;
	}

	public void generateAfeasibleSolution() {
		// 1. Initial feasible solution
		Solution initialSol= createInitialSolution();
		// a solution is a set of routes
		// 2. VNS
		// Local search



	}

	private Solution createInitialSolution() {
		Solution initialSol= new Solution();
		creationRoutes(); // create as many routes as there are vehicles
		// iteratively insert couples - here should be a destructive and constructive method 
		ArrayList<ArrayList<Couple>> clasification= clasificationjob();
		settingStartServiceTime(); // late time
		settingAssigmentSchift(clasification);
		patientVehicleAssigment();
		clientVehicleAssigment();
		clientVehicleAssigmentTW();
		asignar
		iteratedInsertion(clasification);
		return initialSol;
	}

	private void clientVehicleAssigmentTW() {
		for(Route r:routeList) { // iterating over the routes
			if(enoughtCapacity(r)) {  // has the vehicle capacity available
				// clients with narrow tw
				vehicleClientTW(r);
				r.updateRoute(inp);
			}
			System.out.println(r.toString());
		}
	}

	private void patientVehicleAssigment() {
		for(Route r:routeList) { // iterating over the routes
			if(enoughtCapacity(r)) {  // has the vehicle capacity available
				// 1: Insert patient job
				vehiclePatient(r);
			}
			if(!r.getSubJobsList().isEmpty()) {
				System.out.println(r.toString());}
		}
	}

	private void clientVehicleAssigment() {
		for(Route r:routeList) { // iterating over the routes
			if(enoughtCapacity(r)) {  // has the vehicle capacity available
				// clients with narrow tw
				vehicleClient(r);
				r.updateRoute(inp);
			}
			System.out.println(r.toString());
		}
	}

	private void vehicleClient(Route r) {
		for(int i=1;i<schift.size();i++) {
			ArrayList<Jobs> client= schift.get(i); // calling client job
			for(Jobs j:client) {
				if(!jobsVehicle.containsKey(j.getId())) {
					System.out.print("TW Size  "+(j.getEndTime()-j.getStartTime()));
					if(j.getEndTime()-j.getStartTime()==0) {
						insertClient(j,r);
						System.out.print(" insert  ");
					}
				}
			}
		}

	}
	
	private void vehicleClientTW(Route r) {
		for(int i=1;i<schift.size();i++) {
			ArrayList<Jobs> client= schift.get(i); // calling client job
			for(Jobs j:client) {
				if(!jobsVehicle.containsKey(j.getId())) {
					System.out.print("TW Size  "+(j.getEndTime()-j.getStartTime()));
					if(j.getEndTime()-j.getStartTime()>0) {
						insertClient(j,r);
						System.out.print(" insert  ");
					}
				}
			}
		}

	}

	private void insertClient(Jobs j, Route r) {
		if(r.getSubJobsList().isEmpty()) {
			r.getSubJobsList().add(j);
			jobsVehicle.put(j.getId(), j);
			settingTimes(j);
		}
		else {
			boolean insertedJob=iterateOverRouteClient(j,r);
			if(insertedJob) {
				jobsVehicle.put(j.getId(), j);
			}
		}
	}

	private void settingTimes(Jobs j) {
		double serviceTime=j.getEndTime();
		j.setStartServiceTime(serviceTime);
		double arrival=0;
		if(j.isClient()) {
			arrival=j.getstartServiceTime()-test.getloadTimeHomeCareStaff();}
		else {
			arrival=j.getstartServiceTime()-test.getloadTimePatient();}
		j.setarrivalTime(arrival);
		j.setWaitingTime(arrival, serviceTime);
	}

	private void vehiclePatient(Route r) {
		ArrayList<Jobs> patients= schift.get(0); // calling patient job
		boolean insertedJob= false;
		for(Jobs j:patients) {
			if(!jobsVehicle.containsKey(j.getIdUser())) {
				if(r.getSubJobsList().isEmpty()) {
					addingPatientJob(j,r);
					jobsVehicle.put(j.getIdUser(), j);
					insertedJob= true;
				}
				else {
					insertedJob=iterateOverRoute(j,r);
					if(insertedJob) {
						jobsVehicle.put(j.getIdUser(), j);
					}
				}
			}
		}
		r.updateRoute(inp);
	}



	private void addingPatientJob(Jobs j, Route r) {
		// pick up patient at home
		Jobs pickUpHome=inp.getNodes().get(j.getIdUser()-1);
		settingPickUpTime(pickUpHome, j);
		//	r.getSubJobsList().add(pickUpHome);
		// j<- drop off patient at medical centre
		r.getSubJobsList().add(j);
		settingMedicalAppointment(pickUpHome, j);	
	}


	private void settingMedicalAppointment(Jobs pickUpHome, Jobs j) {
		double tv=inp.getCarCost().getCost(pickUpHome.getId()-1, j.getId()-1);
		double arrivalTime=pickUpHome.getstartServiceTime()+tv+test.getloadTimePatient();
		double endService=j.getstartServiceTime()+j.getReqTime()+test.getCumulativeWaitingTime();
		j.setarrivalTime(arrivalTime);
		j.setWaitingTime(j.getArrivalTime(), j.getStartTime());
		j.setEndServiceTime(endService);
	}

	private void settingPickUpTime(Jobs pickUp, Jobs dropOff) {
		double tv=inp.getCarCost().getCost(pickUp.getId()-1, dropOff.getId()-1);
		double arrivalTime=dropOff.getstartServiceTime()-2*test.getloadTimePatient()-tv;
		double startServiceTime=dropOff.getstartServiceTime()-test.getloadTimePatient()-tv;
		double endService=dropOff.getstartServiceTime()+test.getloadTimePatient();
		pickUp.setarrivalTime(arrivalTime);
		pickUp.setStartServiceTime(startServiceTime);
		pickUp.setEndServiceTime(endService);
	}

	private boolean enoughtCapacity(Route r) {
		boolean capacity =false;
		double passengerAmount=0;
		// 1. working time 
		r.computeTravelTime(inp);
		// 2. capacity of vehicle
		r.computePassenger();
		if(Math.abs(r.getPassengers())<=inp.getVehicles().get(0).getMaxCapacity() && r.getTravelTime()<test.getWorkingTime()) {
			capacity =true;
		}
		return capacity;
	}

	private void settingAssigmentSchift(ArrayList<ArrayList<Couple>> clasification) {
		//1. list of jobs
		// Classifying clients: Home care staff
		ArrayList<Jobs> clasification3 = creationJobsHomeCareStaff(clasification.get(0)); 
		ArrayList<Jobs> clasification2 = creationJobsHomeCareStaff(clasification.get(1));
		ArrayList<Jobs> clasification1 = creationJobsHomeCareStaff(clasification.get(2));
		// Classifying patients: Paramedics
		ArrayList<Jobs> clasification0 = creationJobsParamedics(clasification.get(3));

		// 2. Calling the type and quantity of home care staff
		List<AttributeNurse> homeCareStaff= inp.getNurse(); // home Care Staff according the qualification level
		List<AttributeParamedics> paramedic= inp.getParamedic(); // paramedic qualification level
		// Home care staff for:
		int q3= homeCareStaff.get(2).getQuantity(); // Qualification 3
		int q2= homeCareStaff.get(1).getQuantity(); // Qualification 2
		int q1= homeCareStaff.get(0).getQuantity(); // Qualification 1

		int q0= paramedic.get(0).getQuantity(); // Qualification 0

		// 3. Definition of a feasible sequence of jobs for each qualification level
		ArrayList<ArrayList<Jobs>> qualification3= assigmentHighQualification(q3,clasification3);
		//checkingWorkingTime(qualification3);
		downgradings(clasification2,qualification3);
		downgradings(clasification1,qualification3);

		//Qualification level =2
		clasification2 = creationJobsHomeCareStaff(clasification.get(1)); // update after downgradings
		clasification1 = creationJobsHomeCareStaff(clasification.get(2));  // update 
		ArrayList<ArrayList<Jobs>> qualification2= assigmentHighQualification(q2,clasification2);
		downgradings(clasification1,qualification2);

		//Qualification level =1
		clasification1 = creationJobsHomeCareStaff(clasification.get(2));
		ArrayList<ArrayList<Jobs>> qualification1= assigmentHighQualification(q1,clasification1);


		//Qualification level =0
		ArrayList<ArrayList<Jobs>> qualification0= assigmentParamedic(q0,clasification0);

		//Storing schifts

		// paramedics
		for(ArrayList<Jobs> schifts:qualification0) {
			if(!schifts.isEmpty()) {
				schift.add(schifts);
			}
		}
		// home care staff
		for(ArrayList<Jobs> schifts:qualification1) {
			if(!schifts.isEmpty()) {
				schift.add(schifts);
			}
		}

		for(ArrayList<Jobs> schifts:qualification2) {
			if(!schifts.isEmpty()) {
				schift.add(schifts);
			}
		}
		for(ArrayList<Jobs> schifts:qualification3) {
			if(!schifts.isEmpty()) {
				schift.add(schifts);
			}
		}
	}

	private void checkingWorkingTime(ArrayList<ArrayList<Jobs>> qualification3) {
		ArrayList<ArrayList<Jobs>> jobsListCopy= copyJobsList(qualification3);
		qualification3.clear();
		double serviceTimeDuration=0;
		double waitingTime=0;
		double travelTimeDuration=0;
		ArrayList<Jobs> newSchift= new ArrayList<Jobs>();
		qualification3.add(newSchift);
		for(ArrayList<Jobs> list:jobsListCopy) {	
			for(int j=0; j<list.size();j++) {
				Jobs jNode=list.get(j);
				double serviceTime=jNode.getReqTime();// service time
				double waitTime=jNode.getWaitingTime();// waiting time
				double travelTime=0;// travel time
				serviceTimeDuration+=serviceTime;
				waitingTime+=waitTime;
				if(j!=list.size()-1 && jNode.getId()!=0) { // travel time
					Jobs kNode=list.get(j+1);
					travelTime=inp.getCarCost().getCost(jNode.getId(), kNode.getId());
					travelTimeDuration+=travelTime;
				}
				double duration=serviceTimeDuration+waitingTime+travelTimeDuration;
				if(duration<=296) {
					//if(duration<=test.getWorkingTime()) {
					newSchift.add(jNode);
				}
				else {

					newSchift= new ArrayList<Jobs>();
					newSchift.add(jNode);
					qualification3.add(newSchift);
					serviceTimeDuration=serviceTime;
					waitingTime=waitTime;
					travelTimeDuration=travelTime;
				}
			}
		}
	}

	private ArrayList<ArrayList<Jobs>> copyJobsList(ArrayList<ArrayList<Jobs>> qualification3) {
		ArrayList<ArrayList<Jobs>> jobsListCopy= new ArrayList<ArrayList<Jobs>>();
		for(ArrayList<Jobs> list:qualification3) {
			jobsListCopy.add(list);
		}
		return jobsListCopy;
	}

	private ArrayList<Jobs> creationJobsHomeCareStaff(ArrayList<Couple> qualification) {
		ArrayList<Jobs> clasification = new ArrayList<Jobs>();

		// home care Staff
		for(Couple c:qualification) {
			if(!assignedJobs.containsKey(c.getPresent().getId())) {
				clasification.add(c.getPresent());}
		}
		clasification.sort(Jobs.SORT_BY_STARTW);
		return clasification;

	}


	private ArrayList<Jobs> creationJobsParamedics(ArrayList<Couple> qualification) {
		ArrayList<Jobs> clasification = new ArrayList<Jobs>();

		// paramedic
		for(Couple c:qualification) {
			if(c.getFuture().isMedicalCentre()) {
				if(!assignedJobs.containsKey(c.getFuture().getId())) {
					clasification.add(c.getFuture());}
			}}
		clasification.sort(Jobs.SORT_BY_STARTW);
		return clasification;

	}
	private void downgradings(ArrayList<Jobs> clasification2, ArrayList<ArrayList<Jobs>> qualification3) {
		for(Jobs j:clasification2) { // iterate over jobs
			for(ArrayList<Jobs> homeCare:qualification3) {
				if(!homeCare.isEmpty()) {
					boolean insertion=possibleInsertion(j,homeCare);

					if(insertion) {
						homeCare.add(j);
						assignedJobs.put(j.getId(), j);
						break;
					}
				}
			}
		}
		System.out.println("Stop");
	}

	private ArrayList<ArrayList<Jobs>> assigmentHighQualification(int q3, ArrayList<Jobs> clasification3) {
		ArrayList<ArrayList<Jobs>> qualification3= new ArrayList<> (q3);
		for(int i=0;i<q3;i++) {
			ArrayList<Jobs> schift=new ArrayList<>();
			qualification3.add(schift);
		}
		for(Jobs j:clasification3) { // iterate over jobs
			if(!assignedJobs.containsKey(j.getId())) {
				for(ArrayList<Jobs> homeCare:qualification3) {
					boolean insertion=possibleInsertion(j,homeCare);
					if(insertion) {
						assignedJobs.put(j.getId(), j);
						homeCare.add(j);
						break;
					}

				}
			}
		}
		System.out.println("Stop");
		return qualification3;
	}



	private ArrayList<ArrayList<Jobs>> assigmentParamedic(int q3, ArrayList<Jobs> clasification3) {
		ArrayList<ArrayList<Jobs>> qualification3= new ArrayList<> (q3);
		for(int i=0;i<q3;i++) {
			ArrayList<Jobs> schift=new ArrayList<>();
			qualification3.add(schift);
		}
		for(Jobs j:clasification3) { // iterate over jobs
			if(!assignedJobs.containsKey(j.getIdUser())) {
				for(ArrayList<Jobs> paramedic:qualification3) {
					boolean insertion=possibleInsertion(j,paramedic);
					if(insertion) {
						assignedJobs.put(j.getId(), j);
						paramedic.add(j);
						break;
					}

				}
			}
		}
		System.out.println("Stop");
		return qualification3;
	}

	private boolean possibleInsertion(Jobs j, ArrayList<Jobs> homeCare) {
		boolean inserted=false;
		if(homeCare.isEmpty()) {
			inserted=true;
		}
		else {
			inserted=iterateOverSchift(j,homeCare);
		}
		return inserted;
	}

	private boolean iterateOverRoute(Jobs j, Route r) {
		boolean inserted=false;
		for(int i=0;i<r.getSubJobsList().size();i++) {
			if(!r.getJobsDirectory().containsKey(j.getIdUser())) {
				Jobs inRoute=r.getSubJobsList().get(i);
				if(i==r.getSubJobsList().size()-1 || i==0) {
					inserted=insertionLaterVehicle(inRoute,j);//(inRoute)******(j)
					if(inserted) {
						r.getSubJobsList().add(j);
					}
					else{
						if(i==0) {
							inserted=insertionEarlyVehicle(inRoute,j);//(j)******(inRoute)
							if(inserted) {
								r.getSubJobsList().add(i-1,j);
							}	}					
					}
				}
				else {// // (inRoute)*******(j)******(inRouteK)
					if(i==0) {
						inserted=insertionEarlyVehicle(inRoute,j);//(j)******(inRoute)
						if(inserted) {
							r.getSubJobsList().add(0,j);
						}
					}
					if(!inserted) {
						Jobs inRouteK=r.getSubJobsList().get(i+1);
						inserted=insertionIntermediateVehicle(inRoute,j,inRouteK);// (inRoute)*******(j)******(inRouteK)
						if(inserted) {
							r.getSubJobsList().add(i+1,j);
							updateRouteTW(r);
							System.out.println(r.toString());
						}
					}
				}
				r.updatingJobsList();
			}
		}
		return inserted;
	}

	private void updateRouteTW(Route r) {
		double arrivalTime=0;
		double travelTime=0;
		double serviceTime=0;
		for(int i=1;i<r.getSubJobsList().size();i++) {
			travelTime=inp.getCarCost().getCost(r.getSubJobsList().get(i-1).getId()-1, r.getSubJobsList().get(i).getId()-1);
			if(r.getSubJobsList().get(i).isPatient()) {
				arrivalTime=r.getSubJobsList().get(i-1).getArrivalTime()+travelTime+test.getloadTimePatient();
			}
			else {
				arrivalTime=r.getSubJobsList().get(i-1).getArrivalTime()+travelTime+test.getloadTimeHomeCareStaff();
			}
			serviceTime=Math.max(arrivalTime, r.getSubJobsList().get(i).getStartTime());
			r.getSubJobsList().get(i).setStartServiceTime(serviceTime);
			r.getSubJobsList().get(i).setarrivalTime(arrivalTime);
			r.getSubJobsList().get(i).setWaitingTime(arrivalTime, serviceTime);
		}
		System.out.println(r.toString());
	}

	private boolean iterateOverRouteClient(Jobs j, Route r) {
		boolean inserted=false;
		for(int i=0;i<r.getSubJobsList().size();i++) {
			if(!r.getJobsDirectory().containsKey(j.getId())) {
				Jobs inRoute=r.getSubJobsList().get(i);
				if(i==r.getSubJobsList().size()-1 ) {
					inserted=insertionLaterVehicle(inRoute,j);//(inRoute)******(j)
					if(inserted) {
						r.getSubJobsList().add(j);
					}
					//					else{
					//						inserted=insertionEarlyVehicle(inRoute,j);//(j)******(inRoute)
					//						if(inserted) {
					//							r.getSubJobsList().add(0,j);
					//						}
					//					}
				}
				else {// // (inRoute)*******(j)******(inRouteK)
					if(i==0) {
						inserted=insertionEarlyVehicle(inRoute,j);//(j)******(inRoute)
						if(inserted) {
							r.getSubJobsList().add(0,j);
						}
					}
					if(!inserted) {
						Jobs inRouteK=r.getSubJobsList().get(i+1);
						inserted=insertionIntermediateVehicle(inRoute,j,inRouteK);// (inRoute)*******(j)******(inRouteK)
						if(inserted) {
							r.getSubJobsList().add(i+1,j);
							updateRouteTW(r);
							System.out.println(r.toString());
						}
					}
				}
				r.updatingJobsList();
			}
		}
		return inserted;
	}


	private boolean iterateOverSchift(Jobs j, ArrayList<Jobs> homeCare) {
		boolean inserted=false;
		for(int i=0;i<homeCare.size();i++) { 
			Jobs inRoute=homeCare.get(i);
			if(i==homeCare.size()-1 ) {
				inserted=insertionLater(inRoute,j);//(inRoute)******(j)
				if(!inserted) {
					inserted=insertionEarly(inRoute,j);//(j)******(inRoute)
				}
			}
			else {// // (inRoute)*******(j)******(inRouteK)
				if(i==0) {
					inserted=insertionEarly(inRoute,j);//(j)******(inRoute)
				}
				if(!inserted) {
					Jobs inRouteK=homeCare.get(i+1);
					inserted=insertionIntermediateJb(inRoute,j,inRouteK);// (inRoute)*******(j)******(inRouteK)
				}
			}
		}
		return inserted;
	}

	private boolean insertionIntermediateJb(Jobs inRoute, Jobs j, Jobs inRouteK) {
		boolean inserted=insertedMiddle(inRoute,j,inRouteK);
		return inserted;
	}
	private boolean insertionIntermediateVehicle(Jobs inRoute, Jobs j, Jobs inRouteK) {
		boolean inserted=insertedMiddleVehicle(inRoute,j,inRouteK);

		return inserted;
	}


	private boolean insertedMiddleVehicle(Jobs inRoute, Jobs j, Jobs inRouteK) {
		boolean inserted=false;
		double tv=inp.getCarCost().getCost(j.getId()-1,inRouteK.getId()-1);
		double possibleArrivalTime=inRouteK.getstartServiceTime()-(tv+test.getloadTimeHomeCareStaff());
		if(possibleArrivalTime>inRoute.getstartServiceTime()-test.getloadTimeHomeCareStaff() && possibleArrivalTime<=inRouteK.getstartServiceTime()-test.getloadTimeHomeCareStaff() &&  possibleArrivalTime<=j.getEndTime()) {
			if(inRouteK.getstartServiceTime()-test.getloadTimeHomeCareStaff()>j.getstartServiceTime()) {	
			settingTimes(possibleArrivalTime,j);
			//if(j.getWaitingTime()<=test.getCumulativeWaitingTime()) {
			inserted=true;
			//}
		}
		}
		if(!inserted) {
			tv=inp.getCarCost().getCost(inRoute.getId()-1,j.getId()-1);
			possibleArrivalTime=inRoute.getstartServiceTime()+(tv+test.getloadTimeHomeCareStaff());
			if(possibleArrivalTime>inRoute.getstartServiceTime()-test.getloadTimeHomeCareStaff() && possibleArrivalTime<=inRouteK.getstartServiceTime()-test.getloadTimeHomeCareStaff() && inRouteK.getstartServiceTime()-test.getloadTimeHomeCareStaff()>j.getstartServiceTime() && possibleArrivalTime<=j.getEndTime()) {
				settingTimes(possibleArrivalTime,j);
				//if(j.getWaitingTime()<=test.getCumulativeWaitingTime()) {
				inserted=true;
				//}
			}
		}

		return inserted;
	}

	private boolean insertedMiddle(Jobs inRoute, Jobs j, Jobs inRouteK) {
		boolean inserted=false;
		double tv=inp.getCarCost().getCost(j.getId()-1,inRouteK.getId());
		double possibleArrivalTime=inRouteK.getstartServiceTime()-(j.getReqTime()+tv+test.getloadTimeHomeCareStaff());
		if(possibleArrivalTime>inRoute.getstartServiceTime() && possibleArrivalTime>0) {
			if(possibleArrivalTime<=j.getstartServiceTime()) {
				inserted=true;
			}
			else {
				if(possibleArrivalTime<=j.getEndTime() && possibleArrivalTime>0) {
					//if(possibleArrivalTime<=j.getEndTime() && possibleArrivalTime>=j.getStartTime()) {
					settingTimes(possibleArrivalTime,j);
					inserted=true;
				}
			}
		}

		return inserted;
	}

	private boolean insertionEarly(Jobs inRoute, Jobs j) {
		boolean inserted=false;
		double tv=inp.getCarCost().getCost(j.getId()-1,inRoute.getId()-1);
		double possibleArrivalTime=inRoute.getstartServiceTime()-(j.getReqTime()+tv+test.getloadTimeHomeCareStaff());
		if(possibleArrivalTime<=j.getEndTime() && possibleArrivalTime>0) {
			//if(possibleArrivalTime<=j.getEndTime() && possibleArrivalTime>=j.getStartTime()) {
			settingTimes(possibleArrivalTime,j);
			inserted=true;

		}
		return inserted;
	}

	private boolean insertionEarlyVehicle(Jobs inRoute, Jobs j) {
		boolean inserted=false;
		double tv=inp.getCarCost().getCost(j.getId()-1,inRoute.getId()-1);
		double possibleArrivalTime=inRoute.getstartServiceTime()-(tv+test.getloadTimeHomeCareStaff());
		if(possibleArrivalTime<=j.getEndTime()) { // se considera menor para considerar los tiempos de espera
			if(inRoute.getstartServiceTime()>=j.getStartTime() || inRoute.getstartServiceTime()>=j.getEndTime()) {
				settingTimes(possibleArrivalTime,j);
				inserted=true;
			}
		}
		return inserted;
	}


	private void settingTimes(double possibleArrivalTime, Jobs j) {
		j.setarrivalTime(possibleArrivalTime);
		double serviceTime=Math.max(possibleArrivalTime, j.getStartTime());
		j.setStartServiceTime(serviceTime);
		j.setWaitingTime(possibleArrivalTime, j.getstartServiceTime());	
	}

	private boolean insertionLater(Jobs inRoute,Jobs j) {//(inRoute)******(j)
		boolean inserted=false;
		double tv=inp.getCarCost().getCost(inRoute.getId()-1, j.getId()-1);
		double possibleArrivalTime=inRoute.getstartServiceTime()+inRoute.getReqTime()+tv+test.getloadTimeHomeCareStaff();
		if(possibleArrivalTime<=j.getstartServiceTime()) {
			inserted=true;
			settingTimes(possibleArrivalTime,j);
			///j.setWaitingTime(possibleArrivalTime, j.getstartServiceTime());
		}
		else{
			if(possibleArrivalTime<=j.getEndTime()) {
				//if(possibleArrivalTime<=j.getEndTime() && possibleArrivalTime>=j.getStartTime()) {
				settingTimes(possibleArrivalTime,j);
				inserted=true;

			}
		}
		return inserted;
	}

	private boolean insertionLaterVehicle(Jobs inRoute,Jobs j) {//(inRoute)******(j)
		boolean inserted=false;
		double tv=inp.getCarCost().getCost(inRoute.getId()-1, j.getId()-1);
		double possibleArrivalTime=inRoute.getstartServiceTime()+tv+test.getloadTimeHomeCareStaff();
		if(possibleArrivalTime<=j.getEndTime()) {
			if(inRoute.getstartServiceTime()<=j.getStartTime() || inRoute.getstartServiceTime()<=j.getEndTime()) {
				settingTimes(possibleArrivalTime,j);

				inserted=true;
			}
		}
		return inserted;
	}

	private void settingStartServiceTime() {
		for(Couple couple:subJobsList) {
			double serviceTimePresent=couple.getPresent().getEndTime();
			double serviceTimeFuture=couple.getFuture().getEndTime();
			if(serviceTimePresent==0 ||serviceTimeFuture==0 ) {
				System.out.println("Stop");
			}
			couple.getPresent().setStartServiceTime(serviceTimePresent);
			couple.getFuture().setStartServiceTime(serviceTimeFuture);
		}

	}

	private void iteratedInsertion(ArrayList<ArrayList<Couple>> clasification) {
		for(int i=0;i<clasification.size()-1;i++) {
			jobReq3Qualification(clasification.get(i)); // job with a qualification equal to 3
		}

		System.out.println("Stop");
	}

	private void jobReq3Qualification(ArrayList<Couple>qualification) {
		ArrayList<Jobs> seq1= new ArrayList<Jobs>();
		for(Couple c:qualification) {
			seq1.add(c.getPresent());
		}
		seq1.sort(Jobs.SORT_BY_STARTW);


		for(Route r:routeList) {
			for(Jobs j:seq1) {
				if(r.getDurationRoute()<test.getWorkingTime()) {
					checkFeasibleInsertion(r,j);		
				}
				else {
					routeList.add(r);
				}
			}
		}
		System.out.println("Stop");
	}

	private void checkFeasibleInsertion(Route r, Jobs toInsert) {

		if(r.getSubJobsList().isEmpty()) {
			toInsert.setarrivalTime(toInsert.getstartServiceTime());
			r.getSubJobsList().add(toInsert);
		}
		else{
			//for(int n=0;n<r.getSubJobsList().size();n++) {
			//if(n==0 || n==r.getSubJobsList().size()-1) {
			Jobs j=r.getSubJobsList().getLast(); // append at the end
			checkPossibleInsertion(j,toInsert,r);
			//}

			//}
		}

	}

	private boolean checkPossibleInsertion(Jobs j, Jobs toInsert, Route r) {
		// 1. to insert could be the job after j
		boolean insertion =false;
		if(toInsert.getStartTime()>j.getstartServiceTime()) {
			if(toInsertIsReacheable(j,toInsert)) {
				r.getSubJobsList().add(toInsert);
			}
			else {

				if(toInsert.getStartTime()<j.getstartServiceTime()) {// toInsert is early
					if(toInsertIsEARLYReacheable(j,toInsert)) {
						r.getSubJobsList().add(toInsert);
					}	
				}
			}

		}
		return insertion;
	}



	private boolean toInsertIsEARLYReacheable(Jobs j, Jobs toInsert) {
		boolean reacheble=false;
		double tv=inp.getCarCost().getCost(j.getId()-1, toInsert.getId()-1);
		double possibleArrival=toInsert.getstartServiceTime()+toInsert.getReqTime()+tv+test.getloadTimeHomeCareStaff();
		if(possibleArrival<=j.getstartServiceTime()) {
			reacheble=true;
		}
		else {
			possibleArrival=j.getstartServiceTime()-(toInsert.getReqTime()+tv+test.getloadTimeHomeCareStaff());	
			if(possibleArrival>=toInsert.getStartTime() && possibleArrival<=toInsert.getEndTime()) {
				toInsert.setarrivalTime(possibleArrival);
				toInsert.setStartServiceTime(possibleArrival);
				toInsert.setWaitingTime(possibleArrival, toInsert.getstartServiceTime());
				reacheble=true;
			}
		}
		return reacheble;
	}

	private boolean toInsertIsReacheable(Jobs j, Jobs toInsert) {
		boolean reacheable=false;
		double tv=inp.getCarCost().getCost(j.getId()-1, toInsert.getId()-1);
		double possibleArrival=j.getstartServiceTime()+j.getReqTime()+tv+test.getloadTimeHomeCareStaff();
		if(possibleArrival<toInsert.getstartServiceTime()) { // checking expected start service time
			toInsert.setarrivalTime(possibleArrival);
			toInsert.setWaitingTime(possibleArrival, toInsert.getstartServiceTime());
			reacheable=true;
		}
		else { // checking expected within TW
			if(possibleArrival>=toInsert.getStartTime() && possibleArrival>=toInsert.getEndTime()) {
				toInsert.setarrivalTime(possibleArrival);
				toInsert.setStartServiceTime(possibleArrival);
			}
		}
		return reacheable;
	}



	private boolean checkReaminingRoute(Route newRoute) {
		// 1. Time windows

		// 2. Driving working Route

		//3. Vehicle capacity

		//4. Max. detour duration

		return false;
	}


	private void creationRoutes() {
		int totalVehicle= inp.getVehicles().get(0).getQuantity();
		for(int i=0;i<totalVehicle;i++) {
			Route r= new Route();
			r.setIdRoute(i);
			routeList.add(r);
		}
	}

	private  ArrayList<ArrayList<Couple>> clasificationjob() {
		ArrayList<ArrayList<Couple>> clasification= new ArrayList<ArrayList<Couple>>();
		// 0.classified couples according the req qualification
		int i=-1;
		for(Couple c:subJobsList) {
			i++;
			subJobs.put(i, c.getPresent());

			i++;
			subJobs.put(i, c.getFuture());
			if(c.getPresent().getReqQualification()!=c.getFuture().getReqQualification()) {
				System.out.print("Error");	
			}
		}

		for(int qualification=0;qualification<=inp.getMaxQualificationLevel();qualification++) {
			for(Couple c:subJobsList) {
				if(c.getQualification()==qualification && qualification==0) {
					subJobspatients.add(c);
				}
				if(c.getQualification()==qualification && qualification==1) {
					subJobsLowestQualification.add(c);
				}
				if(c.getQualification()==qualification && qualification==2) {
					this.subJobsMediumQualification.add(c);
				}
				if(c.getQualification()==qualification && qualification==3) {
					this.subJobsHighestQualification.add(c);
				}
			}
		}
		clasification.add(subJobsHighestQualification);
		clasification.add(subJobsMediumQualification);
		clasification.add(subJobsLowestQualification);
		clasification.add(subJobspatients);

		return clasification;
	}

	public void assigmentHomeCareStaff() {
		ArrayList<Jobs> listserviceJobs=new ArrayList<Jobs>();
		for(Couple c:subJobsList) {
			if(c.getPresent().isClient()) {
				listserviceJobs.add(c.getPresent());
			}
			//			else {
			//				if(c.getPresent().isPatient()) {
			//					if() {
			//						
			//					}
			//				}
			//			}	
		}
		// definición de lista de trabajos por enfermenra
		List<AttributeNurse> nursesInformation=inp.getNurse();
		//		int amount=nursesInformation.get(3);
		//		ArrayList<ArrayList<Jobs>> homeCareStaff=new ArrayList<nursesInformation.>();
		//		




	}









}

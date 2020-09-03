import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
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
	private HashMap<Integer, Jobs>checkedFutureJobs=new HashMap<Integer, Jobs>();
	ArrayList<ArrayList<Jobs>> schift= new ArrayList<>();
	private Solution initialSol=null;

	public Solution getInitialSol() {
		return initialSol;
	}

	public DrivingRoutes(Inputs i, Random r, Test t, ArrayList<Couple> subJobsList) {
		inp=i;
		test=t;
		rn=r;
		this.subJobsList=subJobsList;
	}

	public void generateAfeasibleSolution() {
		// 1. Initial feasible solution
		initialSol= createInitialSolution();
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
		asignmentFutureJobs(); // FUTURE JOBS!!
		for(Route r:routeList ) {
			if(!r.getSubJobsList().isEmpty()) {
				initialSol.getRoutes().add(r);
			}
		}
		return initialSol;
	}


	private void changingArrivalTimeSequence(Route r) {
		for(Jobs j:r.getSubJobsList()) {
			double loadTime=0;
			if(j.isClient()) {
				loadTime=test.getloadTimeHomeCareStaff();
			}
			else {
				loadTime=test.getloadTimePatient();
			}
			j.setarrivalTime(j.getstartServiceTime()-loadTime);
			j.setEndServiceTime(j.getstartServiceTime()+j.getReqTime());
		}
	}

	private void asignmentFutureJobs() {
		boolean insertedJobs=false;
		ArrayList<Route> copyrouteList= copyListRoute();
		for(Route r:copyrouteList) {
			if(!r.getSubJobsList().isEmpty()) {
				for(Jobs jobsInRoute:r.getSubJobsList()) {
					Jobs pair=extractingJobInformationClient(r,jobsInRoute);
					if(pair!=null) {
						if(jobsInRoute.isMedicalCentre() || jobsInRoute.isPatient()) {
							patientProcedure(jobsInRoute,insertedJobs,pair);
						}
						else {
							if(jobsInRoute.isClient()) {
								clientProcedure(jobsInRoute,insertedJobs,pair);
							}
						}
					}
				}
			}	
		}
	}

	private void clientProcedure(Jobs jobsInRoute, boolean insertedJobs,Jobs pair) {
		if(!checkedFutureJobs.containsValue(jobsInRoute) && pair!=null) {
			for(Route route:this.routeList){
				if(!jobsVehicle.containsValue(pair)) {
					System.out.println("\nRoute "+route.toString());
					Jobs pickUp=new Jobs(pair);
					settingPickHCSUp(pickUp,jobsInRoute);
					if(!checkedFutureJobs.containsKey(pickUp.getId())) {
						insertedJobs=insertingPair(pickUp,route);
						if(jobsVehicle.containsValue(pickUp)) {
							checkedFutureJobs.put(pickUp.getId(), pickUp);
						}	}
				}
				if(insertedJobs) {
					break;
				}
			}
		}
	}

	private void settingPickHCSUp(Jobs pickUp, Jobs jobsInRoute) {
		pickUp.setTotalPeople(1);
		// Time window
		double earlyTime=0;
		double laterTime=0;
		if(jobsInRoute.isPatient() || jobsInRoute.isMedicalCentre()) {
			earlyTime=jobsInRoute.getstartServiceTime()+test.getloadTimePatient();
			laterTime=earlyTime+test.getCumulativeWaitingTime()+test.getloadTimePatient();
		}
		else {
			earlyTime=jobsInRoute.getstartServiceTime()+test.getloadTimeHomeCareStaff();
			laterTime=earlyTime+test.getCumulativeWaitingTime()+test.getloadTimeHomeCareStaff();
		}
		pickUp.setStartTime(earlyTime);
		pickUp.setEndTime(laterTime);
		pickUp.setStartServiceTime(laterTime);
		pickUp.setserviceTime(0);
	}

	private boolean insertingPair(Jobs pickUp, Route r) {
		boolean insertedJobs=false;
		Route copy=copyRoute(r);
		if(pickUp!=null) {
			// 3. call and try to insert task
			insertedJobs=insertFutureClient(pickUp,copy);
			copy.updateRoute(inp);
			System.out.print(copy.toString());
			if(insertedJobs) {
				r.getSubJobsList().clear();
				for(Jobs j:copy.getSubJobsList()) {
					r.getSubJobsList().add(j);
				}
				r.updateRoute(inp);
				System.out.print("\nRoute "+r.toString());
			}
		}
		return insertedJobs;

	}

	private void patientProcedure(Jobs jobsInRoute, boolean insertedJobs,Jobs pair) {
		if(!checkedFutureJobs.containsValue(jobsInRoute) && pair!=null) {
			for(Route route:this.routeList){
				if(!jobsVehicle.containsValue(pair)) {
					System.out.println("\nRoute "+route.toString());
					Jobs pickUp=new Jobs(jobsInRoute);
					Jobs dropOff=new Jobs(pair);
					settingPickUp(pickUp);
					settingDropOff(pickUp,dropOff);
					if(!checkedFutureJobs.containsKey(dropOff.getId())) {
						insertedJobs=insertingPair(pickUp, dropOff,route);
						if(jobsVehicle.containsValue(dropOff)) {
							checkedFutureJobs.put(pickUp.getId(), pickUp);
							checkedFutureJobs.put(dropOff.getId(), dropOff);
						}	}
				}
				if(insertedJobs) {
					break;
				}
			}
		}

	}

	private void settingDropOff(Jobs jobsInRoute,Jobs dropOff) {
		dropOff.setTotalPeople(-1);
		double tv=inp.getCarCost().getCost(jobsInRoute.getId()-1, dropOff.getId()-1)*test.getDetour();
		// Time window
		double earlyTime=0;
		double laterTime=0;
		if(dropOff.isPatient() || dropOff.isMedicalCentre()) {
			earlyTime=jobsInRoute.getstartServiceTime()+tv+test.getloadTimePatient();
			laterTime=earlyTime+test.getCumulativeWaitingTime()+test.getloadTimePatient();
		}
		else {
			earlyTime=jobsInRoute.getstartServiceTime()+tv+test.getloadTimeHomeCareStaff();
			laterTime=earlyTime+test.getCumulativeWaitingTime()+test.getloadTimeHomeCareStaff();
		}
		dropOff.setStartTime(earlyTime);
		dropOff.setEndTime(laterTime);
		dropOff.setStartServiceTime(laterTime);
		dropOff.setserviceTime(0);
	}

	private void settingPickUp(Jobs pickUp) {
		// 3. how many people involve the service // time window // preliminary service start time
		pickUp.setTotalPeople(2);
		// Time window
		double earlyTime=pickUp.getendServiceTime();
		double laterTime=earlyTime+test.getCumulativeWaitingTime();
		pickUp.setStartTime(earlyTime);
		pickUp.setEndTime(laterTime);
		pickUp.setStartServiceTime(laterTime);
		pickUp.setserviceTime(0);
	}

	private ArrayList<Route> copyListRoute() {
		ArrayList<Route> copyrouteList= new ArrayList<Route>();
		for(Route r:routeList) {
			Route newCopy=copyRoute(r);	
			copyrouteList.add(newCopy);
		}
		return copyrouteList;
	}

	private Route copyRoute(Route r) {
		Route newCopy=new Route (r);	
		return newCopy;
	}

	private boolean insertingPair(Jobs pickUp, Jobs pair, Route r) {
		boolean insertedJobs=false;
		Route copy=copyRoute(r);
		if(pair!=null) {
			// 3. call and try to insert task
			insertedJobs=insertFutureClient(pickUp,copy);
			copy.updateRoute(inp);
			System.out.print(copy.toString());
			//insertedJobs=checkingFutureJobs(copy,pickUp);
			if(insertedJobs) {
				insertedJobs=insertFutureClient(pair,copy);
				copy.updateRoute(inp);
				System.out.print(copy.toString());
				//insertedJobs=checkingFutureJobs(copy,pair);
			}
			if(insertedJobs) {
				r.getSubJobsList().clear();;
				for(Jobs j:copy.getSubJobsList()) {
					r.getSubJobsList().add(j);
				}
				r.updateRoute(inp);
				System.out.print(r.toString());
			}
		}
		return insertedJobs;
	}


	private Jobs extractingJobInformationClient(Route r, Jobs jobsInRoute) {
		Jobs pair=null;
		// 1. calling job
		if(jobsInRoute.isClient()) { // setting the pick up of the home care staff
			pair= callingPairClient(jobsInRoute); // 2. calling tasks
		}
		else {
			if(jobsInRoute.isMedicalCentre() ) {  // setting the pick up of paramedic and patient
				pair= callingPairPatient(jobsInRoute); // 2. calling tasks
				settingInformationPair(pair, jobsInRoute); // 3. how many people involve the service // time window // preliminary service start time
			}
		}
		return pair;
	}

	private void settingInformationPair(Jobs pair, Jobs inRoute) {
		// 3. how many people involve the service // time window // preliminary service start time
		pair.setTotalPeople(-1);
		// Time window
		double tv=inp.getCarCost().getCost(inRoute.getId()-1, pair.getId()-1)*test.getDetour();
		double earlyTime=inRoute.getendServiceTime()+tv;
		double laterTime=earlyTime+test.getCumulativeWaitingTime();
		pair.setStartTime(earlyTime);
		pair.setEndTime(laterTime);
		pair.setStartServiceTime(laterTime);
	}


	private Jobs callingPairClient(Jobs jobsInRoute) {
		Jobs pair=null;
		if(jobsInRoute.getsubJobPair()!=null) {
			pair=jobsInRoute.getsubJobPair();
		}
		return pair;
	}

	private Jobs callingPairPatient(Jobs jobsInRoute) {
		Jobs pair=new Jobs(inp.getNodes().get(jobsInRoute.getIdUser()-1));	
		return pair;
	}

	private void clientVehicleAssigmentTW() {
		for(Route r:routeList) { // iterating over the routes
			if(enoughtCapacity(r)) {  // has the vehicle capacity available
				// clients with narrow tw
				vehicleClientTW(r);
				r.updateRoute(inp);
			}

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
						r.updateRoute(inp);
						System.out.print(r.toString());
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
						r.updateRoute(inp);
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

	private boolean insertFutureClient(Jobs j, Route r) {
		boolean insertedJob=false;
		if(r.getSubJobsList().isEmpty()) {
			r.getSubJobsList().add(j);
			jobsVehicle.put(j.getId(), j);
			settingTimes(j);
			insertedJob=true;
		}
		else {
			insertedJob=iterateOverRouteFutureClient(j,r);
			if(insertedJob) {
				jobsVehicle.put(j.getId(), j);
			}
		}
		return insertedJob;
	}

	private void settingTimes(Jobs j) {
		double serviceTime=j.getEndTime();
		j.setStartServiceTime(serviceTime);
		double arrival=0;
		if(j.isClient()) {
			arrival=j.getstartServiceTime()-test.getloadTimeHomeCareStaff();}
		else {
			arrival=j.getstartServiceTime()-test.getloadTimePatient()-test.getRegistrationTime();}

		j.setarrivalTime(arrival);
		if(j.isClient()) {
			j.setdepartureTime(j.getArrivalTime()+test.getloadTimeHomeCareStaff());}
		else {
			j.setdepartureTime(j.getArrivalTime()+test.getloadTimePatient());}

		j.setWaitingTime(j.getstartServiceTime(), j.getStartTime());
		j.setEndServiceTime(j.getstartServiceTime()+j.getReqTime());
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
					System.out.println(r.toString());
				}
				else {
					insertedJob=iterateOverRoute(j,r);
					if(insertedJob) {
						jobsVehicle.put(j.getIdUser(), j);
						System.out.println(r.toString());
					}
				}
			}
		}
		updateRouteTimes(r);
		r.updateRoute(inp);
	}



	private void addingPatientJob(Jobs j, Route r) { // the node j is the medical centre
		// inf at medical centre
		double arrival=j.getstartServiceTime()-test.getloadTimePatient()-test.getRegistrationTime(); // this is the arrival time of the vehicle at node j
		j.setarrivalTime(arrival);   // j es el drop-off node - setting the arrival time
		j.setdepartureTime(j.getArrivalTime()+test.getloadTimePatient()); // setting the departure time of the vehicle
		// pick up patient at home
		Jobs pickUpHome=inp.getNodes().get(j.getIdUser()-1); // node which represents the patient home
		pickUpHome.setTotalPeople(2); // setting passengers
		j.setserviceTime(pickUpHome.getReqTime()); // setting the treatment time of the patient j at the medical centre
		pickUpHome.setserviceTime(0); // setting that theres is any job for the paramedic at the medical centre
		pickUpHome.setPair(j); // link the relation

		settingPickUpTime(pickUpHome, j); // setting Pick up time // considering the detour time
		j.setEndServiceTime(j.getstartServiceTime()+j.getReqTime());
		r.getSubJobsList().add(pickUpHome);
		// j<- drop off patient at medical centre
		System.out.println("Information drop off job " + j.toString());
		System.out.println("Information pick up job " + pickUpHome.toString());
		r.getSubJobsList().add(j);
		//settingMedicalAppointment(pickUpHome, j);	
	}



	private void settingPickUpTime(Jobs pickUp, Jobs dropOff) { // pensando en la pareja entre pacientes
		double tv=computingTravelTimeWithDetour(pickUp,dropOff); // considering the detour time
		//double arrivalTime=dropOff.getstartServiceTime()-test.getloadTimePatient()-tv;
		double arrivalTime=dropOff.getArrivalTime()-test.getloadTimePatient()-tv;
		double startServiceTime=arrivalTime+test.getloadTimePatient();
		double departure=startServiceTime;
		pickUp.setStartTime(startServiceTime);// setting tw according the job position in the route
		pickUp.setEndTime(startServiceTime);
		pickUp.setStartServiceTime(startServiceTime);
		pickUp.setEndServiceTime(departure);
		pickUp.setarrivalTime(arrivalTime);
		pickUp.setdepartureTime(departure);
	}



	private boolean enoughtCapacity(Route r) {
		boolean capacity =false;
		// 1. working time 
		r.computeTravelTime(inp);
		// 2. capacity of vehicle
		r.computePassenger();
		if(Math.abs(r.getPassengers())<inp.getVehicles().get(0).getMaxCapacity() && r.getTravelTime()<test.getWorkingTime()) {
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


	private ArrayList<Jobs> creationJobsHomeCareStaff(ArrayList<Couple> qualification) {
		ArrayList<Jobs> clasification = new ArrayList<Jobs>();

		// home care Staff
		for(Couple c:qualification) {
			if(!assignedJobs.containsKey(c.getPresent().getId())) {
				c.getPresent().setTotalPeople(-1);
				clasification.add(c.getPresent());
			}
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
					c.getFuture().setTotalPeople(-2);
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
			if(j.isClient()) {
				j.setarrivalTime(j.getstartServiceTime()-test.getloadTimeHomeCareStaff());}
			else {j.setarrivalTime(j.getstartServiceTime()-test.getloadTimePatient()-test.getRegistrationTime());}
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
				if(i==r.getSubJobsList().size()-1) {
					inserted=insertionLaterVehicle(inRoute,j);//(inRoute)******(j)
					if(inserted) {
						//r.getSubJobsList().add(j);
						addingPickUp(r,inRoute,j);
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
							int position=0;
							addingPickUpPosition(r,position,j);

						}
					}
					if(!inserted) {
						Jobs inRouteK=r.getSubJobsList().get(i+1);
						inserted=insertedMiddleVehicle(inRoute,j,inRouteK);// (inRoute)*******(j)******(inRouteK)
						if(inserted) {
							addingPickUpPosition(r,i+1,j);
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

	private void addingPickUpPosition(Route r, int position, Jobs j) {
		// pick up patient at home
		Jobs pickUpHome=inp.getNodes().get(j.getIdUser()-1);
		pickUpHome.setTotalPeople(2);
		settingPickUpTime(pickUpHome, j);
		r.getSubJobsList().add(pickUpHome);
		// j<- drop off patient at medical centre
		r.getSubJobsList().add(position,j);
		//settingMedicalAppointment(pickUpHome, j);	
	}

	private void addingPickUp(Route r,Jobs inRoute,Jobs j) {
		// pick up patient at home
		Jobs pickUpHome=inp.getNodes().get(j.getIdUser()-1);
		pickUpHome.setTotalPeople(2);
		settingPickUpTime(pickUpHome, j);
		r.getSubJobsList().add(pickUpHome);
		// j<- drop off patient at medical centre
		r.getSubJobsList().add(j);
		//settingMedicalAppointment(pickUpHome, j);
	}

	private void updateRouteTW(Route r) {
		double arrivalTime=0;
		double departure=0;
		double travelTime=0;
		double serviceTime=0;
		// initial node -. first job uin the route
		for(int i=1;i<r.getSubJobsList().size();i++) {
			// first job
			Jobs previous=r.getSubJobsList().get(i-1);
			Jobs current=r.getSubJobsList().get(i);
			travelTime=inp.getCarCost().getCost(previous.getId()-1, current.getId()-1);
			if(previous.isClient()) {
				arrivalTime=previous.getArrivalTime()+travelTime+test.getloadTimeHomeCareStaff();
			}
			else {
				arrivalTime=previous.getArrivalTime()+travelTime+test.getloadTimePatient();
			}
			r.getSubJobsList().get(i).setarrivalTime(arrivalTime);
			if(r.getSubJobsList().get(i).isClient()) {
				departure=r.getSubJobsList().get(i).getArrivalTime()+test.getloadTimeHomeCareStaff();

			}
			else {
				departure=r.getSubJobsList().get(i).getArrivalTime()+test.getloadTimePatient();
			}
			serviceTime=Math.max(arrivalTime, r.getSubJobsList().get(i).getStartTime());
			r.getSubJobsList().get(i).setStartServiceTime(serviceTime);
			r.getSubJobsList().get(i).setdepartureTime(departure);
			r.getSubJobsList().get(i).setWaitingTime(arrivalTime, serviceTime);
		}
		System.out.println(r.toString());
	}



	private void updateRouteTimes(Route r) {
		double arrivalTime=0;
		double serviceTime=0;
		for(int i=0;i<r.getSubJobsList().size();i++) {
			if(r.getSubJobsList().get(i).isClient()) {

				arrivalTime=r.getSubJobsList().get(i).getstartServiceTime()-test.getloadTimeHomeCareStaff();
			}
			else {
				arrivalTime=r.getSubJobsList().get(i).getstartServiceTime()-test.getloadTimePatient();
			}
			if(r.getSubJobsList().get(i).isClient()) {
				r.getSubJobsList().get(i).setdepartureTime(r.getSubJobsList().get(i).getstartServiceTime()+test.getloadTimeHomeCareStaff());
			}
			else {
				r.getSubJobsList().get(i).setdepartureTime(r.getSubJobsList().get(i).getstartServiceTime()+test.getloadTimePatient());
			}
			r.getSubJobsList().get(i).setarrivalTime(arrivalTime);
			r.getSubJobsList().get(i).setWaitingTime(arrivalTime, serviceTime);
		}
		System.out.println(r.toString());
	}


	private boolean iterateOverRouteFutureClient(Jobs j, Route r) {
		boolean inserted=false;
		if(enoughCapacityForNewJob(r,j)) {
			for(int i=0;i<r.getSubJobsList().size();i++) {
				if(!r.getSubFutureJobsList().containsKey(j.getId())) {
					Jobs inRoute=r.getSubJobsList().get(i);
					if(i==r.getSubJobsList().size()-1 ) {
						inserted=insertionLaterVehicle(inRoute,j);//(inRoute)******(j)
						checkingCapacityRouteLastJob(r,j, inserted);
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
							checkingCapacityRouteEarlyJob(r,j, inserted);
							if(inserted) {
								r.getSubJobsList().add(0,j);
							}
						}
						if(!inserted) {
							Jobs inRouteK=r.getSubJobsList().get(i+1);
							inserted=insertedMiddleVehicle(inRoute,j,inRouteK);// (inRoute)*******(j)******(inRouteK)
							checkingCapacityRouteIntermediateJob(r,i+1,j, inserted);
							if(inserted) {
								r.getSubJobsList().add(i+1,j);
								System.out.println(r.toString());
								updateRouteTimes(r);
								System.out.println(r.toString());
							}
						}
					}
					if(inserted) {
						r.updatingJobsFutureList(r,j);}
				}
			}}
		updateRouteTimes(r);
		return inserted;
	}

	private boolean iterateOverRouteClient(Jobs j, Route r) {
		boolean inserted=false;
		if(enoughCapacityForNewJob(r,j)) {
			for(int i=0;i<r.getSubJobsList().size();i++) {
				if(!r.getJobsDirectory().containsKey(j.getId())) {
					Jobs inRoute=r.getSubJobsList().get(i);
					if(i==r.getSubJobsList().size()-1 ) {
						inserted=insertionLaterVehicle(inRoute,j);//(inRoute)******(j)
						checkingCapacityRouteLastJob(r,j, inserted);
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
							checkingCapacityRouteEarlyJob(r,j, inserted);
							if(inserted) {
								r.getSubJobsList().add(0,j);
							}
						}
						if(!inserted) {
							Jobs inRouteK=r.getSubJobsList().get(i+1);
							inserted=insertedMiddleVehicle(inRoute,j,inRouteK);// (inRoute)*******(j)******(inRouteK)
							checkingCapacityRouteIntermediateJob(r,i+1,j, inserted);
							if(inserted) {
								r.getSubJobsList().add(i+1,j);
								updateRouteTW(r);
								System.out.println(r.toString());
							}
						}
					}
					r.updatingJobsList();
				}
			}}
		return inserted;
	}




	private void checkingCapacityRouteIntermediateJob(Route r, int i, Jobs j, boolean inserted) {
		// 1. copy the current route
		Route copy= new Route (r);
		// 2. insert the new job
		copy.getSubJobsList().add(i,j);
		// 3. check vehicle capacity: driving route and passengers
		inserted=enoughtCapacity(copy);
	}

	private void checkingCapacityRouteEarlyJob(Route r, Jobs j, boolean inserted) {
		// 1. copy the current route
		Route copy= new Route (r);
		// 2. insert the new job
		copy.getSubJobsList().add(0,j);
		// 3. check vehicle capacity: driving route and passengers
		inserted=enoughtCapacity(copy);
	}

	private void checkingCapacityRouteLastJob(Route r, Jobs j, boolean inserted) {
		// 1. copy the current route
		Route copy= new Route (r);
		// 2. insert the new job
		copy.getSubJobsList().add(j);
		// 3. check vehicle capacity: driving route and passengers
		inserted=enoughtCapacity(copy);
	}

	private boolean enoughCapacityForNewJob(Route r, Jobs j) {
		boolean capacity=false;
		double passengerAmount=j.getTotalPeople();
		// 1. working time 
		r.computeTravelTime(inp);
		// 2. capacity of vehicle
		r.computePassenger();
		if(Math.abs(r.getPassengers())+Math.abs(passengerAmount)<=inp.getVehicles().get(0).getMaxCapacity() && r.getTravelTime()<test.getWorkingTime()) {
			capacity =true;
		}
		return capacity;
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
					inserted=insertedMiddleJob(inRoute,j,inRouteK);// (inRoute)*******(j)******(inRouteK)
				}
			}
		}
		return inserted;
	}


	private boolean insertedMiddleVehicle(Jobs inRoute, Jobs j, Jobs inRouteK) {
		boolean inserted=false;
		double tv=computingTravelTimeWithDetour(j,inRouteK);
		double possibleArrivalTime=computeArrivalTimeAtMedicalCentreFromNextNode(j,inRouteK,tv);
		double possibleStartServiceTime=computeStartServiceAtMedicalCentre(j,possibleArrivalTime,tv);// considering the registrationTime
		double possibleDepartureTimeTime=computeDepartureTime(j,possibleArrivalTime);// considering the registrationTime
		if(possibleArrivalTime>=inRoute.getDepartureTime() && possibleDepartureTimeTime<=inRouteK.getArrivalTime() &&  possibleArrivalTime<=j.getEndTime()) {
			if(inRouteK.getstartServiceTime()>j.getstartServiceTime()) {	
				settingTimes(possibleArrivalTime,j);
				inserted=true;
			}
		}
		if(!inserted) {
			tv=computingTravelTimeWithDetour(inRoute,j);
			possibleArrivalTime=computeArrivalTimeAtMedicalCentreFromPreviousNode(inRoute,tv);
			possibleStartServiceTime=computeStartServiceAtMedicalCentre(j,possibleArrivalTime,tv);// considering the registrationTime
			possibleDepartureTimeTime=computeDepartureTime(j,possibleArrivalTime);// considering the registrationTime
			if(possibleArrivalTime>inRoute.getDepartureTime() && possibleDepartureTimeTime<=inRouteK.getArrivalTime() && possibleArrivalTime<=j.getEndTime()) {
				if(inRouteK.getstartServiceTime()>j.getstartServiceTime()) {
					settingTimes(possibleArrivalTime,j);
					inserted=true;}
			}
		}

		return inserted;
	}

	private double computeDepartureTime(Jobs j, double possibleArrivalTime) {
		double departure=possibleArrivalTime;
		if(j.isClient()) {
			departure+=test.getloadTimeHomeCareStaff();
		}
		else {
			departure+=test.getloadTimePatient();
		}
		return departure;
	}

	private boolean insertedMiddleJob(Jobs inRoute, Jobs j, Jobs inRouteK) {
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

	private boolean insertionEarlyVehicle(Jobs inRoute, Jobs j) {//(j)******(inRoute)
		boolean inserted=false;
		double tv=computingTravelTimeWithDetour(j,inRoute);
		// from the next node
		double possibleArrivalTime=computeArrivalTimeAtMedicalCentreFromNextNode(j,inRoute,tv);// considering the registrationTime;
		double possibleStartServiceTime=computeStartServiceAtMedicalCentre(j,possibleArrivalTime,tv);// considering the registrationTime
		if(possibleStartServiceTime<=j.getEndTime()) { // se considera menor para considerar los tiempos de espera
			if(inRoute.getstartServiceTime()>=j.getStartTime() || inRoute.getstartServiceTime()>=j.getEndTime()) {
				settingTimes(possibleArrivalTime,j);
				inserted=true;
			}
		}
		return inserted;
	}


	private double computeArrivalTimeAtMedicalCentreFromNextNode(Jobs j, Jobs inRoute, double tv) {
		double possibleArrivalTime=inRoute.getArrivalTime()-(tv);
		if(j.isClient()) {
			possibleArrivalTime-=test.getloadTimeHomeCareStaff();
		}
		else {
			possibleArrivalTime-=test.getloadTimePatient()-test.getRegistrationTime();
		}
		return possibleArrivalTime;

	}

	private double computingTravelTimeWithDetour(Jobs inRoute, Jobs j) {
		double travelTime=Math.max((int) Math.ceil(inp.getCarCost().getCost(inRoute.getId()-1, j.getId()-1)*test.getDetour()),inp.getCarCost().getCost(inRoute.getId()-1, j.getId()-1));
		return travelTime;
	}

	private void settingTimes(double possibleArrivalTime, Jobs j) {
		// setting arrival time
		double arrivalTime=j.getstartServiceTime();
		if(j.isClient()) {
			arrivalTime=arrivalTime-test.getloadTimeHomeCareStaff();
		}
		else {
			arrivalTime=arrivalTime-test.getloadTimePatient()-test.getRegistrationTime();
		}
		j.setarrivalTime(arrivalTime);
		// setting departure time
		double departureTime=j.getArrivalTime();
		if(j.isClient()) {
			departureTime+=test.getloadTimeHomeCareStaff();
		}
		else {
			departureTime+=(test.getloadTimePatient()-test.getRegistrationTime());
		}
		j.setdepartureTime(departureTime);
		// setting service time
		double possibleStartServiceTime=j.getArrivalTime();
		if(j.isClient()) {
			possibleStartServiceTime+=test.getloadTimeHomeCareStaff();
		}
		else {
			possibleStartServiceTime+=(test.getloadTimePatient()+test.getRegistrationTime());
		}
		double serviceTime=Math.max(possibleStartServiceTime, j.getstartServiceTime());
		j.setStartServiceTime(serviceTime);
		// setting waitting time
		j.setWaitingTime(possibleStartServiceTime, j.getstartServiceTime());	
		j.setEndServiceTime(j.getstartServiceTime()+j.getReqTime());
	}

	private boolean insertionLater(Jobs inRoute,Jobs j) {//(inRoute)******(j)
		boolean inserted=false;
		double tv=computingTravelTimeWithDetour(inRoute,j);// considering the detour
		double possibleArrivalTime=inRoute.getstartServiceTime()+inRoute.getReqTime()+test.getloadTimeHomeCareStaff()+tv; // the possible arrival time have to be lower than the end time of the tw of the nodo
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
		double tv=computingTravelTimeWithDetour(inRoute,j);// considering the detour
		// from previous node inRoute
		double possibleArrivalTime=computeArrivalTimeAtMedicalCentreFromPreviousNode(inRoute,tv);// considering the registrationTime
		double possibleStartServiceTime=computeStartServiceAtMedicalCentre(j,possibleArrivalTime,tv);// considering the registrationTime
		if(possibleStartServiceTime<=j.getEndTime()) {
			if(inRoute.getstartServiceTime()<=j.getStartTime() || inRoute.getstartServiceTime()<=j.getEndTime()) {
				settingTimes(possibleArrivalTime,j);
				inserted=true;
			}
		}
		// from the node to insert node j
		if(!inserted) {// the job is not inserted
			possibleArrivalTime=computeArrivalTimeAtMedicalCentreFromCurrentNode(inRoute,j,tv);// considering the registrationTime
			possibleStartServiceTime=computeStartServiceAtMedicalCentre(j,possibleArrivalTime,tv);// considering the registrationTime
			if(possibleStartServiceTime<=j.getEndTime()) {
				if(inRoute.getstartServiceTime()<=j.getStartTime() || inRoute.getstartServiceTime()<=j.getEndTime()) {
					settingTimes(possibleArrivalTime,j);
					inserted=true;
				}
			}	
		}

		return inserted;
	}

	private double computeArrivalTimeAtMedicalCentreFromCurrentNode(Jobs inRoute, Jobs j, double tv) {
		double possibleArrivalTime=j.getstartServiceTime();
		if(j.isClient()) {
			possibleArrivalTime=j.getstartServiceTime()-test.getloadTimeHomeCareStaff();
		}
		else {
			possibleArrivalTime=j.getstartServiceTime()-test.getloadTimePatient()-test.getRegistrationTime();
		}
		return possibleArrivalTime;
	}

	private double computeStartServiceAtMedicalCentre(Jobs j, double possibleArrivalTime, double tv) {
		double possibleStartServiceTime=possibleArrivalTime;
		if(j.isClient()) {
			possibleStartServiceTime+=test.getloadTimeHomeCareStaff();
		}
		else {
			possibleStartServiceTime+=test.getRegistrationTime()+test.getloadTimePatient();
		}
		double startServiceTime=Math.max(j.getstartServiceTime(), possibleStartServiceTime);
		return startServiceTime;
	}

	private double computeArrivalTimeAtMedicalCentreFromPreviousNode(Jobs inRoute, double tv) { //(inRoute)******(j)
		double possibleArrivalTime=inRoute.getArrivalTime()+tv; 
		if(inRoute.getTotalPeople()>0) { // the load time here is for the node which are already in the route
			possibleArrivalTime+=test.getloadTimeHomeCareStaff();
		}
		return possibleArrivalTime;
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
					if(inp.getpatients().containsKey(c.getFuture().getId())) {
						c.getFuture().setPatient(true);}
					if(inp.getMedicalCentre().containsKey(c.getFuture().getId())) {
						c.getPresent().setMedicalCentre(true);}
					subJobspatients.add(c);
				}
				if(c.getQualification()==qualification && qualification==1) {
					c.getFuture().setClient(true);
					c.getPresent().setClient(true);
					subJobsLowestQualification.add(c);
				}
				if(c.getQualification()==qualification && qualification==2) {
					c.getFuture().setClient(true);
					c.getPresent().setClient(true);
					this.subJobsMediumQualification.add(c);
				}
				if(c.getQualification()==qualification && qualification==3) {
					c.getFuture().setClient(true);
					c.getPresent().setClient(true);
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
		}




	}









}

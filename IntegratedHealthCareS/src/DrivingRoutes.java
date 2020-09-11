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
	private HashMap<String, SubJobs>assignedJobs=new HashMap<String, SubJobs>();
	private HashMap<Integer, Jobs>jobsVehicle=new HashMap<Integer, Jobs>();
	private HashMap<Integer, Jobs>checkedFutureJobs=new HashMap<Integer, Jobs>();
	ArrayList<ArrayList<SubJobs>> schift= new ArrayList<>();
	ArrayList<ArrayList<SubJobs>> qualificationParamedic= new ArrayList<>(); // turn for paramedics
	ArrayList<ArrayList<SubJobs>> qualification1= new ArrayList<>();  // turn for home care staff 1
	ArrayList<ArrayList<SubJobs>> qualification2= new ArrayList<>();  // turn for home care staff 2
	ArrayList<ArrayList<SubJobs>> qualification3= new ArrayList<>();  // turn for home care staff 2

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
		// Solution < set routes. 
		// Route<- partial shifts. 
		// Shifts <- set of jobs (working hours, qualification, tw)
		// Shifts <- set of subjobs (pick up and drop-off: 1/2 people at a location:medical centre/patient home/ client home / depot )
		// 1. Initial feasible solution
		initialSol= createInitialSolution();



		// TO DO 2. VNS
		// TO DO Local search



	}

	private Solution createInitialSolution() {
		// creationRoutes(); // create as many routes as there are vehicles
		// iteratively insert couples - here should be a destructive and constructive method 
		ArrayList<ArrayList<Couple>> clasification= clasificationjob(); // classification according to the job qualification
		// Los tiempos de las ventanas de tiempo son menores que la hora de inicio del servicio porque considera el tiempo del registro y el tiempo de carga y descarga del personal
		settingStartServiceTime(); // late time - the start service time is fixed for the jobs which have a hard time window
		assigmentJobsToQualifications(clasification);
		//settingAssigmentSchift(clasification); // Create a large sequence of jobs-  the amount of sequences depende on the synchronization between time window each jobs - it does not consider the working hours of the personal- here is only considered the job qualification
		insertingDepotConnections();
		//		crossingBetweenRoute();
		//		patientVehicleAssigment(); // assigment to the vehicle
		//
		//		clientVehicleAssigment();
		//		clientVehicleAssigmentTW();
		//		asignmentFutureJobs(); // FUTURE JOBS!!
		Solution initialSol= solutionInformation();

		return initialSol;
	}


	private Solution solutionInformation() {
		Solution initialSol= new Solution();
		for(Route r:routeList ) {
			if(!r.getSubJobsList().isEmpty()) {
				r.updateRoute(inp);
				initialSol.getRoutes().add(r);
			}
		}
		// Computar costos asociados a la solucion
		computeSolutionCost(initialSol);

		return initialSol;
	}

	private void computeSolutionCost(Solution initialSol) {
		// sólo se considera la driving route
		// update each route
		// 1. Compute waiting time
		double waiting=0;
		double serviceTime=0;
		double drivingTime=0;
		int passengers=0;
		for(Route r:initialSol.getRoutes()) {
			r.updateRoute(inp);
			waiting=r.getWaitingTime(); // waiting time
			serviceTime=r.getServiceTime(); // 2. Service time 
			drivingTime+=r.getTravelTime(); // 3. Travel time
			passengers+=r.getPassengers();// 4. Passengers
		}
		// 3. Setting values to a solution
		initialSol.setServiceTime(serviceTime);
		initialSol.setWaitingTime(waiting);
		initialSol.setdrivingTime(drivingTime);
		initialSol.setDurationSolution(waiting+drivingTime+serviceTime);
		initialSol.setPassengers(passengers);

	}

	private void crossingBetweenRoutes() {
		// seleccionar la primera ruta
		for(Route r1:this.routeList ) {
			// Split in logical parts - vehicle is empty
			ArrayList<ArrayList<SubJobs>> parts=extractingParts(r1);
			for(ArrayList<SubJobs> part:parts) {
				assigningPartsToVehicles(part);
			}
		}
	}



	private void assigningPartsToVehicles(ArrayList<SubJobs> part) {
		HashMap<Integer,SubJobs> positionJobs= new HashMap<Integer,SubJobs>();
		Route r= new Route();	
		for(int i=1;i<part.size();i++) {
			SubJobs j=part.get(i);
			int position=-1;
			for(Route r2:this.routeList ) {
				// shift(r1)--------shift(r2)
				double gap=Math.abs(j.getDepartureTime()-r2.getSubJobsList().get(0).getDepartureTime());
				if(r2.getJobsDirectory().containsKey(j.getSubJobKey()) && gap<test.getWorkingTime()) {
					position=iterateOverRoute(j,r2);
				}
				if(position>0) {
					positionJobs.put(position, j);
				}
				if(positionJobs.size()==part.size()) {
					r=copyRoute(r2);
					r2.getSubJobsList().clear();
					break;
				}
			}	
		}

		for(Route r2:this.routeList) {
			if(r2.getSubJobsList().isEmpty()) {
				for(SubJobs j:r.getSubJobsList()) {
					r2.getSubJobsList().add(j);
				}
				r2.updateRoute(inp);
			}
		}
	}

	private ArrayList<ArrayList<SubJobs>> extractingParts(Route r1) {
		ArrayList<ArrayList<SubJobs>>parts=new ArrayList<ArrayList<SubJobs>>();
		ArrayList<SubJobs> firstPartR1=new ArrayList<SubJobs>();
		parts.add(firstPartR1);
		int passenger=0;
		for(SubJobs j:r1.getSubJobsList()) {
			passenger+=j.getTotalPeople();
			firstPartR1.add(j);
			if(passenger==0) {
				firstPartR1=new ArrayList<SubJobs>();
				parts.add(firstPartR1);				
			}
		}
		return parts;
	}

	private void assigmentJobsToQualifications(ArrayList<ArrayList<Couple>> clasification) { 
		// solo la asignación de trabajos a los niveles de calificaciones // se hace una secuencia ordenad_
		//  no se consideran los downgradings solo se asignan tareas de acuerdo con la compatibilidad de
		// de las ventanas de tiempo
		// 1. Clasification de trabajos de acuerdo a las qualificaciones
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
		//	ArrayList<ArrayList<Jobs>> qualification3= assigmentHighQualification(q3,clasification3);

		//Qualification level =0
		ArrayList<ArrayList<SubJobs>> qualification0= assigmentParamedic(q0,clasification0);

		//Qualification level from 1 to 3
		ArrayList<ArrayList<SubJobs>> qualification1= assigmentParamedic(q1,clasification1);
		ArrayList<ArrayList<SubJobs>> qualification2= assigmentParamedic(q2,clasification2);
		ArrayList<ArrayList<SubJobs>> qualification3= assigmentParamedic(q3,clasification3);

		// 4. Savings los turnos
		// paramedics
		for(ArrayList<SubJobs> schifts:qualification0) {
			if(!schifts.isEmpty()) {
				schift.add(schifts);
			}
		}
		// home care staff
		for(ArrayList<SubJobs> schifts:qualification1) {
			if(!schifts.isEmpty()) {
				schift.add(schifts);
			}
		}

		for(ArrayList<SubJobs> schifts:qualification2) {
			if(!schifts.isEmpty()) {
				schift.add(schifts);
			}
		}
		for(ArrayList<SubJobs> schifts:qualification3) {
			if(!schifts.isEmpty()) {
				schift.add(schifts);
			}
		}
		System.out.println("all turns");

	}












	private void insertingDepotConnections() {
		// 1. each turn is a route
		for(ArrayList<SubJobs> turn:schift ) { // cada turno se convertira en una ruta
			makeTurnInRoute(turn);
		}
		// 2. compute the the start and end time of route
		timeStartEndRoutes();
		//3. compute the connections between SubJobs
		settingEdges();

	}

	private void settingEdges() {
		for(Route r:routeList) {
			for(int i=1;i<r.getSubJobsList().size()-1;i++) {
				SubJobs previous=r.getSubJobsList().get(i-1);
				SubJobs job=r.getSubJobsList().get(i);
				Edge e=new Edge(previous,job,inp);
				r.getEdges().put(e.getEdgeKey(), e);

			}
		}
	}

	private void timeStartEndRoutes() {
		// 1. computing times Route
		for(Route r:this.routeList) {
			//1. start time
			SubJobs firstJob=r.getSubJobsList().get(1); // it is not the depot node
			computeStartTimeRoute(firstJob,r);
			// end time
			SubJobs lastJob=r.getSubJobsList().get(r.getSubJobsList().size()-2);  // it is not the depot node
			computeEndTimeRoute(lastJob,r);
		}
	}

	private void computeEndTimeRoute(SubJobs lastJob, Route r) {
		// 1. Compute travel time
		SubJobs depot=r.getSubJobsList().getLast();
		double tv=inp.getCarCost().getCost(lastJob.getId()-1, depot.getId()-1);
		double arrivalTime=	lastJob.getDepartureTime()+tv;
		depot.setarrivalTime(arrivalTime);
		depot.setdepartureTime(arrivalTime);
		depot.setdepartureTime(arrivalTime);
		depot.setStartServiceTime(arrivalTime);
		depot.setEndServiceTime(arrivalTime);
	}

	private void computeStartTimeRoute(SubJobs firstJob, Route r) {
		// 1. Compute travel time
		SubJobs depot=r.getSubJobsList().getFirst();
		double tv=inp.getCarCost().getCost(depot.getId()-1,firstJob.getId()-1);
		double arrivalTime=	firstJob.getArrivalTime()-tv;
		depot.setarrivalTime(arrivalTime);
		depot.setdepartureTime(arrivalTime);
		depot.setStartServiceTime(arrivalTime);
		depot.setEndServiceTime(arrivalTime);
	}

	private void makeTurnInRoute(ArrayList<SubJobs> turn) { // crea tantas rutas como son posibles
		//	routeList
		SubJobs depotStartNode=new SubJobs(inp.getNodes().get(0));
		depotStartNode.setTotalPeople(1);
		SubJobs depotEndNode=new SubJobs(inp.getNodes().get(0));
		depotEndNode.setTotalPeople(-1);
		// create a Route
		Route r= new Route();
		routeList.add(r);
		r.getSubJobsList().add(depotStartNode);
		// 1. Insert the depot node as the initial node
		double passengers=1;
		for(SubJobs sj:turn) {
			passengers+=sj.getTotalPeople();
			if(passengers!=0) {
				r.getSubJobsList().add(sj);
			}

			if(passengers==0) {// here is logic close a route
				r.getSubJobsList().add(sj);
				r.updateRoute(inp);
			}
			if(r.getDurationRoute()>=test.getWorkingTime()){
				r.getSubJobsList().add(depotEndNode);
				r.updateRoute(inp);
				r= new Route();
				routeList.add(r);
				r.getSubJobsList().add(depotStartNode);
			}
		}
		// 2. Close the route - el cierre de la ruta tiene que ser lógico no se puede cerrar la ruta si el paciente esta en el vehículo
		if(!r.getSubJobsList().getLast().getSubJobKey().equals(depotEndNode.getSubJobKey())) {
			r.getSubJobsList().add(depotEndNode);
			r.updateRoute(inp);
		}

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
		////*//////*///


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
		ArrayList<ArrayList<SubJobs>> qualification0= assigmentParamedic(q0,clasification0);

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

	private ArrayList<ArrayList<SubJobs>> assigmentHighQualification(int q3, ArrayList<Jobs> clasification3) {
		for(int i=0;i<q3;i++) { // generación de copia de los trabajos
			ArrayList<SubJobs> schift=new ArrayList<>();
			qualificationParamedic.add(schift);
		}
		for(Jobs j:clasification3) { // iterate over jobs para determinar cual es la secuencia
			if(!assignedJobs.containsKey(j.getId())) {
				for(ArrayList<SubJobs> homeCare:qualificationParamedic) {
					boolean insertion=possibleInsertion(j,homeCare);
					if(insertion) {
						assignedJobs.put(j.getId(), j);

						break;
					}
				}
			}
		}
		System.out.println("Stop");
		return qualification3;
	}



	private ArrayList<ArrayList<SubJobs>> assigmentParamedic(int q3, ArrayList<Jobs> clasification3) { // return the list of subJobs
		qualificationParamedic= new ArrayList<> (q3);
		for(int i=0;i<q3;i++) {
			ArrayList<SubJobs> schift=new ArrayList<>();
			qualificationParamedic.add(schift);
		}
		for(Jobs j:clasification3) { // iterate over jobs
			for(ArrayList<SubJobs> paramedic:qualificationParamedic) {
				System.out.println(" Turn ");
				printing(paramedic);
				boolean insertion=possibleInsertion(j,paramedic);
				if(insertion) {
					break;
				}
			}
		}
		return qualificationParamedic;
	}


	private int computePassengersInVehicel(ArrayList<SubJobs> subJobsList) {
		int passengerTotal=0;
		for(SubJobs j:subJobsList) {
			passengerTotal+=j.getTotalPeople();
		}

		return passengerTotal;
	}

	private void printing(ArrayList<SubJobs> paramedic) {
		for(SubJobs j:paramedic) {
			System.out.println(j.getSubJobKey()+" "+ j.getstartServiceTime());
		}

	}

	private boolean possibleInsertion(Jobs j, ArrayList<SubJobs> homeCare) { // si el trabajo se acepta para ser insertado- entonces el trabajo se desagrega en subtrabajos
		boolean inserted=false;
		if(homeCare.isEmpty()) {
			insertionJob(j,homeCare);
			printing(homeCare);
			inserted=true;
		}
		else { // inside the array there are more jobs
			//int position=iterateOverSchift(j,homeCare);
			int position=iterateOverSchiftLastPosition(j,homeCare);
			if(position>=0) {
				inserted=true;
				insertingSubJobsParamedic(position,j,homeCare); // i <- is the position j <- is the job to be inserted
			}
		}
		return inserted;
	}

	private void insertionJob(Jobs j, ArrayList<SubJobs> homeCare) {
		if(j.isClient()) { // revisar si solo los trabajos j se realizan casa de clientes o en medical centres
			ArrayList<SubJobs> dropOffPickUp=splitClientJobInSubJobs(j);
			insertingSubJobsIntheSequence(homeCare,dropOffPickUp); // la asignación de la lista de subjobs no necesariamente se hace dentro de un mismo turno
			j.setarrivalTime(j.getstartServiceTime()-test.getloadTimeHomeCareStaff());
		}
		else { // the job j have to be a medical centre
			ArrayList<SubJobs> pickUpDropOff=splitPatientJobInSubJobs(j);
			insertingSubJobsIntheSequence(homeCare,pickUpDropOff); // la asignación de la lista de subjobs no necesariamente se hace dentro de un mismo turno

			j.setarrivalTime(j.getstartServiceTime()-test.getloadTimePatient()-test.getRegistrationTime());
		}
	}

	private void insertingSubJobsIntheSequence(ArrayList<SubJobs> homeCare, ArrayList<SubJobs> dropOffPickUp) {
		for(SubJobs sb:dropOffPickUp ) {
			String key=creatingKey(sb);
			assignedJobs.put(key, sb);
			homeCare.add(sb);
			settingTimes(0,sb);
		}



	}

	private String creatingKey(SubJobs sb) {
		String key="";
		if(sb.getTotalPeople()>0) {
			key="P"+sb.getId();
		}
		else {
			key="D"+sb.getId();
		}
		if(sb.isMedicalCentre()) {
			key=key+sb.getIdUser();
		}
		return key;
	}

	private ArrayList<SubJobs> splitPatientJobInSubJobs(Jobs j) {
		ArrayList<SubJobs> subJobsList= new ArrayList<SubJobs>();// considerar el inicio y el fin del servicio
		// 2. Generation del drop off at medical centre
		Jobs medicalCentre=j;
		Jobs patient=inp.getNodes().get(j.getIdUser()-1);
		SubJobs dropOffMedicalCentre= new SubJobs(j);
		settingTimeDropOffPatientParamedicSubJob(dropOffMedicalCentre,j);


		// 1. Generation del pick up at patient home 
		SubJobs pickUpPatientHome= new SubJobs(patient);
		settingTimePickUpPatientSubJob(pickUpPatientHome,j);

		// 3. Generación del pick at medical centre
		SubJobs pickUpMedicalCentre= new SubJobs(j);
		settingTimePickUpPatientParamedicSubJob(pickUpMedicalCentre,j);

		// 4. Generación del drop-off at client home
		SubJobs dropOffPatientHome= new SubJobs(patient);
		settingTimeDropOffPatientSubJob(dropOffPatientHome,pickUpMedicalCentre);

		// 3. Addding the subjobs to the list
		subJobsList.add(pickUpPatientHome); // Se apilan por orden de sequencia
		subJobsList.add(dropOffMedicalCentre);
		subJobsList.add(pickUpMedicalCentre); // Se apilan por orden de sequencia
		subJobsList.add(dropOffPatientHome);
		return subJobsList;
	}

	private void settingTimeDropOffPatientSubJob(SubJobs dropOffPatientHome, Jobs pickUpMedicalCentre) {
		//()--------------(j)-------------(dropOffPatientHome)
		// 1. Setting the start service time -- startServiceTime
		double travel=inp.getCarCost().getCost(pickUpMedicalCentre.getId()-1, dropOffPatientHome.getId()-1); // es necesario considerar el travel time porque involucra dos locaciones
		double earlyTW=pickUpMedicalCentre.getEndTime()+test.getloadTimePatient()+travel;  // load al lugar de la cita medica
		double lateTW=earlyTW+test.getCumulativeWaitingTime();  // load al lugar de la cita medica
		dropOffPatientHome.setStartTime(earlyTW);
		dropOffPatientHome.setEndTime(lateTW);// departure from patient home - el tiempo de viaje - el tiempo necesario para cargar los pacientes al vehículo
		// modificar el tiempo requerido para el trabajo+
		dropOffPatientHome.setserviceTime(0);	
		// 1. Setting the start service time -- startServiceTime
		dropOffPatientHome.setStartServiceTime(dropOffPatientHome.getEndTime());
		// 3. Set el fin del servicio
		dropOffPatientHome.setEndServiceTime(dropOffPatientHome.getstartServiceTime()+dropOffPatientHome.getReqTime());
		// 2. Set ArrivalTime-<- la enferemera puede ser recogida una vez esta haya terminado el servicio
		dropOffPatientHome.setarrivalTime(0);
		// 5. Setting the total people (+) pick up   (-) drop-off
		dropOffPatientHome.setTotalPeople(-1);
		dropOffPatientHome.setPatient(true);



	}

	private void settingTimePickUpPatientParamedicSubJob(SubJobs pickUpMedicalCentre, Jobs j) {
		//()--------------(pickUpMedicalCentre=j)-------------()-------------()
		// 1. Setting the start service time -- startServiceTime
		double earlyTw=j.getstartServiceTime()+j.getReqTime(); // departure from patient home - el tiempo de viaje - el tiempo necesario para cargar los pacientes al vehículo
		double lateTw=earlyTw+test.getCumulativeWaitingTime(); // Considers the waiting time
		pickUpMedicalCentre.setStartTime(earlyTw);
		pickUpMedicalCentre.setEndTime(lateTw);
		// modificar el tiempo requerido para el trabajo+
		pickUpMedicalCentre.setserviceTime(0);	
		// 1. Setting the start service time -- startServiceTime
		pickUpMedicalCentre.setStartServiceTime(pickUpMedicalCentre.getEndTime());
		// 3. Set el fin del servicio
		pickUpMedicalCentre.setEndServiceTime(pickUpMedicalCentre.getstartServiceTime()+pickUpMedicalCentre.getReqTime());
		// 5. Setting the total people (+) pick up   (-) drop-off
		pickUpMedicalCentre.setTotalPeople(2);
		pickUpMedicalCentre.setMedicalCentre(true);	
	}

	private void settingTimeDropOffPatientParamedicSubJob(SubJobs dropOffMedicalCentre, Jobs j) { // hard time window
		//()-------------(dropOffMedicalCentre=j)-------------()-------------()
		// 1. Setting the start service time -- startServiceTime
		dropOffMedicalCentre.setStartServiceTime(j.getEndTime());
		// 2. Set ArrivalTime
		dropOffMedicalCentre.setarrivalTime(dropOffMedicalCentre.getstartServiceTime()-test.getloadTimePatient()-test.getRegistrationTime()); // el tiempo del registro
		dropOffMedicalCentre.setdepartureTime(dropOffMedicalCentre.getArrivalTime()+test.getloadTimePatient());
		// 3. Set el fin del servicio
		dropOffMedicalCentre.setEndServiceTime(dropOffMedicalCentre.getstartServiceTime()+dropOffMedicalCentre.getReqTime());	
		// 5. Setting the total people (+) pick up   (-) drop-off
		dropOffMedicalCentre.setTotalPeople(-2);
		dropOffMedicalCentre.setMedicalCentre(true);		
	}

	private void settingTimePickUpPatientSubJob(SubJobs pickUpPatientHome, Jobs j) { // j <- es el nodo en donde se tiene la cita medica
		//(pickUpPatientHome)--------------(j)-------------()-------------()
		// 1. Setting the start service time -- startServiceTime
		double travel=inp.getCarCost().getCost(pickUpPatientHome.getId()-1, j.getId()-1); // es necesario considerar el travel time porque involucra dos locaciones
		double arrivalTimeAtMedicalCentre=j.getstartServiceTime()-test.getloadTimePatient()-test.getRegistrationTime();  // load al lugar de la cita medica
		double departureTimeFromPatientHome=arrivalTimeAtMedicalCentre-travel-test.getloadTimePatient(); // departure from patient home - el tiempo de viaje - el tiempo necesario para cargar los pacientes al vehículo
		pickUpPatientHome.setStartTime(departureTimeFromPatientHome); // setting time window
		pickUpPatientHome.setEndTime(departureTimeFromPatientHome);
		// modificar el tiempo requerido para el trabajo+
		pickUpPatientHome.setserviceTime(0);	
		// 1. Setting the start service time -- startServiceTime
		pickUpPatientHome.setStartServiceTime(pickUpPatientHome.getEndTime());
		// 3. Set el fin del servicio
		pickUpPatientHome.setEndServiceTime(pickUpPatientHome.getstartServiceTime()+pickUpPatientHome.getReqTime());
		// 2. Set ArrivalTime-<- la enferemera puede ser recogida una vez esta haya terminado el servicio
		pickUpPatientHome.setarrivalTime(0);
		// 5. Setting the total people (+) pick up   (-) drop-off
		pickUpPatientHome.setTotalPeople(1);
		pickUpPatientHome.setPatient(true);
	}

	private ArrayList<SubJobs> splitClientJobInSubJobs(Jobs j) {
		ArrayList<SubJobs> subJobsList= new ArrayList<SubJobs>();// considerar el inicio y el fin del servicio
		// 1. Generación del drop-off job
		SubJobs dropOff= new SubJobs(j);
		// 2. Generación del pick-up job
		SubJobs pickUp= new SubJobs(j.getsubJobPair());
		settingTimeClientSubJob(dropOff,pickUp);
		// 3. Addding the subjobs to the list
		subJobsList.add(dropOff); // Se apilan por orden de sequencia
		subJobsList.add(pickUp);
		return subJobsList;
	}

	private void settingTimeClientSubJob(SubJobs dropOff, SubJobs pickUp) {
		homeCareDropOff(dropOff);
		homeCarePickUp(dropOff,pickUp);
	}

	private void homeCarePickUp(SubJobs dropOff, SubJobs pickUp) {
		// Setting the TW
		double earlyTimeWindow=dropOff.getstartServiceTime()+dropOff.getReqTime(); // la ventana de tiempo más temprana para ser recogido 
		double lateTimeWindow=earlyTimeWindow+ test.getCumulativeWaitingTime(); // la ventana de tiempo más temprana para ser recogido <- se consideran los tiempos de espera
		pickUp.setStartTime(earlyTimeWindow);
		pickUp.setEndTime(lateTimeWindow);
		// modificar el tiempo requerido para el trabajo+
		pickUp.setserviceTime(0);	
		// 1. Setting the start service time -- startServiceTime
		pickUp.setStartServiceTime(pickUp.getEndTime());
		// 3. Set el fin del servicio
		pickUp.setEndServiceTime(pickUp.getstartServiceTime()+pickUp.getReqTime());
		// 2. Set ArrivalTime-<- la enferemera puede ser recogida una vez esta haya terminado el servicio
		pickUp.setarrivalTime(0);
		// 5. Setting the total people (+) pick up   (-) drop-off
		pickUp.setTotalPeople(1);
		pickUp.setClient(true);		
	}


	private void homeCareDropOff(SubJobs dropOff) {
		// 1. Setting the start service time -- startServiceTime
		dropOff.setStartServiceTime(dropOff.getEndTime());
		// 2. Set ArrivalTime
		dropOff.setarrivalTime(dropOff.getstartServiceTime()-test.getloadTimeHomeCareStaff());
		// 3. Set el fin del servicio
		dropOff.setEndServiceTime(dropOff.getstartServiceTime()+dropOff.getReqTime());	
		// 5. Setting the total people (+) pick up   (-) drop-off
		dropOff.setTotalPeople(-1);
		dropOff.setClient(true);		
	}

	private boolean enoughWorkingHours(ArrayList<Jobs> homeCare) {
		boolean enough=false;
		int idLastJobs=homeCare.size()-1;
		Jobs depot=inp.getNodes().get(0);
		Jobs firstJob=homeCare.get(0);
		Jobs lastJob=homeCare.get(idLastJobs);
		double departureTimeToTheDepot= computingStartWorkingDay(depot,firstJob);// computing the travel time from
		double arrivalTimeToTheDepot=computingEndWorkingDay(lastJob,depot);
		double workingTime=arrivalTimeToTheDepot-departureTimeToTheDepot;
		if(workingTime<test.getWorkingTime()) {
			enough=true;
		}
		return enough;
	}

	private double computingEndWorkingDay(Jobs lastJob, Jobs depot) {
		double travelTime=inp.getCarCost().getCost(lastJob.getId()-1,depot.getId()-1);
		double loadTime=0;
		if(lastJob.isClient()) {
			loadTime=test.getloadTimeHomeCareStaff();
		}
		else {
			loadTime=test.getloadTimePatient();

		}
		double arrivalTimeToTheDepot= lastJob.getendServiceTime()+travelTime+loadTime; // cuando sale el personal del deposito es cuando empieza a contar el tiempo

		return arrivalTimeToTheDepot;
	}

	private double computingStartWorkingDay(Jobs depot, Jobs firstJob) {
		double travelTime=inp.getCarCost().getCost(depot.getId()-1,firstJob.getId()-1);
		double loadTime=0;
		if(firstJob.isClient()) {
			loadTime=test.getloadTimeHomeCareStaff();
		}
		else {
			loadTime=test.getloadTimePatient()+test.getRegistrationTime();

		}
		double arrivalTimeToTheNode= firstJob.getstartServiceTime()-loadTime;
		double departureTimeToTheDepot= arrivalTimeToTheNode-travelTime; // cuando sale el personal del deposito es cuando empieza a contar el tiempo
		return departureTimeToTheDepot;
	}

	private int iterateOverRoute(SubJobs j, Route r) {
		int position=-1;
		boolean inserted=false;
		for(int i=0;i<r.getSubJobsList().size();i++) {
			if(!r.getJobsDirectory().containsKey(j.getIdUser())) {
				SubJobs inRoute=r.getSubJobsList().get(i);
				if(i==r.getSubJobsList().size()-1) {
					inserted=insertionLaterVehicle(inRoute,j);//(inRoute)******(j)
					if(inserted) {
						position=r.getSubJobsList().size();
					}
					else{
						if(i==0) {
							inserted=insertionEarlyVehicle(inRoute,j);//(j)******(inRoute)
							if(inserted) {
								position=0;
							}	
						}					
					}
				}
				else {// // (inRoute)*******(j)******(inRouteK)
					if(i==0) {
						inserted=insertionEarlyVehicle(inRoute,j);//(j)******(inRoute)
						if(inserted) {
							position=0;

						}
					}
					if(!inserted) {
						SubJobs inRouteK=r.getSubJobsList().get(i+1);
						inserted=insertedMiddleVehicle(inRoute,j,inRouteK);// (inRoute)*******(j)******(inRouteK)
						if(inserted) {
							position=i+1;
						}
					}
				}
				r.updatingJobsList();
			}
		}
		return position;
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

	private int iterateOverSchift(Jobs j, ArrayList<SubJobs> homeCare) {
		System.out.println("Job to insert "+ j.getId()+" "+ j.getSubJobKey()+" "+ j.getstartServiceTime());
		boolean inserted=false;
		int position=-1;
		// se evalua inser trabajo por trabajo - Tan pronto sea posible insertar el trabajo se para la iteración sobre el turno y se inserta
		//ArrayList<SubJobs> homeCare=copyListJobs(currentJobs);
		for(int i=0;i<homeCare.size();i++) { 
			SubJobs inRoute=homeCare.get(i);
			if(i==homeCare.size()-1 ) {
				inserted=insertionLater(inRoute,j);//(inRoute)******(j)
				if(!inserted) {
					inserted=insertionEarly(inRoute,j);//(j)******(inRoute)
					if(inserted) {
						position=i-1;	
						break;
					}
				}
				else {
					position=i+1;
					break;
				}
			}
			else {// // (inRoute)*******(j)******(inRouteK)
				if(i==0) {
					inserted=insertionEarly(inRoute,j);//(j)******(inRoute)
					if(inserted) {
						position=0;
						break;
					}
				}
				if(!inserted) {
					Jobs inRouteK=homeCare.get(i+1);
					inserted=insertedMiddleJob(inRoute,j,inRouteK);// (inRoute)*******(j)******(inRouteK)
					if(inserted) {
						position=i+1;
						break;
					}
				}
			}
		}
		return position;
	}

	private int iterateOverSchiftLastPosition(Jobs j, ArrayList<SubJobs> homeCare) {
		System.out.println("Job to insert "+ j.getId()+" "+ j.getSubJobKey()+" "+ j.getstartServiceTime());
		boolean inserted=false;
		int position=-1;
		// se evalua inser trabajo por trabajo - Tan pronto sea posible insertar el trabajo se para la iteración sobre el turno y se inserta
		//ArrayList<SubJobs> homeCare=copyListJobs(currentJobs);
		SubJobs inRoute=homeCare.get(homeCare.size()-1);
		inserted=insertionLater(inRoute,j);//(inRoute)******(j)
		if(inserted) {
			position=homeCare.size();
		}			
		return position;
	}


	private void insertingSubJobsParamedic(int i, Jobs j, ArrayList<SubJobs> homeCare) {
		// 1. split the job into subjobs
		ArrayList<SubJobs> pickUpDropOff= null; 
		if(j.isClient()) {
			pickUpDropOff=splitClientJobInSubJobs(j);}
		else {
			pickUpDropOff=splitPatientJobInSubJobs(j);
		}
		for(SubJobs sj:pickUpDropOff) {
			if(sj.getEndTime()==j.getEndTime() && sj.getStartTime()==j.getStartTime()) {
				homeCare.add(i,sj);
				printing(homeCare);
				assignedJobs.put(sj.getSubJobKey(),sj);
				break;
			}

		}
		missingSubJobs(pickUpDropOff); // try to insert the remaining subjobs
		// insert the equivalent job j in the sequence homeCare
	}

	private void missingSubJobs(ArrayList<SubJobs> pickUpDropOff) {
		for(SubJobs sj:pickUpDropOff) {
			if(!assignedJobs.containsKey(sj.getSubJobKey())) {
				iteratingOverCurrentShifts(sj);
			}

		}


	}

	private void iteratingOverCurrentShifts(SubJobs sj) {
		// ArrayList<ArrayList<SubJobs>> qualificationParamedic= new ArrayList<>(); // turn for paramedics
		int position=-1;
		for(ArrayList<SubJobs> schift:qualificationParamedic) {
			if(schift.isEmpty()) {
				insertionJob(sj,schift);
				printing(schift);
			}
			else {
				position=iterateOverSchift(sj,schift);
				//	position=iterateOverSchiftLastPosition(sj,schift);
				if(position>0) {
					schift.add(position,sj);
					printing(schift);
					assignedJobs.put(sj.getSubJobKey(),sj);
					break;
				}}
		}
	}

	private ArrayList<SubJobs> copyListJobs(ArrayList<SubJobs> currentJobs) {
		ArrayList<SubJobs> listJobs= new ArrayList<SubJobs>();
		for(SubJobs j:currentJobs) {
			listJobs.add(j);
		}
		return listJobs;
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
		double tv=inp.getCarCost().getCost(j.getId()-1,inRouteK.getId()-1);
		double possibleArrivalTime=inRouteK.getstartServiceTime()-(j.getReqTime()+tv+test.getloadTimeHomeCareStaff());
		if(possibleArrivalTime>=inRoute.getstartServiceTime() && possibleArrivalTime>0) {
			if(possibleArrivalTime<=j.getEndTime()) {
				if(j.getstartServiceTime()>=inRoute.getstartServiceTime() && j.getstartServiceTime()<=inRouteK.getstartServiceTime()) {
					inserted=true;}
			}
			else {
				tv=inp.getCarCost().getCost(inRoute.getId()-1,j.getId()-1);
				possibleArrivalTime=inRoute.getstartServiceTime()+(j.getReqTime()+tv+test.getloadTimeHomeCareStaff());
				if(possibleArrivalTime<=inRouteK.getstartServiceTime() && possibleArrivalTime>0) {
					if(possibleArrivalTime<=j.getEndTime() && possibleArrivalTime>0) {
						if(j.getstartServiceTime()>=inRoute.getstartServiceTime() && j.getstartServiceTime()<=inRouteK.getstartServiceTime()) {
							settingTimes(possibleArrivalTime,j);
							inserted=true;
						}
					}
				}
			}
		}
		else {
			tv=inp.getCarCost().getCost(inRoute.getId()-1,j.getId()-1);
			possibleArrivalTime=inRoute.getstartServiceTime()+(j.getReqTime()+tv+test.getloadTimeHomeCareStaff());
			if(possibleArrivalTime<=inRouteK.getstartServiceTime() && possibleArrivalTime>0) {
				if(possibleArrivalTime<=j.getEndTime() && possibleArrivalTime>0) {
					if(j.getstartServiceTime()>=inRoute.getstartServiceTime() && j.getstartServiceTime()<=inRouteK.getstartServiceTime()) {
						settingTimes(possibleArrivalTime,j);
						inserted=true;
					}
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
			if(inRoute.getstartServiceTime()>=j.getstartServiceTime()) {
				settingTimes(possibleArrivalTime,j);
				inserted=true;
			}
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
			departureTime+=(test.getloadTimePatient());
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
			System.out.println("\n Couples information \n");
			System.out.println("\n Couple information "+ couple.toString()+"\n");
			System.out.println("Stop");
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

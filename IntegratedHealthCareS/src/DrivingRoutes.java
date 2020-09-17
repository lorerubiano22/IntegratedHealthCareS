import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

public class DrivingRoutes {

	private Inputs inp; // input problem
	private Test test; // input problem
	private Random rn;
	// Routes
	private  ArrayList<Route> routeList= new ArrayList<Route>(); // dummy routes
	private  HashMap<Integer, Jobs> subJobs= new HashMap<>();
	// variables para definir al ruta de un vehículo
	private HashMap<String, ArrayList<SubJobs>>assignedShiftParts=new HashMap<>();
	private  ArrayList<Route> routeVehicleList= new ArrayList<Route>(); // dummy routes

	///
	private  ArrayList<Couple> subJobsList= new ArrayList<Couple>();
	private  ArrayList<Couple> subJobsHighestQualification= new ArrayList<Couple>();
	private  ArrayList<Couple> subJobsMediumQualification= new ArrayList<Couple>();
	private  ArrayList<Couple> subJobsLowestQualification= new ArrayList<Couple>();
	private  ArrayList<Couple> subJobspatients= new ArrayList<Couple>();
	private HashMap<String, SubJobs>assignedJobs=new HashMap<String, SubJobs>();
	private HashMap<Integer, Jobs>jobsVehicle=new HashMap<Integer, Jobs>();
	private HashMap<Integer, Jobs>checkedFutureJobs=new HashMap<Integer, Jobs>();
	private ArrayList<ArrayList<SubJobs>> schift= new ArrayList<>();
	private ArrayList<Schift>splittedSchift= new ArrayList<Schift>();
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
		Solution initialSol= solutionInformation();
		splitingShift(); // saves the schift and split acording in the point where the vehicle is empty
		//downgradingsRoutes();
		crossingBetweenRoutes();
		//		patientVehicleAssigment(); // assigment to the vehicle
		//
		//		clientVehicleAssigment();
		//		clientVehicleAssigmentTW();
		//		asignmentFutureJobs(); // FUTURE JOBS!!


		return initialSol;
	}


	private void downgradingsRoutes() {
		// el objetivo es por partes hacer el merge entre los turnos controlando que la jornada laboral no exceda el máximo de horas permitidas
		ArrayList<Route> homeCareStaffRoutes = selectingHomeCareStaffRoutes();
		ArrayList<Route> q3Routes = selectingHighQualification(homeCareStaffRoutes);
		ArrayList<Route> q2Routes = selectingMediumQualification(homeCareStaffRoutes);
		ArrayList<Route> q1Routes = selectinglowQualification(homeCareStaffRoutes);
		// el objetivo es reducir el número de personas necesarias
		fromLowToHigher(q3Routes,q2Routes);
		fromLowToHigher(q3Routes,q1Routes);
		fromLowToHigher(q2Routes,q1Routes);
	}


	private void fromLowToHigher(ArrayList<Route> q3Routes, ArrayList<Route> q2Routes) {
		// cosas a cuidar la ruta 2 se intenta insertar en q2Routes
		// 1. Working hours
		boolean inserted=false;
		for(Route r2:q2Routes) {
			inserted=tryToInsertInq3(r2,q3Routes);
		}
		//2. los tiempos de espera
		// 3. Las ventanas de tiempo

	}

	private boolean tryToInsertInq3(Route r2, ArrayList<Route> q3Routes) {
		boolean inserted= false;
		ArrayList<ArrayList<SubJobs>> copy= removingDepot(r2);
		for(Route r3:q3Routes) { // r2 se intenta insertar en r1
			// Option 1--- keeping the same time
			inserted=keepingSameTime(r3,copy);
			// Option 2----- changing the time
		}
		return inserted;
	}


	private boolean keepingSameTime(Route r3, ArrayList<ArrayList<SubJobs>> copy) {

		return false;
	}

	private ArrayList<ArrayList<SubJobs>> removingDepot(Route r2) {
		ArrayList<ArrayList<SubJobs>> copy= new ArrayList<ArrayList<SubJobs>>();
		for(ArrayList<SubJobs> part:r2.getPartsRoute()) {
			ArrayList<SubJobs> newPart=onlyJobs(part);

			copy.add(newPart);
		}
		return copy;
	}

	private ArrayList<SubJobs> onlyJobs(ArrayList<SubJobs> part) {
		ArrayList<SubJobs> newPart=new ArrayList<SubJobs> ();
		if(part.get(0).getId()==1) {// depot
			for(int i=1;i<part.size();i++) {
				newPart.add(part.get(i));
			}
		}
		else {
			// check si el ultimo nodo es el depot
			int indexLast=part.size();
			if(part.get(indexLast-1).getId()==1) {
				for(int i=0;i<part.size()-1;i++) {
					newPart.add(part.get(i));
				}

			}
			else {
				for(int i=0;i<part.size();i++) {
					newPart.add(part.get(i));
				}
			}
		}
		return newPart;
	}

	private ArrayList<Route> selectinglowQualification(ArrayList<Route> routes) {
		ArrayList<Route> homeCareStaffRoutes = new ArrayList<Route>();
		for(Route r:routes ) {
			if(r.getSubJobsList().get(0).getReqQualification()==1) {
				homeCareStaffRoutes.add(r);
			}
		}
		return homeCareStaffRoutes;
	}

	private ArrayList<Route> selectingMediumQualification(ArrayList<Route> routes) {
		ArrayList<Route> homeCareStaffRoutes = new ArrayList<Route>();
		for(Route r:routes ) {
			if(r.getSubJobsList().get(1).getReqQualification()==2) {
				homeCareStaffRoutes.add(r);
			}
		}
		return homeCareStaffRoutes;
	}

	private ArrayList<Route> selectingHighQualification(ArrayList<Route> routes) {
		ArrayList<Route> homeCareStaffRoutes = new ArrayList<Route>();
		for(Route r:routes ) {
			if(r.getSubJobsList().get(1).getReqQualification()==3) {
				homeCareStaffRoutes.add(r);
			}
		}
		return homeCareStaffRoutes;
	}

	private ArrayList<Route> selectingHomeCareStaffRoutes() {
		ArrayList<Route> homeCareStaffRoutes = new ArrayList<Route>();
		for(Route r:routeList ) {
			if(r.getSubJobsList().get(1).isClient()) {
				homeCareStaffRoutes.add(r);
			}
		}
		return homeCareStaffRoutes;
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
		assigningSchiftToVehicles();

	}



	private void assigningSchiftToVehicles() {
		sortingEarlyStartShift();// sort shifts
		//ArrayList<Schift> partsToInsert=sortingEarlyEndShift();// sort shifts
		assignmentShiftVehicle(); // asignar los shifts a los vehículos  



	}

	private void assignmentShiftVehicle() {
		for(Schift turn:splittedSchift) {//1. intentar las cabezas de cada ruta
			System.out.println("turn " + turn.toString());
			insertingHeadings(turn);
			String key= "T"+turn.getId()+"P"+0;
			assignedShiftParts.put(key, turn.getRouteParts().get(0));
			// to remove
			int i=-1;
			for(Route r:routeVehicleList) {
				i++;
				System.out.println("route " + i);
				for(ArrayList<SubJobs> route: r.getPartsRoute()) {
					for(SubJobs j: route) {
						System.out.println("SubJobs "+ j.getSubJobKey()+ " arrival "+j.getArrivalTime()+ " Start Service"+ j.getStartTime());
					}
				}
			}
			System.out.println("end ");

			// to remove
		}
		for(Schift turn:splittedSchift) {//1. intentar las cabezas de cada ruta

			System.out.println("turn " + turn.toString());
			insertingParts(turn); // the heading of each route has been already inserted
		}
		// assigment turn according to early end
		sortingEarlyEndShift();// sort shifts


	}

	private void insertingParts(Schift turn) {
		boolean merging=false;
		for(Route r: routeVehicleList) { // the turn is inserted by parts
			printingPartRoute(r);
			int i=-1;
			for(ArrayList<SubJobs> part:turn.getRouteParts()) { // tan pronto se inserta una parte entonces se continua con la siguiente hasta que todos los turnos sean asignados a un vehículo
				i++;
				int sizePart=part.size();
				//if(!assignedShiftParts.containsValue(part) &&part.get(sizePart-1).getId()!=1) {
				if(!assignedShiftParts.containsValue(part)) {
					printing(part);
					String key= "T"+turn.getId()+"P"+i;
					int lastPartAssigned=r.getPartsRoute().size()-1;
					ArrayList<SubJobs> lastPart=r.getPartsRoute().get(lastPartAssigned);
					int lastSubJobAssigned=lastPart.size()-1;
					SubJobs lastSubJob= lastPart.get(lastSubJobAssigned); 	// 1. Seleccionar el ultimo trabajo de la ruta
					SubJobs firtSubJobToInsert=part.get(0); // the first job- index 0 is the depot
					merging=driverSchedulle(r,part);//check<- el primer trabajo de la ruta con respecto al ultimo trabajo de la parte no debe exceder las 8 horas de trabajo
					if(merging) {
						// intentar insertando respentando los tiempos máximos de espera.
						//1. probar insertarlo con las horas actuales del trabajo
						merging=checkingTime(r,lastSubJob,firtSubJobToInsert);
						if(merging) {			// 2. Seleccionar el primer turno del trabajo que puede ser insertado
							// 3. Revisar la factibilidad de poder combinar ambas rutas: Detour, capacidad del vehículo
							r.getPartsRoute().add(part);
							assignedShiftParts.put(key,part);
							printingPartRoute(r);
						}
						else { // changing the hour
							merging=changingTimeToTheNextJob(lastSubJob,firtSubJobToInsert,r);
							if(merging) {
								movingTimeShiftMiddlePart(firtSubJobToInsert,turn);
								//movingTimeShift(firtSubJobToInsert,turn);
								ArrayList<SubJobs> newPart1=new ArrayList<SubJobs>();
								for(int j=0;j<part.size();j++) {
									newPart1.add(part.get(j));
								}
								r.getPartsRoute().add(newPart1);
								assignedShiftParts.put(key,newPart1);
								System.out.println("final Route ");
								printingPartRoute(r);
								break;
							}
						}
					}
				}
			}
		}
	}

	private boolean driverSchedulle(Route r, ArrayList<SubJobs> part) {
		boolean workingTime=false;
		// considerar la hora en la que el vehículo llega al dept
		SubJobs lastJobsPart=part.get(part.size()-1);
		/////////////////
		// start of the route
		SubJobs startJobsRoute=r.getPartsRoute().get(0).get(0);
		// end of the route
		int longRoute=r.getPartsRoute().size();
		ArrayList<SubJobs> lastPart=r.getPartsRoute().get(longRoute-1);
		int longLastPart=lastPart.size();
		SubJobs lastJobPart=lastPart.get(longLastPart-1);
		//// travel time	
		double tv= inp.getCarCost().getCost(lastJobPart.getId()-1, 0);
		///
		double fromDepot=startJobsRoute.getArrivalTime();
		double toDepot=lastJobPart.getDepartureTime()+tv;
		if(Math.abs(toDepot-fromDepot)<=test.getWorkingTime()) {
			workingTime=true;
		}
		return workingTime;
	}

	private void insertingHeadings(Schift turn) {
		if(routeVehicleList.isEmpty()) {
			Route r= new Route();
			routeVehicleList.add(r);
			r.getPartsRoute().add(turn.getRouteParts().get(0));
			printingPartRoute(r);
		}
		else {
			mergingHeadRoutes(turn);	
		}
	}


	private void printingPartRoute(Route r) {
		for(ArrayList<SubJobs> route: r.getPartsRoute()) {
			for(SubJobs j: route) {
				System.out.println("SubJobs "+ j.getSubJobKey()+ " arrival "+j.getArrivalTime()+ " Start Service"+ j.getStartTime());
			}
		}
	}

	private void mergingHeadRoutes(Schift turn) {
		boolean merging=false;
		for(Route r: routeVehicleList) {
			int lastPartAssigned=r.getPartsRoute().size()-1;
			ArrayList<SubJobs> lastPart=r.getPartsRoute().get(lastPartAssigned);
			int lastSubJobAssigned=lastPart.size()-1;
			SubJobs lastSubJob= lastPart.get(lastSubJobAssigned); 	// 1. Seleccionar el ultimo trabajo de la ruta
			SubJobs firtSubJobToInsert=turn.getRouteParts().get(0).get(1); // the first job- index 0 is the depot
			// 1. Max detour
			merging=checkingDetour(r,lastSubJob,firtSubJobToInsert); // as soon as the maximum detour constraint is met
			if(merging) {
				// 2. Time window
				merging=checkingTime(r,lastSubJob,firtSubJobToInsert);
				if(merging) {			// 2. Seleccionar el primer turno del trabajo que puede ser insertado
					// 3. Revisar la factibilidad de poder combinar ambas rutas: Detour, capacidad del vehículo
					insertingHeadingPart(r,turn,lastSubJob);
				}
				else { // changing the hour
					merging=changingTimeToTheNextJob(lastSubJob,firtSubJobToInsert,r);
				}
			}
			if(merging) {
				// 1. Updating the shift time
				movingTimeShift(firtSubJobToInsert,turn);
				ArrayList<SubJobs> newPart1=new ArrayList<SubJobs>();
				for(int i=1;i<turn.getRouteParts().get(0).size();i++) {
					newPart1.add(turn.getRouteParts().get(0).get(i));
				}
				r.getPartsRoute().add(newPart1);
				///
				break;
			}

		}
		if(!merging){
			Route newRoute=new Route();
			newRoute.getPartsRoute().add(turn.getRouteParts().get(0));
			routeVehicleList.add(newRoute);

			System.out.println("route ");
			for(ArrayList<SubJobs> route: newRoute.getPartsRoute()) {
				for(SubJobs j: route) {
					System.out.println("SubJobs "+ j.getSubJobKey()+ " arrival "+j.getArrivalTime()+ " Start Service"+ j.getStartTime());
				}
			}	
		}
	}

	private void movingTimeShift(SubJobs firtSubJobToInsert, Schift turn) {
		// 1. Cambiar el arrival y el departure time para cada subjob
		changingArrivalDepartureTimes(firtSubJobToInsert,turn);
		// 2. cambiar la hora en la que empieza el servicion
		changingStartEndServiceTime(turn);
		//3. Compute the waiting tiem

	}

	
	private void movingTimeShiftMiddlePart(SubJobs firtSubJobToInsert, Schift turn) {
		// 1. Cambiar el arrival y el departure time para cada subjob
		changingArrivalDepartureTimesMiddleParts(firtSubJobToInsert,turn);
		// 2. cambiar la hora en la que empieza el servicion
		changingStartEndServiceTimeMiddleParts(firtSubJobToInsert,turn);
		//3. Compute the waiting tiem

	}

	private void changingStartEndServiceTime(Schift turn) {
		for(ArrayList<SubJobs> part:turn.getRouteParts()) {
			for(SubJobs subjob:part) {
				double arrivalTime=subjob.getArrivalTime();
				double additionalTime=computeAdditionalTime(subjob);
				double startServiceTime=Math.max(subjob.getStartTime(),(arrivalTime+additionalTime));	// setting start service time
				subjob.setStartServiceTime(startServiceTime);
				subjob.setEndServiceTime(startServiceTime+subjob.getReqTime());// setting end service time
				computingWaitingTime(subjob,additionalTime);
			}
		}
	}

	
	private void changingStartEndServiceTimeMiddleParts(SubJobs firtSubJobToInsert, Schift turn) {
		for(ArrayList<SubJobs> part:turn.getRouteParts()) {
			if(part.get(0).getSubJobKey().equals(firtSubJobToInsert.getSubJobKey())) {
				for(SubJobs subjob:part) {
					double arrivalTime=subjob.getArrivalTime();
					double additionalTime=computeAdditionalTime(subjob);
					double startServiceTime=Math.max(subjob.getStartTime(),(arrivalTime+additionalTime));	// setting start service time
					subjob.setStartServiceTime(startServiceTime);
					subjob.setEndServiceTime(startServiceTime+subjob.getReqTime());// setting end service time
					computingWaitingTime(subjob,additionalTime);
				}
			}
			printing(part);
			System.out.println("end" );
		}
	}
	
	
	private void computingWaitingTime(SubJobs subjob, double additionalTime) {
		if((subjob.getArrivalTime()+additionalTime)<=subjob.getstartServiceTime()-additionalTime) {
			double idealArrivalTime=subjob.getstartServiceTime()-additionalTime;
			double waitingTime=idealArrivalTime-(subjob.getArrivalTime()+additionalTime);
			subjob.setWaitingTime(waitingTime);
		}

	}

	private void changingArrivalDepartureTimes(SubJobs firtSubJobToInsert, Schift turn) {

		// computing for the first part of the shift
		computingFirstPart(firtSubJobToInsert,turn);
		// remaining part of the shift
		computingRemainingShift(turn);

		printingShift(turn);
		System.out.println("end ");

	}


	private void changingArrivalDepartureTimesMiddleParts(SubJobs firtSubJobToInsert, Schift turn) {
		// remaining part of the shift
		int refPart=0;
		for(int i=0;i<turn.getRouteParts().size();i++) {// iterar sobre las partes
			ArrayList<SubJobs> currentList= turn.getRouteParts().get(i);	// Calling the first part of the turn
			refPart++;
			if(currentList.get(0).getSubJobKey().equals(firtSubJobToInsert.getSubJobKey())) {
				computingNewTimes(firtSubJobToInsert,currentList);
break;
			}
		}
		for(int i=refPart+1;i<turn.getRouteParts().size();i++) {// iterar sobre las partes
			ArrayList<SubJobs> currentList= turn.getRouteParts().get(i);	// Calling the first part of the turn
			ArrayList<SubJobs> preList= turn.getRouteParts().get(i-1);	// Calling the first part of the turn
			int size=preList.size()-1;
			SubJobs lastSubJobs=preList.get(size);
		computingTimes(lastSubJobs,currentList);
		}
		printingShift(turn);
		System.out.println("end ");

	}


//	private void computingTimes(SubJobs lastSubJobs, ArrayList<SubJobs> currentList) {
//		double departureTimeLast=lastSubJobs.getDepartureTime();
//		for(SubJobs job:currentList) {
//			double tv=inp.getCarCost().getCost(lastSubJobs.getId()-1, job.getId()-1);
//			double arrivalTime=departureTimeLast+tv;	
//			job.setarrivalTime(arrivalTime);
//			double additionalTime=computeAdditionalTime(job);
//			departureTimeLast=arrivalTime+additionalTime;
//			job.setdepartureTime(departureTimeLast);
//		}
//	}
	
	private void computingNewTimes(SubJobs firtSubJobToInsert, ArrayList<SubJobs> currentList) {
		SubJobs job=currentList.get(0);
		job.setarrivalTime(firtSubJobToInsert.getArrivalTime());
		job.setdepartureTime(firtSubJobToInsert.getDepartureTime());
		double departureTimeLast=firtSubJobToInsert.getDepartureTime();
		for(int i=1; i<currentList.size();i++) {
			job=currentList.get(i);
		double tv=inp.getCarCost().getCost(currentList.get(i-1).getId()-1, job.getId()-1);
		double arrivalTime=departureTimeLast+tv;	
		job.setarrivalTime(arrivalTime);
		double additionalTime=computeAdditionalTime(job);
		departureTimeLast=arrivalTime+additionalTime;
		job.setdepartureTime(departureTimeLast);
	}
	printing(currentList);
	System.out.println("end" );
	}

	private void computingRemainingShiftMiddleParts(Schift turn) {
		//ArrayList<SubJobs> jobList= turn.getRouteList().getPartsRoute().get(0);	// Calling the first part of the turn
		//int size=jobList.size();
		//SubJobs lastSubJobs=jobList.get(size-1); // calling the last subjobs


	}


	private void printingShift(Schift turn) {
		int i=-1;
		for(ArrayList<SubJobs> part:turn.getRouteParts()) {
			i++;
			System.out.println("part "+i);
			for(SubJobs j:part) {
				System.out.println("Subjob "+j.getSubJobKey() +" arrival  " +j.getArrivalTime()+" service time " +j.getstartServiceTime());
			}
		}
	}

	private void computingRemainingShift(Schift turn) {
		//ArrayList<SubJobs> jobList= turn.getRouteList().getPartsRoute().get(0);	// Calling the first part of the turn
		//int size=jobList.size();
		//SubJobs lastSubJobs=jobList.get(size-1); // calling the last subjobs

		for(int i=1;i<turn.getRouteParts().size();i++) {// iterar sobre las partes
			ArrayList<SubJobs> currentList= turn.getRouteParts().get(i);	// Calling the first part of the turn
			ArrayList<SubJobs> preList= turn.getRouteParts().get(i-1);	// Calling the first part of the turn
			int size=preList.size()-1;
			SubJobs lastSubJobs=preList.get(size);
			computingTimes(lastSubJobs,currentList);
		}
	}

	private void computingTimes(SubJobs lastSubJobs, ArrayList<SubJobs> currentList) {
		double departureTimeLast=lastSubJobs.getDepartureTime();
		for(int i=1; i<currentList.size();i++) {
			SubJobs job=currentList.get(i);
			double tv=inp.getCarCost().getCost(lastSubJobs.getId()-1, job.getId()-1);
			double arrivalTime=departureTimeLast+tv;	
			job.setarrivalTime(arrivalTime);
			double additionalTime=computeAdditionalTime(job);
			departureTimeLast=arrivalTime+additionalTime;
			job.setdepartureTime(departureTimeLast);
			lastSubJobs=currentList.get(i-1);
		}
	}

	private void computingFirstPart(SubJobs firtSubJobToInsert, Schift turn) {
		ArrayList<SubJobs> jobList= turn.getRouteParts().get(0); 
		// setting the time for the first job
		SubJobs iNode=jobList.get(1);
		iNode.setarrivalTime(firtSubJobToInsert.getArrivalTime());
		iNode.setdepartureTime(firtSubJobToInsert.getDepartureTime());
		double departureTimePreviousJob=iNode.getDepartureTime();
		for(int i=2; i<jobList.size();i++){
			iNode=jobList.get(i-1);
			departureTimePreviousJob=iNode.getDepartureTime();
			SubJobs jNode=jobList.get(i);
			double tv=inp.getCarCost().getCost(iNode.getId()-1, jNode.getId()-1);
			double arrivalTime=departureTimePreviousJob+tv;
			double departure=0;
			if(jNode.isClient()) {
				departure=arrivalTime+test.getloadTimeHomeCareStaff();
			}
			else {
				departure=arrivalTime+test.getloadTimePatient();
			}
			jNode.setarrivalTime(arrivalTime);
			jNode.setdepartureTime(departure);
		}
	}

	private boolean changingTimeToTheNextJob(SubJobs lastSubJob, SubJobs firtSubJobToInsert, Route r) {
		// obj: cambiar la hora de empezar el servicio del trabajo que se va a hacer
		// 1. Desde el primer trabajo calcular el timpo de llegada
		boolean merging=false;
		double tv=inp.getCarCost().getCost(lastSubJob.getId()-1, firtSubJobToInsert.getId()-1);
		double possibleArrivalTime= computeArrivalToNexNode(lastSubJob,tv);
		double additiontalTime=computeAdditionalTime(firtSubJobToInsert); // load /unloading time, registratrion time
		if(possibleArrivalTime<=(firtSubJobToInsert.getEndTime()-additiontalTime)) { 	//2. comparar el tiempo de llegada con las ventanas de tiempo
			if(possibleArrivalTime>=firtSubJobToInsert.getStartTime()) {// inicio de la ventana de tiempo
				updatingTimeShift(firtSubJobToInsert,possibleArrivalTime);
				merging=true;
			}
			else { // checking the waiting time  quiere decir que llega más temprano que la hora de inicio del servicio
				double additionalTime=computeAdditionalTime(firtSubJobToInsert);
				if((possibleArrivalTime+additionalTime)<=firtSubJobToInsert.getStartTime()) {// se generan tiempos de espera
					double waiting=firtSubJobToInsert.getStartTime()-(possibleArrivalTime+additionalTime);
					if(waiting<=test.getCumulativeWaitingTime()) {
						updatingTimeShift(firtSubJobToInsert,possibleArrivalTime);
						merging=true;
					}
				}
				else {
					merging=true;
				}
			}
		}
		return merging;
	}

	private void updatingTimeShift(SubJobs firtSubJobToInsert, double possibleArrivalTime) {
		// setting arrival time
		firtSubJobToInsert.setarrivalTime(possibleArrivalTime);
		// setting departure time	
		double departureTime=firtSubJobToInsert.getArrivalTime();
		// 1.1 Set departure time
		if(firtSubJobToInsert.isClient()) {
			departureTime=firtSubJobToInsert.getArrivalTime()+test.getloadTimeHomeCareStaff();
		}
		else {
			departureTime=firtSubJobToInsert.getArrivalTime()+test.getloadTimeHomeCareStaff();
		}
		firtSubJobToInsert.setdepartureTime(departureTime);
		// 2. setting start service time additional 

		double additionalTime=computeAdditionalTime(firtSubJobToInsert);
		double startService=Math.max(firtSubJobToInsert.getArrivalTime()+additionalTime, firtSubJobToInsert.getStartTime());
		firtSubJobToInsert.setStartServiceTime(startService);
		// 2.1 setting end service time
		firtSubJobToInsert.setEndServiceTime(firtSubJobToInsert.getstartServiceTime()+firtSubJobToInsert.getReqTime());
		System.out.println("start service time"+ firtSubJobToInsert.getstartServiceTime());
		System.out.println("end service time"+ firtSubJobToInsert.getendServiceTime());
	}

	private void insertingHeadingPart(Route r, Schift turn, SubJobs lastSubJob) {
		// 2. inserting the jobs of the first part
		ArrayList<SubJobs> jobsList=turn.getRouteList().getPartsRoute().get(0);
		ArrayList<SubJobs> jobsListToInsert=turn.getRouteList().getPartsRoute().get(0);
		for(int job=1;job<jobsList.size();job++) {
			SubJobs j=jobsList.get(job);
			jobsListToInsert.add(j);
		}
		r.getPartsRoute().add(jobsListToInsert);
	}
	private void insertingPart(Route r, Schift turn, SubJobs lastSubJob) {
		// 2. inserting the jobs of the first part
		ArrayList<SubJobs> jobsList=turn.getRouteList().getPartsRoute().get(0);
		ArrayList<SubJobs> jobsListToInsert=turn.getRouteList().getPartsRoute().get(0);
		for(int job=0;job<jobsList.size();job++) {
			SubJobs j=jobsList.get(job);
			jobsListToInsert.add(j);
		}
		r.getPartsRoute().add(jobsListToInsert);

	}

	private boolean checkingTime(Route r, SubJobs lastSubJob, SubJobs firtSubJobToInsert) {
		boolean merging= false;
		boolean timeWindowMatch=false;
		boolean waitingTime=false;
		//1. checking the time - reach job firtSubJobToInsert from lastSubJob
		double tv=inp.getCarCost().getCost(lastSubJob.getId()-1, firtSubJobToInsert.getId()-1);
		double possibleArrivalTime= computeArrivalToNexNode(lastSubJob,tv);
		double neededArribal= computeNeededArrivalToNexNode(firtSubJobToInsert);
		if(possibleArrivalTime<=neededArribal) {	
			double additionalTime=computeAdditionalTime(firtSubJobToInsert);
			if((possibleArrivalTime+additionalTime)<firtSubJobToInsert.getStartTime()) {// se generan tiempos de espera
				double waiting=firtSubJobToInsert.getStartTime()-(possibleArrivalTime+additionalTime);
				if(waiting<=test.getCumulativeWaitingTime()) {
					waitingTime=true;
				}
			}
			else {
				waitingTime=true;
			}
		}
		if(timeWindowMatch && waitingTime) {
			merging= true;
		}
		return merging;
	}

	private boolean checkingDetour(Route r, SubJobs lastSubJob, SubJobs firtSubJobToInsert) {
		boolean detourMax=false;
		//1. checking the time - reach job firtSubJobToInsert from lastSubJob
		double distanceSoFarRoute=computeDistanceRotue(r);
		double maxDetour=computeMaxDetour(firtSubJobToInsert);
		if(maxDetour>=distanceSoFarRoute){
			detourMax=true;
		}
		return detourMax;
	}

	private double computeAdditionalTime(SubJobs firtSubJobToInsert) {
		double additionalTime=0;
		if(firtSubJobToInsert.isClient()) {
			additionalTime=test.getloadTimeHomeCareStaff();
		}
		else {
			additionalTime=test.getloadTimePatient()+test.getRegistrationTime();
		}
		return additionalTime;
	}


	private void settingTimes(SubJobs firtSubJobToInsert, double maxTime) {
		// 1. Set arrival time 
		firtSubJobToInsert.setarrivalTime(maxTime);


	}

	private double computeMaxDetour(SubJobs firtSubJobToInsert) {
		double directConnection=inp.getCarCost().getCost(0, firtSubJobToInsert.getId()-1);
		double maxDetour=directConnection*test.getDetour();
		return maxDetour;
	}

	private double computeDistanceRotue(Route r) {
		double distance=0;
		for(ArrayList<SubJobs> part:r.getPartsRoute()) {
			for(int i=1;i<part.size();i++) {
				SubJobs iNode=part.get(i-1);
				SubJobs jNode=part.get(i);
				double tv=inp.getCarCost().getCost(iNode.getId()-1, jNode.getId()-1);
				distance+=tv;
			}
		}
		return distance;
	}

	private double computeNeededArrivalToNexNode(SubJobs firtSubJobToInsert) {
		double neededArribal=0;
		int addionalTime=0;
		if(firtSubJobToInsert.isMedicalCentre()) {// 1. en le caso de que sea un centro médico
			addionalTime=test.getRegistrationTime()+test.getloadTimePatient();
		}
		else{
			if(firtSubJobToInsert.isPatient()) {// en el caso en que sea la casa de un paciente
				addionalTime=test.getloadTimePatient();
			}
			else {// es un cliente
				addionalTime=test.getloadTimeHomeCareStaff();
			}
		}
		neededArribal=firtSubJobToInsert.getstartServiceTime()-addionalTime;
		return neededArribal;
	}

	private double computeArrivalToNexNode(SubJobs lastSubJob, double tv) {
		double possibleArrivalTime=lastSubJob.getDepartureTime()+tv;
		return possibleArrivalTime;
	}

	private void sortingEarlyStartShift() {
		//private
		ArrayList<Schift>sortedSchift= new ArrayList<Schift>();
		//HashMap<Integer, Schift> sortedSchift= new HashMap<>();
		// iterando sobre todos los turnos 
		int size=0;
		int t=-1;
		for(Schift turn:splittedSchift) {
			t++;
			System.out.println("turn "+t+" "+ "Subjob "+turn.getRouteList().getSubJobsList().get(0).getSubJobKey() +"  " +turn.getRouteList().getSubJobsList().get(0).getArrivalTime()+" ");
			if(sortedSchift.isEmpty()) {
				sortedSchift.add(turn);
			}
			else {
				readingTurns(sortedSchift,turn);
			}
			size=sortedSchift.size()-1;

			/////
			for(Schift copy:sortedSchift) {
				System.out.println("copy turn "+" "+ "Subjob "+copy.getRouteList().getSubJobsList().get(0).getSubJobKey() +"  " +copy.getRouteList().getSubJobsList().get(0).getArrivalTime()+" ");

			}

		}
		splittedSchift.clear();
		t=-1;
		for(Schift turn: sortedSchift) {
			splittedSchift.add(turn);
			t++;
			System.out.println("turn "+t+" "+ "Subjob "+turn.getRouteList().getSubJobsList().get(1).getSubJobKey() +"  " +turn.getRouteList().getSubJobsList().get(1).getArrivalTime()+" ");

		}
	}

	private void sortingEarlyEndShift() {
		//private
		ArrayList<Schift>sortedSchift= new ArrayList<Schift>();
		//HashMap<Integer, Schift> sortedSchift= new HashMap<>();
		// iterando sobre todos los turnos 
		int size=0;
		int t=-1;
		for(Schift turn:splittedSchift) {
			t++;
			System.out.println("turn "+t+" "+ "Subjob "+turn.getRouteList().getSubJobsList().get(0).getSubJobKey() +"  " +turn.getRouteList().getSubJobsList().get(0).getArrivalTime()+" ");
			if(sortedSchift.isEmpty()) {
				sortedSchift.add(turn);
			}
			else {
				readingTailsTurns(sortedSchift,turn);
			}
			size=sortedSchift.size()-1;
			/////
			for(Schift copy:sortedSchift) {
				System.out.println("copy turn "+" "+ "Subjob "+copy.getRouteList().getSubJobsList().get(0).getSubJobKey() +"  " +copy.getRouteList().getSubJobsList().get(0).getArrivalTime()+" ");
			}
		}
		splittedSchift.clear();
		t=-1;
		for(Schift turn: sortedSchift) {
			splittedSchift.add(turn);
			t++;
			System.out.println("turn "+t+" "+ "Subjob "+turn.getRouteList().getSubJobsList().get(1).getSubJobKey() +"  " +turn.getRouteList().getSubJobsList().get(1).getArrivalTime()+" ");
		}
	}

	private void readingTurns(ArrayList<Schift> sortedSchift, Schift turn) {
		if(sortedSchift.size()==1) { 	// 1. si el primer trabajo ya en la lista es más tardío el el inicio del trabajo de part
			SubJobs firstsubJob=turn.getRouteList().getSubJobsList().get(0);
			SubJobs firstTurnInList=sortedSchift.get(0).getRouteList().getSubJobsList().get(0);
			if(firstTurnInList.getstartServiceTime()<=firstsubJob.getArrivalTime()) {
				sortedSchift.add(turn);
			}
			else {
				sortedSchift.add(0,turn);
			}
		}
		else {
			int lastTurn=sortedSchift.size()-1;
			SubJobs firstsubJob=turn.getRouteList().getSubJobsList().get(0);
			SubJobs firstTurnInList=sortedSchift.get(0).getRouteList().getSubJobsList().get(0);
			SubJobs lastTurnInList=sortedSchift.get(lastTurn).getRouteList().getSubJobsList().get(0);
			if(firstTurnInList.getArrivalTime()>=firstsubJob.getArrivalTime()) {
				sortedSchift.add(0,turn);
			}
			else {				
				// 1. si es más temprano que el primer trabajo
				if(lastTurnInList.getArrivalTime()<=firstsubJob.getArrivalTime()) {
					sortedSchift.add(turn);
				}
				else {
					readingArray(sortedSchift,turn);			
				}
			}
		}
	}


	private void readingTailsTurns(ArrayList<Schift> sortedSchift, Schift turn) {
		int lastJobToInsert=turn.getRouteList().getSubJobsList().size();
		SubJobs firstsubJob=turn.getRouteList().getSubJobsList().get(lastJobToInsert-1);
		int lastJobInRoute=sortedSchift.get(0).getRouteList().getSubJobsList().size();
		SubJobs firstTurnInList=sortedSchift.get(0).getRouteList().getSubJobsList().get(lastJobInRoute-1);
		if(sortedSchift.size()==1) { 	// 1. si el primer trabajo ya en la lista es más tardío el el inicio del trabajo de part
			if(firstTurnInList.getArrivalTime()<=firstsubJob.getArrivalTime()) {
				sortedSchift.add(turn);
			}
			else {
				sortedSchift.add(0,turn);
			}
		}
		else {
			int lastTurn=sortedSchift.size()-1;
			lastJobInRoute=sortedSchift.get(lastTurn).getRouteList().getSubJobsList().size();
			SubJobs lastTurnInList=sortedSchift.get(lastTurn).getRouteList().getSubJobsList().get(lastJobInRoute-1);
			if(lastTurnInList.getArrivalTime()<=firstsubJob.getArrivalTime()) {
				sortedSchift.add(turn);
			}
			else {				
				// 1. si es más temprano que el primer trabajo
				if(firstTurnInList.getArrivalTime()>=firstsubJob.getArrivalTime()) {
					sortedSchift.add(turn);
				}
				else {
					readingArray(sortedSchift,turn);			
				}
			}
		}
	}

	private void settingHeadRoutes(ArrayList<ArrayList<SubJobs>> headRoutes) {
		// where are saving the routes: routeVehicleList <-
		Route vehicle= new Route();
		for(ArrayList<SubJobs> part:headRoutes ) {
			if(vehicle.getPartsRoute().isEmpty()) {
				vehicle.getPartsRoute().add(part);
			}
			else {
				checkingCanBeInsertedAsPartRoute(vehicle,part);
			}
		}
		// 1. Seleccionar la más temprana
		// asignar la cabeza más temprana a una ruta
		// intentar asignar las otras cabezas a la ruta considerando el detour del depot
		// en caso de que no se puedan asignar se asignan a otra ruta
		/// una vez se han asignado las cabezas de las rutas se empieza a asignar las tareas que no estan relacionadas al depor
	}



	private ArrayList<ArrayList<SubJobs>> intermediatePart(ArrayList<ArrayList<SubJobs>> partsToInsert) {
		ArrayList<ArrayList<SubJobs>> middlePartes=new ArrayList<ArrayList<SubJobs>>();// order
		for(ArrayList<SubJobs> part:partsToInsert) {
			if(part.get(part.size()-1).getId()!=1 && part.get(part.size()-1).getId()!=1) {
				middlePartes.add(part);	
			}				
		}
		return middlePartes;
	}

	private ArrayList<ArrayList<SubJobs>> partConnectedToDepot(ArrayList<ArrayList<SubJobs>> partsToInsert) {
		ArrayList<ArrayList<SubJobs>> tailRoutes=new ArrayList<ArrayList<SubJobs>>();// order
		for(ArrayList<SubJobs> part:partsToInsert) {
			if(part.get(part.size()-1).getId()==1) {
				tailRoutes.add(part);	
			}				
		}
		return tailRoutes;
	}

	private ArrayList<ArrayList<SubJobs>> partConnectedFromDepot(ArrayList<ArrayList<SubJobs>> partsToInsert) {
		ArrayList<ArrayList<SubJobs>> headRoutes=new ArrayList<ArrayList<SubJobs>>();// order
		for(ArrayList<SubJobs> part:partsToInsert) {
			if(part.get(0).getId()==1) {
				headRoutes.add(part);	
			}				
		}
		return headRoutes;
	}


	private void printingTurn(Schift s) {
		int i=-1 ;// part of the turn
		for(ArrayList<SubJobs> j:s.getRouteParts()) {	
			i++;
			System.out.println("Part "+ i);
			for(SubJobs jb:j) {
				System.out.println("subjob "+ jb.getSubJobKey()+ "arival "+ jb.getArrivalTime()+ "Start_service " + jb.getstartServiceTime()+ "  " );
			}
		}

	}


	private void readingArray(ArrayList<Schift> sortedSchift, Schift turn) {
		for(int i=1;i<sortedSchift.size();i++) {
			SubJobs firstsubJob=turn.getRouteList().getSubJobsList().get(0);
			SubJobs firstTurnInList=sortedSchift.get(i).getRouteList().getSubJobsList().get(0);
			if(firstTurnInList.getArrivalTime()>=firstsubJob.getArrivalTime()) {
				SubJobs previousTurnInList=sortedSchift.get(i-1).getRouteList().getSubJobsList().get(0);
				if(previousTurnInList.getArrivalTime()<=firstsubJob.getArrivalTime()) {
					sortedSchift.add(i,turn);
					break;
				}
			}
		}
	}

	private void splitingShift() {
		int idSchift=-1;
		for(Route r1:this.routeList ) {
			idSchift++;
			Schift turn=new Schift(r1,idSchift);
			splittedSchift.add(turn);
			r1.setSchiftRoute(turn);
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
		ArrayList<ArrayList<SubJobs>> qualification1= assigmentParamedic(q1,clasification1); // here are not considering working hours
		ArrayList<ArrayList<SubJobs>> qualification2= assigmentParamedic(q2,clasification2);
		ArrayList<ArrayList<SubJobs>> qualification3= assigmentParamedic(q3,clasification3);


		//downgradings(qualification3,qualification2); //No se considera porque no es tan facil controlar el tiempo de trabajo
		//downgradings(qualification2,qualification1);


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



	private void downgradings(ArrayList<ArrayList<SubJobs>> qualification3,ArrayList<ArrayList<SubJobs>> qualification2) {
		// here both arrays are modified qualification2 and qualification22
		// 1. Copy copy both arrays
		ArrayList<ArrayList<SubJobs>> higherQualification = copyArrayOfArrays(qualification3);
		ArrayList<ArrayList<SubJobs>> lowerQualification = copyArrayOfArrays(qualification2);
		int shiftToRemove=-1;
		for(ArrayList<SubJobs> shift:lowerQualification) {
			shiftToRemove++;
			boolean inserted=tryToInsertInHigherShift(shift,qualification2);
			if(inserted) { 
				qualification2.remove(shiftToRemove);
			}

		}
		System.out.println("Stop");
	}










	private boolean tryToInsertInHigherShift(ArrayList<SubJobs> shift, ArrayList<ArrayList<SubJobs>> qualification22) {
		// check if it can be inserted after or before
		boolean inserted= false;	
		int i=-1;
		for(ArrayList<SubJobs> a:qualification22) { // reading over the high qualification level
			if(!a.isEmpty()) {
				i++;
				inserted= keepingTimes(a,shift,qualification22,i);
				if(!inserted) {
					break;
				}	
			}
		}
		return inserted;
	}


	private boolean keepingTimes(ArrayList<SubJobs> a, ArrayList<SubJobs> shift, ArrayList<ArrayList<SubJobs>> qualification22, int i) {
		boolean inserted= false;	
		// times of the high qualification level
		double startTimeHigherQualification=a.get(0).getArrivalTime();
		int indexLastJob=a.size();
		double endTimeHigherQualification=a.get(indexLastJob-1).getArrivalTime();
		// times of the low qualification level
		double startTimelowerQualification=shift.get(0).getArrivalTime();
		indexLastJob=shift.size();
		double endTimelowerQualification=shift.get(indexLastJob-1).getArrivalTime();
		if(startTimeHigherQualification<startTimelowerQualification &&  endTimeHigherQualification<endTimelowerQualification ) {// (higher qualifications)-------- (lower qualification)
			qualification22.add(shift);
			inserted=true;
		}
		else {// (lower qualifications)-------- (higher qualification)
			if(startTimeHigherQualification>startTimelowerQualification &&  endTimeHigherQualification>endTimelowerQualification ) {
				qualification22.add(i-1,shift);
				inserted=true;
			}
		}
		return inserted;
	}

	private ArrayList<ArrayList<SubJobs>> copyArrayOfArrays(ArrayList<ArrayList<SubJobs>> qualification2) {
		ArrayList<ArrayList<SubJobs>> copy= new ArrayList<ArrayList<SubJobs>> (); /// solo se copian los elementos que no estan vacios
		for(ArrayList<SubJobs> a:qualification2) {
			if(!a.isEmpty()) {
				copy.add(a);
			}
		}
		return copy;
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




	private ArrayList<Jobs> creationJobsHomeCareStaff(ArrayList<Couple> qualification) {
		ArrayList<Jobs> clasification = new ArrayList<Jobs>();
		// home care Staff
		for(Couple c:qualification) {
			c.getPresent().setTotalPeople(-1);
			clasification.add(c.getPresent());
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


	private void printing(ArrayList<SubJobs> paramedic) {
		for(SubJobs j:paramedic) {
			System.out.println(j.getSubJobKey()+" arrival "+ j.getArrivalTime()+" departure "+ j.getstartServiceTime());
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

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
	// variables para definir al ruta de un veh�culo
	private HashMap<String, Parts>assignedShiftParts=new HashMap<>();
	private  ArrayList<Route> routeVehicleList= new ArrayList<Route>(); // dummy routes
	// computing walking hours
	private double walkingTime=0;
	///
	private  ArrayList<Couple> subJobsList= new ArrayList<Couple>();
	private  ArrayList<Couple> subJobsHighestQualification= new ArrayList<Couple>();
	private  ArrayList<Couple> subJobsMediumQualification= new ArrayList<Couple>();
	private  ArrayList<Couple> subJobsLowestQualification= new ArrayList<Couple>();
	private  ArrayList<Couple> subJobspatients= new ArrayList<Couple>();
	private HashMap<String, SubJobs>assignedJobs=new HashMap<String, SubJobs>();
	private HashMap<Integer, Jobs>jobsVehicle=new HashMap<Integer, Jobs>();
	private HashMap<Integer, Jobs>checkedFutureJobs=new HashMap<Integer, Jobs>();
	private ArrayList<Parts> schift= new ArrayList<>();
	private ArrayList<Schift>splittedSchift= new ArrayList<Schift>();
	ArrayList<Parts> qualificationParamedic= new ArrayList<>(); // turn for paramedics
	ArrayList<ArrayList<SubJobs>> qualification1= new ArrayList<>();  // turn for home care staff 1
	ArrayList<ArrayList<SubJobs>> qualification2= new ArrayList<>();  // turn for home care staff 2
	ArrayList<ArrayList<SubJobs>> qualification3= new ArrayList<>();  // turn for home care staff 2

	private Solution initialSol=null;
	private Solution solution=null;


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
		solution= assigningRoutesToDrivers(initialSol);
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
		Solution initialSol= solutionInformation(); // this is the initial solution in which each personal has a vehicle assigned
		return initialSol;
	}


	private Solution assigningRoutesToDrivers(Solution initialSol) {
		Solution copySolution= new Solution(initialSol);
		changingDepartureTimes(copySolution);
		LinkedList<Parts> partsList=extractingParts(initialSol);
		System.out.println("printing initial solution");
		System.out.println(initialSol.toString());
		System.out.println("printing copy solution");
		System.out.println(copySolution.toString());
		System.out.println("end");
		// falta modificar las horas de departure de acuerdo al veh�culo y no al personal
		Solution newSol= mergingRoutes(copySolution,partsList); // las rutas se mezclan por partes
		System.out.println("printing initial solution");
		System.out.println(initialSol.toString());
		System.out.println("printing copy solution");
		System.out.println(newSol.toString());
		System.out.println("end");
		return newSol;
	}


	private void changingDepartureTimes(Solution copySolution) {
		// se cambian los departure times de cada subjob en cada parte
		for(Route r:copySolution.getRoutes()) {
			for(Parts a:r.getPartsRoute()) {
				changinPartTime(a);
			}	
			r.updateRoute(inp);
		}

	}

	private void changinPartTime(Parts a) {
		for(SubJobs j:a.getListSubJobs()) {
			// if pick up time el tiempo cuenta
			double departure=0;
			if(j.isClient()) {
				departure=j.getArrivalTime()+test.getloadTimeHomeCareStaff();
			}
			else {
				departure=j.getArrivalTime()+test.getloadTimePatient();
			}
			j.setdepartureTime(departure);
		}

	}

	private LinkedList<Parts> extractingParts(Solution initialSol2) {
		LinkedList<Parts> partsList= new LinkedList<>();
		for(Route r:initialSol2.getRoutes()) {
			for(Parts part:r.getPartsRoute()) {
				partsList.add(part);
			}
		}
		return partsList;
	}

	private Solution mergingRoutes(Solution copySolution, LinkedList<Parts> partsList) {


		Solution Sol= interMergingParts(copySolution,partsList); // 1 merging parts (without complete parts)
		updatingSolution(copySolution);
		//Solution newSol= intraMergingParts(Sol); // 2 merging parts (splited the parts)
		//updatingSolution(newSol);
		return Sol;
	}

	private Solution intraMergingParts(Solution copySolution) {
		// copiar la lista de las rutas a compiar
		LinkedList<Route> copyRoute = new LinkedList<>();
		for(Route r:copySolution.getRoutes()) {
			copyRoute.add(r);
		}
		for(Route referenceRoute:copyRoute) {
			for(Route toSplittedRoute:copyRoute) {
				if(referenceRoute!=toSplittedRoute) {


				}
			}

		}

		Solution newSol= new Solution(copySolution);
		return newSol;
	}

	private Solution interMergingParts(Solution copySolution, LinkedList<Parts> partsList) {
		// tengo que asegurar que la jornada laboral destinada al conductor sea mayor que la m�xima jornada permitida
		System.out.println(copySolution.toString());
		for(int route1=0;route1<copySolution.getRoutes().size();route1++) {
			for(int route2=0;route2<copySolution.getRoutes().size();route2++) {
				int part=0;
				int start=2;
				Route vehicle= new Route();
				Route iR=copySolution.getRoutes().get(route1);
				if(iR.getPartsRoute().size()>2) {
					System.out.println("Route iR"+ iR.toString());
					Route jR=copySolution.getRoutes().get(route2);
					System.out.println("Route jR"+ jR.toString());
					if(possibleMerge(iR,jR)) {
						Route refRoute=selecctingStartRoute(iR,jR);
						Route toInsertRoute=selecctingRouteToInsert(iR,jR);
						boolean inserted=insertionAllRoute(refRoute,toInsertRoute,vehicle,part);
						if(!inserted) { // insertion part by part
							vehicle.getPartsRoute().add(refRoute.getPartsRoute().get(0));
							vehicle.getPartsRoute().add(refRoute.getPartsRoute().get(1));
							for(part=start;part<refRoute.getPartsRoute().size();part++) {
								inserted=isertingRoute(vehicle,toInsertRoute,refRoute);
								if(!inserted) { // insert part by part
									vehicle.getPartsRoute().add(refRoute.getPartsRoute().get(part));
									vehicle.updateRoute(inp);
								}
							}
						}
						refRoute=updatingNewRoutes(refRoute,vehicle,part);	
						toInsertRoute=updatingNewRoutes(toInsertRoute,vehicle,part);	
						System.out.println("Route vehicle"+ vehicle.toString());
						routeVehicleList.add(vehicle);
					}
				}
				else {
					break;
				}
			}
			updatingListRoutes();
			System.out.println(" Route list ");
			for(Route r:routeVehicleList) {
				System.out.println(r.toString());
			}
		}
		
		return copySolution;
	}

	private void updatingSolution(Solution copySolution) {
		copySolution.getRoutes().clear();
		for(Route r:routeVehicleList) {
			copySolution.getRoutes().add(r);
		}
		computeSolutionCost(copySolution);
	}

	private boolean insertionAllRoute(Route refRoute, Route toInsertRoute, Route vehicle, int part) {
		boolean feasibleTimes= false;
		boolean workingHours=lessThanMaxWorkingHours(refRoute,toInsertRoute);
		if(workingHours) {
			System.out.print("sTOP");
			feasibleTimes=checkingTimeWindows(refRoute,toInsertRoute,vehicle,part);
		}	

		return feasibleTimes;
	}

	private boolean checkingTimeWindows(Route refRoute, Route toInsertRoute, Route vehicle, int part) {
		boolean inserted=false;
		int lastPart=refRoute.getPartsRoute().size()-1;// <- esta parte hace referencia al depot
		//	.getListSubJobs().get(0);
		Parts partEnd=refRoute.getPartsRoute().get(lastPart-1);
		SubJobs lastSubjobInRoute= partEnd.getListSubJobs().get(partEnd.getListSubJobs().size()-1);
		SubJobs firstSubjob= toInsertRoute.getPartsRoute().get(1).getListSubJobs().get(0);
		if(lastSubjobInRoute.getDepartureTime()<firstSubjob.getArrivalTime()) {
			part=refRoute.getPartsRoute().size();
			inserted=true;
			for(int j=0;j<refRoute.getPartsRoute().size()-1;j++) { // no se incluye el depot
				vehicle.getPartsRoute().add(refRoute.getPartsRoute().get(j));
			}
			for(int j=1;j<toInsertRoute.getPartsRoute().size();j++) { // no se incluye el depot
				vehicle.getPartsRoute().add(toInsertRoute.getPartsRoute().get(j));
			}
			vehicle.updateRoute(inp);
		}
		return inserted;
	}

	private boolean lessThanMaxWorkingHours(Route vehicle, Route toInsertRoute) {
		boolean inserted=false;
		SubJobs firstSubjobInRoute= vehicle.getPartsRoute().get(0).getListSubJobs().get(0);
		SubJobs lastSubjobtoInsert= toInsertRoute.getPartsRoute().get(toInsertRoute.getPartsRoute().size()-1).getListSubJobs().get(0);
		if(lastSubjobtoInsert.getArrivalTime()-firstSubjobInRoute.getDepartureTime()<test.getWorkingTime()) {
			inserted=true;
		}
		return inserted;
	}

	private void updatingListRoutes() {
		ArrayList<Route> list= gettingListOfRoutes(); // dummy routes
		for(Route r:list) {
			if(r.getPartsRoute().isEmpty()) {// 1. eliminar las rutas que no tienen partes
				routeVehicleList.remove(r);
			}

		}

		System.out.println(" step 1 ");
		for(Route r:routeVehicleList) {
			System.out.println(r.toString());
		}

		list.clear();
		for(Route r:routeVehicleList) {
			list.add(r);
		}

		routeVehicleList=removingRepeatedRoutes(list);

		System.out.println(" step 2 ");
		for(Route r:routeVehicleList) {
			System.out.println(r.toString());
		}

		System.out.println(" end ");
	}

	private ArrayList<Route> removingRepeatedRoutes(ArrayList<Route> list) {
		ArrayList<Route> cleanList= gettingListOfRoutes(); // list of clean routes
		//2. eliminar las rutas con trabajos repetidos
		for(Route r:routeVehicleList) {
			int lenghtRoute=0;
			int index=-1;
			ArrayList<Route> repeatedRoutes= checkingRepeatedRoutes(list,r); // el objetivo es que devuelva las rutas con trabajos repetidos
			if(repeatedRoutes.size()>1) {
				for(Route clone:repeatedRoutes) { // se itera sobre la lista de rutas y se seleccional la m�s larga
					list.remove(clone);
					if(clone.getSubJobsList().size()>lenghtRoute) {
						lenghtRoute=clone.getSubJobsList().size();
						index=repeatedRoutes.indexOf(clone);
					}
				}
			}
			if(index!=-1) {
				cleanList.add(repeatedRoutes.get(index));}
		}

		routeVehicleList.clear();
		for(Route r:cleanList) {
			routeVehicleList.add(r);
		}
		return routeVehicleList;
	}

	private ArrayList<Route> checkingRepeatedRoutes(ArrayList<Route> list, Route r) {
		ArrayList<Route> repeatedRoutes= new ArrayList<Route>(); 
		for(Route clone:list) {

			for(int job=1;job<clone.getSubJobsList().size()-1;job++) { // no se considera el depot
				SubJobs j=clone.getSubJobsList().get(job);
				if(r.getJobsDirectory().containsKey(j.getSubJobKey())) { //si es as� es porque la ruta clone es una copia de la ruta r
					repeatedRoutes.add(clone);
					break;
				}
			}
		}
		return repeatedRoutes;
	}

	private ArrayList<Route> gettingListOfRoutes() {
		ArrayList<Route> list= new ArrayList<Route>(); // dummy routes
		for(Route r:routeVehicleList) {
			list.add(r);
		}
		return list;
	}

	private Route updatingNewRoutes(Route refRoute, Route vehicle, int part) {
		// el objetivo es dejar que la ruta refRoute s�lo tenga partes contenidas en la ruta de vehicle

		for(int index=1;index<vehicle.getPartsRoute().size()-1;index++) {// no se considera el depot
			Parts j=vehicle.getPartsRoute().get(index);
			if(vehicle.getPartsRoute().contains(j)) {
				refRoute.getPartsRoute().remove(j);
			}
		}
		refRoute.updateRoute(inp);
		return refRoute;
	}

	private boolean isertingRoute(Route vehicle, Route toInsertRoute, Route refRoute) {
		boolean inserted=false;		
		// Este metodo intena insertar las partes de la otra ruta. Siempre evalua las rutas que ya estan integradas en otro
		Route changing= new Route(toInsertRoute);
		for(int i=1;i<toInsertRoute.getPartsRoute().size()-1;i++) { // -1 para no incluir el depot al final
			// primer intenta insertar la parte de la otra ruta
			Parts endPart=vehicle.getPartsRoute().getLast();
			SubJobs lastSubjobInRoute= endPart.getListSubJobs().get(endPart.getListSubJobs().size()-1);
			SubJobs firstSubjob= toInsertRoute.getPartsRoute().get(i).getListSubJobs().get(0);
			if(lastSubjobInRoute.getDepartureTime()<firstSubjob.getArrivalTime()) {
				vehicle.getPartsRoute().add(toInsertRoute.getPartsRoute().get(i));
				vehicle.updateRoute(inp);
				System.out.println("Printing changing 2 " +vehicle.toString());
				changing.removingParts(toInsertRoute.getPartsRoute().get(i));
				changing.updateRoute(inp);
				System.out.println("Printing changing 1 " +changing.toString());
				System.out.println("Printing vehicleRouting" +vehicle.toString());
				inserted=true;	
				break;
			}
		}
		toInsertRoute.getPartsRoute().clear();
		for(Parts part:changing.getPartsRoute()) {
			toInsertRoute.getPartsRoute().add(part);
		}
		toInsertRoute.updateRoute(inp);
		System.out.println("Printing toInsertRoute" +toInsertRoute.toString());
		return inserted; 
	}

	private Route selecctingRouteToInsert(Route iR, Route jR) {
		System.out.println("printing part IR");
		printing(iR.getPartsRoute().get(0));
		printing(iR.getPartsRoute().get(1));
		System.out.println("printing parts JR");
		printing(jR.getPartsRoute().get(0));
		printing(jR.getPartsRoute().get(1));


		if(iR.getPartsRoute().get(1).getListSubJobs().get(0).getstartServiceTime()> jR.getPartsRoute().get(1).getListSubJobs().get(0).getstartServiceTime()) {
			return iR;
		}
		else {
			return jR;
		}
	}

	private Route selecctingStartRoute(Route iR, Route jR) {
		System.out.println("printing part IR");
		System.out.println("part 1");
		printing(iR.getPartsRoute().get(0));
		System.out.println("part 2");
		printing(iR.getPartsRoute().get(1));
		System.out.println("printing parts JR");
		printing(jR.getPartsRoute().get(0));
		printing(jR.getPartsRoute().get(1));
		if(iR.getPartsRoute().get(1).getListSubJobs().get(0).getstartServiceTime()> jR.getPartsRoute().get(1).getListSubJobs().get(0).getstartServiceTime()) {
			return jR;
		}
		else {
			return iR;
		}
	}

	private boolean possibleMerge(Route iR, Route jR) {
		// Revisar que las rutas que se van a mezclar no sean las mismas rutas
		boolean routeToRemve=false;
		boolean merging=false;
		boolean diffRoute=false;
		boolean capacityVehicle=false;
		if(iR.getPartsRoute().size()>2 && jR.getPartsRoute().size()>2) {
			routeToRemve=true;
		}
		if(iR!=jR) {
			diffRoute=true;	
		}
		// Revisar que el n�mero de personas en el auto no excedan de la capacidad del
		double totalPassenger=iR.getAmountParamedic()+iR.getHomeCareStaff()+jR.getAmountParamedic()+jR.getHomeCareStaff();
		if(totalPassenger < inp.getVehicles().get(0).getMaxCapacity()) {
			capacityVehicle=true;
		}
		if(capacityVehicle && diffRoute && routeToRemve) {
			merging=true;
		}
		return merging;
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
		int routeN=-1;
		for(Route r:routeList ) {
			routeN++;
			if(!r.getSubJobsList().isEmpty()) {
				r.updateRoute(inp);
				r.setIdRoute(routeN);
				initialSol.getRoutes().add(r);
			}
		}
		// Computar costos asociados a la solucion
		computeSolutionCost(initialSol);
		return initialSol;
	}

	private void computeSolutionCost(Solution initialSol) {
		// s�lo se considera la driving route
		// update each route
		// 1. Compute waiting time
		//int passengers=0;
		double durationSolution = 0.0; // Travel distance = waiting time + driving time
		double waiting=0;
		double travelTime=0;
		double serviceTime=0;
		double drivingTime=0;
		double walkingTime=0;
		double paramedic=0;// los paramedicos que salen del depot
		double homeCareStaff=0;// los paramedicos que salen del depot
		int i=-1;
		for(Route r:initialSol.getRoutes()) {
			i++;
			r.setIdRoute(i);
			r.updateRoute(inp);
			waiting+=r.getWaitingTime(); // waiting time
			serviceTime+=r.getServiceTime(); // 2. Service time 
			drivingTime+=r.getTravelTime(); // 3. Travel time
			//passengers+=r.getPassengers();// 4. Passengers
			durationSolution+=r.getDurationRoute();
			paramedic+=r.getAmountParamedic();
			homeCareStaff+=r.getHomeCareStaff();
		}
		// 3. Setting values to a solution
		initialSol.setWaitingTime(waiting);
		initialSol.setServiceTime(serviceTime);
		initialSol.setdrivingTime(drivingTime);
		//initialSol.setPassengers(passengers);
		initialSol.setDurationSolution(durationSolution);
		initialSol.setParamedic(paramedic);
		initialSol.setHomeCareStaff(homeCareStaff);
	}

	private void crossingBetweenRoutes() {
		assigningSchiftToVehicles();

	}



	private void assigningSchiftToVehicles() {
		sortingEarlyStartShift();// sort shifts
		//ArrayList<Schift> partsToInsert=sortingEarlyEndShift();// sort shifts
		assignmentShiftVehicle(); // asignar los shifts a los veh�culos  



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
				for(Parts route: r.getPartsRoute()) {
					for(SubJobs j: route.getListSubJobs()) {
						System.out.println("SubJobs "+ j.getSubJobKey()+ " arrival "+j.getArrivalTime()+ " Start Service"+ j.getstartServiceTime() + "Total people" +j.getTotalPeople());
					}
				}
			}
			System.out.println("end ");

			// to remove
		}
		for(Schift turn:splittedSchift) {//1. intentar las cabezas de cada ruta

			System.out.println("turn " + turn.toString());
			for(Parts route: turn.getRouteParts()) {
				printing(route);
			}
			insertingParts(turn); // the heading of each route has been already inserted
			///
			int i=-1;
			for(Route r:routeVehicleList) {
				i++;
				System.out.println("route " + i);
				for(Parts route: r.getPartsRoute()) {
					for(SubJobs j: route.getListSubJobs()) {
						System.out.println("SubJobs "+ j.getSubJobKey()+ " arrival "+j.getstartServiceTime()+ " Start Service"+ j.getStartTime() + "Total people" +j.getTotalPeople());
					}
				}
			}
			System.out.println("end ");

		}
		// assigment turn according to early end
		sortingEarlyEndShift();// sort shifts
	}

	private void insertingParts(Schift turn) {
		boolean merging=false;
		for(Route r: routeVehicleList) { // the turn is inserted by parts
			printingPartRoute(r);
			int i=-1;
			for(Parts part:turn.getRouteParts()) { // tan pronto se inserta una parte entonces se continua con la siguiente hasta que todos los turnos sean asignados a un veh�culo
				i++;
				int sizePart=part.getListSubJobs().size();
				//if(!assignedShiftParts.containsValue(part) &&part.get(sizePart-1).getId()!=1) {
				if(!assignedShiftParts.containsValue(part)) {
					printing(part);
					String key= "T"+turn.getId()+"P"+i;
					int lastPartAssigned=r.getPartsRoute().size()-1;
					Parts lastPart=r.getPartsRoute().get(lastPartAssigned);
					int lastSubJobAssigned=lastPart.getListSubJobs().size()-1;
					SubJobs lastSubJob= lastPart.getListSubJobs().get(lastSubJobAssigned); 	// 1. Seleccionar el ultimo trabajo de la ruta
					SubJobs firtSubJobToInsert=part.getListSubJobs().get(0); // the first job- index 0 is the depot
					merging=driverSchedulle(r,part);//check<- el primer trabajo de la ruta con respecto al ultimo trabajo de la parte no debe exceder las 8 horas de trabajo
					if(merging) {
						// intentar insertando respentando los tiempos m�ximos de espera.
						//1. probar insertarlo con las horas actuales del trabajo
						merging=checkingTime(r,lastSubJob,firtSubJobToInsert);
						if(merging) {			// 2. Seleccionar el primer turno del trabajo que puede ser insertado
							// 3. Revisar la factibilidad de poder combinar ambas rutas: Detour, capacidad del veh�culo
							r.getPartsRoute().add(part);
							assignedShiftParts.put(key,part);
							printingPartRoute(r);
						}
						else { // changing the hour
							merging=changingTimeToTheNextJob(lastSubJob,firtSubJobToInsert,r);
							if(merging) {
								movingTimeShiftMiddlePart(firtSubJobToInsert,turn);
								//movingTimeShift(firtSubJobToInsert,turn);
								Parts newPart=new Parts(part);

								r.getPartsRoute().add(newPart);
								assignedShiftParts.put(key,newPart);
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

	private boolean driverSchedulle(Route r, Parts part) {
		boolean workingTime=false;
		// considerar la hora en la que el veh�culo llega al dept
		SubJobs lastJobsPart=part.getListSubJobs().get(part.getListSubJobs().size()-1);
		/////////////////
		// start of the route
		SubJobs startJobsRoute=r.getPartsRoute().get(0).getListSubJobs().get(0);
		// end of the route
		int longRoute=r.getPartsRoute().size();
		Parts lastPart=r.getPartsRoute().get(longRoute-1);
		int longLastPart=lastPart.getListSubJobs().size();
		SubJobs lastJobPart=lastPart.getListSubJobs().get(longLastPart-1);
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
		for(Parts route: r.getPartsRoute()) {
			for(SubJobs j: route.getListSubJobs()) {
				System.out.println("SubJobs "+ j.getSubJobKey()+ " arrival "+j.getArrivalTime()+ " Start Service"+ j.getstartServiceTime());
			}
		}
	}

	private void mergingHeadRoutes(Schift turn) {
		boolean merging=false;
		for(Route r: routeVehicleList) {
			System.out.println("iluminame espiritu Santo");
			printingPartRoute(r);
			int lastPartAssigned=r.getPartsRoute().size()-1;
			Parts lastPart=r.getPartsRoute().get(lastPartAssigned);
			int lastSubJobAssigned=lastPart.getListSubJobs().size()-1;
			SubJobs lastSubJob= lastPart.getListSubJobs().get(lastSubJobAssigned); 	// 1. Seleccionar el ultimo trabajo de la ruta
			SubJobs firtSubJobToInsert=turn.getRouteParts().get(0).getListSubJobs().get(1); // the first job- index 0 is the depot
			// 1. Max detour
			merging=checkingDetour(r,lastSubJob,firtSubJobToInsert); // as soon as the maximum detour constraint is met
			if(merging) {
				// 2. Time window
				merging=checkingTime(r,lastSubJob,firtSubJobToInsert);
				if(merging) {			// 2. Seleccionar el primer turno del trabajo que puede ser insertado
					// 3. Revisar la factibilidad de poder combinar ambas rutas: Detour, capacidad del veh�culo
					insertingHeadingPart(r,turn,lastSubJob);
				}
				else { // changing the hour
					merging=changingTimeToTheNextJob(lastSubJob,firtSubJobToInsert,r);
				}
			}
			if(merging) {
				// 1. Updating the shift time
				movingTimeShift(firtSubJobToInsert,turn);
				System.out.println("turn " + turn.toString());
				for(Parts route: turn.getRouteParts()) {
					printing(route);
				}
				printingShift(turn);
				ArrayList<SubJobs> newPart1=new ArrayList<SubJobs>();
				Parts newParts=new Parts();
				for(int i=1;i<turn.getRouteParts().get(0).getListSubJobs().size();i++) {

					System.out.println("inf job "+turn.getRouteParts().get(0).getListSubJobs().get(i).toString());
					newPart1.add(turn.getRouteParts().get(0).getListSubJobs().get(i));
				}
				newParts.setListSubJobs(newPart1);
				r.getPartsRoute().add(newParts);
				System.out.println("route ");
				printingPartRoute(r);
				///
				break;
			}

		}
		if(!merging){
			Route newRoute=new Route();
			newRoute.getPartsRoute().add(turn.getRouteParts().get(0));
			routeVehicleList.add(newRoute);

			System.out.println("route ");
			for(Parts route: newRoute.getPartsRoute()) {
				for(SubJobs j: route.getListSubJobs()) {
					System.out.println("SubJobs "+ j.getSubJobKey()+ " arrival "+j.getArrivalTime()+ " Start Service"+ j.getStartTime());
				}
			}	
		}
	}

	private void movingTimeShift(SubJobs firtSubJobToInsert, Schift turn) {
		// 1. Cambiar el arrival y el departure time para cada subjob
		// computing for the first part of the shift
		computingFirstPart(firtSubJobToInsert,turn);
		// remaining part of the shift
		computingRemainingShift(turn);

		printingShift(turn);
		System.out.println("end ");

	}


	private void movingTimeShiftMiddlePart(SubJobs firtSubJobToInsert, Schift turn) {
		// 1. Cambiar el arrival y el departure time para cada subjob
		changingArrivalDepartureTimesMiddleParts(firtSubJobToInsert,turn);
		// 2. cambiar la hora en la que empieza el servicion
		//	changingStartEndServiceTimeMiddleParts(firtSubJobToInsert,turn);
		//3. Compute the waiting tiem

	}

	private void changingStartEndServiceTime(Schift turn) {
		for(Parts part:turn.getRouteParts()) {
			for(SubJobs subjob:part.getListSubJobs()) {
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
		for(Parts part:turn.getRouteParts()) {
			if(part.getListSubJobs().get(0).getSubJobKey().equals(firtSubJobToInsert.getSubJobKey())) {
				for(SubJobs subjob:part.getListSubJobs()) {
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


	private void changingArrivalDepartureTimesMiddleParts(SubJobs firtSubJobToInsert, Schift turn) {
		// remaining part of the shift
		int refPart=0;
		for(int i=0;i<turn.getRouteParts().size();i++) {// iterar sobre las partes
			Parts currentList= turn.getRouteParts().get(i);	// Calling the first part of the turn
			refPart++;
			if(currentList.getListSubJobs().get(0).getSubJobKey().equals(firtSubJobToInsert.getSubJobKey())) {
				computingNewTimes(firtSubJobToInsert,currentList);
				break;
			}
		}
		for(int i=refPart+1;i<turn.getRouteParts().size();i++) {// iterar sobre las partes
			Parts currentList= turn.getRouteParts().get(i);	// Calling the first part of the turn
			Parts preList= turn.getRouteParts().get(i-1);	// Calling the first part of the turn
			int size=preList.getListSubJobs().size()-1;
			SubJobs lastSubJobs=preList.getListSubJobs().get(size);
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

	private void computingNewTimes(SubJobs firtSubJobToInsert, Parts currentList) {
		SubJobs job=currentList.getListSubJobs().get(0);
		job.setarrivalTime(firtSubJobToInsert.getArrivalTime());
		job.setdepartureTime(firtSubJobToInsert.getDepartureTime());
		double departureTimeLast=firtSubJobToInsert.getDepartureTime();
		for(int i=1; i<currentList.getListSubJobs().size();i++) {
			job=currentList.getListSubJobs().get(i);
			double tv=inp.getCarCost().getCost(currentList.getListSubJobs().get(i-1).getId()-1, job.getId()-1);
			double arrivalTime=departureTimeLast+tv;	
			job.setarrivalTime(arrivalTime);
			computeStartEndServiceTime(job);
			departureTimeLast=job.getDepartureTime();
		}
		printing(currentList);
		System.out.println("end" );
	}

	private void computingRemainingShiftMiddleParts(Schift turn) {
		//ArrayList<SubJobs> jobList= turn.getRoute().getPartsRoute().get(0);	// Calling the first part of the turn
		//int size=jobList.size();
		//SubJobs lastSubJobs=jobList.get(size-1); // calling the last subjobs


	}


	private void printingShift(Schift turn) {
		int i=-1;
		for(Parts part:turn.getRouteParts()) {
			i++;
			System.out.println("part "+i);
			for(SubJobs j:part.getListSubJobs()) {
				System.out.println("Subjob "+j.getSubJobKey() +" arrival  " +j.getArrivalTime()+" service time " +j.getstartServiceTime());
			}
		}
	}

	private void computingRemainingShift(Schift turn) {
		//ArrayList<SubJobs> jobList= turn.getRoute().getPartsRoute().get(0);	// Calling the first part of the turn
		//int size=jobList.size();
		//SubJobs lastSubJobs=jobList.get(size-1); // calling the last subjobs

		for(int i=1;i<turn.getRouteParts().size();i++) {// iterar sobre las partes
			Parts currentList= turn.getRouteParts().get(i);	// Calling the first part of the turn
			Parts preList= turn.getRouteParts().get(i-1);	// Calling the first part of the turn
			int size=preList.getListSubJobs().size()-1;
			SubJobs lastSubJobs=preList.getListSubJobs().get(size);
			computingTimes(lastSubJobs,currentList);
			System.out.println("el senor esta conmigo");
		}
	}

	private void computingTimes(SubJobs lastSubJobs, Parts currentList) {
		double departureTimeLast=lastSubJobs.getDepartureTime();
		for(int i=0; i<currentList.getListSubJobs().size();i++) {
			SubJobs job=currentList.getListSubJobs().get(i);
			double tv=inp.getCarCost().getCost(lastSubJobs.getId()-1, job.getId()-1);
			double arrivalTime=departureTimeLast+tv;	
			job.setarrivalTime(arrivalTime);
			computeStartEndServiceTime(job);
			departureTimeLast=job.getDepartureTime();
			System.out.println("el senor esta conmigo");
		}
	}

	private void computingFirstPart(SubJobs firtSubJobToInsert, Schift turn) {
		Parts jobList= turn.getRouteParts().get(0); 
		// setting the time for the first job
		SubJobs iNode=jobList.getListSubJobs().get(1);
		iNode.setarrivalTime(firtSubJobToInsert.getArrivalTime());
		computeStartEndServiceTime(iNode);
		double departureTimePreviousJob=iNode.getDepartureTime();
		for(int i=2; i<jobList.getListSubJobs().size();i++){
			iNode=jobList.getListSubJobs().get(i-1);
			departureTimePreviousJob=iNode.getDepartureTime();
			SubJobs jNode=jobList.getListSubJobs().get(i);
			double tv=inp.getCarCost().getCost(iNode.getId()-1, jNode.getId()-1);
			double arrivalTime=departureTimePreviousJob+tv;
			jNode.setarrivalTime(arrivalTime);
			computeStartEndServiceTime(jNode);
		}
		System.out.println("el espiritu santo esta trabajanto");
		printing(jobList);
	}

	private void computeStartEndServiceTime(SubJobs jNode) {
		double additionalTime=computeAdditionalTime(jNode);
		double startServiceTime=0;
		double departure=0;
		if(jNode.getTotalPeople()<0) {
			startServiceTime=Math.max((jNode.getArrivalTime()+additionalTime), jNode.getStartTime());
			jNode.setStartServiceTime(startServiceTime);
			departure=startServiceTime+jNode.getReqTime();
			jNode.setEndServiceTime(startServiceTime+jNode.getReqTime());
			jNode.setdepartureTime(departure);
		}
		else {
			startServiceTime=Math.max((jNode.getArrivalTime()), jNode.getStartTime());
			jNode.setStartServiceTime(startServiceTime);
			departure=startServiceTime+jNode.getReqTime()+additionalTime;
			jNode.setEndServiceTime(startServiceTime+jNode.getReqTime());
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
			else { // checking the waiting time  quiere decir que llega m�s temprano que la hora de inicio del servicio
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
		computeStartEndServiceTime(firtSubJobToInsert);
		//		double additionalTime=computeAdditionalTime(firtSubJobToInsert);
		//		double serviceStartTime=Math.max((firtSubJobToInsert.getArrivalTime()+additionalTime), firtSubJobToInsert.getStartTime());
		//		firtSubJobToInsert.setStartServiceTime(serviceStartTime);
		//		double departureTime=firtSubJobToInsert.getstartServiceTime()+firtSubJobToInsert.getReqTime();
		//		// 1.1 Set departure time
		//
		//		firtSubJobToInsert.setdepartureTime(departureTime);
		//		// 2.1 setting end service time
		//		firtSubJobToInsert.setEndServiceTime(departureTime);
		System.out.println("start service time"+ firtSubJobToInsert.getstartServiceTime());
		System.out.println("end service time"+ firtSubJobToInsert.getendServiceTime());
	}

	private void insertingHeadingPart(Route r, Schift turn, SubJobs lastSubJob) {
		// 2. inserting the jobs of the first part
		Parts jobsList=turn.getRoute().getPartsRoute().get(0);
		Parts jobsListToInsert=turn.getRoute().getPartsRoute().get(0);
		for(int job=1;job<jobsList.getListSubJobs().size();job++) {
			SubJobs j=jobsList.getListSubJobs().get(job);
			jobsListToInsert.getListSubJobs().add(j);
		}
		r.getPartsRoute().add(jobsListToInsert);
	}
	private void insertingPart(Route r, Schift turn, SubJobs lastSubJob) {
		// 2. inserting the jobs of the first part
		Parts jobsList=turn.getRoute().getPartsRoute().get(0);
		Parts jobsListToInsert=turn.getRoute().getPartsRoute().get(0);
		for(int job=0;job<jobsList.getListSubJobs().size();job++) {
			SubJobs j=jobsList.getListSubJobs().get(job);
			jobsListToInsert.getListSubJobs().add(j);
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
			if(firtSubJobToInsert.isMedicalCentre()) {
				if(firtSubJobToInsert.getTotalPeople()<0) {// drop-off at medical centre
					additionalTime=test.getloadTimePatient()+test.getRegistrationTime();
				}
				else {// pick-up at medical centre
					additionalTime=test.getloadTimePatient();
				}			
			}
			else { // pick up or drop-ff at patient home
				additionalTime=test.getloadTimePatient();
			}
		}
		return additionalTime;
	}



	private double computeMaxDetour(SubJobs firtSubJobToInsert) {
		double directConnection=inp.getCarCost().getCost(0, firtSubJobToInsert.getId()-1);
		double maxDetour=directConnection*test.getDetour();
		return maxDetour;
	}

	private double computeDistanceRotue(Route r) {
		double distance=0;
		for(Parts part:r.getPartsRoute()) {
			for(int i=1;i<part.getListSubJobs().size();i++) {
				SubJobs iNode=part.getListSubJobs().get(i-1);
				SubJobs jNode=part.getListSubJobs().get(i);
				double tv=inp.getCarCost().getCost(iNode.getId()-1, jNode.getId()-1);
				distance+=tv;
			}
		}
		return distance;
	}

	private double computeNeededArrivalToNexNode(SubJobs firtSubJobToInsert) {
		double neededArribal=0;
		int addionalTime=0;
		if(firtSubJobToInsert.isMedicalCentre()) {// 1. en le caso de que sea un centro m�dico
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
			System.out.println("turn "+t+" "+ "Subjob "+turn.getRoute().getSubJobsList().get(0).getSubJobKey() +"  " +turn.getRoute().getSubJobsList().get(0).getArrivalTime()+" ");
			if(sortedSchift.isEmpty()) {
				sortedSchift.add(turn);
			}
			else {
				readingTurns(sortedSchift,turn);
			}
			size=sortedSchift.size()-1;

			/////
			for(Schift copy:sortedSchift) {
				System.out.println("copy turn "+" "+ "Subjob "+copy.getRoute().getSubJobsList().get(0).getSubJobKey() +"  " +copy.getRoute().getSubJobsList().get(0).getArrivalTime()+" ");

			}

		}
		splittedSchift.clear();
		t=-1;
		for(Schift turn: sortedSchift) {
			splittedSchift.add(turn);
			t++;
			System.out.println("turn "+t+" "+ "Subjob "+turn.getRoute().getSubJobsList().get(1).getSubJobKey() +"  " +turn.getRoute().getSubJobsList().get(1).getArrivalTime()+" ");

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
			System.out.println("turn "+t+" "+ "Subjob "+turn.getRoute().getSubJobsList().get(0).getSubJobKey() +"  " +turn.getRoute().getSubJobsList().get(0).getArrivalTime()+" ");
			if(sortedSchift.isEmpty()) {
				sortedSchift.add(turn);
			}
			else {
				readingTailsTurns(sortedSchift,turn);
			}
			size=sortedSchift.size()-1;
			/////
			for(Schift copy:sortedSchift) {
				System.out.println("copy turn "+" "+ "Subjob "+copy.getRoute().getSubJobsList().get(0).getSubJobKey() +"  " +copy.getRoute().getSubJobsList().get(0).getArrivalTime()+" ");
			}
		}
		splittedSchift.clear();
		t=-1;
		for(Schift turn: sortedSchift) {
			splittedSchift.add(turn);
			t++;
			System.out.println("turn "+t+" "+ "Subjob "+turn.getRoute().getSubJobsList().get(1).getSubJobKey() +"  " +turn.getRoute().getSubJobsList().get(1).getArrivalTime()+" ");
		}
	}

	private void readingTurns(ArrayList<Schift> sortedSchift, Schift turn) {
		if(sortedSchift.size()==1) { 	// 1. si el primer trabajo ya en la lista es m�s tard�o el el inicio del trabajo de part
			SubJobs firstsubJob=turn.getRoute().getSubJobsList().get(0);
			SubJobs firstTurnInList=sortedSchift.get(0).getRoute().getSubJobsList().get(0);
			if(firstTurnInList.getstartServiceTime()<=firstsubJob.getArrivalTime()) {
				sortedSchift.add(turn);
			}
			else {
				sortedSchift.add(0,turn);
			}
		}
		else {
			int lastTurn=sortedSchift.size()-1;
			SubJobs firstsubJob=turn.getRoute().getSubJobsList().get(0);
			SubJobs firstTurnInList=sortedSchift.get(0).getRoute().getSubJobsList().get(0);
			SubJobs lastTurnInList=sortedSchift.get(lastTurn).getRoute().getSubJobsList().get(0);
			if(firstTurnInList.getArrivalTime()>=firstsubJob.getArrivalTime()) {
				sortedSchift.add(0,turn);
			}
			else {				
				// 1. si es m�s temprano que el primer trabajo
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
		int lastJobToInsert=turn.getRoute().getSubJobsList().size();
		SubJobs firstsubJob=turn.getRoute().getSubJobsList().get(lastJobToInsert-1);
		int lastJobInRoute=sortedSchift.get(0).getRoute().getSubJobsList().size();
		SubJobs firstTurnInList=sortedSchift.get(0).getRoute().getSubJobsList().get(lastJobInRoute-1);
		if(sortedSchift.size()==1) { 	// 1. si el primer trabajo ya en la lista es m�s tard�o el el inicio del trabajo de part
			if(firstTurnInList.getArrivalTime()<=firstsubJob.getArrivalTime()) {
				sortedSchift.add(turn);
			}
			else {
				sortedSchift.add(0,turn);
			}
		}
		else {
			int lastTurn=sortedSchift.size()-1;
			lastJobInRoute=sortedSchift.get(lastTurn).getRoute().getSubJobsList().size();
			SubJobs lastTurnInList=sortedSchift.get(lastTurn).getRoute().getSubJobsList().get(lastJobInRoute-1);
			if(lastTurnInList.getArrivalTime()<=firstsubJob.getArrivalTime()) {
				sortedSchift.add(turn);
			}
			else {				
				// 1. si es m�s temprano que el primer trabajo
				if(firstTurnInList.getArrivalTime()>=firstsubJob.getArrivalTime()) {
					sortedSchift.add(turn);
				}
				else {
					readingArray(sortedSchift,turn);			
				}
			}
		}
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
		for(Parts j:s.getRouteParts()) {	
			i++;
			System.out.println("Part "+ i);
			for(SubJobs jb:j.getListSubJobs()) {
				System.out.println("subjob "+ jb.getSubJobKey()+ "arival "+ jb.getArrivalTime()+ "Start_service " + jb.getstartServiceTime()+ "  " );
			}
		}

	}


	private void readingArray(ArrayList<Schift> sortedSchift, Schift turn) {
		for(int i=1;i<sortedSchift.size();i++) {
			SubJobs firstsubJob=turn.getRoute().getSubJobsList().get(0);
			SubJobs firstTurnInList=sortedSchift.get(i).getRoute().getSubJobsList().get(0);
			if(firstTurnInList.getArrivalTime()>=firstsubJob.getArrivalTime()) {
				SubJobs previousTurnInList=sortedSchift.get(i-1).getRoute().getSubJobsList().get(0);
				if(previousTurnInList.getArrivalTime()<=firstsubJob.getArrivalTime()) {
					sortedSchift.add(i,turn);
					break;
				}
			}
		}
	}



	private void assigmentJobsToQualifications(ArrayList<ArrayList<Couple>> clasification) { 
		// solo la asignaci�n de trabajos a los niveles de calificaciones // se hace una secuencia ordenad_
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
		ArrayList<Parts> qualification0= assigmentParamedic(q0,clasification0);

		//Qualification level from 1 to 3
		ArrayList<Parts> qualification1= assigmentParamedic(q1,clasification1); // here are not considering working hours
		ArrayList<Parts> qualification2= assigmentParamedic(q2,clasification2);
		ArrayList<Parts> qualification3= assigmentParamedic(q3,clasification3);


		//downgradings(qualification3,qualification2); //No se considera porque no es tan facil controlar el tiempo de trabajo
		//downgradings(qualification2,qualification1);


		// 4. Savings los turnos
		// paramedics
		Parts newParts= null;
		for(Parts schifts:qualification0) {
			if(!schifts.getListSubJobs().isEmpty()) {
				newParts= new Parts(schifts);
			}
			if(newParts!=null) {
				schift.add(newParts);
			}
		}




		// home care staff
		for(Parts schifts:qualification1) {
			if(!schifts.getListSubJobs().isEmpty()) {
				newParts= new Parts(schifts);
			}
			if(newParts!=null) {
				schift.add(newParts);
			}
		}



		for(Parts schifts:qualification2) {
			if(!schifts.getListSubJobs().isEmpty()) {
				newParts= new Parts(schifts);
			}
			if(newParts!=null) {
				schift.add(newParts);
			}
		}
		for(Parts schifts:qualification3) {
			if(!schifts.getListSubJobs().isEmpty()) {
				newParts= new Parts(schifts);
			}
			if(newParts!=null) {
				schift.add(newParts);
			}
		}
		System.out.println("all turns");
		int i=-1;
		for(Parts s:schift) {
			i++;
			System.out.println("turn "+i);
			printing(s);
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
		for(Parts turn:schift ) { // cada turno se convertira en una ruta
			makeTurnInRoute(turn);
		}
		// 2. compute the the start and end time of route
		timeStartEndRoutes();
		//3. compute the connections between SubJobs
		settingEdges();
		creatingSchifts();
	}

	private void creatingSchifts() {
		int intRoute=0;
		for(Route r:routeList) {
			intRoute++;
			Schift personnal= new Schift(r,intRoute);
			r.setSchiftRoute(personnal);
		}
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
			r.getSubJobsList().get(0).setserviceTime(0);
			// end time
			SubJobs lastJob=r.getSubJobsList().get(r.getSubJobsList().size()-2);  // it is not the depot node
			computeEndTimeRoute(lastJob,r);
			r.getSubJobsList().get(r.getSubJobsList().size()-1).setserviceTime(0);
		}
	}

	private void computeEndTimeRoute(SubJobs lastJob, Route r) {
		// 1. Compute travel time
		SubJobs depot=r.getPartsRoute().get(r.getPartsRoute().size()-1).getListSubJobs().get(0);
		double tv=inp.getCarCost().getCost(lastJob.getId()-1, depot.getId()-1);
		double arrivalTime=	lastJob.getDepartureTime()+tv;
		depot.setarrivalTime(arrivalTime);
		depot.setdepartureTime(arrivalTime);
		depot.setdepartureTime(arrivalTime);
		depot.setStartServiceTime(arrivalTime);
		depot.setEndServiceTime(arrivalTime);
		System.out.println(r.toString());
	}

	private void computeStartTimeRoute(SubJobs firstJob, Route r) {
		// 1. Compute travel time
		SubJobs depot=r.getPartsRoute().get(0).getListSubJobs().get(0);
		double tv=inp.getCarCost().getCost(depot.getId()-1,firstJob.getId()-1);
		double arrivalTime=	firstJob.getArrivalTime()-tv-test.getloadTimeHomeCareStaff();
		depot.setarrivalTime(arrivalTime);
		depot.setdepartureTime(arrivalTime);
		depot.setStartServiceTime(arrivalTime);
		depot.setEndServiceTime(arrivalTime);
		System.out.println(r.toString());
	}

	private void makeTurnInRoute(Parts turn) { // crea tantas rutas como son posibles
		// la creaci�n de estas rutas lo que hace es identificar las partes de cada turno
		// pero en esencia s�lo deberia agregar el deport
		System.out.println(turn.toString());
		//	calling depot
		//Jobs depot=inp.getNodes().get(0);
		SubJobs depotStart = new SubJobs(inp.getNodes().get(0));
		depotStart.setTotalPeople(1);
		SubJobs depotEnd = new SubJobs(inp.getNodes().get(0));
		depotEnd.setTotalPeople(1);
		ArrayList<SubJobs> partStart= new ArrayList<SubJobs>();
		ArrayList<SubJobs> partEnd= new ArrayList<SubJobs>();
		partStart.add(depotStart);

		partEnd.add(depotEnd);
		Parts partObject= new Parts();
		Route r= new Route();
		routeList.add(r);
		partObject.setListSubJobs(partStart);
		r.getPartsRoute().add(partObject);

		// 1. hacer las partes
		double passengers=1;
		ArrayList<SubJobs> part= new ArrayList<SubJobs>();
		part= new ArrayList<SubJobs>();
		partObject= new Parts();
		r.getPartsRoute().add(partObject);
		for(int i=0;i<turn.getListSubJobs().size();i++) {
			SubJobs sj=turn.getListSubJobs().get(i);
			passengers+=sj.getTotalPeople();
			if(passengers!=0) {
				partObject.getListSubJobs().add(sj);
				if(i==turn.getListSubJobs().size()-1) {
					r.updateRoute(inp);
					System.out.println(r.toString());
				}
			}
			else {
				partObject.getListSubJobs().add(sj);
				partObject= new Parts();
				r.getPartsRoute().add(partObject);
				System.out.println(r.toString());
			}		
		}
		partObject= new Parts();
		partObject.setListSubJobs(partEnd);
		r.getPartsRoute().add(partObject);
		r.updateRoute(inp);

		System.out.println("Route");
		System.out.println(r.toString());
		System.out.println("end");
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




	private ArrayList<Parts> assigmentParamedic(int q3, ArrayList<Jobs> clasification3) { // return the list of subJobs
		qualificationParamedic= new ArrayList<> (q3);
		for(int i=0;i<q3;i++) {
			Parts newPart=new Parts();
			qualificationParamedic.add(newPart);
		}
		// se guarda los schift
		for(Jobs j:clasification3) { // iterate over jobs
			for(Parts paramedic:qualificationParamedic) {
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


	private void printing(Parts paramedic) {
		for(SubJobs j:paramedic.getListSubJobs()) {
			System.out.println(j.getSubJobKey()+" arrival "+ j.getArrivalTime()+" start time "+ j.getstartServiceTime()+ "req time " + j.getReqTime());
		}
	}

	private boolean possibleInsertion(Jobs j, Parts homeCare) { // si el trabajo se acepta para ser insertado- entonces el trabajo se desagrega en subtrabajos
		boolean inserted=false;
		if(homeCare.getListSubJobs().isEmpty()) {
			insertionJob(j,homeCare);
			printing(homeCare);
			inserted=true;
		}
		else { // inside the array there are more jobs
			//int position=iterateOverSchift(j,homeCare);
			// dividir el trabajo
			Parts pickUpDropOff=disaggregatedJob(j);

			// revisar si el primer trabajo de esta parte puede ser insertado
			SubJobs jsplited=pickUpDropOff.getListSubJobs().get(0);
			//double workingHours= computingWorkingHours(homeCare,pickUpDropOff);
			int position=iterateOverSchiftLastPosition(jsplited,homeCare);
			if(position>=0 && enoughWorkingHours(homeCare,pickUpDropOff)) {
				for(SubJobs sj:pickUpDropOff.getListSubJobs()) {
					homeCare.getListSubJobs().add(position,sj);
					assignedJobs.put(sj.getSubJobKey(),sj);
					printing(homeCare);
					position++;
				}
				inserted=true;
			}
		}
		return inserted;
	}


	private boolean enoughWorkingHours(Parts homeCare, Parts pickUpDropOff) {
		boolean enoughtTime=false;
		SubJobs primerInRouteSubJob=homeCare.getListSubJobs().get(0);
		SubJobs lastSubJobToInsert=pickUpDropOff.getListSubJobs().get(pickUpDropOff.getListSubJobs().size()-1);
		if(primerInRouteSubJob.getArrivalTime()-lastSubJobToInsert.getDepartureTime()<test.getWorkingTime()) {
			enoughtTime=true;
		}
		return enoughtTime;
	}

	private Parts disaggregatedJob(Jobs j) {
		Parts pickUpDropOff= null; 
		if(j.isClient()) {
			pickUpDropOff=splitClientJobInSubJobs(j);}
		else {
			pickUpDropOff=splitPatientJobInSubJobs(j);
		}
		return pickUpDropOff;
	}

	private double computingWorkingHours(ArrayList<SubJobs> homeCare, ArrayList<SubJobs> pickUpDropOff) {
		double workingTime=0;
		double timeWithPatient=computeService(homeCare,pickUpDropOff);
		double travelTime=computeTravelTime(homeCare,pickUpDropOff);
		workingTime=timeWithPatient+travelTime;
		return workingTime;
	}

	private double computeTravelTime(ArrayList<SubJobs> homeCare, ArrayList<SubJobs> pickUpDropOff) {

		Jobs depot=inp.getNodes().get(0);
		double travelTime=inp.getCarCost().getCost(depot.getId()-1, homeCare.get(0).getId()-1);
		for(int i=0;i<homeCare.size()-1;i++) {
			travelTime+=inp.getCarCost().getCost(homeCare.get(i).getId()-1, homeCare.get(i).getId()-1);
		}
		travelTime+=inp.getCarCost().getCost(homeCare.get(homeCare.size()-1).getId()-1, pickUpDropOff.get(0).getId()-1);
		for(int i=0;i<pickUpDropOff.size()-1;i++) {
			travelTime+=inp.getCarCost().getCost(pickUpDropOff.get(i).getId()-1, pickUpDropOff.get(i).getId()-1);
		}
		travelTime+=inp.getCarCost().getCost(pickUpDropOff.get(0).getId()-1,depot.getId()-1);
		return travelTime;
	}

	private double computeService(ArrayList<SubJobs> homeCare, ArrayList<SubJobs> pickUpDropOff) {
		double timeWithPatient=0;
		double serviceTime=0;
		double loadUnloadingTime=0;
		double registrationTime=0;
		// service time
		for(SubJobs job:homeCare) {
			serviceTime+=job.getReqTime();
			if(job.isClient()) {
				loadUnloadingTime+=test.getloadTimeHomeCareStaff();
			}
			else {
				loadUnloadingTime+=test.getloadTimePatient();
				if(job.isMedicalCentre() && job.getTotalPeople()<0) {
					registrationTime=test.getRegistrationTime();
				}
			}
		}
		// newJobsToInsert
		for(SubJobs job:pickUpDropOff) {
			serviceTime+=job.getReqTime();
			if(job.isClient()) {
				loadUnloadingTime+=test.getloadTimeHomeCareStaff();
			}
			else {
				loadUnloadingTime+=test.getloadTimePatient();
				if(job.isMedicalCentre() && job.getTotalPeople()<0) {
					registrationTime=test.getRegistrationTime();
				}
			}
		}
		timeWithPatient=serviceTime+loadUnloadingTime+registrationTime;
		return timeWithPatient;
	}

	private void insertionJob(Jobs j, Parts homeCare) {
		if(j.isClient()) { // revisar si solo los trabajos j se realizan casa de clientes o en medical centres
			Parts dropOffPickUp=splitClientJobInSubJobs(j);
			insertingSubJobsIntheSequence(homeCare,dropOffPickUp); // la asignaci�n de la lista de subjobs no necesariamente se hace dentro de un mismo turno
			j.setarrivalTime(j.getstartServiceTime()-test.getloadTimeHomeCareStaff());
		}
		else { // the job j have to be a medical centre
			Parts pickUpDropOff=splitPatientJobInSubJobs(j);
			insertingSubJobsIntheSequence(homeCare,pickUpDropOff); // la asignaci�n de la lista de subjobs no necesariamente se hace dentro de un mismo turno

			j.setarrivalTime(j.getstartServiceTime()-test.getloadTimePatient()-test.getRegistrationTime());
		}
	}

	private void insertingSubJobsIntheSequence(Parts homeCare, Parts dropOffPickUp) {
		for(SubJobs sb:dropOffPickUp.getListSubJobs() ) {
			String key=creatingKey(sb);
			assignedJobs.put(key, sb);
			homeCare.getListSubJobs().add(sb);
			//	settingTimes(0,sb);
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

	private Parts splitPatientJobInSubJobs(Jobs j) {
		Parts newPart= new Parts();
		ArrayList<SubJobs> subJobsList= new ArrayList<SubJobs>();// considerar el inicio y el fin del servicio
		// 2. Generation del drop off at medical centre
		Jobs patient= new Jobs(inp.getNodes().get(j.getIdUser()-1));
		SubJobs dropOffMedicalCentre= new SubJobs(j);
		settingTimeDropOffPatientParamedicSubJob(dropOffMedicalCentre,j);


		// 1. Generation del pick up at patient home 
		SubJobs pickUpPatientHome= new SubJobs(patient);
		settingTimePickUpPatientSubJob(pickUpPatientHome,dropOffMedicalCentre);

		// 3. Generaci�n del pick at medical centre
		SubJobs pickUpMedicalCentre= new SubJobs(j);
		settingTimePickUpPatientParamedicSubJob(pickUpMedicalCentre,dropOffMedicalCentre);

		// 4. Generaci�n del drop-off at client home
		SubJobs dropOffPatientHome= new SubJobs(patient);
		settingTimeDropOffPatientSubJob(dropOffPatientHome,pickUpMedicalCentre);

		// 3. Addding the subjobs to the list
		subJobsList.add(pickUpPatientHome); // Se apilan por orden de sequencia
		subJobsList.add(dropOffMedicalCentre);
		subJobsList.add(pickUpMedicalCentre); // Se apilan por orden de sequencia
		subJobsList.add(dropOffPatientHome);
		newPart.setListSubJobs(subJobsList);
		return newPart;
	}

	private void settingTimeDropOffPatientSubJob(SubJobs dropOffPatientHome, Jobs pickUpMedicalCentre) {
		//()--------------(j)-------------(dropOffPatientHome)
		dropOffPatientHome.setTotalPeople(-1);
		dropOffPatientHome.setPatient(true);
		// 1. Setting the start service time -- startServiceTime
		double travel=inp.getCarCost().getCost(pickUpMedicalCentre.getId()-1, dropOffPatientHome.getId()-1); // es necesario considerar el travel time porque involucra dos locaciones
		double arrivalTime=pickUpMedicalCentre.getDepartureTime()+travel;  // load al lugar de la cita medica
		dropOffPatientHome.setarrivalTime(arrivalTime);
		dropOffPatientHome.setStartTime(arrivalTime);
		dropOffPatientHome.setEndTime(arrivalTime);// departure from patient home - el tiempo de viaje - el tiempo necesario para cargar los pacientes al veh�culo
		// modificar el tiempo requerido para el trabajo+
		dropOffPatientHome.setserviceTime(0);	
		// 1. Setting the start service time -- startServiceTime
		dropOffPatientHome.setStartServiceTime(dropOffPatientHome.getEndTime());
		// 3. Set el fin del servicio
		dropOffPatientHome.setEndServiceTime(dropOffPatientHome.getstartServiceTime()+test.getloadTimePatient());
		dropOffPatientHome.setdepartureTime(dropOffPatientHome.getendServiceTime());
	}

	private void settingTimePickUpPatientParamedicSubJob(SubJobs pickUpMedicalCentre, Jobs j) {
		//()--------------(pickUpMedicalCentre=j)-------------()-------------()
		pickUpMedicalCentre.setTotalPeople(2); // 5. Setting the total people (+) pick up   (-) drop-off
		pickUpMedicalCentre.setMedicalCentre(true);	
		pickUpMedicalCentre.setserviceTime(j.getReqTime());
		j.setserviceTime(0);
		pickUpMedicalCentre.setStartTime(j.getDepartureTime()+pickUpMedicalCentre.getReqTime());
		pickUpMedicalCentre.setEndTime(j.getDepartureTime()+pickUpMedicalCentre.getReqTime());
		// 1. Setting the start service time -- startServiceTime
		pickUpMedicalCentre.setStartServiceTime(pickUpMedicalCentre.getEndTime());
		pickUpMedicalCentre.setarrivalTime(pickUpMedicalCentre.getstartServiceTime());
		// 3. Set el fin del servicio
		pickUpMedicalCentre.setEndServiceTime(pickUpMedicalCentre.getstartServiceTime()+test.getloadTimePatient());
		pickUpMedicalCentre.setdepartureTime(pickUpMedicalCentre.getendServiceTime());
	}

	private void settingTimeDropOffPatientParamedicSubJob(SubJobs dropOffMedicalCentre, Jobs j) { // hard time window
		//()-------------(dropOffMedicalCentre=j)-------------()-------------()
		dropOffMedicalCentre.setTotalPeople(-2);	// 5. Setting the total people (+) pick up   (-) drop-off
		dropOffMedicalCentre.setMedicalCentre(true);
		dropOffMedicalCentre.setStartServiceTime(j.getEndTime());// 1. Setting the start service time -- startServiceTime
		// 2. Set ArrivalTime
		dropOffMedicalCentre.setarrivalTime(dropOffMedicalCentre.getstartServiceTime()-test.getloadTimePatient()-test.getRegistrationTime()); // el tiempo del registro
		dropOffMedicalCentre.setEndServiceTime(dropOffMedicalCentre.getstartServiceTime());	
		dropOffMedicalCentre.setdepartureTime(dropOffMedicalCentre.getendServiceTime());
	}

	private void settingTimePickUpPatientSubJob(SubJobs pickUpPatientHome, Jobs j) { // j <- es el nodo en donde se tiene la cita medica
		//(pickUpPatientHome)--------------(j)-------------()-------------()
		// 1. Setting the start service time -- startServiceTime
		// 5. Setting the total people (+) pick up   (-) drop-off
		pickUpPatientHome.setTotalPeople(1);
		pickUpPatientHome.setPatient(true);
		double travel=inp.getCarCost().getCost(pickUpPatientHome.getId()-1, j.getId()-1); // es necesario considerar el travel time porque involucra dos locaciones
		double departureTimeAtMedicalCentre=j.getArrivalTime()-travel;  // load al lugar de la cita medica
		pickUpPatientHome.setEndServiceTime(departureTimeAtMedicalCentre);
		pickUpPatientHome.setdepartureTime(departureTimeAtMedicalCentre);	

		pickUpPatientHome.setarrivalTime(departureTimeAtMedicalCentre-test.getloadTimePatient());
		pickUpPatientHome.setStartTime(departureTimeAtMedicalCentre); // setting time window
		pickUpPatientHome.setEndTime(departureTimeAtMedicalCentre);
		// modificar el tiempo requerido para el trabajo+
		pickUpPatientHome.setserviceTime(0);	
		// 1. Setting the start service time -- startServiceTime
		pickUpPatientHome.setStartServiceTime(pickUpPatientHome.getEndTime());
		// 3. Set el fin del servicio

	}

	private Parts splitClientJobInSubJobs(Jobs j) {
		Parts newParts= new Parts();
		ArrayList<SubJobs> subJobsList= new ArrayList<SubJobs>();// considerar el inicio y el fin del servicio
		// 1. Generaci�n del drop-off job
		SubJobs dropOff= new SubJobs(j);
		// 2. Generaci�n del pick-up job
		SubJobs pickUp= new SubJobs(j.getsubJobPair());
		settingTimeClientSubJob(dropOff,pickUp);
		// 3. Addding the subjobs to the list
		subJobsList.add(dropOff); // Se apilan por orden de sequencia
		subJobsList.add(pickUp);
		newParts.setListSubJobs(subJobsList);
		return newParts;
	}

	private void settingTimeClientSubJob(SubJobs dropOff, SubJobs pickUp) {
		homeCareDropOff(dropOff);
		homeCarePickUp(dropOff,pickUp);
	}

	private void homeCarePickUp(SubJobs dropOff, SubJobs pickUp) {
		pickUp.setTotalPeople(1);
		pickUp.setClient(true);	
		pickUp.setserviceTime(dropOff.getReqTime()); //los nodos pick up contienen la informaci�n de los nodos
		dropOff.setserviceTime(0);
		// Setting the TW
		pickUp.setStartTime(dropOff.getDepartureTime()+pickUp.getReqTime());
		pickUp.setEndTime(dropOff.getDepartureTime()+pickUp.getReqTime());
		// modificar el tiempo requerido para el trabajo+	
		// 1. Setting the start service time -- startServiceTime
		pickUp.setStartServiceTime(pickUp.getEndTime());
		// 3. Set el fin del servicio
		pickUp.setEndServiceTime(pickUp.getstartServiceTime()+test.getloadTimeHomeCareStaff());
		// 2. Set ArrivalTime-<- la enferemera puede ser recogida una vez esta haya terminado el servicio
		pickUp.setarrivalTime(pickUp.getStartTime());
		pickUp.setdepartureTime(pickUp.getendServiceTime());
		// 5. Setting the total people (+) pick up   (-) drop-off
	}


	private void homeCareDropOff(SubJobs dropOff) {
		dropOff.setTotalPeople(-1);
		dropOff.setClient(true);
		// 1. Setting the start service time -- startServiceTime
		dropOff.setStartServiceTime(dropOff.getEndTime());
		// 2. Set ArrivalTime
		dropOff.setarrivalTime(dropOff.getstartServiceTime()-test.getloadTimeHomeCareStaff());
		// 3. Set el fin del servicio
		dropOff.setEndServiceTime(dropOff.getstartServiceTime());	
		dropOff.setdepartureTime(dropOff.getendServiceTime());
		// 5. Setting the total people (+) pick up   (-) drop-off	
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

	private int iterateOverSchift(SubJobs j, Parts homeCare) {
		System.out.println("Job to insert "+ j.getId()+" "+ j.getSubJobKey()+" "+ j.getstartServiceTime());
		boolean inserted=false;
		int position=-1;
		// se evalua inser trabajo por trabajo - Tan pronto sea posible insertar el trabajo se para la iteraci�n sobre el turno y se inserta
		//ArrayList<SubJobs> homeCare=copyListJobs(currentJobs);
		for(int i=0;i<homeCare.getListSubJobs().size();i++) { 
			SubJobs inRoute=homeCare.getListSubJobs().get(i);
			if(i==homeCare.getListSubJobs().size()-1 ) {
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
					SubJobs inRouteK=homeCare.getListSubJobs().get(i+1);
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

	private int iterateOverSchiftLastPosition(Jobs j, Parts homeCare) {
		System.out.println("Job to insert "+ j.getId()+" "+ j.getSubJobKey()+" "+ j.getstartServiceTime());
		boolean inserted=false;
		int position=-1;
		// se evalua inser trabajo por trabajo - Tan pronto sea posible insertar el trabajo se para la iteraci�n sobre el turno y se inserta
		//ArrayList<SubJobs> homeCare=copyListJobs(currentJobs);
		SubJobs inRoute=homeCare.getListSubJobs().get(homeCare.getListSubJobs().size()-1);
		SubJobs subj=new SubJobs(j);
		inserted=insertionLater(inRoute,subj);//(inRoute)******(j)
		if(inserted) {
			position=homeCare.getListSubJobs().size();
		}			
		return position;
	}


	private void insertingSubJobsParamedic(int i, Jobs j, Parts homeCare) {
		// 1. split the job into subjobs
		Parts pickUpDropOff= null; 
		if(j.isClient()) {
			pickUpDropOff=splitClientJobInSubJobs(j);}
		else {
			pickUpDropOff=splitPatientJobInSubJobs(j);
		}
		for(SubJobs sj:pickUpDropOff.getListSubJobs()) {
			if(sj.getEndTime()==j.getEndTime() && sj.getStartTime()==j.getStartTime()) {
				homeCare.getListSubJobs().add(i,sj);
				printing(homeCare);
				assignedJobs.put(sj.getSubJobKey(),sj);
				break;
			}

		}
		missingSubJobs(pickUpDropOff); // try to insert the remaining subjobs
		// insert the equivalent job j in the sequence homeCare
	}

	private void missingSubJobs(Parts pickUpDropOff) {
		for(SubJobs sj:pickUpDropOff.getListSubJobs()) {
			if(!assignedJobs.containsKey(sj.getSubJobKey())) {
				iteratingOverCurrentShifts(sj);
			}

		}


	}

	private void iteratingOverCurrentShifts(SubJobs sj) {
		// ArrayList<ArrayList<SubJobs>> qualificationParamedic= new ArrayList<>(); // turn for paramedics
		int position=-1;
		for(Parts schift:qualificationParamedic) {
			if(schift.getListSubJobs().isEmpty()) {
				insertionJob(sj,schift);
				printing(schift);
			}
			else {
				position=iterateOverSchift(sj,schift);
				//	position=iterateOverSchiftLastPosition(sj,schift);
				if(position>0) {
					schift.getListSubJobs().add(position,sj);
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

	private boolean insertedMiddleVehicle(SubJobs inRoute, SubJobs j, SubJobs inRouteK) {
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

	private boolean insertedMiddleJob(SubJobs inRoute, SubJobs j, SubJobs inRouteK) {
		boolean inserted=false;
		double tv=inp.getCarCost().getCost(j.getId()-1,inRouteK.getId()-1);
		double additionalTime=computeAdditionalTime(inRouteK);
		double possibleArrivalTime=inRouteK.getstartServiceTime()-(additionalTime+tv);
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

	private boolean insertionEarly(SubJobs inRoute, SubJobs j) {
		boolean inserted=false;
		double tv=inp.getCarCost().getCost(j.getId()-1,inRoute.getId()-1);
		double additionalTime=computeAdditionalTime(inRoute);
		double possibleArrivalTime=inRoute.getstartServiceTime()-(additionalTime+tv);
		if(possibleArrivalTime<=j.getEndTime() && possibleArrivalTime>0) {
			if(inRoute.getstartServiceTime()>=j.getstartServiceTime()) {
				settingTimes(possibleArrivalTime,j);
				inserted=true;
			}
		}
		return inserted;
	}

	private boolean insertionEarlyVehicle(SubJobs inRoute, SubJobs j) {//(j)******(inRoute)
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

	private double computingTravelTimeWithDetour(SubJobs inRoute, SubJobs j) {
		double travelTime=inp.getCarCost().getCost(inRoute.getId()-1, j.getId()-1);
		return travelTime;
	}

	private void settingTimes(double possibleArrivalTime, SubJobs j) {
		// setting arrival time
		j.setarrivalTime(possibleArrivalTime);
		// setting departure time
		double additionalTime=computeAdditionalTime(j);
		j.setWaitingTime(possibleArrivalTime+additionalTime, j.getstartServiceTime());	
		j.setEndServiceTime(j.getstartServiceTime()+j.getReqTime());
	}

	private boolean insertionLater(SubJobs inRoute,SubJobs j) {//(inRoute)******(j)
		boolean inserted=false;
		double tv=computingTravelTimeWithDetour(inRoute,j);// considering the detour
		double possibleArrivalTime=inRoute.getDepartureTime()+tv; // the possible arrival time have to be lower than the end time of the tw of the nodo
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



	private boolean insertionLaterVehicle(SubJobs inRoute,SubJobs j) {//(inRoute)******(j)
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


	// getters 
	public Solution getInitialSol() {return initialSol;}
	public Solution getSol() {return solution;}

}

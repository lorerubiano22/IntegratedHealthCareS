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
	// subJobs couple
	HashMap<String, Couple> coupleList= new HashMap<>();  // turn for home care staff 2
	private Solution initialSol=null;
	private Solution solution=null;
	// to remove
	private HashMap<String,SubJobs> assignedSubJobs= new HashMap<String,SubJobs>();// Security copy
	private HashMap<String,SubJobs> missingubJobs= new HashMap<String,SubJobs>();// Security copy

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
		initialSol= createInitialSolution(); // la ruta ya deberia tener los arrival times
		System.out.println(initialSol.toString());
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
		System.out.println(initialSol.toString());
		return initialSol;
	}




	private Solution assigningRoutesToDrivers(Solution initialSol) {
		Solution copySolution= new Solution(initialSol);
		System.out.println("printing copy solution");
		System.out.println(copySolution.toString());
		changingDepartureTimes(copySolution);
		for(Route r:copySolution.getRoutes()) {
			for(int i=1;i<r.getPartsRoute().size()-1;i++) {
				Parts p= r.getPartsRoute().get(i);
				if(p.getDirectoryConnections().isEmpty()) {
					System.out.println("empty list of connections");
				}		
			}
		}
		boolean merge= checkingSubJobs(initialSol,initialSol);
		Solution solFixedTime=insertTWNarrow(initialSol); // insertar la 


		System.out.println("printing initial solution");
		System.out.println(initialSol.toString());
		System.out.println("printing copy solution");
		System.out.println(copySolution.toString());
		System.out.println("end");
		Solution newSol= mergingRoutes(copySolution); // las rutas se mezclan por partes
		//Solution newSolVehicle= mergingRoutes(copySolution); // ajustando las rutas al número de vehículos
		merge= checkingSubJobs(initialSol,newSol);
		System.out.println("printing initial solution");
		updatingSolution(newSol);
		System.out.println("printing initial solution");
		System.out.println(initialSol.toString());
		System.out.println("printing copy solution");
		System.out.println(newSol.toString());

		// haciendo una combinación diferente


		//Solution auxSolution=sclackTime(newSol);

		//merge= checkingSubJobs(initialSol,auxSolution);
		//boolean noduplicateJobs= checkDoubleJobs(auxSolution);
		//System.out.println("solution "+merge);

		//updatingSolution(copySolution);
		return newSol;
	}











	private Solution insertTWNarrow(Solution initialSol) {
		Solution sol=new Solution();
		ArrayList<Route> walkingRoutes= new ArrayList<Route> ();
		ArrayList<Route> remainingRoutes= new ArrayList<Route> ();
		boolean isWalkingRotue=false;
		for(Route r:initialSol.getRoutes()) {
			isWalkingRotue=arewalkingRoutesIntheRotue(r); // true if at least one job represents a walking route
			if(isWalkingRotue) {
				isWalkingRotue=false;
				walkingRoutes.add(r);
			}
			else {
				remainingRoutes.add(r);
			}
		}

		// total route as the total vehivles
		ArrayList<Route> routeSol=assigmentWalkingRouteToVehicle(walkingRoutes);// todas las rutas son asignadas a un vehículo
		ArrayList<SubJobs> subJobsToAssign=missingAssigments(walkingRoutes);
		HashMap<String, SubJobs> subJobsID=poolSubJobsToAssign(subJobsToAssign,remainingRoutes);
		assigmentSubJobsToVehicle(routeSol,subJobsID);// todas las rutas son asignadas a un vehículo
		for(Route r:routeSol) {
		printList(r);
			if(!r.getSubJobsList().isEmpty()) {
				sol.getRoutes().add(r);
			}
		}
		
		return sol;
	}

	private void printList(Route r) {
		System.out.println("Route start");
		for(SubJobs j:r.getSubJobsList()) {
			System.out.println("ID "+j.getId()+" B "+j.getstartServiceTime()+" TW["+j.getStartTime()+";"+j.getEndTime()+"]");
		}
		System.out.println("Route end");
	}

	private void assigmentSubJobsToVehicle(ArrayList<Route> routeSol, HashMap<String, SubJobs> subJobsID) {
		ArrayList<SubJobs> list= new ArrayList<SubJobs> ();
		for(SubJobs j:subJobsID.values()) {
			list.add(j);
		}
		for(SubJobs j:list) {
			if(j.getId()==39) {
				boolean a=false;
			}
			if(subJobsID.containsKey(j.getSubJobKey())) {
			boolean inserted=false;
			if(j.isClient()) {
				SubJobs j1=(SubJobs)this.coupleList.get(j.getSubJobKey()).getStartEndNodes().get(1);
				SubJobs j2=(SubJobs)this.coupleList.get(j.getSubJobKey()).getStartEndNodes().get(0);
				inserted=insertingIndividualClientJob(j1,routeSol, subJobsID);
				updatingCouple(j1,j2);
				if(inserted) {
					inserted=insertingIndividualClientJob(j2,routeSol,subJobsID);
				}
			}}
		}
		
	}

	

	private void updatingCouple(SubJobs j1, SubJobs j2) {
	double arrivalTime=j1.getstartServiceTime()+j2.getReqTime(); // cuando puede pasar el vehículo a recoger la enfermenra
	double departureTime=arrivalTime+test.getloadTimeHomeCareStaff(); // cuando puede pasar el vehículo a recoger la enfermenra	
	j2.setserviceTime(arrivalTime);// TW
	j2.setEndTime(arrivalTime);// TW
	j2.setarrivalTime(arrivalTime);
	j2.setStartServiceTime(arrivalTime);
	j2.setEndServiceTime(arrivalTime);
	j2.setdepartureTime(departureTime);
	}

	private boolean insertingIndividualClientJob(SubJobs j1, ArrayList<Route> routeSol, HashMap<String, SubJobs> subJobsID) {
		boolean inserted=false;
		if(subJobsID.containsKey(j1.getSubJobKey())) {
			for(Route r:routeSol) {
				inserted=insertingInTheRoute(r,j1);
				if(inserted) {
					break;
				}
			}
		}
		if(inserted) {
			subJobsID.remove(j1.getSubJobKey());
		}
		return inserted;
	}

	private boolean insertingInTheRoute(Route r, SubJobs j1) {
		boolean inserted=false;
		// iterando sobre la rute
		if(r.getSubJobsList().isEmpty()) { // en caso de que este vacia
			r.getSubJobsList().add(j1);
		}
		else {
			if(r.getSubJobsList().size()==1) { // en caso de que sólo haya un trabajo
				SubJobs a=r.getSubJobsList().get(0);
				if(a.getDepartureTime()>j1.getstartServiceTime()) {
					inserted=true;
					r.getSubJobsList().add(0,j1);
				}
				else {
					inserted=true;
					r.getSubJobsList().add(j1);
				}
			}
			else { // más de un trabajo
				inserted=readingRouteAndChangingTime(j1,r);
			}
		}
		return inserted;
	}

//	early=true;
//	double startTime=Math.max(j1.getStartTime(),possibleStartTime);
//	double ArrivalTime=startTime-test.getloadTimeHomeCareStaff();
//	j1.setarrivalTime(ArrivalTime);
//	j1.setStartServiceTime(startTime);
//	j1.setEndServiceTime(startTime+j1.getReqTime());
//	j1.setdepartureTime(j1.getendServiceTime());
	
	private boolean readingRouteAndChangingTime(SubJobs j1, Route r) {
		// checking if can be inserted with the current time
		boolean early=firstSubJob(j1,r.getSubJobsList().get(0));
		boolean late=false;
		boolean intermediateSubJob=false;
		boolean insertion=false;
		if(early) {
			r.getSubJobsList().add(0,j1);
		} 
		else {
			late=lastSubJob(j1,r.getSubJobsList().get(r.getSubJobsList().size()-1));
			if(late) {
			r.getSubJobsList().add(j1);}
		}
		if(!late && !early) {
			// structure (a)---(j1)---(b)
			for(int j=0;j<r.getSubJobsList().size()-1;j++) {
				SubJobs a=r.getSubJobsList().get(j);
				SubJobs b=r.getSubJobsList().get(j+1);
				double tvAj1=inp.getCarCost().getCost(a.getId()-1, j1.getId()-1);
				double tvj1B=inp.getCarCost().getCost(j1.getId()-1,b.getId()-1);
				if(a.getDepartureTime()+tvAj1<=j1.getArrivalTime() && j1.getDepartureTime()+tvj1B<=b.getArrivalTime()) {
					intermediateSubJob=true;
					r.getSubJobsList().add(j+1,j1);
					break;
				}
				else {
					if((j1.getEndTime()-j1.getStartTime()>0)) {
					if(a.getEndTime()<j1.getEndTime() && j1.getEndTime()<b.getEndTime()) {// changing time
						double posibleDepartureTime=b.getArrivalTime()-tvj1B;
						double posibleStartTime=posibleDepartureTime-j1.getReqTime();
						if(a.getDepartureTime()<posibleStartTime && posibleStartTime>=j1.getStartTime() &&posibleStartTime<=j1.getEndTime() && posibleStartTime<b.getArrivalTime() ) {
							intermediateSubJob=true;
							double startService=Math.max(j1.getStartTime(), posibleStartTime);
							double ArrivalTime=startService-test.getloadTimeHomeCareStaff();
							j1.setarrivalTime(ArrivalTime);
							j1.setStartServiceTime(startService);
							j1.setEndServiceTime(startService+j1.getReqTime());
							j1.setdepartureTime(j1.getendServiceTime());
						}
					}
				}
				}
			}
		}
		if(early || intermediateSubJob || late) {
			insertion=true;
		}
		return insertion;
	}

	private boolean lastSubJob(SubJobs j1, SubJobs subJobs2) {
		boolean late=false;
		double tv=inp.getCarCost().getCost(subJobs2.getId()-1, j1.getId()-1);
		if(subJobs2.getDepartureTime()+tv<=j1.getArrivalTime()) {
			late=true;
		}	
		return late;
	}

	private boolean firstSubJob(SubJobs j1, SubJobs s) {
		boolean early=false;
		double tv=inp.getCarCost().getCost(j1.getId()-1, s.getId()-1);
			if(j1.getDepartureTime()+tv<=s.getArrivalTime()) {
				early=true;
			}	
			else { // changing time
				if((j1.getEndTime()-j1.getStartTime())>0) {
				double possibleDeparture=s.getArrivalTime()-tv;
				double possibleStartTime=possibleDeparture-j1.getReqTime();
				if(j1.getStartTime()<=possibleStartTime &&j1.getEndTime()>=possibleStartTime && s.getArrivalTime()>possibleStartTime) {
					early=true;
					double startTime=Math.max(j1.getStartTime(),possibleStartTime);
					//double startTime=Math.max(j1.getStartTime(),possibleStartTime);
					double ArrivalTime=startTime-test.getloadTimeHomeCareStaff();
					j1.setarrivalTime(ArrivalTime);
					j1.setStartServiceTime(startTime);
					j1.setEndServiceTime(startTime+j1.getReqTime());
					j1.setdepartureTime(j1.getendServiceTime());
				}
			}
			}	
		return early;
	}

	private HashMap<String, SubJobs> poolSubJobsToAssign(ArrayList<SubJobs> subJobsToAssign, ArrayList<Route> remainingRoutes) {
		HashMap<String, SubJobs> list= new HashMap<String, SubJobs>();
		for(SubJobs j:subJobsToAssign) {
			list.put(j.getSubJobKey(), j);
		}
		for(Route r: remainingRoutes) {
			for(SubJobs j:r.getSubJobsList()) {
				subJobsToAssign.add(j);
				list.put(j.getSubJobKey(), j);
			}
		}
		return list;
	}

	private ArrayList<SubJobs> missingAssigments(ArrayList<Route> walkingRoutes) {
		ArrayList<SubJobs> subJobsToAssign=new ArrayList<SubJobs>();
		for(Route r:walkingRoutes) {
			for(SubJobs j:r.getSubJobsList()) {
				subJobsToAssign.add(j);
			}
		}
		return subJobsToAssign;
	}

	private boolean arewalkingRoutesIntheRotue(Route r) {
		boolean isWalkingRotue=false;
		for(SubJobs j:r.getSubJobsList()) {
			if(j.isClient()) {
				SubJobs j1=(SubJobs)this.coupleList.get(j.getSubJobKey()).getStartEndNodes().get(1);
				SubJobs j2=(SubJobs)this.coupleList.get(j.getSubJobKey()).getStartEndNodes().get(0);
				if(j1.getEndTime()-j1.getStartTime()==0 && j2.getEndTime()-j2.getStartTime()==0) {
					isWalkingRotue=true;
					break;
				}
			}
		}
		return isWalkingRotue;
	}

	private ArrayList<Route> assigmentWalkingRouteToVehicle(ArrayList<Route> walkingRoutes) {
		ArrayList<Route> routeSol=new ArrayList<Route>();
		for(int i=0;i<inp.getVehicles().get(0).getQuantity();i++) {
			routeSol.add(new Route());
		}
		for(Route r:walkingRoutes) {// toda ruta que contenga al menos una walking route se selecciona para ser asignada
			assignningBigJobs(r,routeSol);
		}
		return routeSol;
	}

	private void assignningBigJobs(Route r, ArrayList<Route> routeSol) {
		//
		boolean insertion=false;
		HashMap<String,SubJobs> listJobs= new  HashMap<String,SubJobs>();
		LinkedList<SubJobs> jobs= new LinkedList<SubJobs>();
		for(SubJobs j:r.getSubJobsList()) {
			jobs.add(j);
			listJobs.put(j.getSubJobKey(), j);
		}

		for(SubJobs j:jobs) {
			if(listJobs.containsKey(j.getSubJobKey())) {
				if((j.getEndTime()-j.getStartTime())==0) {
					insertion=assignWalkingRouteVehicle(listJobs,j,routeSol);
				}
			}
		}
		if(insertion) {
			for(SubJobs j:jobs) {
				r.getSubJobsList().remove(j);
			}
		}
	}

	private boolean assignWalkingRouteVehicle(HashMap<String, SubJobs> listJobs, SubJobs j, ArrayList<Route> routeSol) {
		// se inserta sea como sea los dos subJobs asociados a j
		boolean inserted=false; // true cuando se insertan las dos partes
		SubJobs j1=(SubJobs)this.coupleList.get(j.getSubJobKey()).getStartEndNodes().get(1);
		SubJobs j2=(SubJobs)this.coupleList.get(j.getSubJobKey()).getStartEndNodes().get(0);

		for(Route route:routeSol) {// se asigna a una ruta
			inserted=assigningJob(j1,route,listJobs);
			if(inserted){
				// se inserta la segunda parte
				inserted=assigningJob(j2,route,listJobs);
			}
			if(inserted) {
				break;
			}
		}
		if(!inserted) { // se creo una nueva ruta
			Route r=new Route();
			if(listJobs.containsKey(j1.getSubJobKey())) {
				r.getSubJobsList().add(j1);
				listJobs.remove(j1.getSubJobKey());
			}
			if(listJobs.containsKey(j2.getSubJobKey())) {
				r.getSubJobsList().add(j2);
				listJobs.remove(j2.getSubJobKey());
			}
			routeSol.add(r);
		}
		return inserted;
	}

	private boolean assigningJob(SubJobs j1, Route route, HashMap<String, SubJobs> listJobs) {
		boolean inserted=false;
		if(listJobs.containsKey(j1.getSubJobKey())) {
			if(route.getSubJobsList().isEmpty()) {
				inserted=true;
				route.getSubJobsList().add(j1);	
			}
			else {
				inserted=readingSubJobList(route,j1);
			}
		}
		if(inserted) {
			listJobs.remove(j1.getSubJobKey());
		}
		return inserted;
	}

	private boolean readingSubJobList(Route route, SubJobs j1) {
		// structure (a)---(j1)---(b)
		boolean inserted=false;
		for(int i=0;i<route.getSubJobsList().size()-1;i++){
			SubJobs a=route.getSubJobsList().get(i);

			SubJobs b=route.getSubJobsList().get(i+1);
			if(a.getDepartureTime()<j1.getArrivalTime() && j1.getDepartureTime()<b.getArrivalTime()) {
				inserted=true;
				route.getSubJobsList().add(i+1,j1);
			}

		}

		if(route.getSubJobsList().size()==1) {
			SubJobs a=route.getSubJobsList().get(0);
			// si sólo hay un trabajo
			if(a.getstartServiceTime()>j1.getstartServiceTime()) {
				inserted=true;
				route.getSubJobsList().add(0,j1);
			}
			else {
				inserted=true;
				route.getSubJobsList().add(j1);
			}
		}
		return inserted;
	}

	private boolean checkDoubleJobs(Solution newSol) {
		boolean uniqueJobs=true;
		HashMap<String, SubJobs> storeJobs= new HashMap<>();
		for(Route r:newSol.getRoutes()) {
			for(SubJobs j:r.getSubJobsList()) {
				if(j.getId()!=1) {
					if(!storeJobs.containsKey(j.getSubJobKey())) {
						storeJobs.put(j.getSubJobKey(), j);
					}
					else {
						uniqueJobs=false; 
						break;
					}
				}
			}
			if(!uniqueJobs) {
				break;
			}
		}
		return uniqueJobs;
	}

	private Solution sclackTime(Solution newSol) {
		// compute: arrival time, start service time, end service time, departure time
		Solution copySolution= new Solution(initialSol);
		for(Route r:copySolution.getRoutes()) {
			int routeLength=r.getSubJobsList().size()-1;
			for(int i=routeLength;i>0;i--) {
				// Last subjob
				SubJobs jSubJob=r.getSubJobsList().get(i);

				// additional time
				double additionalTime=0;
				if(jSubJob.isClient()) {
					additionalTime=test.getloadTimeHomeCareStaff();
				}
				else {
					if(jSubJob.getId()!=1) {
						additionalTime=test.getloadTimeHomeCareStaff();	
					}
				}

				jSubJob.setarrivalTime(jSubJob.getstartServiceTime()-additionalTime);
				jSubJob.setdepartureTime(jSubJob.getstartServiceTime()+additionalTime);
				// previous to the last subjob
				SubJobs iSubJob=r.getSubJobsList().get(i-1);
				double tv=inp.getCarCost().getCost(iSubJob.getId()-1, jSubJob.getId()-1);
				double newDepartureTime=jSubJob.getArrivalTime()-tv; // el vehículo parte del nodo
				if(newDepartureTime<=iSubJob.getEndTime() && newDepartureTime>= iSubJob.getStartTime()) {
					iSubJob.setStartServiceTime(newDepartureTime);
					iSubJob.setEndServiceTime(iSubJob.getstartServiceTime()+iSubJob.getReqTime());
				}
			}
			r.updateRouteFromParts(inp,test);
		}

		return copySolution;
	}

	private boolean checkingSubJobs(Solution refSolution,Solution test) {
		boolean merge=false;
		for(Route r:refSolution.getRoutes()) {
			for(SubJobs s:r.getSubJobsList()){
				if(s.getId()!=1) {
					assignedSubJobs.put(s.getSubJobKey(), s);
				}
			}
		}

		HashMap<String, SubJobs>list= new HashMap<String, SubJobs>();
		for(Route route:test.getRoutes()){
			for(SubJobs s:route.getSubJobsList()) {
				list.put(s.getSubJobKey(), s);
			}

		}
		for(SubJobs ss:assignedSubJobs.values()) {
			if(list.containsKey(ss.getSubJobKey())) {
				list.remove(ss.getSubJobKey());
			}
			else {missingubJobs.put(ss.getSubJobKey(), ss);}
		}
		if(missingubJobs.isEmpty()){
			merge=true;	
		}

		return merge;
	}

	private void changingDepartureTimes(Solution copySolution) {
		// se cambian los departure times de cada subjob en cada parte
		for(Route r:copySolution.getRoutes()) {
			for(Parts a:r.getPartsRoute()) {
				changinPartTime(a);
			}	
			r.updateRouteFromParts(inp,test);
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

	private Solution mergingRoutes(Solution copySolution) {
		// revisar la capacidad del vehículo en tiempos de 
		Solution Sol= interMergingParts(copySolution); // 1 merging parts (without complete parts)

		updatingSolution(Sol);

		settingNewPart(Sol);

		boolean merge= checkingSubJobs(Sol,copySolution);

		System.out.println("primer merging solution");
		System.out.println(Sol.toString());


		Solution newSol= intraMergingParts(Sol); // 2 merging parts (splited the parts) // here the detours are considered
		updatingSolution(newSol);
		merge= checkingSubJobs(Sol,newSol);
		Solution reducingRoutes= slackingTimes(newSol);

		System.out.println("second merging solution");
		System.out.println(newSol.toString());
		System.out.println("end");
		return newSol;
	}

	private Solution slackingTimes(Solution Sol) {
		Solution newSol= new Solution(Sol);
		// se seleccional las rutas potenciales a eliminar
		ArrayList<Route> potencialRoutesToRemove=selectingRouteWithdependentJobs(newSol); // es importante que sólo trabajos relacionados al home health care service
		// los trabajos contenidos en la rutas a eliminar se tienen que reasignar
		ArrayList<SubJobs> subJobsToRelocate=extractingSubJobs(potencialRoutesToRemove);
		// seleccionar los trabajos y reubicar y uno a uno intentar insertarlos
		for(SubJobs s:subJobsToRelocate) {
			// llamar la ruta que tiene
			Route routeToAjust=callingRoutePair(newSol,s);
			if(routeToAjust!=null) {
				routeToAjust=insertingSubJobs(routeToAjust,s);
			}
		}
		// update the routes
		return newSol;
	}

	private Route callingRoutePair(Solution newSol, SubJobs s) {
		Route r=null;
		Couple n=coupleList.get(s.getSubJobKey());
		SubJobs mainElement=null;
		if(n.getPresent().getSubJobKey().equals(s.getSubJobKey())) {
			mainElement=(SubJobs)n.getFuture();
		}
		else {
			mainElement=(SubJobs)n.getFuture();
		}
		for(Route route:newSol.getRoutes()) {
			if(route.getJobsDirectory().containsKey(mainElement.getSubJobKey())) {
				if(mainElement.getEndTime()-mainElement.getStartTime()>0) {
					r=route;
					break;
				}
				else {
					break;
				}
			}
		}
		return r;
	}

	private Route insertingSubJobs(Route currentRoutes, SubJobs s) {
		Route r= new Route();

		return currentRoutes;
	}

	private ArrayList<SubJobs> extractingSubJobs(ArrayList<Route> potencialRoutesToRemove) {
		ArrayList<SubJobs> subJobsToRelocate=new ArrayList<SubJobs>();
		for(Route r: potencialRoutesToRemove) {
			for(SubJobs s:r.getSubJobsList()) {
				if(s.isClient()) {
					subJobsToRelocate.add(s);
				}
			}
		}
		return subJobsToRelocate;
	}

	private ArrayList<Route> selectingRouteWithdependentJobs(Solution newSol) {
		ArrayList<Route> potencialRoutesToRemove=new ArrayList<Route>(); 
		for(Route r:newSol.getRoutes()) {
			boolean toInsert=true;
			for(SubJobs s:r.getSubJobsList()) {
				double tw=s.getEndTime()-s.getStartTime();
				if(tw>0) {
					toInsert=false;
					break;
				}
			}
			if(toInsert) {
				potencialRoutesToRemove.add(r);
			}

		}
		return potencialRoutesToRemove;
	}



	private void settingNewPart(Solution sol) {
		for(Route r:sol.getRoutes()) {
			int passengers=1;
			ArrayList<Parts> parts= new ArrayList<>();
			ArrayList<SubJobs> listSubJobs= new ArrayList<>();
			parts.add(r.getPartsRoute().get(0));
			for(SubJobs j:r.getSubJobsList()) {
				passengers+=j.getTotalPeople();
				listSubJobs.add(j);
				if(passengers==0) {
					Parts part=new Parts();
					if(!listSubJobs.isEmpty()) {
						part.setListSubJobs(listSubJobs, inp, test);
						parts.add(part);}
					listSubJobs= new ArrayList<>();
				}
				if(r.getSubJobsList().getLast()==j) {
					Parts part=new Parts();
					if(!listSubJobs.isEmpty()) {
						part.setListSubJobs(listSubJobs, inp, test);
						parts.add(part);}
				}
			}
			parts.add(r.getPartsRoute().getLast());
			r.getPartsRoute().clear();
			for(Parts p:parts) {
				r.getPartsRoute().add(p);
			}
		}
	}

	private Solution intraMergingParts(Solution copySolution) {

		Solution newSol= new Solution(copySolution); // 1. copy solution
		// 2. Seleccionar las rutas que se van a mezclar
		ArrayList<Route> copyRoute = copyListRoute(copySolution); // copy original route for safe
		for(Route refRoute:copySolution.getRoutes()) {
			for(Route toSplit:copySolution.getRoutes()) {
				if(toSplit.getPartsRoute().get(1).getListSubJobs().get(0).getId()==25 || refRoute.getPartsRoute().get(1).getListSubJobs().get(0).getId()==25) {
					System.out.println("***Parte vacia");
				}
				boolean mergingParts= insertingPartIntoPart(refRoute,toSplit); // true <- cuando las rutas estan cambiando (se mezclan) false <-cuando las rutas permanecen igual
				System.out.println("***Parte vacia");
			}
		}


		System.out.println("***Parte vacia");
		System.out.println(newSol.toString());

		return newSol;
	}

	private boolean insertingPartIntoPart(Route refRoute, Route toSplit) {
		boolean merging=false;
		boolean mergingParts=false;
		if((refRoute.getDurationRoute()+toSplit.getDurationRoute())<=test.getRouteLenght()) {
			//if((refRoute.getTravelTime()+refRoute.getloadUnloadRegistrationTime()+toSplit.getTravelTime()+toSplit.getloadUnloadRegistrationTime())<test.getRouteLenght()) {
			mergingParts=true;
		}
		if(refRoute!=toSplit && mergingParts) { // sólo se pueden mezclar si son rutas diferentes
			// se tiene que tener cuidado con las working horas y con los detours
			boolean node=false;


			Route earlyRoute=selecctingStartRoute(refRoute,toSplit);
			Route lateRoute=selecctingRouteToInsert(refRoute,toSplit);
			if(earlyRoute==refRoute) {
				Route newRoute= new Route(); // la ruta que va a almacenar 

				// 1. Evaluar mezclar las primeras partes 
				boolean mergingHeading=seatingHeading(earlyRoute,lateRoute,newRoute);
				// 2. Evaluar mezclar las partes intermedias
				if(mergingHeading && node) {
					System.out.println("Stop");
				}
				boolean mergingIntermediateParts=settingIntermediateParts(earlyRoute,lateRoute,newRoute);
				if(mergingIntermediateParts && node) {
					System.out.println("Stop");
				}

				// 3. Evaluar mezclar las ultimas partes
				//TO DO: boolean mergingTail=settingTailsParts(earlyRoute,lateRoute,mergingPart);
				// 4. Los trabajos contenidos en la nueva ruta corresponden a los nuevos trabajos que va a integrar la ruta más temprana
				boolean mergingLastParts=settingTailsParts(earlyRoute,lateRoute,newRoute);
				updatingRefRoute(earlyRoute,newRoute);
				updatingLateRoute(lateRoute,earlyRoute,newRoute); // los trabajos que aparecen en la ruta nueva no tienen porque aparecer en la ruta vieja
				System.out.println("Stop");
				System.out.println("Stop");
				System.out.println("Route Late"  +lateRoute.toString());
				System.out.println("Stop");}
		}
		return merging;
	}



	private boolean settingTailsParts(Route earlyRoute, Route lateRoute, Route newRoute) {
		boolean merge =false;
		ArrayList<Parts> partsList=selectingParts(earlyRoute,lateRoute);// 0<- parte más larga 1<- parte más corta

		if(partsList.get(0).getListSubJobs().size()>1 && partsList.get(1).getListSubJobs().size()>1 ) {
			merge=settingTailsRoute(partsList,newRoute);	
		}
		else {
			merge=settingTails(partsList,newRoute);
		}
		if(!merge) {
			if(earlyRoute.getPartsRoute().size()>3) {
				Parts p=earlyRoute.getPartsRoute().get(earlyRoute.getPartsRoute().size()-2);
				Parts newPart=new Parts(p);
				newRoute.getPartsRoute().add(newPart);
				newPart.settingConnections( partsList.get(0),  partsList.get(1));
				newRoute.getPartsRoute().add(newPart);
			}
			Parts newPart=new Parts(earlyRoute.getPartsRoute().getLast());
			newRoute.getPartsRoute().add(newPart);
			//newRoute.getSubJobsList().add(earlyRoute.getPartsRoute().getLast().getListSubJobs().get(0));
		}
		else {
			Parts newPart=new Parts(earlyRoute.getPartsRoute().getLast());
			newRoute.getPartsRoute().add(newPart);
			newPart.settingConnections( partsList.get(0),  partsList.get(1));
			newRoute.getPartsRoute().add(newPart);
			//newRoute.getSubJobsList().add(earlyRoute.getPartsRoute().getLast().getListSubJobs().get(0));
		}

		return merge;
	}



	private boolean settingTails(ArrayList<Parts> partsList, Route newRoute) {
		boolean merge=false;
		ArrayList<SubJobs> list=new ArrayList<SubJobs>();


		SubJobs toInsert=partsList.get(1).getListSubJobs().get(0);
		SubJobs depot=new SubJobs(inp.getNodes().get(0));
		Edge edgeShortPart=new Edge(toInsert,depot,inp,test);
		merge=earlyInsertion(toInsert,edgeShortPart,partsList);
		if(merge) {
			newRoute.getSubJobsList().add(toInsert);
			for(int i=0;i<partsList.get(0).getListSubJobs().size();i++) {
				newRoute.getSubJobsList().add(partsList.get(0).getListSubJobs().get(i));
			}
		}
		else {
			for(int i=0;i<partsList.get(0).getListSubJobs().size()-1;i++) {
				//detour=checkingDetour(edgeLongPart,edgeShortPart,partsList);
				String key=partsList.get(0).getListSubJobs().get(i).getSubJobKey()+partsList.get(0).getListSubJobs().get(i+1).getSubJobKey();
				Edge edgeLongPart=partsList.get(0).getDirectoryConnections().get(key);	
				double remainingDistanceshortPart=remainingDistance(-1,partsList);// el toInsert es más tarde que el primer trabajo en la parte larga
				if(remainingDistanceshortPart<=edgeShortPart.getDetour()) {
					double origentoInsert=inp.getCarCost().getCost(edgeLongPart.getOrigin().getId()-1, toInsert.getId()-1);
					double toInsertEnd=inp.getCarCost().getCost(toInsert.getId()-1,edgeLongPart.getEnd().getId()-1);
					double distance=origentoInsert+toInsertEnd;
					if(distance<=edgeLongPart.getDetour()) { // detour insertando el nodo toInsert en la connexion edgeLongPart
						if(edgeLongPart.getOrigin().getDepartureTime()+origentoInsert<toInsert.getArrivalTime()) {// ventana de tiempo
							if(toInsert.getDepartureTime()+toInsertEnd<edgeLongPart.getEnd().getArrivalTime()) {// ventana de tiempo

								insertingTailRoute(list,partsList,i,0);
								if(vehicleCapacityPart(list)) {
									merge=true;
								}
								else {
									list.clear();
								}
							}
						}
					}
				}
				if(!merge) {
					if(!list.isEmpty()) {
						for(SubJobs k:list) {
							newRoute.getSubJobsList().add(k);
						}
					}
					break;
				}
			}
		}
		return merge;
	}

	private boolean vehicleCapacityPart(ArrayList<SubJobs> list) {
		boolean enoughCapacity=false;
		int passegers=0;
		int action=0;
		for(SubJobs s: list) {
			action=s.getTotalPeople();
			passegers+=action;
		}
		if(Math.abs(passegers)>inp.getVehicles().get(0).getMaxCapacity()) {
			enoughCapacity=false;
		}
		else {
			enoughCapacity=true;
		}
		return enoughCapacity;
	}

	private boolean earlyInsertion(SubJobs toInsert, Edge edgeShortPart, ArrayList<Parts> partsList) {
		boolean merge=false;
		if(toInsert.getDepartureTime()<partsList.get(0).getListSubJobs().get(0).getDepartureTime() && toInsert.getDepartureTime()<partsList.get(0).getListSubJobs().get(0).getArrivalTime()) {
			double remainingDistanceshortPart=remainingDistance(-1,partsList);// el toInsert es más tarde que el primer trabajo en la parte larga
			if(remainingDistanceshortPart<=edgeShortPart.getDetour()) {
				merge=true;
			}	
		}
		return merge;
	}

	private boolean settingTailsRoute(ArrayList<Parts> partsList, Route newRoute) {
		boolean merging=false;
		ArrayList<SubJobs> list=new ArrayList<SubJobs>();
		Parts newPart=new Parts();
		for(int i=0;i<partsList.get(0).getListSubJobs().size()-1;i++) {// large {0,1,2,3}
			for(int j=0;j<partsList.get(1).getListSubJobs().size()-1;j++) { // short {a,b}
				//{a,0,b,1,2,3} or {0,a,1,b,2,3}
				merging=false;
				boolean detour=false;
				String key=partsList.get(0).getListSubJobs().get(i).getSubJobKey()+partsList.get(0).getListSubJobs().get(i+1).getSubJobKey();
				Edge edgeLongPart=partsList.get(0).getDirectoryConnections().get(key);


				key=partsList.get(1).getListSubJobs().get(j).getSubJobKey()+partsList.get(1).getListSubJobs().get(j+1).getSubJobKey();
				Edge edgeShortPart=partsList.get(1).getDirectoryConnections().get(key);

				SubJobs j1=edgeShortPart.getOrigin();
				SubJobs j2=edgeShortPart.getEnd();
				detour=checkingDetour(edgeLongPart,edgeShortPart,partsList);
				if(detour) {
					double tvShortLarge=inp.getCarCost().getCost(edgeShortPart.getOrigin().getId()-1, edgeLongPart.getOrigin().getId()-1);
					if(edgeShortPart.getOrigin().getDepartureTime()+tvShortLarge<=edgeLongPart.getOrigin().getArrivalTime()) {// checking time window (short O)---(large O)---(short E)---(large E)
						double tvLargeShort=inp.getCarCost().getCost(edgeLongPart.getOrigin().getId()-1, edgeShortPart.getEnd().getId()-1);
						if(edgeLongPart.getOrigin().getDepartureTime()+tvLargeShort<=edgeShortPart.getEnd().getArrivalTime()) {// checking time window (large O)---(short E)
							double tvShortLargeE=inp.getCarCost().getCost(edgeShortPart.getEnd().getId()-1, edgeLongPart.getEnd().getId()-1);
							if(edgeShortPart.getEnd().getDepartureTime()+tvShortLargeE<=edgeLongPart.getEnd().getArrivalTime()) {// checking time window (short E)---(large E)

								insertingTailRoute(list,partsList,i,j);
								if(vehicleCapacityPart(list)) {
									merging=true;
								}
								else {
									list.clear();
								}
							}	
						}	
					}
					if(!merging && (i+1)==(partsList.get(0).getListSubJobs().size()-1) ) {
						// (short O)---(Last subJob Large O)--- (short End) ----(resto de la parte short)---(Depot)
						if(edgeShortPart.getOrigin().getDepartureTime()+tvShortLarge<=edgeLongPart.getOrigin().getArrivalTime()) {// checking TW
							double tvLargeShort=inp.getCarCost().getCost(edgeLongPart.getEnd().getId()-1, edgeShortPart.getEnd().getId()-1);
							if(edgeLongPart.getOrigin().getDepartureTime()+tvLargeShort<=edgeShortPart.getEnd().getArrivalTime()) {
								double remainingDistanceshortPart=remainingDistance(j,partsList);
								double currentDistDepot=inp.getCarCost().getCost(edgeLongPart.getEnd().getId()-1, inp.getNodes().get(0).getId()-1)*(1+test.getDetour());
								if(remainingDistanceshortPart<=currentDistDepot) {// detour long
									insertingTailRoute(list,partsList,i,j);
									if(vehicleCapacityPart(list)) {
										merging=true;
									}
									else {
										list.clear();
									}
								}
							}
						}
					}	
				}
				if(!merging) {
					break;
				}
				else {
					if(!list.isEmpty()) {
						newPart.setListSubJobs(list, inp, test);
						//						for(SubJobs k:list) {
						//							newRoute.getSubJobsList().add(k);
						//						}
					}
				}
			}
		}
		if(!merging) {
			String key=partsList.get(0).getListSubJobs().get(partsList.get(0).getListSubJobs().size()-2).getSubJobKey()+partsList.get(0).getListSubJobs().get(partsList.get(0).getListSubJobs().size()-1).getSubJobKey();
			Edge edgeLongPart=partsList.get(0).getDirectoryConnections().get(key);
			key=partsList.get(1).getListSubJobs().get(partsList.get(1).getListSubJobs().size()-2).getSubJobKey()+partsList.get(1).getListSubJobs().get(partsList.get(1).getListSubJobs().size()-1).getSubJobKey();
			Edge edgeShortPart=partsList.get(1).getDirectoryConnections().get(key);
			if(edgeShortPart==null) {
				System.out.print("stop");
			}
			boolean detour=checkingDetour(edgeLongPart,edgeShortPart,partsList);
			if(detour) {
				double tvShortLarge=inp.getCarCost().getCost(edgeShortPart.getOrigin().getId()-1,edgeLongPart.getOrigin().getId()-1);
				if(edgeShortPart.getOrigin().getDepartureTime()+tvShortLarge<=edgeLongPart.getOrigin().getArrivalTime()) {// checking TW
					double tvLargeShort=inp.getCarCost().getCost(edgeLongPart.getEnd().getId()-1, edgeShortPart.getEnd().getId()-1);
					if(edgeLongPart.getOrigin().getDepartureTime()+tvLargeShort<=edgeShortPart.getEnd().getArrivalTime()) {
						double remainingDistanceshortPart=remainingDistance(0,partsList);
						double currentDistDepot=inp.getCarCost().getCost(edgeLongPart.getEnd().getId()-1, inp.getNodes().get(0).getId()-1)*(1+test.getDetour());
						if(remainingDistanceshortPart<=currentDistDepot) {// detour long
							merging=true;
							insertingTailRoute(list,partsList,partsList.get(0).getListSubJobs().size()-2,0);
							if(!list.isEmpty()) {
								newPart.setListSubJobs(list, inp, test);
							}
						}
					}
				}
			}
		}
		return merging;
	}



	private boolean checkingDetour(Edge edgeLongPart, Edge edgeShortPart, ArrayList<Parts> partsList) {
		boolean feasible=false;
		if(edgeLongPart==null) {
			System.out.print("stop");
		}
		if(edgeShortPart==null) {
			System.out.print("stop");
		}
		SubJobs jA=edgeLongPart.getOrigin();
		SubJobs jB=edgeLongPart.getEnd();
		SubJobs j1=edgeShortPart.getOrigin();
		SubJobs j2=edgeShortPart.getEnd();
		String key=jA.getSubJobKey()+jB.getSubJobKey();
		Edge connectionLarge=partsList.get(0).getDirectoryConnections().get(key);
		key=j1.getSubJobKey()+j2.getSubJobKey();
		Edge connectionShort=null;
		if(!partsList.get(1).getDirectoryConnections().isEmpty()) {
			connectionShort=partsList.get(1).getDirectoryConnections().get(key);			
		}
		else {
			connectionShort=new Edge(j1,j2,inp,test);
		}
		double distJAj1JB=inp.getCarCost().getCost(jA.getId()-1, j1.getId()-1)+inp.getCarCost().getCost(j1.getId()-1, jB.getId()-1);
		double distJ1jBJ2=inp.getCarCost().getCost(j1.getId()-1, jB.getId()-1)+inp.getCarCost().getCost(jB.getId()-1, j2.getId()-1);
		if(distJAj1JB<=connectionLarge.getDetour() && distJ1jBJ2<=connectionShort.getDetour()) {
			feasible=true;
		}
		return feasible;
	}

	private void insertingTailRoute(ArrayList<SubJobs> list, ArrayList<Parts> partsList, int i2, int j2) {
		for(int i=i2;i<partsList.get(0).getListSubJobs().size();i++) {
			if(i<partsList.get(0).getListSubJobs().size()-1) {
				list.add(partsList.get(0).getListSubJobs().get(i));
			}
			else {
				list.add(partsList.get(1).getListSubJobs().get(j2));
				list.add(partsList.get(0).getListSubJobs().get(i));
				for(int j=j2+1;j<partsList.get(1).getListSubJobs().size();j++) {
					list.add(partsList.get(1).getListSubJobs().get(j));
				}
			}

		}

	}

	private double remainingDistance(int j, ArrayList<Parts> partsList) {
		double distance=0;
		SubJobs lastLarge=partsList.get(0).getListSubJobs().get(partsList.get(0).getListSubJobs().size()-1);
		SubJobs startShort=partsList.get(1).getListSubJobs().get(j+1);
		double distNewConnectionLargeShort=inp.getCarCost().getCost(lastLarge.getId()-1, startShort.getId()-1);
		double distConnectionDepot=inp.getCarCost().getCost(partsList.get(1).getListSubJobs().get(partsList.get(1).getListSubJobs().size()-1).getId()-1, inp.getNodes().get(0).getId()-1);
		double distPart=0;
		for(int k=j+1;k<partsList.get(1).getListSubJobs().size()-1;k++) {
			String key=partsList.get(1).getListSubJobs().get(k).getSubJobKey()+partsList.get(1).getListSubJobs().get(k+1).getSubJobKey();
			Edge partEdge=partsList.get(1).getDirectoryConnections().get(key);
			distPart+=partEdge.getTime();
		}
		distance=distNewConnectionLargeShort+distPart+distConnectionDepot;
		return distance;
	}

	private ArrayList<Parts> selectingParts(Route earlyRoute, Route lateRoute) {
		ArrayList<Parts> partsList= new ArrayList<Parts>();
		int lengthEarly=earlyRoute.getPartsRoute().size()-1;
		int lengthLate=lateRoute.getPartsRoute().size()-1;
		if(earlyRoute.getPartsRoute().get(lengthEarly-1).getListSubJobs().size()>=lateRoute.getPartsRoute().get(lengthLate-1).getListSubJobs().size()) {
			partsList.add(0,earlyRoute.getPartsRoute().get(lengthEarly-1));
			partsList.add(lateRoute.getPartsRoute().get(lengthLate-1));
		}
		else {
			partsList.add(0,lateRoute.getPartsRoute().get(lengthLate-1));
			partsList.add(earlyRoute.getPartsRoute().get(lengthEarly-1));
		}
		return partsList;
	}

	private void updatingLateRoute(Route lateRoute, Route x, Route newRoute) {
		HashMap<String,Parts> listSubJobs=new HashMap<String,Parts>(); // copy of current list Jobs
		HashMap<String,SubJobs> newSubJobs=new HashMap<String,SubJobs>(); // new list Jobs
		for(Parts p:newRoute.getPartsRoute()) {
			for(SubJobs j:p.getListSubJobs()) {
				if(j.getId()!=1) {
					newSubJobs.put(j.getSubJobKey(),j);}
			}
		}

		for(Parts j:lateRoute.getPartsRoute()) {
			listSubJobs.put(j.getKey(),j);
		}

		for(Parts p:listSubJobs.values()) {
			boolean removePart=false;
			for(SubJobs s:p.getListSubJobs()) {
				if(newSubJobs.containsKey(s.getSubJobKey())) {
					removePart=true;
				}
				if(removePart) {
					break;
				}
			}
			if(removePart) {
				removePart=false;
				lateRoute.getPartsRoute().remove(p);
				break;
			}
		}
		lateRoute.updateRouteFromParts(inp,test);
		for(Parts p:lateRoute.getPartsRoute()) {
			if(p.getListSubJobs().isEmpty()) {
				System.out.println("stop");
			}
			p.settingConnections(p.getListSubJobs(),inp,test);
		}

	}

	private void updatingRefRoute(Route earlyRoute, Route newRoute) {
		earlyRoute.getSubJobsList().clear();
		//		for(SubJobs s:newRoute.getSubJobsList()) {
		//			earlyRoute.getSubJobsList().add(s);
		//		}
		for(Parts p:newRoute.getPartsRoute()) {
			for(SubJobs s:p.getListSubJobs()) {
				if(s.getId()!=1) {
					earlyRoute.getSubJobsList().add(s);
				}
			}
		}
		earlyRoute.updateRouteFromParts(inp,test);
		//earlyRoute.updateRouteFromSubJobsList(inp,test);
		for(Parts p:earlyRoute.getPartsRoute()) {
			p.settingConnections(p.getListSubJobs(),inp,test);
		}
	}

	private boolean settingIntermediateParts(Route earlyRoute, Route lateRoute, Route newRoute) {
		// este método no esta definido. Esta provisional
		// 1. Es necesario indetificar el tipo de partes a mezclar

		boolean merging=false;
		// empieza a mezclar desde la parte 2 de las rutas
		for(int partEarly=2; partEarly<(earlyRoute.getPartsRoute().size()-2);partEarly++) {
			System.out.print("Size"+ (lateRoute.getPartsRoute().size()-1));
			for(int partLate=2; partLate<(lateRoute.getPartsRoute().size()-2);partLate++) {
				System.out.print("Current partt"+ partLate);
				int options=typeOfParts(earlyRoute.getPartsRoute().get(partEarly),lateRoute.getPartsRoute().get(partLate)); 
				switch(options) {
				case 1 :// 1 patient-patient 

					merging=mergingItermediatePartsPatientPatient(options,earlyRoute.getPartsRoute().get(partEarly),lateRoute.getPartsRoute().get(partLate), newRoute,earlyRoute,lateRoute);// option1
					break;

				case 2 : // 2 homeCare-patient or patient-homeCare
					merging=mergingItermediatePartsPatientClient(options,earlyRoute.getPartsRoute().get(partEarly),lateRoute.getPartsRoute().get(partLate), newRoute);// option1

					break; // optional

				default : // 3 homeCare-homeCare

					merging=mergingItermediatePartsClients(earlyRoute.getPartsRoute().get(partEarly),lateRoute.getPartsRoute().get(partLate), newRoute);// option1
				}
			}	
		}

		for(int i=2;i<earlyRoute.getPartsRoute().size()-2;i++) {
			Parts p=earlyRoute.getPartsRoute().get(i);
			for(SubJobs s:p.getListSubJobs()) {
				if(!newRoute.getSubJobsList().contains(s)) {
					newRoute.getSubJobsList().add(s);
				}}
		}
		//	newRoute.getSubJobsList().add(earlyRoute.getPartsRoute().getLast().getListSubJobs().get(0));
		return merging;
	}

	private boolean mergingItermediatePartsPatientClient(int options, Parts earlyRoute, Parts lateRoute, Route newRoute) {
		// structure (pik-up)---(drop-off)---(pik-up)---(drop-off)
		boolean merge=false;
		ArrayList<Parts> ordering=orderingParts(earlyRoute,lateRoute);
		merge=notequalMerge(ordering,newRoute);
		//		if(!merge) {
		//			merge=changeTimePatientClient(ordering,newRoute);
		//		}
		return merge;
	}

	private boolean changeTimePatientClient(ArrayList<Parts> ordering, Route newRoute) {
		boolean merge=false;

		return merge;
	}

	private boolean notequalMerge(ArrayList<Parts> ordering, Route newRoute) {
		boolean merging= false;
		//
		ArrayList<SubJobs> listSubJobs= new ArrayList<SubJobs>(); // vector to store the new part
		Parts newPart=new Parts();

		// list of jobs - Late
		SubJobs pickUp1Late=ordering.get(1).getListSubJobs().get(0);
		SubJobs dropOff1Late=ordering.get(1).getListSubJobs().get(1);
		for(SubJobs s:ordering.get(0).getListSubJobs()) { // (ordering.get(1))---(ordering.get(0))---(ordering.get(1))

			double tvPickUp1LatePickUp1Early=inp.getCarCost().getCost(pickUp1Late.getId()-1, s.getId()-1);
			boolean waintingTime=checkWaitingTime(s,tvPickUp1LatePickUp1Early,pickUp1Late);
			if(pickUp1Late.getDepartureTime()+tvPickUp1LatePickUp1Early<=s.getArrivalTime() && waintingTime) {// (ordering.get(1))---(ordering.get(0))
				String key="";
				boolean feasibledetour=checkFeasibilityDetour(s,ordering.get(0),pickUp1Late);
				if(feasibledetour) { // check detour
					for(SubJobs next:ordering.get(0).getListSubJobs()) { // trying to insert the next part (ordering.get(0))---(ordering.get(1))
						double tvnextdropOff1Late= inp.getCarCost().getCost(next.getId(), dropOff1Late.getId());
						if(next.getDepartureTime()+tvnextdropOff1Late<=dropOff1Late.getArrivalTime()) {// (ordering.get(0))---(ordering.get(1))
							// home care
							double detour=computingdetour(s,next,ordering.get(0));
							double detourDuration=detour+tvPickUp1LatePickUp1Early+tvnextdropOff1Late;
							key=ordering.get(1).getListSubJobs().get(0).getSubJobKey()+ordering.get(1).getListSubJobs().get(1).getSubJobKey();
							Edge connection=ordering.get(1).getDirectoryConnections().get(key);
							// patient home
							int lastJob=ordering.get(0).getListSubJobs().size()-1;
							if(!next.equals(ordering.get(0).getListSubJobs().get(lastJob))) {
								int a=ordering.get(0).getListSubJobs().indexOf(next);
								SubJobs afterJob=ordering.get(0).getListSubJobs().get(a+1);
								feasibledetour=checkFeasibilityDetour(afterJob,ordering.get(0),pickUp1Late);
							}
							else { // next is the last subJobs
								feasibledetour=true;
							}
							if(detourDuration<=connection.getDetour() && feasibledetour) { // checking detour

								for(SubJobs jS:ordering.get(0).getListSubJobs()) {
									if(jS.equals(s) || next.equals(s)) {
										if(jS.equals(s) ) {
											listSubJobs.add(pickUp1Late);
											listSubJobs.add(jS);
											//											newRoute.getSubJobsList().add(pickUp1Late);
											//											newRoute.getSubJobsList().add(jS);
										}
										else if(next.equals(s)) {
											listSubJobs.add(jS);
											listSubJobs.add(dropOff1Late);
											//											newRoute.getSubJobsList().add(jS);
											//											newRoute.getSubJobsList().add(dropOff1Late);
										}
									}
									else {
										listSubJobs.add(jS);
									}
								}
								if(vehicleCapacityPart(listSubJobs)) {
									merging=true;
									newPart.setListSubJobs(listSubJobs, inp, test);
									newRoute.getPartsRoute().add(newPart);
									newPart.settingConnections( ordering.get(0),  ordering.get(1));
									newRoute.getPartsRoute().add(newPart);
									break;}
							}
						}
						if(merging) {
							break;
						}
					}
				}
			}
		}
		return merging;
	}



	private boolean checkFeasibilityDetour(SubJobs s, Parts parts, SubJobs pickUp1Late) {
		boolean feasibledetour=false;
		double detourPart1=0;
		String key="";
		int i=parts.getListSubJobs().indexOf(s);

		if(i!=0 &&!s.equals(parts.getListSubJobs().get(parts.getListSubJobs().size()-1))) {

			key=parts.getListSubJobs().get(i-1).getSubJobKey()+parts.getListSubJobs().get(i).getSubJobKey();
			Edge connection=parts.getDirectoryConnections().get(key);
			detourPart1=computingDetour(s,parts,pickUp1Late);		
			if(detourPart1<=connection.getDetour()) {
				feasibledetour=true;
			}
		}
		else {feasibledetour=true;}
		return feasibledetour;
	}

	private double computingDetour(SubJobs s, Parts parts, SubJobs pickUp1Late) {
		double detour=0;
		if(!s.equals(parts.getListSubJobs().get(0))) {
			int a=parts.getListSubJobs().indexOf(s);
			SubJobs j=parts.getListSubJobs().get(a-1);
			SubJobs i=parts.getListSubJobs().get(a);
			detour=inp.getCarCost().getCost(i.getId()-1, pickUp1Late.getId()-1)+inp.getCarCost().getCost(pickUp1Late.getId()-1,j.getId()-1);
		}
		return detour;
	}

	private double computingdetour(SubJobs s, SubJobs next, Parts parts) {
		double detour=0;
		int a=parts.getListSubJobs().indexOf(s);
		for(int i=a;i<parts.getListSubJobs().size();i++) {
			SubJobs jobs=parts.getListSubJobs().get(i);
			if(next.equals(jobs)) {		
				break;
			}
			else {
				if(i+1<parts.getListSubJobs().size()) {
					SubJobs jobsNext=parts.getListSubJobs().get(i+1);
					detour=inp.getCarCost().getCost(jobs.getId()-1, jobsNext.getId()-1);}
			}
		}
		return detour;
	}

	private boolean mergingItermediatePartsPatientPatient(int options, Parts early, Parts late, Route newRoute, Route earlyRoute, Route lateRoute) {
		boolean merging=keepPatientPatient(options,early,late,newRoute,earlyRoute,lateRoute);
		//		if(!merging) {
		//			merging=changePatientPatient(options,early,late,newRoute,earlyRoute,lateRoute);
		//		}

		return merging;
	}

	private boolean changePatientPatient(int options, Parts early, Parts late, Route newRoute, Route earlyRoute,
			Route lateRoute) {
		boolean merging=false;

		ArrayList<SubJobs> listSubJobs= new ArrayList<SubJobs>(); // vector to store the new part
		Parts newPart=new Parts();

		/* part structure: (pick patient up at medical centre)
		(drop patient off at patient home)
		(pick next patient up at home)
		(drop next patient off at at medical centre)
		 */
		// early part

		SubJobs earlypickMC=early.getListSubJobs().get(0);
		SubJobs earlydropOffPatientHome=early.getListSubJobs().get(1);
		SubJobs earlypickUpPatientHome=early.getListSubJobs().get(2);
		SubJobs earlydropOffMedicalCentre=early.getListSubJobs().get(3);

		// late part
		SubJobs latepickMC=late.getListSubJobs().get(0);
		SubJobs latedropOffPatientHome=late.getListSubJobs().get(1);
		SubJobs latepickUpPatientHome=late.getListSubJobs().get(2);
		SubJobs latedropOffMedicalCentre=late.getListSubJobs().get(3);
		/* option 1: (early pick patient up at medical centre) ---(late pick patient up at medical centre)
		(early drop patient off at patient home) --- (late drop patient off at patient home)
		(early pick next patient up at home)---(late pick next patient up at home)
		(early drop next patient off at at medical centre) ---(late drop next patient off at at medical centre)
		 */
		boolean mergingPickUp= changingTWDetour(earlypickMC,latepickMC,earlydropOffPatientHome,latedropOffPatientHome,early,late); // checking: (early pick medical centre)---(late pick up medica centre)---(early drop off patient home)---(late drop off patient home)--
		if(mergingPickUp) { //
			mergingPickUp= checkingTWDetour(earlypickUpPatientHome,latepickUpPatientHome,earlydropOffMedicalCentre,latedropOffMedicalCentre,early,late); // checking: (early pick medical centre)---(late pick medical centre)---(early drop off medical centre)---(late drop off medical centre)
			if(mergingPickUp) {
				listSubJobs.add(earlypickMC);
				listSubJobs.add(latepickMC);
				listSubJobs.add(earlydropOffPatientHome);
				listSubJobs.add(latedropOffPatientHome);
				listSubJobs.add(earlypickUpPatientHome);
				listSubJobs.add(latepickUpPatientHome);
				listSubJobs.add(earlydropOffMedicalCentre);
				listSubJobs.add(latedropOffMedicalCentre);
				newPart.setListSubJobs(listSubJobs, inp, test);
				newRoute.getPartsRoute().add(newPart);
				newPart.settingConnections( early,  late);
				newRoute.getPartsRoute().add(newPart);
			}
		}
		else {
			/*Option 2 
		(early pick patient up at medical centre)---(early drop patient off at patient home)
		(late pick patient up at medical centre)---(late drop patient off at patient home)
		(early pick next patient up at home)---(early drop patient off at medical centre)
		(late pick next patient up at home)---(late drop patient off at medical centre) 
			 */
			double distDropOffPickEarlyLate= inp.getCarCost().getCost(earlydropOffPatientHome.getId()-1, latepickMC.getId()-1); // (early drop patient off at patient home)---(late pick patient up at medical centre)
			// evaluate option 2: en la opción dos no se consideran los detours
			boolean waintingTime=checkWaitingTime(latepickMC,distDropOffPickEarlyLate,earlydropOffPatientHome);
			if(earlydropOffPatientHome.getDepartureTime()+distDropOffPickEarlyLate<=latepickMC.getArrivalTime() && waintingTime) { // checking TW: (early drop patient off at patient home)---(late pick patient up at medical centre)	
				double distDropOffPickLateEarly=inp.getCarCost().getCost(latedropOffPatientHome.getId()-1, earlypickUpPatientHome.getId()-1);// (late drop patient off at patient home)---(early pick next patient up at home)
				waintingTime=checkWaitingTime(earlypickUpPatientHome,distDropOffPickLateEarly,latedropOffPatientHome);
				if(latedropOffPatientHome.getDepartureTime()+distDropOffPickLateEarly<=earlypickUpPatientHome.getArrivalTime() && waintingTime) { // check TW: (late drop patient off at patient home)---(early pick next patient up at home)
					double distDropOffPickEaryLate=inp.getCarCost().getCost(earlydropOffMedicalCentre.getId()-1, latepickUpPatientHome.getId()-1);
					waintingTime=checkWaitingTime(latepickUpPatientHome,distDropOffPickEaryLate,earlydropOffMedicalCentre);
					if(earlydropOffMedicalCentre.getDepartureTime()+distDropOffPickEaryLate<=latepickUpPatientHome.getArrivalTime() && waintingTime) {// check TW: (early drop patient off at medical centre)---(late pick next patient up at home)
						mergingPickUp=true;
						listSubJobs.add(earlypickMC);
						listSubJobs.add(earlydropOffPatientHome);
						listSubJobs.add(latepickMC);
						listSubJobs.add(latedropOffPatientHome);
						listSubJobs.add(earlypickUpPatientHome);
						listSubJobs.add(earlydropOffMedicalCentre);
						listSubJobs.add(latepickUpPatientHome);
						listSubJobs.add(latedropOffMedicalCentre);
						newPart.setListSubJobs(listSubJobs, inp, test);
						newRoute.getPartsRoute().add(newPart);
						newPart.settingConnections( early,  late);
						newRoute.getPartsRoute().add(newPart);

					}
				}
			}
		}
		return merging;}

	private boolean keepPatientPatient(int options, Parts early, Parts late, Route newRoute, Route earlyRoute,
			Route lateRoute) {
		boolean merging=false;

		ArrayList<SubJobs> listSubJobs= new ArrayList<SubJobs>(); // vector to store the new part
		Parts newPart=new Parts();

		/* part structure: (pick patient up at medical centre)
		(drop patient off at patient home)
		(pick next patient up at home)
		(drop next patient off at at medical centre)
		 */
		// early part

		SubJobs earlypickMC=early.getListSubJobs().get(0);
		SubJobs earlydropOffPatientHome=early.getListSubJobs().get(1);
		SubJobs earlypickUpPatientHome=early.getListSubJobs().get(2);
		SubJobs earlydropOffMedicalCentre=early.getListSubJobs().get(3);

		// late part
		SubJobs latepickMC=late.getListSubJobs().get(0);
		SubJobs latedropOffPatientHome=late.getListSubJobs().get(1);
		SubJobs latepickUpPatientHome=late.getListSubJobs().get(2);
		SubJobs latedropOffMedicalCentre=late.getListSubJobs().get(3);
		/* option 1: (early pick patient up at medical centre) ---(late pick patient up at medical centre)
		(early drop patient off at patient home) --- (late drop patient off at patient home)
		(early pick next patient up at home)---(late pick next patient up at home)
		(early drop next patient off at at medical centre) ---(late drop next patient off at at medical centre)
		 */
		boolean mergingPickUp= checkingTWDetour(earlypickMC,latepickMC,earlydropOffPatientHome,latedropOffPatientHome,early,late); // checking: (early pick medical centre)---(late pick up medica centre)---(early drop off patient home)---(late drop off patient home)--
		if(mergingPickUp) { //
			mergingPickUp= checkingTWDetour(earlypickUpPatientHome,latepickUpPatientHome,earlydropOffMedicalCentre,latedropOffMedicalCentre,early,late); // checking: (early pick medical centre)---(late pick medical centre)---(early drop off medical centre)---(late drop off medical centre)
			if(mergingPickUp) {
				listSubJobs.add(earlypickMC);
				listSubJobs.add(latepickMC);
				listSubJobs.add(earlydropOffPatientHome);
				listSubJobs.add(latedropOffPatientHome);
				listSubJobs.add(earlypickUpPatientHome);
				listSubJobs.add(latepickUpPatientHome);
				listSubJobs.add(earlydropOffMedicalCentre);
				listSubJobs.add(latedropOffMedicalCentre);
				mergingPickUp=vehicleCapacityPart(listSubJobs);
				if(mergingPickUp) {
					newPart.setListSubJobs(listSubJobs, inp, test);
					newRoute.getPartsRoute().add(newPart);
					newPart.settingConnections( early,  late);
					newRoute.getPartsRoute().add(newPart);}

				//				newRoute.getSubJobsList().add(earlypickMC);
				//				newRoute.getSubJobsList().add(latepickMC);
				//				newRoute.getSubJobsList().add(earlydropOffPatientHome);
				//				newRoute.getSubJobsList().add(latedropOffPatientHome);
				//				newRoute.getSubJobsList().add(earlypickUpPatientHome);
				//				newRoute.getSubJobsList().add(latepickUpPatientHome);
				//				newRoute.getSubJobsList().add(earlydropOffMedicalCentre);
				//				newRoute.getSubJobsList().add(latedropOffMedicalCentre);
			}
		}
		else {
			/*Option 2 
		(early pick patient up at medical centre)---(early drop patient off at patient home)
		(late pick patient up at medical centre)---(late drop patient off at patient home)
		(early pick next patient up at home)---(early drop patient off at medical centre)
		(late pick next patient up at home)---(late drop patient off at medical centre) 
			 */
			double distDropOffPickEarlyLate= inp.getCarCost().getCost(earlydropOffPatientHome.getId()-1, latepickMC.getId()-1); // (early drop patient off at patient home)---(late pick patient up at medical centre)
			// evaluate option 2: en la opción dos no se consideran los detours
			boolean waintingTime=checkWaitingTime(latepickMC,distDropOffPickEarlyLate,earlydropOffPatientHome);
			if(earlydropOffPatientHome.getDepartureTime()+distDropOffPickEarlyLate<=latepickMC.getArrivalTime() && waintingTime) { // checking TW: (early drop patient off at patient home)---(late pick patient up at medical centre)	
				double distDropOffPickLateEarly=inp.getCarCost().getCost(latedropOffPatientHome.getId()-1, earlypickUpPatientHome.getId()-1);// (late drop patient off at patient home)---(early pick next patient up at home)
				waintingTime=checkWaitingTime(earlypickUpPatientHome,distDropOffPickLateEarly,latedropOffPatientHome);
				if(latedropOffPatientHome.getDepartureTime()+distDropOffPickLateEarly<=earlypickUpPatientHome.getArrivalTime() && waintingTime) { // check TW: (late drop patient off at patient home)---(early pick next patient up at home)
					double distDropOffPickEaryLate=inp.getCarCost().getCost(earlydropOffMedicalCentre.getId()-1, latepickUpPatientHome.getId()-1);
					waintingTime=checkWaitingTime(latepickUpPatientHome,distDropOffPickEaryLate,earlydropOffMedicalCentre);
					if(earlydropOffMedicalCentre.getDepartureTime()+distDropOffPickEaryLate<=latepickUpPatientHome.getArrivalTime() && waintingTime) {// check TW: (early drop patient off at medical centre)---(late pick next patient up at home)

						listSubJobs.add(earlypickMC);
						listSubJobs.add(earlydropOffPatientHome);
						listSubJobs.add(latepickMC);
						listSubJobs.add(latedropOffPatientHome);
						listSubJobs.add(earlypickUpPatientHome);
						listSubJobs.add(earlydropOffMedicalCentre);
						listSubJobs.add(latepickUpPatientHome);
						listSubJobs.add(latedropOffMedicalCentre);
						if(vehicleCapacityPart(listSubJobs)) {
							mergingPickUp=true;
							newPart.setListSubJobs(listSubJobs, inp, test);
							newRoute.getPartsRoute().add(newPart);
							newPart.settingConnections( early,  late);
							newRoute.getPartsRoute().add(newPart);}
						//						newRoute.getSubJobsList().add(earlypickMC);
						//						newRoute.getSubJobsList().add(earlydropOffPatientHome);
						//						newRoute.getSubJobsList().add(latepickMC);
						//						newRoute.getSubJobsList().add(latedropOffPatientHome);
						//						newRoute.getSubJobsList().add(earlypickUpPatientHome);
						//						newRoute.getSubJobsList().add(earlydropOffMedicalCentre);
						//						newRoute.getSubJobsList().add(latepickUpPatientHome);
						//						newRoute.getSubJobsList().add(latedropOffMedicalCentre);
					}
				}
			}
		}
		return merging;}

	private boolean changingTWDetour(SubJobs earlypickMC, SubJobs latepickMC, SubJobs earlydropOffPatientHome,
			SubJobs latedropOffPatientHome, Parts early, Parts late) {
		/* option 1: (early pick patient up at medical centre) ---(late pick patient up at medical centre)
		(early drop patient off at patient home) --- (late drop patient off at patient home)
		(early pick next patient up at home)---(late pick next patient up at home)
		(early drop next patient off at at medical centre) ---(late drop next patient off at at medical centre)*/
		boolean merge=false;
		double distpickUpDropOff=inp.getCarCost().getCost(latepickMC.getId()-1, earlydropOffPatientHome.getId()-1);
		//computing distances
		double distpickMedicalCentre= inp.getCarCost().getCost(earlypickMC.getId()-1, latepickMC.getId()-1);// (early pick medical centre)---(late pick up medica centre)
		double distdropOffPatient= inp.getCarCost().getCost(earlydropOffPatientHome.getId()-1, latedropOffPatientHome.getId()-1);// (early pick medical centre)---(late pick up medica centre)
		// changing times latepickMC
		double [] times= calculatingTimesLateEarly(earlydropOffPatientHome,latepickMC);  // departure=times[0];  arrival=times[1]; startServiceTime=times[2];
		// changing times latepickMC
		double [] timesEarlyLate= calculatingTimesEarlyLate(earlydropOffPatientHome,latedropOffPatientHome);  // departure=times[0];  arrival=times[1]; startServiceTime=times[2];

		if(times[2]>=latepickMC.getStartTime() && times[2]<=latepickMC.getEndTime() &&
				timesEarlyLate[2]>=latedropOffPatientHome.getStartTime() && timesEarlyLate[2]<=latedropOffPatientHome.getEndTime()	){
			if(times[0]>=earlydropOffPatientHome.getArrivalTime() && times[1]<=(earlypickMC.getDepartureTime()+distpickMedicalCentre) &&
					(earlydropOffPatientHome.getDepartureTime()+distdropOffPatient)<=timesEarlyLate[1]){
				String key=latepickMC.getSubJobKey()+latedropOffPatientHome.getSubJobKey();
				Edge rempovedConnectionLate=late.getDirectoryConnections().get(key);
				Edge rempovedConnectionearly=early.getDirectoryConnections().get(key);
				if((distpickMedicalCentre+distdropOffPatient)<=rempovedConnectionearly.getDetour() && (distpickUpDropOff+distdropOffPatient)<=rempovedConnectionLate.getDetour()) { // checking detour
					merge=true;
					latepickMC.setarrivalTime(times[1]);
					latepickMC.setStartServiceTime(times[2]);
					latepickMC.setEndServiceTime(times[2]+latepickMC.getReqTime());
					latepickMC.setdepartureTime(times[0]);
				}
			}	
		}

		return merge;
	}


	private double[] calculatingTimesEarlyLate(SubJobs earlydropOffPatientHome, SubJobs latedropOffPatientHome) {
		double [] times= new double [3];
		double loadUnloadTime=0;
		double registration=0;
		double tv=inp.getCarCost().getCost(earlydropOffPatientHome.getId()-1, latedropOffPatientHome.getId()-1);
		if(latedropOffPatientHome.isClient()) {
			loadUnloadTime=test.getloadTimeHomeCareStaff();
		}
		else {
			loadUnloadTime=test.getloadTimePatient();
			registration=test.getRegistrationTime();
		}
		double departure=earlydropOffPatientHome.getDepartureTime()+tv;
		double arrival=departure-loadUnloadTime;
		double startServiceTime=arrival+registration;
		times[0]=departure;
		times[1]=arrival;
		times[2]=startServiceTime;
		return times;
	}

	private double[] calculatingTimesLateEarly(SubJobs earlydropOffPatientHome, SubJobs latepickMC) {
		double [] times= new double [3];
		double loadUnloadTime=0;
		double registration=0;
		double tv=inp.getCarCost().getCost(latepickMC.getId()-1, earlydropOffPatientHome.getId()-1);
		if(latepickMC.isClient()) {
			loadUnloadTime=test.getloadTimeHomeCareStaff();
		}
		else {
			loadUnloadTime=test.getloadTimePatient();
			registration=test.getRegistrationTime();
		}
		double departure=earlydropOffPatientHome.getArrivalTime()-tv;
		double arrival=departure-loadUnloadTime;
		double startServiceTime=arrival+registration;
		times[0]=departure;
		times[1]=arrival;
		times[2]=startServiceTime;
		return times;
	}

	private boolean checkingTWDetour(SubJobs earlypickMC, SubJobs latepickMC, SubJobs earlydropOffPatientHome,
			/* option 1: (early pick patient up at medical centre) ---(late pick patient up at medical centre)
			(early drop patient off at patient home) --- (late drop patient off at patient home)
			(early pick next patient up at home)---(late pick next patient up at home)
			(early drop next patient off at at medical centre) ---(late drop next patient off at at medical centre)*/
			SubJobs latedropOffPatientHome, Parts early, Parts late) {
		boolean merge=false;
		double distpickUpDropOff=inp.getCarCost().getCost(latepickMC.getId()-1, earlydropOffPatientHome.getId()-1);
		double distDropOffDropOff=inp.getCarCost().getCost(earlydropOffPatientHome.getId()-1, latedropOffPatientHome.getId()-1);
		//computing distances
		double distpickMedicalCentre= inp.getCarCost().getCost(earlypickMC.getId()-1, latepickMC.getId()-1);// (early pick medical centre)---(late pick up medica centre)
		double distdropOffPatient= inp.getCarCost().getCost(earlydropOffPatientHome.getId()-1, latedropOffPatientHome.getId()-1);// (early pick medical centre)---(late pick up medica centre)
		boolean waintingTime=checkWaitingTime(earlydropOffPatientHome,distpickUpDropOff,latedropOffPatientHome);
		if(latedropOffPatientHome.getDepartureTime()+distpickUpDropOff<earlydropOffPatientHome.getArrivalTime() && waintingTime) {// (early pick medical centre)---x---(early drop off patient home)
			waintingTime=checkWaitingTime(latedropOffPatientHome,distDropOffDropOff,earlydropOffPatientHome);
			if(earlydropOffPatientHome.getDepartureTime()+distDropOffDropOff<latedropOffPatientHome.getArrivalTime() && waintingTime) {//(late pick up medica centre)---x---(late drop off patient home)
				String key=latepickMC.getSubJobKey()+latedropOffPatientHome.getSubJobKey();
				Edge rempovedConnectionLate=late.getDirectoryConnections().get(key);
				Edge rempovedConnectionearly=early.getDirectoryConnections().get(key);
				if((distpickMedicalCentre+distdropOffPatient)<=rempovedConnectionearly.getDetour() && (distpickUpDropOff+distdropOffPatient)<=rempovedConnectionLate.getDetour()) { // checking detour
					merge=true;
				}
			}
		}
		return merge;
	}



	private boolean mergingItermediatePartsClients(Parts earlyPart, Parts latePart, Route newRoute) {
		boolean merging=false;
		if(earlyPart.getListSubJobs().size()==2 && latePart.getListSubJobs().size()==2 ) { // structure: (pick-up) --- (drop-off)
			merging=fourSubJobs(earlyPart,latePart,newRoute);
		}
		if((earlyPart.getListSubJobs().size()==1 && latePart.getListSubJobs().size()==2) || (latePart.getListSubJobs().size()==1 && earlyPart.getListSubJobs().size()==2) ) {
			merging=	threeSubJobs(earlyPart,latePart,newRoute);
		}
		return merging;
	}






	private boolean threeSubJobs(Parts earlyPart, Parts latePart, Route newRoute) {
		boolean merging=false;
		//
		ArrayList<SubJobs> listSubJobs= new ArrayList<SubJobs>(); // vector to store the new part
		Parts newPart=new Parts();
		//
		ArrayList<Parts> ordering=orderingParts(earlyPart,latePart); //ordena de manera descendente las partes de acuerdo al número de subjobs contenidos en cada parte 
		SubJobs pickUpRoute1=ordering.get(0).getListSubJobs().get(0);
		SubJobs pickUpRoute2=ordering.get(1).getListSubJobs().get(0);
		SubJobs dropOffRoute2=ordering.get(1).getListSubJobs().get(1);
		/*Option 1: (pickUpRoute2)---(pickUpRoute1)---(dropOffRoute2)*/
		double distpickUpRoute2pickUpRoute1=inp.getCarCost().getCost(pickUpRoute1.getId()-1, pickUpRoute2.getId()-1);
		boolean waintingTime=checkWaitingTime(pickUpRoute1,distpickUpRoute2pickUpRoute1,pickUpRoute2);
		if(pickUpRoute2.getDepartureTime()+distpickUpRoute2pickUpRoute1<=pickUpRoute1.getArrivalTime() && waintingTime) { // checking time windows 
			double distpickUpRoute1dropOffRoute2= inp.getCarCost().getCost(pickUpRoute1.getId()-1, dropOffRoute2.getId()-1);
			waintingTime=checkWaitingTime(dropOffRoute2,distpickUpRoute1dropOffRoute2,pickUpRoute1);
			if(pickUpRoute1.getDepartureTime()+distpickUpRoute1dropOffRoute2<=dropOffRoute2.getArrivalTime() && waintingTime) {	// checking time window
				String key=pickUpRoute2.getSubJobKey()+dropOffRoute2.getSubJobKey();
				Edge connection=ordering.get(1).getDirectoryConnections().get(key);
				if((distpickUpRoute2pickUpRoute1+distpickUpRoute1dropOffRoute2)<=connection.getDetour()) { // checking detour

					listSubJobs.add(pickUpRoute2);
					listSubJobs.add(pickUpRoute1);
					listSubJobs.add(dropOffRoute2);		
					if(vehicleCapacityPart(listSubJobs)) {
						merging=true;
						newPart.setListSubJobs(listSubJobs, inp, test);
						newRoute.getPartsRoute().add(newPart);
						newPart.settingConnections(earlyPart, latePart);
						newRoute.getPartsRoute().add(newPart);}
					//					newRoute.getSubJobsList().add(pickUpRoute2);
					//					newRoute.getSubJobsList().add(pickUpRoute1);
					//					newRoute.getSubJobsList().add(dropOffRoute2);					
				}
			}
		}


		return merging;
	}

	private ArrayList<Parts> orderingParts(Parts earlyPart, Parts latePart) { // pequeno- grande
		ArrayList<Parts> ordering=new ArrayList<>();
		if(earlyPart.getListSubJobs().size()<latePart.getListSubJobs().size()) {
			ordering.add(0,earlyPart);
			ordering.add(latePart);
		}
		else {
			ordering.add(0,latePart);
			ordering.add(earlyPart);
		}
		return ordering;
	}

	private boolean fourSubJobs(Parts earlyPart, Parts latePart, Route newRoute) {
		boolean merging=false;
		//
		ArrayList<SubJobs> listSubJobs= new ArrayList<SubJobs>(); // vector to store the new part
		Parts newPart=new Parts();
		// structure de una parte: (pickUp)---(dropOff)
		SubJobs pickUpEarly=earlyPart.getListSubJobs().get(0);
		SubJobs dropOffEarly=earlyPart.getListSubJobs().get(1);
		SubJobs pickUpLate=latePart.getListSubJobs().get(0);
		SubJobs dropOffLate=latePart.getListSubJobs().get(1);

		// Option 1: (pickUpPatientEarly)---(pickUpPatientLate)---(dropOffPatientEarly)---(dropOffPatientLate)
		double pickUpEarlypickUpLate=inp.getCarCost().getCost(pickUpEarly.getId()-1, pickUpLate.getId()-1);
		double dropOffEarlydropOffLate=inp.getCarCost().getCost(dropOffEarly.getId()-1,dropOffLate.getId()-1);
		// connection
		boolean waintingTime=checkWaitingTime(pickUpLate,pickUpEarlypickUpLate,pickUpEarly);
		if(pickUpEarly.getDepartureTime()+pickUpEarlypickUpLate<=pickUpLate.getArrivalTime() && waintingTime) {// Travel time first combination
			waintingTime=checkWaitingTime(dropOffLate,dropOffEarlydropOffLate,dropOffEarly);
			if(dropOffEarly.getDepartureTime()+dropOffEarlydropOffLate<=dropOffLate.getArrivalTime()) { // Travel time second combination
				// control detour early job (pickUpPatientEarly)---x---(dropOffPatientEarly)
				String key=pickUpEarly.getSubJobKey()+dropOffEarly.getSubJobKey();
				Edge connectionEarly=earlyPart.getDirectoryConnections().get(key);
				if(connectionEarly==null) {
					System.out.println("stop");
				}
				double detourEarly=pickUpEarlypickUpLate+inp.getCarCost().getCost(pickUpLate.getId()-1, dropOffEarly.getId()-1);

				// control detour early job (pickUpPatientLate)---x---(dropOffPatientLate)
				key=pickUpLate.getSubJobKey()+dropOffLate.getSubJobKey();
				Edge connectionLate=latePart.getDirectoryConnections().get(key);
				if(connectionLate==null) {
					System.out.println("stop");
				}
				double detourLate=dropOffEarlydropOffLate+inp.getCarCost().getCost(pickUpLate.getId()-1, dropOffEarly.getId()-1);
				if(detourEarly<=connectionEarly.getDetour() && detourLate<=connectionLate.getDetour()) {// tour early and late

					listSubJobs.add(pickUpEarly);
					listSubJobs.add(pickUpLate);
					listSubJobs.add(dropOffEarly);
					listSubJobs.add(dropOffLate);
					if(vehicleCapacityPart(listSubJobs)) {
						merging=true;

						newPart.setListSubJobs(listSubJobs, inp, test);
						newRoute.getPartsRoute().add(newPart);
						newPart.settingConnections(earlyPart, latePart);
						newRoute.getPartsRoute().add(newPart);}
					//					newRoute.getSubJobsList().add(pickUpEarly);
					//					newRoute.getSubJobsList().add(pickUpLate);
					//					newRoute.getSubJobsList().add(dropOffEarly);
					//					newRoute.getSubJobsList().add(dropOffLate);
				}
			}	
		}



		// Option 2: (pickUpPatientEarly)---(pickUpPatientLate)---(dropOffPatientEarly)---(dropOffPatientLate)
		if(!merging) {
			double dropOffLatedropOffEarly=inp.getCarCost().getCost(dropOffLate.getId()-1, dropOffEarly.getId()-1);
			waintingTime=checkWaitingTime(pickUpLate,pickUpEarlypickUpLate,pickUpEarly);
			if(pickUpEarly.getDepartureTime()+pickUpEarlypickUpLate<=pickUpLate.getArrivalTime() && waintingTime) { // time window (pickUpPatientEarly)---(pickUpPatientLate)
				waintingTime=checkWaitingTime(dropOffEarly,dropOffLatedropOffEarly,dropOffLate);
				if(dropOffLate.getDepartureTime()+dropOffLatedropOffEarly<=dropOffEarly.getArrivalTime() && waintingTime) { // time window (dropOffPatientEarly)---(dropOffPatientLate)
					// distance detour
					String key=pickUpLate.getSubJobKey()+dropOffLate.getSubJobKey();
					Edge edgeConnectionLate=latePart.getDirectoryConnections().get(key);
					double distDetour=pickUpEarlypickUpLate+edgeConnectionLate.getTime()+dropOffLatedropOffEarly;
					key=pickUpLate.getSubJobKey()+dropOffLate.getSubJobKey();
					Edge directConnection= earlyPart.getDirectoryConnections().get(key);
					if(distDetour<=directConnection.getDetour()) { // detour
						merging=true;
						listSubJobs.add(pickUpEarly);
						listSubJobs.add(pickUpLate);
						listSubJobs.add(dropOffLate);
						listSubJobs.add(dropOffEarly);
						if(vehicleCapacityPart(listSubJobs)) {
							merging=true;
							newPart.setListSubJobs(listSubJobs, inp, test);
							newRoute.getPartsRoute().add(newPart);
							newPart.settingConnections(earlyPart, latePart);
							newRoute.getPartsRoute().add(newPart);}
						//						newRoute.getSubJobsList().add(pickUpEarly);
						//						newRoute.getSubJobsList().add(pickUpLate);
						//						newRoute.getSubJobsList().add(dropOffLate);
						//						newRoute.getSubJobsList().add(dropOffEarly);
					}
				}
			}
		}
		return merging;
	}

	private boolean seatingHeading(Route earlyRoute, Route lateRoute, Route newRoute) {
		boolean headingMerge= false;// la early es la ruta de referencia  - eso quiere decir que al final el array mergingPart debe ser igual a la ruta más cercana
		// selecting depot
		//SubJobs depot=new SubJobs(earlyRoute.getPartsRoute().get(0).getListSubJobs().get(0));
		//newRoute.getSubJobsList().add(depot);
		newRoute.getPartsRoute().add(earlyRoute.getPartsRoute().get(0));
		headingMerge=mergingHeading(earlyRoute,lateRoute,newRoute);
		if(headingMerge) { // setting the new amount of people
			/*Actualizando la nueva ruta*/
			newRoute.getPartsRoute().get(0).getListSubJobs().get(0).setTotalPeople(earlyRoute.getPartsRoute().get(0).getListSubJobs().get(0).getTotalPeople()+lateRoute.getPartsRoute().get(0).getListSubJobs().get(0).getTotalPeople());
			//newRoute.getSubJobsList().get(0).setTotalPeople(earlyRoute.getPartsRoute().get(0).getListSubJobs().get(0).getTotalPeople()+lateRoute.getPartsRoute().get(0).getListSubJobs().get(0).getTotalPeople());
			newRoute.setAmountParamedic(earlyRoute.getAmountParamedic()+lateRoute.getAmountParamedic());
			newRoute.setHomeCareStaff(earlyRoute.getHomeCareStaff()+lateRoute.getHomeCareStaff());
			/*Actualizando la ruta que queda*/
			lateRoute.getPartsRoute().get(0).getListSubJobs().get(0).setTotalPeople(0);
		}
		else {
			//newRoute.getSubJobsList().get(0).setTotalPeople(earlyRoute.getPartsRoute().get(0).getListSubJobs().get(0).getTotalPeople());
			newRoute.getPartsRoute().get(0).getListSubJobs().get(0).setTotalPeople(earlyRoute.getPartsRoute().get(0).getListSubJobs().get(0).getTotalPeople());
			newRoute.setAmountParamedic(earlyRoute.getAmountParamedic());
			newRoute.setHomeCareStaff(earlyRoute.getHomeCareStaff());
			/*Actualizando la ruta que queda*/

			Parts p=earlyRoute.getPartsRoute().get(1);
			newRoute.getPartsRoute().add(p);

		}

		return headingMerge;
	}

	private boolean mergingHeading(Route earlyRoute, Route lateRoute, Route newRoute) {
		// este método lo que hace es modificar la ruta temprana la earlyRoute
		boolean feasibleOption1=false;

		//newRoute.getSubJobsList().add(earlyRoute.getSubJobsList().get(0).getSubJobs().get(0));
		// typeParts: 1 patient-patient 2 homeCare-patient or patient-homeCare 3 homeCare-homeCare 
		int options=typeOfParts(earlyRoute.getPartsRoute().get(1),lateRoute.getPartsRoute().get(1)); 

		switch(options) {
		case 1 :// 1 patient-patient 
			feasibleOption1=mergingPatientPatient(options,earlyRoute.getPartsRoute().get(1),lateRoute.getPartsRoute().get(1), newRoute);// option1
			break;

		case 2 : // 2 homeCare-patient or patient-homeCare
			feasibleOption1=mergingPatientClient(options,earlyRoute.getPartsRoute().get(1),lateRoute.getPartsRoute().get(1), earlyRoute,newRoute);// option1

			break; // optional

		default : // 3 homeCare-homeCare
			feasibleOption1=mergingClients(options,earlyRoute.getPartsRoute().get(1),lateRoute.getPartsRoute().get(1), newRoute);// option1
		}
		return feasibleOption1;
	}

	private boolean mergingClients(int options, Parts early, Parts late, Route newRoute) {
		boolean feasible=keepTimeClients(options,early,late,newRoute);
		//		if(!feasible) {
		//			feasible=changeTimeClients(options,early,late,newRoute);
		//		}
		return feasible;
	}


	private boolean changeTimeClients(int options, Parts early, Parts late, Route newRoute) {
		boolean feasible=false;
		ArrayList<SubJobs> listSubJobs= new ArrayList<SubJobs>(); // vector to store the new part
		Parts newPart=new Parts();

		SubJobs depot=newRoute.getPartsRoute().get(0).getListSubJobs().get(0);
		SubJobs earlyDropOff=early.getListSubJobs().get(0);
		SubJobs lateDropOff=late.getListSubJobs().get(0);
		Edge removedConncetion= new Edge(depot,lateDropOff,inp,test);
		Edge conncetionDepot= new Edge(depot,earlyDropOff,inp,test);
		/*current distance*/
		double distEarlyDropOffLateDropOff=inp.getCarCost().getCost(earlyDropOff.getId()-1, lateDropOff.getId()-1);
		// changing times

		double loadUnloadTime=0;
		double registration=0;
		if(lateDropOff.isClient()) {
			loadUnloadTime=test.getloadTimeHomeCareStaff();
		}
		else {
			loadUnloadTime=test.getloadTimePatient();
			registration=test.getRegistrationTime();
		}
		double departure=earlyDropOff.getDepartureTime()-distEarlyDropOffLateDropOff;
		double arrival=departure-loadUnloadTime;
		double startServiceTime=arrival+registration;
		if(startServiceTime>=lateDropOff.getStartTime()  && startServiceTime<=lateDropOff.getEndTime()) {
			feasible=true;
			listSubJobs.add(earlyDropOff);
			listSubJobs.add(lateDropOff);
			newPart.setListSubJobs(listSubJobs, inp, test);
			newPart.settingConnections(early,late);
			newRoute.getPartsRoute().add(newPart);
		}
		return feasible;
	}


	private boolean keepTimeClients(int options, Parts early, Parts late, Route newRoute) {
		boolean feasible=false;

		ArrayList<SubJobs> listSubJobs= new ArrayList<SubJobs>(); // vector to store the new part
		Parts newPart=new Parts();

		SubJobs depot=newRoute.getPartsRoute().get(0).getListSubJobs().get(0);
		SubJobs earlyDropOff=early.getListSubJobs().get(0);
		SubJobs lateDropOff=late.getListSubJobs().get(0);
		Edge removedConncetion= new Edge(depot,lateDropOff,inp,test);
		Edge conncetionDepot= new Edge(depot,earlyDropOff,inp,test);
		/*current distance*/
		double distEarlyDropOffLateDropOff=inp.getCarCost().getCost(earlyDropOff.getId()-1, lateDropOff.getId()-1);
		boolean waintingTime=checkWaitingTime(lateDropOff,conncetionDepot.getTime()+distEarlyDropOffLateDropOff,earlyDropOff);
		if((earlyDropOff.getDepartureTime()+conncetionDepot.getTime()+distEarlyDropOffLateDropOff)<=lateDropOff.getArrivalTime() && waintingTime) { // time window
			if((conncetionDepot.getTime()+distEarlyDropOffLateDropOff)<=removedConncetion.getDetour()) {// checking detour

				listSubJobs.add(earlyDropOff);
				listSubJobs.add(lateDropOff);
				if(vehicleCapacityPart(listSubJobs)) {
					feasible=true;
					newPart.setListSubJobs(listSubJobs, inp, test);
					newPart.settingConnections(early,late);
					newRoute.getPartsRoute().add(newPart);
				}

				//				newRoute.getSubJobsList().add(earlyDropOff);
				//				newRoute.getSubJobsList().add(lateDropOff);
			}
		}

		return feasible;
	}

	private boolean mergingPatientClient(int options, Parts early, Parts late, Route earlyRoute, Route newRoute) {
		boolean merging=keepTimePatientClient(options,early,late,newRoute);
		//		if(!merging) {
		//			merging=changeTimePatientClient(options,early,late,earlyRoute,newRoute);
		//		}
		return merging;
	}


	private boolean changeTimePatientClient(int options, Parts early, Parts late, Route earlyRoute, Route newRoute) {
		boolean merging=false;
		ArrayList<SubJobs> listSubJobs= new ArrayList<SubJobs>(); // vector to store the new part
		Parts newPart=new Parts();

		SubJobs depot=newRoute.getPartsRoute().get(0).getListSubJobs().get(0);
		// calling subjobs
		SubJobs pickUpEarly=null;
		SubJobs dropoffEarly=null;
		SubJobs pickUpLate=null;
		SubJobs dropoffLate=null;

		if(early.getListSubJobs().size()==1 && late.getListSubJobs().size()==2) { // opción en que el prime trabajo sea uno asociado a un home care staff 
			if( early.getListSubJobs().get(0).getId()!=1) { 
				// patient <- home care staff  late<- patient
				dropoffEarly=early.getListSubJobs().get(0); // home care
				pickUpLate=late.getListSubJobs().get(0); // patient
				dropoffLate=late.getListSubJobs().get(1);  // patient

				boolean feasibleTW=changeTimeWindow(pickUpEarly,dropoffLate,dropoffEarly);
				boolean feasibleDetour=checkDetour(depot,pickUpEarly,dropoffLate,dropoffEarly,early);
				boolean feasibleNextPart=true;
				if(earlyRoute.getPartsRoute().get(2).getListSubJobs().get(0).getId()!=1) {
					double distancedropoffLateNextPart=inp.getCarCost().getCost(dropoffLate.getId()-1, earlyRoute.getPartsRoute().get(2).getListSubJobs().get(0).getId()-1);
					if(dropoffLate.getDepartureTime()+distancedropoffLateNextPart<=earlyRoute.getPartsRoute().get(2).getListSubJobs().get(0).getArrivalTime()) {
						feasibleNextPart=true;
					}
					else {
						feasibleNextPart=false;
					}
				}

				if(feasibleTW && feasibleDetour && feasibleNextPart){
					merging=true;
					listSubJobs.add(pickUpLate);
					listSubJobs.add(dropoffEarly);
					listSubJobs.add(dropoffLate);
					newPart.setListSubJobs(listSubJobs, inp, test);
					newPart.settingConnections(early,late);
					newRoute.getPartsRoute().add(newPart);
				}
			}
		}
		else {// opción en que el primer trabajo se uno asociado a  un paciente
			// early <- patient  late<- home care staff

			if(late.getListSubJobs().get(0).getId()!=1) {
				pickUpEarly=early.getListSubJobs().get(0);  // patient
				dropoffEarly=early.getListSubJobs().get(1); // patient
				dropoffLate=late.getListSubJobs().get(0); // home care staff
				Edge connectionToRemove= new Edge(depot,dropoffLate,inp,test);
				Edge connectionDepot= new Edge(depot,pickUpEarly,inp,test);
				double distDropOffEarlyPickUpLate=inp.getCarCost().getCost(dropoffEarly.getId()-1, dropoffLate.getId());


				double loadUnloadTime=0;
				double registration=0;
				if(dropoffLate.isClient()) {
					loadUnloadTime=test.getloadTimeHomeCareStaff();
				}
				else {
					loadUnloadTime=test.getloadTimePatient();
					registration=test.getRegistrationTime();
				}
				double departure=dropoffEarly.getDepartureTime()+distDropOffEarlyPickUpLate;
				double arrival=departure-loadUnloadTime;
				double startServiceTime=arrival+registration;
				boolean feasibleNextPart=true;
				if(earlyRoute.getPartsRoute().get(2).getListSubJobs().get(0).getId()!=1) {
					double distancedropoffLateNextPart=inp.getCarCost().getCost(dropoffLate.getId()-1, earlyRoute.getPartsRoute().get(2).getListSubJobs().get(0).getId()-1);
					if(dropoffLate.getDepartureTime()+distancedropoffLateNextPart<=earlyRoute.getPartsRoute().get(2).getListSubJobs().get(0).getArrivalTime()) {
						feasibleNextPart=true;
					}
					else {
						feasibleNextPart=false;
					}
				}
				if(startServiceTime>=dropoffLate.getStartTime() && startServiceTime<=dropoffLate.getEndTime() && feasibleNextPart) {
					if(connectionDepot.getTime()+distDropOffEarlyPickUpLate<=connectionToRemove.getDetour()) { // detour 
						merging=true;
						listSubJobs.add(pickUpEarly);
						listSubJobs.add(dropoffEarly);
						listSubJobs.add(dropoffLate);
						newPart.setListSubJobs(listSubJobs, inp, test);
						newPart.settingConnections(early,late);
						newRoute.getPartsRoute().add(newPart);
					}
				}		
			}
		}
		return merging;
	}


	private boolean keepTimePatientClient(int options, Parts early, Parts late, Route newRoute) {
		boolean merging=false;
		ArrayList<SubJobs> listSubJobs= new ArrayList<SubJobs>(); // vector to store the new part
		Parts newPart=new Parts();

		SubJobs depot=newRoute.getPartsRoute().get(0).getListSubJobs().get(0);
		// calling subjobs
		SubJobs pickUpEarly=null;
		SubJobs dropoffEarly=null;
		SubJobs pickUpLate=null;
		SubJobs dropoffLate=null;

		if(early.getListSubJobs().size()==1 && late.getListSubJobs().size()==2) { // opción en que el prime trabajo sea uno asociado a un home care staff 
			if( early.getListSubJobs().get(0).getId()!=1) { 
				// early <- home care staff  late<- patient
				dropoffEarly=early.getListSubJobs().get(0); // home care
				pickUpLate=late.getListSubJobs().get(0); // patient
				dropoffLate=late.getListSubJobs().get(1); 

				boolean feasibleTW=ckeckTimeWindow(pickUpEarly,dropoffLate,dropoffEarly);
				boolean feasibleDetour=checkDetour(depot,pickUpEarly,dropoffLate,dropoffEarly,early);
				if(feasibleTW && feasibleDetour ){

					listSubJobs.add(pickUpEarly);
					listSubJobs.add(dropoffLate);
					listSubJobs.add(dropoffEarly);
					if(vehicleCapacityPart(listSubJobs)) {
						merging=true;
						newPart.setListSubJobs(listSubJobs, inp, test);
						newPart.settingConnections(early,late);
						newRoute.getPartsRoute().add(newPart);
					}

					//				newRoute.getSubJobsList().add(pickUpEarly);
					//				newRoute.getSubJobsList().add(dropoffLate);
					//				newRoute.getSubJobsList().add(dropoffEarly);
				}
			}
		}
		else {// opción en que el primer trabajo se uno asociado a  un paciente
			// early <- patient  late<- home care staff

			if(late.getListSubJobs().get(0).getId()!=1) {
				pickUpEarly=early.getListSubJobs().get(0);  // patient
				dropoffEarly=early.getListSubJobs().get(1);
				dropoffLate=late.getListSubJobs().get(0); // home care staff
				Edge connectionToRemove= new Edge(depot,dropoffLate,inp,test);
				Edge connectionDepot= new Edge(depot,pickUpEarly,inp,test);
				double distDropOffEarlyPickUpLate=inp.getCarCost().getCost(dropoffEarly.getId()-1, dropoffLate.getId());
				double distDropOffEarlyDropOffLate=inp.getCarCost().getCost(dropoffLate.getId()-1, dropoffEarly.getId());
				boolean waintingTime=checkWaitingTime(dropoffLate,distDropOffEarlyPickUpLate,pickUpEarly);
				if(pickUpEarly.getDepartureTime()+distDropOffEarlyPickUpLate<dropoffLate.getArrivalTime() && waintingTime) { // time window (depot)---x---(pick up patient)
					waintingTime=checkWaitingTime(dropoffEarly,distDropOffEarlyDropOffLate,dropoffLate);
					if(dropoffLate.getDepartureTime()+distDropOffEarlyDropOffLate<=dropoffEarly.getArrivalTime() && waintingTime) {
						if(connectionDepot.getTime()+distDropOffEarlyPickUpLate<=connectionToRemove.getDetour()) { // detour 


							listSubJobs.add(dropoffEarly);
							listSubJobs.add(pickUpLate);
							listSubJobs.add(dropoffLate);
							if(vehicleCapacityPart(listSubJobs)) {
								merging=true;
								newPart.setListSubJobs(listSubJobs, inp, test);
								newPart.settingConnections(early,late);
								newRoute.getPartsRoute().add(newPart);}
						}
					}
				}
			}
		}
		return merging;
	}

	private boolean checkDetour(SubJobs depot, SubJobs pickUpEarly, SubJobs dropoffLate, SubJobs dropoffEarly, Parts early) {
		boolean feasible=false;
		double tvPickUpEarlyDropOffLate=inp.getCarCost().getCost(pickUpEarly.getId()-1, dropoffLate.getId()-1);
		double tvDropOffLateDropOffEarly=inp.getCarCost().getCost(dropoffLate.getId()-1, dropoffEarly.getId()-1);
		/* current connection to depot*/
		Edge depotConnection=new Edge(depot, pickUpEarly,inp,test);


		/*removed connection to depot*/
		Edge depotConnectionRemoved=new Edge(depot, dropoffLate,inp,test);
		if(depotConnection.getTime()+tvPickUpEarlyDropOffLate<=depotConnectionRemoved.getDetour()) { //detour: (Depot)---x---(dropoff home care staff)

			String key=pickUpEarly.getSubJobKey()+dropoffEarly.getSubJobKey();
			Edge pickUpDropOff=early.getDirectoryConnections().get(key);
			if(tvPickUpEarlyDropOffLate+tvDropOffLateDropOffEarly<=pickUpDropOff.getDetour()) { //detour: (Pick patient)---x---(dropoff patient)
				feasible=true;
			}
		}
		return feasible;
	}

	private boolean changeTimeWindow(SubJobs pickUpEarly, SubJobs dropoffLate, SubJobs dropoffEarly) { //(Pick patient)---(dropoff home care staff)---(dropoff patient)
		boolean feasible=false;
		double tvDropOffLateDropOffEarly=inp.getCarCost().getCost(dropoffLate.getId()-1, dropoffEarly.getId()-1);
		double loadUnloadTime=0;
		double registration=0;
		if(dropoffLate.isClient()) {
			loadUnloadTime=test.getloadTimeHomeCareStaff();
		}
		else {
			loadUnloadTime=test.getloadTimePatient();
			registration=test.getRegistrationTime();
		}
		double departure=dropoffEarly.getDepartureTime()-tvDropOffLateDropOffEarly;
		double arrival=departure-loadUnloadTime;
		double startServiceTime=arrival+registration;

		if(pickUpEarly.getDepartureTime()<=arrival  && startServiceTime>=dropoffLate.getStartTime() && startServiceTime<=dropoffLate.getEndTime()) {
			dropoffLate.setarrivalTime(arrival);
			dropoffLate.setStartServiceTime(startServiceTime);
			dropoffLate.setEndServiceTime(startServiceTime+dropoffLate.getReqTime());
			dropoffLate.setdepartureTime(departure);
			feasible=true;
		}
		return feasible;
	}

	private boolean ckeckTimeWindow(SubJobs pickUpEarly, SubJobs dropoffLate, SubJobs dropoffEarly) { //(Pick patient)---(dropoff home care staff)---(dropoff patient)
		boolean feasible=false;
		double tvPickUpEarlyDropOffLate=inp.getCarCost().getCost(pickUpEarly.getId()-1, dropoffLate.getId()-1);
		double tvDropOffLateDropOffEarly=inp.getCarCost().getCost(dropoffLate.getId()-1, dropoffEarly.getId()-1);
		if(pickUpEarly.getDepartureTime()+tvPickUpEarlyDropOffLate<dropoffLate.getArrivalTime()) { //(Pick patient)---(dropoff home care staff)
			if(dropoffLate.getDepartureTime()+tvDropOffLateDropOffEarly<dropoffEarly.getArrivalTime()) {//(dropoff home care staff)---(dropoff patient)
				feasible=true;
			}
		}
		return feasible;
	}

	private boolean mergingPatientPatient(int typeParts, Parts earlyPart, Parts latePart, Route newRoute) {
		boolean merging= keepTime(typeParts,earlyPart,latePart,newRoute);
		//		if(!merging) { // changing times
		//			merging= changeTime(typeParts,earlyPart,latePart,newRoute);
		//		}
		return merging;
	}

	private boolean changeTime(int typeParts, Parts earlyPart, Parts latePart, Route newRoute) {
		boolean merging= false;
		ArrayList<SubJobs> listSubJobs= new ArrayList<SubJobs>(); // vector to store the new part
		Parts newPart=new Parts();
		// typeParts: 1 patient-patient 2 homeCare-patient or patient-homeCare 3 homeCare-homeCare

		//SubJobs depot=newRoute.getSubJobsList().get(0);
		SubJobs depot=newRoute.getPartsRoute().get(0).getListSubJobs().get(0);
		// option 1: depot - patient Early pick up - Patient Late pick up - patient Early drop-off - Patient Late drop-off
		SubJobs pickUpPatientEarly=earlyPart.getListSubJobs().get(0);
		SubJobs dropOffPatientEarly=earlyPart.getListSubJobs().get(1);
		SubJobs pickUpPatientLate=latePart.getListSubJobs().get(0); // se cambia la hora
		SubJobs dropOffPatientLate=latePart.getListSubJobs().get(1); // se cambia la hora
		Edge connectionToDepot=new Edge(depot,pickUpPatientEarly,inp,test);
		Edge removeConnection=new Edge(depot,pickUpPatientLate,inp,test);

		/*Distance Option*/
		double travelTime12=inp.getCarCost().getCost(pickUpPatientEarly.getId()-1, pickUpPatientLate.getId()-1);
		double travelTime23=inp.getCarCost().getCost(pickUpPatientLate.getId()-1, dropOffPatientEarly.getId()-1);
		double travelTime34=inp.getCarCost().getCost(dropOffPatientEarly.getId()-1, dropOffPatientLate.getId()-1);

		/* detour early -early */	
		String key= pickUpPatientEarly.getSubJobKey()+dropOffPatientEarly.getSubJobKey();// to check detour ->depot- tarde
		Edge PatientHouseMedicalCentreEarly= earlyPart.getDirectoryConnections().get(key);
		double disHomeMedicalCentreEarly=travelTime12+travelTime34;

		double depotJobLate=connectionToDepot.getTime()+travelTime12;

		// Early <- detour temprano - temprano
		double distHomeMedicalCentreEarlyOption2=travelTime12+travelTime23;	

		/* detour tarde - tarde */
		key= pickUpPatientLate.getSubJobKey()+dropOffPatientLate.getSubJobKey();// to check detour ->depot- tarde
		Edge PatientHouseMedicalCentreLate= latePart.getDirectoryConnections().get(key);
		double distHomeMedicalCentrelteOption2Late=travelTime23+travelTime34;	

		/*Option 1<-pick up temprano - pick up tarde - drop off temprano - drop off tarde*/

		double loadUnloadTime=0;
		double registration=0;
		if(pickUpPatientLate.isClient()) {
			loadUnloadTime=test.getloadTimeHomeCareStaff();
		}
		else {
			loadUnloadTime=test.getloadTimePatient();
			registration=test.getRegistrationTime();
		}
		// for pick up tarde
		double departurePickUp=dropOffPatientEarly.getArrivalTime()-travelTime23;
		double arrivalPickUp=departurePickUp-loadUnloadTime;
		double startServiceTimePickUp=arrivalPickUp+registration;

		// for drop off tarde
		loadUnloadTime=0;
		registration=0;
		if(dropOffPatientLate.isClient()) {
			loadUnloadTime=test.getloadTimeHomeCareStaff();
		}
		else {
			loadUnloadTime=test.getloadTimePatient();
			registration=test.getRegistrationTime();
		}
		double departureDropOff=dropOffPatientEarly.getArrivalTime()+travelTime34;
		double arrivalDropOff=departureDropOff-loadUnloadTime;
		double startServiceTimeDropOff=arrivalDropOff+registration;

		if(startServiceTimePickUp>=pickUpPatientLate.getStartTime() && startServiceTimePickUp<=pickUpPatientLate.getEndTime() &&
				startServiceTimeDropOff>=dropOffPatientLate.getStartTime() && startServiceTimeDropOff<=dropOffPatientLate.getEndTime()) {
			boolean detourLateSubjob=checkingDetourPatient(depotJobLate,removeConnection,distHomeMedicalCentrelteOption2Late,PatientHouseMedicalCentreLate);// connección con el depot y connección entre trabajos
			boolean detourEarlySubjob=checkingDetourEarlyPatient(disHomeMedicalCentreEarly,PatientHouseMedicalCentreEarly);// connección con el depot y connección entre trabajos
			if(detourLateSubjob && detourEarlySubjob) {
				pickUpPatientLate.setarrivalTime(arrivalPickUp);
				pickUpPatientLate.setStartServiceTime(startServiceTimePickUp);
				pickUpPatientLate.setEndServiceTime(startServiceTimePickUp+pickUpPatientLate.getReqTime());
				pickUpPatientLate.setdepartureTime(departurePickUp);

				dropOffPatientLate.setarrivalTime(arrivalDropOff);
				dropOffPatientLate.setStartServiceTime(startServiceTimeDropOff);
				dropOffPatientLate.setEndServiceTime(startServiceTimeDropOff+dropOffPatientLate.getReqTime());
				dropOffPatientLate.setdepartureTime(departureDropOff);
				merging= true;
				listSubJobs.add(pickUpPatientEarly);
				listSubJobs.add(pickUpPatientLate);
				listSubJobs.add(dropOffPatientEarly);
				listSubJobs.add(dropOffPatientLate);
				newPart.setListSubJobs(listSubJobs, inp, test);
				newPart.settingConnections(earlyPart,latePart);
				newRoute.getPartsRoute().add(newPart);
			}
		}


		/*Option 2 <-pick up temprano - pick up tarde - drop off tarde - drop off temprano*/
		if(!merging) {
			/* Distance option 2*/ 
			double traveltimePickupDropOff=inp.getCarCost().getCost(pickUpPatientLate.getId()-1, dropOffPatientLate.getId()-1);
			double traveltimeDropOffDropOff=inp.getCarCost().getCost(dropOffPatientLate.getId()-1, dropOffPatientEarly.getId()-1);
			double distPickUpDropOffLate=travelTime12+traveltimePickupDropOff+traveltimeDropOffDropOff;
			// additional time

			loadUnloadTime=0;
			registration=0;
			if(pickUpPatientLate.isClient()) {
				loadUnloadTime=test.getloadTimeHomeCareStaff();
			}
			else {
				loadUnloadTime=test.getloadTimePatient();
				registration=test.getRegistrationTime();
			}
			// changing pick up late

			departurePickUp=pickUpPatientEarly.getDepartureTime()-travelTime12;
			arrivalPickUp=departurePickUp-loadUnloadTime;
			startServiceTimePickUp=arrivalPickUp+registration;

			// changing drop off late
			loadUnloadTime=0;
			registration=0;
			if(dropOffPatientLate.isClient()) {
				loadUnloadTime=test.getloadTimeHomeCareStaff();
			}
			else {
				loadUnloadTime=test.getloadTimePatient();
				registration=test.getRegistrationTime();
			}
			departureDropOff=dropOffPatientEarly.getArrivalTime()-traveltimeDropOffDropOff;
			arrivalDropOff=departureDropOff-loadUnloadTime;
			startServiceTimeDropOff=arrivalDropOff+registration;

			// time windows
			if(startServiceTimePickUp>=pickUpPatientLate.getStartTime() && startServiceTimePickUp<=pickUpPatientLate.getEndTime() &&
					startServiceTimeDropOff>=dropOffPatientLate.getStartTime() && startServiceTimeDropOff<=dropOffPatientLate.getEndTime()) {
				boolean detourLateSubjob=checkingDetourPatient(depotJobLate,removeConnection,distHomeMedicalCentrelteOption2Late,PatientHouseMedicalCentreLate);
				boolean detourEarlySubJob=checkingDetourEarly(distPickUpDropOffLate,PatientHouseMedicalCentreEarly);
				if(detourLateSubjob && detourEarlySubJob) {
					merging= true;
					listSubJobs.add(pickUpPatientEarly);
					listSubJobs.add(pickUpPatientLate);
					listSubJobs.add(dropOffPatientLate);
					listSubJobs.add(dropOffPatientEarly);
					newPart.setListSubJobs(listSubJobs, inp, test);
					newPart.settingConnections(earlyPart,latePart);
					newRoute.getPartsRoute().add(newPart);
				}

			}
		}
		return merging;
	}




	private boolean keepTime(int typeParts, Parts earlyPart, Parts latePart, Route newRoute) {
		boolean merging= false;
		ArrayList<SubJobs> listSubJobs= new ArrayList<SubJobs>(); // vector to store the new part
		Parts newPart=new Parts();
		// typeParts: 1 patient-patient 2 homeCare-patient or patient-homeCare 3 homeCare-homeCare

		//SubJobs depot=newRoute.getSubJobsList().get(0);
		SubJobs depot=newRoute.getPartsRoute().get(0).getListSubJobs().get(0);
		// option 1: depot - patient Early pick up - Patient Late pick up - patient Early drop-off - Patient Late drop-off
		SubJobs pickUpPatientEarly=earlyPart.getListSubJobs().get(0);
		SubJobs dropOffPatientEarly=earlyPart.getListSubJobs().get(1);
		SubJobs pickUpPatientLate=latePart.getListSubJobs().get(0);
		SubJobs dropOffPatientLate=latePart.getListSubJobs().get(1);
		Edge connectionToDepot=new Edge(depot,pickUpPatientEarly,inp,test);
		Edge removeConnection=new Edge(depot,pickUpPatientLate,inp,test);

		/*Distance Option*/
		double travelTime12=inp.getCarCost().getCost(pickUpPatientEarly.getId()-1, pickUpPatientLate.getId()-1);
		double travelTime23=inp.getCarCost().getCost(pickUpPatientLate.getId()-1, dropOffPatientEarly.getId()-1);
		double travelTime34=inp.getCarCost().getCost(dropOffPatientEarly.getId()-1, dropOffPatientLate.getId()-1);

		/* detour early -early */	
		String key= pickUpPatientEarly.getSubJobKey()+dropOffPatientEarly.getSubJobKey();// to check detour ->depot- tarde
		Edge PatientHouseMedicalCentreEarly= earlyPart.getDirectoryConnections().get(key);
		double disHomeMedicalCentreEarly=travelTime12+travelTime34;

		double depotJobLate=connectionToDepot.getTime()+travelTime12;

		// Early <- detour temprano - temprano
		double distHomeMedicalCentreEarlyOption2=travelTime12+travelTime23;	

		/* detour tarde - tarde */
		key= pickUpPatientLate.getSubJobKey()+dropOffPatientLate.getSubJobKey();// to check detour ->depot- tarde
		Edge PatientHouseMedicalCentreLate= latePart.getDirectoryConnections().get(key);
		double distHomeMedicalCentrelteOption2Late=travelTime23+travelTime34;	

		/*Option 1<-pick up temprano - pick up tarde - drop off temprano - drop off tarde*/
		boolean waintingTime=checkWaitingTime(pickUpPatientLate,travelTime12,pickUpPatientEarly);
		if((pickUpPatientEarly.getDepartureTime()+travelTime12)<=pickUpPatientLate.getArrivalTime() && waintingTime) {// checking time windows (Early pick up)--- (patient Late pick up)
			waintingTime=checkWaitingTime(dropOffPatientEarly,travelTime23,pickUpPatientLate);
			if((pickUpPatientLate.getDepartureTime()+travelTime23)<=dropOffPatientEarly.getArrivalTime() && waintingTime) {// checking time windows (patient Late pick up)---(patient Early drop-off)
				waintingTime=checkWaitingTime(dropOffPatientLate,travelTime34,dropOffPatientEarly);
				if((dropOffPatientEarly.getDepartureTime()+travelTime34)<=dropOffPatientLate.getArrivalTime() && waintingTime) {// checking time windows (patient Late pick up)---(patient Early drop-off)
					// detour current distance (shared trips) vs distance (individual trips)
					boolean detourLateSubjob=checkingDetourPatient(depotJobLate,removeConnection,distHomeMedicalCentrelteOption2Late,PatientHouseMedicalCentreLate);// connección con el depot y connección entre trabajos
					boolean detourEarlySubjob=checkingDetourEarlyPatient(disHomeMedicalCentreEarly,PatientHouseMedicalCentreEarly);// connección con el depot y connección entre trabajos
					if(detourLateSubjob && detourEarlySubjob) {

						listSubJobs.add(pickUpPatientEarly);
						listSubJobs.add(pickUpPatientLate);
						listSubJobs.add(dropOffPatientEarly);
						listSubJobs.add(dropOffPatientLate);
						if(vehicleCapacityPart(listSubJobs)) {
							merging= true;
							newPart.setListSubJobs(listSubJobs, inp, test);
							newPart.settingConnections(earlyPart,latePart);
							newRoute.getPartsRoute().add(newPart);
						}

						//						newRoute.getSubJobsList().add(pickUpPatientEarly);
						//						newRoute.getSubJobsList().add(pickUpPatientLate);
						//						newRoute.getSubJobsList().add(dropOffPatientEarly);
						//						newRoute.getSubJobsList().add(dropOffPatientLate);
					}
				}
			}
		}

		/*Option 2 <-pick up temprano - pick up tarde - drop off tarde - drop off temprano*/
		if(!merging) {
			/* Distance option 2*/ 

			double traveltimePickupDropOff=inp.getCarCost().getCost(pickUpPatientLate.getId()-1, dropOffPatientLate.getId()-1);
			double traveltimeDropOffDropOff=inp.getCarCost().getCost(dropOffPatientLate.getId()-1, dropOffPatientEarly.getId()-1);
			double distPickUpDropOffLate=travelTime12+traveltimePickupDropOff+traveltimeDropOffDropOff;
			// time windows
			if((dropOffPatientLate.getDepartureTime()+traveltimeDropOffDropOff)<=dropOffPatientEarly.getArrivalTime()) {// checking time windows (Early pick up)--- (patient Late pick up)
				// checking the detour
				boolean detourLateSubjob=checkingDetourPatient(depotJobLate,removeConnection,distHomeMedicalCentrelteOption2Late,PatientHouseMedicalCentreLate);
				boolean detourEarlySubJob=checkingDetourEarly(distPickUpDropOffLate,PatientHouseMedicalCentreEarly);
				if(detourLateSubjob && detourEarlySubJob) {

					listSubJobs.add(pickUpPatientEarly);
					listSubJobs.add(pickUpPatientLate);
					listSubJobs.add(dropOffPatientLate);
					listSubJobs.add(dropOffPatientEarly);
					if(vehicleCapacityPart(listSubJobs)) {
						newPart.setListSubJobs(listSubJobs, inp, test);
						newPart.settingConnections(earlyPart,latePart);
						newRoute.getPartsRoute().add(newPart);
						merging= true;
					}

				}

			}
		}
		return merging;
	}

	private boolean checkWaitingTime(SubJobs dropOffPatientLate, double travelTime34, SubJobs dropOffPatientEarly) {
		boolean waitingTime=false;

		// checking the waiting time for the node dropOffPatientEarly
		// structure (dropOffPatientLate)---travel34---(dropOffPatientEarly)
		if(dropOffPatientEarly.getArrivalTime()>=(dropOffPatientLate.getDepartureTime()+travelTime34)) {
			if(dropOffPatientEarly.getArrivalTime()-(dropOffPatientLate.getDepartureTime()+travelTime34)<=test.getCumulativeWaitingTime()) {
				waitingTime=true;	
			}}
		return waitingTime;
	}

	private boolean checkingDetourEarly(double distPickUpDropOffLate, Edge patientHouseMedicalCentreEarly) {
		boolean merging=false;
		if(distPickUpDropOffLate<=patientHouseMedicalCentreEarly.getDetour()){
			merging=true;
		}
		return merging;
	}

	private boolean checkingDetourEarlyPatient(double depotJobLate, Edge removeConnection) {
		boolean detourLate=false;
		if(depotJobLate<=removeConnection.getDetour()) {
			detourLate=true;
		}

		return detourLate;
	}


	private boolean checkingDetourPatient(double detour1, Edge directConnectionRemoved,double detour2, Edge directConnection) {
		boolean detourLate=false;
		if(detour1<=directConnectionRemoved.getDetour() && detour2<=directConnection.getDetour()) {
			detourLate=true;
		}
		return detourLate;
	}

	private boolean mergingIntraParts(Parts earlyPartRoute, Parts latePartRoute) {
		boolean merge= false;
		SubJobs firstJobEarly=earlyPartRoute.getListSubJobs().get(0);
		SubJobs secondJobEarly=earlyPartRoute.getListSubJobs().get(0);

		return merge;
	}

	private boolean evaluationDetour(Route earlyRoute, Route lateRoute) {
		boolean evaluation=false;
		SubJobs depot=earlyRoute.getPartsRoute().get(0).getListSubJobs().get(0);
		// Evaluation con el depot
		int options=typeOfParts(earlyRoute.getPartsRoute().get(1),lateRoute.getPartsRoute().get(1)) ; 
		switch(options) {// 1 patient-patient 2 homeCare-patient or patient-homeCare 3 homeCare-homeCare  
		case 1 :// 1 patient-patient 
			evaluation=detourPatients(earlyRoute.getPartsRoute().get(1),lateRoute.getPartsRoute().get(1),depot);
			break;

		case 2 : // 2 homeCare-patient or patient-homeCare
			evaluation=detourPatientClient(earlyRoute.getPartsRoute().get(1),lateRoute.getPartsRoute().get(1),depot);
			break; // optional

		default : // 3 homeCare-homeCare
			evaluation=detourClients(earlyRoute.getPartsRoute().get(1),lateRoute.getPartsRoute().get(1),depot);
		}

		return evaluation;
	}

	private boolean detourPatients(Parts early, Parts late, SubJobs depot) {
		boolean feasible=false;
		// checking respect to early patient
		SubJobs earlyPatientHouse=early.getListSubJobs().get(0);
		SubJobs earlyPatientMedicalCentre=early.getListSubJobs().get(1);
		SubJobs latePatientHouse=late.getListSubJobs().get(0);
		SubJobs latePatientMedicalCentre=late.getListSubJobs().get(1);
		// counting distance

		/*Option 1<-temprano - tarde - tarde - temprano*/
		// early patient
		String key= earlyPatientHouse.getSubJobKey()+earlyPatientMedicalCentre.getSubJobKey();  //to check detour ->temprano- temprano
		Edge HomeMedicalCentreEarly= early.getDirectoryConnections().get(key); 	
		double distHomeMedicalCentreEarlyOption1=inp.getCarCost().getCost(earlyPatientHouse.getId()-1, latePatientHouse.getId()-1)+inp.getCarCost().getCost(latePatientHouse.getId()-1,latePatientMedicalCentre.getId()-1)+inp.getCarCost().getCost(latePatientMedicalCentre.getId()-1,earlyPatientMedicalCentre.getId()-1);

		// late pateint
		Edge depotPatientHouseLate=new Edge(depot,latePatientHouse,inp,test);
		double distDepotHomeMedicalCentrelteOption1Late=inp.getCarCost().getCost(depot.getId()-1, earlyPatientHouse.getId()-1)+inp.getCarCost().getCost(earlyPatientHouse.getId()-1,latePatientHouse.getId()-1);

		if(distHomeMedicalCentreEarlyOption1<HomeMedicalCentreEarly.getDetour() && distDepotHomeMedicalCentrelteOption1Late<depotPatientHouseLate.getDetour()) {
			feasible=true;
		}
		else {
			/*Option 2<-temprano - tarde - temprano - tarde*/
			// Early <- detour temprano - temprano
			double distHomeMedicalCentreEarlyOption2=inp.getCarCost().getCost(earlyPatientHouse.getId()-1, latePatientHouse.getId()-1)+inp.getCarCost().getCost(latePatientHouse.getId()-1,earlyPatientMedicalCentre.getId()-1);	
			// Early <- detour tarde - tarte
			key= latePatientHouse.getSubJobKey()+latePatientMedicalCentre.getSubJobKey();// to check detour ->depot- tarde
			Edge PatientHouseMedicalCentreLate= late.getDirectoryConnections().get(key);
			double distHomeMedicalCentrelteOption2Late=inp.getCarCost().getCost(latePatientHouse.getId()-1, earlyPatientMedicalCentre.getId()-1)+inp.getCarCost().getCost(earlyPatientMedicalCentre.getId()-1,latePatientMedicalCentre.getId()-1);	
			if(distHomeMedicalCentreEarlyOption1<HomeMedicalCentreEarly.getDetour() && distHomeMedicalCentreEarlyOption2<HomeMedicalCentreEarly.getDetour() && distHomeMedicalCentrelteOption2Late<PatientHouseMedicalCentreLate.getDetour()) {
				feasible=true;
			}
		}
		return feasible;
	}

	private boolean detourPatientClient(Parts early, Parts late, SubJobs depot) {
		boolean feasible=false;
		double distanceAccum=inp.getCarCost().getCost(depot.getId()-1, early.getListSubJobs().get(0).getId()-1)+inp.getCarCost().getCost(early.getListSubJobs().get(0).getId()-1,late.getListSubJobs().get(0).getId()-1);

		Edge toCheck=new Edge(depot,late.getListSubJobs().get(0),inp,test);

		if(early.getListSubJobs().size()==1) { // el primer trabajo de la ruta es un cliente
			distanceAccum=inp.getCarCost().getCost(depot.getId()-1, early.getListSubJobs().get(0).getId()-1)+inp.getCarCost().getCost(early.getListSubJobs().get(0).getId()-1,late.getListSubJobs().get(0).getId()-1);
			if(toCheck.getDetour()>distanceAccum) {
				feasible=true;
			}

		}
		if(early.getListSubJobs().size()==2) { // Depot- patient- homeCareStaff-Patient
			// checking respect to the patient
			distanceAccum=inp.getCarCost().getCost(early.getListSubJobs().get(0).getId()-1, late.getListSubJobs().get(0).getId()-1)+inp.getCarCost().getCost(late.getListSubJobs().get(0).getId()-1,early.getListSubJobs().get(1).getId()-1);
			String key=early.getListSubJobs().get(0).getSubJobKey()+early.getListSubJobs().get(1).getSubJobKey();
			toCheck=early.getDirectoryConnections().get(key);
			boolean patient=false;
			boolean client=false;
			if(toCheck.getDetour()>distanceAccum) {
				patient=true;
			}
			// checking respect to the client
			distanceAccum=inp.getCarCost().getCost(depot.getId()-1, early.getListSubJobs().get(0).getId()-1)+inp.getCarCost().getCost(early.getListSubJobs().get(0).getId()-1,late.getListSubJobs().get(0).getId()-1);
			if(toCheck.getDetour()>distanceAccum) {
				client=true;
			}

			if(patient && client) {
				feasible=true;
			}
		}
		return feasible;
	}

	private boolean detourClients(Parts early, Parts late, SubJobs depot) {
		// son sólo dos clientes
		boolean detour=false;
		double distanceAccum=inp.getCarCost().getCost(depot.getId()-1, early.getListSubJobs().get(0).getId()-1)+inp.getCarCost().getCost(early.getListSubJobs().get(0).getId()-1,late.getListSubJobs().get(0).getId()-1);
		Edge toCheck=new Edge(depot,late.getListSubJobs().get(0),inp,test);
		if(toCheck.getDetour()>distanceAccum) {
			detour=true;
		}
		return detour;
	}

	private int typeOfParts(Parts parts, Parts parts2) {
		// typeParts: 1 patient-patient 2 homeCare-patient or patient-homeCare 3 homeCare-homeCare 
		int typeParts=-1;
		int part1=parts.getListSubJobs().size();
		int part2=parts2.getListSubJobs().size();
		if(parts.getQualificationLevel()>0 && parts2.getQualificationLevel()==0 ||parts2.getQualificationLevel()>0 && parts.getQualificationLevel()==0) {
			typeParts=2;
		}
		else {
			if(parts.getQualificationLevel()>0 && parts2.getQualificationLevel()>0) {

				typeParts=3;
			}	
			else {
				if(parts.getQualificationLevel()==0 && parts2.getQualificationLevel()==0) {
					typeParts=1;
				}	
			}
		}
		return typeParts;
	}

	private boolean mergingPart(Parts pref, Parts psplit,ArrayList<SubJobs> mergingPart) {
		// pref <- early part // psplit<- late part
		boolean mergingParts= false;





		for(int i=0;i<pref.getListSubJobs().size();i++) {
			SubJobs j0=pref.getListSubJobs().get(i);
			for(int j=0;j<pref.getListSubJobs().size();j++) {
				SubJobs j1=pref.getListSubJobs().get(j);
				boolean feasibleDetour=respectMaxdetour(i,j,pref,psplit);
				boolean insertingAfterJo= insertingSubjobs(j0,j1,pref,psplit);

				if(j0.getArrivalTime()<j1.getArrivalTime()) {

				}

			}
		}


		SubJobs lastInsertedJob=mergingPart.get(mergingPart.size()-1);
		SubJobs firstJobEarly=psplit.getListSubJobs().get(0);
		SubJobs firstJobLate=psplit.getListSubJobs().get(0);
		if(firstJobLate.getId()!=1) {
			if(firstJobEarly.getArrivalTime()<firstJobLate.getArrivalTime()) { // es factible ir del primer trabajo de pref a el primer trabajo psplit
				double tv=inp.getCarCost().getCost(firstJobEarly.getId()-1, firstJobLate.getId()-1); 
				double preliminarArrivalTime=firstJobEarly.getDepartureTime()+tv;
				if(firstJobLate.getArrivalTime()>preliminarArrivalTime) {
					if(firstJobLate.getArrivalTime()>preliminarArrivalTime) {


					}

				}
			}

		}
		return mergingParts;
	}


	private boolean respectMaxdetour(int i, int j, Parts pref, Parts psplit) {
		boolean feasible=false;
		if(i>0) { // evaluar el detour para el trabajo más cercano
			SubJobs jobN=pref.getListSubJobs().get(i);
			SubJobs jobO=pref.getListSubJobs().get(i);
			SubJobs jobP=pref.getListSubJobs().get(i);
		}

		return feasible;
	}

	private boolean insertingSubjobs(SubJobs j0, SubJobs j1, Parts pref, Parts psplit) {
		boolean instert=false;
		if(j0.getArrivalTime()<j1.getArrivalTime()) {
			double tv=inp.getCarCost().getCost(j0.getId()-1, j1.getId()-1);
			double arrivalTime=j0.getDepartureTime()+tv;



			if(j1.getArrivalTime()>arrivalTime) { //che evaluar si es posible llegar a tiempo


			}
		}
		return instert;
	}

	private Solution interMergingParts(Solution copySolution) {
		// tengo que asegurar que la jornada laboral destinada al conductor sea mayor que la máxima jornada permitida
		System.out.println(copySolution.toString());
		for(int route1=0;route1<copySolution.getRoutes().size();route1++) {
			for(int route2=0;route2<copySolution.getRoutes().size();route2++) {
				int part=0;
				int start=2;
				Route vehicle= new Route();
				Route iR=copySolution.getRoutes().get(route1);
				if(iR.getPartsRoute().size()>2) {
					Route jR=copySolution.getRoutes().get(route2);
					System.out.println("\nRoute iR"+ iR.toString());
					System.out.println("\nRoute jR"+ jR.toString());
					if(possibleMerge(iR,jR)) {
						if(iR.getJobsDirectory().containsKey("D50") || jR.getJobsDirectory().containsKey("D50")) {
							System.out.println("\nRoute iR"+ iR.toString());
							System.out.println("\nRoute jR"+ jR.toString());
						}
						if(iR.getJobsDirectory().containsKey("D2") || jR.getJobsDirectory().containsKey("D2")) {
							System.out.println("\nRoute iR"+ iR.toString());
							System.out.println("\nRoute jR"+ jR.toString());
						}
						Route refRoute=selecctingStartRoute(iR,jR);
						Route toInsertRoute=selecctingRouteToInsert(iR,jR);
						if(refRoute.getJobsDirectory().containsKey("D59")) {
							System.out.println("\nRoute iR"+ iR.toString());
							System.out.println("\nRoute jR"+ jR.toString());
						}
						System.out.println("\nRoute refRoute"+ refRoute.toString());
						System.out.println("\nRoute toInsertRoute"+ toInsertRoute.toString());
						if(refRoute.getPartsRoute().get(1).getListSubJobs().get(0).getId()==21 && toInsertRoute.getPartsRoute().get(1).getListSubJobs().get(0).getId()==7) {
							System.out.println("\nRoute iR"+ iR.toString());
							System.out.println("\nRoute jR"+ jR.toString());
						}
						if(toInsertRoute.getPartsRoute().get(1).getListSubJobs().get(0).getId()==21 && refRoute.getPartsRoute().get(1).getListSubJobs().get(0).getId()==7) {
							System.out.println("\nRoute iR"+ iR.toString());
							System.out.println("\nRoute jR"+ jR.toString());
						}
						boolean inserted=insertionAllRoute(refRoute,toInsertRoute,vehicle,part);
						if(!inserted) { // insertion part by part
							vehicle.getPartsRoute().add(refRoute.getPartsRoute().get(0));
							vehicle.getPartsRoute().add(refRoute.getPartsRoute().get(1));
							vehicle.setAmountParamedic(refRoute.getAmountParamedic());
							vehicle.setHomeCareStaff(refRoute.getHomeCareStaff());
							for(part=start;part<refRoute.getPartsRoute().size()-1;part++) {
								int newStart=0;
								inserted=isertingRoute(vehicle,toInsertRoute,refRoute, part);
								if(!inserted) { // insert part by part
									vehicle.getPartsRoute().add(refRoute.getPartsRoute().get(part));
									vehicle.updateRouteFromParts(inp,test);
								}
								else {
									newStart=part-1;
									part=newStart; // para que siga metiendo las partes de la ruta que aún falta por incorporar
								}

							}
							if(part==refRoute.getPartsRoute().size()-1 && !inserted ) {
								// en el caso de que no se haya terminado de insertar el resto
								boolean insertRest=isertingTheRest(vehicle,toInsertRoute);
								if(!insertRest) {
									vehicle.getPartsRoute().add(refRoute.getPartsRoute().get(part));
									vehicle.updateRouteFromParts(inp,test);
								}
							}
						}
						//						refRoute=updatingNewRoutes(refRoute,vehicle,part);	
						//						toInsertRoute.updateRouteFromParts(inp,test);
						refRoute=updatingNewRoutes(refRoute,vehicle,part);	
						toInsertRoute=updatingChangedRoutes(toInsertRoute,refRoute,part);	
						vehicle.getPartsRoute().clear();

						System.out.println("Route refRoute"+ refRoute.toString());
						System.out.println("Route toInsertRoute"+ toInsertRoute.toString());
						//routeVehicleList.add(vehicle);
					}
				}
				else {
					break;
				}
			}
			//updatingListRoutes();
			//			System.out.println(" Route list ");
			//			for(Route r:routeVehicleList) {
			//				System.out.println(r.toString());
			//			}
		}
		settingSolution(copySolution);

		return copySolution;
	}

	private void settingSolution(Solution copySolution) {
		for(Route r:copySolution.getRoutes()) {
			if(r.getPartsRoute().size()>2) {
				routeVehicleList.add(r);
			}
		}
		copySolution.getRoutes().clear();
		for(Route r:routeVehicleList) {
			copySolution.getRoutes().add(r);
		}
	}

	private void updatingSolution(Solution copySolution) {
		copySolution.getRoutes().clear();
		for(Route r:routeVehicleList) {
			copySolution.getRoutes().add(r);
		}
		computeSolutionCost(copySolution);

		transferPartsInformation(routeVehicleList);

	}

	private void transferPartsInformation(ArrayList<Route> routeVehicleList2) {
		for(Route r:routeVehicleList2 ) {
			r.getSubJobsList().clear();
			for(int part=1;part<r.getPartsRoute().size()-1;part++) {
				for(SubJobs j:r.getPartsRoute().get(part).getListSubJobs()) {
					r.getSubJobsList().add(j);
				}
				r.getPartsRoute().get(part).settingConnections(r.getPartsRoute().get(part).getListSubJobs(), inp, test);
			}
		}
	}

	private boolean insertionAllRoute(Route refRoute, Route toInsertRoute, Route vehicle, int part) {
		boolean feasibleTimes= false;
		if(capacityVehicle(refRoute,toInsertRoute)) {	
			feasibleTimes=checkingTimeWindows(refRoute,toInsertRoute,vehicle,part);
		}
		//}	
		return feasibleTimes;
	}

	private boolean capacityVehicle(Route refRoute, Route toInsertRoute) {
		boolean enoughCapacity=false;
		int passengers=0;
		int action=0;
		for(int i=0;i<refRoute.getPartsRoute().size()-1;i++) { // route 1
			Parts r=refRoute.getPartsRoute().get(i);
			for(SubJobs s:r.getListSubJobs()) {
				action=s.getTotalPeople();
				passengers+=action;
			}
		}
		for(int i=0;i<toInsertRoute.getPartsRoute().size()-1;i++) { // route 2
			Parts r=toInsertRoute.getPartsRoute().get(i);
			for(SubJobs s:r.getListSubJobs()) {
				action=s.getTotalPeople();
				passengers+=action;
			}
		}
		if(Math.abs(passengers)>inp.getVehicles().get(0).getMaxCapacity()) {
			enoughCapacity=false	;
		}
		else {
			enoughCapacity=true	
					;}
		return enoughCapacity;
	}

	private boolean checkingTimeWindows(Route refRoute, Route toInsertRoute, Route vehicle, int part) {
		boolean inserted=keeptheTimes(refRoute,toInsertRoute,vehicle,part);
		//		if(!inserted) { // changing time
		//			inserted=changetheTimes(refRoute,toInsertRoute,vehicle,part);
		//		}
		return inserted;
	}


	private boolean changetheTimes(Route refRoute, Route toInsertRoute, Route vehicle, int part) {
		// estructure: lastSubjobInRoute ----firstSubjob**---
		boolean inserted=false;
		int lastPart=refRoute.getPartsRoute().size()-1;// <- esta parte hace referencia al depot
		//	.getListSubJobs().get(0);
		Parts partEnd=refRoute.getPartsRoute().get(lastPart-1);
		SubJobs lastSubjobInRoute= partEnd.getListSubJobs().get(partEnd.getListSubJobs().size()-1);
		SubJobs firstSubjob= toInsertRoute.getPartsRoute().get(1).getListSubJobs().get(0);
		double tv=inp.getCarCost().getCost(lastSubjobInRoute.getId()-1, firstSubjob.getId()-1);
		// computing new Times
		double loadUnloadTime=0;
		double registration=0;
		if(firstSubjob.isClient()) {
			loadUnloadTime=test.getloadTimeHomeCareStaff();
		}
		else {
			loadUnloadTime=test.getloadTimePatient();
			registration=test.getRegistrationTime();
		}
		double departure=lastSubjobInRoute.getDepartureTime()-tv;
		double arrival=departure-loadUnloadTime;
		double startServiceTime=arrival+registration;
		if(startServiceTime >=firstSubjob.getStartTime() && startServiceTime<=firstSubjob.getEndTime()) {
			firstSubjob.setarrivalTime(arrival);
			firstSubjob.setStartServiceTime(startServiceTime);
			firstSubjob.setEndServiceTime(startServiceTime+firstSubjob.getReqTime());
			firstSubjob.setdepartureTime(departure);
		}

		if((lastSubjobInRoute.getDepartureTime()+tv)<firstSubjob.getArrivalTime()) {
			part=refRoute.getPartsRoute().size();
			inserted=true;
			for(int j=0;j<refRoute.getPartsRoute().size()-1;j++) { // no se incluye el depot
				vehicle.getPartsRoute().add(refRoute.getPartsRoute().get(j));
			}
			for(int j=1;j<toInsertRoute.getPartsRoute().size();j++) { // no se incluye el depot
				vehicle.getPartsRoute().add(toInsertRoute.getPartsRoute().get(j));
			}
			vehicle.setAmountParamedic(refRoute.getAmountParamedic()+toInsertRoute.getAmountParamedic());
			vehicle.setHomeCareStaff(refRoute.getHomeCareStaff()+toInsertRoute.getHomeCareStaff());

			vehicle.updateRouteFromParts(inp,test);
		}
		return inserted;
	}

	private boolean keeptheTimes(Route refRoute, Route toInsertRoute, Route vehicle, int part) {
		boolean inserted=false;
		int lastPart=refRoute.getPartsRoute().size()-1;// <- esta parte hace referencia al depot
		//	.getListSubJobs().get(0);
		Parts partEnd=refRoute.getPartsRoute().get(lastPart-1);
		SubJobs lastSubjobInRoute= partEnd.getListSubJobs().get(partEnd.getListSubJobs().size()-1);
		SubJobs firstSubjob= toInsertRoute.getPartsRoute().get(1).getListSubJobs().get(0);
		double tv=inp.getCarCost().getCost(lastSubjobInRoute.getId()-1, firstSubjob.getId()-1);
		if((lastSubjobInRoute.getDepartureTime()+tv)<firstSubjob.getArrivalTime()) {
			part=refRoute.getPartsRoute().size();
			inserted=true;
			for(int j=0;j<refRoute.getPartsRoute().size()-1;j++) { // no se incluye el depot
				vehicle.getPartsRoute().add(refRoute.getPartsRoute().get(j));
			}
			for(int j=1;j<toInsertRoute.getPartsRoute().size();j++) { // no se incluye el depot
				vehicle.getPartsRoute().add(toInsertRoute.getPartsRoute().get(j));
			}
			vehicle.setAmountParamedic(refRoute.getAmountParamedic()+toInsertRoute.getAmountParamedic());
			vehicle.setHomeCareStaff(refRoute.getHomeCareStaff()+toInsertRoute.getHomeCareStaff());
			vehicle.getPartsRoute().get(0).getListSubJobs().get(0).setTotalPeople((int)(vehicle.getAmountParamedic()+vehicle.getHomeCareStaff()));
			vehicle.updateRouteFromParts(inp,test);
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
				for(Route clone:repeatedRoutes) { // se itera sobre la lista de rutas y se seleccional la más larga
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
				if(r.getJobsDirectory().containsKey(j.getSubJobKey())) { //si es así es porque la ruta clone es una copia de la ruta r
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
		// el objetivo es dejar que la ruta refRoute sólo tenga partes contenidas en la ruta de vehicle
		refRoute.getPartsRoute().clear();
		for(Parts p:vehicle.getPartsRoute()) {// no se considera el depot
			refRoute.getPartsRoute().add(p);
		}
		refRoute.updateRouteFromParts(inp,test);
		int totalPeople=vehicle.getPartsRoute().get(0).getListSubJobs().get(0).getTotalPeople();
		refRoute.setAmountParamedic(vehicle.getAmountParamedic());
		refRoute.setHomeCareStaff(vehicle.getHomeCareStaff());
		refRoute.getPartsRoute().get(0).getListSubJobs().get(0).setTotalPeople(totalPeople);
		return refRoute;
	}

	private Route updatingChangedRoutes(Route refRoute, Route vehicle, int part) {
		ArrayList<Parts> toKeep= new ArrayList<Parts>();
		for(Parts p:refRoute.getPartsRoute()) {
			toKeep.add(p);
		}
		for(Parts p:toKeep) {
			for(SubJobs s:p.getListSubJobs()) {
				if(s.getId()!=1) {
					if(vehicle.getJobsDirectory().containsKey(s.getSubJobKey())) {
						refRoute.getPartsRoute().remove(p);
						break;
					}
				}
			}

		}
		refRoute.updateRouteFromParts(inp,test);
		return refRoute;
	}
	private boolean isertingRoute(Route vehicle, Route toInsertRoute, Route refRoute, int part2) {
		boolean inserted=keepAStartServiceTime(vehicle,toInsertRoute,refRoute,part2);

		return inserted; 
	}

	private boolean changeServiceTime(Route vehicle, Route toInsertRoute, Route refRoute, int part2) {
		boolean inserted=false;		
		// Este metodo intena insertar las partes de la otra ruta. Siempre evalua las rutas que ya estan integradas en otro
		Route changing= new Route(toInsertRoute);
		for(int i=1;i<toInsertRoute.getPartsRoute().size()-1;i++) { // -1 para no incluir el depot al final
			// primer intenta insertar la parte de la otra ruta
			// structure route ref part 1 {1,...,lastSubjobInRoute} part 2{nextSubJobPart, } route to insert part 1 {firstSubjob,...,lastSubjob}
			Parts endPart=vehicle.getPartsRoute().getLast();
			SubJobs lastSubjobInRoute= endPart.getListSubJobs().get(endPart.getListSubJobs().size()-1);
			SubJobs firstSubjob= toInsertRoute.getPartsRoute().get(i).getListSubJobs().get(0);
			SubJobs lastSubjob= toInsertRoute.getPartsRoute().get(i).getListSubJobs().get(toInsertRoute.getPartsRoute().get(i).getListSubJobs().size()-1);
			SubJobs nextSubJobPart= refRoute.getPartsRoute().get(part2).getListSubJobs().get(0);
			double tv=inp.getCarCost().getCost(lastSubjobInRoute.getId()-1, firstSubjob.getId()-1);
			boolean nextPart= checkingNextPart(lastSubjobInRoute,firstSubjob,lastSubjob,nextSubJobPart,refRoute.getPartsRoute().get(part2),toInsertRoute.getPartsRoute().get(i),vehicle);
			if((lastSubjobInRoute.getDepartureTime()+tv)<firstSubjob.getArrivalTime() && nextPart) {
				if((firstSubjob.getArrivalTime()-(lastSubjobInRoute.getDepartureTime()+tv))<=test.getCumulativeWaitingTime()) {
					vehicle.getPartsRoute().add(toInsertRoute.getPartsRoute().get(i));
					if(refRoute.getPartsRoute().get(part2).getListSubJobs().get(0).getId()==1) {
						if(i==1) {
							vehicle.setAmountParamedic(vehicle.getAmountParamedic()+toInsertRoute.getAmountParamedic());
							vehicle.setHomeCareStaff(vehicle.getHomeCareStaff()+toInsertRoute.getHomeCareStaff());
						}
						vehicle.getPartsRoute().add(refRoute.getPartsRoute().get(part2));
						part2++;
					}
					vehicle.updateRouteFromParts(inp,test);
					System.out.println("Printing changing 2 " +vehicle.toString());
					changing.removingParts(toInsertRoute.getPartsRoute().get(i));
					changing.updateRouteFromParts(inp,test);
					System.out.println("Printing changing 1 " +changing.toString());
					System.out.println("Printing vehicleRouting" +vehicle.toString());
					inserted=true;	
					break;
				}
			}
		}
		toInsertRoute.getPartsRoute().clear();
		for(Parts part:changing.getPartsRoute()) {
			toInsertRoute.getPartsRoute().add(part);
		}
		toInsertRoute.updateRouteFromParts(inp,test);
		System.out.println("Printing toInsertRoute" +toInsertRoute.toString());
		return inserted;
	}

	private boolean keepAStartServiceTime(Route vehicle, Route toInsertRoute, Route refRoute, int part2) {
		boolean inserted=false;		
		// Este metodo intena insertar las partes de la otra ruta. Siempre evalua las rutas que ya estan integradas en otro
		Route changing= new Route(toInsertRoute);
		for(int i=1;i<toInsertRoute.getPartsRoute().size()-1;i++) { // -1 para no incluir el depot al final
			// primer intenta insertar la parte de la otra ruta

			Parts endPart=vehicle.getPartsRoute().getLast();
			SubJobs lastSubjobInRoute= endPart.getListSubJobs().get(endPart.getListSubJobs().size()-1);
			SubJobs firstSubjob= toInsertRoute.getPartsRoute().get(i).getListSubJobs().get(0);
			SubJobs lastSubjob= toInsertRoute.getPartsRoute().get(i).getListSubJobs().get(toInsertRoute.getPartsRoute().get(i).getListSubJobs().size()-1);
			SubJobs nextSubJobPart= refRoute.getPartsRoute().get(part2).getListSubJobs().get(0);
			double tv=inp.getCarCost().getCost(lastSubjobInRoute.getId()-1, firstSubjob.getId()-1);
			boolean nextPart= checkingNextPart(lastSubjobInRoute,firstSubjob,lastSubjob,nextSubJobPart,refRoute.getPartsRoute().get(part2),toInsertRoute.getPartsRoute().get(i),vehicle);
			if((lastSubjobInRoute.getDepartureTime()+tv)<firstSubjob.getArrivalTime() && nextPart) {
				if((firstSubjob.getArrivalTime()-(lastSubjobInRoute.getDepartureTime()+tv))<=test.getCumulativeWaitingTime()) {
					if(vehicleCapacity(refRoute,part2,toInsertRoute.getPartsRoute().get(i))) {
						vehicle.getPartsRoute().add(toInsertRoute.getPartsRoute().get(i));
						if(i==1) {
							//if(refRoute.getPartsRoute().get(part2).getListSubJobs().get(0).getId()==1) {
							//if(i==1) {
							double personnel1=	refRoute.getAmountParamedic();
							vehicle.setAmountParamedic(personnel1+toInsertRoute.getAmountParamedic());
							personnel1=refRoute.getHomeCareStaff();
							vehicle.setHomeCareStaff(personnel1+toInsertRoute.getHomeCareStaff());

							vehicle.getPartsRoute().get(0).getListSubJobs().get(0).setTotalPeople((int)(vehicle.getAmountParamedic()+vehicle.getHomeCareStaff()));

							//}
							//vehicle.getPartsRoute().add(refRoute.getPartsRoute().get(part2));
							part2++;
						}
						vehicle.updateRouteFromParts(inp,test);
						System.out.println("Printing changing 2 " +vehicle.toString());
						changing.removingParts(toInsertRoute.getPartsRoute().get(i));
						changing.updateRouteFromParts(inp,test);
						System.out.println("Printing changing 1 " +changing.toString());
						System.out.println("Printing vehicleRouting" +vehicle.toString());
						inserted=true;	
						break;	
					}				
				}
			}
			if(!inserted) {
				break;
			}
		}
		toInsertRoute.getPartsRoute().clear();
		for(Parts part:changing.getPartsRoute()) {
			toInsertRoute.getPartsRoute().add(part);
		}
		toInsertRoute.updateRouteFromParts(inp,test);
		System.out.println("Printing toInsertRoute" +toInsertRoute.toString());
		return inserted;
	}

	private boolean vehicleCapacity(Route ref,int insertionPosition, Parts parts2) {
		boolean capacityEnough=false;
		int passenger=0;
		int action=0;
		for(int i=0;i<ref.getPartsRoute().size();i++) {
			if(i==insertionPosition) {
				for(SubJobs s:parts2.getListSubJobs()) {
					action=s.getTotalPeople();
					passenger+=action;
				}	
				for(SubJobs s:ref.getPartsRoute().get(i).getListSubJobs()) {
					action=s.getTotalPeople();
					passenger+=action;
				}
				if(Math.abs(passenger)>inp.getVehicles().get(0).getMaxCapacity()){
					capacityEnough=false;
					break;
				}
				else {
					capacityEnough=true;
				}
			}
			else {
				for(SubJobs s:ref.getPartsRoute().get(i).getListSubJobs()) {
					action=s.getTotalPeople();
					passenger+=action;
				}
				if(Math.abs(passenger)>inp.getVehicles().get(0).getMaxCapacity()){
					capacityEnough=false;
					break;
				}
				else {
					capacityEnough=true;
				}
			}
		}

		if(Math.abs(passenger)>inp.getVehicles().get(0).getMaxCapacity()){
			capacityEnough=false;
		}
		else {
			capacityEnough=true;
		}
		return capacityEnough;
	}

	private boolean isertingTheRest(Route vehicle, Route toInsertRoute) {
		boolean inserted=false;		
		// Este metodo intena insertar las partes de la otra ruta. Siempre evalua las rutas que ya estan integradas en otro
		Route changing= new Route(toInsertRoute);
		Parts endPart=vehicle.getPartsRoute().getLast();
		SubJobs lastSubjobInRoute= endPart.getListSubJobs().get(endPart.getListSubJobs().size()-1);
		SubJobs firstSubjob= toInsertRoute.getPartsRoute().get(1).getListSubJobs().get(0);

		// primer intenta insertar la parte de la otra ruta

		if(lastSubjobInRoute.getDepartureTime()<firstSubjob.getArrivalTime()) {
			for(int i=1;i<toInsertRoute.getPartsRoute().size();i++) { // -1 para no incluir el depot al final
				vehicle.getPartsRoute().add(toInsertRoute.getPartsRoute().get(i));
				vehicle.updateRouteFromParts(inp,test);
				System.out.println("Printing changing 2 " +vehicle.toString());
				changing.removingParts(toInsertRoute.getPartsRoute().get(i));
				changing.updateRouteFromParts(inp,test);
				System.out.println("Printing changing 1 " +changing.toString());
				System.out.println("Printing vehicleRouting" +vehicle.toString());
				inserted=true;	

			}
		}
		toInsertRoute.getPartsRoute().clear();
		for(Parts part:changing.getPartsRoute()) {
			toInsertRoute.getPartsRoute().add(part);
		}
		toInsertRoute.updateRouteFromParts(inp,test);
		System.out.println("Printing toInsertRoute" +toInsertRoute.toString());
		return inserted; 
	}
	//boolean nextPart= checkingNextPart(lastSubjobInRoute,firstSubjob,lastSubjob,nextSubJobPart,refRoute.getPartsRoute().get(part2),toInsertRoute.getPartsRoute().get(i),vehicle);

	private boolean checkingNextPart(SubJobs lastSubjobInRoute, SubJobs firstSubjob, SubJobs lastSubjob, SubJobs nextSubJobPart, Parts nextPartInsert, Parts toInsert, Route vehicle) {
		// structure route ref part 1 {1,...,lastSubjobInRoute} part 2{nextSubJobPart, } route to insert part 1 {firstSubjob,...,lastSubjob}
		boolean nextPart=false;
		double tvlastSubjobInRoutefirstSubjob=inp.getCarCost().getCost(lastSubjobInRoute.getId()-1, firstSubjob.getId()-1);
		double tvlastSubjobnextSubJobPart =inp.getCarCost().getCost(lastSubjob.getId()-1, nextSubJobPart.getId()-1);
		if(lastSubjobInRoute.getId()==1) {
			nextPart=true;
		}
		else {

			if(lastSubjobInRoute.getDepartureTime()+tvlastSubjobInRoutefirstSubjob<firstSubjob.getArrivalTime() && lastSubjob.getDepartureTime()+tvlastSubjobnextSubJobPart<nextSubJobPart.getArrivalTime()) {
			}
			else { // intentar cambiar la hora del firstSubjob
				// se cambia la hora tomando como referencia el trabajo futuro
				double registrationTime=0;
				double loadUnloadTime=0;
				if(firstSubjob.isClient()) { // si es un cliente 
					loadUnloadTime=test.getloadTimeHomeCareStaff();
				}
				else {
					loadUnloadTime=test.getloadTimePatient();
					registrationTime=test.getRegistrationTime();
				}
				double departureTimefirstSubjob=nextSubJobPart.getArrivalTime()-tvlastSubjobnextSubJobPart;
				double arrivalTime=departureTimefirstSubjob-loadUnloadTime;
				double startServiceTime=arrivalTime+registrationTime;
				if(startServiceTime>=firstSubjob.getStartTime() && startServiceTime<=firstSubjob.getEndTime()) {
					firstSubjob.setStartServiceTime(startServiceTime);
					firstSubjob.setEndServiceTime(firstSubjob.getsortLTWSizeCriterion()+firstSubjob.getReqTime());
					firstSubjob.setarrivalTime(arrivalTime);
					firstSubjob.setdepartureTime(departureTimefirstSubjob);
					nextPart=true;
				}

			}
		}
		return nextPart;
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
		boolean driverRouteLength=false;
		if(iR.getPartsRoute().size()>2 && jR.getPartsRoute().size()>2) {
			routeToRemve=true;
		}
		if(iR!=jR ) {
			//if(iR.getHomeCareStaff()==jR.getHomeCareStaff() && iR.getAmountParamedic()==jR.getAmountParamedic()) {
			diffRoute=true;	
			//}
		}
		// Revisar que el número de personas en el auto no excedan de la capacidad del
		double totalPassenger=iR.getAmountParamedic()+iR.getHomeCareStaff()+jR.getAmountParamedic()+jR.getHomeCareStaff();
		if(totalPassenger < inp.getVehicles().get(0).getMaxCapacity()) {
			capacityVehicle=true;
		}
		if((iR.getDurationRoute()+jR.getDurationRoute())<=test.getRouteLenght()) {
			//if((iR.getTravelTime()+iR.getloadUnloadRegistrationTime()+jR.getTravelTime()+jR.getloadUnloadRegistrationTime())<=test.getRouteLenght()) {
			driverRouteLength=true;
		}

		if(capacityVehicle && diffRoute && routeToRemve && driverRouteLength) {
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
				System.out.println(r.toString());
				r.updateRouteFromParts(inp,test);
				System.out.println(r.toString());
				r.setIdRoute(routeN);
				initialSol.getRoutes().add(r);
			}
		}
		System.out.println(initialSol);
		// Computar costos asociados a la solucion
		computeSolutionCost(initialSol);
		System.out.println(initialSol);
		// la lista de trabajos asociados a la ruta

		transferPartsInformation(routeList);

		// list passengers
		double paramedic=0;
		double homeCoreStaff=0;
		double driver=0;
		for(Route r:routeList) {
			paramedic+=r.getAmountParamedic();
			homeCoreStaff+=r.getHomeCareStaff();
			driver+=r.getAmountDriver();
		}
		initialSol.setHomeCareStaff(homeCoreStaff);
		initialSol.setParamedic(paramedic);
		return initialSol;
	}

	private void computeSolutionCost(Solution initialSol) {
		// sólo se considera la driving route
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
			r.updateRouteFromParts(inp,test);
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
		System.out.println(initialSol.toString());
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
			for(Parts part:turn.getRouteParts()) { // tan pronto se inserta una parte entonces se continua con la siguiente hasta que todos los turnos sean asignados a un vehículo
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
		// considerar la hora en la que el vehículo llega al dept
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
				newParts.setListSubJobs(newPart1,inp,test);
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
		if(sortedSchift.size()==1) { 	// 1. si el primer trabajo ya en la lista es más tardío el el inicio del trabajo de part
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
		int lastJobToInsert=turn.getRoute().getSubJobsList().size();
		SubJobs firstsubJob=turn.getRoute().getSubJobsList().get(lastJobToInsert-1);
		int lastJobInRoute=sortedSchift.get(0).getRoute().getSubJobsList().size();
		SubJobs firstTurnInList=sortedSchift.get(0).getRoute().getSubJobsList().get(lastJobInRoute-1);
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
			lastJobInRoute=sortedSchift.get(lastTurn).getRoute().getSubJobsList().size();
			SubJobs lastTurnInList=sortedSchift.get(lastTurn).getRoute().getSubJobsList().get(lastJobInRoute-1);
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
		ArrayList<Parts> qualification0= assigmentParamedic(q0,clasification0);

		//Qualification level from 1 to 3
		ArrayList<Parts> qualification1= assigmentParamedic(q1,clasification1); // here are not considering working hours
		ArrayList<Parts> qualification2= assigmentParamedic(q2,clasification2);
		ArrayList<Parts> qualification3= assigmentParamedic(q3,clasification3);

		//downgradings(qualification3,qualification2);
		downgradings(qualification2,qualification1);
		downgradings(qualification3,qualification2);
		downgradings(qualification3,qualification1); //No se considera porque no es tan facil controlar el tiempo de trabajo

		//downgradings(qualification2,qualification1);


		for(Parts schifts:qualification0) {
			Parts newParts= null;
			if(!schifts.getListSubJobs().isEmpty()) {
				newParts= new Parts(schifts);
			}
			if(newParts!=null) {
				schift.add(newParts);
			}
		}




		// home care staff
		for(Parts schifts:qualification1) {
			Parts newParts= null;
			if(!schifts.getListSubJobs().isEmpty()) {
				newParts= new Parts(schifts);
			}
			if(newParts!=null) {
				schift.add(newParts);
			}
		}



		for(Parts schifts:qualification2) {
			Parts newParts= null;
			if(!schifts.getListSubJobs().isEmpty()) {
				newParts= new Parts(schifts);
			}
			if(newParts!=null) {
				schift.add(newParts);
			}
		}
		for(Parts schifts:qualification3) {
			Parts newParts= null;
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



	private void downgradings(ArrayList<Parts> highQualification, ArrayList<Parts> lowQualification) {
		// cada parte es un personal
		boolean insertion=false;
		for(Parts high:highQualification) {
			insertion=downgradingsPart(high,lowQualification);

		}	
		System.out.println("Print solution"+insertion );
	}











	private boolean downgradingsPart(Parts high, ArrayList<Parts> lowQualification) {
		boolean insertion =false;
		// structure (firstHight)---(lastHight)---(firstLow)---(lastLow)
		for(Parts low:lowQualification) {
			if(!low.getListSubJobs().isEmpty() && !high.getListSubJobs().isEmpty()) {
				SubJobs firstHight=high.getListSubJobs().get(0); // head of the high cualification level
				SubJobs lastHight=high.getListSubJobs().get(high.getListSubJobs().size()-1); // head of the high cualification level
				SubJobs firstLow=low.getListSubJobs().get(0); // head of the low cualification level
				SubJobs lastLow=low.getListSubJobs().get(low.getListSubJobs().size()-1); // head of the low cualification level
				double distanceFromDepot=inp.getCarCost().getCost(0, firstHight.getId()-1);
				double distanceToDepot=inp.getCarCost().getCost(lastLow.getId()-1, 0);
				double distanceConnection=inp.getCarCost().getCost(lastHight.getId()-1, firstLow.getId()-1);
				if ((lastHight.getDepartureTime()-lastLow.getDepartureTime()+distanceFromDepot+distanceToDepot+distanceConnection)<test.getWorkingTime()) { // revisar las working horas
					insertion=tryingToInsert(distanceFromDepot,distanceToDepot,distanceConnection,firstLow,lastLow,lastLow,high,low);
				}
				else {
					insertion=tryingToInsertearly(high,low);
					if(!insertion) {
						insertion=tryingToInsertchangingTime(high,low);}
				}
				if(insertion) {
					insertion=false;
					low.getListSubJobs().clear();
				}
			}
		}


		return insertion;
	}

	private boolean tryingToInsertearly(Parts high, Parts low) {
		// structure (low) - (high)
		boolean inserted=false;
		SubJobs firstHight=high.getListSubJobs().get(0); // head of the high cualification level
		SubJobs lastHight=high.getListSubJobs().get(high.getListSubJobs().size()-1); // head of the high cualification level
		SubJobs firstLow=low.getListSubJobs().get(0); // head of the low cualification level
		SubJobs lastLow=low.getListSubJobs().get(low.getListSubJobs().size()-1); // head of the low cualification level
		double distanceFromDepot=inp.getCarCost().getCost(0, firstLow.getId()-1);
		double distanceToDepot=inp.getCarCost().getCost(lastHight.getId()-1, 0);
		double distanceConnection=inp.getCarCost().getCost(lastLow.getId()-1, firstHight.getId()-1);
		double possibleDepartureTime=firstHight.getArrivalTime()-distanceConnection;
		double workTime=lastHight.getDepartureTime()-firstLow.getArrivalTime()+distanceFromDepot+distanceToDepot+distanceConnection;
		if(workTime<test.getWorkingTime()) { // working time
			if(lastLow.getDepartureTime()<=possibleDepartureTime && lastLow.getDepartureTime()<firstHight.getArrivalTime()) {
				inserted=true;
				ArrayList<SubJobs> list= new ArrayList<SubJobs>();
				for(SubJobs j:low.getListSubJobs()) {
					list.add(j);
				}
				for(SubJobs j:high.getListSubJobs()) {
					list.add(j);
				}
				high.getListSubJobs().clear();
				for(SubJobs j:list) {
					high.getListSubJobs().add(j);
				}
			}
		}
		return inserted;
	}

	private boolean tryingToInsertchangingTime(Parts high, Parts low) {
		// quien cambia la hora son los trabajos que estan en low
		boolean inserted=false;

		SubJobs firstHight=high.getListSubJobs().get(0); // head of the high cualification level
		SubJobs lastHight=high.getListSubJobs().get(high.getListSubJobs().size()-1); // head of the high cualification level
		SubJobs firstLow=low.getListSubJobs().get(0); // head of the low cualification level
		SubJobs lastLow=low.getListSubJobs().get(low.getListSubJobs().size()-1); // head of the low cualification level
		if(lastHight.getDepartureTime()<firstLow.getStartTime()) {
			double distanceFromDepot=inp.getCarCost().getCost(0, firstHight.getId()-1);
			double distanceToDepot=inp.getCarCost().getCost(lastLow.getId()-1, 0);
			double distanceConnection=inp.getCarCost().getCost(lastHight.getId()-1, firstLow.getId()-1);
			// possible new times
			double arrivalTime=lastHight.getDepartureTime()+distanceConnection;
			double possibleStartTime=arrivalTime+test.getloadTimeHomeCareStaff();
			if(possibleStartTime>=firstLow.getStartTime() && possibleStartTime<=lastLow.getEndTime()) {
				ArrayList<SubJobs> part=calculating(possibleStartTime,low);
				if(!part.isEmpty()) {
					double newEndLow=part.get(part.size()-1).getDepartureTime();
					if((newEndLow-firstHight.getArrivalTime()+distanceFromDepot+distanceToDepot+distanceConnection)<=test.getWorkingTime()) {
						inserted=true;
						changingTimeLow(part,low);
					}
				}
			}
		}
		return inserted;
	}

	private void changingTimeLow(ArrayList<SubJobs> part, Parts low) {
		for(int i=0;i<part.size(); i++) {
			low.getListSubJobs().get(i).setarrivalTime(part.get(i).getArrivalTime());
			low.getListSubJobs().get(i).setStartServiceTime(part.get(i).getstartServiceTime());
			low.getListSubJobs().get(i).setEndServiceTime(part.get(i).getendServiceTime());
			low.getListSubJobs().get(i).setdepartureTime(part.get(i).getDepartureTime());
		}

	}

	private ArrayList<SubJobs> calculating(double possibleStartTime, Parts lowRef) {
		double newEnd=0;
		ArrayList<SubJobs> low= new ArrayList<SubJobs>();
		for(SubJobs j:lowRef.getListSubJobs()) {
			low.add(new SubJobs(j));
		}
		double travelTime=0;
		double serviceTime=0;
		double arrivalTime=possibleStartTime;
		double startServiceTime=0;
		double endServiceTime=0;
		double departureTime=0;

		for(int i=0;i<low.size()-1;i++) {

			travelTime=inp.getCarCost().getCost(low.get(i).getId()-1, low.get(i+1).getId()-1);
			low.get(i).setarrivalTime(possibleStartTime-test.getloadTimeHomeCareStaff());
			low.get(i).setStartServiceTime(possibleStartTime);
			low.get(i).setEndServiceTime(possibleStartTime+low.get(i).getReqTime());
			low.get(i).setdepartureTime(low.get(i).getendServiceTime()+test.getloadTimeHomeCareStaff());
			possibleStartTime=low.get(i).getDepartureTime()+travelTime;
		}
		return low;
	}

	private boolean tryingToInsert(double distanceFromDepot, double distanceToDepot, double distanceConnection,
			SubJobs lastHight, SubJobs firstLow, SubJobs lastLow, Parts high, Parts low) {
		boolean inserted=false;
		if((lastHight.getDepartureTime()+distanceConnection) <=firstLow.getstartServiceTime() && lastHight.getDepartureTime()>firstLow.getArrivalTime()) { // revisar la ventana de tiempo

			inserted=true;
			//	setListSubJobs(ArrayList<SubJobs> listSubJobs, Inputs inp, Test test)
			ArrayList<SubJobs> newListJob= new ArrayList<SubJobs>();
			for(SubJobs p:high.getListSubJobs()) {
				newListJob.add(p);
			}
			for(SubJobs p:low.getListSubJobs()) {
				newListJob.add(p);
			}
			high.getListSubJobs().clear();
			high.setListSubJobs(newListJob, inp, test);
		}
		else {

			inserted=tryingToInsertearly(high,low);
			if(!inserted) {
				inserted=tryingToInsertchangingTime(high,low);}
		}
		return inserted;
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
		for(Route r:routeList) {
			System.out.println(r.toString());
		}
		System.out.println("Stop");
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
				Edge e=new Edge(previous,job,inp,test);
				r.getEdges().put(e.getEdgeKey(), e);

			}
		}
	}

	private void timeStartEndRoutes() {
		// 1. computing times Route
		for(Route r:this.routeList) {
			//1. start time
			SubJobs firstJob=r.getSubJobsList().get(0); // it is not the depot node
			computeStartTimeRoute(firstJob,r);
			//r.getSubJobsList().get(0).setserviceTime(0);
			// end time
			SubJobs lastJob=r.getSubJobsList().get(r.getSubJobsList().size()-1);  // it is not the depot node
			computeEndTimeRoute(lastJob,r);
			//r.getSubJobsList().get(r.getSubJobsList().size()-1).setserviceTime(0);
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
		depot.setserviceTime(0);
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
		depot.setserviceTime(0);
		System.out.println(r.toString());
	}

	private void makeTurnInRoute(Parts turn) { // crea tantas rutas como son posibles
		// la creación de estas rutas lo que hace es identificar las partes de cada turno
		// pero en esencia sólo deberia agregar el deport
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
		partObject.setListSubJobs(partStart,inp,test);
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
					r.updateRouteFromParts(inp,test);
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
		partObject.setListSubJobs(partEnd,inp,test);
		r.getPartsRoute().add(partObject);
		r.updateRouteFromParts(inp,test);
		// setting personnal
		if(turn.getListSubJobs().get(0).isPatient()) {
			r.setAmountDriver(0);
			r.setAmountParamedic(1);
			r.setHomeCareStaff(0);
		}
		else {
			r.setAmountDriver(0);
			r.setAmountParamedic(0);
			r.setHomeCareStaff(1);
		}
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

	private ArrayList<Route> copyListRoute(Solution s) {
		ArrayList<Route> copyrouteList= new ArrayList<Route>();
		for(Route r:s.getRoutes()) {
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
		//r.computePassenger();
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
			System.out.println(" Stop "+ j.getId());

			for(Parts paramedic:qualificationParamedic) {
				System.out.println(" Turn ");
				printing(paramedic);
				if(j.getId()==53 ) {
					if(paramedic.getListSubJobs().size()!=0) {
						if(paramedic.getListSubJobs().get(0).getId()==54) {
							System.out.println(" Stop "+ j.getId());
						}
					}
				}
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
		double distanceFromDepot=inp.getCarCost().getCost(0,primerInRouteSubJob.getId()-1);
		double distancetoDepot=inp.getCarCost().getCost(lastSubJobToInsert.getId()-1, 0);
		double preliminaryWorkingTime=primerInRouteSubJob.getArrivalTime()-lastSubJobToInsert.getDepartureTime()+distanceFromDepot+distancetoDepot;
		if((primerInRouteSubJob.getArrivalTime()-lastSubJobToInsert.getDepartureTime())>0 && preliminaryWorkingTime<test.getWorkingTime()) {
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
			insertingSubJobsIntheSequence(homeCare,dropOffPickUp); // la asignación de la lista de subjobs no necesariamente se hace dentro de un mismo turno
			j.setarrivalTime(j.getstartServiceTime()-test.getloadTimeHomeCareStaff());
		}
		else { // the job j have to be a medical centre
			Parts pickUpDropOff=splitPatientJobInSubJobs(j);
			insertingSubJobsIntheSequence(homeCare,pickUpDropOff); // la asignación de la lista de subjobs no necesariamente se hace dentro de un mismo turno

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
		Couple n=new Couple(pickUpPatientHome, dropOffMedicalCentre);
		coupleList.put(pickUpPatientHome.getSubJobKey(), n);
		coupleList.put(dropOffMedicalCentre.getSubJobKey(), n);
		//dropOffMedicalCentre.setPair(pickUpPatientHome);
		// 3. Generación del pick at medical centre
		SubJobs pickUpMedicalCentre= new SubJobs(j);
		settingTimePickUpPatientParamedicSubJob(pickUpMedicalCentre,dropOffMedicalCentre);

		// 4. Generación del drop-off at client home
		SubJobs dropOffPatientHome= new SubJobs(patient);
		settingTimeDropOffPatientSubJob(dropOffPatientHome,pickUpMedicalCentre);
		n=new Couple(pickUpMedicalCentre, dropOffPatientHome);
		coupleList.put(pickUpMedicalCentre.getSubJobKey(), n);
		coupleList.put(dropOffPatientHome.getSubJobKey(), n);

		//dropOffPatientHome.setPair(pickUpMedicalCentre);
		// 3. Addding the subjobs to the list
		subJobsList.add(pickUpPatientHome); // Se apilan por orden de sequencia
		subJobsList.add(dropOffMedicalCentre);
		subJobsList.add(pickUpMedicalCentre); // Se apilan por orden de sequencia
		subJobsList.add(dropOffPatientHome);
		newPart.setListSubJobs(subJobsList, inp, test);
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
		dropOffPatientHome.setEndTime(arrivalTime);// departure from patient home - el tiempo de viaje - el tiempo necesario para cargar los pacientes al vehículo
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
		// 1. Generación del drop-off job
		SubJobs dropOff= new SubJobs(j);
		// 2. Generación del pick-up job
		SubJobs pickUp= new SubJobs(j.getsubJobPair());
		settingTimeClientSubJob(dropOff,pickUp);
		// 3. Addding the subjobs to the list
		subJobsList.add(dropOff); // Se apilan por orden de sequencia
		subJobsList.add(pickUp);
		Couple n=new Couple(dropOff,pickUp);
		coupleList.put(dropOff.getSubJobKey(), n);
		coupleList.put(pickUp.getSubJobKey(), n);
		//pickUp.setPair(dropOff);
		newParts.setListSubJobs(subJobsList,inp,test);
		return newParts;
	}

	private void settingTimeClientSubJob(SubJobs dropOff, SubJobs pickUp) {
		homeCareDropOff(dropOff);
		homeCarePickUp(dropOff,pickUp);
	}

	private void homeCarePickUp(SubJobs dropOff, SubJobs pickUp) {
		pickUp.setTotalPeople(1);
		pickUp.setClient(true);	
		pickUp.setserviceTime(dropOff.getReqTime()); //los nodos pick up contienen la información de los nodos
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
		//r.computePassenger();
		if(Math.abs(r.getPassengers())+Math.abs(passengerAmount)<=inp.getVehicles().get(0).getMaxCapacity() && r.getTravelTime()<test.getWorkingTime()) {
			capacity =true;
		}
		return capacity;
	}

	private int iterateOverSchift(SubJobs j, Parts homeCare) {
		if(j.getSubJobKey().equals("D54")) {
			System.out.println("Job to insert "+ j.getId()+" "+ j.getSubJobKey()+" "+ j.getstartServiceTime());	
		}
		System.out.println("Job to insert "+ j.getId()+" "+ j.getSubJobKey()+" "+ j.getstartServiceTime());
		boolean inserted=false;
		int position=-1;
		// se evalua inser trabajo por trabajo - Tan pronto sea posible insertar el trabajo se para la iteración sobre el turno y se inserta
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

	private int iterateOverSchiftLastPosition(SubJobs j, Parts homeCare) {
		System.out.println("Job to insert "+ j.getId()+" "+ j.getSubJobKey()+" "+ j.getstartServiceTime());
		boolean inserted=false;
		int position=-1;
		// se evalua inser trabajo por trabajo - Tan pronto sea posible insertar el trabajo se para la iteración sobre el turno y se inserta
		//ArrayList<SubJobs> homeCare=copyListJobs(currentJobs);
		SubJobs inRoute=homeCare.getListSubJobs().get(homeCare.getListSubJobs().size()-1);
		inserted=insertionLater(inRoute,j);//(inRoute)******(j)
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

import java.util.ArrayList;
import java.util.Random;

public class Interaction {

	private final Test test;
	private final Inputs input;
	private DrivingRoutes initialRoutes;
	private DrivingRoutes newRoutes;
	private DrivingRoutes bestRoutes;
	private WalkingRoutes subroutes;

	public Interaction(DrivingRoutes routes, ArrayList<Couple> subJobsList, Inputs i, Random r, Test t) {
		test = t;
		input = i;
		initialRoutes=routes;
		stagesIteration(); // se cambian las rutas caminando lo que quiere decir que se deben cambiar cambiar todas las rutas de conducción
	}

	// Auxiliar methods
	private void stagesIteration() {
		long timer = System.nanoTime(); // timer
		double end=0;
		// iteration to combine stage 1 and stage 2
		// la interacción por etapas se puede dar a través de una busqueda orientada entre rutas o un comportamiento greedy en donde 
		// aleatoriamente se crea un nuevo conjunto de rutas caminando
		while(end<test.getTestTime()) {

			end= (System.nanoTime() - timer) / Math.pow(10, 6);
		}

	}

	// Getters

	public DrivingRoutes getBestRoutes() {
		return bestRoutes;
	}
	
	
	public WalkingRoutes getBestWalkingRoutes() {
		return subroutes;
	}

	// Setters
	public void setBestRoutes(DrivingRoutes bestRoutes) {
		this.bestRoutes = bestRoutes;
	}
	
	
	public void setBestRoutes(WalkingRoutes wr) {
		this.subroutes = wr;
	}
	
	

}

import java.util.Random;



public class Algorithm {
	private final Test test;
	private final Inputs input;
	private final WalkingRoutes subroutes;
	private final DrivingRoutes routes;

	public Algorithm(Test t, Inputs i, Random r) {
		test = t;
		input = i;
		subroutes = new WalkingRoutes(input, r, t, i.getNodes());
		routes = new DrivingRoutes(input, r, t, i.getNodes());
		setSolution(subroutes,routes);
	}

	private void setSolution(WalkingRoutes subroutes2, DrivingRoutes routes2) {
		// TODO Auto-generated method stub
		
	}

	public void solve(String outputsFilePath) {
	}
}

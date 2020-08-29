import java.io.IOException;
import java.io.PrintWriter;

public class Outputs {
	private WalkingRoutes subroutes;
	private DrivingRoutes routes;
	
	
	public Outputs(Algorithm algorithm) {
		subroutes=algorithm.getSubroutes();
		routes=algorithm.getRoutes();
	}
	
	   /* AUXILIARY METHODS */
    public void sendToFile(String outFile, Double endTime)
    {
        try 
        {   PrintWriter out = new PrintWriter(outFile);
            out.println("***************************************************");
            out.println("*                      OUTPUTS                    *");
            out.println("***************************************************");
            out.println("\r\n");
            out.println("--------------------------------------------");
            //out.println("Driving Routes");
            out.println("--------------------------------------------");
          //  out.println(routes.toString() + "\r\n");
            out.println("--------------------------------------------");
            out.println("\r\n Walking Routes:\r\n");
            out.println("--------------------------------------------");
            out.println(subroutes.toString() + "\r\n");
            // job | Arrival time | start time | qualification
            out.println("\r\n pc Time:\r\n");
            // job | Arrival time | start time | qualification
            out.println(endTime);
            out.close();
        } catch (IOException exception) 
        {   System.out.println("Error processing output file: " + exception);
        }
    }

}

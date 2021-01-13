import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedList;

public class Outputs {
	private LinkedList<SubRoute> subroutes;
	private Solution initialSolution;
	private Solution solution;


	public Outputs(Algorithm algorithm) {
		subroutes=algorithm.getSubroutes();
		initialSolution=algorithm.getInitialSolution();
		solution=algorithm.getSolution();
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
		out.println("\r\n Walking Routes:\r\n");
		out.println("--------------------------------------------");
		if(subroutes!=null) {
			out.println(subroutes.toString() + "\r\n");
			out.println("--------------------------------------------");
		}
		else {
			out.println("------------------ NA ---------------------");
			out.println("--------------------------------------------");
		}
			
		out.println("Driving Routes");
		out.println("--------------------------------------------");
		out.println("\n Initial solution \n");
		//out.println(initialSolution.toString() + "\r\n");
		for(Route r:initialSolution.getRoutes()) {
			if(!r.getSubJobsList().isEmpty()) {
				int paramedic=0;
				int homeCareStaff=0;
				if(r.getAmountParamedic()>0) {
					paramedic++;
				out.println("\n Paramedic "+paramedic );
				}
				else {
					homeCareStaff++;
					out.println("\n Home care Staff " + homeCareStaff );}
				}
					for(Parts p:r.getPartsRoute()) {
					for(SubJobs j:p.getListSubJobs()) {
						String type="";
						if(j.isClient()) {
							type="c";
						}
						if(j.isPatient()) {
							type="p";
						}
						out.println(" ( " + j.getSubJobKey()+type+" A  "+(int)j.getArrivalTime()+"  B  "+(int)j.getstartServiceTime()+ " end service "+ (int)j.getendServiceTime()+"   D  "+(int)j.getDepartureTime()+"  reqTime_"+j.getReqTime()+"  TW ["+(int)j.getStartTime()+";"+(int)j.getEndTime()+"]"+") \n");
					}
					//out.println("\n\n");
				}
			}	
		out.println("\n Best solution \n");
		out.println(solution.toString() + "\r\n");


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

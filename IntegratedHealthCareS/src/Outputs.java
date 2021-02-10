import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedList;

public class Outputs {
	private LinkedList<SubRoute> subroutes;
	private Solution initialSolution;
	private Solution solution;


	public Outputs(Test currentTest, Algorithm algorithm) {
		subroutes=algorithm.getSolution().getWalkingRoute();
		initialSolution=algorithm.getInitialSolution();
		solution=algorithm.getSolution();
		printSolST(currentTest,algorithm);
		
	}

	private void printSolST(Test currentTest, Algorithm algorithm) {
		try 
		{   
			///
			String objective="";
			if(currentTest.getdriverObjective()==1 && currentTest.gethomeCareStaffObjective()==0) {
				objective="Driver";
			}
			else {
				if(currentTest.getdriverObjective()==0 && currentTest.gethomeCareStaffObjective()==1) {
					objective="Home_Care_Staff";	
				}
				else { // integrated
					objective="Integrated";	
				}
			}
			
			
			////
			
			PrintWriter out = new PrintWriter(currentTest.getInstanceName()+"_"+objective+"_ResumeSols.txt");
			// 1 objective function + 2 walking time + 3 cost driver + 4 cost home care staff + 5 travel time+ 6 waiting time
			
			out.printf("Iter	OF	   WalkingTime 	  driverCost   HHCcost   travelTime  	waitingTime");
		//7 
			for(int i=0;i<algorithm.getPerformance().length;i++) {
				out.println();
				out.printf("	%.2f",algorithm.getPerformance()[i][0]);
				out.printf("	%.2f",algorithm.getPerformance()[i][1]);
				out.printf("	%.2f", algorithm.getPerformance()[i][2]);
				out.printf("	%.2f",algorithm.getPerformance()[i][3]);
				out.printf("	%.2f",algorithm.getPerformance()[i][4]);
				out.printf("	%.2f", algorithm.getPerformance()[i][5]);
				out.printf("	%.2f",algorithm.getPerformance()[i][6]);
			}
		
			out.close();
		} 
		catch (IOException exception) 
		{   System.out.println("Error processing output file: " + exception);
		}
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
		int paramedic=0;
		int homeCareStaff=0;
		for(Route r:initialSolution.getRoutes()) {
			if(!r.getSubJobsList().isEmpty()) {	
				if(r.getAmountParamedic()>0) {
					paramedic++;
					out.print("\n Paramedic "+paramedic );
					out.print("    Working day: ");
					out.println(r.getDurationRoute() + "\r\n");
				}
				else {
					homeCareStaff++;
					out.print("\n Home care Staff " + homeCareStaff );
					out.print("    Working day: ");
					out.println(r.getDurationRoute() + "\r\n");	
				}
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
					out.println(" ( " + j.getSubJobKey()+type+" A  "+(int)j.getArrivalTime()+"  B  "+(int)j.getstartServiceTime()+ " end service "+ (int)j.getendServiceTime()+"   D  "+(int)j.getDepartureTime()+"  reqTime_"+j.getReqTime()+"  TW ["+(int)j.getSoftStartTime()+";"+(int)j.getSoftStartTime()+"]"+") \n");
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

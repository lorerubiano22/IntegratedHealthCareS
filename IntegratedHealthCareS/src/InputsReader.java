import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;


public class InputsReader {
	private static final String SERVICES_INPUT = "Jobs.txt";
	private static final String CAR_INPUT = "ODcar.txt";
	private static final String WALK_INPUT = "ODwalk.txt";
	private static final String VEHICLES_INPUT = "Vehicles.txt";
	private static final String NURSES_INPUT = "Nurses.txt";
	private static final String PARAMEDIC_INPUT = "Paramedics.txt";
	
    /** Reads inputs (nodes and vehicle types) from txt files */
    public static Inputs readInputs(String source) {
        // CREATES CONTAINER-LISTS FOR NODES AND VEHICLE TYPES
        List<Jobs> nodes = new LinkedList<Jobs>();
        List<AttributeNurse> nurses = new LinkedList<AttributeNurse>();
        List<VehicleBase> vehicles = new LinkedList<VehicleBase>();
        List<AttributeParamedics> paramedic= new LinkedList<AttributeParamedics>();
        TimeMatrix walk = null;
        TimeMatrix car = null;

        try
        {   // CREATES ALL NODES AND FILL THE NODES LIST
            FileReader reader = new FileReader(source+SERVICES_INPUT);
            Scanner in = new Scanner(reader);
            in.useLocale(Locale.US);      
            JobManager nodeFactory = JobManager.getNodeFactory(); // joint the information of the nurse jobs
            while( in.hasNextLine() )
            {   
                String s = in.next();
                if( s.charAt(0) == '#' ) // this is a comment line
                    in.nextLine(); // skip comment lines
                else // number | x-coord | y-coord | demand
                {   
                   // 1. if the TW <> 0 the it is a job (it could be a patient or client job)
                	int startTime = Integer.parseInt(s);
                    int endTime = in.nextInt();
                    int reqQualification = in.nextInt();
                    int reqTime = in.nextInt();
                    Jobs aNode = nodeFactory.getNode(startTime, endTime, reqQualification, reqTime);
                    nodes.add(aNode);
                }
            }
            in.close();
          
            //LOAD ALL COSTS
            reader = new FileReader(source+CAR_INPUT);
            in = new Scanner(reader);
            in.useLocale(Locale.US);
            List<Integer> cList = new LinkedList<Integer>();
            while( in.hasNextLine() )
            {   
                String s = in.next();
               
                if( s.charAt(0) == '#' ) // this is a comment line
                    in.nextLine(); // skip comment lines
                else // nVehInType | vCap | fixCost | varCost | eff | range
                {   
                   int sVal = Integer.parseInt(s);
                   cList.add(sVal);
  
                }
            }
            car = TimeMatrix.getCostMatrix(TimeMatrix.fromList(cList));
            in.close();
            
            reader = new FileReader(source+WALK_INPUT);
            in = new Scanner(reader);
            in.useLocale(Locale.US);
            List<Integer> wList = new LinkedList<Integer>();
            while( in.hasNextLine() )
            {   
                String s = in.next();
               
                if( s.charAt(0) == '#' ) // this is a comment line
                    in.nextLine(); // skip comment lines
                else // nVehInType | vCap | fixCost | varCost | eff | range
                {   
                   int sVal = Integer.parseInt(s);
                   wList.add(sVal);
  
                }
            }
            
            walk = TimeMatrix.getCostMatrix(TimeMatrix.fromList(wList));
            in.close();
            
            reader = new FileReader(source+NURSES_INPUT);
            in = new Scanner(reader);
            in.useLocale(Locale.US);
            int id = 0;
            while( in.hasNextLine() )
            {   
                String s = in.next();
               
                if( s.charAt(0) == '#' ) // this is a comment line
                    in.nextLine(); // skip comment lines
                else // nVehInType | vCap | fixCost | varCost | eff | range
                {   
                   int number = Integer.parseInt(s);
                   int qualif = in.nextInt();
                   nurses.add(new AttributeNurse(id++, number, qualif));
                }
            }
            in.close();
            
            reader = new FileReader(source+VEHICLES_INPUT);
            in = new Scanner(reader);
            in.useLocale(Locale.US);
            id = 0;
            while( in.hasNextLine() )
            {   
                String s = in.next();
               
                if( s.charAt(0) == '#' ) // this is a comment line
                    in.nextLine(); // skip comment lines
                else // nVehInType | vCap | fixCost | varCost | eff | range
                {   
                   int quantity = Integer.parseInt(s);
                   int maxCapacity = in.nextInt();
                   vehicles.add(new VehicleBase(id++, quantity, maxCapacity));
                }
            }
            in.close();
            
            reader = new FileReader(source+PARAMEDIC_INPUT);
            in = new Scanner(reader);
            in.useLocale(Locale.US);
            id = 0;
            while( in.hasNextLine() )
            {   
                String s = in.next();
               
                if( s.charAt(0) == '#' ) // this is a comment line
                    in.nextLine(); // skip comment lines
                else // nVehInType | vCap | fixCost | varCost | eff | range
                {   
                   int quantity = Integer.parseInt(s);
                   int maxCapacity = in.nextInt();
                   paramedic.add(new AttributeParamedics(id++, quantity, maxCapacity));
                }
            }
            in.close();
            
        }
        catch (IOException exception)
        {   
            System.out.println("Error processing inputs files: " + exception);
        }
        
        
        // CREATES INPUTS 
     //   Inputs inputs = new Inputs(nodes, vehTypes);
        return new Inputs(nodes, car, walk, nurses,paramedic, vehicles);
    }
}
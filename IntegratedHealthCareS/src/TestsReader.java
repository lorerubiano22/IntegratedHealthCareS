import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Scanner;

public class TestsReader {
    public static ArrayList<Test> getTestsList(String testsFilePath) {   
        ArrayList<Test> list = new ArrayList<Test>();
        try
        {   
            FileReader reader = new FileReader(testsFilePath);
            Scanner in = new Scanner(reader);
            in.useLocale(Locale.US);
             while( in.hasNextLine() )
            {   
                String s = in.next();
                if (s.charAt(0) == '#') // this is a comment line
                    in.nextLine(); // skip comment lines
                else
                {   
                    String instanceName = s; // e.g.: A-n32-k5
                    int maxTime = in.nextInt();
                    long seed = in.nextLong();
                    double workingTime=in.nextDouble();
                    double walking2jobs=in.nextDouble();
                    double detour=in.nextDouble();
                    double cumulativeWalkingTime=in.nextDouble();
                    double cumulativeWaitingTime=in.nextDouble();
                    Test aTest = new Test(instanceName, maxTime, seed,workingTime, walking2jobs,  detour,  cumulativeWalkingTime, cumulativeWaitingTime);
                    list.add(aTest);
                }
            }
            in.close();
        }
        catch (IOException exception)
        {   
            System.out.println("Error processing tests file: " + exception);
        }
        return list;
    }
}

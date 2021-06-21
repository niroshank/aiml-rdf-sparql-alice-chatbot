package com.company;

import java.io.File;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.alicebot.ab.Bot;
import org.alicebot.ab.Chat;
import org.alicebot.ab.History;
import org.alicebot.ab.MagicBooleans;
import org.alicebot.ab.MagicStrings;
import org.alicebot.ab.utils.IOUtils;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.util.FileManager;
import org.apache.log4j.BasicConfigurator;

public class Chatbot {
    private static final boolean TRACE_MODE = false;
    static String botName = "CoronaBot";
    enum qType {
        whatIs,// both input and output
        whatAre,//only a list of items
    }
    public static void main(String[] args) {
        try {
            //BasicConfigurator.configure();
            String resourcesPath = getResourcesPath();
            System.out.println(resourcesPath);
            MagicBooleans.trace_mode = TRACE_MODE;
            Bot bot = new Bot(botName, resourcesPath);
            Chat chatSession = new Chat(bot);
            bot.brain.nodeStats();
            String textLine = "";
            String rdfResponse="";

            //Creating rdf link
            FileManager.get().addLocatorClassLoader(Main.class.getClassLoader());
            Model model = FileManager.get().loadModel("C:/Users/sinethf/Desktop/MSC BigData SEMESTER 2/Data Mining/CW/Corona_Mistake/Corona_virus_still_not.rdf");

            while(true) {
                System.out.print("Human : ");
                textLine = IOUtils.readInputTextLine();
                if ((textLine == null) || (textLine.length() < 1))
                    textLine = MagicStrings.null_input;
                if (textLine.equals("q")) {
                    System.exit(0);
                } else if (textLine.equals("wq")) {
                    bot.writeQuit();
                    System.exit(0);
                } else {
                    String request = textLine;

                    //if (MagicBooleans.trace_mode)
                    //System.out.println("STATE=" + request + ":THAT=" + ((History) chatSession.thatHistory.get(0)).get(0) + ":TOPIC=" + chatSession.predicates.get("topic"));

                    String response = chatSession.multisentenceRespond(request);
                    List<String> splitWords= Arrays.asList(response.split(","));

                    if(splitWords.contains(qType.whatIs.toString())){
                        rdfResponse = queryOntology(model,qType.whatIs.toString(),splitWords,request);
                    }
                    else if (splitWords.contains(qType.whatAre.toString()))
                    {
                        List<String> splitSingleWords= Arrays.asList(response.split("[\\s,]+"));
                        rdfResponse = queryOntology(model,qType.whatAre.toString(),splitSingleWords,request);
                    }

                    if(rdfResponse==""){
                        while (response.contains("&lt;"))
                            response = response.replace("&lt;", "<");
                        while (response.contains("&gt;"))
                            response = response.replace("&gt;", ">");
                        System.out.println("Robot : " + response);
                    }
                    else{
                        System.out.println("Robot : " + rdfResponse);
                        rdfResponse="";
                    }

                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private static String queryOntology(Model model,String questionType,List<String> keywords,String inputText) {

      //  List<DeseaseKeys> coronaKeyWords= new ArrayList<DeseaseKeys>();

        List<RdfResponse> rdfResponse=new ArrayList<RdfResponse>();
        String queryString="";
        String response="";
        if(questionType==qType.whatIs.toString())
        {
            String name=keywords.get(0);
              queryString ="prefix corona: <http://corona_virus.rdf#>\n" +
                    "SELECT  ?name ?description\n" +
                    "WHERE\n" +
                    "{\n" +
                    " ?covid corona:name"+ "'"+name+"'.\n" +
                    " ?covid corona:name ?name.\n" +
                    " ?covid corona:description ?description.\n" +
                    "}";
        }
        else if(questionType==qType.whatAre.toString()){


            String relationShip= keywords.get(0);
            String name=keywords.get(1);
            String object=keywords.get(2);

            if(keywords.contains("covid") && keywords.contains("covid19") && keywords.contains("coronavirus") && keywords.contains("corona")){
                name="Coronavirus Disease 2019";
            }
            else if(keywords.contains("sars")){
                name="Severe Acute Respiratory Syndrome";
            }
            else if(keywords.contains("mers")){
                name="Middle East Respiratory Syndrome";
            }
            else
            {
                name="Coronavirus Disease 2019";
            }

            queryString ="prefix corona: <http://corona_virus.rdf#>\n" +
                    "SELECT ?name ?description\n" +
                    "WHERE\n" +
                    "{\n" +
                    " ?covid corona:name"+ "'"+name+"'.\n" +
                    " ?covid corona:"+relationShip+" ?"+object+".\n" +
                    " ?"+object+" corona:name ?name.\n" +
                    " ?"+object+" corona:description ?description.\n" +
                    "}";
        }

        Query query = QueryFactory.create(queryString);
        QueryExecution qexec=QueryExecutionFactory.create(query,model);
        try{
                ResultSet results=qexec.execSelect();
            RdfResponse resObj;
                while(results.hasNext()){
                    QuerySolution soln =results.nextSolution();
                    resObj= new RdfResponse();
                    resObj.name=soln.getLiteral("name").toString();
                    resObj.description=soln.getLiteral("description").toString();
                    rdfResponse.add(resObj);
                }
        }finally {

        }
        return formatReponse((ArrayList<RdfResponse>) rdfResponse);
    }
    private static String formatReponse(ArrayList<RdfResponse> response){
        String parsedResponse="";
        try {
                if(response !=null){
                    int length= response.size();
                    for(int i=0;i<response.size();i++) {
                        RdfResponse res=response.get(i);
                        if(res!=null){
                            //format name
                            int nameIndex=res.name.indexOf("^^http://www.w3.org");
                            String name =res.name.substring(0,nameIndex);

                            //format description
                            int  descIndex=res.description.indexOf("^^http://www.w3.org");
                            String desc =res.description.substring(0,descIndex);

                            if(i==response.size()-1){
                                parsedResponse = parsedResponse +name+"-->"+desc;
                            }
                            else{
                                parsedResponse = parsedResponse +name+"-->"+desc+"\n";
                            }

                        }
                    }
                }
        }
        catch (Exception ex){
            return "";
        }
        return  parsedResponse;
    }
    private static String getResourcesPath() {
        File currDir = new File(".");
        String path = currDir.getAbsolutePath();
        path = path.substring(0, path.length() - 2);
        System.out.println(path);
        String resourcesPath = path + File.separator + "src" + File.separator + "main" + File.separator + "resources";
        System.out.println(resourcesPath);
        return resourcesPath;
    }

    public class DeseaseKeys{
        public String key;
        public String value;
    }
}
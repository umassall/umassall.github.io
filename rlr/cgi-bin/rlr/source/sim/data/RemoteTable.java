package sim.data;
import parse.*;
import java.util.*;
import matrix.*;
import Random;
import matrix.*;
import expression.*;
import java.io.*;
import java.net.*;

import javaFTP;  //Paldino's 5 FTP classes
import commandResponse;
import headers;
import timekeeper;
import command;

/** This allows the user to specify a remote data file that contains data records.  This
  * data file is then used to create input/output vectors to be used for training a arbitrary function
  * approximators including neural networks.  The attributes of each record do not have to be scalar numbers.
  * Attributes can be binary, doubles, floats, integers, characters, words, and strings.
  *    <p>This code is (c) 1997 Mance E. Harmon
  *    <<a href=mailto:mharmon@acm.org>mharmon@acm.org</a>>,
  *    <a href=http://eureka1.aa.wpafb.af.mil>http://eureka1.aa.wpafb.af.mil</a><br>
  *    The source and object code may be redistributed freely.
  *    If the code is modified, please state so in the comments.
  * @version 1.0, 15 April 97
  * @author Mance E. Harmon
  */
public class RemoteTable extends Data {
  //These variables are used exclusively with the ftp connection
  String location = null;
  String filename = null;
  String username = null;
  String password = null;

  boolean unparseTable = false;  //Flag that determines how to unparse this object
  boolean download = false; //Download a file?
  boolean saveFile = true;  //Save the file permanently?

  NumExp nullAttribute = new NumExp(0); //default for null ("?") attributes is 0;
  NumExp min = new NumExp(0); //The lower bound for normalization
  NumExp max = new NumExp(1); //The upper bound for normalization

  MatrixD[] inData;  //Array of input vectors
  MatrixD[] outData; //Array of output vectors
  IntExp[] inputAttributes = null; //the vector inputs is copied into this
  IntExp[] outputAttributes = null;//the vector outputs is copies into this


  /** Put the input/output pair into arrays in/out.
    * The arrays must already have been initialized to the right sizes.
    * A randomly-chosen data point will be returned.
    */
  public void getData(double[] in, double[] out, Random rnd) {
    getData(rnd.nextInt(0,nPairs-1),in,out);
  }

  /** Put the nth input/output pair into arrays in/out.
    * The arrays must already have been initialized to the right sizes.
    * An exception is raised if n<0 or n>=nPairs.
    * If number of pairs is infinite, then exception is always thrown.
    * @exception ArrayIndexOutOfBoundsException arrays were too small or there is no "nth" data item
    */
  public void getData(int n,double[] in,double[] out) throws ArrayIndexOutOfBoundsException {
    java.lang.System.arraycopy(inData [n].data,0,in ,0, inData[0].size);
    java.lang.System.arraycopy(outData[n].data,0,out,0,outData[0].size);
  }

  /* Return the BNF description of how to parse the parameters of this object. */
  public String BNF(int lang) {
    return "'{' 'download' <boolean> "+
           "'location' <string> "+
           "'filename' <string> "+
           "'inputs' (IntExp [','])+ "+
           "'outputs' (IntExp [','])+ "+
           "( ('savefile' <boolean>) | "+
           "('username' <string>) | "+
           "('password' <string>) | "+
           "('normalize' IntExp IntExp) | "+
           "('nullAttribute' IntExp) | "+
           "('unparseTable' <boolean>)* "+
           "'}'"+
           "//downloads a remote table of records and converts to input/output vectors for training. "+
           "'download' specifies whether to actually download the file.  This is included because it is possible "+
           "to download once and then use in multiple times by not deleting from the local drive.  'location' is "+
           "the ftp site from which to download the file. 'filename' includes the path on the remote host. 'inputs' "+
           "is a list of integers that represent the attribute fields in the record that are to be used as the inputs "+
           "to the function approximator (Ex. 1,3,7: Data from columns 1, 3 and 7 of the table are to be used as inputs. "+
           "'outputs' - works the same as inputs. 'savefile' tells the system whether or not to save the file for future "+
           "use.  'username' - The username for the ftp account. 'password' - for ftp account. 'normalize' Will normalize "+
           "the data to a range specified by the integers following 'normalize' (Ex. normalize -1 1 normlizes the training "+
           "data to range from [-1,1]. 'nullAttribute' - Specifies the value of null fields in incomplete records. "+
           "If 'unparseTable' is set to true, then the unparse method will create the resulting table from the datafile "+
           "rather than simply returning the standard information.  In this case, the data object name remoteTable should be "+
           "changed to table in the unparsed file, if the user wants to use the unparsed file as the definition of a new "+
           "experiment."+
           "DEFAULTS: download - false; savefile - true; username - anonymous; password - WebSim; normalize - 0 1; "+
           "nullAttribute 0; unparseTable - fales.  All other parameters are required.";
  }

  /** Output a description of this object that can be parsed with parse().
    * @see Parsable
    */
  public void unparse(Unparser u, int lang) {
    if (unparseTable) {
        u.emit("{ ");
        u.indent();
          for (int i=0;i<nPairs;i++) {
            u.emitLine();
            inData[i].transpose();
            u.emitUnparse(inData[i],lang);
            inData[i].transpose();
            u.emit(" ");
            u.emitUnparse(outData[i],lang);
          }
        u.unindent();
        u.emitLine();
        u.emit('}');
    }
    else {
        u.emit("{ ");
        u.indent();
            u.emitLine();
            u.emitLine("download "+download);
            u.emitLine("saveFile "+saveFile);
            u.emitLine("location \'"+location.toString()+"\'");
            u.emitLine("filename \'"+filename.toString()+"\'");
            u.emitLine("username \'"+username.toString()+"\'");
            u.emitLine("password \'"+password.toString()+"\'");
            u.emitLine("nullAttribute "+nullAttribute.val);
            u.emit("inputs " );
                for (int j=0; j<inputAttributes.length; j++)
                  u.emitUnparse(inputAttributes[j],lang);
            u.emitLine();
            u.emit("outputs ");
                for (int j=0; j<outputAttributes.length; j++)
                  u.emitUnparse(outputAttributes[j],lang);
            u.emitLine();
            u.emit("normalize ");
                u.emitUnparse(min,lang);
                u.emitUnparse(max,lang);
            u.emitLine();
        u.unindent();
        u.emit('}');
    }
  }

  /** Parse the input file to get the parameters for this object.
    * @exception parse.ParserException parser didn't find the required token
    */
  public Object parse(Parser p,int lang) throws ParserException {
    Vector attribute = new Vector();  //used to store the attributes for each record when parsing datafile
    Vector inputs = new Vector();     //used to parse which elements of the record will be used as inputs to the net
    Vector outputs = new Vector();    //used to parse which elements of the record will be used as desired outputs
    double[] inputVector = null; //input vector that is constructed from the record.  There will be an array of these.
    double[] outputVector = null; //output vector constructed from record of attributes.  Will be array of these.
    double[] upperBound = null; //the upper bound for each field of the record
    double[] lowerBound = null; //the lower bound for each field of the record, used for normalizing values
    IntExp num;

    //These are used in conjunction with the data file and its parsing
    File dataFile = null;
    FileInputStream data = null; //this is the stream used to actually parse all of the data file
    FileInputStream countData = null; //this stream is used to only count the number of attributes in a record
    StreamTokenizer st = null;
    StreamTokenizer attCounter = null; //used only to count the number of attributes in a record
    int tokenType;
    Hashtable[] stringData = null;
    int[] hashCount = null; //the total number of attributes in a hashtable for each element position
    int recordSize = 0; //the number of attributes in a record

    commandResponse cr = null;

    //PARSE THIS OBJECT'S PARAMETERS
    p.parseChar('{',true);
    password="WebSim";
    username="anonymous";
    while (true) {
        if (p.parseID("download",false))
            download=p.parseBoolean(true);
        else if (p.parseID("saveFile",false))
            saveFile=p.parseBoolean(true);
        else if (p.parseID("location",false))
            location=p.parseString(true);
        else if (p.parseID("filename",false))
            filename=p.parseString(true);
        else if (p.parseID("username",false))
            username=p.parseString(true);
        else if (p.parseID("unparseTable",false))
            unparseTable=p.parseBoolean(true);
        else if (p.parseID("password",false))
            password=p.parseString(true);
        else if (p.parseID("inputs",false)) {
            while (null!=(num=(IntExp)p.parseClass("IntExp",lang,false))) {
              inputs.addElement(num);
              p.parseChar(',',false);
            }
            inputAttributes = new IntExp[inputs.size()];
            inputs.copyInto(inputAttributes);
        }
        else if (p.parseID("outputs",false)) {
            while (null!=(num=(IntExp)p.parseClass("IntExp",lang,false))) {
              outputs.addElement(num);
              p.parseChar(',',false);
            }
            outputAttributes = new IntExp[outputs.size()];
            outputs.copyInto(outputAttributes);
        }
        else if (p.parseID("normalize",false)) {
            min=(NumExp)p.parseClass("NumExp",lang,true);
            max=(NumExp)p.parseClass("NumExp",lang,true);
        }
        else if (p.parseID("nullAttribute",false))
            nullAttribute=(NumExp)p.parseClass("NumExp",lang,true);
        else break;
    }
    p.parseChar('}',true);

//DOWNLOAD (FTP) DATA FILE
    if (download) {
        javaFTP ftp = new javaFTP(location.toString());
        System.out.println("Establishing Connection.");
        cr = ftp.open(location.substring(6).toString());
        System.out.println(cr.getResponseString());
        cr = ftp.user(username.toString());
        System.out.println(cr.getResponseString());
        cr = ftp.pass(password.toString());
        System.out.println(cr.getResponseString());
        cr = ftp.type("ascii");
        System.out.println(cr.getResponseString());
        System.out.println("Receiving data file.");
        cr = ftp.retr(filename.toString(),"data.txt");
        System.out.println("Transmission Complete");
        ftp.closeData();
        ftp.close();
        ftp = null;
    }

//PARSE THE INPUT FILE
    try {
        dataFile = new File("data.txt");
        data = new FileInputStream(dataFile);
        countData = new FileInputStream(dataFile);

        st = new StreamTokenizer(data); //st is used for parsing the entire data file
        st.eolIsSignificant(true);
        st.whitespaceChars(',',',');
        st.quoteChar('\"');

        attCounter = new StreamTokenizer(countData); //attCounter is used only to count the # of elements in the first record
        attCounter.eolIsSignificant(true);
        attCounter.whitespaceChars(',',',');
        attCounter.quoteChar('\"');

        try{
          //COUNT THE NUMBER OF ATTRIBUTES IN A RECORD
    //REWRITE THE CODE THAT COUNTS TO MORE EFFICIENTLY ASSIGN HASHTABLE RESOURCES!!!!
            while ((tokenType=attCounter.nextToken())!=attCounter.TT_EOL) recordSize++;
            hashCount = new int[recordSize];
            stringData = new Hashtable[recordSize];
            for (int j=0; j<recordSize; j++) {
                stringData[j] = new Hashtable(); //create a hashtable for this element of the record
                hashCount[j] = 0;
            }

            //INITIALIZE THE ARRAYS THAT WILL HOLD THE LOWER AND UPPER BOUNDS FOR EACH ATTRIBUTE
            upperBound = new double[recordSize];
            lowerBound = new double[recordSize];

            int elementCounter = 0; //used to parameterize determine which hash table to use (which element of the record
                                    //is being hashed
            boolean firstPass = true;

          //PARSE THE ENTIRE DATA FILE
            while (st.nextToken()!=st.TT_EOF) { //while not at the EOF
                st.pushBack();
                while ((tokenType=st.nextToken())!=st.TT_EOL) { //while token is not EOL
                    if(tokenType==st.TT_EOF) st.pushBack();
                    else if(tokenType==st.TT_NUMBER) {          //if token is a number, convert to double and add to
                        attribute.addElement(new Double(st.nval)); //attribute list
                        if (firstPass) upperBound[elementCounter] = lowerBound[elementCounter] = st.nval;
                        else {
                            if (st.nval>upperBound[elementCounter]) upperBound[elementCounter] = st.nval; //check bounds of value
                            if (st.nval<lowerBound[elementCounter]) lowerBound[elementCounter] = st.nval;
                        }
                    }
                    else if((tokenType==34)||(tokenType==st.TT_WORD)) { //type 34 is a string, TT_WORD covers words not enclosed by quotes
                        Double n = (Double)stringData[elementCounter].get(st.sval); //look to see if string is in hashtable
                        if (n==null) {
                            stringData[elementCounter].put(st.sval, new Double(++hashCount[elementCounter])); //if not in hashtable add to table
                            attribute.addElement(new Double(hashCount[elementCounter])); //convert index to double and add to attribute list
                            if (firstPass) upperBound[elementCounter] = lowerBound[elementCounter] = 1;
                            else {
                                if (hashCount[elementCounter]>upperBound[elementCounter]) upperBound[elementCounter] = hashCount[elementCounter]; //check bounds of value
                                if (hashCount[elementCounter]<lowerBound[elementCounter]) lowerBound[elementCounter] = hashCount[elementCounter];
                            }
                        } else attribute.addElement(n); //else use index from hash and add to attribute list
                    }
                    else if(tokenType==63) { //63 is the int value of '?'
                        Double n = new Double(nullAttribute.val);  //if attribute was '?' convert to nullAttribute.val
                        attribute.addElement(n);                   //and add to list
                    }
                    if(elementCounter==recordSize-1) elementCounter=0; else elementCounter++;
                }
                nPairs++; //increment the number of input/output pairs
                firstPass = false;
            }
            if(!saveFile) dataFile.delete();
        } catch (IOException e2) {System.out.println("Parse Exception in RemoteTable: "+e2);}

        //recordSize = attribute.size()/nPairs;  //calculate the size of a record

    } catch (FileNotFoundException e) {System.out.println("RemoteTable: "+e);}

//MOVE THE ATTRIBUTES INTO ARRAYS OF INPUT AND OUTPUT VECTORS
    inSize = inputAttributes.length;  //inSize and outSize are Class variables and must be set
    outSize = outputAttributes.length;
    inputVector = new double[inSize];
    outputVector = new double[outSize];

    inData =new MatrixD[nPairs]; //the array of input vectors that will be used for training
    outData=new MatrixD[nPairs]; //the array of output vectors that will be used for training

    for (int j=0; j<nPairs; j++) { //move the attributes from the attribute list to the inData and outData array
        for (int k=0; k<inSize; k++) { //the user defines which elements are inputs and which are outputs
            inputVector[k] = ((Double)attribute.elementAt(j*recordSize+inputAttributes[k].val-1)).doubleValue();
            inputVector[k] = ((inputVector[k]-lowerBound[(inputAttributes[k].val-1)])*  //normalize between [0,1]
                             (1/(upperBound[(inputAttributes[k].val-1)]-lowerBound[(inputAttributes[k].val-1)])));
            inputVector[k] = (max.val-min.val)*inputVector[k]+min.val; //normalize between [min,max]
        }

        for (int k=0; k<outSize; k++) {
            outputVector[k] = ((Double)attribute.elementAt(j*recordSize+outputAttributes[k].val-1)).doubleValue();
            outputVector[k] = ((outputVector[k]-lowerBound[(outputAttributes[k].val-1)])* //normalize between [0,1]
                              (1/(upperBound[(outputAttributes[k].val-1)]-lowerBound[(outputAttributes[k].val-1)])));
            outputVector[k] = (max.val-min.val)*outputVector[k]+min.val; //normalize between [min,max]
        }

        try {
            inData [j] = (MatrixD)(new MatrixD((double[])inputVector.clone()));  //copy i/o pairs into perspective arrays
            outData[j] = (MatrixD)(new MatrixD((double[])outputVector.clone()));
            if (false) throw new CloneNotSupportedException(); //needed for 1.1 compilers/**/
        } catch (CloneNotSupportedException e) {System.out.println(e);}
    }
    return this;

  }//end parse
}//end RemoteTable

class FileCopyException extends IOException {
    public FileCopyException(String msg) { super(msg); }
}

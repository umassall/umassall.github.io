import vrml.*;
import vrml.field.*;
import vrml.node.*;
import java.util.*;
import watch.*;
import pointer.*;

/** VRMLInterface lets WebSim control a VRML environment.
  * The VRML datatypes this object can use are
  * SFBool, SFInt32, SFFloat, SFVec3f for both field and eventOut.
  *    <p>This code is (c) 1997 Mance Harmon
  *    <<a href=mailto:mharmon@acm.org>mharmon@acm.org</a>>,
  *    <a href=http://eureka1.aa.wpafb.af.mil>http://eureka1.aa.wpafb.af.mil</a><br>
  *    The source and object code may be redistributed freely.
  *    If the code is modified, please state so in the comments.
  * @version 0.8, 12 May 97
  * @author Mance E. Harmon
  */

public class VRMLInterface extends Script implements Watcher {
  private SFInt32 frequency;          //frequency variable defined in VRML file
  private MFString controlVariables;  //list of variables associated with EventOuts in VRML file
                                      //These should be three tuples of the following form:
                                      //('EventOut' | 'Field') <dataType> <variableName>
  private SFString triggerVariable;   //variable to watch specified in VRML file
  private SFString simDescription;    //the definition of the simulation given in VRML file

  private WatchManager watchManager = null;

  private Field[]         controls          = null;//used to create links to control variables in VRML file
  private StringTokenizer st                = null;
  private String[]        controlVars       = null;//needed for use by StringTokenizer
  private Pointer[]       controlVarPointer = null;//will hold pointers to all variables being watched by this
  private ThreeTuple[]    threeTuples       = null;//this needs to be converted to a hash table /*HASH*/
  private ThreeTuple      a3Tuple           = null;
  private FourTuple[]     fourTuples        = null;//this needs to be converted to a hash table /*HASH*/
  private FourTuple       a4Tuple           = null;

  public void initialize() {
    //get values of variables from VRML file
    frequency = (SFInt32) getField("frequency");
    controlVariables = (MFString) getField("controlVariables");
    triggerVariable  = (SFString) getField("triggerVariable");
    simDescription   = (SFString) getField("simDescription");

    //instantiate a WebSim object
 //   if((simDescription.getValue()!=null) && (!simDescription.getValue().equals("")))
 //     WebSim websim = new WebSim(simDescription.getValue(),10,10,630,400);
 // These lines of code are commented out when using a plug-in (i.e. an html files contains a
 // .wrl file and the simulation description)

    //register watch and watchable variables
    synchronized(WebSim.watchManagerGuard) { //wait for WebSim to initialize, then get its WatchManager
      if (WebSim.watchManager==null)
        try {
          WebSim.watchManagerGuard.wait();
        } catch(InterruptedException e) {
        }
      watchManager=WebSim.watchManager;
    }

    controlVars = new String[controlVariables.getSize()];
    controlVariables.getValue(controlVars); //put the array of MFString controlVariables into String[] controlVars

    controlVarPointer = new Pointer[controlVars.length]; //will hold pointers to watchable variables
    controls = new Field[controlVariables.getSize()]; //create array of fields for use as links to EventOuts
    String token = null; //the token holder for processing the variables and types passed in by MFString controlVariables
    threeTuples = new ThreeTuple[controlVars.length]; //array to hold threeTuples for types like SFVec3f
    fourTuples = new FourTuple[controlVars.length]; //array to hold fourTuples for types like SFRotatoin

    //this loop fills the control array that holds pointers to each of the control variables of various vrml types
    //it also registers each of these variables as a watch, so that the update routine will be called when the value of
    //one of the variables changes.
    for (int j=0; j<controlVars.length; j++) {
      st = new StringTokenizer(controlVars[j]);
      token = getToken(st);
      //Process Fields
      if(token.equalsIgnoreCase("field")) {
        System.out.println("field");
        token=getToken(st);
        //SFInt32, SFBool, SFFloat
        if(token.equals("SFInt32") ||
           token.equals("SFBool")  ||
           token.equals("SFFloat")) {
          System.out.println(token);
          token = getToken(st);
          controls[j] = getField(token);
          controlVarPointer[j] = watchManager.findVar(token.toString());
        } //SFVec3f
        else if(token.equals("SFVec3f")) {
          System.out.println("SFVec3f");
          threeTuples[j] = new ThreeTuple();
          threeTuples[j].type = new String("SFVec3f"); //tell the ThreeTuple what type it is

          token = getToken(st);
          controls[j] = getField(token); //create a pointer to the field in vrml file

          //parse the x coordinate variable
          token = getToken(st);
          if(Character.isLowerCase(token.charAt(0)) ||
             Character.isUpperCase(token.charAt(0))) {
            threeTuples[j].xPointer=true;
            threeTuples[j].xPointerVar = watchManager.findVar(token.toString());
          } else threeTuples[j].x = Float.valueOf(token).floatValue();
          //parse the y coordinate variable
          token = getToken(st);
          if(Character.isLowerCase(token.charAt(0)) ||
             Character.isUpperCase(token.charAt(0))) {
            threeTuples[j].yPointer=true;
            threeTuples[j].yPointerVar = watchManager.findVar(token.toString());
          } else threeTuples[j].y = Float.valueOf(token).floatValue();
          //parse the z coordinate variable
          token = getToken(st);
          if(Character.isLowerCase(token.charAt(0)) ||
             Character.isUpperCase(token.charAt(0))) {
            threeTuples[j].zPointer=true;
            threeTuples[j].zPointerVar = watchManager.findVar(token.toString());
          } else threeTuples[j].z = Float.valueOf(token).floatValue();
        } else System.out.println("Unrecognized VRML type in VRMLInterface"); //this needs to be changed to throwexception
      } //end if token equals field
      //Process eventOuts
      else if(token.equalsIgnoreCase("eventOut")) {
        token=getToken(st);
        //SFInt32, SFBool, SFFloat
        if(token.equals("SFInt32") ||
           token.equals("SFBool")  ||
           token.equals("SFFloat")) {
          System.out.println("SFInt32");
          token = getToken(st);
          controls[j] = getEventOut(token);
          controlVarPointer[j] = watchManager.findVar(token.toString());
        } else
        //SFVec3f
        if(token.equals("SFVec3f")) {
          System.out.println("SFVec3f");
          threeTuples[j] = new ThreeTuple();
          threeTuples[j].type = new String("SFVec3f"); //tell the ThreeTuple what type it is

          token = getToken(st);
          controls[j] = getEventOut(token); //create a pointer to the field in vrml file

          //parse the x coordinate variable
          token = getToken(st);
          if(Character.isLowerCase(token.charAt(0)) ||
            Character.isUpperCase(token.charAt(0))) {
            threeTuples[j].xPointer=true;
            threeTuples[j].xPointerVar = watchManager.findVar(token.toString());
          } else
            threeTuples[j].x = Float.valueOf(token).floatValue();
          //parse the y coordinate variable
          token = getToken(st);
          if(Character.isLowerCase(token.charAt(0)) ||
             Character.isUpperCase(token.charAt(0))) {
            threeTuples[j].yPointer=true;
            threeTuples[j].yPointerVar = watchManager.findVar(token.toString());
          } else
            threeTuples[j].y = Float.valueOf(token).floatValue();
          //parse the z coordinate variable
          token = getToken(st);
          if(Character.isLowerCase(token.charAt(0)) ||
             Character.isUpperCase(token.charAt(0))) {
            threeTuples[j].zPointer=true;
            threeTuples[j].zPointerVar = watchManager.findVar(token.toString());
          } else
            threeTuples[j].z = Float.valueOf(token).floatValue();
        } else
        //SFRotation
        if(token.equalsIgnoreCase("SFRotation")) {
          System.out.println("SFRotation: Starting parsing");
          fourTuples[j] = new FourTuple();
          fourTuples[j].type = new String("SFRotation"); //tell the FourTuple what type it is
          token = getToken(st);
          controls[j] = getEventOut(token); //create a pointer to the field in vrml file
          //parse the x coordinate variable
          token = getToken(st);
          if(Character.isLowerCase(token.charAt(0)) ||
            Character.isUpperCase(token.charAt(0))) { System.out.println("B");
            fourTuples[j].xPointer=true;
            fourTuples[j].xPointerVar = watchManager.findVar(token.toString());
          } else
            fourTuples[j].x = Float.valueOf(token).floatValue();
          //parse the y coordinate variable
          token = getToken(st);
          if(Character.isLowerCase(token.charAt(0)) ||
             Character.isUpperCase(token.charAt(0))) {
            fourTuples[j].yPointer=true;
            fourTuples[j].yPointerVar = watchManager.findVar(token.toString());
          } else
            fourTuples[j].y = Float.valueOf(token).floatValue();
          //parse the z coordinate variable
          token = getToken(st);
          if(Character.isLowerCase(token.charAt(0)) ||
             Character.isUpperCase(token.charAt(0))) {
            fourTuples[j].zPointer=true;
            fourTuples[j].zPointerVar = watchManager.findVar(token.toString());
          } else
            fourTuples[j].z = Float.valueOf(token).floatValue();
          token = getToken(st);
          if(Character.isLowerCase(token.charAt(0)) ||
             Character.isUpperCase(token.charAt(0))) {
            fourTuples[j].rotPointer=true;
            fourTuples[j].rotPointerVar = watchManager.findVar(token.toString());
          } else
            fourTuples[j].rotation = Float.valueOf(token).floatValue();
        } else
          System.out.println("Unrecognized VRML type: VRMLInterface"); //this needs to be changed to throwexception
      }//end if token equals eventOut
    } //end for j
    watchManager.registerWatch(triggerVariable.getValue(),new PInt(frequency.getValue()),this);
    System.out.println("Registered triggerVariable");
  } //end initialization

  synchronized public void update(String changedName, Pointer changedVar, Watchable obj) {
    for(int j=0; j<controls.length; j++) {
      //This code handles all of the VRML datatypes that are scaler
      if(controlVarPointer[j] != null) { System.out.println("processing scalar in update");
        if(controlVarPointer[j] instanceof PInt    ) ((SFInt32)controls[j]).setValue(((PInt)controlVarPointer[j]).val);
        if(controlVarPointer[j] instanceof PDouble ) ((SFFloat)controls[j]).setValue((float)(((PDouble)controlVarPointer[j]).val));
        if(controlVarPointer[j] instanceof PBoolean) ((SFBool )controls[j]).setValue(((PBoolean)controlVarPointer[j]).val);
      }
      else {
              //This code handles all of the VRML datatypes that are 3tuples
              a3Tuple = threeTuples[j];
              if(a3Tuple!=null) {
                float x,y,z;
                //SFVec3f
                if(a3Tuple.type.equalsIgnoreCase("SFVec3f")) {
                  if(a3Tuple.xPointer) {
                    if(a3Tuple.xPointerVar instanceof PInt)
                      x = (((PInt)(a3Tuple.xPointerVar)).val);
                    else
                      x = (float)(((PDouble)(a3Tuple.xPointerVar)).val);
                  } else
                    x = (float)a3Tuple.x;
                  if(a3Tuple.yPointer) {
                    if(a3Tuple.yPointerVar instanceof PInt)
                      y = (((PInt)(a3Tuple.yPointerVar)).val);
                    else
                      y = (float)(((PDouble)(a3Tuple.yPointerVar)).val);
                  } else
                    y = (float)a3Tuple.y;
                  if(a3Tuple.zPointer) {
                    if(a3Tuple.zPointerVar instanceof PInt)
                      z = (((PInt)(a3Tuple.zPointerVar)).val);
                    else
                      z = (float)(((PDouble)(a3Tuple.zPointerVar)).val);
                  } else
                    z = (float)a3Tuple.z;
                  ((SFVec3f)controls[j]).setValue(x,y,z);
                }//end if type is SFVec3f
              }//end if a3Tuple!=null
          else { //a3Tuple=null
          a4Tuple = fourTuples[j];
          if(a4Tuple!=null) {
                float x,y,z,rot;
                //SFRotation
                if(a4Tuple.type.equalsIgnoreCase("SFRotation")) {
                  if(a4Tuple.xPointer) {
                    if(a4Tuple.xPointerVar instanceof PInt)
                      x = (((PInt)(a4Tuple.xPointerVar)).val);
                    else
                      x = (float)(((PDouble)(a4Tuple.xPointerVar)).val);
                  } else
                    x = (float)a4Tuple.x;
                  if(a4Tuple.yPointer) {
                    if(a4Tuple.yPointerVar instanceof PInt)
                      y = (((PInt)(a4Tuple.yPointerVar)).val);
                    else
                      y = (float)(((PDouble)(a4Tuple.yPointerVar)).val);
                  } else
                    y = (float)a4Tuple.y;
                  if(a4Tuple.zPointer) {
                    if(a4Tuple.zPointerVar instanceof PInt)
                      z = (((PInt)(a4Tuple.zPointerVar)).val);
                    else
                      z = (float)(((PDouble)(a4Tuple.zPointerVar)).val);
                  } else
                    z = (float)a4Tuple.z;
                  if(a4Tuple.rotPointer) {
                    if(a4Tuple.rotPointerVar instanceof PInt)
                      rot = (((PInt)(a4Tuple.rotPointerVar)).val);
                    else
                      rot = (float)(((PDouble)(a4Tuple.rotPointerVar)).val);
                  } else
                    rot = (float)a4Tuple.rotation;
                  ((SFRotation)controls[j]).setValue(x,y,z,rot);
                }//end if type is SFRotation
              }//end if fourTuples!=null
          } //end else a3Tuple=null
      } //end else controlVarPointer[j] = null
    }//end for j
  } //end update

  // This method is called when any event is received
  public void processEvent (Event e) {
     // ... perform some operation ...
  }

  synchronized public void unregister(String watchedVar){}

  private String getToken(StringTokenizer st) {
    String token = "";
    String tokenPart = "";

    token=st.nextToken();

    if(token.startsWith("'")) {

      tokenPart=token.substring(1);
      if (tokenPart.endsWith("'")) token = tokenPart.substring(0,tokenPart.length()-1);
      else {
          token = st.nextToken();
          if(token.endsWith("'"))
            tokenPart=tokenPart+" "+ token.substring(0,token.length()-1);
          else
            tokenPart=tokenPart+" "+token;

          token = tokenPart;
      }
    }

    return token;
  }//end getToken
}//end class VRMLInterface

////////////////////////////////////////////////////////
////////////////////////////////////////////////////////
////////////////////////////////////////////////////////

class ThreeTuple {
  public String type = null;

  public boolean xPointer = false;
  public boolean yPointer = false;
  public boolean zPointer = false;

  public Pointer xPointerVar = null;
  public Pointer yPointerVar = null;
  public Pointer zPointerVar = null;

  public float x;
  public float y;
  public float z;
} //end class ThreeTuple

class FourTuple extends ThreeTuple {
  public boolean rotPointer = false;

  public Pointer rotPointerVar = null;

  public float rotation;
} //end class FourTuple
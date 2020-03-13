package sim;
import parse.*;
import watch.*;
import pointer.*;
import expression.*;

/** This object sets up a series of experiments to be run.
  * This object takes as its parameters the name of a variable to
  * change in each experiment, the initial and final values of the
  * variable, the increment, and the experiment to run.
  *    <p>This code is (c) 1996 Mance E. Harmon
  *    <<a href=mailto:harmonme@aa.wpafb.af.mil>harmonme@aa.wpafb.af.mil</a>>,
  *    <a href=http://www.cs.cmu.edu/~baird>http://www.cs.cmu.edu/~baird</a><br>
  *    The source and object code may be redistributed freely.
  *    If the code is modified, please state so in the comments.
  * @version 1.01, 1 July 97
  * @author Mance E. Harmon
  */

public class ForExperiment extends Experiment {
  //Version 1.01 1 July 97: Added code to handle saving a restarting an experiment in the middle of a run. - MH
  //Version 1.00 8 October 96 - Mance E. Harmon


  /** This experiment to be run. */
  protected Experiment experiment;
  /** the final value of the index */
  protected NumExp finalVal=new NumExp(0);
  /** the increment to the variable */
  protected NumExp increment=new NumExp(0);
  /** the initial value of the index */
  protected NumExp initVal=new NumExp(0);
  /** this name of the variable to used as the index */
  protected PString variable=new PString("");
  /** this is used to graph the error vs. index */
  protected Pointer index;
  /** is the experiment being loaded in a save that should be resumed where it left off? */
  private boolean resume=false;

  /** Register all variables with this WatchManager.
    * This will be called after all parsing is done.
    * setWatchManager should be overridden and forced to
    * call the same method on all the other objects in the experiment. */
  public void setWatchManager(WatchManager wm,String name) {
    super.setWatchManager(wm,name);
    experiment.setWatchManager(wm,name+"experiment/");
    wm.registerVar(name+"final value",  finalVal, this);
    wm.registerVar(name+"increment",    increment,this);
    wm.registerVar(name+"initial value",initVal,  this);
    wm.registerVar(name+"variable",     variable, this);
    index=super.watchManager.findVar(variable.val);
    wm.registerVar(name+"index",        index,    this);
  }

  /* Return the BNF description of how to parse the parameters of this object. */
  public String BNF(int lang) {
    return "'{' ('variable' <string> | 'initVal' <double> | "+
           "'finalVal' <double> "+
           "'increment' <double>)* 'experiment' <sim.Experiment> '}'"+
           "//Repeats an Experiment.  On each repetition, the watchable "+
           "variable is set to values from 'initVal' to 'finalVal', "+
           "incremented by 'increment' each time (defaults all 0). This "+
           "can be used, for example, to rerun a neural net experiment "+
           "multiple times with different learning rates each time.  To "+
           "also vary the momentum, there would be a ForExperiment inside "+
           "another ForExperiment (note that a ForExperiment is itself "+
           "an Experiment, and so can be nested).";
  }

  /** Output a description of this object that can be parsed with parse().
    * @see Parsable
    */
  public void unparse(Unparser u, int lang) {
    u.emit("{");
    u.emitLine();
    if (lang<0) {
      u.emit("index ");
      if (index instanceof PInt)
        {u.emit(((PInt)index).val); u.emitLine();}
      else if (index instanceof PDouble)
        {u.emit(((PDouble)index).val); u.emitLine();}
      else if (index instanceof PFloat)
        {u.emit(((PFloat)index).val); u.emitLine();}
    }
    u.emit("initVal ");
      u.emitUnparse(initVal,lang);
      u.emitLine();
    u.emit("finalVal ");
      u.emitUnparse(finalVal,lang);
      u.emitLine();
    u.emit("increment ");
      u.emitUnparse(increment,lang);
      u.emitLine();
    u.emit("variable '");
      u.emit(variable);
      u.emit("'");
      u.emitLine();
    u.emit("experiment ");
      u.emitUnparseWithClassName(experiment,lang,false);
    u.emit("}");
   }

  /** Parse the input file to get the parameters for this object.
    * @exception parse.ParserException parser didn't find the required token
    */
  public Object parse(Parser p,int lang) throws ParserException {
    NumExp ind=new NumExp(0);
    p.parseChar('{',true);
    if (lang<0) {
      p.parseID("index",true);
      ind         =(NumExp)p.parseClass("NumExp",lang,true);
      resume=true; //pick up where you left off when the experiment was saved
    }
    while (true) { //parse whatever parameters are there
      if (p.tID.equals("variable")) {
        p.parseID("variable",true);
        variable.val=p.parseString(true);
      } else if (p.tID.equals("initVal")) {
        p.parseID("initVal",true);
        initVal=(NumExp)p.parseClass("NumExp",lang,true);
      } else if (p.tID.equals("finalVal")) {
        p.parseID("finalVal",true);
        finalVal=(NumExp)p.parseClass("NumExp",lang,true);
      } else if (p.tID.equals("increment")) {
        p.parseID("increment",true);
        increment=(NumExp)p.parseClass("NumExp",lang,true);
      } else if (p.tID.equals("experiment")) {
        p.parseID("experiment",true);
        experiment=(Experiment)p.parseType("sim.Experiment",lang,true);
      } else break;
    }
    p.parseChar('}',true);
    if (lang<0) initVal=ind;
    return this;
  }

  /** This runs the simulation.  The function returns when the simulation
    * is completely done.  As the simulation is running, it should call
    * the watchManager.update() function periodically so all the display
    * windows can be updated.
    */
  public void run(){

    if (index instanceof PInt)
       for(((PInt)index).val=(int)(initVal.val+0.5); ((PInt)index).val<=(int)(finalVal.val+0.5); ((PInt)index).val=((PInt)index).val+(int)(increment.val+0.5))
          experiment.run();
    else if (index instanceof PDouble)
       for(((PDouble)index).val=initVal.val; ((PDouble)index).val<=finalVal.val; ((PDouble)index).val=((PDouble)index).val+increment.val)
          experiment.run();
    else if (index instanceof PFloat)
       for(((PFloat)index).val=(float)(initVal.val); ((PFloat)index).val<=(float)(finalVal.val); ((PFloat)index).val=((PFloat)index).val+(float)(increment.val))
          experiment.run();
  }

  /** Initialize, either partially or completely.
    *@see parse.Parsable#initialize
    */
  public void initialize(int level) {
    super.initialize(level);
    experiment.initialize(level);
  }
}

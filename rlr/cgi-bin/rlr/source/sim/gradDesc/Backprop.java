package sim.gradDesc;
import WebSim;
import matrix.*;
import parse.*;
import watch.*;
import pointer.*;
import expression.*;
import sim.errFun.*;
import Random;

/** Backprop with a fixed learning rate.
  *    <p>This code is (c) 1996 Leemon Baird
  *    <<a href=mailto:leemon@cs.cmu.edu>leemon@cs.cmu.edu</a>>,
  *    <a href=http://www.cs.cmu.edu/~baird>http://www.cs.cmu.edu/~baird</a><br>
  *    The source and object code may be redistributed freely.
  *    If the code is modified, please state so in the comments.
  * @version 1.1, 29 July 97
  * @author Leemon Baird
  */
public class Backprop extends GradDesc {
  //Version 1.1   29 Jul 97 converted to automatic parsing -LCB
  //Version 1.09            Removed avgGrad variable.  It was not being used anywhere. - MH
  //Version 1.08   1 Jul 97 Added resume=false at conclusion of main while loop.  Needed for the case of multiple runs (ForExperiment). -MH
  //Version 1.07  16 Jun 97 Corrected bug in unparse() - MH
  //Version 1.06   4 Jun 97 Added code that initialized the error,smoothederror, and random number generator at beginning of run() -MH
  //Version 1.05   3 Jun 97 Added smoothingFactor as a watchable variable - MH
  //Version 1.04  22 Apr 97 removed epoch/batch code, since there's now a class for that - LCB
  //Version 1.03  15 Apr 97 made minor modifications to unparse - MH
  //Version 1.02            emitLine() added to end of unparse - Mance Harmon
  //Version 1.01            momentum added - Leemon Baird

  private ErrFun[] errFun          ={null};             //the error function to minimize
  private NumExp   rate            =new NumExp(.001);   //the learning rate, a small positive number
  private NumExp   momentum        =new NumExp(0);      //the momentum (near 1=much momentum)
  private NumExp   error           =new NumExp(1);      //a noisy estimate of the error being gradient descended on
  private NumExp   smoothedError   =new NumExp(1);      //an exponentially smoothed estimate of the error
  private NumExp   smoothingFactor =new NumExp(.999);   //the constant used to smooth the error (near 1 = long halflife)
  private NumExp   tolerance       =new NumExp(0);      //stop learning when smoothed error < tolerance
  private NumExp   logSmoothedError=new NumExp(1);      //log base 10 of the smoothed error
  private IntExp   time            =new IntExp(0);      //current time (time increments once per weight change
  private NumExp   minWeight       =new NumExp(-.1);    //range for initial random weights
  private NumExp   maxWeight       =new NumExp(.1);     //range for initial random weights
  private PBoolean resume/**/      =new PBoolean(false);//is the experiment being loaded in a save that should be resumed where it left off?
  private PMatrixD startWeights    =new PMatrixD(null); //the weights loaded in from the BNF code as the starting weights
  private PMatrixD weights         =new PMatrixD(null); //the weights for the function approximator

  private Object[][] parameters=
    {{"backprop with momentum."},
     {"learningRate", rate,           "learning rate, 0=don't learn, 1000=change weights fast",
      "momentum",     momentum,       "0=no momentum, .9999=lots of momentum",
      "smooth",       smoothingFactor,"exponential smoothing factor for 'avg error', 0=raw error, no smoothing. .9999=very smooth, averaged error",
      "tolerance",    tolerance,      "learning stops when smoothed error below this",
      "minInitWeight",minWeight,      "min value for random initial weights",
      "maxInitWeight",maxWeight,      "max value for random initial weights",
      "error",        errFun,         "the error function to be minimized"},
     {time,error,smoothedError,startWeights,resume}};   //variables saved with saved state

  /** Return a parameter array if BNF(), parse(), and unparse() are to be automated, null otherwise.
    * @see parse.Parsable#getParameters
    */
  public Object[][] getParameters(int lang) {
    return parameters;
  }

  /** Register all variables with this WatchManager.
    * This will be called after all parsing is done.
    */
  public void setWatchManager(WatchManager wm,String name) {
    wm.registerVar(name+"time",         time,            this);
    wm.registerVar(name+"error",        error,           this);
    wm.registerVar(name+"avg error",    smoothedError,   this);
    wm.registerVar(name+"log error",    logSmoothedError,this);
    wm.registerVar(name+"seed",         seed,            this);
    super.setWatchManager(wm,name);
    errFun[0].setWatchManager(wm,name+"err/");
  }

  /** repeatedly change x until f(x) reaches a local minimum */
  public void run() {
        weights.val =errFun[0].getInput();
    MatrixD gradient=errFun[0].getGradient();
    MatrixD dw      =(gradient.duplicate()).mult(0);
    double sf; //smoothing factor to use on this iteration, =min(1-1/time, smoothingFactor)
    try {
      if (resume.val) //starting this experiment from where it left off
        weights.val.replace(startWeights.val);
      else { //starting this experiment from scratch
        startWeights.val=weights.val;  //ensure current weights are saved when saving the state
        resume.val=true;
        error.val=1; //initialize the error
        smoothedError.val=1; //initialize the smoothed error
        time.val=0;
        logSmoothedError.val=1;
        dw.mult(0); //erase the trace for momentum.  This has to be done in case we are in a ForExperiment loop.
        rnd=new Random(seed.val); //initialize the random number generator
        weights.val.setRandom(minWeight.val,maxWeight.val,rnd);
      }

      while (!(smoothedError.val<=tolerance.val)) { //the main loop of the experiment
        if (lastRestartNumber!=WebSim.restartNumber) { //if its time to restart the experiment/**/
          lastRestartNumber=WebSim.restartNumber;
          time.val=0;
          error.val=1;
          smoothedError.val=1;
          logSmoothedError.val=1;
          rnd=new Random(seed.val);
          weights.val.setRandom(minWeight.val,maxWeight.val,rnd);
          dw.mult(0); //erase the trace for momentum
        }

        time.val++;

        error.val=errFun[0].evaluate(rnd,true,false,false);
        errFun[0].findGradient();

        sf=(smoothingFactor.val < 1-1./time.val) ? smoothingFactor.val : 1-1./time.val;
        smoothedError.val=sf*smoothedError.val + (1-sf)*error.val;
        if (smoothedError.val<=0)
          logSmoothedError.val=0;
        else
          logSmoothedError.val=(double)Math.log(smoothedError.val) /
                               (double)Math.log(10);

        gradient.mult(-rate.val);
        if (momentum.val==0) {
          weights.val.add(gradient);
        } else { //use momentum
          dw.mult(momentum.val);
          dw.add(gradient);  // dw = momentum * dw - rate * gradient
          weights.val.add(dw);   // w = w + dw
        }
        watchManager.update();
      }//end while

      resume.val=false;  //important when this object is imbedded in a ForExperiment experiment.  This gaurantees that
                         //all subsequent experiments start from scratch.
    } catch (MatrixException e) {
      e.print();
    }
  }//end run

  /** Initialize, either partially or completely.
    *@see parse.Parsable#initialize
    */
  public void initialize(int level) {
    super.initialize(level);
    errFun[0].initialize(level);
  }
} //end class Backprop

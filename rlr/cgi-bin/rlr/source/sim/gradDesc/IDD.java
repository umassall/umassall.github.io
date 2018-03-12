package sim.gradDesc;
import WebSim;
import matrix.*;
import parse.*;
import watch.*;
import pointer.*;
import expression.*;
import sim.errFun.*;
import Random;

/** Incremental Delta Delta (reference "Multi-Agent Residual Advantage Learning With
  * General Function Approximation" on publication list at http://www-anw.cs.umass.edu/~mharmon).
  *    <p>This code is (c) 1997 Mance Harmon
  *    <<a href=mailto:mharmon@acm.org>mharmon@acm.org</a>>,
  *    <a href=http://www-anw.cs.umass.edu/~mharmon>http://www-anw.cs.umass.edu/~mharmon</a><br>
  *    The source and object code may be redistributed freely.
  *    If the code is modified, please state so in the comments.
  * @version 0.9, 22 July 97   This code will become 1.0 when autoparse is done.
  * @author Mance Harmon
  */
public class IDD extends GradDesc {

  /** the error function to minimize */
  protected ErrFun errFun=null;
  /** the learning rate, a small positive number */
  protected NumExp theta=new NumExp(0);
  /** the learning rates, alpha = e^beta(i) */
  protected MatrixD alphas=null;
  /** a pointer to alphas */
  protected PMatrixD pAlphas=null;
  /** the beta matrix */
  protected MatrixD betas=null;
  /** a pointer to the betas */
  protected PMatrixD pBetas=null;
  /** the change in the weights at time t */
  protected MatrixD weightChange1=null;
  /** the change in the weights at time t+1 */
  protected MatrixD weightChange2=null;
  /** a noisy estimate of the error being gradient descended on */
  protected PDouble error=new PDouble(1);
  /** an exponentially smoothed estimate of the error */
  protected PDouble smoothedError=new PDouble(1);
  /** the constant used to smooth the error (near 1 = long halflife)*/
  protected NumExp smoothingFactor=new NumExp(1);
  /** stop learning when smoothed error < tolerance */
  protected NumExp tolerance=new NumExp(1);
  /** log base 10 of the smoothed error */
  protected PDouble logSmoothedError=new PDouble(1);
  /** current time (time increments once per weight change */
  protected PInt time=new PInt(0);
  /** range for initial random weights */
  private NumExp minWeight=new NumExp(-.1),maxWeight=new NumExp(.1);
  /** is the experiment being loaded in a save that should be resumed where it left off? */
  private boolean resume=false;
  /** the weights for the function approximator */
  private MatrixD weights;
  /** the weights loaded in from the BNF code as the starting weights */
  private MatrixD startWeights;
  /** a copy of the generator passed to evaluate().  Is used to generate the same inputs when evaluate is
    * called a second time. */
  protected Random rndCopy=new Random(0);



  /** Register all variables with this WatchManager.
    * This will be called after all parsing is done.
    */
  public void setWatchManager(WatchManager wm,String name) {
    super.setWatchManager(wm,name);
    wm.registerVar(name+"time",         time,            this);
    wm.registerVar(name+"theta",        theta,            this);
    wm.registerVar(name+"tolerance",    tolerance,       this);
    wm.registerVar(name+"error",        error,           this);
    wm.registerVar(name+"avg error",    smoothedError,   this);
    wm.registerVar(name+"log error",    logSmoothedError,this);
    wm.registerVar(name+"avg const",    smoothingFactor, this);
    wm.registerVar(name+"seed",         seed,            this);
    wm.registerVar(name+"alphas",       pAlphas,         this);
    wm.registerVar(name+"betas",        pBetas,         this);
    errFun.setWatchManager(wm,name+"err/");
  }

  /** repeatedly change x until f(x) reaches a local minimum */
  public void run() {
    double sf; //smoothing factor to use on this iteration, =min(1-1/time, smoothingFactor)
    MatrixD gradient=errFun.getGradient();

    MatrixD weightChange1=new MatrixD(weights.size);
    MatrixD weightChange2=new MatrixD(weights.size);
    double betaChange=0;
    double maxBeta=-0.5;
    double minBeta=-30;

    try {

      if (resume) //starting this experiment from where it left off
        weights.replace(startWeights);
      else { //starting this experiment from scratch
        error.val=1; //initialize the error
        smoothedError.val=1; //initialize the smoothed error
        time.val=0; //initialize the time
        logSmoothedError.val=1; //initilize the log of smoothed error
        rnd=new Random(seed.val); //initialize the random number generator
        weights.setRandom(minWeight.val,maxWeight.val,rnd);
        for(int j=0; j<weights.size; j++) {
          betas.set(j,-1); //initilize the betas
          alphas.set(j,-Math.exp(betas.val(j))); //initialize the alphas
        }
      }

      while (!(smoothedError.val<=tolerance.val)) { //the main loop of the experiment
        if (lastRestartNumber!=WebSim.restartNumber) { //if its time to restart the experiment/**/
          lastRestartNumber=WebSim.restartNumber;
          time.val=0;
          error.val=1;
          smoothedError.val=1;
          logSmoothedError.val=1;
          rnd=new Random(seed.val);
          weights.setRandom(minWeight.val,maxWeight.val,rnd);
          for(int j=0; j<weights.size; j++) {
            betas.set(j,-1); //initialize the betas
            alphas.set(j,-Math.exp(betas.val(j))); //initialize the alphas
          }
        }

        time.val++;
        rnd.copyInto(rndCopy); //make a copy of the state of the random number generator
//System.out.println("1: ");
        error.val=errFun.evaluate(rnd,true,false,true);  //calculate the error at time t
        errFun.findGradient();  //calculate the derivative
        gradient.multEl(alphas); //calculate the weight change at t
        weightChange1.replace(gradient); //make a copy of the first weight change
        weights.add(gradient); //update the weights
//System.out.println("2: ");

        errFun.evaluate(rndCopy,true,false,false); //calculate the error at time t+1
        errFun.findGradient();  //calculate the derivative at time t+1
        gradient.multEl(alphas); //calculate the weight change at t+1
        weightChange2.replace(gradient); //make a copy of the second weight change

        for(int j=0; j<weights.size; j++) {//update betas and alphas
            betaChange=theta.val*(weightChange2.val(j)/alphas.val(j))*weightChange1.val(j);
            if(betaChange>2) betaChange=2;  //make sure that we don't take too large of steps
            if(betaChange<-2) betaChange=-2;
            betas.set(j,betas.val(j)+betaChange);
            if(betas.val(j)>maxBeta) betas.set(j,maxBeta); //bound the beta values
            if(betas.val(j)<minBeta) betas.set(j,minBeta);
            alphas.set(j,-Math.exp(betas.val(j)));
        }

        sf=(smoothingFactor.val < 1-1./time.val) ? smoothingFactor.val : 1-1./time.val;
        smoothedError.val=sf*smoothedError.val + (1-sf)*error.val;
        if (smoothedError.val<=0)
          logSmoothedError.val=0;
        else
          logSmoothedError.val=(double)Math.log(smoothedError.val) /
                               (double)Math.log(10);
        watchManager.update();
      }//end while

      resume=false;  //important when this object is imbedded in a ForExperiment experiment.  This gaurantees that
                     //all subsequent experiments start from scratch.
    } catch (MatrixException e) {
      e.print();
    }
  }//end run
/*  This will be used when autoparsing is completed.
  private Object[][] parameters=
    {{"Incremental Delta Delta. An approximate second order method that minimizes error in ErrFun.  "+
      "Each funApp parameter has an associated weight.  IDD does gradient descent in the learning rate space as"+
      " well as doing gradient descent on the ErrFun."},
     {"theta",     theta, "The meta learning rate that determines the step size in learning rate space.",
      "smooth",    smoothingFactor,  "Determines how fast the smoothed error reacts.",
      "tolerance", tolerance, "Learning stops when the smoothed error drops below tolerance.",
      "error",     errFun,   "The ErrFun on which to do gradient descent.",
      "minInitWeight", minWeight, "The minimum value possible when initializing the weights.",
      "maxInitWeight", maxWeight, "The maximum value possible when initializing the weights."},
     {time,error,smoothedError,weights,betas}}; */

  /** Return a parameter array if BNF(), parse(), and unparse() are to be automated, null otherwise.
    * @see parse.Parsable#getParameters

  public Object[][] getParameters(int lang) {
    return parameters;
  } */


  /* Return the BNF description of how to parse the parameters of this object. */
  public String BNF(int lang) {
    return "'{' "+(lang<0 ? "IntExp , IntExp , IntExp MatrixD " : "") +
           "('theta' NumExp | "+
           "'smooth' NumExp | 'tolerance' NumExp | "+
           "'initWeights' NumExp [','] NumExp)* 'error' <sim.errFun.ErrFun>'}'"+
           "//Incremental Delta Delta. An approximate second order method that minimizes error in ErrFun.  "+
           "Each funApp parameter has an associated weight.  IDD does gradient descent in the learning rate space as"+
           " well as doing gradient descent on the ErrFun.  Learning stops when the "+
           "smoothed error drops below tolerance.  The smoothing "+
           "factor determines how fast the smoothed error reacts."+
           "The two numbers after 'initWeights' are the min and max "+
           "respectively for the initial random weights." +
           (lang>0 ? "" : "The first 4 items after the '{' are the "+
                          "time, error, smoothed error, and weights.");
  }

  /** Output a description of this object that can be parsed with parse().
    * @see Parsable
    */
  public void unparse(Unparser u, int lang) {
    u.emit(" { ");
    u.indent();
    u.emitLine();
    if (lang<0) {
      u.emit(time.val);           u.emit(", ");
      u.emit(error.val);          u.emit(", ");
      u.emit(smoothedError.val);  u.emit(" ");
      u.emitUnparse(weights,lang);    u.emitLine();
    }
    u.emit("theta ");
    u.emitUnparse(theta,lang);
    u.emit("smooth ");
    u.emitUnparse(smoothingFactor,lang);
    u.emit("tolerance ");
    u.emitUnparse(tolerance,lang);
    u.emit("initWeights ");
    u.emitUnparse(minWeight,lang);
    u.emit(", ");
    u.emitUnparse(maxWeight,lang);
    u.emitLine();
    u.emit("error ");
    u.emitUnparseWithClassName(errFun,lang,false);
    u.emitLine();
    u.emit("}");
    u.emitLine();
  }

  /** Parse the input file to get the parameters for this object.
    * @exception parse.ParserException parser didn't find the required token
    */
  public Object parse(Parser p,int lang) throws ParserException {
    theta.val           =(double).001; //default learning rate
    tolerance.val      =(double)0;    //default: learn forever, never stop
    smoothingFactor.val=(double).999; //default: 700-step halflife on smoothing
    p.parseChar('{',true);
    if (lang<0) {
      time.val         =((IntExp)p.parseClass("IntExp",lang,true)).val;
      p.parseChar(',',false);
      error.val        =((NumExp)p.parseClass("NumExp",lang,true)).val;
      p.parseChar(',',false);
      smoothedError.val=((NumExp)p.parseClass("NumExp",lang,true)).val;
      startWeights     =(MatrixD)p.parseClass("MatrixD",lang,true);
      logSmoothedError.val=(double)Math.log(smoothedError.val) /
                           (double)Math.log(10);
      resume=true; //pick up where you left off when the experiment was saved
    }
    while (true) { //parse whatever parameters are there
      if (p.tID.equals("theta")) {
        p.parseID("theta",true);
        theta=(NumExp)p.parseClass("NumExp",lang,true);
      } else if (p.tID.equals("tolerance")) {
        p.parseID("tolerance",true);
        tolerance=(NumExp)p.parseClass("NumExp",lang,true);
      } else if (p.tID.equals("smooth")) {
        p.parseID("smooth",true);
        smoothingFactor=(NumExp)p.parseClass("NumExp",lang,true);
      } else if (p.tID.equals("initWeights")) {
        p.parseID("initWeights",true);
        minWeight=(NumExp)p.parseClass("NumExp",lang,true);
        p.parseChar(',',false);
        maxWeight=(NumExp)p.parseClass("NumExp",lang,true);
      } else
        break;
    }
    p.parseID("error",true);
    errFun=(ErrFun)p.parseType("sim.errFun.ErrFun",lang,true);
    p.parseChar('}',true);

    weights =errFun.getInput();
    alphas=new MatrixD(weights.size);
    pAlphas=new PMatrixD(alphas);
    betas=new MatrixD(weights.size);
    pBetas=new PMatrixD(betas);

    return this;
  }

  /** Initialize, either partially or completely.
    *@see parse.Parsable#initialize
    */
  public void initialize(int level) {
    super.initialize(level);
    errFun.initialize(level);
  }
} //end class IDD

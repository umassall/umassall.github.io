package sim.funApp;
import parse.*;
import matrix.*;
import watch.*;
import pointer.*;
import sim.mdp.*;
import expression.*;
import Random;

/** This funApp is used in conjunction with the Graph3D display object to observe the
  * policy and value function associated with a learning algorithm and MDP.
  * This does not implement findGradient or findHessian.
  *    <p>This code is (c) 1996 Mance Harmon
  *    <<a href=mailto:harmonme@aa.wpafb.af.mil>harmonme@aa.wpafb.af.mil</a>>,
  *    <a href=http://www.cs.cmu.edu/~baird>http://www.cs.cmu.edu/~baird</a><br>
  *    The source and object code may be redistributed freely.
  *    If the code is modified, please state so in the comments.
  * @version 1.11, 17 June 97
  * @author Mance Harmon
  */
public  class ValuePolicy extends FunApp {
//Version 1.11 17 June 97: changed the call to findValue() to reflect the new method definition in MDP

//Version 1.10 3 June 97: Lots o' changes.  Changed parse routine to work with Graph3D interface.  This required
//changes throughout the code.

//Version 1.01 13 November 96: Added the capability to use this object with value iteration and
//TD(lambda).  Any RL algorithm that utilizes a function of states and not state/action pairs.

//Version 1.0 6 October 96

  /** the random number generator */
  protected Random random=new Random(0);
  /** the action vector that is optimal in a given state */
  protected MatrixD optAction=null;
  /** the action vector that is passed to findValAct().  This vector points to the location of the
    * input vector to the function approximator that changed the action.
    */
  protected MatrixD action=null;
  /** A flag stating whether or not we know for certain the value of a state. */
  protected PBoolean valueKnown=new PBoolean(false); /**/ //delete this
  /** the mdp to control */
  protected MDP[] mdp={null};
  /** Function approximator to plot (a duplicate of the original)*/
  protected FunApp function=null;
  /** The origianl function approximator whose duplicate will be plotted */
  protected FunApp[] origFunction={null};
  /** the value of the state passed to the MDP */
  protected MatrixD value=new MatrixD(1);
  /** A flag switching from state/action pairs to states only (as in value iteration). */
  protected PBoolean statesOnly=new PBoolean(false);

  private PDouble r=new PDouble(1);
  private PBoolean v=new PBoolean(false);
  private NumExp dt=new NumExp(0.1);
  private NumExp gamma=new NumExp(0);

  private Object[][] parameters=
    {{"A function that takes a state and returns the value of the state with the optimal action."+
      "This is generally used with Graph3D.  When used as the FunApp this object can be used to "+
      "display the policy and the value function learned by a machine learning algorithm. "},
     {"statesOnly",statesOnly,   "",
      "dt",        dt,           "",
      "gamma",     gamma,        "",
      "mdp",       mdp,          "",
      "funApp",    origFunction, ""},
     {}};

  /** Return a parameter array if BNF(), parse(), and unparse() are to be automated, null otherwise.
    * @see parse.Parsable#getParameters
    */
  public Object[][] getParameters(int lang) {
    return parameters;
  }

  /** Define the MatrixD objects that will be used by evaluate(), findGradients(),
    * and findHessian().  First 6 should be column vectors (n by 1 matrices).
    * The last 3 parameters can be null if the Hessian is never to be calculated.
    * If a function approximator overrides this, it should first call
    * super.setIO() for important housekeeping.
    * @exception MatrixException if inputs are vectors with nonmatching sizes
    */
  public void setIO(MatrixD inVect, MatrixD outVect, MatrixD weights,
                    MatrixD dEdIn,  MatrixD dEdOut,  MatrixD dEdWeights,
                    MatrixD dEdIndIn,  MatrixD dEdOutdOut,  MatrixD dEdWeightsdWeights)
                 throws MatrixException {
    super.setIO(inVect,outVect,weights,dEdIn,dEdOut,dEdWeights,dEdIndIn,dEdOutdOut,dEdWeightsdWeights);
    pInput.val=inVect.submatrix(1,mdp[0].stateSize(),1);
    function.setIO(inVect,value,pWeights.val,null,null,null,null,null,null);
    optAction=pOutput.val.submatrix(1,mdp[0].actionSize(),1);
    if(statesOnly.val)
      action=new MatrixD(mdp[0].actionSize());
    else
      action=inVect.submatrix(1+mdp[0].stateSize(),mdp[0].actionSize(),1);
  }


  /** calculate the output for the given input */
  public void evaluate(){
    try{

    if(statesOnly.val) pOutput.val.set(0,mdp[0].findValue(pInput.val,action,gamma,function,dt,value,r,v,null,random));
    else pOutput.val.set(0,mdp[0].findValAct(pInput.val,action,function,value,valueKnown));
    optAction.replace(action);

    } catch (MatrixException e) {
      e.print();
    }
  }

  /** Calculate the output and gradient for a given input.
    * This does everything evaluate() does, plus it calculates
    * the gradient of the error with respect to the inputs and
    * weights, dEdx and dEdw,
    */
  public void findGradients(){}

  /** Calculate the output, gradient, and Hessian for a given input.
    * This does everything evaluate() and findGradients() do, plus
    * it calculates the Hessian of the error with resepect to the
    * the weights and inputs, dEdxdx, dEdwdx, and dEdwdw.
    */
  public void findHessian(){}

  /** Return # weights needed for nIn inputs (including the first      //this routine is never called, should throw null pointer exception
    * one which is always 1.0), and nOut outputs.
    */
  public int nWeights(int nIn,int nOut){return function.nWeights(nIn,nOut);}

  /* Return the BNF description of how to parse the parameters of this object. */
    public String BNF(int lang){
    return "'{' ('statesOnly' <boolean> | 'dt' NumExp | "+
           "'gamma' NumExp | 'mdp' <sim.mdp.MDP> | 'funApp' <sim.funApp.FunApp>)* '}'"+
           "//A function that takes a state and returns the value of the state with the optimal action."+
           "This is generally used with Graph3D.  When used as the FunApp this object can be used to "+
           "display the policy and the value function learned by a machine learning algorithm. "+
           "The keyword 'statesOnly' refers to finding values of states (as opposed to Q-values). "+
           "For example, if the learning algorithm is Value Iteration or TD(lambda) then 'statesOnly' "+
           "should be true.  If statesOnly=true, then gamma and dt should be specified as well. "+
           "DEFAULTS: gamma=0.9, dt=0.1, statesOnly=false";
  }

  /** Output a description of this object that can be parsed with parse().
    * @see Parsable
    */
  public void unparse(Unparser u, int lang) {
    u.emit("{");
    u.indent();
      u.emitLine();
      u.emit("mdp[0] ");
        u.emitUnparseWithClassName(mdp[0],lang,false);
      u.emit("funApp ");
        u.emitUnparseWithClassName(origFunction[0],lang,false);
      if(statesOnly.val){
        u.emit("statesOnly true");
        u.indent();
        u.emitLine();
        u.emit("dt "+dt.val);
        u.emitLine();
        u.emit("gamma "+gamma.val);
        u.unindent();}
    u.emitLine();
    u.emit("} ");
    u.unindent();
  }

  /** Parse the input file to get the parameters for this object.
    * @exception parse.ParserException parser didn't find the required token
    */
  public Object parse(Parser p,int lang) throws ParserException {
    p.parseChar('{',true);
    while (true) { //parse whatever parameters are there
      if (p.parseID("mdp",false))
        mdp[0]=(MDP)p.parseType("sim.mdp.MDP",lang,true);
      else if (p.parseID("funApp",false)) {
        origFunction[0]=(FunApp)p.parseType("sim.funApp.FunApp",lang,true);
        function=(FunApp)origFunction[0].clone(); }
      else if (p.parseID("statesOnly",false))
        statesOnly=new PBoolean(p.parseBoolean(false));
      else if (p.parseID("dt",false))
        dt=(NumExp)p.parseClass("NumExp",lang,false);
      else if (p.parseID("gamma",false))
        gamma=(NumExp)p.parseClass("NumExp",lang,false);
      else break;
    }
    p.parseChar('}',true);

    try {
      inVect=(MatrixD)origFunction[0].inVect.clone(); //the entire input vector to the net
      outVect=(MatrixD)origFunction[0].outVect.clone();
      pInput.val=inVect.submatrix(1,mdp[0].stateSize(),1);  //this assumes that the first element of the inVect is a bias
      pWeights.val = (MatrixD)origFunction[0].getWeights().clone();
      function.setIO(inVect,value,pWeights.val,null,null,null,null,null,null);
      pOutput.val=new MatrixD(mdp[0].actionSize()+1);
      optAction=pOutput.val.submatrix(1,mdp[0].actionSize(),1);
      if(statesOnly.val)
        action=new MatrixD(mdp[0].actionSize());
      else
        action=inVect.submatrix(1+mdp[0].stateSize(),mdp[0].actionSize(),1); //this assumes that the input vector is a state, action, and bias
    } catch (MatrixException e) {
        System.out.println(e);
    }
    return this;
  }//end parse


  /** Register all variables with this WatchManager.
    * This will be called after all parsing is done.
    * setWatchManager should be overridden and forced to
    * call the same method on all the other objects in the experiment.
    */
  public void setWatchManager(WatchManager wm,String name) {
    super.setWatchManager(wm,name);
    function.setWatchManager(wm,name+"function/");
    mdp[0].setWatchManager(wm,name+"mdp/");
    wm.registerVar(name+"dt",         dt,         this);
    wm.registerVar(name+"gamma",      gamma,      this);
    wm.registerVar(name+"statesOnly", statesOnly, this);
  }

  /** Make an exact duplicate of this class.  For objects it contains, it
    * only duplicates the pointers, not the objects they point to.  For a
    * new FunApp called MyFunApp, the code in this method should be the
    * single line: return cloneVars(new MyFunApp());
    */
  public Object clone() {
    return cloneVars(new ValuePolicy());
  }

  /** After making a copy of self during a clone(), call cloneVars() to
    * copy variables into the copy, then return super.cloneVars(copy).
    * The variables copied are just those set in parse() and
    * setWatchManager().  The caller will be required to call
    * setIO to set up the rest of the variables.
    */
  public Object cloneVars(FunApp copy) {
    ValuePolicy c=(ValuePolicy)copy;
    c.optAction  = optAction;
    c.action     = action;
    c.valueKnown = valueKnown;
    c.mdp[0]     = mdp[0];
    c.function   = function;
    c.value      = value;
    c.statesOnly = new PBoolean(statesOnly.val);
    c.r          = r;
    c.v          = v;
    c.dt         = dt;
    c.gamma      = gamma;
    return super.cloneVars(copy);
  }

  /** Initialize, either partially or completely.
    *@see parse.Parsable#initialize
    */
  public void initialize(int level) {
    super.initialize(level);
    if (level==0) { //initialize right after this object created and parse()/setWatchManager() called
      function=(FunApp)origFunction[0].clone();
      try {
        inVect=(MatrixD)origFunction[0].inVect.clone(); //the entire input vector to the net
        outVect=(MatrixD)origFunction[0].outVect.clone();
        pInput.val=inVect.submatrix(1,mdp[0].stateSize(),1);  //this assumes that the first element of the inVect is a bias
        pWeights.val = (MatrixD)origFunction[0].getWeights().clone();
        function.setIO(inVect,value,pWeights.val,null,null,null,null,null,null);
        pOutput.val=new MatrixD(mdp[0].actionSize()+1);
        optAction=pOutput.val.submatrix(1,mdp[0].actionSize(),1);
        if(statesOnly.val)
          action=new MatrixD(mdp[0].actionSize());
        else
          action=inVect.submatrix(1+mdp[0].stateSize(),mdp[0].actionSize(),1); //this assumes that the input vector is a state, action, and bias
      } catch (MatrixException e) {
          System.out.println(e);
      }
    }
    function.initialize(level);
    mdp[0].initialize(level);
  }//end initialization
}//end FunApp

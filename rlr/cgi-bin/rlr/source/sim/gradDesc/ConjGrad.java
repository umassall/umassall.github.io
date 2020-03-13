package sim.gradDesc;
import matrix.*;
import parse.*;
import watch.*;
import pointer.*;
import expression.*;
import sim.errFun.*;
import Random;
import WebSim;

/** Conjugate Gradient.
  *    <p>This code is (c) 1996 Mance Harmon
  *    <<a href=mailto:harmonme@aa.wpafb.af.mil>harmonme@aa.wpafb.af.mil</a>>,
  *    <a href=http://www.aa.wpafb.af.mil/~harmonme>http://www.aa.wpafb.af.mil/~harmonme</a><br>
  *    The source and object code may be redistributed freely.
  *    If the code is modified, please state so in the comments.
  * @version 1.03, 29 May 97
  * @author Mance E. Harmon
  */
public class ConjGrad extends GradDesc {
//Version 1.03, 29 May 97: Added ability to save/restore weights, and initialize weights between min/max bounds.  Also
//added ability to choose between Polak-Ribiere and Flether-Reeves methods of calculating conjugate direction.

//Version 1.02, 21 October 96: Added another criterion for exit.  The convergence tolerance is FTOL.

//Version 1.01, 10 September 96: Changed the code so that the search direction h is reset to the gradient every
//nWeights+1 iterations.  This made a huge difference in the performance of the algorithm. - Mance Harmon

  /** the error function to minimize */
  protected ErrFun errFun=null;
  /** the factor used to create a new search direction */
  protected double beta;
  /** the error being gradient descended on */
  protected PDouble error=new PDouble(1);
  /** the log base 10 of error */
  protected PDouble logError=new PDouble(0);
  /** stop learning when smoothed error < tolerance */
  protected NumExp tolerance=new NumExp(1);
  /** current time (increments once per weight change */
  protected PInt time=new PInt(0);
  /** step size for the initial abscissas used in the mnbrack method */
  protected NumExp abscissa_step=new NumExp(1);
  /** is the experiment being loaded in a save that should be resumed where it left off? */
  private boolean resume=false;
  /** the weights loaded in from the BNF code as the starting weights */
  private MatrixD startWeights;
  /** range for initial random weights */
  private NumExp minWeight=new NumExp(-1),maxWeight=new NumExp(1);
  /** the method used to calculate conjugate direction: Polak-Ribiere (0) or Fletcher-Reeves (1) */
  protected NumExp mode = new NumExp(0);

  private double log10=Math.log(10); //natural log of 10 used to find base 10 logs
  private double ax; // one of the bracketing points used in mnbrak
  private double bx; // the middle bracketing point used in mnbrak
  private double cx; // one of the bracketing points used in mnbrak
  private double fa=0; // the output of the errFun evaluated at a
  private double fb=0; // the output of the errFun evaluated at b
  private double fc=0; // the output of the errFun evaluated at c
  private double fbx=0; // the output of the errFun evaluated at bx, the initial error upon
                        // entering the brent method
  private double xmin=0; // the step size used to find the minimum in one direction
  private double fx=0; //
  private double tol=3.0e-5; // the tolerance value is in brent
  private int    nWeights;  // this is the number of degree of freedom in the function approximator

  /* These are the weights of the function approximator as well as the initial gradient */
  private  MatrixD weights;
  private  MatrixD xi;
  private  MatrixD h; /* direction vector */

  /* These constants are used in the mnbrak method */
  private static final double GOLD=(double)1.618034; //Default ratio by which successive intervals are magnified.
  private static final double GLIMIT=(double)100.0;       //Maximum magnification allowed for a parabolic-fit step.
  private static final double TINY=(double)1.0e-20;
  private static final double FTOL=(double)1.0e-20;

  /* These constants are used in the brent method */
  private static final int    ITMAX =100;
  private static final double CGOLD =(double)0.3819660;
  private static final double ZEPS  =(double)1.0e-10;

  /* These are utility methods used in mnbrak and brent */
  final private double SIGN(double a, double b) {return ((b > 0.0) ? Math.abs(a) : -(Math.abs(a)));}

  /** Register all variables with this WatchManager.
    * This will be called after all parsing is done.
    */
  public void setWatchManager(WatchManager wm,String name) {
    super.setWatchManager(wm,name);
    wm.registerVar(name+"time",         time,           this);
    wm.registerVar(name+"abscissa step",abscissa_step,  this);
    wm.registerVar(name+"tolerance",    tolerance,      this);
    wm.registerVar(name+"error",        error,          this);
    wm.registerVar(name+"log error",    logError,       this);
    wm.registerVar(name+"mode",         mode,           this);
    errFun.setWatchManager(wm,name+"err/");
  }

  /** repeatedly change x until f(x) reaches a local minimum */
  public void run() {
    int count=0;
    double old_dot;
    time.val=0;
    error.val=1;
    logError.val=0;
    MatrixD old_gradient;
    weights=errFun.getInput();

    try {
      if (resume) //starting this experiment from where it left off
        weights.replace(startWeights);
      else  //starting this experiment from scratch
        weights.setRandom(minWeight.val,maxWeight.val,rnd);

      xi=errFun.getGradient(); /* current gradient direction */
      errFun.evaluate(rnd,true,false,false);     //Initialize the gradient vector
      errFun.findGradient();
      old_gradient=xi.duplicate();

      xi.mult(-1);                  //set the gradient vector to the downhill direction
      h=xi.duplicate();             //Trace of past gradients
      old_dot=xi.dot(xi);           //To be used in calculation of beta

      nWeights=weights.size+1;      //used for determining when to reset the search direction

      while (true) {
        if (lastRestartNumber!=WebSim.restartNumber) { //if its time to restart the experiment/**/
          lastRestartNumber=WebSim.restartNumber;
          time.val=0;
          error.val=1;
          rnd=new Random(seed.val);
          weights.setRandom(minWeight.val,maxWeight.val,rnd);
        }

        // line minimization code
        old_gradient.replace(xi);   //save a copy of the gradient
        ax=0; bx=ax+abscissa_step.val;
        cx=bx+abscissa_step.val;    //Initial guess for mnbrak
        mnbrak();                   // bracket minimum to get us in the ballpark
        brent();                    // now use quadratic interpolation to find minimum
        weights.addMult(xmin,xi);   // update the weight vector: weights=weights + xmin*xi
                                    // xi is set to h in the eval() method called by brent()
        // display administrivia
        error.val=errFun.evaluate(rnd,true,false,false); //  calculate the error and find the gradient
        logError.val=Math.log(error.val)/log10;
        time.val++;
        watchManager.update();

        // Check if error<tolerance, if so, quit.  The normal exit criteria is the second half
        // of this conditional statement.  The convergence tolerance is FTOL.
        if((error.val<tolerance.val) ||
          (2.0*Math.abs(fx-fbx)<=FTOL*(Math.abs(fx)+Math.abs(fbx)+TINY))) return;

        // calculate new gradient and new conjugate direction */
        errFun.findGradient();
        xi.mult(-1);                                 //set the gradient vector to the downhill direction
        if(mode.val==0)
          old_dot=polak_ribiere(old_dot,old_gradient); //calc beta, uses new gradient
        else
          old_dot=fletcher_reeves(old_dot);            //calc beta, uses new gradient
        h.multAdd(beta,xi);                          //update the direction vector h=h*beta+xi

        // if num of iterations > degrees of freedom then reset and start again
        if((count++)<nWeights) xi.replace(h);
        else {
            count=0;
            h.replace(xi);
        }

      }//end while(true)
    } catch (MatrixException e) {
      e.print();
    }
  }//end run

  /** Return the BNF description of how to parse the parameters of this object. */
  public String BNF(int lang) {
    return "'{' ( 'tolerance' NumExp | 'step' NumExp | 'initWeights' NumExp [','] NumExp)* "+
           "'error' <sim.errFun.ErrFun> | 'mode' NumExp '}'//Conjugate Gradient.  minimizes error in ErrFun.  "+
           "The two numbers after 'initWeights' are the min and max "+
           "respectively for the initial random weights.  Learning stops when the error drops "+
           "below 'tolerance' or a local minimum is reached.  'step' is used when initially "+
           "guessing points that bracket a minimum for the line search. 'mode' is the method used "+
           "to calculate the conjugate direction: 0=Polak-Ribiere, 1=Fletcher-Reeves. "+
           "DEFAULTS: tolerance = 0, step = 1, initWeights (min=-1,max=1)";
  }

  /* This routine is used to bracket a minimum of a function.   */
  /*  ax, bx, and cx are points on the function. */
  /*  fa, fb, and fc are the value of the function evaluated at those points. */
  /* Adapted from "Numerical Recipes in C", 1988. */
  private final void mnbrak() throws MatrixException {

    double ulim,u,r,q,fu=0,dum=0;

        fa=eval(ax);
        fb=eval(bx);
        if (fb>fa) {   //Switch roles of a and b so that we can go downhill in the direction from a to b
                dum=ax; ax=bx; bx=dum;
                dum=fb; fb=fa; fa=dum;
        }
        cx=bx+GOLD*(bx-ax);     //First guess for c.
        fc=eval(cx);
        while (fb>fc) {                 //Keep returning here until we bracket.
                r=(bx-ax)*(fb-fc);      //Compute u by parabolic extrapolation from a,b,c.
                q=(bx-cx)*(fb-fa);              //TINY is used to prevent any possible division by zero.
                u=(double)(bx-((bx-cx)*q-(bx-ax)*r)/(2.0*SIGN(Math.max(Math.abs(q-r),TINY),q-r)));
                ulim=bx+GLIMIT*(cx-bx);
                //We won't go farther than this.  Now to test various possibilites.
                if ((bx-u)*(u-cx) > 0.0) {              //Parabolic u is between b and c: try it.
                        fu=eval(u);
                        if (fu < fc) {          //Got a minimum between b and c.
                                ax=bx;
                                bx=u;
                                fa=fb;
                                fb=fu;
                                return;
                        } else if (fu > fb) {           //Got a minimum between a and u.
                                cx=u;
                                fc=fu;
                                return;
                        }
                        u=cx+GOLD*(cx-bx);              //Parabolic fit was no use.  Use default magnification.
                        fu=eval(u);
                } else if ((cx-u)*(u-ulim) > 0.0) {     //Parabolic fit is between c and its allowed limit.
                        fu=eval(u);
                        if (fu < fc) {
                                bx=cx; cx=u; u=(cx+GOLD*(cx-bx));
                                fb=fc; fc=fu; fu=eval(u);
                        }
                } else if ((u-ulim)*(ulim-cx) >= 0.0) { //Limit parabolic u to maximum allowed value.
                        u=ulim;
                        fu=eval(u);
                } else {                                        //Reject parabolic u, use default magnification.
                        u=cx+GOLD*(cx-bx);
                        fu=eval(u);
                }
                ax=bx; bx=cx; cx=u;                     //Eliminate oldest point and continue.
                fa=fb; fb=fc; fc=fu;
        }
  } //end mnbrak

  /* Given a function f, and given a bracketing triplet of abscissas ax, bx, cx (such
    that bx is between ax and cx, and f(bx) is less than both f(ax) and f(cx)), this
    routine isolates the minimum to a fractional precision of about tol using Brent's
    method.  The abscissa of the minimum is returned as xmin, and the minimum function
    value is returned as brent, the returned function value. */
    private final void brent() throws MatrixException {
    int iter;
    double a,b,d=0,etemp,fu,fv,fw,p,q,r,tol1,tol2,u,v,w,x,xm;
    double e=0.0f;                   //This will be the distance moved on the step before last.
        a=((ax<cx) ? ax : cx);  //a and b must be in ascending order, though the input abscissas need not be.
        b=((ax>cx) ? ax : cx);
        x=w=v=bx;                               //Initializations...
        fbx=fw=fv=fx=eval(x);
        for (iter=1;iter<=ITMAX;iter++) {       //Main program loop
                xm=0.5f*(a+b);
                tol2=(double)(2.0*(tol1=(double)(tol*(Math.abs(x))+ZEPS)));
                if (Math.abs(x-xm)<=(tol2-0.5*(b-a))) {         //Test for done here.
                        xmin=x;                 //Arrive here ready to exit with best values.
                        return;
                }
                if (Math.abs(e)>tol1) {         //Construct a trial parabolic fit.
                        r=(x-w)*(fx-fv);
                        q=(x-v)*(fx-fw);
                        p=(x-v)*q-(x-w)*r;
                        q=(double)(2.0*(q-r));
                        if (q>0.0) p= -p;
                        q=Math.abs(q);
                        etemp=e;
                        e=d;
                        if (Math.abs(p) >= Math.abs(0.5*q*etemp) ||
                            p <= q*(a-x) || p>= q*(b-x)) {
                                e=((x>=xm) ? (a-x) : (b-x));  //The above conditions determine the acceptibility
                                d=CGOLD*e;                    //The above conditions determine the acceptibility
                        } else {                                          // of the parabolic fit.  Here we take the golden section
                                d=p/q;  //Take the parabolic step       // step into the larger of the two segments.
                                u=x+d;
                                if ((u-a) < tol2 || (b-u) <tol2)
                                        d=SIGN(tol1,xm-x);
                        }
                } else {
                        ///**/ this line crashed Symantec jit 33beta: d=CGOLD*(e=((x >= xm) ? (a-x) : (b-x)));
                        ///**/ but it works fine when split into two lines like this:
                        e=(x >= xm) ? (a-x) : (b-x);
                        d=CGOLD*e;
                }
                u=((Math.abs(d) >= tol1) ? (x+d) : (x+SIGN(tol1,d)));
                fu=eval(u);                                             //This is the one function evaluation per iteration,
                if (fu <= fx) {                                         //and now we have to decide what to do with our
                        if (u >= x) a=x; else b=x;              //function evaluation.  Housekeeping follows:
                        v=w; w=x; x=u;
                        fv=fw; fw=fx; fx=fu;
                } else {
                        if (u < x) a=u; else b=u;
                        if ((fu <= fw) || (w==x)) {
                                v=w;
                                w=u;
                                fv=fw;
                                fw=fu;
                        } else if (fu <= fv || v==x || v==w) {
                                v=u;
                                fv=fu;
                        }
                }                       //Done with housekeeping.  Back for another iteration.
        } //end for ITER
        System.out.println("Too many iterations in BRENT");
        xmin=x;
        return;
  } //end brent

  /* Calculates the mean-squared error at a given point on the error surface */
  private final double eval(double point) throws MatrixException {
    MatrixD step_size = new MatrixD(xi.size);

          if (point==0) {
            error.val=errFun.evaluate(rnd,true,false,false); //calculate the error for the given location
                xi.replace(h);
            return error.val;
          } else {
                xi.mult(point);  //point is the step length
                step_size.replace(xi);  //set the step size to be taken in the search direction
                weights.add(step_size); //move to another location along this direction
                error.val=errFun.evaluate(rnd,true,false,false); //calculate the error for the given location
                weights.sub(step_size); //move back to the original location on the error surface
                xi.replace(h);  //reset xi to the search direction
            return error.val;
          }
  }//end eval

  /* The Polakr-Ribiere method of calculating beta */
  private final double polak_ribiere(double old_dot, MatrixD old_gradient) throws MatrixException {

      double new_dot=xi.dot(xi);  //beta=(g(t+1)*g(t+1)-g(t+1)*g(t))/(g(t)*g(t))
      double mixed_dot=xi.dot(old_gradient);
      beta=(new_dot-mixed_dot)/old_dot;
      return new_dot;
  }

  /* The Fletcher-Reeves method of calculating beta */
  private final double fletcher_reeves(double old_dot) throws MatrixException {

      double new_dot=xi.dot(xi);  //beta=(g(t+1)*g(t+1))/(g(t)*g(t))
          beta=new_dot/old_dot;
          return new_dot;
  }

  /** Output a description of this object that can be parsed with parse().
    * @see Parsable
    */
  public void unparse(Unparser u, int lang) {
    u.emit("{ ");
    u.indent();
    u.emitLine();
    if (lang<0) {
      u.emit(time.val);           u.emit(", ");
      u.emit(error.val);          u.emit(", ");
      u.emitUnparse(weights,lang);    u.emitLine();
    }
    u.emit("tolerance ");
    u.emitUnparse(tolerance,lang);
    u.emitLine();
    u.emit("initWeights ");
    u.emitUnparse(minWeight,lang);
    u.emit(",");
    u.emitUnparse(maxWeight,lang);
    u.emitLine();
    u.emit("step ");
    u.emitUnparse(abscissa_step,lang);
    u.emitLine();
    u.emit("mode ");
    u.emitUnparse(mode,lang);
    u.unindent();
    u.emitLine();
    u.emit("error ");
    u.emitUnparseWithClassName(errFun,lang,false);
    u.emit("}");
  }

  /** Parse the input file to get the parameters for this object.
    * @exception parse.ParserException parser didn't find the required token
    */
  public Object parse(Parser p,int lang) throws ParserException {
    tolerance.val       =0f;    //default: learn forever, never stop
    abscissa_step.val   =1f;
    p.parseChar('{',true);
    if (lang<0) {
      time.val         =((IntExp)p.parseClass("IntExp",lang,true)).val;
      p.parseChar(',',false);
      error.val        =((NumExp)p.parseClass("NumExp",lang,true)).val;
      p.parseChar(',',false);
      startWeights     =(MatrixD)p.parseClass("MatrixD",lang,true);
      resume=true; //pick up where you left off when the experiment was saved
    }
    while (true) { //parse whatever parameters are there
      if (p.tID.equals("tolerance")) {
        p.parseID("tolerance",true);
        tolerance=(NumExp)p.parseClass("NumExp",lang,true);
      } else if (p.tID.equals("step")) {
        p.parseID("step",true);
        abscissa_step=(NumExp)p.parseClass("NumExp",lang,true);
      } else if (p.tID.equals("initWeights")) {
        p.parseID("initWeights",true);
        minWeight=(NumExp)p.parseClass("NumExp",lang,true);
        p.parseChar(',',false);
        maxWeight=(NumExp)p.parseClass("NumExp",lang,true);
      } else if (p.tID.equals("mode")) {
        p.parseID("mode",true);
        mode=(NumExp)p.parseClass("NumExp",lang,true);
      } else
        break;
    }
    p.parseID("error",true);
    errFun=(ErrFun)p.parseType("sim.errFun.ErrFun",lang,true);
    p.parseChar('}',true);

    return this;
  }

  /** Initialize, either partially or completely.
    *@see parse.Parsable#initialize
    */
  public void initialize(int level) {
    super.initialize(level);
    errFun.initialize(level);
  }
} //end class ConjGrad

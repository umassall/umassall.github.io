package picture;
import parse.*;
import Random;
import expression.*;

/** Antialias returns the same picture as its source, but
  * gets multiple samples per pixel from the source and
  * averages them. It breaks the pixel up into several
  * pieces along each dimension, and randomly picks a sample
  * within each resulting hypercube.
  *    <p>This code is (c) 1996 Leemon Baird
  *    <<a href=mailto:leemon@cs.cmu.edu>leemon@cs.cmu.edu</a>>,
  *    <a href=http://www.cs.cmu.edu/~baird>http://www.cs.cmu.edu/~baird</a><br>
  *    The source and object code may be redistributed freely.
  *    If the code is modified, please state so in the comments.
  * @version 1.0, 28 March 96
  * @author Leemon Baird
  */
public class Antialias extends PicPipe {
  protected IntExp samplesX=new IntExp(1); //# samples per pixel per dimension
  protected IntExp samplesY=new IntExp(1);
  protected IntExp samplesZ=new IntExp(1);
  protected IntExp samplesT=new IntExp(1);
  protected int numSamples=1; //product of the 4 variables above
  protected boolean jitter;
  protected Random random=new Random(); //random number generator for creating random rays

  /* Return the BNF description of how to parse the parameters of this object. */
  public String BNF(int lang) {
    return "'jitter' ('true' | 'false') "+
           "'raysX' IntExp "+
           "'raysY' IntExp "+
           "'raysZ' IntExp "+
           "'raysT' IntExp//Antialias each pixel. "+
           "Breaks the pixel into raysX * raysY * raysZ * raysT boxes, "+
           "sends one ray through each box, and averages the results. "+
           "if 'jitter' is true, then shoots each ray at a random "+
           "spot within the box.  Antialiasing in the T direction "+
           "can be useful for animations (gives motion blurring). "+
           "Antialiasing in the Z direction generally isn't useful.";
  }

  /** Output a description of this object that can be parsed with parse().
    * @see Parsable
    */
  public void unparse(Unparser u, int lang) {
    u.emit("jitter ");
    u.emit(jitter ? "true" : "false");
    u.emit(" raysX ");  u.emitUnparse(samplesX,lang);
    u.emit(" raysY ");  u.emitUnparse(samplesY,lang);
    u.emit(" raysZ ");  u.emitUnparse(samplesZ,lang);
    u.emit(" raysT ");  u.emitUnparse(samplesT,lang);
  }

  /** Parse the input file to get the parameters for this object.
    * @exception parse.ParserException parser didn't find the required token
    */
  public Object parse(Parser p,int lang) throws ParserException {
    p.parseID("jitter",true);
    jitter=p.parseBoolean(true);
//    if (p.parseID("true",false))
//      jitter=true;
//    else p.parseID("false",true);
    p.parseID("raysX",true);
    samplesX=(IntExp)p.parseClass("IntExp",lang,true);
    p.parseID("raysY",true);
    samplesY=(IntExp)p.parseClass("IntExp",lang,true);
    p.parseID("raysZ",true);
    samplesZ=(IntExp)p.parseClass("IntExp",lang,true);
    p.parseID("raysT",true);
    samplesT=(IntExp)p.parseClass("IntExp",lang,true);
    numSamples=samplesX.val * samplesY.val * samplesZ.val * samplesT.val;
    return this;
  } //end method parse

  /** Return the color for a particular pixel given its size and location. */
  public void get(PicPipeList source, Colors color,
                  double  x,double  y,double  z,double  t,
                  double dx,double dy,double dz,double dt) {
    int sumr=0,sumg=0,sumb=0,ix,iy,iz,it;
    for (ix=0;ix<samplesX.val;ix++)
      for (iy=0;iy<samplesY.val;iy++)
        for (iz=0;iz<samplesZ.val;iz++)
          for (it=0;it<samplesT.val;it++) {
            source.first.get(source.rest,color,
              x+(double)(ix+(jitter?random.nextDouble():0))*dx/samplesX.val,
              y+(double)(iy+(jitter?random.nextDouble():0))*dy/samplesY.val,
              z+(double)(iz+(jitter?random.nextDouble():0))*dz/samplesZ.val,
              t+(double)(it+(jitter?random.nextDouble():0))*dt/samplesT.val,
              dx,dy,dz,dt);
            sumr+=(int)color.red;
            sumg+=(int)color.green;
            sumb+=(int)color.blue;
          }
    color.red  =(byte)(sumr/numSamples);
    color.green=(byte)(sumg/numSamples);
    color.blue =(byte)(sumb/numSamples);
  } //end method get
} //end class Antialias

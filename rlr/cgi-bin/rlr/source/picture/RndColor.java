package picture;
import parse.*;
import Random;

/**
  * This PicPipe pipeline always ignores its source and returns a
  * random color over half the area, random black or white
  * over another quarter, and a solid color on the other quarter.
  *    <p>This code is (c) 1996 Leemon Baird
  *    <<a href=mailto:leemon@cs.cmu.edu>leemon@cs.cmu.edu</a>>,
  *    <a href=http://www.cs.cmu.edu/~baird>http://www.cs.cmu.edu/~baird</a><br>
  *    The source and object code may be redistributed freely.
  *    If the code is modified, please state so in the comments.
  * @version 1.0, 28 March 96
  * @author Leemon Baird
  */
public class RndColor extends PicPipe {
  protected Colors solidCol;
  protected ColorVector  colVect=null;
  protected Random random=new Random(); //rnd # generator for pixel colors

  /** Return the BNF definition of parameters this object parses. */
  public String BNF(int lang) {
    return "[ picture.ColorVector ]//Random colors. "+
           "A picture with 3 regions, one random black/white, "+
           "one random colors, one a solid color (default green).";
  }

  /** Output a description of this object that can be parsed with parse().
    * @see Parsable
    */
  public void unparse(Unparser u, int lang) {
    if (colVect!=null)
      u.emitUnparse(colVect,lang);
  }

  /** Parse the input file to get this object's parameters.
    * @exception parse.ParserException parser didn't find the required token
    */
  public Object parse(Parser p,int lang) throws ParserException {
    colVect=(ColorVector)p.parseClass("ColorVector",lang,false);
    if (colVect!=null)
      solidCol=colVect.color;
    else {
      solidCol=new Colors();
      solidCol.setGreen(); //default is green if no color vector
    }
    return this;
  } //end method parse

  /** Return the color for a particular pixel given its size and location. */
  public void get(PicPipeList source,Colors color,
                  double  x,double  y,double  z,double  t,
                  double dx,double dy,double dz,double dt) {
    if(random.nextDouble()>.5)
      color.red=color.green=color.blue=0;
    else
      color.red=color.green=color.blue=(byte)255;

    if (x+y<0)
      if (x-y>0)
        solidCol.copyInto(color);
      else
        if (random.nextDouble()>0.5)
          color.setBlack();
        else
          color.setWhite();
    else
      color.setRGB(
        random.nextDouble(),
        random.nextDouble(),
        random.nextDouble());
  }
} //end class RndColor

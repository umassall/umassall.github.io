package picture;
import java.util.Vector;
import parse.*;
import expression.*;

/** This PicPipe returns the max difference between values of adjacent source pixels.
  * It compares the current pixel to it's immediate neighbor to the right,
  * and the neighbor below (and also into the z direction if dz!=0).
  *    <p>This code is (c) 1996 Leemon Baird
  *    <<a href=mailto:leemon@cs.cmu.edu>leemon@cs.cmu.edu</a>>,
  *    <a href=http://www.cs.cmu.edu/~baird>http://www.cs.cmu.edu/~baird</a><br>
  *    The source and object code may be redistributed freely.
  *    If the code is modified, please state so in the comments.
  * @version 1.0, 20 April 96
  * @author Leemon Baird
  */
public class Edges extends PicPipe {
  protected IntExp thickness; //how many pixels wide does a vertical edge become?
  protected Colors middle=new Colors(); //value of current pixel
  protected Colors right =new Colors(); //value of neighbor on the right
  protected Colors down  =new Colors(); //value of neighbor below
  protected Colors into  =new Colors(); //value of neighbor in Z direction

  /* Return the BNF description of how to parse the parameters of this object. */
  public String BNF(int lang) {
    return "IntExp//Trace figure edges. "+
           "Each edge becomes a line IntExp pixels thick.";
  }

  /** Output a description of this object that can be parsed with parse().
    * @see Parsable
    */
  public void unparse(Unparser u, int lang) {
    u.emitUnparse(thickness,lang);
  }

  /** Parse the input file to get the parameters for this object.
    * @exception parse.ParserException parser didn't find the required token
    */
  public Object parse(Parser p,int lang) throws ParserException {
    thickness=(IntExp)p.parseClass("IntExp",lang,true);
    return this;
  } //end method parse

  /** Return the color for a particular pixel given its size and location. */
  public void get(PicPipeList source, Colors color,
                  double  x,double  y,double  z,double  t,
                  double dx,double dy,double dz,double dt) {
    double dr,dd,di;
    source.first.get(source.rest,middle,x                 ,y,z,t,dx,dy,dz,dt);
    source.first.get(source.rest,right ,x+dx*thickness.val,y,z,t,dx,dy,dz,dt);
    source.first.get(source.rest,down  ,x,y+dy*thickness.val,z,t,dx,dy,dz,dt);
    if (dz==0)  //if not doing 3D texture, don't look at edges in Z direction
      into.value=middle.value;
    else
      source.first.get(source.rest,into,x,y,z+dz*thickness.val,t,dx,dy,dz,dt);
    dr=(right.value>middle.value) ? right.value -middle.value //find absulute values
                                  : middle.value-right.value; //  of differences
    dd=(down.value >middle.value) ? down.value  -middle.value
                                  : middle.value-down.value;
    di=(into.value >middle.value) ? into.value  -middle.value
                                  : middle.value-into.value;
    color.value = dr>dd ? (dr > di ? dr : di)  //return max of 3 differences
                        : (dd > di ? dd : di);
  } //end get
} //end class Edges

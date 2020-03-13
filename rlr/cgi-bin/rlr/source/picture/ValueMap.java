package picture;
import java.util.Vector;
import parse.*;

/** This pipeline gets a double and returns a double based on a piecewise linear function.
  * The function is formed by linear interpolation of a list of (x,y) pairs.
  * If the value exactly matches one or more x values, it takes the first one.
  * The (x,y) pairs should be sorted with increasing x values.
  *    <p>This code is (c) 1996 Leemon Baird
  *    <<a href=mailto:leemon@cs.cmu.edu>leemon@cs.cmu.edu</a>>,
  *    <a href=http://www.cs.cmu.edu/~baird>http://www.cs.cmu.edu/~baird</a><br>
  *    The source and object code may be redistributed freely.
  *    If the code is modified, please state so in the comments.
  * @version 1.0, 20 April 96
  * @author Leemon Baird
  */
public class ValueMap extends PicPipe {
  protected ValueMapEntry[] entries; //each (x,y) pair saying to map x to y

  /* Return the BNF description of how to parse the parameters of this object. */
  public String BNF(int lang) {
    return "picture.ValueMapEntry +//Piecewise linear function. "+
           "A piecewise linear mapping from doubles to doubles. "+
           "Function is defined at a list of points, and linearly "+
           "interpolated in between.";
  }

  /** Output a description of this object that can be parsed with parse().
    * @see Parsable
    */
  public void unparse(Unparser u, int lang) {
    u.indent();
    for (int i=0;i<entries.length;i++) {
      u.emitLine();
      u.emitUnparse(entries[i],lang);
    }
    u.unindent();
  }

  /** Parse the input file to get the parameters for this object.
    * @exception parse.ParserException parser didn't find the required token
    */
  public Object parse(Parser p,int lang) throws ParserException {
    ValueMapEntry temp;  //used for sorting entries.
    Vector vect;         //hold the vector of color map entries
    vect=p.parseClassList("ValueMapEntry",lang,true);
    entries=new ValueMapEntry[vect.size()];
    vect.copyInto(entries);
    for (int i=0;i<entries.length-1;i++) //sort entries[] by .x
      for (int j=i+1;j<entries.length;j++)
        if (entries[j].x.val<entries[i].x.val) {
          temp=entries[i];
          entries[i]=entries[j];
          entries[j]=temp;
        }
    return this;
  } //end method parse

  /** Return the color for a particular pixel given its size and location. */
  public void get(PicPipeList source, Colors color,
                  double  x,double  y,double  z,double  t,
                  double dx,double dy,double dz,double dt) {
    double dist; //distance from color.value to next lower map entry .x
    source.first.get(source.rest,color,x,y,z,t,dx,dy,dz,dt);
    if (color.value<=entries[0].x.val) {
      color.value=entries[0].y.val;
      return;
    }
    for (int i=1;i<entries.length;i++) {
      if (color.value<=entries[i].x.val) { //linear interpolation between adjacent numbers
        dist=(color.value-entries[i-1].x.val)/(entries[i].x.val-entries[i-1].x.val);
        color.value=(1-dist)*entries[i-1].y.val + dist*(entries[i].y.val);
        return;
      }
    }
    color.value=entries[entries.length-1].y.val; //value is above highest x, so use last y
    return;
  } //end get
} //end class ValueMap

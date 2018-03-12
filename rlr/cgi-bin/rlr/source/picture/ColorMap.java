package picture;
import java.util.Vector;
import parse.*;

/** This pipeline gets a double and returns a color based on linear interpolation.
  * The color map comprises multiple assignments of colors to real values,
  * and this program does linear interpolation between the colors to
  * convert doubles to colors.
  * If the value exactly matches one or more of the doubles, it takes the first one.
  * The entries should be sorted with increasing real values.
  *    <p>This code is (c) 1996 Leemon Baird
  *    <<a href=mailto:leemon@cs.cmu.edu>leemon@cs.cmu.edu</a>>,
  *    <a href=http://www.cs.cmu.edu/~baird>http://www.cs.cmu.edu/~baird</a><br>
  *    The source and object code may be redistributed freely.
  *    If the code is modified, please state so in the comments.
  * @version 1.0, 28 March 96
  * @author Leemon Baird
  */
public class ColorMap extends PicPipe {
  protected ColorMapEntry colors[]; //the entries, each giving one color for one value

  /* Return the BNF description of how to parse the parameters of this object. */
  public String BNF(int lang) {
    return "picture.ColorMapEntry +//linearly-interpolated mapping from double to color.";
  }

  /** Output a description of this object that can be parsed with parse().
    * @see Parsable
    */
  public void unparse(Unparser u, int lang) {
    u.indent();
    for (int i=0;i<colors.length;i++) {
      u.emitLine();
      u.emitUnparse(colors[i],lang);
    }
    u.unindent();
  }

  /** Parse the input file to get the parameters for this object.
    * @exception parse.ParserException parser didn't find the required token
    */
  public Object parse(Parser p,int lang) throws ParserException {
    ColorMapEntry temp; //used for sorting colors[]
    Vector vect;        //hold the vector of color map entries
    vect=p.parseClassList("ColorMapEntry",lang,true);
    colors=new ColorMapEntry[vect.size()];
    vect.copyInto(colors);
    for (int i=0;i<colors.length-1;i++) //sort colors[] by .level
      for (int j=i+1;j<colors.length;j++)
        if (colors[j].level.val<colors[i].level.val) {
          temp=colors[i];
          colors[i]=colors[j];
          colors[j]=temp;
        }
    return this;
  } //end method parse

  /** Return the color for a particular pixel given its size and location. */
  public void get(PicPipeList source, Colors color,
                  double  x,double  y,double  z,double  t,
                  double dx,double dy,double dz,double dt) {
    double dist; //distance from color.value to next lower level in map
    source.first.get(source.rest,color,x,y,z,t,dx,dy,dz,dt);
    if (color.value<=colors[0].level.val) { //return first color if value < its value
      colors[0].cols.color.copyInto(color);
      return;
    }
    for (int i=1;i<colors.length;i++) {
      if (color.value<=colors[i].level.val) { //linear interpolation between adjacent colors
      dist=(color.value-colors[i-1].level.val)/(colors[i].level.val-colors[i-1].level.val);

        color.set(
          dist*colors[i].red   +(1-dist)*colors[i-1].red,
          dist*colors[i].green +(1-dist)*colors[i-1].green,
          dist*colors[i].blue  +(1-dist)*colors[i-1].blue,
          dist*colors[i].filter+(1-dist)*colors[i-1].filter,
          dist*colors[i].trans +(1-dist)*colors[i-1].trans);
        return;
      }
    }
    colors[colors.length-1].cols.color.copyInto(color);
  } //end get
} //end class ColorMap


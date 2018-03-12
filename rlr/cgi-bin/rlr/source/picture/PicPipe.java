package picture;
import parse.*;

/** PicPipe objects are connected together for drawing pictures.
  * Each one calls the next on the pipeline to get a number or color,
  * and returns a modified number or color.
  * Each PicPipe has a method called get() that,
  * given an (x,y,z,t) coordinate, returns
  * the color associated with that location (a PicColor).
  * So it calls its source, then returns the value or color for
  * location (x,y,z,t). x,y,z,t are doubles representing a point in
  * the region being drawn (usually (-1..1,-1..1,-1..1,0..1)).
  * The size of a pixel (dx,dy,dz,dt) is also passed in to allow
  * antialiasing, edge detection, etc.  t is the time of the current frame
  * in an animation, and dt is the time between the current and next frame.
  *    <p>This code is (c) 1996 Leemon Baird
  *    <<a href=mailto:leemon@cs.cmu.edu>leemon@cs.cmu.edu</a>>,
  *    <a href=http://www.cs.cmu.edu/~baird>http://www.cs.cmu.edu/~baird</a><br>
  *    The source and object code may be redistributed freely.
  *    If the code is modified, please state so in the comments.
  * @version 1.0, 26 April 96
  * @author Leemon Baird
  * @see Colors
  * @see PicPipeList
  */
public abstract class PicPipe implements Parsable {
  protected double regionMinX=-1; //region to display initially
  protected double regionMaxX= 1;
  protected double regionMinY=-1;
  protected double regionMaxY= 1;
  protected String desc=null; //String describing this picture (get from source if null)
  protected boolean useSourceRegion=true; //region to show comes from source?

  /** Return a parameter array if BNF(), parse(), and unparse() are to be automated, null otherwise.
    * @see parse.Parsable#getParameters
    */
  public Object[][] getParameters(int lang) {
    return null;
  }

  /** Return BNF description of parameters for this object, when parsing language lang.
    * It should use single quotes around literal strings, * for zero or more,
    * + for one or more,  [ ] for optional, ( ) for grouping, and should
    * end with a comment describing it that starts with a dot.
    */
  public abstract String BNF(int lang);

  /** Output a description of this object and its state.
    * If emitName, then output the name of the class first.  Create
    * it so that it can be parsed with the parse command, when it
    * is parsing with language lang.  Output is emitted by calling
    * u.emit(...).
    */
  public abstract void unparse(Unparser u, int lang);

  /** Create/initialize an object from a string describing it in language lang.
    * @exception parse.ParserException parser didn't find the required token
    */
  public abstract Object parse(Parser p, int lang) throws ParserException;

  /** Passing in an object color, change its contents to return a pixel's color.
    * This PicPipe may call it's source PicPipe one or more times
    * first, then modify the values it receives before returning.
    * If not overridden, the PicPipe does nothing.
    */
  public void get(PicPipeList source, Colors color,
                  double  x,double  y,double  z,double  t,
                  double dx,double dy,double dz,double dt) {
    source.first.get(source.rest,color,x,y,z,t,dx,dy,dz,dt);
  }

  /** Initialize this object, and possibly remove from linked list.
    * This object is given a list containing itself and is sources.
    * It can call its sources and set them up, then remove itself
    * from the linked list if desired.  This method does nothing by
    * default, and usually will not be implemented in most classes.
    */
  public PicPipeList init(PicPipeList picPipeList) {
    return picPipeList; //don't initialize anything, or remove self from list
  }

  /** Set the string describing what this picture is. */
  public void setDescription(String description) {
    desc=description;
  }

  /** Get the string describing what this picture is. */
  public String getDescription(PicPipeList source) {
    if (desc!=null)   //I have one, so return it
      return desc;
    if (source==null) //I have no source, there is no description
      return null;
    return source.first.getDescription(source.rest); //ask the source
  }

  /** Set the coordinates of the region this pipeline looks at */
  public void setRegion(double minX, double minY, double maxX, double maxY) {
    useSourceRegion=false;
    regionMinX=minX;
    regionMaxX=maxX;
    regionMinY=minY;
    regionMaxY=maxY;
  }

  /** Return min x coordinate of region this pipeline looks at */
  public double regionMinX(PicPipeList source) {
    return useSourceRegion ? (source==null ? -1
                                           : source.first.regionMinX(source.rest))
                           : regionMinX;
  }
  /** Return max x coordinate of region this pipeline looks at */
  public double regionMaxX(PicPipeList source) {
    return useSourceRegion ? (source==null ? 1
                                           : source.first.regionMaxX(source.rest))
                           : regionMaxX;
  }
  /** Return min y coordinate of region this pipeline looks at */
  public double regionMinY(PicPipeList source) {
    return useSourceRegion ? (source==null ? -1
                                           : source.first.regionMinY(source.rest))
                           : regionMinY;
  }
  /** Return max y coordinate of region this pipeline looks at */
  public double regionMaxY(PicPipeList source) {
    return useSourceRegion ? (source==null ? 1
                                           : source.first.regionMaxY(source.rest))
                           : regionMaxY;
  }

  /** Initialize, either partially or completely.
    *@see parse.Parsable#initialize
    */
  public void initialize(int level) {}
} //end class PicPipe

package picture;
import java.util.Vector;
import parse.*;

/**
  * A gallery combines multiple PicPipe pictures into one tiling.
  *    <p>This code is (c) 1996 Leemon Baird
  *    <<a href=mailto:leemon@cs.cmu.edu>leemon@cs.cmu.edu</a>>,
  *    <a href=http://www.cs.cmu.edu/~baird>http://www.cs.cmu.edu/~baird</a><br>
  *    The source and object code may be redistributed freely.
  *    If the code is modified, please state so in the comments.
  * @version 1.0, 28 March 96
  * @author Leemon Baird
  */
public class Gallery extends PicPipe {
  protected int numRows;    //# of rows of pictures in gallery (= # columns)
  protected double border=1.f/32.f; //fraction of picture width taken up by half of black frame on left side.
  protected double expand=1.f/(1.f-border*3.f); //expand nonborder part of picture by this
  protected PicPipeList[] pic;     //all the pictures being drawn
  protected PicPipePipeline[] pipes; //all parsed objects: pictures and #defines, etc.
  protected double[] minX,maxX,minY,maxY; // drawn for each picture
  protected int currPict=-1; //-1 shows all pictures, nonnegative shows only one

  /** Calculates a modulo b for doubles.  Same as % if a and b are positive*/
  public static final double mod(double a, double b) {
    return (double)(a-Math.floor(a/b)*b);
  }

  /** return the picture in the gallery containing point (x,y)
    */
  public PicPipeList whichPic(double x, double y) {
    int gx=(int)Math.floor((x+1.f)/2.f*numRows);
    int gy=numRows-1-(int)Math.floor((y+1.f)/2.f*numRows);
    int which=(int)mod((gy*numRows+gx),numRows*numRows);
    return (which<pic.length) ? pic[which] : null;
  }

  /** return the number of the picture in the gallery containing point (x,y)
    */
  public int picNum(double x, double y) {
    int gx=(int)Math.floor((x+1.f)/2.f*numRows);
    int gy=numRows-1-(int)Math.floor((y+1.f)/2.f*numRows);
    int which=(int)mod((gy*numRows+gx),numRows*numRows);
    return (which<pic.length) ? which : -1;
  }

  /** Return min x coordinate of the picture containing point (x,y).
    * This tells where on the screen the corners of this little picture lie.
    * This can be used to highlight the little picture when clicked on.
    */
  public double minX(double x, double y) {
    int gx=(int)Math.floor((x+1.f)/2.f*numRows);
      return (double)gx/numRows*2.f-1.f;
  }

  /** return max x coordinate of the picture containing point (x,y)
    * This tells where on the screen the corners of this little picture lie.
    * This can be used to highlight the little picture when clicked on.
    */
  public double maxX(double x, double y) {
    int gx=1+(int)Math.floor((x+1.f)/2.f*numRows);
      return (double)gx/numRows*2.f-1.f;
  }

  /** return min y coordinate of the picture containing point (x,y)
    * This tells where on the screen the corners of this little picture lie.
    * This can be used to highlight the little picture when clicked on.
    */
  public double minY(double x, double y) {
    int gy=numRows-1-(int)Math.floor((y+1.f)/2.f*numRows);
      return (double)gy/numRows*2.f-1.f;
  }

  /** return max y coordinate of the picture containing point (x,y)
    * This tells where on the screen the corners of this little picture lie.
    * This can be used to highlight the little picture when clicked on.
    */
  public double maxY(double x, double y) {
    int gy=1+numRows-1-(int)Math.floor((y+1.f)/2.f*numRows);
      return (double)gy/numRows*2.f-1.f;
  }

  /** Return the color of a point (x,y) in the gallery where pixels are size (dx,dy).
    * The gallery has (x,y) going from (-1,-1) to (1,1).
    * The subpicture at slot (gx,gy) has a local frame of reference going
    * from (-1,-1) to (1,1) within that picture, and (x,y) in that coordinate
    * system is (px,py).  The subpictures can be changed to
    * show a region other than (-1,-1)-(1,1).
    * The 4 functions such as regionMinX() define the region to show.
    */
  public void get(PicPipeList source, Colors color,
                  double  x,double  y,double  z,double  t,
                  double dx,double dy,double dz,double dt) {
    double xx,yy,px,py;
    int gx,gy,whichPic;
    if (currPict>=0) { //if showing 1 picture, pass through the get() directly
      pic[currPict].first.get(pic[currPict],color,x,y,z,t,dx,dy,dz,dt);
      return;
    }
    xx=(x+1)/2; //fraction of way across
    yy=(y+1)/2; //fraction of way up
    gx=(int)Math.floor(xx*numRows); //which row of pictures
    gy=numRows-1-(int)Math.floor(yy*numRows); //which col of pictures
    whichPic=(int)mod((gy*numRows+gx),numRows*numRows); //which picture #
    px=mod((xx*numRows),1.f); //x coordinate within picture+frame from [0,1]
    py=mod((yy*numRows),1.f); //y coordinate within picture+frame from [0,1]
    if (whichPic>=pic.length) {
      color.setWhite();
      return;
    }
    if (Math.abs(px*2-1)>1.f-border || Math.abs(py*2-1)>1.f-border) {
      color.setBlack();
      return;
    }
    if (Math.abs(px*2-1)>1.f-border*3 || Math.abs(py*2-1)>1.f-border*3) {
      color.setWhite();
      return;
    }
    px=(px-.5f)*expand+.5f; //x coordinate within picture (not including frame) from [0,1]
    py=(py-.5f)*expand+.5f; //y coordinate within picture (not including frame) from [0,1]
    pic[whichPic].first.get(pic[whichPic].rest,color,
      minX[whichPic]+px*(maxX[whichPic]-minX[whichPic]),     //x coordinate
      minY[whichPic]+py*(maxY[whichPic]-minY[whichPic]),z,t, //y,z,t coordinates
      dx*numRows*expand*(maxX[whichPic]-minX[whichPic])/2,
      dy*numRows*expand*(maxY[whichPic]-minY[whichPic])/2,
      dz,dt);
  } //end method get

  /** Return a BNF describing how this object parses its parameters. */
  public String BNF(int lang) { //a Gallery is a list of PicPipe objects in {}
    return "'{' picture.PicPipePipeline * '}'//A list of pictures to tile.";
  }

  /** Output a description of this object that can be parsed with parse().
    * @see Parsable
    */
  public void unparse(Unparser u, int lang) {
    u.emit(" {");
    u.indent();
    for (int i=0;i<pipes.length;i++) {
      u.emitLine();
      u.emitUnparse(pipes[i],lang);
    }
    u.unindent();
    u.emitLine();
    u.emit("}");
  }

  /** parse a series of pictures and combine into a tiled gallery.
    * @exception parse.ParserException parser didn't find the required token
    */
  public Object parse(Parser p,int lang) throws ParserException {
    Vector all;
    Vector viewable;
    p.parseChar('{',true);
    all=p.parseClassList("PicPipePipeline",lang,false);
    p.parseChar('}',true);
    pipes=new PicPipePipeline[all.size()];
    all.copyInto(pipes); //remember all so they can be unparsed later
    viewable=new Vector();
    for (int i=0;i<pipes.length;i++) {
      if (pipes[i].viewable) //find which ones were viewable and display them
        viewable.addElement(pipes[i].source);
    }
    pic =new PicPipeList[viewable.size()];
    minX=new double        [viewable.size()];
    maxX=new double        [viewable.size()];
    minY=new double        [viewable.size()];
    maxY=new double        [viewable.size()];
    viewable.copyInto(pic);
    for (int i=0;i<pic.length;i++) { //find out region graphed for each picture
      minX[i]=pic[i].first.regionMinX(pic[i].rest);
      maxX[i]=pic[i].first.regionMaxX(pic[i].rest);
      minY[i]=pic[i].first.regionMinY(pic[i].rest);
      maxY[i]=pic[i].first.regionMaxY(pic[i].rest);
    }
    for(numRows=1;numRows*numRows<pic.length;numRows++); //calculate # rows
    return this; //return this Gallery object with all parameters parsed
  }//end method parse

  /** If this shows multiple pictures, return this as the first visible gallery.
    * If this shows only one picture, then start following the list for that
    * picture to see if it is a gallery (possible preceded by various filters).
    * This is used by the ViewApplet to know which object to ask various
    * gallery-specific questions such as "which picture did the mouse click on?".
    */
  public Gallery getGallery() {
    Gallery g;
    if (currPict==-1) //if this Gallery is showing the whole Gallery, return it
      return this;
    else
      return pic[currPict].getGallery();
  }

  /** Reset all Galleries to show all their pictures, not just one. */
  public void resetGalleries() {
    currPict=-1; //show all the pictures
    for (int i=0; i<pic.length; i++)
      pic[i].resetGalleries();  //reset any galleries within these pictures
  }

  /** In the future, show just one picture, not a collection.
    * The one picture should be the one selected by the mouse click
    * at location (x,y).
    */
  public void select(double x, double y) {
    int gx=(int)Math.floor((x+1.f)/2.f*numRows);
    int gy=numRows-1-(int)Math.floor((y+1.f)/2.f*numRows);
    int which=(int)mod((gy*numRows+gx),numRows*numRows);
    currPict=which;
  }
  /** Return min x coordinate of region this pipeline looks at */
  public double regionMinX(PicPipeList source) {
    if (currPict>-1) //if showing only one picture, use its boundary
      return pic[currPict].first.regionMinX(pic[currPict].rest);
    else             //if showing many, use this gallery's boundary
      return super.regionMinX(source);
  }
  /** Return max x coordinate of region this pipeline looks at */
  public double regionMaxX(PicPipeList source) {
    if (currPict>-1) //if showing only one picture, use its boundary
      return pic[currPict].first.regionMaxX(pic[currPict].rest);
    else             //if showing many, use this gallery's boundary
      return super.regionMaxX(source);
  }
  /** Return min y coordinate of region this pipeline looks at */
  public double regionMinY(PicPipeList source) {
    if (currPict>-1) //if showing only one picture, use its boundary
      return pic[currPict].first.regionMinY(pic[currPict].rest);
    else             //if showing many, use this gallery's boundary
      return super.regionMinY(source);
  }
  /** Return max y coordinate of region this pipeline looks at */
  public double regionMaxY(PicPipeList source) {
    if (currPict>-1) //if showing only one picture, use its boundary
      return pic[currPict].first.regionMaxY(pic[currPict].rest);
    else             //if showing many, use this gallery's boundary
      return super.regionMaxY(source);
  }

  /** Return the description string that tells what this picture is.
    * If multiple pictures are showing, return this gallery's description,
    * else return the description for the single picture that's showing.
    */
  public String getDescription(PicPipeList source) {
    if (currPict==-1)
      return super.getDescription(source);
    return pic[currPict].first.getDescription(pic[currPict].rest);
  }
} //end class Gallery

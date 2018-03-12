package picture;
import parse.*;
import expression.*;

/** An animation is a gallery of pictures that differ only in the
  * the time parameter.  These pictures can be combined into a
  * movie.
  *    <p>This code is (c) 1996 Leemon Baird
  *    <<a href=mailto:leemon@cs.cmu.edu>leemon@cs.cmu.edu</a>>,
  *    <a href=http://www.cs.cmu.edu/~baird>http://www.cs.cmu.edu/~baird</a><br>
  *    The source and object code may be redistributed freely.
  *    If the code is modified, please state so in the comments.
  * @version 1.0, 28 March 96
  * @author Leemon Baird
  */
public class Animation extends Gallery {
   protected double time[];           //time parameter for each frame
   protected double dTime;            //time between adjacent frames
   protected NumExp first =new NumExp(0); //time of first frame
   protected NumExp last  =new NumExp(1); //time of last frame
   protected IntExp frames=new IntExp(4); //number of frames
   protected boolean skipLast=false; //don't generate the last frame that would have time last
   protected PicPipePipeline source; //pipeline describing one frame

  /** Return the color of a point (x,y) in the gallery where pixels are size (dx,dy).
    * This is identical to Gallery, except it uses time[] and dTime to
    * set the time parameters for this picture.
    */
  public void get(PicPipeList source, Colors color,
                  double  x,double  y,double  z,double  t,
                  double dx,double dy,double dz,double dt) {
    int which=currPict>0 ? currPict : picNum(x,y);
    if (which>-1)
      super.get(source,color,x,y,z,time[which],dx,dy,dz,dTime);
    else
      color.setWhite(); //white region outside the defined frames
  } //end method get

  /** Return a BNF describing how this object parses its parameters. */
  public String BNF(int lang) {
    return "( ('first' NumExp) | ('last' NumExp) | ('frames' IntExp) )* " +
           "<picture.PicPipe>//The frames of an Animation. "+
           "Each frame differs only in the time, and they are all laid "+
           "out like the frames in a gallery.";
  }
  /** Output a description of this object that can be parsed with parse().
    * @see Parsable
    */
  public void unparse(Unparser u, int lang) {
    u.emit(" first ");  u.emitUnparse(first, lang);
    u.emit(" last ");   u.emitUnparse(last,  lang);
    u.emit(" frames "); u.emitUnparse(frames,lang);
    if (skipLast)
      u.emit(" skipLast");
    u.indent();
    u.emitLine();
    u.emitUnparse(source,lang);
    u.unindent();
  } //end unparse

  /** parse a series of pictures and combine into a tiled gallery.
    * @exception parse.ParserException parser didn't find the required token
    */
  public Object parse(Parser p,int lang) throws ParserException {
    while (true) {
      if (p.parseID("first",false))
        first=(NumExp)p.parseClass("NumExp",lang,true);
      else if (p.parseID("last",false))
        last=(NumExp)p.parseClass("NumExp",lang,true);
      else if (p.parseID("frames",false))
        frames=(IntExp)p.parseClass("IntExp",lang,true);
      else if (p.parseID("skipLast",false))
        skipLast=true;
      else
        break;
    }
    pic =new PicPipeList[frames.val];
    minX=new double      [frames.val];
    maxX=new double      [frames.val];
    minY=new double      [frames.val];
    maxY=new double      [frames.val];
    time=new double      [frames.val];
    for(numRows=1;numRows*numRows<pic.length;numRows++); //calculate # rows
    source=(PicPipePipeline)p.parseClass("PicPipePipeline",lang,true);
    pic[0]=source.source;
    dTime=(last.val-first.val)/(frames.val - (skipLast ? 0 : 1));
    for (int i=0;i<frames.val;i++) {
      pic [i]=pic[0]; //all frames are pointers to a single picture object
      minX[i]=pic[i].first.regionMinX(pic[i].rest);
      maxX[i]=pic[i].first.regionMaxX(pic[i].rest);
      minY[i]=pic[i].first.regionMinY(pic[i].rest);
      maxY[i]=pic[i].first.regionMaxY(pic[i].rest);
      time[i]=first.val+i*dTime;
    }
    return this;
  }//end method parse
} //end class Animation

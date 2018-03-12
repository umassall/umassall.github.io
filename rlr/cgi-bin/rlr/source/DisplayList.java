import Display;
import watch.*;
import pointer.*;
import java.awt.*;
import java.util.Vector;
import parse.*;
import expression.*;
import java.applet.Applet;

/** A list of displays that are each a separate window
  *    <p>This code is (c) 1996 Leemon Baird
  *    <<a href=mailto:leemon@cs.cmu.edu>leemon@cs.cmu.edu</a>>,
  *    <a href=http://www.cs.cmu.edu/~baird>http://www.cs.cmu.edu/~baird</a><br>
  *    The source and object code may be redistributed freely.
  *    If the code is modified, please state so in the comments.
  * @version 1.01, 24 July 96
  * @author Leemon Baird
  */
public class DisplayList implements Parsable {
  /** The WatchManager that this Watcher watches */
  WatchManager watchManager;
  /** The list of displays */
  public Display[] displays;
  /** Is each display embedded in the HTML page? */
  public Boolean[] embed;

  /** constructor that does nothing */
  public DisplayList() {super();} //CAFE bug: this must be typed here, even though it's supposedly redundant

  /** This constructor takes the same parameters that parse() parses, and
    * initializes this object just as if it had been created by parsing instead.
    * All the arrays should be the same length.  They give the (x,y) coordinates
    * of the upper-left corner of each display, its width and height,
    * whether it should be embedded, and the Display it contains.  The last
    * parameter is the applet that owns this display list.
    */
  public DisplayList(int[] x, int[] y, int[] width, int[] height,
                     Boolean[] embedDisp, Display[] displayList,
                     Applet applet) {
    displays=displayList;
    embed   =embedDisp;
    for (int i=0;i<displays.length;i++)
      displays[i].createWin(x[i],y[i],width[i],height[i],applet,embed[i].booleanValue());
  }

  /** Return a parameter array if BNF(), parse(), and unparse() are to be automated, null otherwise.
    * @see parse.Parsable#getParameters
    */
  public Object[][] getParameters(int lang) {
    return null;
  }

  /* Return the BNF description of how to parse the parameters of this object. */
  public String BNF(int lang) {
    return "'{' ( ['embed'] "+
           "['(' IntExp [','] IntExp [','] IntExp [','] IntExp ')'] "+
           "<Display> )* '}'//List of displays. "+
           "Gives the (x,y,width,height) for each display window.";
  }

  /** Output a description of this object that can be parsed with parse().
    */
  public void unparse(Unparser u, int lang) {
    u.emit("{");
    u.indent();
      for (int i=0;i<displays.length;i++) {
        u.emitLine();
        if (displays[i]!=null) {
          if (embed[i].booleanValue())
            u.emit("embed ");
          u.emit('(');
          u.emit(displays[i].x());
          u.emit(',');
          u.emit(displays[i].y());
          u.emit(',');
          u.emit(displays[i].width());
          u.emit(',');
          u.emit(displays[i].height());
          u.emit(") ");
          u.emitLine();
        }
        u.emitUnparseWithClassName(displays[i],lang,false);
      }
    u.unindent();
    u.emitLine();
    u.emit("}");
  } //end method unparse

  /** Parse the input file to get the parameters for this object.
    * @exception parse.ParserException parser didn't find the required token
    */
  public Object parse(Parser p,int lang) throws ParserException {
    int winX=0,winY=0,winWidth=200,winHeight=200;
    Display display;
    boolean dEmbed;
    Vector displayList = new Vector();
    Vector embedList = new Vector();
    p.parseChar('{',true);
    while (p.tChar=='(' || p.tID.equals("embed")) {
      dEmbed=p.parseID("embed",false);
      embedList.addElement(new Boolean(dEmbed));
      p.parseChar('(',true);
      winX=((IntExp)p.parseClass("IntExp",lang,true)).val;
      p.parseChar(',',false);
      winY=((IntExp)p.parseClass("IntExp",lang,true)).val;
      p.parseChar(',',false);
      winWidth=((IntExp)p.parseClass("IntExp",lang,true)).val;
      p.parseChar(',',false);
      winHeight=((IntExp)p.parseClass("IntExp",lang,true)).val;
      p.parseChar(')',true);
      display=(Display)p.parseType("Display",lang,true);
      displayList.addElement((Object)display);
      display.createWin(winX,winY,winWidth,winHeight,p.applet,dEmbed);
    }
    p.parseChar('}',true);
    displays=new Display[displayList.size()];
    embed   =new Boolean[embedList.size()];
    displayList.copyInto(displays);
    embedList.copyInto(embed);
    return this;
  } //end method parse

  /** Get the WatchManager being used */
  public WatchManager getWatchManager() {
    return watchManager;
  }

  /** Set the WatchManager, and create the window */
  public void setWatchManager(WatchManager wm,String name) {
    watchManager=wm;
    for (int i=0;i<displays.length;i++)
      displays[i].setWatchManager(wm,name+i+"/");
  }

  /** when the simulator is destroyed, it asks the DisplayList to
    * dispose of every Display's window.
    */
  public void destroy() {
    if (displays!=null)
      for (int i=0;i<displays.length;i++)
        if (displays[i]!=null)
          if (displays[i].window!=null)
            displays[i].window.dispose();
          else if (displays[i].applet!=null) {
            displays[i].applet.remove(displays[i].embedPanel);
            displays[i].applet.layout();
          }
  }

  /** Initialize, either partially or completely.
    *@see parse.Parsable#initialize
    */
  public void initialize(int level) {
    for (int i=0;i<displays.length;i++)
      displays[i].initialize(level);
  }
} //end DisplayList

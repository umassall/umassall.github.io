package sim.display;
import Display;
import watch.*;
import pointer.*;
import parse.*;
import expression.*;
import external.poskanzer.*; //GIF encoder
import java.awt.*;
import java.io.*;

/** This Display takes another Display as a parameter, and periodically
  * copies it to the hard drive as a GIF.  Other programs can take the
  * series of GIFs and create an animated GIF, FLI, FLC, MOV or other
  * animation.
  *    <p>This code is (c) 1997 Leemon Baird
  *    <<a href=mailto:leemon@cs.cmu.edu>leemon@cs.cmu.edu</a>>,
  *    <a href=http://www.cs.cmu.edu/~baird>http://www.cs.cmu.edu/~baird</a><br>
  *    The source and object code may be redistributed freely.
  *    If the code is modified, please state so in the comments.
  * @version 1.1, 20 Feb 97
  * @author Leemon Baird
  */
public class SaveDisplay extends Display {
  //1.1, 29 May 97:   made corrections to the unparse method - Scott Weaver
  //1.0, 20 Feb 97

  protected PString   filename=new PString("");     //files are named as: filename+"0000.gif"
  protected PString   trigger =new PString("");     //name of watchable variable that triggers capture
  protected IntExp    freq    =new IntExp(1);       //capture a GIF every time it changes this many times
  protected IntExp    max     =new IntExp(10);      //maximum number of GIFs to save overall
  protected Display[] display ={null};              //the embedded Display to capture to disk
  protected PBoolean  animate =new PBoolean(false); //create one animated GIF?

  protected PInt      numSaved=new PInt(-1);        //how many GIFs have been saved so far
  protected File      file    =null;                //file for the GIF
  protected FileOutputStream fileStream=null;       //file for the GIF
  protected Pointer   triggerVar=null;              //the variable corresponding to trigger

  private Object[][] parameters=
     {{"Save GIFs periodically. The image of the given "+
       "display is saved to disk periodically."+
       "If the given directory doesn't exist, "+
       "it will not be created, and no files will be written."+
       "WARNING: SaveDisplay will automatically "+
       "overwrite any files with the same names. "},
      {"animate",   animate,  "true=save in one big animated GIF file,"+
                              "false=save in separate files, appending '0001.gif' "+
                              "to the first filename, then incrementing for the rest",
       "filename",  filename, "filename to save to",
       "trigger",   trigger,  "save a frame every freq times trigger changes",
       "freq",      freq,     "save every freq times",
       "maxFrames", max,      "maximum # of frames to be saved",
       "display",   display,  "the Display whose image will be saved"},
      {}};

  /** Return a parameter array if BNF(), parse(), and unparse() are to be automated, null otherwise.
    * @see parse.Parsable#getParameters
    */
  public Object[][] getParameters(int lang) {
    return parameters;
  }

  /** Remember the WatchManager for this object and create the window.
    * After everything is parsed and windows are created, all experiments
    * are given a watchManager by Simulator, then it starts giving each
    * Display a watchManager.  This is where
    * the Display should register each variable it wants to watch.
    */
  public void setWatchManager(WatchManager wm,String name) {
    super.setWatchManager(wm,name);
    wm.registerVar(name+"frames saved", numSaved,this);
    display[0].setWatchManager(wm,"disp/"+name);
  }

  /** One of the watched variables has been unregistered.
    */
  public void unregister(String watchedVar) {
  } //end unregister

  /** add menu items to the window containing this GWin canvas. */
  public void addMenus(MenuBar mb) {
    display[0].addMenus(mb); //give embedded Display a chance to have menus
  }

  /** respond to the menu choices */
  public boolean action(Event e,Object w) {
    return display[0].action(e,w); //let embedded Display respond to a menu selection
  }

  /** The trigger changed the appropriate number of times, so
    * save the GIF to disk.
    */
  public void update(String changedName, Pointer changedVar, Watchable obj) {
    super.update(changedName,changedVar,obj);
    if (changedVar!=triggerVar)
      return;
    if (numSaved.val>=max.val)  //quit after created the max # of GIFs
      return;
    numSaved.val++;
    if (numSaved.val==0)
      return; //don't save anything the very first time called

    Image     img=null;
    Graphics  g  =null;
    Rectangle b  =null;

    if (!animate.val || numSaved.val==1)
      file=new File(filename.val);
    if (file!=null) //filename is OK
      try {
        b=display[0].bounds();
        if (animate.val && numSaved.val==1)
          fileStream=new FileOutputStream(file);
        else if (!animate.val)
          fileStream=new FileOutputStream(
                   (numSaved.val>9999 ? filename.val+(numSaved.val/10000)
                                      : filename.val)
                   +Character.forDigit((numSaved.val/1000)%10,10)
                   +Character.forDigit((numSaved.val/100 )%10,10)
                   +Character.forDigit((numSaved.val/10  )%10,10)
                   +Character.forDigit((numSaved.val     )%10,10)
                   +".gif");
        img=display[0].createImage(b.width,b.height);
        g=img.getGraphics();
        g.translate(-b.x,-b.y); //corner of display is corner of g
        fix.Util.paintAll(g,display[0],false,true); //force display to draw self into this Image
        GifEncoder ge=new GifEncoder(img,fileStream);
        ge.setInterlace(true); //GIF89a interlaced format
        if (!animate.val)
          ge.setOnlyFrame();   //this is not an animated GIF
        else if (numSaved.val==1)
          ge.setFirstFrame();
        else if (numSaved.val==max.val)
          ge.setLastFrame();
        else
          ge.setMiddleFrame();
        ge.encode();           //output the display as a GIF
      } catch (Throwable ee) { //disk full, etc.
      } finally {
        if (g!=null)
          g.dispose(); //dispose of graphics context properly
        try {
          if (!animate.val || numSaved.val==max.val)
            fileStream.close();
        } catch (Throwable eee) {
        }
      }
  } //end update

  /** Initialize, either partially or completely.
    *@see parse.Parsable#initialize
    */
  public void initialize(int level) {
    super.initialize(level);
    if (level==0) {
      triggerVar=watchManager.registerWatch(trigger.val,freq,this);
      setLayout(new BorderLayout(0,0));
      add("Center",(Panel)display[0]);
      display[0].show();
      show();
      layout();
    }
    display[0].initialize(level);
  }
} //end SaveDisplay

import watch.*;
import pointer.*;
import java.awt.*;
import parse.*;
import expression.*;

/** This Display shows a title at the top, with another Display embedded inside.
  *    <p>This code is (c) 1997 Leemon Baird
  *    <<a href=mailto:leemon@cs.cmu.edu>leemon@cs.cmu.edu</a>>,
  *    <a href=http://www.cs.cmu.edu/~baird>http://www.cs.cmu.edu/~baird</a><br>
  *    The source and object code may be redistributed freely.
  *    If the code is modified, please state so in the comments.
  * @version 1.02, 18 July 97
  * @author Leemon Baird
  */
public class Title extends Display {
  //version 1.02 18 July  97
  //Version 1.01 15 April 97
  //Version 1.00 7  Feb   97

  protected PString   title  =new PString(null); //the title to show
  protected Display[] display=new Display[1];    //the embedded Display with this title
  protected PInt      freq   =new PInt(1);       //redraw every time the title changes

  Label label=null; //the title at the top

  /** constructor that does nothing */
  public Title() {super();} //CAFE bug: this must be typed here, even though it's supposedly redundant

  /** This constructor takes the same parameters that parse() parses, and
    * initializes this object just as if it had been created by parsing instead.
    * This window contains the Display disp, and the title is titleString.
    */
  public Title(String titleString, Display disp) {
    setBackground(Color.white);
    title.val=titleString;
    display[0]=disp;
  }

  private Object[][] parameters=
    {{"Title on a Display. Show the given Display with the given title at the top"},
     {"title",    title,  "string to use as the title",
      "display",display,"the display to show below the title"},
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
    display[0].setWatchManager(watchManager,wmName);
  }

  /** add menu items to the window containing this GWin canvas. */
  public void addMenus(MenuBar mb) {
    display[0].addMenus(mb); //give embedded Display a chance to have menus
  }

  /** One of the watched variables has changed, so look at it and others.
    * It should call checkMoved() to
    * make sure the window is a legal size.
    */
  public void update(String changedName, Pointer changedVar, Watchable obj) {
    if (title!=null && label!=null) {
      label.setText(title.val);
      layout();
    }
  }

  /** respond to the menu choices */
  public boolean action(Event e,Object w) {
    return display[0].action(e,w); //let embedded Display respond to a menu selection
  }

  /** This is called when the user closes the window. Override it
    * to react to that event (e.g. kill threads, dispose of Graphics, unregister watches)
    */
  public void destroy () {
    super.destroy();
    display[0].destroy();
    watchManager.unregisterVar  (wmName+"title");
    watchManager.unregisterWatch(wmName+"title",this);
  }

  /** Initialize, either partially or completely.
    *@see parse.Parsable#initialize
    */
  public void initialize(int level) {
    super.initialize(level);
    if (level==0) { //initialize right after this object created and parse()/setWatchManager() called
      label=new Label(title.val,Label.CENTER);
      setLayout(new BorderLayout(0,0));
      add("North",label);
      add("Center",(Panel)display[0]);
      label.show();
      display[0].show();
      show();
      layout();
      watchManager.registerWatch(wmName+"title",freq,this); //react when someone changes my title
    }
    display[0].initialize(level);
  }
} //end Title
